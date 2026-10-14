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

package com.kingsrook.qbits.edi.map;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;

/*******************************************************************************
 ** Permission-enforced target writes, including association writes and upsert reads.
 *******************************************************************************/
public class InboundApplier
{
   /*******************************************************************************
    ** Run mapping and applying with the supplied least-privilege service identity.
    *******************************************************************************/
   public static <T> T asService(Supplier<QSession> supplier, com.kingsrook.qbits.edi.support.SystemWrites.Work<T> work) throws Exception
   {
      if(supplier == null)
      {
         throw new IllegalStateException("no service session configured");
      }
      QSession session = supplier.get();
      if(session == null)
      {
         throw new IllegalStateException("no service session configured");
      }
      QSession previous = QContext.getQSession();
      QContext.setQSession(session);
      try
      {
         return (work.run());
      }
      finally
      {
         QContext.setQSession(previous);
      }
   }

   /*******************************************************************************
    ** Add this transaction's partner key to a copy of the configured session.
    *******************************************************************************/
   public static <T> T asPartner(Supplier<QSession> supplier, Integer partner, com.kingsrook.qbits.edi.support.SystemWrites.Work<T> work) throws Exception
   {
      return (asService(supplier, () ->
      {
         QSession prior = QContext.getQSession();
         QSession session = prior.clone();
         Map<String, List<java.io.Serializable>> keys = new java.util.HashMap<>();
         if(prior.getSecurityKeyValues() != null)
         {
            prior.getSecurityKeyValues().forEach((key, values) -> keys.put(key, new java.util.ArrayList<>(values)));
         }
         session.setSecurityKeyValues(keys);
         if(partner != null)
         {
            session.withSecurityKeyValue("edi_partner", partner);
         }
         QContext.setQSession(session);
         try
         {
            return (work.run());
         }
         finally
         {
            QContext.setQSession(prior);
         }
      }));
   }

   /*******************************************************************************
    ** The caller owns the per-transaction-set database transaction.
    *******************************************************************************/
   public QRecord apply(QRecord map, QRecord record) throws Exception
   {
      String table = map.getValueString("tableName");
      var targetTable = QContext.getQInstance().getTable(table);
      String backend = com.kingsrook.qbits.edi.EdiRuntime.current().config().getBackendName();
      if(targetTable == null || !backend.equals(targetTable.getBackendName())
         || targetTable.getAssociations() != null && targetTable.getAssociations().stream().anyMatch(a -> !backend.equals(QContext.getQInstance().getTable(a.getAssociatedTableName()).getBackendName())))
      {
         throw new IllegalArgumentException("Inbound targets and associations must share the EDI transactional backend");
      }
      String keys = map.getValueString("matchKeyFields");
      List<QRecord> matches = List.of();
      if(keys != null && !keys.isBlank())
      {
         QQueryFilter filter = new QQueryFilter().withLimit(2);
         for(String key : keys.split(","))
         {
            key = key.trim();
            if(record.getValue(key) == null)
            {
               throw new IllegalArgumentException("Upsert key is missing");
            }
            filter.withCriteria(key, QCriteriaOperator.EQUALS, record.getValue(key));
         }
         QueryInput query = new QueryInput(table).withFilter(filter).withTransaction(QContext.getQBackendTransaction());
         query.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         matches = new QueryAction().execute(query).getRecords();
      }
      if(matches.size() > 1)
      {
         throw new IllegalArgumentException("Upsert key is ambiguous");
      }
      QRecord result;
      if(matches.isEmpty())
      {
         InsertInput insert = new InsertInput(table).withRecord(record).withTransaction(QContext.getQBackendTransaction());
         insert.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         result = new InsertAction().execute(insert).getRecords().getFirst();
      }
      else
      {
         String onMatch = map.getValueString("onMatch");
         if("SKIP".equals(onMatch))
         {
            return (matches.getFirst());
         }
         if(!"UPDATE".equals(onMatch))
         {
            throw new IllegalArgumentException("Existing target record is not updateable by this map");
         }
         String primaryKey = QContext.getQInstance().getTable(table).getPrimaryKeyField();
         record.withValue(primaryKey, matches.getFirst().getValue(primaryKey));
         UpdateInput update = new UpdateInput(table).withRecord(record).withTransaction(QContext.getQBackendTransaction());
         update.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         result = new UpdateAction().execute(update).getRecords().getFirst();
      }
      if(result.getErrors() != null && !result.getErrors().isEmpty())
      {
         throw new IllegalArgumentException("Target write failed validation");
      }
      return (result);
   }
}
