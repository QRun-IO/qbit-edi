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
import com.kingsrook.qbits.edi.app.EdiHome;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Fresh application boots Next UI and serves seeded tables and typed home widgets.
 *******************************************************************************/
class EdiDemoServerTest
{
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
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_partner", new QQueryFilter()))).hasSize(3);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_certificate", new QQueryFilter()))).hasSize(2);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_inbox", new QQueryFilter()))).hasSizeGreaterThanOrEqualTo(40);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_transaction", new QQueryFilter()))).anyMatch(row -> "OUTBOUND".equals(row.getValueString("direction")));
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_outbound_message", new QQueryFilter()))).anyMatch(row -> "DEAD_LETTER".equals(row.getValueString("status")));
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_functional_group", new QQueryFilter()))).anyMatch(row -> row.getValueInstant("ackReceivedAt") != null);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_exception", new QQueryFilter()))).anyMatch(row -> "OVERDUE_ACK".equals(row.getValueString("type")));
         for(var widget : EdiHome.widgets())
         {
            assertThat(new EdiHome().render(new RenderWidgetInput().withWidgetMetaData(widget))).isNotNull();
         }
         int port = demo.start(0);
         assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/app/edi")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
         for(String table : java.util.List.of("edi_partner", "edi_transaction", "edi_exception"))
         {
            assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/qqq/v1/table/" + table + "/query"))
               .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}" )).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
         }
         assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/qqq/v1/widget/edi_home_outbound")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
         assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/qqq/v1/metaData")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
      }
   }
}
