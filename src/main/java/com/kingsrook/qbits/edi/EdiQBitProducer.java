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

package com.kingsrook.qbits.edi;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;


/*******************************************************************************
 ** Entry point for the EDI qbit; component producers are scanned only in the
 ** metadata package.
 *******************************************************************************/
public class EdiQBitProducer implements QBitMetaDataProducer<EdiQBitConfig>
{
   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "qbit-edi";
   public static final String VERSION     = "0.1.0";

   private EdiQBitConfig qBitConfig;
   private String        namespace;



   /*******************************************************************************
    ** Fluent configuration setter.
    *******************************************************************************/
   public EdiQBitProducer withQBitConfig(EdiQBitConfig config)
   {
      qBitConfig = config;
      return (this);
   }



   /*******************************************************************************
    ** Configuration for component producers.
    *******************************************************************************/
   @Override
   public EdiQBitConfig getQBitConfig()
   {
      return (qBitConfig);
   }



   /*******************************************************************************
    ** Fluent namespace setter.
    *******************************************************************************/
   public EdiQBitProducer withNamespace(String namespace)
   {
      this.namespace = namespace;
      return (this);
   }



   /*******************************************************************************
    ** Qbit namespace.
    *******************************************************************************/
   @Override
   public String getNamespace()
   {
      return (namespace);
   }



   /*******************************************************************************
    ** Qbit identity.
    *******************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      return (new QBitMetaData().withGroupId(GROUP_ID).withArtifactId(ARTIFACT_ID).withVersion(VERSION).withNamespace(namespace).withConfig(qBitConfig));
   }



   /*******************************************************************************
    ** Unconfigured producers discovered by host scanning are disabled.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (qBitConfig != null);
   }



   /*******************************************************************************
    ** Only the metadata package holds component producers.
    *******************************************************************************/
   @Override
   public String getPackageNameForFindingMetaDataProducers()
   {
      return ("com.kingsrook.qbits.edi.metadata");
   }



   /*******************************************************************************
    ** Validate the configuration and install the qbit's metadata.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance) throws QException
   {
      qBitConfig.validate(instance);
      MetaDataProducerMultiOutput output = QBitMetaDataProducer.super.produce(instance);
      output.addSelfToInstance(instance);
      EdiRuntime.install(instance, qBitConfig);
      instance.withRecordChangeListener(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.edi.flow.OutboundEnqueue.class));
      instance.withSupplementalMetaData(new com.kingsrook.qbits.edi.transport.EdiTransportPluginsMetaData()
         .withPlugin(new com.kingsrook.qbits.edi.transport.gateway.GatewayTransport())
         .withPlugin(new com.kingsrook.qbits.edi.transport.HttpsTransport()).withPlugin(new com.kingsrook.qbits.edi.transport.SftpTransport())
         .withPlugin(new com.kingsrook.qbits.edi.transport.UnavailableTransport("AS2")).withPlugin(new com.kingsrook.qbits.edi.transport.UnavailableTransport("EMAIL")));
      try
      {
         com.kingsrook.qqq.middleware.javalin.QJavalinMetaData.ofOrWithNew(instance).withAdditionalRouteProviderReference(
            new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.edi.transport.HttpsRouteProvider.class));
      }
      catch(NoClassDefFoundError optionalMiddlewareAbsent)
      {
         // Machine hooks are installed only when optional Javalin is present.
      }
      return (new MetaDataProducerMultiOutput());
   }
}
