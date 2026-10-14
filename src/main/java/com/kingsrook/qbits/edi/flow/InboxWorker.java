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

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.storage.PayloadStore;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Bounded durable worker with database leases and independently committed sets.
 *******************************************************************************/
public class InboxWorker
{
   public static final String DEAD_LETTER_ALERT = "edi.inbox.dead-letter";
   private static final List<Long> BACKOFF = List.of(60L, 300L, 900L, 3600L, 21600L);
   private final PayloadStore store;

   /*******************************************************************************
    ** Default runtime storage.
    *******************************************************************************/
   public InboxWorker()
   {
      this(EdiRuntime.current().payloads());
   }

   /*******************************************************************************
    ** Injectable storage boundary for transport/storage failure tests.
    *******************************************************************************/
   public InboxWorker(PayloadStore store)
   {
      this.store = store;
   }

   /*******************************************************************************
    ** Process at most twenty due messages as EDI's internal writer.
    *******************************************************************************/
   public Integer runOnce() throws QException
   {
      return (SystemWrites.call(() ->
      {
         Instant now = now();
         QQueryFilter due = EdiRecords.equal("status", "RETRY_WAIT")
            .withSubFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
               .withCriteria("nextAttemptAt", QCriteriaOperator.IS_BLANK).withCriteria("nextAttemptAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, now));
         QQueryFilter stale = EdiRecords.equal("status", "PROCESSING").withCriteria("claimedAt", QCriteriaOperator.LESS_THAN, now.minusSeconds(300));
         List<QRecord> rows = EdiRecords.query("edi_inbox", new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilter(due).withSubFilter(EdiRecords.equal("status", "PENDING")).withSubFilter(stale)
            .withOrderBy(new QFilterOrderBy("receivedAt")).withLimit(20));
         int completed = 0;
         for(QRecord row : rows)
         {
            Instant lease = now().truncatedTo(ChronoUnit.MILLIS);
            if(!claim(row, lease))
            {
               continue;
            }
            try(InputStream stream = store.open(new PayloadRef(row.getValueString("payloadRef"), row.getValueString("payloadSha256"), row.getValueLong("sizeBytes"))))
            {
               var interchanges = EdiRuntime.current().reader().read(stream);
               int position = 0;
               for(var interchange : interchanges)
               {
                  new InboundProcessor(row, lease, store).process(interchange, ++position);
               }
               fenced(row, lease, () ->
               {
                  SystemWrites.update("edi_inbox", new QRecord().withValue("id", row.getValue("id")).withValue("status", "PROCESSED")
                     .withValue("processedAt", now()).withValue("nextAttemptAt", null).withValue("lastError", null).withValue("reprocess", false));
                  return (null);
               });
               completed++;
            }
            catch(Exception e)
            {
               fail(row, lease);
            }
         }
         return (completed);
      }));
   }

   /*******************************************************************************
    ** Compare selected status and lease before claiming; only the winner proceeds.
    *******************************************************************************/
   private boolean claim(QRecord row, Instant lease) throws QException
   {
      return (SystemWrites.transaction(() ->
      {
         boolean won = row.getValue("claimedAt") == null
            ? SystemWrites.guard("UPDATE edi_inbox SET id=id WHERE id=? AND status=? AND claimed_at IS NULL", row.getValue("id"), row.getValue("status"))
            : SystemWrites.guard("UPDATE edi_inbox SET id=id WHERE id=? AND status=? AND claimed_at=?", row.getValue("id"), row.getValue("status"), row.getValueInstant("claimedAt"));
         if(won)
         {
            SystemWrites.update("edi_inbox", new QRecord().withValue("id", row.getValue("id")).withValue("status", "PROCESSING").withValue("claimedAt", lease));
         }
         return (won);
      }));
   }

   /*******************************************************************************
    ** Every checkpoint holds the inbox lock and verifies ownership until commit.
    *******************************************************************************/
   public static <T> T fenced(QRecord inbox, Instant lease, SystemWrites.Work<T> work) throws QException
   {
      return (SystemWrites.transaction(() ->
      {
         if(!SystemWrites.guard("UPDATE edi_inbox SET id=id WHERE id=? AND status='PROCESSING' AND claimed_at=?", inbox.getValue("id"), lease))
         {
            throw new QException("Inbox claim was lost");
         }
         return (work.run());
      }));
   }

   /*******************************************************************************
    ** Persist retry outcome separately from failed set writes, with no exception text.
    *******************************************************************************/
   private void fail(QRecord row, Instant lease) throws QException
   {
      fenced(row, lease, () ->
      {
         int attempts = Objects.requireNonNullElse(row.getValueInteger("attempts"), 0) + 1;
         boolean dead = attempts > BACKOFF.size();
         SystemWrites.update("edi_inbox", new QRecord().withValue("id", row.getValue("id")).withValue("attempts", attempts)
            .withValue("status", dead ? "DEAD_LETTER" : "RETRY_WAIT").withValue("nextAttemptAt", dead ? null : now().plusSeconds(BACKOFF.get(attempts - 1)))
            .withValue("lastError", "Inbound processing failed; retry the stored payload"));
         if(dead)
         {
            EdiJournal.exception(row.getValueInteger("id"), null, null, row.getValueInteger("partnerId"), "DEAD_LETTER", "Inbound processing exhausted retries");
            QAlerts.raise(new AlertSignal(DEAD_LETTER_ALERT).withSeverity(AlertSeverity.ERROR).withDedupKey(row.getValueString("id"))
               .withTitle("EDI inbox exhausted retries").withSummary("Review the dead-letter inbox record"));
         }
         return (null);
      });
   }

   /*******************************************************************************
    ** Host clock makes retry and stale-claim behavior deterministic.
    *******************************************************************************/
   private static Instant now()
   {
      return (EdiRuntime.current().config().getClock().instant());
   }
}
