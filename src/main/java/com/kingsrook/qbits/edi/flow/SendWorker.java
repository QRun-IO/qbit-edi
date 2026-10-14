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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.EdiSendResult;
import com.kingsrook.qbits.edi.transport.EdiTransportPluginsMetaData;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Durable at-least-once sender. A database row lock fences concurrent senders;
 ** transport timeouts bound each attempt. Stable bytes/ids make retries detectable.
 *******************************************************************************/
public class SendWorker
{
   public static final List<Long> BACKOFF = List.of(60L, 300L, 900L, 3600L, 10800L, 21600L, 43200L, 86400L);

   /*******************************************************************************
    ** Process only queued or due retry rows, including inbound-generated replies.
    *******************************************************************************/
   public Integer runOnce() throws QException
   {
      return (SystemWrites.call(() ->
      {
         int count = 0;
         var due = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withCriteria("nextAttemptAt", QCriteriaOperator.IS_BLANK)
            .withCriteria("nextAttemptAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, now());
         for(QRecord message : EdiRecords.query("edi_outbound_message", new QQueryFilter().withCriteria("status", QCriteriaOperator.IN, "QUEUED", "RETRY_WAIT").withSubFilter(due).withLimit(20)))
         {
            count += send(message);
         }
         return (count);
      }));
   }

   /*******************************************************************************
    ** Compare selected status and attempts under lock before performing remote I/O.
    *******************************************************************************/
   private Integer send(QRecord selected) throws QException
   {
      return (SystemWrites.transaction(() ->
      {
         if(!SystemWrites.guard("UPDATE edi_outbound_message SET id=id WHERE id=? AND status=? AND COALESCE(attempts,0)=?", selected.getValue("id"), selected.getValue("status"), Objects.requireNonNullElse(selected.getValueInteger("attempts"), 0)))
         {
            return (0);
         }
         QRecord message = OutboundWorker.row("edi_outbound_message", selected.getValue("id"));
         int attempts = Objects.requireNonNullElse(message.getValueInteger("attempts"), 0) + 1;
         int maximum = 8;
         EdiSendResult result;
         try
         {
            QRecord transport = OutboundWorker.row("edi_transport", message.getValue("transportId"));
            maximum = Objects.requireNonNullElse(transport.getValueInteger("maxAttempts"), 8);
            if(!Boolean.TRUE.equals(transport.getValueBoolean("isActive")) || "INBOUND".equals(transport.getValueString("direction")))
            {
               result = new EdiSendResult("FAILED", null, "Outbound transport is disabled");
            }
            else
            {
               result = EdiTransportPluginsMetaData.current().get(transport.getValueString("type")).send(message.toEntity(EdiOutboundMessage.class),
                  new PayloadRef(message.getValueString("payloadRef"), message.getValueString("payloadSha256"), message.getValueLong("sizeBytes")), transport.toEntity(EdiTransport.class));
            }
         }
         catch(Exception e)
         {
            result = new EdiSendResult("RETRY", null, "Transport attempt failed");
         }
         boolean sent = List.of("SENT", "DELIVERED").contains(result.status());
         String status = sent ? result.status() : "FAILED".equals(result.status()) ? "FAILED" : attempts >= maximum ? "DEAD_LETTER" : "RETRY_WAIT";
         QRecord update = new QRecord().withValue("id", message.getValue("id")).withValue("status", status).withValue("attempts", attempts)
            .withValue("claimedAt", null).withValue("lastError", sent ? null : failureText(result))
            .withValue("nextAttemptAt", "RETRY_WAIT".equals(status) ? now().plusSeconds(BACKOFF.get(Math.min(attempts - 1, BACKOFF.size() - 1))) : null);
         if(sent)
         {
            update.withValue("transportMessageId", result.messageId()).withValue("sentAt", now());
            if("DELIVERED".equals(status))
            {
               update.withValue("deliveredAt", now());
            }
         }
         SystemWrites.update("edi_outbound_message", update);
         Integer interchangeId = message.getValueInteger("interchangeId");
         if(interchangeId != null && sent)
         {
            SystemWrites.update("edi_interchange", new QRecord().withValue("id", interchangeId).withValue("status", status));
            for(QRecord group : EdiRecords.query("edi_functional_group", EdiRecords.equal("interchangeId", interchangeId)))
            {
               for(QRecord transaction : EdiRecords.query("edi_transaction", EdiRecords.equal("functionalGroupId", group.getValue("id"))))
               {
                  if("QUEUED".equals(transaction.getValueString("status")))
                  {
                     SystemWrites.update("edi_transaction", new QRecord().withValue("id", transaction.getValue("id")).withValue("status", "SENT"));
                  }
                  if("PENDING".equals(group.getValueString("ackStatus")) && group.getValue("ackDueAt") == null)
                  {
                     QRecord document = OutboundWorker.row("edi_agreement_document", transaction.getValue("agreementDocumentId"));
                     SystemWrites.update("edi_functional_group", new QRecord().withValue("id", group.getValue("id"))
                        .withValue("ackDueAt", now().plusSeconds(60L * Objects.requireNonNullElse(document.getValueInteger("ackDueMinutes"), 1440))));
                  }
               }
            }
         }
         if(List.of("FAILED", "DEAD_LETTER").contains(status))
         {
            SystemWrites.insert("edi_exception", new QRecord().withValue("outboundMessageId", message.getValue("id")).withValue("interchangeId", interchangeId)
               .withValue("partnerId", message.getValue("partnerId")).withValue("type", "TRANSPORT").withValue("status", "OPEN").withValue("occurrences", 1).withValue("summary", "Outbound delivery failed"));
            QAlerts.raise(new AlertSignal("edi.send.failed").withSeverity(AlertSeverity.ERROR).withDedupKey("edi_outbound_message:" + message.getValue("id"))
               .withTitle("EDI delivery failed").withSummary("Review the outbound message"));
         }
         EdiJournal.event(null, interchangeId, null, status);
         return (1);
      }));
   }

   /*******************************************************************************
    ** The recorded failure: the transport reason (value-free by contract, for
    ** example an HTTP status, an AS2 disposition or a missing variable name), or a
    ** generic text.
    *******************************************************************************/
   private static String failureText(EdiSendResult result)
   {
      String reason = result.reason();
      return (reason == null || reason.isBlank() ? "Transport delivery failed" : reason.length() <= 255 ? reason : reason.substring(0, 255));
   }

   /*******************************************************************************
    ** Clock controlled by the host and deterministic tests.
    *******************************************************************************/
   private static Instant now()
   {
      return (EdiRuntime.current().config().getClock().instant());
   }
}
