/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.edi.map;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.ack.AckEnvelope;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.document.EdiDocument;
import com.kingsrook.qbits.edi.edifact.ContrlBuilder;
import com.kingsrook.qbits.edi.edifact.EdifactEnvelopeReader;
import com.kingsrook.qbits.edi.edifact.EdifactWriter;
import com.kingsrook.qbits.edi.guide.Guide;
import com.kingsrook.qbits.edi.guide.GuideRepository;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.security.PhiLogScope;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Shared no-apply preview pipeline. All mapping, including LOOKUP, uses the caller.
 ** This service has no optional middleware dependency.
 *******************************************************************************/
public class MapPreviewService
{
   private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

   /*******************************************************************************
    ** Read ordered persisted rules, or order a copy of the unsaved replacement set.
    *******************************************************************************/
   public static List<QRecord> rules(QRecord map, List<QRecord> overrides) throws Exception
   {
      List<QRecord> result = new ArrayList<>(overrides == null ? EdiRecords.queryEnforced("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id"))) : overrides);
      result.sort(Comparator.comparing(r -> Objects.requireNonNullElse(r.getValueInteger("sequence"), 0)));
      return (result);
   }

   /*******************************************************************************
    ** PHI classification cannot be weakened by the caller or by hidden agreements.
    *******************************************************************************/
   public static boolean containsPhi(QRecord map) throws Exception
   {
      return (healthcare(map.getValueString("transactionSet")) || SystemWrites.call(() -> !EdiRecords.query("edi_agreement_document",
         EdiRecords.equal("mapId", map.getValue("id")).withCriteria("containsPhi", QCriteriaOperator.EQUALS, true)).isEmpty()));
   }

   /*******************************************************************************
    ** Healthcare document types always suppress values, even with a mismatched map.
    *******************************************************************************/
   private static boolean healthcare(String type)
   {
      return (type != null && List.of("837", "835", "270", "271", "276", "277", "834").contains(type));
   }

   /*******************************************************************************
    ** ORIGINAL is the pinned row; CURRENT follows only the same guide family.
    *******************************************************************************/
   public static Guide guide(QRecord map, String revision) throws Exception
   {
      Integer id = map.getValueInteger("guideId");
      if("CURRENT".equals(revision))
      {
         var pinnedRows = EdiRecords.queryEnforced("edi_guide", EdiRecords.equal("id", id));
         if(pinnedRows.isEmpty())
         {
            throw (new com.kingsrook.qqq.backend.core.exceptions.QNotFoundException("Not found"));
         }
         QRecord pinned = pinnedRows.getFirst();
         var candidates = EdiRecords.queryEnforced("edi_guide", EdiRecords.equal("name", pinned.getValue("name")).withCriteria("status", QCriteriaOperator.EQUALS, "ACTIVE"));
         candidates.removeIf(r -> !Objects.equals(r.getValue("partnerId"), pinned.getValue("partnerId"))
            || !Objects.equals(r.getValue("standard"), pinned.getValue("standard"))
            || !Objects.equals(r.getValue("version"), pinned.getValue("version"))
            || !Objects.equals(r.getValue("transactionSet"), pinned.getValue("transactionSet")));
         candidates.sort(Comparator.comparing((QRecord r) -> Objects.requireNonNullElse(r.getValueInteger("revision"), 0)).reversed());
         if(!candidates.isEmpty())
         {
            id = candidates.getFirst().getValueInteger("id");
         }
      }
      return (new GuideRepository().load(id, true));
   }

   /*******************************************************************************
    ** Parse and inspect the complete sample before exposing any values.
    *******************************************************************************/
   public Map<String, Object> preview(QRecord map, QRecord sample, List<QRecord> overrides, String revision) throws Exception
   {
      EdiRuntime runtime = EdiRuntime.current();
      boolean phi = containsPhi(map);
      Guide guide = guide(map, revision);
      List<QRecord> rules = rules(map, overrides);
      State state = new State();
      try(var logging = PhiLogScope.open(true); InputStream stream = SystemWrites.call(() -> runtime.payloads().open(new PayloadRef(sample.getValueString("payloadRef"), null, 0))))
      {
         if("EDIFACT".equals(map.getValueString("standard")))
         {
            var interchanges = new EdifactEnvelopeReader(runtime.config().getMaxSegmentsPerTransaction()).read(stream);
            state.interchanges = interchanges.size();
            for(var interchange : interchanges)
            {
               state.groups += interchange.groups().size();
               state.envelope(interchange.findings());
               interchange.groups().forEach(group -> state.envelope(group.findings()));
               Map<String, List<ValidationFinding>> all = new LinkedHashMap<>();
               for(var message : interchange.allMessages())
               {
                  all.put(message.reference(), document(state, map, sample, rules, guide, message, phi));
               }
               if(interchange.unb() != null)
               {
                  var ack = new ContrlBuilder().build(interchange, all, AckLevel.ALL_TRANSACTIONS, "1", map.getValueString("version"));
                  state.ack("CONTRL", phi ? null : new EdifactWriter().write(ack.segments(), interchange.separators(), ""));
               }
            }
         }
         else
         {
            var interchanges = runtime.reader().read(stream);
            phi = phi || interchanges.stream().flatMap(i -> i.groups().stream()).flatMap(g -> g.transactions().stream()).anyMatch(t -> healthcare(t.st().value(1)));
            state.interchanges = interchanges.size();
            for(var interchange : interchanges)
            {
               state.groups += interchange.groups().size();
               state.envelope(interchange.findings());
               if(!interchange.findings().isEmpty())
               {
                  var ta1 = runtime.ta1().build(interchange.isa(), interchange.findings());
                  var isa = AckEnvelope.replyIsa(interchange.isa(), "1", java.time.LocalDateTime.now(runtime.config().getClock()));
                  var reply = new com.kingsrook.qbits.edi.x12.X12Interchange(interchange.separators(), isa,
                     com.kingsrook.qbits.edi.x12.X12Segment.of("IEA", 3, "0", isa.value(13)), List.of(), List.of(ta1), List.of());
                  state.ack("TA1", phi ? null : runtime.writer().write(reply));
               }
               for(var group : interchange.groups())
               {
                  state.envelope(group.findings());
                  Map<String, List<ValidationFinding>> all = new LinkedHashMap<>();
                  for(var transaction : group.transactions())
                  {
                     all.put(transaction.st().value(2), document(state, map, sample, rules, guide, transaction, phi));
                  }
                  boolean use999 = phi && group.gs().value(8).startsWith("005010X");
                  var ack = use999 ? runtime.ack999().build(group, all, AckLevel.ALL_TRANSACTIONS, "0001") : runtime.ack997().build(group, all, AckLevel.ALL_TRANSACTIONS, "0001");
                  state.ack(use999 ? "999" : "997", phi ? null : runtime.writer().write(AckEnvelope.wrap(interchange, group, ack, "1", "1", java.time.LocalDateTime.now(runtime.config().getClock()))));
               }
            }
         }
      }
      catch(com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         // Parser, custom transform and expected-JSON diagnostics may contain payload values.
         throw (new QBadRequestException("Sample could not be previewed; check the document, guide and rules."));
      }
      return (object("parse", Map.of("interchanges", state.interchanges, "groups", state.groups, "documents", state.documents),
         "findings", state.findings, "ack", object("type", state.ackType, "text", phi ? null : state.ackText.toString()),
         "result", phi ? null : Map.of("records", state.records), "diff", Map.of("againstPreviousRevision", state.previous, "againstExpected", state.expected), "phiSuppressed", phi));
   }

   /*******************************************************************************
    ** Validate and map one document, collecting only value-free findings for PHI.
    *******************************************************************************/
   private List<ValidationFinding> document(State state, QRecord map, QRecord sample, List<QRecord> rules, Guide guide, EdiDocument document, boolean phi) throws Exception
   {
      int index = state.documents++;
      state.envelope(document.findings());
      var findings = EdiRuntime.current().validator().validate(document, guide, phi);
      for(var finding : findings)
      {
         state.findings.add(object("snipLevel", finding.snipLevel(), "severity", finding.severity(), "segmentPosition", finding.segmentPosition(),
            "segmentId", finding.segmentId(), "loopId", finding.loopId(), "elementPosition", finding.elementPosition(),
            "code", finding.elementPosition() == null ? finding.ak3Code() : finding.ak4Code(), "message", phi ? "Validation finding" : finding.message()));
      }
      QRecord mapped;
      try
      {
         mapped = new InboundMapper(EdiRuntime.current().transforms()).map(document, guide, map, rules).record();
      }
      catch(com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         state.findings.add(object("snipLevel", 0, "severity", "ERROR", "segmentPosition", null, "segmentId", null, "loopId", null,
            "elementPosition", null, "code", "MAP", "message", "Mapping failed; check the rule paths and transform arguments"));
         return (findings);
      }
      if(!phi)
      {
         Map<String, Object> actual = record(mapped);
         state.records.add(actual);
         String expected = sample.getValueString("expectedResultJson");
         if(expected != null)
         {
            difference(state.expected, index, JSON.readValue(expected, Object.class), actual);
         }
         Integer revision = map.getValueInteger("revision");
         var previous = EdiRecords.queryEnforced("edi_map", EdiRecords.equal("name", map.getValue("name")));
         previous.removeIf(r -> !Objects.equals(r.getValue("partnerId"), map.getValue("partnerId"))
            || !Objects.equals(r.getValue("standard"), map.getValue("standard")) || !Objects.equals(r.getValue("transactionSet"), map.getValue("transactionSet"))
            || !Objects.equals(r.getValue("direction"), map.getValue("direction")) || !Objects.equals(r.getValue("version"), map.getValue("version"))
            || !Objects.equals(r.getValue("tableName"), map.getValue("tableName"))
            || revision == null || r.getValueInteger("revision") == null || r.getValueInteger("revision") >= revision);
         previous.sort(Comparator.comparing((QRecord r) -> r.getValueInteger("revision")).reversed());
         if(!previous.isEmpty() && !containsPhi(previous.getFirst()))
         {
            QRecord prior = previous.getFirst();
            QRecord priorResult = new InboundMapper(EdiRuntime.current().transforms()).map(document, guide(prior, "ORIGINAL"), prior, rules(prior, null)).record();
            difference(state.previous, index, record(priorResult), actual);
         }
      }
      return (findings);
   }

   /*******************************************************************************
    ** Stable record values and recursive associations, without QQQ diagnostics.
    *******************************************************************************/
   private static Map<String, Object> record(QRecord record)
   {
      Map<String, Object> associations = new LinkedHashMap<>();
      if(record.getAssociatedRecords() != null)
      {
         record.getAssociatedRecords().forEach((name, children) -> associations.put(name, children.stream().map(MapPreviewService::record).toList()));
      }
      return (object("values", record.getValues(), "associations", associations));
   }

   /*******************************************************************************
    ** Compare normalized JSON so temporal values and numeric types match wire output.
    *******************************************************************************/
   private static void difference(List<Map<String, Object>> result, int index, Object before, Object after)
   {
      if(!JSON.valueToTree(before).equals(JSON.valueToTree(after)))
      {
         result.add(object("documentIndex", index, "before", before, "after", after));
      }
   }

   /*******************************************************************************
    ** Ordered objects allowing explicit nulls on the wire.
    *******************************************************************************/
   private static Map<String, Object> object(Object... pairs)
   {
      Map<String, Object> result = new LinkedHashMap<>();
      for(int i = 0; i < pairs.length; i += 2)
      {
         result.put((String) pairs[i], pairs[i + 1]);
      }
      return (result);
   }

   /*******************************************************************************
    ** One invocation's accumulation; never shared across requests.
    *******************************************************************************/
   private static class State
   {
      private int interchanges;
      private int groups;
      private int documents;
      private final List<Map<String, Object>> findings = new ArrayList<>();
      private final List<Map<String, Object>> records = new ArrayList<>();
      private final List<Map<String, Object>> previous = new ArrayList<>();
      private final List<Map<String, Object>> expected = new ArrayList<>();
      private String ackType = "NONE";
      private final StringBuilder ackText = new StringBuilder();

      /*******************************************************************************
       ** Envelope diagnostics carry no raw envelope identifiers.
       *******************************************************************************/
      private void envelope(List<EnvelopeFinding> values)
      {
         values.forEach(f -> findings.add(object("snipLevel", 1, "severity", "ERROR", "segmentPosition", f.segmentPosition(),
            "segmentId", null, "loopId", null, "elementPosition", null, "code", f.code(), "message", "Envelope validation finding")));
      }

      /*******************************************************************************
       ** Aggregate acknowledgements in wire order.
       *******************************************************************************/
      private void ack(String type, String text)
      {
         ackType = "NONE".equals(ackType) || type.equals(ackType) ? type : "MIXED";
         if(text != null)
         {
            ackText.append(text);
         }
      }
   }
}
