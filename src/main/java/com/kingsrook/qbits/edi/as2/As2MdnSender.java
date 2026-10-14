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

package com.kingsrook.qbits.edi.as2;


import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.flow.SendWorker;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Delivers the asynchronous MDNs we owe partners. The edi_as2_mdn row is the
 ** queue entry (deliveryStatus PENDING): a scheduled step posts the stored MDN to
 ** the address the partner gave in Receipt-Delivery-Option, retries with the send
 ** backoff, and fails (with an alert) after the transport maxAttempts. Row locks
 ** fence concurrent senders.
 *******************************************************************************/
public class As2MdnSender
{
   private static final QLogger LOG = QLogger.getLogger(As2MdnSender.class);



   /*******************************************************************************
    ** Deliver pending MDNs that are due; returns how many were attempted.
    *******************************************************************************/
   public Integer runOnce() throws QException
   {
      return (SystemWrites.call(() ->
      {
         int count = 0;
         var due = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withCriteria("nextAttemptAt", QCriteriaOperator.IS_BLANK)
            .withCriteria("nextAttemptAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, EdiRuntime.current().config().getClock().instant());
         for(QRecord row : EdiRecords.query(As2Store.TABLE, new QQueryFilter().withCriteria("deliveryStatus", QCriteriaOperator.EQUALS, "PENDING").withSubFilter(due).withLimit(20)))
         {
            count += deliver(row);
         }
         return (count);
      }));
   }



   /*******************************************************************************
    ** One delivery attempt, fenced by a row lock on the selected state.
    *******************************************************************************/
   private Integer deliver(QRecord selected) throws QException
   {
      return (SystemWrites.transaction(() ->
      {
         if(!SystemWrites.guard("UPDATE edi_as2_mdn SET id=id WHERE id=? AND delivery_status='PENDING' AND COALESCE(delivery_attempts,0)=?", selected.getValue("id"),
            Objects.requireNonNullElse(selected.getValueInteger("deliveryAttempts"), 0)))
         {
            return (0);
         }
         QRecord row = OutboundWorker.row(As2Store.TABLE, selected.getValue("id"));
         EdiTransport transport = OutboundWorker.row("edi_transport", row.getValue("transportId")).toEntity(EdiTransport.class);
         int attempts = Objects.requireNonNullElse(row.getValueInteger("deliveryAttempts"), 0) + 1;
         boolean delivered = post(row, transport);
         int maximum = Objects.requireNonNullElse(transport.getMaxAttempts(), 8);
         Instant now = EdiRuntime.current().config().getClock().instant();
         String status = delivered ? "SENT" : attempts >= maximum ? "FAILED" : "PENDING";
         QRecord update = new QRecord().withValue("id", row.getValue("id")).withValue("deliveryStatus", status).withValue("deliveryAttempts", attempts)
            .withValue("nextAttemptAt", "PENDING".equals(status) ? now.plusSeconds(SendWorker.BACKOFF.get(Math.min(attempts - 1, SendWorker.BACKOFF.size() - 1))) : null);
         if(delivered)
         {
            update.withValue("deliveredAt", now);
         }
         SystemWrites.update(As2Store.TABLE, update);
         if("FAILED".equals(status))
         {
            QAlerts.raise(new AlertSignal("edi.send.failed").withSeverity(AlertSeverity.ERROR).withDedupKey("edi_as2_mdn:" + row.getValue("id"))
               .withTitle("AS2 receipt delivery failed").withSummary("Review the AS2 receipt"));
         }
         return (1);
      }));
   }



   /*******************************************************************************
    ** POST the stored MDN to the partner receipt address; true on any 2xx.
    *******************************************************************************/
   private boolean post(QRecord row, EdiTransport transport)
   {
      try
      {
         byte[] raw;
         try(InputStream stream = EdiRuntime.current().payloads().open(new PayloadRef(row.getValueString("payloadRef"), null, 0)))
         {
            raw = stream.readAllBytes();
         }
         As2Crypto.Wire wire = As2Crypto.splitRaw(raw);
         Map<String, String> headers = new LinkedHashMap<>();
         headers.put("AS2-Version", "1.2");
         headers.put("AS2-From", As2Transport.asId(transport.getAs2LocalId()));
         headers.put("AS2-To", As2Transport.asId(transport.getAs2PartnerId()));
         headers.put("Message-ID", "<" + row.getValueString("messageId") + ">");
         headers.put("MIME-Version", "1.0");
         headers.put("Content-Type", wire.contentType());
         if(wire.transferEncoding() != null)
         {
            headers.put("Content-Transfer-Encoding", wire.transferEncoding());
         }
         if(transport.getHttpAuthUserEnvVar() != null)
         {
            var secrets = EdiRuntime.current().config().getSecretsResolver();
            headers.put("Authorization", As2Http.basic(secrets.resolve(transport.getHttpAuthUserEnvVar()), secrets.resolve(transport.getHttpAuthPasswordEnvVar())));
         }
         HttpClient client = As2Http.client(null);
         int status = As2Http.post(client, URI.create(row.getValueString("deliveryUrl")), headers, wire.body(), Duration.ofSeconds(60)).status();
         return (status >= 200 && status < 300);
      }
      catch(Exception e)
      {
         LOG.warn("AS2 receipt delivery attempt failed for receipt " + row.getValue("id"));
         return (false);
      }
   }
}
