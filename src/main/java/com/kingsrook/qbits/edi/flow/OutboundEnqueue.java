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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;

/*******************************************************************************
 ** Transactional source listener and explicit enqueue boundary for MANUAL/ESB.
 ** The listener applies only to tables with an active RECORD_CHANGE definition
 ** (cached per runtime, see {@link OutboundSourceTables}), and to the definition
 ** tables themselves, whose changes invalidate that cache.
 *******************************************************************************/
public class OutboundEnqueue implements RecordChangeListenerInterface
{
   private static final QLogger LOG = QLogger.getLogger(OutboundEnqueue.class);

   /*******************************************************************************
    ** Definition changes (any type), and inserts/updates of configured sources.
    *******************************************************************************/
   public boolean appliesTo(String table, RecordChangeType type)
   {
      EdiRuntime runtime = EdiRuntime.find();
      if(runtime == null)
      {
         return (false);
      }
      if(OutboundSourceTables.DEFINITION_TABLES.contains(table))
      {
         return (true);
      }
      if(table.startsWith("edi_") || type == RecordChangeType.DELETE)
      {
         return (false);
      }
      try
      {
         return (runtime.outboundSources().contains(table, QContext.getQBackendTransaction()));
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////////
         // if the definitions can't be read, apply - so the event runs its  //
         // own queries and reports the failure, rather than silently        //
         // dropping outbound work.                                          //
         //////////////////////////////////////////////////////////////////////
         LOG.warn("Unable to read EDI outbound source tables", e, logPair("table", table));
         return (true);
      }
   }

   /*******************************************************************************
    ** Use the event transaction, even when the caller did not put it in QContext.
    ** Definitions are read once per event, not once per record.
    *******************************************************************************/
   public void onRecordsChanged(RecordChangeEvent event) throws QException
   {
      if(OutboundSourceTables.DEFINITION_TABLES.contains(event.getTableName()))
      {
         EdiRuntime.current().outboundSources().invalidate(event.getTransaction());
         return;
      }
      var previous = QContext.getQBackendTransaction();
      QContext.setTransaction(event.getTransaction());
      try
      {
         SystemWrites.call(() ->
         {
            List<QRecord> definitions = definitions(event.getTableName(), OutboundSourceTables.TRIGGER);
            for(QRecord record : event.getRecords())
            {
               enqueue(event.getTableName(), record, null, definitions);
            }
            return (null);
         });
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
      return (SystemWrites.call(() -> enqueue(table, source, eventId, definitions(table, trigger))));
   }

   /*******************************************************************************
    ** Active outbound documents for the table whose trigger matches (MANUAL
    ** matches every trigger) and whose agreement is active.  Called inside a
    ** {@link SystemWrites#call}.
    *******************************************************************************/
   static List<QRecord> definitions(String table, String trigger) throws QException
   {
      List<QRecord> matching = new ArrayList<>();
      for(QRecord document : EdiRecords.query("edi_agreement_document", EdiRecords.equal("sourceTableName", table)
         .withCriteria("direction", QCriteriaOperator.EQUALS, "OUTBOUND").withCriteria("isActive", QCriteriaOperator.EQUALS, true)))
      {
         if("MANUAL".equals(trigger) || trigger.equals(document.getValueString("outboundTrigger")))
         {
            matching.add(document);
         }
      }
      Set<Integer> activeAgreements = OutboundSourceTables.activeAgreementIds(matching);
      matching.removeIf(document -> !activeAgreements.contains(document.getValueInteger("agreementId")));
      return (matching);
   }

   /*******************************************************************************
    ** Enqueue one source record against already-resolved definitions.
    *******************************************************************************/
   private static int enqueue(String table, QRecord source, String eventId, List<QRecord> definitions) throws Exception
   {
      int count = 0;
      for(QRecord document : definitions)
      {
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
   }
}
