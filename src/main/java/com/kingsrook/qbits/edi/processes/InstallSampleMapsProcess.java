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

package com.kingsrook.qbits.edi.processes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.map.SampleMaps;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;

/*******************************************************************************
 ** "Install sample maps": a table action on edi_map that adds the bundled
 ** synthetic sample maps whose name does not exist yet. Standard streamed ETL:
 ** Review lists what will be added, Result what was added. Insert-only by map
 ** name, so existing maps and edits are never touched. Installed maps are
 ** drafts with no partner and no agreement document, so nothing changes how a
 ** document is processed until an administrator activates and binds one.
 *******************************************************************************/
public final class InstallSampleMapsProcess
{
   public static final String NAME = "edi_map_installSamples";

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private InstallSampleMapsProcess()
   {
   }

   /*******************************************************************************
    ** The process: a table-level action (no records need to be selected).
    *******************************************************************************/
   public static QProcessMetaData process()
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, "edi_map");
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, "edi_map");
      values.put(StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION, false);
      values.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE,
         "These sample maps will be added as drafts, with no partner and no agreement document. Maps that already exist are not changed.");
      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(Extract.class, Transform.class, Load.class, values);
      process.setName(NAME);
      process.setLabel("Install sample maps");
      process.setIcon(new QIcon("playlist_add"));
      process.setTableName("edi_map");
      process.setIsHidden(true);
      process.setMinInputRecords(0);
      process.setPermissionRules(new QPermissionRules().withPermissionBaseName("edi.admin").withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      return (process);
   }

   /*******************************************************************************
    ** The samples still to install, as the processing user.
    *******************************************************************************/
   static List<String> missing() throws QException
   {
      return (SystemWrites.call(SampleMaps::missing));
   }

   /*******************************************************************************
    ** A summary line with the standard four wordings.
    *******************************************************************************/
   private static ProcessSummaryLine line(Status status, String future, String futurePlural, String past, String pastPlural)
   {
      ProcessSummaryLine line = new ProcessSummaryLine(status, 0, null);
      line.setSingularFutureMessage(future);
      line.setPluralFutureMessage(futurePlural);
      line.setSingularPastMessage(past);
      line.setPluralPastMessage(pastPlural);
      return (line);
   }

   /*******************************************************************************
    ** Extract: the missing samples, computed (no query of selected records).
    *******************************************************************************/
   public static class Extract extends AbstractExtractStep
   {
      /*******************************************************************************
       ** How many samples will be added.
       *******************************************************************************/
      @Override
      public Integer doCount(RunBackendStepInput input) throws QException
      {
         return (missing().size());
      }

      /*******************************************************************************
       ** One record per missing sample.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(String resource : missing())
         {
            QRecord map = SystemWrites.call(() -> SampleMaps.mapRecord(resource));
            getRecordPipe().addRecord(map.withValue("sample", resource));
         }
      }
   }

   /*******************************************************************************
    ** Transform: forward the records and count them by standard and direction.
    *******************************************************************************/
   public static class Transform extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private final Map<String, ProcessSummaryLine> byKind = new LinkedHashMap<>();

      /*******************************************************************************
       ** Count by standard and direction.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord record : input.getRecords())
         {
            String kind = record.getValueString("standard") + " " + record.getValueString("direction").toLowerCase();
            byKind.computeIfAbsent(kind, k -> line(Status.OK, k + " sample map will be added as a draft", k + " sample maps will be added as drafts",
               k + " sample map was added as a draft", k + " sample maps were added as drafts")).incrementCount();
            output.getRecords().add(record);
         }
      }

      /*******************************************************************************
       ** Standard Review summary.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         byKind.values().forEach(line -> line.addSelfToListIfAnyCount(lines));
         if(lines.isEmpty())
         {
            lines.add(new ProcessSummaryLine(Status.INFO, null, "Every sample map is already installed."));
         }
         return (lines);
      }
   }

   /*******************************************************************************
    ** Load: insert each map and its rules as the processing user.
    *******************************************************************************/
   public static class Load extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private final ProcessSummaryLine added = line(Status.OK, "map will be added", "maps will be added", "map was added as a draft", "maps were added as drafts");

      /*******************************************************************************
       ** One transaction per map, so a map never lands without its rules.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord record : input.getRecords())
         {
            Integer id = SystemWrites.call(() -> SystemWrites.transaction(() -> SampleMaps.install(record.getValueString("sample"))));
            if(id != null)
            {
               added.incrementCountAndAddPrimaryKey(id);
               output.getRecords().add(new QRecord().withValue("id", id));
            }
         }
      }

      /*******************************************************************************
       ** Standard Result summary.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         added.addSelfToListIfAnyCount(lines);
         return (lines);
      }
   }
}
