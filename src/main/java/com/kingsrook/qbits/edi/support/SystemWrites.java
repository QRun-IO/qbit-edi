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


import java.sql.SQLException;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** System identity, audited QQQ writes, and transactional database claim guards.
 *******************************************************************************/
public final class SystemWrites
{
   /*******************************************************************************
    ** A unit of work in the caller's QQQ instance.
    *******************************************************************************/
   @FunctionalInterface
   public interface Work<T>
   {
      /*******************************************************************************
       ** Execute the work.
       *******************************************************************************/
      T run() throws Exception;
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private SystemWrites()
   {
   }



   /*******************************************************************************
    ** Preserve the host session and transaction while using the processing user.
    *******************************************************************************/
   public static <T> T call(Work<T> work) throws QException
   {
      QSession previous = QContext.getQSession();
      QContext.setQSession(new QSystemUserSession(QContext.getQInstance())
         .withUser(new QUser().withIdReference("edi").withFullName("EDI processor")));
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         return (work.run());
      }
      catch(Exception e)
      {
         throw (new QException("EDI operation failed", e));
      }
      finally
      {
         QContext.setQSession(previous);
      }
   }



   /*******************************************************************************
    ** Each inbox operation owns a transaction, exposed to listeners via QContext.
    *******************************************************************************/
   public static <T> T transaction(Work<T> work) throws QException
   {
      QBackendTransaction previous = QContext.getQBackendTransaction();
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new QueryInput("edi_inbox")))
      {
         QContext.setTransaction(transaction);
         try
         {
            T result = work.run();
            transaction.commit();
            return (result);
         }
         catch(Exception e)
         {
            transaction.rollback();
            throw (new QException("EDI transaction failed", e));
         }
      }
      finally
      {
         QContext.setTransaction(previous);
      }
   }



   /*******************************************************************************
    ** Query in the active transaction.
    *******************************************************************************/
   public static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table).withFilter(filter).withTransaction(QContext.getQBackendTransaction())).getRecords());
   }



   /*******************************************************************************
    ** Count without loading payloads.
    *******************************************************************************/
   public static Integer count(String table, QQueryFilter filter) throws QException
   {
      return (new CountAction().execute(new CountInput(table).withFilter(filter)).getCount());
   }



   /*******************************************************************************
    ** Insert and fail the transaction on record-level errors as well as exceptions.
    *******************************************************************************/
   public static QRecord insert(String table, QRecord record) throws QException
   {
      QRecord result = new InsertAction().execute(new InsertInput(table).withRecords(List.of(record))
         .withTransaction(QContext.getQBackendTransaction())).getRecords().get(0);
      check(result);
      return (result);
   }



   /*******************************************************************************
    ** Update through QQQ so its normal audit rules apply.
    *******************************************************************************/
   public static void update(String table, QRecord record) throws QException
   {
      List<QRecord> results = new UpdateAction().execute(new UpdateInput(table).withRecords(List.of(record))
         .withTransaction(QContext.getQBackendTransaction())).getRecords();
      if(results.size() != 1)
      {
         throw (new QException("EDI record was not updated"));
      }
      check(results.get(0));
   }



   /*******************************************************************************
    ** Do not propagate record values or database error messages into diagnostics.
    *******************************************************************************/
   private static void check(QRecord record) throws QException
   {
      if(record.getErrors() != null && !record.getErrors().isEmpty())
      {
         throw (new QException("EDI record write failed"));
      }
   }



   /*******************************************************************************
    ** A prepared conditional update holds a row lock until transaction completion.
    ** Only internal SQL and bound values are accepted by the callers.
    *******************************************************************************/
   public static Boolean guard(String sql, Object... values) throws QException
   {
      if(!(QContext.getQBackendTransaction() instanceof RDBMSTransaction transaction))
      {
         throw (new QException("EDI inbox requires an RDBMS transaction"));
      }
      try
      {
         RDBMSBackendMetaData backend = (RDBMSBackendMetaData) new QueryInput("edi_inbox").getBackend();
         return (backend.getActionStrategy().executeUpdateForRowCount(transaction.getConnection(), sql, values) == 1);
      }
      catch(SQLException e)
      {
         throw (new QException("EDI database claim failed"));
      }
   }
}
