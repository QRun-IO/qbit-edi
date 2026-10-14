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
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.guide.GuideRepository;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** No-op apply preview with ordinary Result lines; PHI never enters process output.
 *******************************************************************************/
public class MapPreview
{
   /*******************************************************************************
    ** Parse, validate, acknowledge and map a stored synthetic sample without target writes.
    *******************************************************************************/
   public ArrayList<ProcessSummaryLineInterface> preview(QRecord map, QRecord sample) throws Exception
   {
      EdiRuntime runtime = EdiRuntime.current();
      boolean phi = List.of("837", "835", "270", "271", "276", "277", "834").contains(map.getValueString("transactionSet"));
      phi = phi || com.kingsrook.qbits.edi.support.SystemWrites.call(() -> !EdiRecords.query("edi_agreement_document", EdiRecords.equal("mapId", map.getValue("id"))
         .withCriteria("containsPhi", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, true)).isEmpty());
      var guide = new GuideRepository().load(map.getValueInteger("guideId"));
      ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
      try(var logging = com.kingsrook.qbits.edi.security.PhiLogScope.open(phi); InputStream stream = runtime.payloads().open(new PayloadRef(sample.getValueString("payloadRef"), null, 0)))
      {
         var interchanges = runtime.reader().read(stream);
         lines.add(new ProcessSummaryLine(Status.INFO, interchanges.size(), "interchanges parsed"));
         for(var interchange : interchanges)
         {
            lines.add(new ProcessSummaryLine(Status.INFO, interchange.groups().size(), "functional groups parsed"));
            for(var group : interchange.groups())
            {
               lines.add(new ProcessSummaryLine(Status.INFO, group.transactions().size(), "transaction sets parsed"));
               Map<String, List<com.kingsrook.qbits.edi.guide.ValidationFinding>> all = new java.util.LinkedHashMap<>();
               for(var transaction : group.transactions())
               {
                  var findings = runtime.validator().validate(transaction, guide, phi);
                  all.put(transaction.st().value(2), findings);
                  for(int snip = 1; snip <= 7; snip++)
                  {
                     int level = snip;
                     lines.add(new ProcessSummaryLine(Status.INFO, (int) findings.stream().filter(f -> f.snipLevel() == level).count(), "findings at SNIP " + snip));
                  }
                  List<QRecord> rules = EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id")));
                  rules.sort(java.util.Comparator.comparing(r -> r.getValueInteger("sequence")));
                  var mapped = InboundApplier.asService(runtime.config().getServiceSessionSupplier(), () -> new InboundMapper(runtime.transforms()).map(transaction, guide, map, rules));
                  if(phi)
                  {
                     lines.add(new ProcessSummaryLine(Status.INFO, "PHI map result suppressed; no target records written"));
                  }
                  else
                  {
                     ObjectMapper json = new ObjectMapper().findAndRegisterModules();
                     String actual = resultJson(mapped.record());
                     lines.add(new ProcessSummaryLine(Status.INFO, actual));
                     String expected = sample.getValueString("expectedResultJson");
                     if(expected != null)
                     {
                        diff(lines, "Expected result", json.readTree(expected), json.readTree(actual));
                     }
                     Integer revision = map.getValueInteger("revision");
                     var previous = EdiRecords.query("edi_map", EdiRecords.equal("name", map.getValue("name")));
                     previous.removeIf(r -> !java.util.Objects.equals(r.getValue("partnerId"), map.getValue("partnerId"))
                        || revision == null || r.getValueInteger("revision") == null || r.getValueInteger("revision") >= revision);
                     previous.sort(java.util.Comparator.comparing((QRecord r) -> r.getValueInteger("revision")).reversed());
                     if(!previous.isEmpty())
                     {
                        QRecord prior = previous.getFirst();
                        var priorGuide = new GuideRepository().load(prior.getValueInteger("guideId"));
                        List<QRecord> priorRules = EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", prior.getValue("id")));
                        priorRules.sort(java.util.Comparator.comparing(r -> r.getValueInteger("sequence")));
                        var priorResult = InboundApplier.asService(runtime.config().getServiceSessionSupplier(),
                           () -> new InboundMapper(runtime.transforms()).map(transaction, priorGuide, prior, priorRules));
                        diff(lines, "Previous revision " + prior.getValueInteger("revision"), json.readTree(resultJson(priorResult.record())), json.readTree(actual));
                     }
                  }
               }
               var ack = phi && group.gs().value(8).startsWith("005010X") ? runtime.ack999().build(group, all, AckLevel.ALL_TRANSACTIONS, "0001")
                  : runtime.ack997().build(group, all, AckLevel.ALL_TRANSACTIONS, "0001");
               lines.add(new ProcessSummaryLine(Status.INFO, phi ? "Acknowledgement preview suppressed for PHI document"
                  : runtime.writer().write(com.kingsrook.qbits.edi.ack.AckEnvelope.wrap(interchange, group, ack, "1", "1", java.time.LocalDateTime.now(runtime.config().getClock())))));
            }
         }
      }
      return (lines);
   }

   /*******************************************************************************
    ** Stable record JSON excludes QRecord diagnostics and implementation metadata.
    *******************************************************************************/
   private static String resultJson(QRecord record) throws Exception
   {
      Map<String, Object> result = new java.util.LinkedHashMap<>();
      result.put("values", record.getValues());
      Map<String, Object> associations = new java.util.LinkedHashMap<>();
      if(record.getAssociatedRecords() != null)
      {
         for(var entry : record.getAssociatedRecords().entrySet())
         {
            List<Object> children = new ArrayList<>();
            for(QRecord child : entry.getValue())
            {
               children.add(new ObjectMapper().readTree(resultJson(child)));
            }
            associations.put(entry.getKey(), children);
         }
      }
      result.put("associations", associations);
      return (new ObjectMapper().findAndRegisterModules().writeValueAsString(result));
   }

   /*******************************************************************************
    ** Show a structured before/after diff for synthetic samples only.
    *******************************************************************************/
   private static void diff(ArrayList<ProcessSummaryLineInterface> lines, String label, com.fasterxml.jackson.databind.JsonNode before, com.fasterxml.jackson.databind.JsonNode after) throws Exception
   {
      if(before.equals(after))
      {
         lines.add(new ProcessSummaryLine(Status.INFO, label + " matches"));
      }
      else
      {
         Map<String, Object> difference = Map.of("comparison", label, "before", before, "after", after);
         lines.add(new ProcessSummaryLine(Status.INFO, label + " differs: " + new ObjectMapper().writeValueAsString(difference)));
      }
   }
}
