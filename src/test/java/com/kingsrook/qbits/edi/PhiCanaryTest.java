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


import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsrook.qbits.edi.demo.EdiDemoServer;
import com.kingsrook.qbits.edi.flow.AckMonitor;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.flow.SendWorker;
import com.kingsrook.qbits.edi.metadata.EdiTables;
import com.kingsrook.qbits.edi.processes.EdiProcesses;
import com.kingsrook.qbits.edi.security.EdiClassifications;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadCrypto;
import com.kingsrook.qbits.edi.storage.PreviewDownloadBackend;
import com.kingsrook.qbits.edi.storage.RawDocumentDownload;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.HttpsRouteProvider;
import com.kingsrook.qbits.edi.transport.gateway.EdiFileGatewayConsumer;
import com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayOutbox;
import com.kingsrook.qbits.edi.transport.gateway.standin.InboundFile;
import com.kingsrook.qbits.edi.transport.gateway.standin.OutboxFileRef;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import io.javalin.Javalin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** End-to-end PHI egress scanner. Only explicit PHI columns may retain sentinels.
 ** RC.5 has no secured-data tier resolver; FULL exports and positional egress run here.
 *******************************************************************************/
class PhiCanaryTest
{
   private final List<String> markers = new ArrayList<>();
   private final ObjectMapper json = new ObjectMapper();

   /*******************************************************************************
    ** Every five-character window is enough to detect any longer leaked substring.
    *******************************************************************************/
   private void scan(String output, String channel)
   {
      for(String marker : markers)
      {
         for(int i = 0; i <= marker.length() - 5; i++)
         {
            String window = marker.substring(i, i + 5);
            int at = output.indexOf(window);
            assertThat(at).as("PHI substring in " + channel + (at < 0 ? "" : ": " + output.substring(0, Math.min(120, output.length())) + " ... " + output.substring(Math.max(0, at - 60), Math.min(output.length(), at + 80)))).isNegative();
         }
      }
   }

   /*******************************************************************************
    ** Exempt only values and display values of metadata-classified PHI record columns.
    ** Labels, messages, metadata, context and every other JSON property remain scanned.
    *******************************************************************************/
   private void exemptColumns(JsonNode node, String table)
   {
      if(node.isObject())
      {
         JsonNode records = node.get("records");
         if(records != null && records.isArray())
         {
            for(JsonNode record : records)
            {
               for(String key : List.of("values", "displayValues"))
               {
                  if(record.get(key) instanceof ObjectNode values)
                  {
                     for(var field : QContext.getQInstance().getTable(table).getFields().values())
                     {
                        var classification = field.getSupplementalMetaData("edi.classification");
                        if(classification instanceof EdiClassifications classified && classified.getClassification().equals("PHI"))
                        {
                           values.remove(field.getName());
                        }
                     }
                  }
               }
            }
         }
      }
      node.elements().forEachRemaining(child -> exemptColumns(child, table));
   }

   /*******************************************************************************
    ** Scan all properties of the real serialized response, with narrow column exceptions.
    *******************************************************************************/
   private void response(Object output, String table, String channel) throws Exception
   {
      JsonNode node = json.readTree(JsonUtils.toJson(output));
      if(table != null)
      {
         exemptColumns(node, table);
      }
      scan(node.toString(), channel);
   }

   /*******************************************************************************
    ** Force distinct field sentinels twice, across both ingress contracts and replays.
    *******************************************************************************/
   @Test
   void allAvailableEgress() throws Exception
   {
      try(EdiDemoServer demo = new EdiDemoServer(); HttpClient client = HttpClient.newHttpClient())
      {
         demo.seed();
         var instance = demo.defineQInstance();
         CapturedAlerts.outputs.clear();
         com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(CapturedAlerts.class));
         // A test-only widened diagnosis field allows the long marker through mapping;
         // its invalid code shape still exercises SNIP 5, and separate documents force SNIP 2.
         SystemWrites.call(() ->
         {
            for(QRecord segment : EdiRecords.query("edi_guide_segment", EdiRecords.equal("segmentId", "HI")))
            {
               for(QRecord element : EdiRecords.query("edi_guide_element", EdiRecords.equal("guideSegmentId", segment.getValue("id"))))
               {
                  SystemWrites.update("edi_guide_element", new QRecord().withValue("id", element.getValue("id")).withValue("maxLength", 80));
               }
            }
            SystemWrites.insert("edi_transport", new QRecord().withValue("name", "canary-ingress").withValue("type", "HTTPS").withValue("direction", "INBOUND")
               .withValue("isActive", true).withValue("httpsInboundAuth", "BEARER").withValue("httpsSecretEnvVar", "SYNTHETIC_HTTP_KEY"));
            return (null);
         });
         EdiRuntime.current().config().withSecretsResolver(name -> name.equals("SYNTHETIC_HTTP_KEY") ? "synthetic-auth" : Base64.getEncoder().encodeToString(new byte[32]));
         var provider = new HttpsRouteProvider();
         provider.setQInstance(instance);
         var server = Javalin.create(provider::acceptJavalinConfig).start(0);
         var capture = new CapturedLogs();
         var logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
         capture.start();
         logger.addAppender(capture);
         List<String> transports = new ArrayList<>();
         FileGatewayOutbox.register((endpoint, name, stream, size) ->
         {
            transports.add(new String(stream.readAllBytes(), StandardCharsets.US_ASCII));
            return (new OutboxFileRef(transports.size(), endpoint, name, name, size, null, Instant.now()));
         });
         try
         {
            int control = 800;
            List<QRecord> received = new ArrayList<>();
            for(int variant = 0; variant < 2; variant++)
            {
               List<String> fields = new ArrayList<>();
               for(int field = 0; field < 4; field++)
               {
                  String marker = "CANARY-PHI-" + UUID.randomUUID().toString().substring(0, 8).replaceAll("(.)", "Z$1");
                  fields.add(marker);
                  markers.add(marker);
               }
               for(String key : List.of("837P", "835"))
               {
                  String body = HealthcareSupport.body(key, 123).replace("Testpatient Alpha", fields.get(0))
                     .replaceAll("SYN[0-9]{6}", fields.get(1)).replaceAll("SYNCLM[0-9]{6}", fields.get(2));
                  if(key.equals("837P"))
                  {
                     body = body.replace("ABK:A123", "ABK:" + fields.get(3)).replace("*150***", "*150.01***").replace("SBR*P*18", "SBR*P*19");
                  }
                  else
                  {
                     body = body.replace("SVC*HC:S0001", "REF*DX*" + fields.get(3) + "~SVC*HC:S0001").replace("BPR*I*115", "BPR*I*115.01");
                  }
                  String raw = HealthcareSupport.interchange(key, ++control, body);
                  byte[] bytes = raw.getBytes(StandardCharsets.US_ASCII);
                  if(variant == 0)
                  {
                     var file = new InboundFile(control, "cedar-inbox", "synthetic-user", "synthetic-user", "127.0.0.1", "/", fields.get(1) + ".edi",
                        bytes.length, PayloadCrypto.digest(bytes), Instant.now(), null, null, () -> new ByteArrayInputStream(bytes));
                     response(new EdiFileGatewayConsumer(new InboundSink(EdiRuntime.current().payloads())).accept(file), null, "gateway reply");
                  }
                  else
                  {
                     var reply = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/qqq/hooks/edi/https/canary-ingress"))
                        .header("Authorization", "Bearer synthetic-auth").POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(), HttpResponse.BodyHandlers.ofString());
                     assertThat(reply.statusCode()).isEqualTo(202);
                     scan(reply.body() + reply.headers(), "HTTPS response");
                  }
                  assertThat(new InboxWorker().runOnce()).isEqualTo(1);
                  var transaction = rows("edi_transaction").stream().filter(r -> fields.get(2).equals(r.getValueString("phiBusinessKey")) || key.equals("835") && "SYNTRACE123".equals(r.getValueString("phiBusinessKey")))
                     .reduce((a, b) -> b).orElseThrow();
                  assertThat(transaction.getValueString("status")).isEqualTo("APPLIED");
                  received.add(transaction);
                  // Force a structural rejection with the same markers in the same fields.
                  HealthcareSupport.receive(key, ++control, body.replace(key.equals("837P") ? "SV1*HC:S0001*100" : "BPR*I*115.01", key.equals("837P") ? "SV1*HC:S0001*BAD" : "BPR*I*BAD"));
               }
            }
            assertThat(rows("edi_transaction_error")).extracting(r -> r.getValueInteger("snipLevel")).contains(2, 3, 4);
            QRecord first = received.getFirst();
            QRecord archived = rows("edi_interchange").stream().filter(r -> first.getValue("interchangeId").equals(r.getValue("id"))).findFirst().orElseThrow();
            QRecord sample = SystemWrites.call(() -> SystemWrites.insert("edi_map_sample", new QRecord().withValue("mapId", first.getValue("mapId"))
               .withValue("name", "Synthetic PHI preview").withValue("payloadRef", archived.getValue("payloadRef")).withValue("expectedResultJson", "{\"clinicalKey\":\"" + markers.getFirst() + "\"}")));
            process("edi_map_sample_preview", sample);
            for(QRecord transaction : received)
            {
               process("edi_transaction_reprocess", transaction);
               assertThat(new InboxWorker().runOnce()).isEqualTo(1);
            }
            for(QRecord exception : rows("edi_exception"))
            {
               process("edi_exception_resolve", exception);
            }
            while(new SendWorker().runOnce() > 0)
            {
               // Capture all eligible acknowledgement transports.
            }
            QRecord ack = rows("edi_outbound_message").stream().filter(r -> r.getValue("ackForGroupId") != null && "SENT".equals(r.getValueString("status"))).reduce((a, b) -> b).orElseThrow();
            process("edi_outbound_message_resend", ack);
            new SendWorker().runOnce();
            assertThat(transports).isNotEmpty();
            transports.forEach(payload -> scan(payload, "non-PHI acknowledgement transport"));
            // Actual overdue alerts, with complete serialized signals captured by the sink.
            QRecord group = rows("edi_functional_group").getFirst();
            SystemWrites.call(() -> { SystemWrites.update("edi_functional_group", new QRecord().withValue("id", group.getValue("id")).withValue("ackStatus", "PENDING").withValue("ackDueAt", Instant.now().minusSeconds(60))); return (null); });
            new AckMonitor().runOnce();
            assertThat(CapturedAlerts.outputs).isNotEmpty();
            CapturedAlerts.outputs.forEach(output -> scan(output, "alerts"));
            for(var table : instance.getTables().values())
            {
               if(table.getName().startsWith("edi_") && !table.getName().equals(PreviewDownloadBackend.TABLE))
               {
                  var output = new QueryAction().execute(new QueryInput(table.getName()).withFilter(new QQueryFilter()).withShouldFetchHeavyFields(true));
                  response(output, table.getName(), "export " + table.getName());
               }
            }
            int rendered = 0;
            for(var widget : instance.getWidgets().values())
            {
               var join = EdiTables.joins().stream().filter(j -> j.getName().equals(widget.getName())).findFirst();
               if(join.isEmpty())
               {
                  response(new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(widget)), null, widget.getName());
               }
               else
               {
                  for(QRecord parent : rows(join.get().getLeftTable()))
                  {
                     var params = new HashMap<>(Map.of("tableName", join.get().getLeftTable(), "id", parent.getValueString("id")));
                     response(new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(widget).withUrlParams(params)), join.get().getRightTable(), widget.getName());
                  }
               }
               rendered++;
            }
            assertThat(rendered).isEqualTo(instance.getWidgets().size());
            assertThat(capture.outputs).isNotEmpty();
            capture.outputs.forEach(output -> scan(output, "logs"));
            // Ciphertext contains no plaintext marker, including after decoding the BLOB.
            for(QRecord payload : rows("edi_payload"))
            {
               if(payload.getValue("ciphertext") instanceof byte[] bytes)
               {
                  scan(new String(bytes, StandardCharsets.ISO_8859_1), "ciphertext bytes");
               }
            }
            // Raw access is the explicit authorized escape: bytes are streamed, event is value-free.
            QRecord interchange = rows("edi_interchange").stream().filter(r -> received.getFirst().getValue("interchangeId").equals(r.getValue("id"))).findFirst().orElseThrow();
            var download = process("edi_interchange_download", interchange);
            var storage = new StorageInput(PreviewDownloadBackend.TABLE).withReference(download.getValueString("storageReference"));
            try(var stream = new PreviewDownloadBackend().getInputStream(storage))
            {
               assertThat(new String(stream.readAllBytes(), StandardCharsets.US_ASCII)).contains(markers.getFirst());
            }
            assertThat(rows("edi_event")).anyMatch(r -> "RAW_DOWNLOADED".equals(r.getValueString("type")) && r.getValueString("userName") != null);
            response(new QueryAction().execute(new QueryInput("edi_event").withFilter(new QQueryFilter())), "edi_event", "download audit");
            assertThatThrownBy(() -> RawDocumentDownload.open("raw:edi_transaction:1")).hasMessageContaining("not authorized");
            capture.outputs.forEach(output -> scan(output, "logs after raw download"));
            var fullSession = QContext.getQSession();
            try
            {
               QContext.setQSession(new com.kingsrook.qqq.backend.core.model.session.QSession());
               assertThatThrownBy(() -> new PreviewDownloadBackend().getInputStream(storage)).hasMessageContaining("not authorized");
               assertThatThrownBy(() -> RawDocumentDownload.open(storage.getReference())).hasMessageContaining("not authorized");
            }
            finally
            {
               QContext.setQSession(fullSession);
            }
            hiddenExportsAndWidgets();
            capture.outputs.forEach(output -> scan(output, "logs after HIDDEN exports and widgets"));
         }
         finally
         {
            logger.removeAppender(capture);
            capture.stop();
            server.stop();
         }
      }
   }

   /*******************************************************************************
    ** RC.5 HIDDEN field locks are real enforcement, not test-side JSON redaction.
    *******************************************************************************/
   private void hiddenExportsAndWidgets() throws Exception
   {
      var instance = QContext.getQInstance();
      var enforcement = instance.getEnforcePermissionsInCoreActions();
      instance.setEnforcePermissionsInCoreActions(true);
      var fullSession = QContext.getQSession();
      var viewer = new com.kingsrook.qqq.backend.core.model.session.QSession().withPermissions("edi.admin").withSecurityKeyValue("edi.admin", true).withSecurityKeyValue("edi.guide.loader", true);
      for(var table : instance.getTables().values())
      {
         if(!table.getName().startsWith("edi_") || table.getName().equals(PreviewDownloadBackend.TABLE))
         {
            continue;
         }
         viewer.withPermissions("edi." + table.getName().substring(4) + ".read");
         for(var field : table.getFields().values())
         {
            if(field.getSupplementalMetaData("edi.classification") instanceof EdiClassifications classified && classified.getClassification().equals("PHI"))
            {
               field.withFieldSecurityLock(new com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock().withSecurityKeyType("synthetic_phi_access"));
            }
         }
      }
      try
      {
         QContext.setQSession(viewer);
         for(var table : instance.getTables().values())
         {
            if(table.getName().startsWith("edi_") && !table.getName().equals(PreviewDownloadBackend.TABLE))
            {
               response(new QueryAction().execute(new QueryInput(table.getName()).withFilter(new QQueryFilter()).withShouldFetchHeavyFields(true)), null, "HIDDEN export " + table.getName());
            }
         }
         for(var widget : instance.getWidgets().values())
         {
            var join = EdiTables.joins().stream().filter(j -> j.getName().equals(widget.getName())).findFirst();
            var params = new HashMap<String, String>();
            if(join.isPresent())
            {
               var parents = rows(join.get().getLeftTable());
               assertThat(parents).as(join.get().getLeftTable()).isNotEmpty();
               params.put("tableName", join.get().getLeftTable());
               params.put("id", parents.getLast().getValueString("id"));
            }
            response(new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(widget).withUrlParams(params)), null, "HIDDEN " + widget.getName());
         }
      }
      finally
      {
         QContext.setQSession(fullSession);
         instance.setEnforcePermissionsInCoreActions(enforcement);
      }
   }

   /*******************************************************************************
    ** Exercise standard Review and Result paths with serialized outputs and summaries.
    *******************************************************************************/
   private RunBackendStepOutput process(String name, QRecord record) throws Exception
   {
      var input = new RunBackendStepInput().withProcessName(name).withRecords(List.of(record));
      var review = new EdiProcesses.Review();
      var preview = new RunBackendStepOutput();
      review.runOnePage(input, preview);
      response(Map.of("records", preview.getRecords(), "values", preview.getValues()), null, "Review records");
      response(review.getProcessSummary(preview, false), null, "Review summary");
      var execute = new EdiProcesses.Execute();
      var output = new RunBackendStepOutput();
      execute.runOnePage(input, output);
      response(Map.of("records", output.getRecords(), "values", output.getValues()), null, "Result records");
      response(execute.getProcessSummary(output, true), null, "Result summary");
      return (output);
   }

   /*******************************************************************************
    ** Actual QueryAction rows, including encrypted BLOBs.
    *******************************************************************************/
   private static List<QRecord> rows(String table) throws Exception
   {
      return (new QueryAction().execute(new QueryInput(table).withFilter(new QQueryFilter()).withShouldFetchHeavyFields(true)).getRecords());
   }

   /*******************************************************************************
    ** Capture complete alert payloads, not merely alert type names.
    *******************************************************************************/
   public static class CapturedAlerts implements com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface
   {
      static final List<String> outputs = new ArrayList<>();

      /*******************************************************************************
       ** Retain every serialized signal for the substring scanner.
       *******************************************************************************/
      public void raise(com.kingsrook.qqq.backend.core.alerts.AlertSignal signal)
      {
         outputs.add(JsonUtils.toJson(signal));
      }

      /*******************************************************************************
       ** Resolutions are egress too.
       *******************************************************************************/
      public void resolve(com.kingsrook.qqq.backend.core.alerts.AlertResolution resolution)
      {
         outputs.add(JsonUtils.toJson(resolution));
      }

      /*******************************************************************************
       ** Heartbeats contain only the operational key and time.
       *******************************************************************************/
      public void heartbeat(String key, Instant at)
      {
         outputs.add(key + at);
      }
   }

   /*******************************************************************************
    ** In-memory Log4j appender includes rendered throwable stacks in the scan.
    *******************************************************************************/
   private static class CapturedLogs extends AbstractAppender
   {
      private final List<String> outputs = new java.util.concurrent.CopyOnWriteArrayList<>();

      /*******************************************************************************
       ** No filesystem output or production logging reconfiguration.
       *******************************************************************************/
      private CapturedLogs()
      {
         super("phi-egress", null, PatternLayout.newBuilder().withPattern("%c %m%ex").build(), false, null);
      }

      /*******************************************************************************
       ** Capture the exact formatted event including parameter values and exceptions.
       *******************************************************************************/
      @Override
      public void append(LogEvent event)
      {
         outputs.add(new String(getLayout().toByteArray(event), StandardCharsets.UTF_8));
      }
   }
}
