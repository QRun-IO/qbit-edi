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

package com.kingsrook.qbits.edi.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.metadata.EdiTables;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Fresh application boots Next UI and serves seeded tables and typed home widgets.
 *******************************************************************************/
class EdiDemoServerTest
{
   /*******************************************************************************
    ** The configured runtime is reachable through child-table sourceQBit metadata.
    *******************************************************************************/
   @Test
   void serializesConfigurationWithoutRuntimeServices() throws Exception
   {
      try(EdiDemoServer demo = new EdiDemoServer())
      {
         var calls = new java.util.concurrent.atomic.AtomicInteger();
         var config = EdiRuntime.current().config().withServiceSessionSupplier(() ->
         {
            calls.incrementAndGet();
            return (new QSystemUserSession(demo.defineQInstance()));
         }).withSecretsResolver(name ->
         {
            calls.incrementAndGet();
            return ("synthetic-secret-must-not-serialize");
         }).withPayloadStore("OBJECT_STORAGE").withClock(java.time.Clock.systemUTC());
         String json = JsonUtils.toJson(config);
         assertThat(json).contains("sendAsEdiTables", "appName", "edi").doesNotContain(
            "clock", "SystemClock", "secretsResolver", "serviceSessionSupplier", "payloadStore", "synthetic-secret", "Lambda", "plugins");
         Set<String> properties = new HashSet<>();
         new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).fieldNames().forEachRemaining(properties::add);
         assertThat(properties).isSubsetOf("sendAsEdiTables", "defaultBackendNameForTables", "backendName", "appName", "appLabel", "parentAppName",
            "schedulerName", "maxSegmentsPerTransaction", "partnerLocks", "payloadBucketAlias", "esbDestinationName");
         assertThat(calls.get()).isZero();
      }
   }



   /*******************************************************************************
    ** Full seeded application acceptance on an operating-system assigned port.
    *******************************************************************************/
   @Test
   void bootsSeedsAndServes() throws Exception
   {
      try(EdiDemoServer demo = new EdiDemoServer(); HttpClient client = HttpClient.newHttpClient())
      {
         demo.seed();
         demo.seed();
         assertThat(demo.defineQInstance().getHomeAppName()).isEqualTo("edi");
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_partner", new QQueryFilter()))).hasSize(6);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_certificate", new QQueryFilter()))).hasSize(5);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_inbox", new QQueryFilter()))).hasSizeGreaterThanOrEqualTo(40);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_transaction", new QQueryFilter()))).anyMatch(row -> "OUTBOUND".equals(row.getValueString("direction")));
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_outbound_message", new QQueryFilter()))).anyMatch(row -> "DEAD_LETTER".equals(row.getValueString("status")));
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_functional_group", new QQueryFilter()))).anyMatch(row -> row.getValueInstant("ackReceivedAt") != null);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_exception", new QQueryFilter()))).anyMatch(row -> "OVERDUE_ACK".equals(row.getValueString("type")));
         var partners = SystemWrites.call(() -> EdiRecords.query("edi_partner", new QQueryFilter()));
         assertThat(partners).extracting(row -> row.getValueString("code")).containsExactlyInAnyOrder("NORTHWIND", "HARBOR", "SUMMIT", "RIDGELINE", "PACIFIC", "CEDAR");
         assertThat(partners).allSatisfy(row -> assertThat(row.getValueString("contactEmail")).endsWith("@example.invalid"));
         assertThat(SystemWrites.call(() -> EdiRecords.query("sample_wms_order", new QQueryFilter()))).hasSize(3);
         assertThat(SystemWrites.call(() -> EdiRecords.query("sample_shipment_status", new QQueryFilter()))).hasSize(3);
         var transactions = SystemWrites.call(() -> EdiRecords.query("edi_transaction", new QQueryFilter()));
         assertThat(transactions).anyMatch(row -> "945".equals(row.getValueString("transactionSet")) && "SENT".equals(row.getValueString("status")));
         assertThat(transactions).anyMatch(row -> "204".equals(row.getValueString("transactionSet")) && "SENT".equals(row.getValueString("status")));
         assertThat(transactions).anyMatch(row -> "940".equals(row.getValueString("transactionSet")) && "INVALID".equals(row.getValueString("status")));
         assertThat(transactions).anyMatch(row -> "214".equals(row.getValueString("transactionSet")) && "INVALID".equals(row.getValueString("status")));
         var as2Inbox = SystemWrites.call(() -> EdiRecords.query("edi_inbox", EdiRecords.equal("transportType", "AS2")));
         assertThat(as2Inbox).hasSize(3).allMatch(row -> row.getValueString("externalId").endsWith("@pacific"));
         var receipts = SystemWrites.call(() -> EdiRecords.query("edi_as2_mdn", new QQueryFilter()));
         assertThat(receipts).anyMatch(row -> "INBOUND".equals(row.getValueString("direction")) && Boolean.TRUE.equals(row.getValueBoolean("signatureValid")));
         assertThat(receipts).filteredOn(row -> "OUTBOUND".equals(row.getValueString("direction"))).hasSize(3);
         var as2Transports = SystemWrites.call(() -> EdiRecords.query("edi_transport", EdiRecords.equal("type", "AS2"))).stream().map(row -> row.getValue("id")).toList();
         var as2Messages = SystemWrites.call(() -> EdiRecords.query("edi_outbound_message", new QQueryFilter())).stream().filter(row -> as2Transports.contains(row.getValue("transportId"))).toList();
         assertThat(as2Messages).isNotEmpty().allMatch(row -> "DELIVERED".equals(row.getValueString("status")));
         assertThat(as2Messages).anyMatch(row -> row.getValueString("transportMic") != null);
         int port = demo.start(0);
         assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/qqq/hooks/edi/as2/nobody")).POST(HttpRequest.BodyPublishers.ofString("x")).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
         assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/app/edi")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
         for(String table : java.util.List.of("edi_partner", "edi_transaction", "edi_exception"))
         {
            assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/qqq/v1/table/" + table + "/query"))
               .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}" )).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
         }
         Set<String> rendered = new HashSet<>();
         for(var widget : demo.defineQInstance().getWidgets().values())
         {
            var join = EdiTables.joins().stream().filter(candidate -> candidate.getName().equals(widget.getName())).findFirst();
            Map<String, String> params = Map.of();
            String suffix = "";
            if(join.isPresent())
            {
               String parent = join.get().getLeftTable();
               var parents = SystemWrites.call(() -> EdiRecords.query(parent, new QQueryFilter()));
               assertThat(parents).as(parent).isNotEmpty();
               String id = parents.getFirst().getValueString("id");
               params = Map.of("tableName", parent, "id", id);
               suffix = "?tableName=" + parent + "&id=" + id;
            }
            var output = new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(widget).withUrlParams(new java.util.HashMap<>(params)));
            assertThat(output.getWidgetData()).as(widget.getName()).isNotNull();
            assertThat(JsonUtils.toJson(output)).as(widget.getName()).doesNotContain("secretsResolver", "serviceSessionSupplier", "SystemClock", "\"clock\"");
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/qqq/v1/widget/" + widget.getName() + suffix))
               .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(widget.getName()).isEqualTo(200);
            assertThat(response.body()).doesNotContain("secretsResolver", "serviceSessionSupplier", "SystemClock", "\"clock\"");
            rendered.add(widget.getName());
         }
         assertThat(rendered).containsExactlyInAnyOrderElementsOf(demo.defineQInstance().getWidgets().keySet());
         assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/qqq/v1/metaData")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
      }
   }
}
