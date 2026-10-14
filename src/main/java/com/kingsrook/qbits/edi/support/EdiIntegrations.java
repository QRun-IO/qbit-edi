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
import com.kingsrook.qbits.edi.app.EdiHome;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Optional host search and standard ESB table-event integration declarations.
 *******************************************************************************/
public final class EdiIntegrations
{
   /*******************************************************************************
    ** Main tables eligible for a host's Quick Search and API registration.
    *******************************************************************************/
   public static List<String> mainTables()
   {
      return (java.util.stream.Stream.concat(EdiHome.ACTIVITY.stream(), EdiHome.SETUP.stream()).toList());
   }

   /*******************************************************************************
    ** Isolate optional ESB classes so hosts without the module can install EDI.
    *******************************************************************************/
   public static void configure(QTableMetaData table, String destination)
   {
      if(destination == null || destination.isBlank() || !mainTables().contains(table.getName()))
      {
         return;
      }
      try
      {
         Class.forName("com.kingsrook.qqq.esb.model.EsbTableMetaData", false, EdiIntegrations.class.getClassLoader());
         if(!"edi_transaction".equals(table.getName()))
         {
            OptionalEsb.configure(table, destination);
         }
      }
      catch(ClassNotFoundException | NoClassDefFoundError e)
      {
         throw new IllegalStateException("Configured EDI ESB destination requires the optional qqq-esb module");
      }
   }

   /*******************************************************************************
    ** Core owns commit ordering, delivery, failures and table-event payload policy.
    *******************************************************************************/
   private static class OptionalEsb
   {
      /*******************************************************************************
       ** Attach standard insert, update and delete publication metadata.
       *******************************************************************************/
      private static void configure(QTableMetaData table, String destination)
      {
         com.kingsrook.qqq.esb.model.EsbTableMetaData.ofOrWithNew(table).withPublication(new com.kingsrook.qqq.esb.model.EsbTablePublication()
            .withDestinationName(destination).withEvents(List.of(com.kingsrook.qqq.esb.model.EsbTableEvent.INSERT,
               com.kingsrook.qqq.esb.model.EsbTableEvent.UPDATE, com.kingsrook.qqq.esb.model.EsbTableEvent.DELETE)));
      }
   }
}
