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

package com.kingsrook.qbits.edi.support;

import java.util.List;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.publish.EsbPublisher;

/*******************************************************************************
 ** RC.5's default events copy every field; document events must exclude all keys.
 ** Loaded only when the host enables the optional ESB integration.
 *******************************************************************************/
public class EdiDocumentEvents implements RecordChangeListenerInterface
{
   /*******************************************************************************
    ** Document table changes use an identifier-only payload.
    *******************************************************************************/
   @Override
   public boolean appliesTo(String table, RecordChangeType type)
   {
      return ("edi_transaction".equals(table));
   }

   /*******************************************************************************
    ** Produce core event envelopes, with the core publisher's after-commit semantics.
    *******************************************************************************/
   @Override
   public void onRecordsChanged(RecordChangeEvent change)
   {
      String destination = EdiRuntime.current().config().getEsbDestinationName();
      if(destination == null || !appliesTo(change.getTableName(), change.getType()))
      {
         return;
      }
      var metadata = com.kingsrook.qqq.esb.model.EsbInstanceMetaData.of(com.kingsrook.qqq.backend.core.context.QContext.getQInstance());
      String instanceName = metadata == null ? null : metadata.getInstanceName();
      List<QRecord> records = change.getRecords() == null ? List.of() : change.getRecords();
      for(int i = 0; i < records.size(); i++)
      {
         QRecord prior = change.getOldRecords() == null || i >= change.getOldRecords().size() ? null : change.getOldRecords().get(i);
         var event = EsbEventFactory.forRecordChange(instanceName, change.getTableName(), change.getType(), identifiers(records.get(i)), identifiers(prior));
         Runnable publish = () -> EsbPublisher.getInstance().publish(destination, List.of(event));
         if(change.getTransaction() == null)
         {
            publish.run();
         }
         else
         {
            change.getTransaction().addAfterCommitCallback(publish);
         }
      }
   }

   /*******************************************************************************
    ** Never copy payload-derived business identifiers, field values or diagnostics.
    *******************************************************************************/
   public static QRecord identifiers(QRecord record)
   {
      return (record == null ? null : new QRecord().withValue("id", record.getValue("id")).withValue("status", record.getValue("status")));
   }
}
