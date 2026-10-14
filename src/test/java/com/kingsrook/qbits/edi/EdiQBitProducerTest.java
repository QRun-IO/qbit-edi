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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfigValidationException;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit tests for the EDI qbit producer and configuration.
 *******************************************************************************/
class EdiQBitProducerTest
{

   /*******************************************************************************
    ** An instance with a memory backend.
    *******************************************************************************/
   private static QInstance instance()
   {
      QInstance instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      return (instance);
   }



   /*******************************************************************************
    ** Identity, defaults and the enabled flag.
    *******************************************************************************/
   @Test
   void testIdentityAndDefaults()
   {
      EdiQBitProducer producer = new EdiQBitProducer().withNamespace("ns");
      assertThat(producer.isEnabled()).isFalse();

      EdiQBitConfig config = new EdiQBitConfig().withBackendName("memory").withParentAppName("parent");
      producer.withQBitConfig(config);
      assertThat(producer.isEnabled()).isTrue();
      assertThat(producer.getQBitConfig()).isSameAs(config);
      assertThat(producer.getNamespace()).isEqualTo("ns");
      assertThat(producer.getQBitMetaData().getArtifactId()).isEqualTo("qbit-edi");
      assertThat(producer.getPackageNameForFindingMetaDataProducers()).isEqualTo("com.kingsrook.qbits.edi.metadata");
      assertThat(config.getAppName()).isEqualTo("edi");
      assertThat(config.getAppLabel()).isEqualTo("EDI");
      assertThat(config.getParentAppName()).isEqualTo("parent");
      assertThat(config.getBackendName()).isEqualTo("memory");
      assertThat(config.getDefaultBackendNameForTables()).isEqualTo("memory");
      assertThat(config.withAppLabel("Trading").getAppLabel()).isEqualTo("Trading");
   }



   /*******************************************************************************
    ** Validation collects every error.
    *******************************************************************************/
   @Test
   void testValidation()
   {
      List<String> errors = new ArrayList<>();
      new EdiQBitConfig().withAppName(" ").validate(instance(), errors);
      assertThat(errors).containsExactly("backendName must name an existing backend", "appName is required");

      errors.clear();
      new EdiQBitConfig().withAppName(null).withBackendName("memory").validate(instance(), errors);
      assertThat(errors).containsExactly("appName is required");
   }



   /*******************************************************************************
    ** Produce installs the qbit on a valid instance and refuses a bad config.
    *******************************************************************************/
   @Test
   void testProduce() throws Exception
   {
      QInstance instance = instance();
      new EdiQBitProducer().withQBitConfig(new EdiQBitConfig().withBackendName("memory")).produce(instance);
      assertThat(instance.getQBits().values()).anyMatch(q -> "qbit-edi".equals(q.getArtifactId()));

      assertThatThrownBy(() -> new EdiQBitProducer().withQBitConfig(new EdiQBitConfig()).produce(instance()))
         .isInstanceOf(QBitConfigValidationException.class);
   }
}
