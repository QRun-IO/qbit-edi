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

package com.kingsrook.qbits.edi.storage;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Normal QQQ actions under the caller service session, preserving permissions and customizers.
 *******************************************************************************/
public final class EdiRecords
{
   /*******************************************************************************
    ** Insert one record and refuse action-level validation failures.
    *******************************************************************************/
   public static QRecord insert(String table, QRecord record) throws QException
   {
      QRecord result = new InsertAction().execute(new InsertInput(table).withRecord(record).withTransaction(com.kingsrook.qqq.backend.core.context.QContext.getQBackendTransaction())).getRecords().getFirst();
      if(result.getErrors() != null && !result.getErrors().isEmpty())
      {
         throw new QException("EDI record insert failed for " + table);
      }
      return (result);
   }

   /*******************************************************************************
    ** Query with a caller-supplied filter, fetching payload and guide text fields.
    *******************************************************************************/
   public static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table).withFilter(filter).withShouldFetchHeavyFields(true).withTransaction(com.kingsrook.qqq.backend.core.context.QContext.getQBackendTransaction())).getRecords());
   }

   /*******************************************************************************
    ** Match one field by exact value.
    *******************************************************************************/
   public static QQueryFilter equal(String field, Serializable value)
   {
      return (new QQueryFilter().withCriteria(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value)));
   }
}
