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

package com.kingsrook.qbits.edi.flow;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.guide.GuideRepository;
import com.kingsrook.qbits.edi.map.InboundApplier;
import com.kingsrook.qbits.edi.map.OutboundMapper;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Transactional map/validate/envelope worker. Row locks fence replicas; a crash
 ** rolls the whole envelope back, leaving requests available for the next run.
 *******************************************************************************/
public class OutboundWorker
{
   /*******************************************************************************
    ** Process immediate definitions; batch processes call runDocument explicitly.
    *******************************************************************************/
   public Integer runOnce() throws QException
   {
      return (SystemWrites.call(() ->
      {
         int count = 0;
         for(QRecord document : EdiRecords.query("edi_agreement_document", EdiRecords.equal("direction", "OUTBOUND").withCriteria("isActive", QCriteriaOperator.EQUALS, true)))
         {
            if(!"BATCH".equals(document.getValueString("batchMode")))
            {
               count += runDocument(document.getValueInteger("id"));
            }
         }
         return (count);
      }));
   }

   /*******************************************************************************
    ** A scheduled batch invocation cuts one bounded batch for its definition.
    *******************************************************************************/
   public Integer runDocument(Integer id) throws QException
   {
      return (SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         QRecord document = row("edi_agreement_document", id);
         QRecord agreement = row("edi_agreement", document.getValueInteger("agreementId"));
         if(!Boolean.TRUE.equals(document.getValueBoolean("isActive")) || !Boolean.TRUE.equals(agreement.getValueBoolean("isActive")))
         {
            return (0);
         }
         var requests = EdiRecords.query("edi_outbound_request", EdiRecords.equal("agreementDocumentId", id).withCriteria("status", QCriteriaOperator.EQUALS, "PENDING").withLimit(100));
         if(requests.isEmpty())
         {
            return (0);
         }
         List<QRecord> accepted = new ArrayList<>();
         List<com.kingsrook.qbits.edi.document.EdiDocument> sets = new ArrayList<>();
         QRecord map = row("edi_map", document.getValueInteger("mapId"));
         var rules = EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id")));
         var associations = new java.util.LinkedHashSet<String>();
         for(QRecord rule : rules)
         {
            String path = rule.getValueString("qqqPath");
            if(path != null)
            {
               for(int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', dot + 1))
               {
                  associations.add(path.substring(0, dot));
               }
            }
         }
         var guide = new GuideRepository().load(document.getValueInteger("guideId"));
         for(QRecord request : requests)
         {
            if(!SystemWrites.guard("UPDATE edi_outbound_request SET id=id WHERE id=? AND status='PENDING'", request.getValue("id")))
            {
               continue;
            }
            try
            {
               com.kingsrook.qbits.edi.document.EdiDocument set = InboundApplier.asPartner(EdiRuntime.current().config().getServiceSessionSupplier(), agreement.getValueInteger("partnerId"), () ->
               {
                  String table = request.getValueString("sourceTableName");
                  String primaryKey = QContext.getQInstance().getTable(table).getPrimaryKeyField();
                  var query = new QueryInput(table).withFilter(EdiRecords.equal(primaryKey, request.getValue("sourceRecordId")))
                     .withIncludeAssociations(true).withAssociationNamesToInclude(associations).withTransaction(QContext.getQBackendTransaction());
                  query.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
                  var records = new QueryAction().execute(query).getRecords();
                  if(records.size() != 1 || (!"ACTIVE".equals(map.getValueString("status")) || !table.equals(map.getValueString("tableName"))))
                  {
                     throw new IllegalArgumentException("Source record or active map unavailable");
                  }
                  return ("EDIFACT".equals(agreement.getValueString("standard"))
                     ? new com.kingsrook.qbits.edi.edifact.EdifactMapper(com.kingsrook.qbits.edi.edifact.EdifactSeparators.fromAgreement(agreement).decimal()).message(records.getFirst(), map, rules, com.kingsrook.qbits.edi.edifact.EdifactOutbound.allocate(agreement, "UNH"))
                     : new OutboundMapper().build(records.getFirst(), map, rules, String.format("%04d", sets.size() + 1)));
               });
               if(EdiRuntime.current().validator().validate(set, guide, Boolean.TRUE.equals(document.getValueBoolean("containsPhi"))).stream().anyMatch(f -> "ERROR".equals(f.severity())))
               {
                  reject(request, agreement, "VALIDATION");
                  continue;
               }
               accepted.add(request);
               sets.add(set);
            }
            catch(Exception e)
            {
               reject(request, agreement, "MAPPING");
            }
            if(!"BATCH".equals(document.getValueString("batchMode")) && !sets.isEmpty())
            {
               envelope(agreement, document, map, accepted, sets, null);
               accepted.clear();
               sets.clear();
            }
         }
         if(!sets.isEmpty())
         {
            envelope(agreement, document, map, accepted, sets, null);
         }
         return (requests.size());
      })));
   }

   /*******************************************************************************
    ** Invalid documents are terminal requests and never enter the send queue.
    *******************************************************************************/
   private void reject(QRecord request, QRecord agreement, String type) throws Exception
   {
      SystemWrites.update("edi_outbound_request", new QRecord().withValue("id", request.getValue("id")).withValue("status", "FAILED").withValue("lastError", type));
      EdiJournal.exception(null, null, null, agreement.getValueInteger("partnerId"), type, "Outbound request failed " + type.toLowerCase(java.util.Locale.ROOT));
   }

   /*******************************************************************************
    ** Allocate controls once, store exact bytes, then persist all tracking records.
    *******************************************************************************/
   public static QRecord envelope(QRecord agreement, QRecord document, QRecord map, List<QRecord> requests, List<? extends com.kingsrook.qbits.edi.document.EdiDocument> sets, Integer original) throws Exception
   {
      if("EDIFACT".equals(agreement.getValueString("standard")))
      {
         return (com.kingsrook.qbits.edi.edifact.EdifactOutbound.envelope(agreement, document, map, requests,
            sets.stream().map(s -> (com.kingsrook.qbits.edi.edifact.EdifactMessage) s).toList(), original, null));
      }
      var runtime = EdiRuntime.current();
      QRecord local = row("edi_identity", agreement.getValueInteger("localIdentityId"));
      QRecord partner = row("edi_identity", agreement.getValueInteger("partnerIdentityId"));
      LocalDateTime at = LocalDateTime.ofInstant(runtime.config().getClock().instant(), ZoneOffset.UTC);
      String isaControl = String.format("%09d", allocate(agreement, "ISA"));
      String gsControl = String.valueOf(allocate(agreement, "GS"));
      String version = Objects.requireNonNullElse(agreement.getValueString("interchangeVersion"), "00401");
      String component = value(agreement, "componentSeparator", ":");
      String repetition = value(agreement, "repetitionSeparator", "^");
      X12Segment isa = X12Segment.of("ISA", 1, "00", "", "00", "", local.getValueString("interchangeQualifier"), local.getValueString("interchangeId"),
         partner.getValueString("interchangeQualifier"), partner.getValueString("interchangeId"), at.format(DateTimeFormatter.ofPattern("yyMMdd")), at.format(DateTimeFormatter.ofPattern("HHmm")),
         version.compareTo("00501") >= 0 ? repetition : "U", version, isaControl, Boolean.TRUE.equals(agreement.getValueBoolean("requestTa1")) ? "1" : "0", agreement.getValueString("usageIndicator"), component);
      X12Segment gs = X12Segment.of("GS", 2, document.getValueString("functionalIdCode"), local.getValueString("applicationCode"), partner.getValueString("applicationCode"), at.format(DateTimeFormatter.BASIC_ISO_DATE),
         at.format(DateTimeFormatter.ofPattern("HHmm")), gsControl, "X", document.getValueString("version"));
      var interchange = new X12Interchange(new X12Separators(value(agreement, "elementSeparator", "*").charAt(0), value(agreement, "segmentTerminator", "~").charAt(0), component.charAt(0), version.compareTo("00501") >= 0 ? repetition.charAt(0) : null),
         isa, X12Segment.of("IEA", 0, "1", isaControl), List.of(new X12FunctionalGroup(gs, X12Segment.of("GE", 0, String.valueOf(sets.size()), gsControl), sets.stream().map(s -> (X12TransactionSet) s).toList(), List.of())), List.of(), List.of());
      String ending = switch(value(agreement, "lineEnding", "NONE"))
      {
         case "LF" -> "\n";
         case "CRLF" -> "\r\n";
         default -> "";
      };
      var payload = runtime.payloads().put(PayloadKind.OUTBOUND, new ByteArrayInputStream(runtime.writer().write(interchange, ending).getBytes(StandardCharsets.UTF_8)));
      QRecord persisted = SystemWrites.insert("edi_interchange", new QRecord().withValue("direction", "OUTBOUND").withValue("partnerId", agreement.getValue("partnerId"))
         .withValue("agreementId", agreement.getValue("id")).withValue("transportId", agreement.getValue("outboundTransportId")).withValue("standard", "X12")
         .withValue("senderQualifier", isa.value(5)).withValue("senderId", isa.value(6)).withValue("receiverQualifier", isa.value(7)).withValue("receiverId", isa.value(8))
         .withValue("controlNumber", isaControl).withValue("interchangeVersion", version).withValue("usageIndicator", isa.value(15)).withValue("interchangeDate", runtime.config().getClock().instant())
         .withValue("groupCount", 1).withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256()).withValue("sizeBytes", payload.sizeBytes())
         .withValue("status", "ENVELOPED").withValue("containsPhi", document.getValue("containsPhi")).withValue("originalInterchangeId", original));
      boolean expectsAck = document.getValueString("ackExpectation") != null && !"NONE".equals(document.getValueString("ackExpectation"));
      QRecord group = SystemWrites.insert("edi_functional_group", new QRecord().withValue("interchangeId", persisted.getValue("id")).withValue("senderId", gs.value(2))
         .withValue("functionalIdCode", gs.value(1)).withValue("controlNumber", gsControl).withValue("version", gs.value(8)).withValue("transactionCount", sets.size())
         .withValue("ackStatus", expectsAck ? "PENDING" : "NONE"));
      for(int i = 0; i < sets.size(); i++)
      {
         var set = sets.get(i);
         String businessKey = map.getValueString("businessKeyPath") == null ? null : new com.kingsrook.qbits.edi.guide.RuleEvaluator(set.segments(), java.util.Map.of()).values(map.getValueString("businessKeyPath")).stream().findFirst().orElse(null);
         SystemWrites.insert("edi_transaction", new QRecord().withValue("interchangeId", persisted.getValue("id")).withValue("functionalGroupId", group.getValue("id"))
            .withValue("direction", "OUTBOUND").withValue("partnerId", agreement.getValue("partnerId")).withValue("agreementDocumentId", document.getValue("id"))
            .withValue("transactionSet", set.header().value(1)).withValue("version", gs.value(8)).withValue("controlNumber", set.header().value(2)).withValue("status", "QUEUED")
            .withValue("guideId", document.getValue("guideId")).withValue("guideRevision", document.getValue("guideId") == null ? null : row("edi_guide", document.getValue("guideId")).getValue("revision"))
            .withValue("targetTableName", i < requests.size() ? requests.get(i).getValue("sourceTableName") : null).withValue("targetRecordId", i < requests.size() ? requests.get(i).getValue("sourceRecordId") : null).withValue("mapId", map.getValue("id")).withValue("mapRevision", map.getValue("revision"))
            .withValue("businessKeyType", map.getValue("businessKeyType")).withValue(Boolean.TRUE.equals(document.getValueBoolean("containsPhi")) ? "phiBusinessKey" : "businessKey", businessKey)
            .withValue("containsPhi", document.getValue("containsPhi")).withValue("segmentCount", set.segments().size()));
         if(i < requests.size())
         {
            SystemWrites.update("edi_outbound_request", new QRecord().withValue("id", requests.get(i).getValue("id")).withValue("status", "ENVELOPED"));
         }
      }
      QRecord message = SystemWrites.insert("edi_outbound_message", new QRecord().withValue("interchangeId", persisted.getValue("id")).withValue("agreementId", agreement.getValue("id"))
         .withValue("partnerId", agreement.getValue("partnerId")).withValue("transportId", agreement.getValue("outboundTransportId")).withValue("status", "QUEUED").withValue("attempts", 0)
         .withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256()).withValue("sizeBytes", payload.sizeBytes()));
      SystemWrites.update("edi_interchange", new QRecord().withValue("id", persisted.getValue("id")).withValue("outboundMessageId", message.getValue("id")));
      EdiJournal.event(null, persisted.getValueInteger("id"), null, "ENVELOPED");
      return (message);
   }

   /*******************************************************************************
    ** Allocate in the configured agreement or identity scope.
    *******************************************************************************/
   private static long allocate(QRecord agreement, String level) throws Exception
   {
      boolean local = "LOCAL_IDENTITY".equals(agreement.getValueString("controlNumberScope"));
      return (EdiRuntime.current().controls().allocate(local ? null : agreement.getValueInteger("id"), local ? agreement.getValueInteger("localIdentityId") : null, level, 1, 999999999, true));
   }

   /*******************************************************************************
    ** Lookup required configuration without leaking submitted values.
    *******************************************************************************/
   public static QRecord row(String table, Serializable id) throws QException
   {
      if(id == null)
      {
         throw new QException("Required EDI record reference is absent");
      }
      return (EdiRecords.query(table, EdiRecords.equal("id", id)).getFirst());
   }

   /*******************************************************************************
    ** Default optional envelope settings.
    *******************************************************************************/
   private static String value(QRecord record, String field, String fallback)
   {
      return (Objects.requireNonNullElse(record.getValueString(field), fallback));
   }
}
