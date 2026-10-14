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

package com.kingsrook.qbits.edi.x12;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Atomic cross-replica compare-and-set using the RC.5 RDBMS connection API.
 *******************************************************************************/
public class ControlNumberAllocator
{
   private final RDBMSBackendMetaData backend;

   /*******************************************************************************
    ** Use an explicitly configured QQQ database backend.
    *******************************************************************************/
   public ControlNumberAllocator(RDBMSBackendMetaData backend)
   {
      this.backend = backend;
   }

   /*******************************************************************************
    ** Create on first use and allocate in a short independent transaction.
    *******************************************************************************/
   public long allocate(Integer agreementId, Integer localIdentityId, String level, long min, long max, boolean wrap) throws SQLException
   {
      long ceiling = List.of("UNB", "UNG", "UNH").contains(level) ? 99999999999999L : 999999999;
      if((agreementId == null) == (localIdentityId == null) || !List.of("ISA", "GS", "UNB", "UNG", "UNH").contains(level) || min < 1 || max < min || max > ceiling)
      {
         throw new IllegalArgumentException("Invalid control number scope or bounds");
      }
      String scope = agreementId == null ? "identity:" + localIdentityId : "agreement:" + agreementId;
      for(int attempt = 0; attempt < 10000; attempt++)
      {
         try(Connection connection = ConnectionManager.getConnection(backend))
         {
            connection.setAutoCommit(false);
            try
            {
               Long previous = null;
               Integer id = null;
               long minimum = min;
               long maximum = max;
               boolean wrapping = wrap;
               try(PreparedStatement select = connection.prepareStatement("SELECT id, last_value, min_value, max_value, wrap_allowed FROM edi_control_number WHERE scope_key = ? AND level = ?"))
               {
                  select.setString(1, scope);
                  select.setString(2, level);
                  try(ResultSet result = select.executeQuery())
                  {
                     if(result.next())
                     {
                        id = result.getInt(1);
                        previous = result.getLong(2);
                        minimum = result.getLong(3);
                        maximum = result.getLong(4);
                        wrapping = result.getBoolean(5);
                     }
                  }
               }
               if(previous == null)
               {
                  try(PreparedStatement insert = connection.prepareStatement("INSERT INTO edi_control_number (scope_key, agreement_id, local_identity_id, level, last_value, min_value, max_value, wrap_allowed, create_date, modify_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"))
                  {
                     insert.setString(1, scope);
                     insert.setObject(2, agreementId);
                     insert.setObject(3, localIdentityId);
                     insert.setString(4, level);
                     insert.setLong(5, min);
                     insert.setLong(6, min);
                     insert.setLong(7, max);
                     insert.setBoolean(8, wrap);
                     insert.executeUpdate();
                     connection.commit();
                     return (min);
                  }
               }
               if(minimum < 1 || maximum < minimum || maximum > ceiling || previous < minimum - 1 || previous > maximum)
               {
                  throw new IllegalStateException("Stored control number range is invalid");
               }
               if(previous >= maximum && !wrapping)
               {
                  throw new IllegalStateException("Control number range exhausted");
               }
               long next = previous >= maximum ? minimum : previous + 1;
               try(PreparedStatement update = connection.prepareStatement("UPDATE edi_control_number SET last_value = ?, modify_date = CURRENT_TIMESTAMP WHERE id = ? AND last_value = ?"))
               {
                  update.setLong(1, next);
                  update.setInt(2, id);
                  update.setLong(3, previous);
                  if(update.executeUpdate() == 1)
                  {
                     connection.commit();
                     return (next);
                  }
               }
               connection.rollback();
            }
            catch(SQLException e)
            {
               connection.rollback();
               if(!"23505".equals(e.getSQLState()) && !"40001".equals(e.getSQLState()))
               {
                  throw e;
               }
            }
            catch(RuntimeException e)
            {
               connection.rollback();
               throw e;
            }
         }
      }
      throw new SQLException("Control allocation retry limit exceeded");
   }
}
