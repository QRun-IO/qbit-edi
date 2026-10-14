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
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.transport.HttpsTransport;
import com.kingsrook.qbits.edi.transport.HttpsRouteProvider;
import com.kingsrook.qbits.edi.transport.SftpTransport;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.webhooks.WebhookSecret;
import com.kingsrook.qqq.backend.core.utils.webhooks.WebhookVerifier;
import com.kingsrook.qqq.backend.core.utils.webhooks.WebhookSigner;
import com.sun.net.httpserver.HttpServer;
import io.javalin.Javalin;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Real local HTTP/Javalin/MINA endpoints; only test code uses a disk SFTP root.
 *******************************************************************************/
class TransportTest
{
   /*******************************************************************************
    ** Release thread context.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Every outbound authentication scheme reaches an actual HTTP endpoint.
    *******************************************************************************/
   @Test
   void httpsOutbound() throws Exception
   {
      H2Support.create();
      String secret = WebhookSecret.generate().reveal();
      EdiRuntime.current().config().withSecretsResolver(name -> "AUTH".equals(name) ? secret : Base64.getEncoder().encodeToString(new byte[32]));
      byte[] bytes = "synthetic EDI content".getBytes(StandardCharsets.UTF_8);
      var payload = SystemWrites.call(() -> EdiRuntime.current().payloads().put(PayloadKind.OUTBOUND, new ByteArrayInputStream(bytes)));
      AtomicInteger status = new AtomicInteger(202);
      var requests = new java.util.concurrent.CopyOnWriteArrayList<Map<String, String>>();
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/edi", exchange ->
      {
         var headers = new java.util.HashMap<String, String>();
         exchange.getRequestHeaders().forEach((key, values) -> headers.put(key.toLowerCase(java.util.Locale.ROOT), values.getFirst()));
         requests.add(headers);
         assertThat(exchange.getRequestBody().readAllBytes()).isEqualTo(bytes);
         exchange.sendResponseHeaders(status.get(), -1);
         exchange.close();
      });
      server.start();
      try
      {
         for(String auth : List.of("NONE", "BASIC", "BEARER", "HMAC"))
         {
            var transport = new EdiTransport().withHttpsUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/edi").withDirection("OUTBOUND")
               .withHttpsAuthType(auth).withHttpsSecretEnvVar("AUTH").withHttpsHeadersJson("{\"X-EDI-Test\":\"synthetic\"}");
            var plugin = new HttpsTransport();
            var errors = new java.util.ArrayList<String>();
            plugin.validate(transport, errors);
            assertThat(errors).isEmpty();
            assertThat(plugin.send(new EdiOutboundMessage().withId(17), payload, transport).status()).isEqualTo("DELIVERED");
            var headers = requests.getLast();
            if(auth.equals("HMAC"))
            {
               new WebhookVerifier(List.of(WebhookSecret.parse(secret))).verify(headers, bytes);
            }
            else if(auth.equals("NONE"))
            {
               assertThat(headers).doesNotContainKey("authorization");
            }
            else
            {
               assertThat(headers.get("authorization")).isEqualTo(auth.equals("BASIC") ? "Basic " + Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8)) : "Bearer " + secret);
            }
            status.set(503);
            assertThat(plugin.send(new EdiOutboundMessage().withId(17), payload, transport).status()).isEqualTo("RETRY");
            status.set(400);
            assertThat(plugin.send(new EdiOutboundMessage().withId(17), payload, transport).status()).isEqualTo("FAILED");
            status.set(202);
         }
      }
      finally
      {
         server.stop(0);
      }
   }

   /*******************************************************************************
    ** Authenticated hook commits inbox rows and rejects unauthorized/oversized input.
    *******************************************************************************/
   @Test
   void httpsInbound() throws Exception
   {
      var instance = H2Support.create();
      String secret = WebhookSecret.generate().reveal();
      EdiRuntime.current().config().withSecretsResolver(name -> "AUTH".equals(name) ? secret : Base64.getEncoder().encodeToString(new byte[32]));
      QRecord transport = SystemWrites.call(() -> SystemWrites.insert("edi_transport", new QRecord().withValue("name", "receive").withValue("type", "HTTPS")
         .withValue("direction", "INBOUND").withValue("isActive", true).withValue("httpsInboundAuth", "HMAC").withValue("httpsSecretEnvVar", "AUTH")));
      HttpsRouteProvider provider = new HttpsRouteProvider();
      provider.setQInstance(instance);
      Javalin server = Javalin.create(provider::acceptJavalinConfig).start(0);
      try(HttpClient client = HttpClient.newHttpClient())
      {
         URI uri = URI.create("http://localhost:" + server.port() + "/qqq/hooks/edi/https/receive");
         byte[] body = "synthetic ingress".getBytes(StandardCharsets.UTF_8);
         HttpRequest.Builder request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofByteArray(body));
         assertThat(client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(401);
         WebhookSigner.headers("test-1", java.time.Instant.now(), body, List.of(WebhookSecret.parse(secret))).forEach(request::header);
         assertThat(client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(202);
         assertThat(client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(202);
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_inbox", new QQueryFilter()))).hasSize(1);
         for(String auth : List.of("BASIC", "BEARER"))
         {
            SystemWrites.call(() ->
            {
               SystemWrites.update("edi_transport", new QRecord().withValue("id", transport.getValue("id")).withValue("httpsInboundAuth", auth));
               return (null);
            });
            String authorization = auth.equals("BASIC") ? "Basic " + Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8)) : "Bearer " + secret;
            assertThat(client.send(HttpRequest.newBuilder(uri).header("Authorization", authorization).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(202);
         }
         assertThat(client.send(HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofByteArray(new byte[HttpsRouteProvider.MAX_BODY_BYTES + 1])).build(), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(413);
      }
      finally
      {
         server.stop();
      }
   }

   /*******************************************************************************
    ** SFTP push/poll stream through MINA and enforce the exact server fingerprint.
    *******************************************************************************/
   @Test
   void sftpPushPollAndPin(@TempDir Path root) throws Exception
   {
      H2Support.create();
      EdiRuntime.current().config().withSecretsResolver(name -> "SFTP_AUTH".equals(name) ? "synthetic-password" : Base64.getEncoder().encodeToString(new byte[32]));
      var key = KeyPairGenerator.getInstance("RSA").generateKeyPair();
      try(SshServer server = SshServer.setUpDefaultServer())
      {
         server.setPort(0);
         server.setKeyPairProvider(session -> List.of(key));
         server.setPasswordAuthenticator((user, password, session) -> user.equals("synthetic") && password.equals("synthetic-password"));
         server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
         server.setFileSystemFactory(new VirtualFileSystemFactory(root));
         server.start();
         var transport = new EdiTransport().withId(42).withSftpHost("127.0.0.1").withSftpPort(server.getPort()).withSftpUsername("synthetic")
            .withSftpPasswordEnvVar("SFTP_AUTH").withSftpHostKeyFingerprint(KeyUtils.getFingerPrint(key.getPublic())).withSftpOutboundPath("/").withSftpInboundPath("/").withSftpFilePattern(".*\\.x12").withSftpAfterPickup("DELETE");
         var plugin = new SftpTransport();
         assertThat(plugin.test(transport).success()).isTrue();
         var payload = SystemWrites.call(() -> EdiRuntime.current().payloads().put(PayloadKind.OUTBOUND, new ByteArrayInputStream("synthetic transfer".getBytes(StandardCharsets.UTF_8))));
         assertThat(plugin.send(new EdiOutboundMessage().withId(7), payload, transport).status()).isEqualTo("SENT");
         assertThat(Files.readString(root.resolve("edi-7.x12"))).isEqualTo("synthetic transfer");
         assertThat(Files.exists(root.resolve("edi-7.x12.tmp"))).isFalse();
         plugin.poll(transport, new InboundSink(EdiRuntime.current().payloads()));
         assertThat(Files.exists(root.resolve("edi-7.x12"))).isFalse();
         assertThat(SystemWrites.call(() -> EdiRecords.query("edi_inbox", new QQueryFilter()))).hasSize(1);
         transport.withSftpHostKeyFingerprint("SHA256:invalid");
         assertThat(plugin.test(transport).success()).isFalse();
      }
   }
}
