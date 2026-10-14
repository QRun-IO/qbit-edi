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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Transactional source listener and explicit enqueue boundary for MANUAL/ESB.
 *******************************************************************************/
public class OutboundEnqueue implements RecordChangeListenerInterface
{
   /*******************************************************************************
    ** Ignore internal tables and deletes; definitions are read at event time.
    *******************************************************************************/
   public boolean appliesTo(String table, RecordChangeType type)
   {
      return (!table.startsWith("edi_") && type != RecordChangeType.DELETE);
   }

   /*******************************************************************************
    ** Use the event transaction, even when the caller did not put it in QContext.
    *******************************************************************************/
   public void onRecordsChanged(RecordChangeEvent event) throws QException
   {
      var previous = QContext.getQBackendTransaction();
      QContext.setTransaction(event.getTransaction());
      try
      {
         for(QRecord record : event.getRecords())
         {
            enqueue(event.getTableName(), record, "RECORD_CHANGE", null);
         }
      }
      finally
      {
         QContext.setTransaction(previous);
      }
   }

   /*******************************************************************************
    ** Enqueue matching active outbound definitions using the writer's transaction.
    *******************************************************************************/
   public static Integer enqueue(String table, QRecord source, String trigger, String eventId) throws QException
   {
      return (SystemWrites.call(() ->
      {
         int count = 0;
         var definitions = EdiRecords.query("edi_agreement_document", EdiRecords.equal("sourceTableName", table)
            .withCriteria("direction", QCriteriaOperator.EQUALS, "OUTBOUND").withCriteria("isActive", QCriteriaOperator.EQUALS, true));
         for(QRecord document : definitions)
         {
            if(!"MANUAL".equals(trigger) && !trigger.equals(document.getValueString("outboundTrigger")))
            {
               continue;
            }
            QRecord agreement = OutboundWorker.row("edi_agreement", document.getValueInteger("agreementId"));
            if(!Boolean.TRUE.equals(agreement.getValueBoolean("isActive")))
            {
               continue;
            }
            var metadata = QContext.getQInstance().getTable(table);
            if(!metadata.getBackendName().equals(EdiRuntime.current().config().getBackendName()))
            {
               throw new QException("Outbound source and EDI must share the transactional backend");
            }
            String filter = document.getValueString("triggerFilter");
            if(filter != null && !filter.isBlank() && EdiRecords.query(table, EdiRecords.equal(metadata.getPrimaryKeyField(), source.getValue(metadata.getPrimaryKeyField()))
               .withSubFilter(new ObjectMapper().readValue(filter, QQueryFilter.class))).isEmpty())
            {
               continue;
            }
            if(eventId != null && !EdiRecords.query("edi_outbound_request", EdiRecords.equal("eventId", eventId)
               .withCriteria("agreementDocumentId", QCriteriaOperator.EQUALS, document.getValue("id"))).isEmpty())
            {
               continue;
            }
            SystemWrites.insert("edi_outbound_request", new QRecord().withValue("agreementDocumentId", document.getValue("id"))
               .withValue("sourceTableName", table).withValue("sourceRecordId", source.getValueString(metadata.getPrimaryKeyField()))
               .withValue("eventId", eventId).withValue("status", "PENDING").withValue("attempts", 0)
               .withValue("nextAttemptAt", EdiRuntime.current().config().getClock().instant()));
            count++;
         }
         return (count);
      }));
   }
}
