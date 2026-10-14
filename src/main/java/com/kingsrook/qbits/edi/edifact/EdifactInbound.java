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

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.flow.EdiJournal;
import com.kingsrook.qbits.edi.flow.InboundProcessor;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.guide.GuideRepository;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadStore;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Native UNB routing and CONTRL with the shared revision, validation and apply checkpoints.
 *******************************************************************************/
public class EdifactInbound extends InboundProcessor
{
   private final QRecord inbox;
   private final Instant lease;

   /*******************************************************************************
    ** Bind every write to the same inbox lease used by the X12 worker.
    *******************************************************************************/
   public EdifactInbound(QRecord inbox, Instant lease, PayloadStore store)
   {
      super(inbox, lease, store);
      this.inbox = inbox;
      this.lease = lease;
   }

   /*******************************************************************************
    ** Route UNB identities before validating, acknowledging or applying messages.
    *******************************************************************************/
   public void process(EdifactInterchange input, int position) throws Exception
   {
      if(input.unb() == null)
      {
         InboxWorker.fenced(inbox, lease, () ->
         {
            EdiJournal.exception(inbox.getValueInteger("id"), null, null, null, "ENVELOPE", "Invalid EDIFACT header");
            return (null);
         });
         return;
      }
      QRecord agreement = route(input);
      QRecord interchange = InboxWorker.fenced(inbox, lease, () -> interchange(input, agreement, position));
      if(agreement == null)
      {
         problem(interchange, "UNKNOWN_PARTNER");
         return;
      }
      if(inbox.getValueString("username") != null && EdiRecords.query("edi_partner", EdiRecords.equal("gatewayUsername", inbox.getValue("username"))).stream()
         .anyMatch(p -> !p.getValue("id").equals(agreement.getValue("partnerId"))))
      {
         problem(interchange, "PARTNER_MISMATCH");
         return;
      }
      if(inbox.getValueLong("sizeBytes") != null && inbox.getValueLong("sizeBytes") > Objects.requireNonNullElse(agreement.getValueLong("maxInterchangeBytes"), 52428800L))
      {
         problem(interchange, "ENVELOPE");
         return;
      }
      boolean duplicate = "DUPLICATE".equals(interchange.getValueString("status"));
      boolean ackInput = !input.allMessages().isEmpty() && input.allMessages().stream().allMatch(m -> "CONTRL".equals(m.type()));
      if(duplicate)
      {
         problem(interchange, "DUPLICATE");
         if(!"PROCESS".equals(agreement.getValueString("onDuplicate")))
         {
            if(!ackInput && "REJECT_TA1".equals(agreement.getValueString("onDuplicate")))
            {
               var rejected = new EdifactInterchange(input.separators(), input.unb(), input.unz(), input.groups(), input.messages(),
                  List.of(new com.kingsrook.qbits.edi.x12.EnvelopeFinding("UNB", 1, "19", "Duplicate interchange")));
               acknowledge(rejected, agreement, interchange, Map.of(), AckLevel.GROUP_ONLY);
            }
            return;
         }
      }
      if(!input.findings().isEmpty())
      {
         problem(interchange, "ENVELOPE");
         if(!ackInput)
         {
            acknowledge(input, agreement, interchange, Map.of(), AckLevel.GROUP_ONLY);
         }
         return;
      }
      Map<String, List<ValidationFinding>> findings = new LinkedHashMap<>();
      AckLevel level = AckLevel.ERRORS_ONLY;
      boolean sendAck = false;
      List<EdifactGroup> batches = input.groups().isEmpty() ? List.of(new EdifactGroup(null, null, input.messages(), List.of())) : input.groups();
      int groupPosition = 0;
      for(var batch : batches)
      {
         int gp = ++groupPosition;
         QRecord group = InboxWorker.fenced(inbox, lease, () ->
         {
            var prior = EdiRecords.query("edi_functional_group", EdiRecords.equal("interchangeId", interchange.getValue("id")).withCriteria("inboundPosition", QCriteriaOperator.EQUALS, gp));
            return (prior.isEmpty() ? SystemWrites.insert("edi_functional_group", new QRecord().withValue("interchangeId", interchange.getValue("id")).withValue("inboundPosition", gp)
               .withValue("senderId", input.unb().value(2)).withValue("controlNumber", batch.ung() == null ? input.unb().value(5) : batch.ung().value(5))
               .withValue("version", batch.messages().isEmpty() ? null : batch.messages().getFirst().version())
               .withValue("functionalIdCode", batch.ung() == null ? null : batch.ung().value(1)).withValue("transactionCount", batch.messages().size()).withValue("ackStatus", "NONE")) : prior.getFirst());
         });
         int messagePosition = 0;
         for(var message : batch.messages())
         {
            QRecord document = document(agreement, message);
            int mp = ++messagePosition;
            QRecord configured = document;
            QRecord tx = InboxWorker.fenced(inbox, lease, () ->
            {
               var prior = EdiRecords.query("edi_transaction", EdiRecords.equal("functionalGroupId", group.getValue("id")).withCriteria("inboundPosition", QCriteriaOperator.EQUALS, mp));
               return (prior.isEmpty() ? SystemWrites.insert("edi_transaction", new QRecord().withValue("interchangeId", interchange.getValue("id")).withValue("functionalGroupId", group.getValue("id"))
                  .withValue("inboundPosition", mp).withValue("partnerId", agreement.getValue("partnerId")).withValue("direction", "INBOUND").withValue("transactionSet", message.type())
                  .withValue("version", message.version()).withValue("controlNumber", message.reference()).withValue("segmentCount", message.segments().size()).withValue("status", "RECEIVED")
                  .withValue("agreementDocumentId", configured == null ? null : configured.getValue("id"))
                  .withValue("containsPhi", configured != null && Boolean.TRUE.equals(configured.getValueBoolean("containsPhi")))) : prior.getFirst());
            });
            document = revisions(document, tx);
            List<ValidationFinding> result = new ArrayList<>();
            if(document == null || document.getValue("guideId") == null)
            {
               result.add(finding("No implementation guide is configured"));
               problem(interchange, "NOT_CONFIGURED");
            }
            else
            {
               result.addAll(EdiRuntime.current().validator().validate(message, new GuideRepository().load(document.getValueInteger("guideId")), Boolean.TRUE.equals(document.getValueBoolean("containsPhi"))));
            }
            findings.put(message.reference(), List.copyOf(result));
            if(!batch.findings().isEmpty() || !message.findings().isEmpty())
            {
               result.add(finding("Envelope counts or references failed validation"));
            }
            QRecord effective = document;
            InboxWorker.fenced(inbox, lease, () ->
            {
               validation(tx, result, effective);
               return (null);
            });
            if("CONTRL".equals(message.type()))
            {
               if(result.isEmpty())
               {
                  InboxWorker.fenced(inbox, lease, () ->
                  {
                     new ContrlReader().reconcile(message, agreement, tx);
                     return (null);
                  });
               }
               continue;
            }
            if(document != null && "CONTRL".equals(document.getValueString("ackExpectation")))
            {
               sendAck = true;
               AckLevel selected = AckLevel.valueOf(Objects.requireNonNullElse(document.getValueString("ackLevel"), "ERRORS_ONLY"));
               if(selected == AckLevel.ALL_TRANSACTIONS || level != AckLevel.ALL_TRANSACTIONS && selected == AckLevel.GROUP_ONLY)
               {
                  level = selected;
               }
            }
            if(document != null && result.stream().noneMatch(f -> "ERROR".equals(f.severity()))
               && (!"T".equals(input.usage()) || Boolean.TRUE.equals(agreement.getValueBoolean("applyTestDocuments"))))
            {
               apply(message, tx, document, agreement);
            }
         }
      }
      if(!ackInput && (sendAck || "1".equals(input.unb().value(9))))
      {
         acknowledge(input, agreement, interchange, findings, level);
      }
   }

   /*******************************************************************************
    ** Match qualifier, identifier, standard, usage and identity ownership exactly.
    *******************************************************************************/
   private QRecord route(EdifactInterchange input) throws Exception
   {
      for(var agreement : EdiRecords.query("edi_agreement", EdiRecords.equal("isActive", true).withCriteria("standard", QCriteriaOperator.EQUALS, "EDIFACT")
         .withCriteria("usageIndicator", QCriteriaOperator.EQUALS, input.usage())))
      {
         QRecord sender = OutboundWorker.row("edi_identity", agreement.getValue("partnerIdentityId"));
         QRecord receiver = OutboundWorker.row("edi_identity", agreement.getValue("localIdentityId"));
         if(Boolean.TRUE.equals(sender.getValueBoolean("isActive")) && Boolean.TRUE.equals(receiver.getValueBoolean("isActive"))
            && "EDIFACT".equals(sender.getValueString("standard")) && "EDIFACT".equals(receiver.getValueString("standard"))
            && Objects.equals(sender.getValue("partnerId"), agreement.getValue("partnerId")) && receiver.getValue("partnerId") == null
            && input.unb().value(2).equals(sender.getValueString("interchangeId")) && input.unb().value(3).equals(receiver.getValueString("interchangeId"))
            && input.unb().value(2, 1, 2).equals(Objects.requireNonNullElse(sender.getValueString("interchangeQualifier"), ""))
            && input.unb().value(3, 1, 2).equals(Objects.requireNonNullElse(receiver.getValueString("interchangeQualifier"), "")))
         {
            return (agreement);
         }
      }
      return (null);
   }

   /*******************************************************************************
    ** Preserve retries and serialize reference duplicate checks on the agreement.
    *******************************************************************************/
   private QRecord interchange(EdifactInterchange input, QRecord agreement, int position) throws Exception
   {
      var prior = EdiRecords.query("edi_interchange", EdiRecords.equal("inboxId", inbox.getValue("id")).withCriteria("inboundPosition", QCriteriaOperator.EQUALS, position));
      if(!prior.isEmpty())
      {
         QRecord existing = prior.getFirst();
         if(agreement != null && existing.getValue("agreementId") == null)
         {
            existing.withValue("agreementId", agreement.getValue("id")).withValue("partnerId", agreement.getValue("partnerId"));
            SystemWrites.update("edi_interchange", existing);
         }
         return (existing);
      }
      boolean duplicate = false;
      if(agreement != null)
      {
         SystemWrites.guard("UPDATE edi_agreement SET id=id WHERE id=?", agreement.getValue("id"));
         duplicate = EdiRuntime.current().duplicates().edifact(agreement.getValueInteger("id"), input, Objects.requireNonNullElse(agreement.getValueInteger("duplicateWindowDays"), 365), EdiRuntime.current().config().getClock().instant());
         SystemWrites.update("edi_inbox", new QRecord().withValue("id", inbox.getValue("id")).withValue("partnerId", agreement.getValue("partnerId")));
      }
      return (SystemWrites.insert("edi_interchange", new QRecord().withValue("inboxId", inbox.getValue("id")).withValue("inboundPosition", position).withValue("direction", "INBOUND")
         .withValue("agreementId", agreement == null ? null : agreement.getValue("id")).withValue("partnerId", agreement == null ? null : agreement.getValue("partnerId"))
         .withValue("standard", "EDIFACT").withValue("senderId", input.unb().value(2)).withValue("senderQualifier", input.unb().value(2, 1, 2))
         .withValue("receiverId", input.unb().value(3)).withValue("receiverQualifier", input.unb().value(3, 1, 2)).withValue("controlNumber", input.unb().value(5))
         .withValue("interchangeVersion", input.unb().value(1, 1, 2)).withValue("usageIndicator", input.usage()).withValue("groupCount", input.groups().size())
         .withValue("payloadRef", inbox.getValue("payloadRef")).withValue("payloadSha256", inbox.getValue("payloadSha256")).withValue("sizeBytes", inbox.getValue("sizeBytes"))
         .withValue("status", duplicate ? "DUPLICATE" : input.findings().isEmpty() ? "ACCEPTED" : "REJECTED")));
   }

   /*******************************************************************************
    ** Match message type and directory independently for every UNH.
    *******************************************************************************/
   private QRecord document(QRecord agreement, EdifactMessage message) throws Exception
   {
      var rows = EdiRecords.query("edi_agreement_document", EdiRecords.equal("agreementId", agreement.getValue("id")).withCriteria("direction", QCriteriaOperator.EQUALS, "INBOUND")
         .withCriteria("transactionSet", QCriteriaOperator.EQUALS, message.type()).withCriteria("version", QCriteriaOperator.EQUALS, message.version()).withCriteria("isActive", QCriteriaOperator.EQUALS, true));
      return (rows.isEmpty() ? null : rows.getFirst());
   }

   /*******************************************************************************
    ** Queue at most one CONTRL per received interchange, never acknowledge CONTRL.
    *******************************************************************************/
   private void acknowledge(EdifactInterchange input, QRecord agreement, QRecord received, Map<String, List<ValidationFinding>> findings, AckLevel level) throws Exception
   {
      InboxWorker.fenced(inbox, lease, () ->
      {
         if(EdiRecords.query("edi_outbound_message", EdiRecords.equal("ackForInterchangeId", received.getValue("id"))).isEmpty())
         {
            String version = input.allMessages().isEmpty() ? "D96A" : input.allMessages().getFirst().version();
            var message = new ContrlBuilder().build(input, findings, level, EdifactOutbound.allocate(agreement, "UNH"), version);
            EdifactOutbound.envelope(agreement, null, null, List.of(), List.of(message), null, received.getValueInteger("id"));
            EdiJournal.event(inbox.getValueInteger("id"), received.getValueInteger("id"), null, "ACK_QUEUED");
         }
         return (null);
      });
   }

   /*******************************************************************************
    ** Record a safe positional structural failure.
    *******************************************************************************/
   private ValidationFinding finding(String text)
   {
      return (new ValidationFinding(1, "UNH", null, null, null, 1, "3", null, "3", null, "ERROR", text, null));
   }

   /*******************************************************************************
    ** Journal failures only while holding the current inbox claim.
    *******************************************************************************/
   private void problem(QRecord interchange, String type) throws Exception
   {
      InboxWorker.fenced(inbox, lease, () ->
      {
         EdiJournal.exception(inbox.getValueInteger("id"), interchange.getValueInteger("id"), null, interchange.getValueInteger("partnerId"), type, "EDIFACT processing requires attention: " + type);
         return (null);
      });
   }
}
