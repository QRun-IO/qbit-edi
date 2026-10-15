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


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
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
   public static final String VERSION     = loadVersion();

   private EdiQBitConfig qBitConfig;
   private String        namespace;



   /*******************************************************************************
    ** Read the artifact version from the Maven-filtered resource.
    *******************************************************************************/
   private static String loadVersion()
   {
      try(InputStream inputStream = EdiQBitProducer.class.getClassLoader().getResourceAsStream("qbit-edi.properties"))
      {
         if(inputStream == null)
         {
            throw new IllegalStateException("Missing qbit-edi.properties; build with Maven to supply the artifact version");
         }
         Properties properties = new Properties();
         properties.load(inputStream);
         String version = properties.getProperty("version");
         if(version == null || version.isBlank() || version.contains("${"))
         {
            throw new IllegalStateException("Missing or unfiltered version in qbit-edi.properties; build with Maven");
         }
         return (version);
      }
      catch(IOException e)
      {
         throw new IllegalStateException("Could not read the artifact version from qbit-edi.properties", e);
      }
   }



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
      // Registration is metadata-only; core startup sync skips hosts without helpContent.
      HelpBundles.register(instance, "help/qbit-edi.yaml");
      // The app name is configurable; its bundle key must follow the installed app.
      HelpBundles.of(instance).getBundles().getLast().getEntries().stream()
         .filter(entry -> "app:edi;slot:help".equals(entry.getKey()))
         .forEach(entry -> entry.setKey("app:" + qBitConfig.getAppName() + ";slot:help"));
      if(instance.getTable("helpContent") == null)
      {
         com.kingsrook.qbits.edi.metadata.EdiHelp.apply(instance, qBitConfig.getAppName());
      }
      instance.withSupplementalMetaData(new com.kingsrook.qbits.edi.metadata.EdiRecordLabels());
      instance.withRecordChangeListener(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.edi.flow.OutboundEnqueue.class));
      instance.withSupplementalMetaData(new com.kingsrook.qbits.edi.transport.EdiTransportPluginsMetaData()
         .withPlugin(new com.kingsrook.qbits.edi.transport.gateway.GatewayTransport())
         .withPlugin(new com.kingsrook.qbits.edi.transport.HttpsTransport()).withPlugin(new com.kingsrook.qbits.edi.transport.SftpTransport())
         .withPlugin(new com.kingsrook.qbits.edi.as2.As2Transport()).withPlugin(new com.kingsrook.qbits.edi.transport.UnavailableTransport("EMAIL")));
      try
      {
         com.kingsrook.qbits.edi.api.EdiApi.register(instance);
         com.kingsrook.qqq.middleware.javalin.QJavalinMetaData.ofOrWithNew(instance).withAdditionalRouteProviderReference(
            new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.edi.transport.HttpsRouteProvider.class))
            .withAdditionalRouteProviderReference(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(com.kingsrook.qbits.edi.as2.As2RouteProvider.class));
      }
      catch(NoClassDefFoundError optionalMiddlewareAbsent)
      {
         // Machine hooks are installed only when optional Javalin is present.
      }
      return (new MetaDataProducerMultiOutput());
   }
}
