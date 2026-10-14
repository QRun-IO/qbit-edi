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
import com.kingsrook.qbits.edi.EdiQBitConfig;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.map.MapPreview;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;

/*******************************************************************************
 ** Standard streamed-ETL record/table actions, with Review and Result summaries.
 *******************************************************************************/
public final class EdiProcesses
{
   /*******************************************************************************
    ** Processes are table actions and never app navigation entries.
    *******************************************************************************/
   public static List<QProcessMetaData> processes(EdiQBitConfig config)
   {
      List<QProcessMetaData> result = new ArrayList<>();
      for(String table : List.of("edi_inbox", "edi_interchange", "edi_transaction", "edi_exception"))
      {
         QProcessMetaData process = action(table, "reprocess", "Reprocess", "edi.reprocess");
         process.getFrontendStep("review").withFormField(new QFieldMetaData("useCurrentRevisions", QFieldType.BOOLEAN).withLabel("Use current guide and map revisions").withDefaultValue(false));
         result.add(process);
      }
      for(String table : List.of("edi_map", "edi_map_sample"))
      {
         result.add(action(table, "preview", "Preview Map", "edi.map.preview"));
      }
      result.add(action("edi_exception", "assign", "Assign", "edi.exception.assign"));
      result.add(action("edi_exception", "ignore", "Ignore", "edi.exception.ignore"));
      result.add(action("edi_exception", "resolve", "Mark Resolved", "edi.exception.resolve"));
      result.add(action("edi_guide", "override", "Create Partner Override", "edi.guide.override"));
      result.add(action("edi_guide", "activate", "Activate Guide", "edi.guide.activate"));
      result.add(action("edi_map", "activate", "Activate Map", "edi.map.activate"));
      QProcessMetaData scheduled = new QProcessMetaData().withName("edi_inbox_worker").withLabel("Process Inbox").withIsHidden(true)
         .withPermissionRules(permission("edi.admin")).withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReference(WorkerStep.class)));
      if(config.getSchedulerName() != null)
      {
         scheduled.withSchedule(new QScheduleMetaData().withSchedulerName(config.getSchedulerName()).withRepeatSeconds(15).withInitialDelaySeconds(15));
      }
      result.add(scheduled);
      for(String table : List.of("edi_outbound_message", "edi_interchange"))
      {
         result.add(action(table, "resend", "Resend", "edi.resend"));
         result.add(action(table, "new", "Resend As New", "edi.resend"));
         result.add(action(table, "cancel", "Cancel", "edi.cancel"));
      }
      result.add(action("edi_transport", "test", "Test Connection", "edi.transport.test"));
      result.add(action("edi_certificate", "promote", "Promote Certificate", "edi.certificate.promote"));
      result.add(action("edi_certificate", "export", "Export Public Certificate", "edi.certificate.read"));
      for(String table : config.getSendAsEdiTables())
      {
         result.add(action(table, "send", "Send As EDI", "edi.send"));
      }
      for(String name : List.of("edi_outbound_worker", "edi_send_worker", "edi_as2_mdn_sender", "edi_ack_monitor", "edi_outbound_enqueue"))
      {
         result.add(OutboundProcesses.process(name, config, name.equals("edi_outbound_enqueue") ? null : name.equals("edi_ack_monitor") ? 300 : 15, null));
      }
      return (result);
   }

   /*******************************************************************************
    ** Full-name process permissions.
    *******************************************************************************/
   private static QPermissionRules permission(String name)
   {
      return (new QPermissionRules().withPermissionBaseName(name).withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
   }

   /*******************************************************************************
    ** Use the platform's extraction, Review, confirmation and Result contract.
    *******************************************************************************/
   private static QProcessMetaData action(String table, String operation, String label, String permission)
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, table);
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, table);
      values.put(StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION, true);
      values.put(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(ExtractViaQueryStep.class, Review.class, Execute.class, values)
         .withName(table + "_" + operation).withLabel(label).withTableName(table).withIsHidden(true).withMinInputRecords(1).withPermissionRules(permission(permission));
      if(operation.equals("preview"))
      {
         if(table.equals("edi_map"))
         {
            process.getFrontendStep("review").withFormField(new QFieldMetaData("sampleId", QFieldType.INTEGER)
               .withLabel("Sample (blank previews all samples)").withPossibleValueSourceName("edi_map_sample"));
         }
         process.getFrontendStep("review").withFormField(new QFieldMetaData("sampleUpload", QFieldType.BLOB).withLabel("Upload synthetic sample (optional)")
            .withFieldAdornment(com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType.FileUploadAdornment.newFieldAdornment()));
      }
      if(operation.equals("preview") || operation.equals("export"))
      {
         process.getFrontendStep("result").withComponent(new com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData()
            .withType(com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType.DOWNLOAD_FORM));
         for(String field : List.of("downloadFileName", "storageTableName", "storageReference"))
         {
            process.getBackendStep("execute").getOutputMetaData().withField(new QFieldMetaData(field, QFieldType.STRING));
            process.getFrontendStep("result").withViewField(new QFieldMetaData(field, QFieldType.STRING));
         }
      }
      if(operation.equals("assign"))
      {
         process.getFrontendStep("review").withFormField(new QFieldMetaData("assignedToUserId", QFieldType.STRING).withLabel("Assign to user ID").withIsRequired(true));
      }
      if(operation.equals("override"))
      {
         process.getFrontendStep("review").withFormField(new QFieldMetaData("partnerId", QFieldType.INTEGER).withPossibleValueSourceName("edi_partner").withIsRequired(true));
      }
      return (process);
   }

   /*******************************************************************************
    ** Summaries use stable wording and record ids, never payload values.
    *******************************************************************************/
   public static class Review extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private int count;

      /*******************************************************************************
       ** Forward selected rows without mutating data.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output)
      {
         count += input.getRecords().size();
         output.getRecords().addAll(input.getRecords());
      }

      /*******************************************************************************
       ** Standard Review summary.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return (new ArrayList<>(List.of(new ProcessSummaryLine(Status.INFO, count, "selected records will be processed"))));
      }
   }

   /*******************************************************************************
    ** Execute after Review; re-read selected records before changing workflow state.
    *******************************************************************************/
   public static class Execute extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private final ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();

      /*******************************************************************************
       ** Mutations use the current user's selection and internal audited writes.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String name = input.getProcessName();
         String table = name.substring(0, name.lastIndexOf('_'));
         String operation = name.substring(name.lastIndexOf('_') + 1);
         for(QRecord selected : input.getRecords())
         {
            try
            {
               var query = new com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput(table).withFilter(EdiRecords.equal("id", selected.getValue("id"))).withShouldFetchHeavyFields(true);
               query.setPermissionEnforcement(com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement.ENFORCE);
               QRecord row = new com.kingsrook.qqq.backend.core.actions.tables.QueryAction().execute(query).getRecords().getFirst();
               if(operation.equals("promote") && !"NEXT".equals(row.getValueString("status")))
               {
                  lines.add(new ProcessSummaryLine(Status.WARNING, 1, "only a NEXT certificate can be promoted"));
                  continue;
               }
               if(operation.equals("export"))
               {
                  lines.add(new ProcessSummaryLine(Status.OK, 1, "public certificate exported"));
               }
               else if(operation.equals("preview"))
               {
                  QRecord map = table.equals("edi_map") ? row : EdiRecords.query("edi_map", EdiRecords.equal("id", row.getValue("mapId"))).getFirst();
                  List<QRecord> samples = table.equals("edi_map_sample") ? List.of(row) : EdiRecords.query("edi_map_sample", EdiRecords.equal("mapId", row.getValue("id")));
                  if(input.getValue("sampleUpload") instanceof List<?> uploads && !uploads.isEmpty())
                  {
                     samples = new ArrayList<>();
                     for(Object upload : uploads)
                     {
                        if(!(upload instanceof com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput storage)
                           || !com.kingsrook.qbits.edi.storage.PreviewDownloadBackend.TABLE.equals(storage.getTableName())
                           || !com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload.isStorageRegistered(storage))
                        {
                           throw new IllegalArgumentException("Use the encrypted EDI upload archive for synthetic samples");
                        }
                        samples.add(SystemWrites.call(() -> SystemWrites.insert("edi_map_sample", new QRecord().withValue("mapId", map.getValue("id"))
                           .withValue("name", "Synthetic upload").withValue("payloadRef", storage.getReference()))));
                     }
                  }
                  else if(input.getValueInteger("sampleId") != null)
                  {
                     samples = samples.stream().filter(s -> input.getValueInteger("sampleId").equals(s.getValueInteger("id"))).toList();
                  }
                  if(samples.isEmpty())
                  {
                     throw new IllegalArgumentException("No matching synthetic sample");
                  }
                  for(QRecord sample : samples)
                  {
                     lines.addAll(new MapPreview().preview(map, sample));
                     SystemWrites.call(() ->
                     {
                        SystemWrites.update("edi_map_sample", new QRecord().withValue("id", sample.getValue("id"))
                           .withValue("lastPreviewAt", EdiRuntime.current().config().getClock().instant()).withValue("lastPreviewStatus", "SUCCESS"));
                        return (null);
                     });
                  }
               }
               else
               {
                  String user = QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference();
                  SystemWrites.call(() -> SystemWrites.transaction(() ->
                  {
                     if(operation.equals("reprocess"))
                     {
                        reprocess(table, row, Boolean.TRUE.equals(input.getValueBoolean("useCurrentRevisions")));
                     }
                     else if(List.of("resend", "new", "cancel").contains(operation))
                     {
                        com.kingsrook.qbits.edi.flow.OutboundActions.execute(table, row, operation);
                     }
                     else if(operation.equals("send"))
                     {
                        com.kingsrook.qbits.edi.flow.OutboundEnqueue.enqueue(table, row, "MANUAL", null);
                     }
                     else if(operation.equals("test"))
                     {
                        var test = com.kingsrook.qbits.edi.transport.EdiTransportPluginsMetaData.current().get(row.getValueString("type"))
                           .test(row.toEntity(com.kingsrook.qbits.edi.model.EdiTransport.class));
                        lines.add(new ProcessSummaryLine(Boolean.TRUE.equals(test.success()) ? Status.OK : Status.ERROR, 1, test.summary()));
                     }
                     else if(operation.equals("promote"))
                     {
                        new com.kingsrook.qbits.edi.as2.CertificateService().promote(row);
                     }
                     else if(List.of("assign", "ignore", "resolve").contains(operation))
                     {
                        QRecord update = new QRecord().withValue("id", row.getValue("id")).withValue("status", operation.equals("assign") ? "IN_PROGRESS" : operation.equals("ignore") ? "IGNORED" : "RESOLVED");
                        if(operation.equals("assign"))
                        {
                           update.withValue("assignedToUserId", input.getValue("assignedToUserId"));
                        }
                        else
                        {
                           update.withValue("resolvedBy", user).withValue("resolvedAt", EdiRuntime.current().config().getClock().instant());
                        }
                        SystemWrites.update(table, update);
                     }
                     else
                     {
                        new RevisionActions().execute(table, row, operation, input.getValueInteger("partnerId"));
                     }
                     return (null);
                  }));
                  lines.add(new ProcessSummaryLine(Status.OK, 1, "record processed"));
               }
               if(operation.equals("preview") || operation.equals("export"))
               {
                  boolean export = operation.equals("export");
                  String content = export ? row.getValueString("certificatePem") : new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(lines);
                  var ref = SystemWrites.call(() -> EdiRuntime.current().payloads().put(com.kingsrook.qbits.edi.storage.PayloadKind.SAMPLE,
                     new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
                  var storage = new com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput(com.kingsrook.qbits.edi.storage.PreviewDownloadBackend.TABLE).withReference(ref.value());
                  com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload.registerStorage(storage);
                  output.addValue("downloadFileName", export ? row.getValueString("alias").replaceAll("[^A-Za-z0-9._-]", "-") + ".pem" : "edi-map-preview.json");
                  output.addValue("storageTableName", storage.getTableName());
                  output.addValue("storageReference", ref.value());
               }
               output.getRecords().add(selected);
            }
            catch(Exception e)
            {
               lines.add(new ProcessSummaryLine(Status.ERROR, 1, "record could not be processed; check configuration and permissions"));
            }
         }
      }

      /*******************************************************************************
       ** Queue parsing from the original encrypted inbox; never reset a live lease.
       *******************************************************************************/
      private static void reprocess(String table, QRecord row, boolean current) throws Exception
      {
         Integer inboxId = table.equals("edi_inbox") ? row.getValueInteger("id") : row.getValueInteger("inboxId");
         if(inboxId == null && row.getValueInteger("interchangeId") != null)
         {
            inboxId = EdiRecords.query("edi_interchange", EdiRecords.equal("id", row.getValue("interchangeId"))).getFirst().getValueInteger("inboxId");
         }
         if(inboxId == null || !SystemWrites.guard("UPDATE edi_inbox SET id=id WHERE id=? AND status <> 'PROCESSING'", inboxId))
         {
            throw new IllegalArgumentException("No replayable inbox or message is processing");
         }
         SystemWrites.update("edi_inbox", new QRecord().withValue("id", inboxId).withValue("status", "PENDING").withValue("attempts", 0)
            .withValue("claimedAt", null).withValue("nextAttemptAt", EdiRuntime.current().config().getClock().instant()).withValue("reprocess", true).withValue("useCurrentRevisions", current));
      }

      /*******************************************************************************
       ** Standard Result summary.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return (lines);
      }
   }

   /*******************************************************************************
    ** Framework-managed polling entry point.
    *******************************************************************************/
   public static class WorkerStep implements BackendStep
   {
      /*******************************************************************************
       ** Run one bounded worker pass.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         output.addValue("processed", new InboxWorker().runOnce());
      }
   }
}
