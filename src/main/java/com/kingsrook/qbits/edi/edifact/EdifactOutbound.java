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

package com.kingsrook.qbits.edi.edifact;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.EdiJournal;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.guide.RuleEvaluator;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** EDIFACT outbound envelopes and durable tracking through the existing send queue.
 *******************************************************************************/
public class EdifactOutbound
{
   /*******************************************************************************
    ** Allocate independent UNB/UNH sequences within the configured control scope.
    *******************************************************************************/
   public static String allocate(QRecord agreement, String level) throws Exception
   {
      boolean local = "LOCAL_IDENTITY".equals(agreement.getValueString("controlNumberScope"));
      return (String.valueOf(EdiRuntime.current().controls().allocate(local ? null : agreement.getValueInteger("id"),
         local ? agreement.getValueInteger("localIdentityId") : null, level, 1, 99999999999999L, true)));
   }

   /*******************************************************************************
    ** Build an ungrouped interchange; UNZ counts messages when UNG is absent.
    *******************************************************************************/
   public static EdifactInterchange wrap(QRecord agreement, QRecord sender, QRecord recipient, List<EdifactMessage> messages, String control, LocalDateTime at)
   {
      String syntax = Objects.requireNonNullElse(agreement.getValueString("interchangeVersion"), "3");
      if(!List.of("3", "4").contains(syntax))
      {
         throw new IllegalArgumentException("EDIFACT syntax version must be 3 or 4");
      }
      X12Segment unb = EdifactWriter.segment("UNB", 1, List.of(Objects.requireNonNullElse(agreement.getValueString("syntaxIdentifier"), "UNOC"), syntax),
         List.of(sender.getValueString("interchangeId"), Objects.requireNonNullElse(sender.getValueString("interchangeQualifier"), "")),
         List.of(recipient.getValueString("interchangeId"), Objects.requireNonNullElse(recipient.getValueString("interchangeQualifier"), "")),
         List.of(at.format(DateTimeFormatter.ofPattern("4".equals(syntax) ? "yyyyMMdd" : "yyMMdd")), at.format(DateTimeFormatter.ofPattern("HHmm"))),
         List.of(control), List.of(""), List.of(""), List.of(""), List.of(""), List.of(""), List.of("T".equals(agreement.getValueString("usageIndicator")) ? "1" : ""));
      return (new EdifactInterchange(EdifactSeparators.fromAgreement(agreement), unb, X12Segment.of("UNZ", 0, String.valueOf(messages.size()), control), List.of(), List.copyOf(messages), List.of()));
   }

   /*******************************************************************************
    ** Store bytes and record checkpoints atomically with outbound requests or ack fencing.
    ** A logical tracking group carries ack deadlines even when UNG is absent on the wire.
    *******************************************************************************/
   public static QRecord envelope(QRecord agreement, QRecord document, QRecord map, List<QRecord> requests, List<EdifactMessage> messages, Integer original, Integer ackFor) throws Exception
   {
      var runtime = EdiRuntime.current();
      var interchange = wrap(agreement, OutboundWorker.row("edi_identity", agreement.getValue("localIdentityId")), OutboundWorker.row("edi_identity", agreement.getValue("partnerIdentityId")),
         messages, allocate(agreement, "UNB"), LocalDateTime.ofInstant(runtime.config().getClock().instant(), ZoneOffset.UTC));
      String ending = switch(Objects.requireNonNullElse(agreement.getValueString("lineEnding"), "NONE"))
      {
         case "LF" -> "\n";
         case "CRLF" -> "\r\n";
         default -> "";
      };
      var payload = runtime.payloads().put(PayloadKind.OUTBOUND, new ByteArrayInputStream(new EdifactWriter().write(interchange, ending).getBytes(StandardCharsets.UTF_8)));
      QRecord persisted = SystemWrites.insert("edi_interchange", new QRecord().withValue("direction", "OUTBOUND").withValue("partnerId", agreement.getValue("partnerId"))
         .withValue("agreementId", agreement.getValue("id")).withValue("transportId", agreement.getValue("outboundTransportId")).withValue("standard", "EDIFACT")
         .withValue("senderQualifier", interchange.unb().value(2, 1, 2)).withValue("senderId", interchange.unb().value(2))
         .withValue("receiverQualifier", interchange.unb().value(3, 1, 2)).withValue("receiverId", interchange.unb().value(3))
         .withValue("controlNumber", interchange.unb().value(5)).withValue("interchangeVersion", interchange.unb().value(1, 1, 2)).withValue("usageIndicator", interchange.usage())
         .withValue("interchangeDate", runtime.config().getClock().instant()).withValue("groupCount", 0).withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256())
         .withValue("sizeBytes", payload.sizeBytes()).withValue("status", "ENVELOPED").withValue("originalInterchangeId", original)
         .withValue("containsPhi", document != null && Boolean.TRUE.equals(document.getValueBoolean("containsPhi"))));
      boolean expects = document != null && "CONTRL".equals(document.getValueString("ackExpectation"));
      QRecord group = SystemWrites.insert("edi_functional_group", new QRecord().withValue("interchangeId", persisted.getValue("id")).withValue("senderId", interchange.unb().value(2))
         .withValue("controlNumber", interchange.unb().value(5)).withValue("version", messages.getFirst().version()).withValue("transactionCount", messages.size()).withValue("ackStatus", expects ? "PENDING" : "NONE"));
      for(int i = 0; i < messages.size(); i++)
      {
         var message = messages.get(i);
         String key = map == null || map.getValueString("businessKeyPath") == null ? null : new RuleEvaluator(message.segments(), Map.of()).values(map.getValueString("businessKeyPath")).stream().findFirst().orElse(null);
         SystemWrites.insert("edi_transaction", new QRecord().withValue("interchangeId", persisted.getValue("id")).withValue("functionalGroupId", group.getValue("id"))
            .withValue("direction", "OUTBOUND").withValue("partnerId", agreement.getValue("partnerId")).withValue("agreementDocumentId", document == null ? null : document.getValue("id"))
            .withValue("transactionSet", message.type()).withValue("version", message.version()).withValue("controlNumber", message.reference()).withValue("status", "QUEUED")
            .withValue("guideId", document == null ? null : document.getValue("guideId"))
            .withValue("guideRevision", document == null || document.getValue("guideId") == null ? null : OutboundWorker.row("edi_guide", document.getValue("guideId")).getValue("revision")).withValue("mapId", map == null ? null : map.getValue("id"))
            .withValue("mapRevision", map == null ? null : map.getValue("revision"))
            .withValue(document != null && Boolean.TRUE.equals(document.getValueBoolean("containsPhi")) ? "phiBusinessKey" : "businessKey", key)
            .withValue("businessKeyType", map == null ? null : map.getValue("businessKeyType"))
            .withValue("containsPhi", document != null && Boolean.TRUE.equals(document.getValueBoolean("containsPhi"))).withValue("segmentCount", message.segments().size())
            .withValue("targetTableName", i < requests.size() ? requests.get(i).getValue("sourceTableName") : null).withValue("targetRecordId", i < requests.size() ? requests.get(i).getValue("sourceRecordId") : null));
         if(i < requests.size())
         {
            SystemWrites.update("edi_outbound_request", new QRecord().withValue("id", requests.get(i).getValue("id")).withValue("status", "ENVELOPED"));
         }
      }
      QRecord queued = SystemWrites.insert("edi_outbound_message", new QRecord().withValue("interchangeId", persisted.getValue("id")).withValue("agreementId", agreement.getValue("id"))
         .withValue("partnerId", agreement.getValue("partnerId")).withValue("transportId", agreement.getValue("outboundTransportId")).withValue("status", "QUEUED").withValue("attempts", 0)
         .withValue("ackForInterchangeId", ackFor).withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256()).withValue("sizeBytes", payload.sizeBytes()));
      SystemWrites.update("edi_interchange", new QRecord().withValue("id", persisted.getValue("id")).withValue("outboundMessageId", queued.getValue("id")));
      EdiJournal.event(null, persisted.getValueInteger("id"), null, "ENVELOPED");
      return (queued);
   }

   /*******************************************************************************
    ** Re-envelope archived outbound messages without reading changed source records.
    *******************************************************************************/
   public static void resendAsNew(QRecord original) throws Exception
   {
      QRecord agreement = OutboundWorker.row("edi_agreement", original.getValue("agreementId"));
      QRecord oldInterchange = OutboundWorker.row("edi_interchange", original.getValue("interchangeId"));
      EdifactInterchange archive;
      try(var stream = EdiRuntime.current().payloads().open(new com.kingsrook.qbits.edi.storage.PayloadRef(original.getValueString("payloadRef"), null, 0)))
      {
         archive = new EdifactEnvelopeReader(EdiRuntime.current().config().getMaxSegmentsPerTransaction()).read(stream).getFirst();
      }
      List<EdifactMessage> messages = new java.util.ArrayList<>();
      for(var message : archive.allMessages())
      {
         String reference = allocate(agreement, "UNH");
         var segments = new java.util.ArrayList<>(message.segments());
         var elements = new java.util.ArrayList<>(message.header().elements());
         elements.set(0, List.of(List.of(reference)));
         segments.set(0, new X12Segment("UNH", 1, elements));
         segments.set(segments.size() - 1, X12Segment.of("UNT", segments.size(), String.valueOf(segments.size()), reference));
         messages.add(new EdifactMessage(segments.getFirst(), segments.getLast(), List.copyOf(segments), List.of()));
      }
      var oldTransactions = com.kingsrook.qbits.edi.storage.EdiRecords.query("edi_transaction", com.kingsrook.qbits.edi.storage.EdiRecords.equal("interchangeId", oldInterchange.getValue("id")));
      QRecord first = oldTransactions.getFirst();
      QRecord document = first.getValue("agreementDocumentId") == null ? null : OutboundWorker.row("edi_agreement_document", first.getValue("agreementDocumentId"));
      QRecord queued = envelope(agreement, document, null, List.of(), messages, oldInterchange.getValueInteger("id"), null);
      var newTransactions = com.kingsrook.qbits.edi.storage.EdiRecords.query("edi_transaction", com.kingsrook.qbits.edi.storage.EdiRecords.equal("interchangeId", queued.getValue("interchangeId")));
      for(int i = 0; i < archive.allMessages().size(); i++)
      {
         String reference = archive.allMessages().get(i).reference();
         QRecord old = oldTransactions.stream().filter(t -> reference.equals(t.getValueString("controlNumber"))).findFirst().orElseThrow();
         QRecord update = new QRecord().withValue("id", newTransactions.get(i).getValue("id"));
         for(String field : List.of("guideId", "guideRevision", "mapId", "mapRevision", "businessKey", "phiBusinessKey", "containsPhi", "businessKeyType", "secondaryKey", "targetTableName", "targetRecordId", "agreementDocumentId"))
         {
            update.withValue(field, old.getValue(field));
         }
         SystemWrites.update("edi_transaction", update);
      }
   }

}
