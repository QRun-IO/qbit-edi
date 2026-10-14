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
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.storage.PayloadStore;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Store first, then acknowledge durable intake. Never parse in the receive path.
 *******************************************************************************/
public class InboundSink
{
   private final PayloadStore store;

   /*******************************************************************************
    ** Configure the storage boundary.
    *******************************************************************************/
   public InboundSink(PayloadStore store)
   {
      this.store = store;
   }

   /*******************************************************************************
    ** Unique external ids survive retries; plaintext digests catch byte duplicates.
    *******************************************************************************/
   public QRecord receive(InboundPayload payload) throws Exception
   {
      if(payload.transportType() == null || payload.externalId() == null || payload.externalId().isBlank())
      {
         throw new IllegalArgumentException("Transport and external id are required");
      }
      return (SystemWrites.call(() ->
      {
         QQueryFilter key = EdiRecords.equal("transportType", payload.transportType()).withCriteria("externalId", QCriteriaOperator.EQUALS, payload.externalId());
         List<QRecord> existing = EdiRecords.query("edi_inbox", key);
         if(!existing.isEmpty())
         {
            return (existing.getFirst());
         }
         try
         {
            return (SystemWrites.transaction(() ->
            {
               PayloadRef ref = store.put(PayloadKind.INBOUND, payload.stream());
               Instant now = EdiRuntime.current().config().getClock().instant();
               boolean duplicate = !EdiRecords.query("edi_inbox", EdiRecords.equal("payloadSha256", ref.sha256())
                  .withCriteria("receivedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(365, ChronoUnit.DAYS))).isEmpty();
               QRecord row = SystemWrites.insert("edi_inbox", new QRecord().withValue("transportType", payload.transportType()).withValue("externalId", payload.externalId())
                  .withValue("fileName", payload.fileName()).withValue("remoteAddress", payload.remoteAddress()).withValue("gatewayUserId", payload.gatewayUserId())
                  .withValue("username", payload.username()).withValue("payloadRef", ref.value()).withValue("payloadSha256", ref.sha256()).withValue("sizeBytes", ref.sizeBytes())
                  .withValue("receivedAt", now).withValue("status", duplicate ? "DUPLICATE" : "PENDING").withValue("attempts", 0).withValue("nextAttemptAt", now));
               EdiJournal.event(row.getValueInteger("id"), null, null, "RECEIVED");
               EdiJournal.event(row.getValueInteger("id"), null, null, "STORED");
               return (row);
            }));
         }
         catch(Exception e)
         {
            // A competing receiver may have committed the unique external id.
            existing = EdiRecords.query("edi_inbox", key);
            if(!existing.isEmpty())
            {
               return (existing.getFirst());
            }
            throw e;
         }
      }));
   }
}
