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


import com.kingsrook.qqq.backend.core.actions.help.HelpTargetCatalog;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.testing.HelpBundleAssertions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Coverage of the qbit's own help, independent of host integrations.
 *******************************************************************************/
class EdiHelpBundleTest
{
   /*******************************************************************************
    ** Every owned target resolves and has help, even without a help table.
    *******************************************************************************/
   @Test
   void bundleIsComplete() throws Exception
   {
      QInstance instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      new EdiQBitProducer().withQBitConfig(new EdiQBitConfig().withBackendName("memory")).produce(instance);
      assertThat(instance.getTable("helpContent")).isNull();
      assertThat(HelpBundles.of(instance).getBundles()).singleElement().satisfies(bundle ->
      {
         assertThat(bundle.getId()).isEqualTo("qbit-edi");
         assertThat(bundle.getVersion()).as("the help bundle carries the artifact version").isEqualTo(System.getProperty("qbit.artifactVersion"));
      });
      HelpBundles.syncOnStartup(instance);
      HelpBundleAssertions.forInstance(instance)
         .bundles("help/qbit-edi.yaml")
         .owning(target -> target.name().equals("edi") || target.name().startsWith("edi_"))
         .assertComplete();
      var keys = HelpBundles.of(instance).getBundles().getFirst().getEntries().stream().map(entry -> entry.getKey()).toList();
      // Core's gate currently requires apps, tables, processes and fields; also cover our widgets and value sources.
      for(var target : HelpTargetCatalog.collect(instance))
      {
         if(target.name().startsWith("edi_") && java.util.List.of("widget", "possibleValueSource").contains(target.kind()))
         {
            assertThat(keys).as(target.key()).contains(target.key());
         }
      }
   }


   /*******************************************************************************
    ** Help-enabled hosts use core sync, and a configured app name is respected.
    *******************************************************************************/
   @Test
   void registersForHelpEnabledHost() throws Exception
   {
      QInstance instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      instance.addTable(new com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData().withName("helpContent"));
      new EdiQBitProducer().withQBitConfig(new EdiQBitConfig().withBackendName("memory").withAppName("trading")).produce(instance);
      var entries = HelpBundles.of(instance).getBundles().getFirst().getEntries();
      assertThat(entries).anyMatch(entry -> "app:trading;slot:help".equals(entry.getKey()))
         .noneMatch(entry -> "app:edi;slot:help".equals(entry.getKey()));
      assertThat(instance.getTable("edi_partner").getHelpContent()).isNullOrEmpty();
      assertThat(instance.getApp("trading").getHelpContent()).isNullOrEmpty();
   }

}
