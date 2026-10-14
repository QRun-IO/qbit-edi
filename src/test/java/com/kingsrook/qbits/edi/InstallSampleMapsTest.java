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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import com.kingsrook.qbits.edi.map.SampleMaps;
import com.kingsrook.qbits.edi.processes.InstallSampleMapsProcess;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** The "Install sample maps" table action on edi_map: standard streamed ETL,
 ** insert-only by name, installing inactive and unbound drafts.
 *******************************************************************************/
class InstallSampleMapsTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Every bundled sample is listed, and every listed sample exists.
    *******************************************************************************/
   @Test
   void theListMatchesTheBundledFiles()
   {
      String[] files = new File("src/main/resources/edi/maps").list((dir, name) -> name.endsWith("-sample.json"));
      assertThat(Arrays.stream(files).map(name -> name.replace("-sample.json", "")).toList()).containsExactlyInAnyOrderElementsOf(SampleMaps.RESOURCES);
   }

   /*******************************************************************************
    ** A standard streamed-ETL table action that needs no selected records.
    *******************************************************************************/
   @Test
   void isAStandardTableAction() throws Exception
   {
      QInstance        instance = H2Support.create();
      QProcessMetaData process  = instance.getProcess(InstallSampleMapsProcess.NAME);
      assertThat(process).isNotNull();
      assertThat(process.getTableName()).isEqualTo("edi_map");
      assertThat(process.getMinInputRecords()).isEqualTo(0);
      assertThat(process.getPermissionRules().getPermissionBaseName()).isEqualTo("edi.admin");
      assertThat(process.getStepList()).extracting(step -> step.getName()).contains("review", "result");
   }

   /*******************************************************************************
    ** Installs every sample as an inactive, unbound draft with its rules; a
    ** second run adds nothing; an edited map is kept; a deleted one comes back.
    *******************************************************************************/
   @Test
   void installsDraftsInsertOnly() throws Exception
   {
      H2Support.create();
      EdiRuntime.current().initialize();
      assertThat(count("edi_map")).isZero();

      run();
      assertThat(count("edi_map")).isEqualTo(SampleMaps.RESOURCES.size());
      List<QRecord> maps = SystemWrites.query("edi_map", new QQueryFilter());
      assertThat(maps).allSatisfy(map ->
      {
         assertThat(map.getValueString("status")).as(map.getValueString("name")).isEqualTo("DRAFT");
         assertThat(map.getValueInteger("revision")).isZero();
         assertThat(map.getValue("partnerId")).as("unbound to a partner").isNull();
         assertThat(map.getValue("guideId")).as("bound to the built-in guide for its document").isNotNull();
      });
      assertThat(count("edi_agreement_document")).as("bound to no agreement document").isZero();

      QRecord sample850 = maps.stream().filter(map -> "Sample 850 004010".equals(map.getValueString("name"))).findFirst().orElseThrow();
      int     rules     = count("edi_map_rule");
      assertThat(EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", sample850.getValue("id")))).hasSize(5);
      assertThat(rules).isGreaterThan(SampleMaps.RESOURCES.size());

      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_map", new QRecord().withValue("id", sample850.getValue("id")).withValue("tableName", "my_orders").withValue("status", "ACTIVE"));
         return (null);
      });
      run();
      assertThat(count("edi_map")).isEqualTo(SampleMaps.RESOURCES.size());
      assertThat(count("edi_map_rule")).isEqualTo(rules);
      QRecord edited = EdiRecords.query("edi_map", EdiRecords.equal("id", sample850.getValue("id"))).getFirst();
      assertThat(edited.getValueString("tableName")).isEqualTo("my_orders");
      assertThat(edited.getValueString("status")).isEqualTo("ACTIVE");

      SystemWrites.call(() ->
      {
         new com.kingsrook.qqq.backend.core.actions.tables.DeleteAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput("edi_map_rule")
            .withQueryFilter(new QQueryFilter().withCriteria("mapId", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, sample850.getValue("id"))));
         new com.kingsrook.qqq.backend.core.actions.tables.DeleteAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput("edi_map")
            .withPrimaryKey(sample850.getValue("id")));
         return (null);
      });
      assertThat(SampleMaps.RESOURCES).hasSize(32);
      assertThat(SystemWrites.call(SampleMaps::missing)).containsExactly("850-004010");
      run();
      assertThat(count("edi_map")).isEqualTo(SampleMaps.RESOURCES.size());
      assertThat(EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", EdiRecords.query("edi_map", EdiRecords.equal("name", "Sample 850 004010")).getFirst().getValue("id")))).hasSize(5);
   }

   /*******************************************************************************
    ** A host that already has a map of the same name keeps it, and gets no copy.
    *******************************************************************************/
   @Test
   void neverTouchesAMapTheHostAlreadyHas() throws Exception
   {
      H2Support.create();
      EdiRuntime.current().initialize();
      SystemWrites.call(() -> SystemWrites.insert("edi_map", new QRecord().withValue("name", "Sample 850 004010").withValue("direction", "INBOUND")
         .withValue("transactionSet", "850").withValue("version", "004010").withValue("status", "ACTIVE").withValue("revision", 3).withValue("tableName", "host_orders")));
      run();
      List<QRecord> same = EdiRecords.query("edi_map", EdiRecords.equal("name", "Sample 850 004010"));
      assertThat(same).hasSize(1);
      assertThat(same.getFirst().getValueString("tableName")).isEqualTo("host_orders");
      assertThat(same.getFirst().getValueString("status")).isEqualTo("ACTIVE");
      assertThat(count("edi_map")).isEqualTo(SampleMaps.RESOURCES.size());
      assertThat(count("edi_map_rule")).as("no rules were added to the host's map").isEqualTo(
         SampleMaps.RESOURCES.stream().filter(r -> !r.equals("850-004010")).mapToInt(InstallSampleMapsTest::ruleCount).sum());
   }

   /*******************************************************************************
    ** Run the real process through the standard runner (frontend steps skipped).
    *******************************************************************************/
   private void run() throws Exception
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(InstallSampleMapsProcess.NAME);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      new RunProcessAction().execute(input);
   }

   /*******************************************************************************
    ** The number of rules a bundled sample carries.
    *******************************************************************************/
   private static int ruleCount(String resource)
   {
      try
      {
         return (SampleMaps.read(resource).path("rules").size());
      }
      catch(java.io.IOException e)
      {
         throw (new IllegalStateException(e));
      }
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   private int count(String table) throws Exception
   {
      return (SystemWrites.count(table, new QQueryFilter()));
   }
}
