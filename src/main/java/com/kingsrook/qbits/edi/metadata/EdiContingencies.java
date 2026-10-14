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

package com.kingsrook.qbits.edi.metadata;

import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** RC.5 form filters; no dependency on later dependency-behavior metadata.
 *******************************************************************************/
public final class EdiContingencies
{
   /*******************************************************************************
    ** Restrict every partner-scoped form reference to its owning context.
    *******************************************************************************/
   public static void apply(List<QTableMetaData> tables)
   {
      for(QTableMetaData table : tables)
      {
         for(var field : table.getFields().values())
         {
            String source = field.getPossibleValueSourceName();
            if(source == null)
            {
               continue;
            }
            QQueryFilter filter = null;
            if(table.getFields().containsKey("partnerId") && List.of("edi_identity", "edi_transport", "edi_certificate", "edi_guide", "edi_map").contains(source))
            {
               filter = new QQueryFilter(new QFilterCriteria("partnerId", QCriteriaOperator.EQUALS, "${input.partnerId}"));
               if(List.of("localIdentityId", "localCertificateId").contains(field.getName()))
               {
                  filter = new QQueryFilter(new QFilterCriteria("partnerId", QCriteriaOperator.IS_BLANK));
               }
               else if(List.of("edi_transport", "edi_guide", "edi_map").contains(source))
               {
                  filter = new QQueryFilter().withSubFilter(filter).withCriteria("partnerId", QCriteriaOperator.IS_BLANK)
                     .withBooleanOperator(QQueryFilter.BooleanOperator.OR);
               }
            }
            if(table.getName().equals("edi_map") && field.getName().equals("guideId"))
            {
               filter = new QQueryFilter().withSubFilter(filter).withCriteria("transactionSet", QCriteriaOperator.EQUALS, "${input.transactionSet}");
            }
            if(table.getName().equals("edi_agreement_document") && List.of("guideId", "mapId").contains(field.getName()))
            {
               filter = new QQueryFilter().withCriteria("transactionSet", QCriteriaOperator.EQUALS, "${input.transactionSet}")
                  .withCriteria("version", QCriteriaOperator.EQUALS, "${input.version}");
            }
            if(filter != null)
            {
               field.withPossibleValueSourceFilter(filter);
            }
         }
      }
   }
}
