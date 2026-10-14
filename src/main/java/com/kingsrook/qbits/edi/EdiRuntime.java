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


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import com.kingsrook.qbits.edi.ack.Ack997Builder;
import com.kingsrook.qbits.edi.ack.Ack999Builder;
import com.kingsrook.qbits.edi.ack.InboundAckReader;
import com.kingsrook.qbits.edi.ack.TA1Builder;
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.storage.DatabasePayloadStore;
import com.kingsrook.qbits.edi.storage.PayloadCrypto;
import com.kingsrook.qbits.edi.x12.ControlNumberAllocator;
import com.kingsrook.qbits.edi.x12.DuplicateDetector;
import com.kingsrook.qbits.edi.x12.X12EnvelopeReader;
import com.kingsrook.qbits.edi.x12.X12Writer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Per-QInstance service holder; metadata installation does not access the database.
 *******************************************************************************/
public final class EdiRuntime
{
   private static final QLogger LOG = QLogger.getLogger(EdiRuntime.class);
   private static final Map<QInstance, EdiRuntime> INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());
   private final X12EnvelopeReader reader;
   private final X12Writer writer = new X12Writer();
   private final GuideValidator validator;
   private final BuiltInGuideLoader guides = new BuiltInGuideLoader();
   private final com.kingsrook.qbits.edi.storage.PayloadStore payloads;
   private final EdiQBitConfig config;
   private final com.kingsrook.qbits.edi.map.TransformRegistry transforms = new com.kingsrook.qbits.edi.map.TransformRegistry();
   private final DuplicateDetector duplicates = new DuplicateDetector();
   private final ControlNumberAllocator controls;
   private final Ack997Builder ack997 = new Ack997Builder();
   private final Ack999Builder ack999 = new Ack999Builder();
   private final TA1Builder ta1 = new TA1Builder();
   private final InboundAckReader acknowledgements = new InboundAckReader();

   /*******************************************************************************
    ** Construct services without resolving keys until content is stored or opened.
    *******************************************************************************/
   private EdiRuntime(QInstance instance, EdiQBitConfig config)
   {
      this.config = config;
      validator = new GuideValidator(config);
      reader = new X12EnvelopeReader(config.getMaxSegmentsPerTransaction());
      payloads = "OBJECT_STORAGE".equals(config.getPayloadStore())
         ? new com.kingsrook.qbits.edi.storage.ObjectStoragePayloadStore(new PayloadCrypto(config.getSecretsResolver()), config.getPayloadBucketAlias(), config.getClock())
         : new DatabasePayloadStore(new PayloadCrypto(config.getSecretsResolver()));
      controls = instance.getBackend(config.getBackendName()) instanceof RDBMSBackendMetaData backend ? new ControlNumberAllocator(backend) : null;
   }

   /*******************************************************************************
    ** Install for a host instance.
    *******************************************************************************/
   public static void install(QInstance instance, EdiQBitConfig config)
   {
      INSTANCES.put(instance, new EdiRuntime(instance, config));
   }

   /*******************************************************************************
    ** Get services for the current QQQ context.
    *******************************************************************************/
   public static EdiRuntime current()
   {
      return (Objects.requireNonNull(INSTANCES.get(QContext.getQInstance()), "EDI runtime has not been installed"));
   }

   /*******************************************************************************
    ** Call at host startup after DDL, with the service session established.
    *******************************************************************************/
   public List<Integer> initialize() throws Exception
   {
      if("DATABASE".equals(config.getPayloadStore()))
      {
         LOG.warn("EDI database payload storage is enabled; object storage is recommended for production");
      }
      var ids = guides.load();
      com.kingsrook.qbits.edi.processes.OutboundProcesses.installSchedules();
      return (ids);
   }

   /*******************************************************************************
    ** Access reader.
    *******************************************************************************/
   public X12EnvelopeReader reader()
   {
      return (reader);
   }

   /*******************************************************************************
    ** Access writer.
    *******************************************************************************/
   public X12Writer writer()
   {
      return (writer);
   }

   /*******************************************************************************
    ** Access validator.
    *******************************************************************************/
   public GuideValidator validator()
   {
      return (validator);
   }

   /*******************************************************************************
    ** Access guides.
    *******************************************************************************/
   public BuiltInGuideLoader guides()
   {
      return (guides);
   }

   /*******************************************************************************
    ** Access payloads.
    *******************************************************************************/
   public com.kingsrook.qbits.edi.storage.PayloadStore payloads()
   {
      return (payloads);
   }

   /*******************************************************************************
    ** Access duplicates.
    *******************************************************************************/
   public DuplicateDetector duplicates()
   {
      return (duplicates);
   }

   /*******************************************************************************
    ** Access controls.
    *******************************************************************************/
   public ControlNumberAllocator controls()
   {
      return (Objects.requireNonNull(controls, "Control allocation requires an RDBMS backend"));
   }

   /*******************************************************************************
    ** Access ack997.
    *******************************************************************************/
   public Ack997Builder ack997()
   {
      return (ack997);
   }

   /*******************************************************************************
    ** Access ack999.
    *******************************************************************************/
   public Ack999Builder ack999()
   {
      return (ack999);
   }

   /*******************************************************************************
    ** Access ta1.
    *******************************************************************************/
   public TA1Builder ta1()
   {
      return (ta1);
   }

   /*******************************************************************************
    ** Access acknowledgements.
    *******************************************************************************/
   public InboundAckReader acknowledgements()
   {
      return (acknowledgements);
   }

   /*******************************************************************************
    ** Installed host configuration.
    *******************************************************************************/
   public EdiQBitConfig config()
   {
      return (config);
   }

   /*******************************************************************************
    ** Host-extensible inbound transformations.
    *******************************************************************************/
   public com.kingsrook.qbits.edi.map.TransformRegistry transforms()
   {
      return (transforms);
   }
}
