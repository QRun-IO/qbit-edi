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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.ack.AckEnvelope;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.guide.Guide;
import com.kingsrook.qbits.edi.guide.GuideRepository;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.map.InboundApplier;
import com.kingsrook.qbits.edi.map.InboundMapper;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.storage.PayloadStore;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Routing, validation, acknowledgements and service-session application checkpoints.
 *******************************************************************************/
public class InboundProcessor
{
   private final QRecord inbox;
   private final Instant lease;
   private final PayloadStore store;
   private final EdiRuntime runtime = EdiRuntime.current();

   /*******************************************************************************
    ** One processor per claimed payload; database checkpoints fence every side effect.
    *******************************************************************************/
   public InboundProcessor(QRecord inbox, Instant lease, PayloadStore store)
   {
      this.inbox = inbox;
      this.lease = lease;
      this.store = store;
   }

   /*******************************************************************************
    ** Route before mapping; unknown and mismatched identities never reach target writes.
    *******************************************************************************/
   public void process(X12Interchange x12, int position) throws Exception
   {
      QRecord agreement = route(x12);
      QRecord interchange = InboxWorker.fenced(inbox, lease, () -> interchange(x12, agreement, position));
      Integer id = interchange.getValueInteger("id");
      if(agreement == null)
      {
         problem(id, null, null, "UNKNOWN_PARTNER", "No active agreement matches the interchange identities and usage");
         return;
      }
      Integer partner = agreement.getValueInteger("partnerId");
      if(inbox.getValueString("username") != null)
      {
         var linked = EdiRecords.query("edi_partner", EdiRecords.equal("gatewayUsername", inbox.getValue("username")));
         if(linked.stream().anyMatch(p -> !partner.equals(p.getValueInteger("id"))))
         {
            problem(id, null, partner, "PARTNER_MISMATCH", "Gateway identity does not match the routed trading partner");
            return;
         }
      }
      String code = x12.findings().stream().filter(f -> "TA1".equals(f.level())).map(f -> f.code()).findFirst().orElse("000");
      boolean duplicate = "DUPLICATE".equals(interchange.getValueString("status"));
      if(duplicate)
      {
         code = "025";
         problem(id, null, partner, "DUPLICATE", "Duplicate interchange control number");
      }
      boolean ackInput = x12.serviceSegments().stream().anyMatch(s -> "TA1".equals(s.id())) || x12.groups().stream().anyMatch(g -> !g.transactions().isEmpty()) && x12.groups().stream().flatMap(g -> g.transactions().stream()).allMatch(t -> List.of("997", "999").contains(t.st().value(1)));
      String ta1Code = code;
      String policy = agreement.getValueString("sendTa1");
      boolean send = !ackInput && ("ALWAYS".equals(policy) || "ON_REQUEST".equals(policy) && "1".equals(x12.isa().value(14))
         || "ON_ERROR".equals(policy) && !"000".equals(code));
      if(duplicate && "REJECT_TA1".equals(agreement.getValueString("onDuplicate")))
      {
         send = true;
      }
      if(send)
      {
         InboxWorker.fenced(inbox, lease, () ->
         {
            if(EdiRecords.query("edi_outbound_message", EdiRecords.equal("ackForInterchangeId", id)).isEmpty())
            {
               queue(runtime.ta1().interchange(x12, control(agreement, "ISA"), ta1Code, LocalDateTime.now(runtime.config().getClock())), agreement, id, null, "TA1");
            }
            return (null);
         });
      }
      if(duplicate && !"PROCESS".equals(agreement.getValueString("onDuplicate")))
      {
         return;
      }
      if(!"000".equals(code) && !duplicate)
      {
         problem(id, null, partner, "ENVELOPE", "Interchange envelope failed validation");
         return;
      }
      int groupPosition = 0;
      for(X12FunctionalGroup group : x12.groups())
      {
         int currentGroupPosition = ++groupPosition;
         QRecord groupRow = InboxWorker.fenced(inbox, lease, () -> group(group, interchange, agreement, currentGroupPosition));
         Map<String, List<ValidationFinding>> findings = new LinkedHashMap<>();
         QRecord ackDocument = null;
         int transactionPosition = 0;
         for(X12TransactionSet transaction : group.transactions())
         {
            QRecord document = document(agreement, transaction, group);
            if(document != null && !"NONE".equals(document.getValueString("ackExpectation")))
            {
               ackDocument = document;
            }
            QRecord configuredDocument = document;
            int currentTransactionPosition = ++transactionPosition;
            QRecord tx = InboxWorker.fenced(inbox, lease, () -> transaction(transaction, groupRow, interchange, configuredDocument, currentTransactionPosition));
            if(List.of("997", "999").contains(transaction.st().value(1)))
            {
               continue;
            }
            document = revisions(document, tx);
            QRecord effectiveDocument = document;
            List<ValidationFinding> result = validate(transaction, group, document, groupRow);
            findings.put(transaction.st().value(2), result);
            InboxWorker.fenced(inbox, lease, () ->
            {
               validation(tx, result, effectiveDocument);
               return (null);
            });
            if(document == null)
            {
               problem(id, tx.getValueInteger("id"), partner, "NOT_CONFIGURED", "Inbound document has no active agreement configuration");
               continue;
            }
            if(result.stream().anyMatch(f -> "ERROR".equals(f.severity()) && f.snipLevel() <= 2))
            {
               continue;
            }
            if("T".equals(x12.isa().value(15)) && !Boolean.TRUE.equals(agreement.getValueBoolean("applyTestDocuments")))
            {
               continue;
            }
            apply(transaction, tx, document, agreement);
         }
         if(!ackInput && ackDocument != null)
         {
            QRecord config = ackDocument;
            InboxWorker.fenced(inbox, lease, () ->
            {
               if(EdiRecords.query("edi_outbound_message", EdiRecords.equal("ackForGroupId", groupRow.getValue("id"))).isEmpty())
               {
                  AckLevel level = AckLevel.valueOf(Objects.requireNonNullElse(config.getValueString("ackLevel"), "ERRORS_ONLY"));
                  boolean hipaa = Boolean.TRUE.equals(config.getValueBoolean("containsPhi")) && group.gs().value(8).startsWith("005010X");
                  var ack = hipaa ? runtime.ack999().build(group, findings, level, "0001") : runtime.ack997().build(group, findings, level, "0001");
                  queue(AckEnvelope.wrap(x12, group, ack, control(agreement, "ISA"), control(agreement, "GS"), LocalDateTime.now(runtime.config().getClock())),
                     agreement, id, groupRow.getValueInteger("id"), hipaa ? "999" : "997");
               }
               return (null);
            });
         }
      }
      if(ackInput)
      {
         InboxWorker.fenced(inbox, lease, () ->
         {
            if(x12.serviceSegments().stream().anyMatch(s -> "TA1".equals(s.id()))
               && EdiRecords.query("edi_transaction", EdiRecords.equal("interchangeId", id)).isEmpty())
            {
               SystemWrites.insert("edi_transaction", new QRecord().withValue("interchangeId", id).withValue("partnerId", partner)
                  .withValue("direction", "INBOUND").withValue("transactionSet", "TA1").withValue("controlNumber", x12.isa().value(13)).withValue("status", "RECEIVED"));
            }
            reconcile(x12, agreement, id);
            return (null);
         });
      }
   }

   /*******************************************************************************
    ** Route on sender, receiver, qualifier, standard and test/production indicator.
    *******************************************************************************/
   private QRecord route(X12Interchange x12) throws Exception
   {
      for(QRecord agreement : EdiRecords.query("edi_agreement", EdiRecords.equal("isActive", true).withCriteria("standard", QCriteriaOperator.EQUALS, "X12")
         .withCriteria("usageIndicator", QCriteriaOperator.EQUALS, x12.isa().value(15))))
      {
         QRecord sender = row("edi_identity", agreement.getValueInteger("partnerIdentityId"));
         QRecord receiver = row("edi_identity", agreement.getValueInteger("localIdentityId"));
         if(sender != null && receiver != null && Objects.equals(sender.getValueInteger("partnerId"), agreement.getValueInteger("partnerId"))
            && receiver.getValue("partnerId") == null && x12.isa().value(5).equals(sender.getValueString("interchangeQualifier"))
            && x12.isa().value(6).trim().equals(sender.getValueString("interchangeId")) && x12.isa().value(7).equals(receiver.getValueString("interchangeQualifier"))
            && x12.isa().value(8).trim().equals(receiver.getValueString("interchangeId")))
         {
            return (agreement);
         }
      }
      return (null);
   }

   /*******************************************************************************
    ** Persist each interchange once; serialize duplicate checks on its agreement.
    *******************************************************************************/
   private QRecord interchange(X12Interchange x12, QRecord agreement, int position) throws Exception
   {
      var prior = EdiRecords.query("edi_interchange", EdiRecords.equal("inboxId", inbox.getValue("id")).withCriteria("inboundPosition", QCriteriaOperator.EQUALS, position));
      if(!prior.isEmpty())
      {
         QRecord existing = prior.getFirst();
         if(agreement != null && existing.getValue("agreementId") == null)
         {
            existing.withValue("agreementId", agreement.getValue("id")).withValue("partnerId", agreement.getValue("partnerId"));
            SystemWrites.update("edi_interchange", existing);
            SystemWrites.update("edi_inbox", new QRecord().withValue("id", inbox.getValue("id")).withValue("partnerId", agreement.getValue("partnerId")));
         }
         return (existing);
      }
      boolean duplicate = false;
      if(agreement != null)
      {
         SystemWrites.guard("UPDATE edi_agreement SET id=id WHERE id=?", agreement.getValue("id"));
         duplicate = runtime.duplicates().interchange(agreement.getValueInteger("id"), x12.isa(), agreement.getValueInteger("duplicateWindowDays"), runtime.config().getClock().instant());
         SystemWrites.update("edi_inbox", new QRecord().withValue("id", inbox.getValue("id")).withValue("partnerId", agreement.getValue("partnerId")));
      }
      QRecord row = SystemWrites.insert("edi_interchange", new QRecord().withValue("inboundPosition", position).withValue("inboxId", inbox.getValue("id")).withValue("direction", "INBOUND")
         .withValue("agreementId", agreement == null ? null : agreement.getValue("id")).withValue("partnerId", agreement == null ? null : agreement.getValue("partnerId"))
         .withValue("standard", "X12").withValue("senderQualifier", x12.isa().value(5)).withValue("senderId", x12.isa().value(6).trim())
         .withValue("receiverQualifier", x12.isa().value(7)).withValue("receiverId", x12.isa().value(8).trim()).withValue("controlNumber", x12.isa().value(13))
         .withValue("interchangeVersion", x12.isa().value(12)).withValue("usageIndicator", x12.isa().value(15)).withValue("groupCount", x12.groups().size())
         .withValue("payloadRef", inbox.getValue("payloadRef")).withValue("payloadSha256", inbox.getValue("payloadSha256")).withValue("sizeBytes", inbox.getValue("sizeBytes"))
         .withValue("status", duplicate ? "DUPLICATE" : x12.findings().isEmpty() ? "ACCEPTED" : "REJECTED"));
      EdiJournal.event(inbox.getValueInteger("id"), row.getValueInteger("id"), null, "PARSED");
      return (row);
   }

   /*******************************************************************************
    ** Group checkpoints retain sender and control duplicate evidence.
    *******************************************************************************/
   private QRecord group(X12FunctionalGroup group, QRecord interchange, QRecord agreement, int position) throws Exception
   {
      QQueryFilter filter = EdiRecords.equal("interchangeId", interchange.getValue("id")).withCriteria("inboundPosition", QCriteriaOperator.EQUALS, position);
      var prior = EdiRecords.query("edi_functional_group", filter);
      if(!prior.isEmpty())
      {
         return (prior.getFirst());
      }
      boolean duplicate = runtime.duplicates().group(group.gs().value(2), group.gs().value(6), agreement.getValueInteger("duplicateWindowDays"), runtime.config().getClock().instant());
      return (SystemWrites.insert("edi_functional_group", new QRecord().withValue("inboundPosition", position).withValue("interchangeId", interchange.getValue("id")).withValue("senderId", group.gs().value(2))
         .withValue("functionalIdCode", group.gs().value(1)).withValue("controlNumber", group.gs().value(6)).withValue("version", group.gs().value(8))
         .withValue("transactionCount", group.transactions().size()).withValue("ackStatus", duplicate && !"PROCESS".equals(agreement.getValueString("onDuplicate")) ? "R" : "NONE")));
   }

   /*******************************************************************************
    ** Select the inbound document; outbound ack expectation never controls our reply.
    *******************************************************************************/
   private QRecord document(QRecord agreement, X12TransactionSet tx, X12FunctionalGroup group) throws Exception
   {
      var rows = EdiRecords.query("edi_agreement_document", EdiRecords.equal("agreementId", agreement.getValue("id"))
         .withCriteria("direction", QCriteriaOperator.EQUALS, "INBOUND").withCriteria("transactionSet", QCriteriaOperator.EQUALS, tx.st().value(1))
         .withCriteria("version", QCriteriaOperator.EQUALS, group.gs().value(8)).withCriteria("isActive", QCriteriaOperator.EQUALS, true));
      return (rows.isEmpty() ? null : rows.getFirst());
   }

   /*******************************************************************************
    ** Transaction checkpoints preserve guide and map references across worker retries.
    *******************************************************************************/
   private QRecord transaction(X12TransactionSet tx, QRecord group, QRecord interchange, QRecord document, int position) throws Exception
   {
      var rows = EdiRecords.query("edi_transaction", EdiRecords.equal("functionalGroupId", group.getValue("id")).withCriteria("inboundPosition", QCriteriaOperator.EQUALS, position));
      if(!rows.isEmpty())
      {
         return (rows.getFirst());
      }
      return (SystemWrites.insert("edi_transaction", new QRecord().withValue("inboundPosition", position).withValue("interchangeId", interchange.getValue("id")).withValue("functionalGroupId", group.getValue("id"))
         .withValue("direction", "INBOUND").withValue("partnerId", interchange.getValue("partnerId")).withValue("transactionSet", tx.st().value(1))
         .withValue("version", group.getValue("version")).withValue("controlNumber", tx.st().value(2)).withValue("segmentCount", tx.segments().size())
         .withValue("agreementDocumentId", document == null ? null : document.getValue("id")).withValue("containsPhi", document != null && Boolean.TRUE.equals(document.getValueBoolean("containsPhi")))
         .withValue("status", "RECEIVED")));
   }

   /*******************************************************************************
    ** Replays select original persisted definitions or the currently active revision.
    *******************************************************************************/
   private QRecord revisions(QRecord document, QRecord transaction) throws Exception
   {
      if(document == null)
      {
         return (null);
      }
      QRecord effective = new QRecord();
      effective.setValues(new java.util.HashMap<>(document.getValues()));
      for(String kind : List.of("guide", "map"))
      {
         Integer originalId = transaction.getValueInteger(kind + "Id");
         boolean current = Boolean.TRUE.equals(inbox.getValueBoolean("useCurrentRevisions"));
         if(originalId != null && !current)
         {
            effective.withValue(kind + "Id", originalId);
         }
         else if(current)
         {
            QRecord referenced = row("edi_" + kind, document.getValueInteger(kind + "Id"));
            if(referenced != null)
            {
               var active = EdiRecords.query("edi_" + kind, EdiRecords.equal("name", referenced.getValue("name")).withCriteria("status", QCriteriaOperator.EQUALS, "ACTIVE"));
               active.stream().filter(r -> Objects.equals(r.getValue("partnerId"), referenced.getValue("partnerId"))).findFirst()
                  .ifPresent(r -> effective.withValue(kind + "Id", r.getValue("id")));
            }
         }
      }
      return (effective);
   }

   /*******************************************************************************
    ** Gather positional findings, including group and repeated transaction controls.
    *******************************************************************************/
   private List<ValidationFinding> validate(X12TransactionSet tx, X12FunctionalGroup group, QRecord document, QRecord groupRow) throws Exception
   {
      List<ValidationFinding> findings = new ArrayList<>();
      if(document != null && document.getValueInteger("guideId") != null)
      {
         findings.addAll(runtime.validator().validate(tx, new GuideRepository().load(document.getValueInteger("guideId")), Boolean.TRUE.equals(document.getValueBoolean("containsPhi"))));
         int snip = Objects.requireNonNullElse(document.getValueInteger("snipLevel"), 2);
         findings.removeIf(f -> f.snipLevel() > snip);
      }
      else
      {
         findings.add(finding("No implementation guide is configured", "3"));
      }
      if("R".equals(groupRow.getValueString("ackStatus")) || runtime.duplicates().transactions(group).contains(tx.st().value(2)))
      {
         findings.add(finding("Duplicate control number", "7"));
      }
      if(!group.findings().isEmpty() || !tx.findings().isEmpty())
      {
         findings.add(finding("Envelope counts or controls failed validation", "7"));
      }
      return (findings);
   }

   /*******************************************************************************
    ** Construct a value-free structural error.
    *******************************************************************************/
   private static ValidationFinding finding(String message, String code)
   {
      return (new ValidationFinding(1, "ST", null, null, null, 1, code, null, code, null, "ERROR", message, null));
   }

   /*******************************************************************************
    ** Persist findings without repeatedly adding error rows on storage retries.
    *******************************************************************************/
   private void validation(QRecord tx, List<ValidationFinding> findings, QRecord document) throws Exception
   {
      boolean rejected = findings.stream().anyMatch(f -> "ERROR".equals(f.severity()) && f.snipLevel() <= 2);
      if(!"APPLIED".equals(tx.getValueString("status")) || Boolean.TRUE.equals(inbox.getValueBoolean("reprocess")))
      {
         QRecord guide = document == null ? null : row("edi_guide", document.getValueInteger("guideId"));
         SystemWrites.update("edi_transaction", new QRecord().withValue("id", tx.getValue("id")).withValue("status", rejected ? "INVALID" : "VALID")
            .withValue("ackStatus", rejected ? "R" : findings.isEmpty() ? "A" : "E").withValue("errorCount", (int) findings.stream().filter(f -> "ERROR".equals(f.severity())).count())
            .withValue("warningCount", (int) findings.stream().filter(f -> !"ERROR".equals(f.severity())).count())
            .withValue("guideId", guide == null ? null : guide.getValue("id")).withValue("guideRevision", guide == null ? null : guide.getValue("revision"))
            .withValue("mapId", document == null ? null : document.getValue("mapId"))
            .withValue("mapRevision", document == null || row("edi_map", document.getValueInteger("mapId")) == null ? null : row("edi_map", document.getValueInteger("mapId")).getValue("revision")));
      }
      if(Boolean.TRUE.equals(inbox.getValueBoolean("reprocess")))
      {
         new com.kingsrook.qqq.backend.core.actions.tables.DeleteAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput("edi_transaction_error")
            .withQueryFilter(EdiRecords.equal("transactionId", tx.getValue("id"))).withTransaction(com.kingsrook.qqq.backend.core.context.QContext.getQBackendTransaction()));
      }
      if(EdiRecords.query("edi_transaction_error", EdiRecords.equal("transactionId", tx.getValue("id"))).isEmpty())
      {
         for(ValidationFinding f : findings)
         {
            SystemWrites.insert("edi_transaction_error", new QRecord().withValue("transactionId", tx.getValue("id")).withValue("segmentPosition", f.segmentPosition())
               .withValue("segmentId", f.segmentId()).withValue("loopId", f.loopId()).withValue("elementPosition", f.elementPosition()).withValue("componentPosition", f.componentPosition())
               .withValue("snipLevel", f.snipLevel()).withValue("ackCode", f.ak4Code() == null ? f.ak3Code() : f.ak4Code()).withValue("severity", f.severity())
               .withValue("message", f.message()).withValue("valueExcerpt", Boolean.TRUE.equals(tx.getValueBoolean("containsPhi")) ? null : f.valueExcerpt()));
         }
      }
      EdiJournal.event(inbox.getValueInteger("id"), tx.getValueInteger("interchangeId"), tx.getValueInteger("id"), "VALIDATED");
      if(!findings.isEmpty())
      {
         EdiJournal.exception(inbox.getValueInteger("id"), tx.getValueInteger("interchangeId"), tx.getValueInteger("id"), tx.getValueInteger("partnerId"), "VALIDATION", "Document validation has findings");
      }
   }

   /*******************************************************************************
    ** A failed target write rolls back only this set, then records an APPLY exception.
    *******************************************************************************/
   private void apply(X12TransactionSet x12, QRecord tx, QRecord document, QRecord agreement) throws Exception
   {
      if("APPLIED".equals(tx.getValueString("status")) && !Boolean.TRUE.equals(inbox.getValueBoolean("reprocess")))
      {
         return;
      }
      QRecord map = row("edi_map", document.getValueInteger("mapId"));
      if(map == null || !("ACTIVE".equals(map.getValueString("status")) || Boolean.TRUE.equals(inbox.getValueBoolean("reprocess")) && !Boolean.TRUE.equals(inbox.getValueBoolean("useCurrentRevisions"))))
      {
         return;
      }
      String[] stage = {"MAPPING"};
      try
      {
         InboxWorker.fenced(inbox, lease, () ->
         {
            Guide guide = new GuideRepository().load(document.getValueInteger("guideId"));
            List<QRecord> rules = EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id")));
            rules.sort(java.util.Comparator.comparing(r -> r.getValueInteger("sequence")));
            if(runtime.config().getServiceSessionSupplier() == null)
            {
               stage[0] = "APPLY";
            }
            InboundMapper.Result mapped = InboundApplier.asPartner(runtime.config().getServiceSessionSupplier(), agreement.getValueInteger("partnerId"), () -> new InboundMapper(runtime.transforms()).map(x12, guide, map, rules));
            stage[0] = "APPLY";
            QRecord target = InboundApplier.asPartner(runtime.config().getServiceSessionSupplier(), agreement.getValueInteger("partnerId"), () -> new InboundApplier().apply(map, mapped.record()));
            String primaryKey = com.kingsrook.qqq.backend.core.context.QContext.getQInstance().getTable(map.getValueString("tableName")).getPrimaryKeyField();
            boolean phi = Boolean.TRUE.equals(document.getValueBoolean("containsPhi"));
            SystemWrites.update("edi_transaction", new QRecord().withValue("id", tx.getValue("id")).withValue("status", "APPLIED")
               .withValue("mapId", map.getValue("id")).withValue("mapRevision", map.getValue("revision")).withValue("businessKeyType", map.getValue("businessKeyType"))
               .withValue(phi ? "phiBusinessKey" : "businessKey", mapped.businessKey()).withValue("secondaryKey", phi ? null : mapped.secondaryKey())
               .withValue("targetTableName", map.getValue("tableName")).withValue("targetRecordId", target.getValueString(primaryKey)));
            EdiJournal.event(inbox.getValueInteger("id"), tx.getValueInteger("interchangeId"), tx.getValueInteger("id"), "MAPPED");
            EdiJournal.event(inbox.getValueInteger("id"), tx.getValueInteger("interchangeId"), tx.getValueInteger("id"), "APPLIED");
            return (null);
         });
      }
      catch(Exception e)
      {
         InboxWorker.fenced(inbox, lease, () ->
         {
            SystemWrites.update("edi_transaction", new QRecord().withValue("id", tx.getValue("id")).withValue("status", "EXCEPTION"));
            EdiJournal.exception(inbox.getValueInteger("id"), tx.getValueInteger("interchangeId"), tx.getValueInteger("id"), agreement.getValueInteger("partnerId"), stage[0],
               runtime.config().getServiceSessionSupplier() == null ? "no service session configured" : "Inbound " + stage[0].toLowerCase(java.util.Locale.ROOT) + " failed");
            return (null);
         });
      }
   }

   /*******************************************************************************
    ** Allocate controls from the agreement or local identity's configured scope.
    *******************************************************************************/
   private String control(QRecord agreement, String level) throws Exception
   {
      boolean local = "LOCAL_IDENTITY".equals(agreement.getValueString("controlNumberScope"));
      return (String.valueOf(runtime.controls().allocate(local ? null : agreement.getValueInteger("id"), local ? agreement.getValueInteger("localIdentityId") : null, level, 1, 999999999, true)));
   }

   /*******************************************************************************
    ** Persist an encrypted, fully enveloped acknowledgement; sending is phase b.
    *******************************************************************************/
   private void queue(X12Interchange ack, QRecord agreement, Integer receivedId, Integer groupId, String type) throws Exception
   {
      PayloadRef payload = store.put(PayloadKind.OUTBOUND, new ByteArrayInputStream(runtime.writer().write(ack).getBytes(StandardCharsets.US_ASCII)));
      QRecord message = SystemWrites.insert("edi_outbound_message", new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("partnerId", agreement.getValue("partnerId"))
         .withValue("transportId", agreement.getValue("outboundTransportId")).withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256()).withValue("sizeBytes", payload.sizeBytes())
         .withValue("status", "QUEUED").withValue("attempts", 0).withValue("ackForGroupId", groupId).withValue("ackForInterchangeId", groupId == null ? receivedId : null));
      QRecord interchange = SystemWrites.insert("edi_interchange", new QRecord().withValue("outboundMessageId", message.getValue("id")).withValue("direction", "OUTBOUND")
         .withValue("agreementId", agreement.getValue("id")).withValue("partnerId", agreement.getValue("partnerId")).withValue("controlNumber", ack.isa().value(13))
         .withValue("senderQualifier", ack.isa().value(5)).withValue("senderId", ack.isa().value(6).trim()).withValue("receiverQualifier", ack.isa().value(7)).withValue("receiverId", ack.isa().value(8).trim())
         .withValue("standard", "X12").withValue("interchangeVersion", ack.isa().value(12)).withValue("usageIndicator", ack.isa().value(15)).withValue("groupCount", ack.groups().size())
         .withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256()).withValue("sizeBytes", payload.sizeBytes()).withValue("status", "ENVELOPED"));
      SystemWrites.update("edi_outbound_message", new QRecord().withValue("id", message.getValue("id")).withValue("interchangeId", interchange.getValue("id")));
      Integer outboundGroup = null;
      if(!ack.groups().isEmpty())
      {
         var gs = ack.groups().getFirst().gs();
         outboundGroup = SystemWrites.insert("edi_functional_group", new QRecord().withValue("interchangeId", interchange.getValue("id")).withValue("functionalIdCode", "FA")
            .withValue("senderId", gs.value(2)).withValue("controlNumber", gs.value(6)).withValue("version", gs.value(8)).withValue("transactionCount", 1).withValue("ackStatus", "NONE")).getValueInteger("id");
      }
      SystemWrites.insert("edi_transaction", new QRecord().withValue("interchangeId", interchange.getValue("id")).withValue("functionalGroupId", outboundGroup)
         .withValue("direction", "OUTBOUND").withValue("partnerId", agreement.getValue("partnerId")).withValue("transactionSet", type).withValue("status", "QUEUED")
         .withValue("controlNumber", outboundGroup == null ? ack.isa().value(13) : "0001").withValue("version", ack.groups().isEmpty() ? ack.isa().value(12) : ack.groups().getFirst().gs().value(8)));
      EdiJournal.event(inbox.getValueInteger("id"), receivedId, null, "ACK_QUEUED");
   }

   /*******************************************************************************
    ** Reconcile only outbound controls on this agreement; acknowledgements get no ack.
    *******************************************************************************/
   private void reconcile(X12Interchange x12, QRecord agreement, Integer received) throws Exception
   {
      for(var ack : runtime.acknowledgements().read(x12))
      {
         var receivedAcks = EdiRecords.query("edi_transaction", EdiRecords.equal("interchangeId", received).withCriteria("transactionSet", QCriteriaOperator.EQUALS, ack.type()));
         Integer ackTransactionId = receivedAcks.isEmpty() ? null : receivedAcks.getFirst().getValueInteger("id");
         if(ackTransactionId != null)
         {
            SystemWrites.update("edi_transaction", new QRecord().withValue("id", ackTransactionId).withValue("status", "ACKNOWLEDGED").withValue("ackStatus", ack.status()));
         }
         for(QRecord interchange : EdiRecords.query("edi_interchange", EdiRecords.equal("agreementId", agreement.getValue("id")).withCriteria("direction", QCriteriaOperator.EQUALS, "OUTBOUND")))
         {
            boolean ta1 = "TA1".equals(ack.type());
            if(ta1 && !ack.controlNumber().equals(interchange.getValueString("controlNumber")))
            {
               continue;
            }
            if(ta1)
            {
               SystemWrites.update("edi_interchange", new QRecord().withValue("id", interchange.getValue("id")).withValue("ta1Status", ack.status()));
               continue;
            }
            for(QRecord group : EdiRecords.query("edi_functional_group", EdiRecords.equal("interchangeId", interchange.getValue("id"))))
            {
               if(!ta1 && !ack.controlNumber().equals(group.getValueString("controlNumber")))
               {
                  continue;
               }
               SystemWrites.update("edi_functional_group", new QRecord().withValue("id", group.getValue("id")).withValue("ackStatus", ack.status()).withValue("ackReceivedAt", runtime.config().getClock().instant()).withValue("ackTransactionId", ackTransactionId));
               for(QRecord transaction : EdiRecords.query("edi_transaction", EdiRecords.equal("functionalGroupId", group.getValue("id"))))
               {
                  String status = ack.transactionStatuses().getOrDefault(transaction.getValueString("controlNumber"), ack.status());
                  SystemWrites.update("edi_transaction", new QRecord().withValue("id", transaction.getValue("id")).withValue("ackStatus", status)
                     .withValue("status", "R".equals(status) ? "REJECTED" : "ACKNOWLEDGED"));
               }
               QAlerts.resolve("edi.ack.overdue", "edi_functional_group:" + group.getValue("id"), "Acknowledgement received");
               for(QRecord exception : EdiRecords.query("edi_exception", EdiRecords.equal("interchangeId", interchange.getValue("id"))
                  .withCriteria("type", QCriteriaOperator.EQUALS, "OVERDUE_ACK").withCriteria("status", QCriteriaOperator.IN, "OPEN", "IN_PROGRESS")))
               {
                  SystemWrites.update("edi_exception", new QRecord().withValue("id", exception.getValue("id")).withValue("status", "RESOLVED")
                     .withValue("resolvedAt", runtime.config().getClock().instant()).withValue("resolution", "Acknowledgement received"));
               }
               if(List.of("R", "P").contains(ack.status()))
               {
                  EdiJournal.exception(inbox.getValueInteger("id"), received, null, agreement.getValueInteger("partnerId"), "REJECTED_ACK", "Partner rejected an outbound group");
                  QAlerts.raise(new AlertSignal("edi.ack.rejected").withSeverity(AlertSeverity.ERROR).withDedupKey(group.getValueString("id"))
                     .withTitle("EDI acknowledgement rejected").withSummary("Review the outbound functional group"));
               }
            }
         }
      }
      EdiJournal.event(inbox.getValueInteger("id"), received, null, "ACK_RECEIVED");
   }

   /*******************************************************************************
    ** Persist safe failures with the current claim held.
    *******************************************************************************/
   private void problem(Integer interchange, Integer transaction, Integer partner, String type, String summary) throws Exception
   {
      InboxWorker.fenced(inbox, lease, () ->
      {
         EdiJournal.exception(inbox.getValueInteger("id"), interchange, transaction, partner, type, summary);
         return (null);
      });
   }

   /*******************************************************************************
    ** Optional foreign-key lookup.
    *******************************************************************************/
   private static QRecord row(String table, Integer id) throws Exception
   {
      if(id == null)
      {
         return (null);
      }
      var rows = EdiRecords.query(table, EdiRecords.equal("id", id));
      return (rows.isEmpty() ? null : rows.getFirst());
   }
}
