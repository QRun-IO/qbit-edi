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

package com.kingsrook.qbits.edi.processes;

import java.util.List;
import com.kingsrook.qbits.edi.EdiQBitConfig;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.AckMonitor;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.flow.OutboundEnqueue;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.flow.SendWorker;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.EdiTransportPluginsMetaData;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;

/*******************************************************************************
 ** Scheduled workers plus an explicit process boundary for broker-owned ESB wiring.
 *******************************************************************************/
public class OutboundProcesses implements BackendStep
{
   /*******************************************************************************
    ** Common worker metadata is hidden from application navigation.
    *******************************************************************************/
   public static QProcessMetaData process(String name, EdiQBitConfig config, Integer seconds, String cron)
   {
      QProcessMetaData process = new QProcessMetaData().withName(name).withLabel(name).withIsHidden(true)
         .withPermissionRules(new QPermissionRules().withPermissionBaseName("edi.admin").withLevel(PermissionLevel.HAS_ACCESS_PERMISSION))
         .withStep(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(OutboundProcesses.class)));
      if(config.getSchedulerName() != null && (seconds != null || cron != null))
      {
         process.withSchedule(new QScheduleMetaData().withSchedulerName(config.getSchedulerName()).withRepeatSeconds(seconds).withCronExpression(cron).withCronTimeZoneId("UTC"));
      }
      return (process);
   }

   /*******************************************************************************
    ** Install data-driven schedules after the database is seeded and before startup.
    *******************************************************************************/
   public static void installSchedules() throws QException
   {
      SystemWrites.call(() ->
      {
         var config = EdiRuntime.current().config();
         for(QRecord document : EdiRecords.query("edi_agreement_document", EdiRecords.equal("direction", "OUTBOUND")
            .withCriteria("isActive", QCriteriaOperator.EQUALS, true).withCriteria("batchMode", QCriteriaOperator.EQUALS, "BATCH")))
         {
            if(document.getValueString("batchSchedule") == null)
            {
               throw new QException("BATCH requires batchSchedule");
            }
            install(process("edi_batch_" + document.getValue("id"), config, null, document.getValueString("batchSchedule")));
         }
         for(QRecord transport : EdiRecords.query("edi_transport", EdiRecords.equal("type", "SFTP").withCriteria("isActive", QCriteriaOperator.EQUALS, true)
            .withCriteria("sftpPollCron", QCriteriaOperator.IS_NOT_BLANK)))
         {
            install(process("edi_poll_" + transport.getValue("id"), config, null, transport.getValueString("sftpPollCron")));
         }
         return (null);
      });
   }

   /*******************************************************************************
    ** ESB passes sourceTableName/sourceRecordId/eventId; hosts own broker bindings.
    *******************************************************************************/
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      String name = input.getProcessName();
      if(name.equals("edi_outbound_worker"))
      {
         new OutboundWorker().runOnce();
      }
      else if(name.equals("edi_send_worker"))
      {
         new SendWorker().runOnce();
      }
      else if(name.equals("edi_ack_monitor"))
      {
         new AckMonitor().runOnce();
      }
      else if(name.startsWith("edi_batch_"))
      {
         new OutboundWorker().runDocument(Integer.valueOf(name.substring("edi_batch_".length())));
      }
      else if(name.startsWith("edi_poll_"))
      {
         SystemWrites.call(() ->
         {
            EdiTransport transport = OutboundWorker.row("edi_transport", Integer.valueOf(name.substring("edi_poll_".length()))).toEntity(EdiTransport.class);
            if(Boolean.TRUE.equals(transport.getIsActive()))
            {
               EdiTransportPluginsMetaData.current().get(transport.getType()).poll(transport, new InboundSink(EdiRuntime.current().payloads()));
            }
            return (null);
         });
      }
      else if(name.equals("edi_outbound_enqueue"))
      {
         String table = input.getValueString("sourceTableName");
         String key = QContext.getQInstance().getTable(table).getPrimaryKeyField();
         OutboundEnqueue.enqueue(table, new QRecord().withValue(key, input.getValue("sourceRecordId")), "ESB_EVENT", input.getValueString("eventId"));
      }
   }

   /*******************************************************************************
    ** Repeated startup initialization updates existing data-driven schedule metadata.
    *******************************************************************************/
   private static void install(QProcessMetaData process)
   {
      if(QContext.getQInstance().getProcesses().containsKey(process.getName()))
      {
         QContext.getQInstance().getProcess(process.getName()).setSchedule(process.getSchedule());
      }
      else
      {
         QContext.getQInstance().addProcess(process);
      }
   }
}
