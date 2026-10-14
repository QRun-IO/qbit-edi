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


import com.kingsrook.qbits.edi.EdiQBitConfig;
import com.kingsrook.qbits.edi.EdiQBitProducer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitComponentMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Install tables, relations, record widgets and value sources.
 *******************************************************************************/
public class EdiMetaDataProducer extends QBitComponentMetaDataProducer<MetaDataProducerMultiOutput, EdiQBitConfig>
{
   /*******************************************************************************
    ** Only run when configured by the owning producer.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (getQBitConfig() != null);
   }

   /*******************************************************************************
    ** Produce tables, the typed app home, record actions and alert types.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance) throws QException
   {
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      EdiTables.possibleValueSources().forEach(output::add);
      EdiTables.joins().forEach(output::add);
      EdiTables.childWidgets().forEach(output::add);
      output.add(new QSecurityKeyType().withName("edi_partner").withAllAccessKeyName("edi.admin"));
      output.add(new QSecurityKeyType().withName("edi_guide_source").withAllAccessKeyName("edi.guide.loader"));
      java.util.List<QTableMetaData> tables = EdiTables.tables(getQBitConfig());
      for(QTableMetaData table : tables)
      {
         table.setSourceQBitName(instance.getQBits().values().stream().filter(q -> EdiQBitProducer.ARTIFACT_ID.equals(q.getArtifactId())).findFirst().orElseThrow().getName());
         output.add(table);
      }
      if(getQBitConfig().getEsbDestinationName() != null && !getQBitConfig().getEsbDestinationName().isBlank())
      {
         instance.withRecordChangeListener(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.edi.support.EdiDocumentEvents.class));
      }
      output.add(com.kingsrook.qbits.edi.app.EdiHome.app(getQBitConfig(), tables));
      com.kingsrook.qbits.edi.app.EdiHome.widgets().forEach(output::add);
      for(String name : java.util.List.of("edi.inbox.dead-letter", "edi.ack.overdue", "edi.ack.rejected", "edi.send.failed", "edi.certificate.expiring", "edi.certificate.expired"))
      {
         output.add(new com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData().withName(name).withLabel(name)
            .withSourceName("edi").withDefaultSeverity(com.kingsrook.qqq.backend.core.alerts.AlertSeverity.ERROR)
            .withDescription("EDI operational exception").withHowToFix("Review the related EDI records")
            .withVisibilityPermission("edi.exception.read"));
      }
      com.kingsrook.qbits.edi.processes.EdiProcesses.processes(getQBitConfig()).forEach(output::add);
      output.add(new com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData().withName("edi_preview_download")
         .withBackendType(com.kingsrook.qbits.edi.storage.PreviewDownloadBackend.class));
      output.add(new QTableMetaData().withName("edi_preview_download").withLabel("Preview Download").withBackendName("edi_preview_download")
         .withPrimaryKeyField("id").withField(new com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData("id", com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.INTEGER).withLabel("ID"))
         .withPermissionRules(new com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules().withPermissionBaseName("edi.map.preview")
            .withLevel(com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel.HAS_ACCESS_PERMISSION))
         .withoutCapabilities(com.kingsrook.qqq.backend.core.model.metadata.tables.Capability.TABLE_INSERT,
            com.kingsrook.qqq.backend.core.model.metadata.tables.Capability.TABLE_UPDATE, com.kingsrook.qqq.backend.core.model.metadata.tables.Capability.TABLE_DELETE));
      return (output);
   }
}
