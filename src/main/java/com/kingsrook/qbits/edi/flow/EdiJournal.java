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

import java.util.List;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Value-free operational timeline and coalesced exception work queue.
 *******************************************************************************/
public final class EdiJournal
{
   /*******************************************************************************
    ** Events contain only step names, record references and timestamps.
    *******************************************************************************/
   public static void event(Integer inbox, Integer interchange, Integer transaction, String type) throws Exception
   {
      SystemWrites.insert("edi_event", new QRecord().withValue("inboxId", inbox).withValue("interchangeId", interchange)
         .withValue("transactionId", transaction).withValue("at", EdiRuntime.current().config().getClock().instant())
         .withValue("type", type).withValue("detail", type).withValue("userName", "edi"));
   }

   /*******************************************************************************
    ** Repeated failures of the same item increment occurrences without exposing values.
    *******************************************************************************/
   public static void exception(Integer inbox, Integer interchange, Integer transaction, Integer partner, String type, String summary) throws Exception
   {
      QQueryFilter filter = EdiRecords.equal("type", type);
      for(var pair : List.of(new Object[] {"inboxId", inbox}, new Object[] {"interchangeId", interchange}, new Object[] {"transactionId", transaction}))
      {
         filter.withCriteria(pair[0].toString(), pair[1] == null ? QCriteriaOperator.IS_BLANK : QCriteriaOperator.EQUALS, (Integer) pair[1]);
      }
      List<QRecord> matches = EdiRecords.query("edi_exception", filter);
      QRecord exception;
      boolean opened = matches.isEmpty();
      if(opened)
      {
         exception = SystemWrites.insert("edi_exception", new QRecord().withValue("inboxId", inbox).withValue("interchangeId", interchange).withValue("transactionId", transaction)
            .withValue("partnerId", partner).withValue("type", type).withValue("summary", summary).withValue("status", "OPEN").withValue("occurrences", 1));
      }
      else
      {
         QRecord row = matches.getFirst();
         exception = row;
         opened = List.of("RESOLVED", "IGNORED").contains(row.getValueString("status"));
         SystemWrites.update("edi_exception", new QRecord().withValue("id", row.getValue("id")).withValue("occurrences", row.getValueInteger("occurrences") + 1).withValue("status", "OPEN"));
      }
      if(opened && partner != null && List.of("REJECTED_ACK", "DEAD_LETTER").contains(type))
      {
         String[] assignees = EdiRecords.query("edi_exception", EdiRecords.equal("partnerId", partner)
            .withCriteria("status", QCriteriaOperator.IN, "OPEN", "IN_PROGRESS")).stream()
            .map(row -> row.getValueString("assignedToUserId")).filter(user -> user != null && !user.isBlank()).distinct().toArray(String[]::new);
         if(assignees.length > 0)
         {
            QAlerts.notify(new NotificationSignal("EDI exception opened")
               .withBody("An acknowledgement rejection or dead-letter exception needs attention.")
               .withAudience(NotificationAudience.users(assignees))
               .withSourceName("edi").withSourceTableName("edi_exception").withSourceRecordId(exception.getValueString("id")));
         }
      }
      event(inbox, interchange, transaction, "EXCEPTION");
   }
}
