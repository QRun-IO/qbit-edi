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
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Stable operator labels, including a PHI-safe fallback for documents without keys.
 *******************************************************************************/
public class EdiRecordLabels implements TableCustomizerInterface, QSupplementalInstanceMetaData
{
   /*******************************************************************************
    ** Instance enrichment runs after core has supplied default table formats.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("edi.recordLabels");
   }



   /*******************************************************************************
    ** Keep search/security label fields, while letting the query customizer supply
    ** the document label. RC.5 otherwise generates a fixed format from those fields.
    *******************************************************************************/
   @Override
   public void enrich(QInstance instance)
   {
      instance.getTable("edi_transaction").setRecordLabelFormat(null);
   }



   /*******************************************************************************
    ** Use native formats except where the label depends on available business keys.
    *******************************************************************************/
   public static void configure(QTableMetaData table)
   {
      switch(table.getName())
      {
         case "edi_identity" -> table.withRecordLabelFormatAndFields("%s %s", "interchangeId", "applicationCode");
         case "edi_agreement" -> table.withRecordLabelFormatAndFields("%s %s", "partnerId", "usageIndicator");
         case "edi_agreement_document" -> table.withRecordLabelFormatAndFields("%s %s %s", "transactionSet", "direction", "version");
         case "edi_control_number" -> table.withRecordLabelFormatAndFields("%s %s", "level", "lastValue");
         case "edi_guide", "edi_map" -> table.withRecordLabelFormatAndFields("%s r%s", "name", "revision");
         case "edi_guide_rule" -> table.withRecordLabelFormatAndFields("%s: %s", "type", "message");
         case "edi_map_rule" -> table.withRecordLabelFormatAndFields("%s: %s", "ediPath", "qqqPath");
         case "edi_interchange" -> table.withRecordLabelFormatAndFields("%s ISA %s", "direction", "controlNumber");
         case "edi_functional_group" -> table.withRecordLabelFormatAndFields("GS %s", "controlNumber");
         case "edi_exception" -> table.withRecordLabelFormatAndFields("%s: %s", "type", "summary");
         case "edi_inbox" -> table.withRecordLabelFormatAndFields("%s %s", "transportType", "externalId");
         case "edi_outbound_message" -> table.withRecordLabelFormatAndFields("%s %s", "transportId", "status");
         case "edi_outbound_request" -> table.withRecordLabelFormatAndFields("%s %s %s", "sourceTableName", "sourceRecordId", "status");
         case "edi_transaction_error" -> table.withRecordLabelFormatAndFields("%s: %s", "severity", "message");
         case "edi_event" -> table.withRecordLabelFormatAndFields("%s %s", "type", "at");
         case "edi_transaction" -> table.withRecordLabelFormat(null)
            .withRecordLabelFields("transactionSet", "businessKeyType", "businessKey", "controlNumber", "containsPhi")
            .withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(EdiRecordLabels.class));
         default ->
         {
            // Tables with a natural name retain their base record-label field.
         }
      }
   }



   /*******************************************************************************
    ** Queries, record reads and table value sources all use this same fallback.
    *******************************************************************************/
   @Override
   public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
   {
      for(QRecord record : records)
      {
         String key = record.getValueString("businessKey");
         String type = record.getValueString("businessKeyType");
         String suffix = !Boolean.TRUE.equals(record.getValueBoolean("containsPhi")) && StringUtils.hasContent(key) && StringUtils.hasContent(type)
            ? type + " " + key : "ST " + record.getValueString("controlNumber");
         record.setRecordLabel(record.getValueString("transactionSet") + " " + suffix);
      }
      return (records);
   }
}
