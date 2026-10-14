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
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import com.kingsrook.qbits.edi.As2Support.Party;
import com.kingsrook.qbits.edi.As2Support.PeerServer;
import com.kingsrook.qbits.edi.as2.As2Http;
import com.kingsrook.qbits.edi.as2.As2MdnSender;
import com.kingsrook.qbits.edi.as2.As2RouteProvider;
import com.kingsrook.qbits.edi.as2.As2Transport;
import com.kingsrook.qbits.edi.flow.AckMonitor;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.flow.SendWorker;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.sun.net.httpserver.HttpServer;
import io.javalin.Javalin;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** AS2 acceptance: our sender against our own receiver route, and both directions
 ** against an independent BouncyCastle peer. Synthetic data, in-process, no files.
 *******************************************************************************/
class As2Test
{
   static final String LOCAL_ID = "QQQ-LOCAL";
   static final String PEER_ID = "PACIFIC-WHOLESALE";

   private Javalin server;
   private final List<AutoCloseable> closeables = new ArrayList<>();

   /*******************************************************************************
    ** Alerts raised and resolved, as type and deduplication key.
    *******************************************************************************/
   public static class Alerts implements AlertSinkInterface
   {
      static final List<String> RAISED = new CopyOnWriteArrayList<>();
      static final List<String> RESOLVED = new CopyOnWriteArrayList<>();

      /*******************************************************************************
       ** Record a raised alert.
       *******************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         RAISED.add(signal.getAlertTypeName() + "|" + signal.getDedupKey());
      }

      /*******************************************************************************
       ** Record a resolved alert.
       *******************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         RESOLVED.add(resolution.getAlertTypeName() + "|" + resolution.getDedupKey());
      }

      /*******************************************************************************
       ** Heartbeats are not interesting here.
       *******************************************************************************/
      @Override
      public void heartbeat(String heartbeatKey, Instant at)
      {
         // Not recorded.
      }
   }

   /*******************************************************************************
    ** Captures every log message so secrets can be searched for.
    *******************************************************************************/
   static class Capture extends AbstractAppender
   {
      final List<String> lines = new CopyOnWriteArrayList<>();

      /*******************************************************************************
       ** Attach to the root logger.
       *******************************************************************************/
      Capture()
      {
         super("as2-capture", null, null, true, Property.EMPTY_ARRAY);
         start();
         LoggerContext context = (LoggerContext) LogManager.getContext(false);
         context.getConfiguration().getRootLogger().addAppender(this, Level.ALL, null);
         context.updateLoggers();
      }

      /*******************************************************************************
       ** Keep message and exception text.
       *******************************************************************************/
      @Override
      public void append(LogEvent event)
      {
         lines.add(event.getMessage().getFormattedMessage() + (event.getThrown() == null ? "" : " " + event.getThrown()));
      }

      /*******************************************************************************
       ** Detach from the root logger.
       *******************************************************************************/
      void detach()
      {
         LoggerContext context = (LoggerContext) LogManager.getContext(false);
         context.getConfiguration().getRootLogger().removeAppender(getName());
         context.updateLoggers();
         stop();
      }
   }

   /*******************************************************************************
    ** Everything is held in memory and per test.
    *******************************************************************************/
   @AfterEach
   void clear() throws Exception
   {
      for(AutoCloseable closeable : closeables)
      {
         closeable.close();
      }
      closeables.clear();
      if(server != null)
      {
         server.stop();
         server = null;
      }
      As2Support.ENV.clear();
      Alerts.RAISED.clear();
      Alerts.RESOLVED.clear();
      QContext.clear();
   }

   /*******************************************************************************
    ** A fresh database, secrets, the AS2 hooks on a random port and our own key.
    *******************************************************************************/
   Fixture fixture() throws Exception
   {
      var instance = H2Support.create();
      return (startHooks(instance));
   }

   /*******************************************************************************
    ** Start the hooks for an instance that already exists.
    *******************************************************************************/
   Fixture startHooks(com.kingsrook.qqq.backend.core.model.metadata.QInstance instance) throws Exception
   {
      As2RouteProvider provider = new As2RouteProvider();
      provider.setQInstance(instance);
      server = Javalin.create(provider::acceptJavalinConfig).start(0);
      EdiRuntime.current().config().withSecretsResolver(As2Support.secrets()).withPublicBaseUrl("http://127.0.0.1:" + server.port());
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(Alerts.class));
      return (new Fixture());
   }

   /*******************************************************************************
    ** Row helpers for certificates, transports and outbound messages.
    *******************************************************************************/
   class Fixture
   {
      final Party ours = As2Support.party("qqq-local", 365);
      final Party pacific = As2Support.party("pacific", 365);
      Integer partnerId;
      Integer oursLocalId;
      Integer pacificPublicId;

      /*******************************************************************************
       ** Our key goes into the in-memory environment; the partner row is created.
       *******************************************************************************/
      Fixture() throws Exception
      {
         partnerId = partner("Pacific Wholesale").getValueInteger("id");
         As2Support.ENV.put("AS2_KEY_OURS", ours.keyPem());
         As2Support.ENV.put("AS2_KEY_PACIFIC", pacific.keyPem());
      }

      /*******************************************************************************
       ** Insert a partner.
       *******************************************************************************/
      QRecord partner(String name) throws Exception
      {
         return (SystemWrites.call(() -> SystemWrites.insert("edi_partner", new QRecord().withValue("name", name).withValue("code", name.replace(' ', '-').toUpperCase()).withValue("status", "ACTIVE"))));
      }

      /*******************************************************************************
       ** Insert a certificate row; the qbit parses the PEM.
       *******************************************************************************/
      QRecord certificate(String alias, Party party, Integer owner, String status, String keyVariable) throws Exception
      {
         return (SystemWrites.call(() -> SystemWrites.insert("edi_certificate", new QRecord().withValue("alias", alias).withValue("partnerId", owner).withValue("usage", "SIGNING_AND_ENCRYPTION")
            .withValue("certificatePem", party.certificatePem()).withValue("privateKeyEnvVar", keyVariable).withValue("status", status))));
      }

      /*******************************************************************************
       ** Our certificate and the partner certificate for an external peer.
       *******************************************************************************/
      Fixture peerCertificates() throws Exception
      {
         oursLocalId = certificate("qqq-local", ours, null, "ACTIVE", "AS2_KEY_OURS").getValueInteger("id");
         pacificPublicId = certificate("pacific", pacific, partnerId, "ACTIVE", null).getValueInteger("id");
         return (this);
      }

      /*******************************************************************************
       ** Insert an AS2 transport with the usual settings and the given overrides.
       *******************************************************************************/
      QRecord transport(String name, String direction, String local, String partner, Integer localCertificate, Integer partnerCertificate, Integer owner, String url, String mdnMode,
         Map<String, Object> overrides) throws Exception
      {
         QRecord record = new QRecord().withValue("name", name).withValue("type", "AS2").withValue("direction", direction).withValue("partnerId", owner)
            .withValue("isActive", true).withValue("as2LocalId", local).withValue("as2PartnerId", partner).withValue("as2Url", url).withValue("signAlgorithm", "SHA256")
            .withValue("encryptAlgorithm", "AES256_CBC").withValue("mdnMode", mdnMode).withValue("mdnSigned", true).withValue("localCertificateId", localCertificate)
            .withValue("partnerCertificateId", partnerCertificate).withValue("maxAttempts", 3);
         overrides.forEach((k, v) -> record.setValue(k, (java.io.Serializable) v));
         return (SystemWrites.call(() -> SystemWrites.insert("edi_transport", record)));
      }

      /*******************************************************************************
       ** Our outbound transport to an external peer at a URL.
       *******************************************************************************/
      QRecord toPeer(String url, String mdnMode) throws Exception
      {
         return (transport("pacific-out", "OUTBOUND", LOCAL_ID, PEER_ID, oursLocalId, pacificPublicId, partnerId, url, mdnMode, Map.of()));
      }

      /*******************************************************************************
       ** Our inbound transport for an external peer.
       *******************************************************************************/
      QRecord fromPeer(Map<String, Object> overrides) throws Exception
      {
         return (transport("pacific-in", "INBOUND", LOCAL_ID, PEER_ID, oursLocalId, pacificPublicId, partnerId, null, "SYNC", overrides));
      }

      /*******************************************************************************
       ** Our sender and our own receiver, both configured from the same overrides.
       ** The receiver plays the partner: it holds the partner key as a local
       ** certificate and our public certificate as the partner certificate.
       *******************************************************************************/
      QRecord loopback(String mdnMode, Map<String, Object> overrides) throws Exception
      {
         oursLocalId = certificate("qqq-local", ours, null, "ACTIVE", "AS2_KEY_OURS").getValueInteger("id");
         Integer pacificLocal = certificate("pacific-local", pacific, null, "ACTIVE", "AS2_KEY_PACIFIC").getValueInteger("id");
         pacificPublicId = certificate("pacific", pacific, partnerId, "ACTIVE", null).getValueInteger("id");
         Integer other = partner("Loopback").getValueInteger("id");
         Integer oursPublic = certificate("qqq-local-as-partner", ours, other, "ACTIVE", null).getValueInteger("id");
         transport("pacific-in", "INBOUND", PEER_ID, LOCAL_ID, pacificLocal, oursPublic, other, null, "SYNC", overrides);
         return (transport("pacific-out", "OUTBOUND", LOCAL_ID, PEER_ID, oursLocalId, pacificPublicId, partnerId, hook("pacific-in"), mdnMode, overrides));
      }

      /*******************************************************************************
       ** Queue an outbound message holding the document.
       *******************************************************************************/
      QRecord outbound(QRecord transport, String text) throws Exception
      {
         return (SystemWrites.call(() ->
         {
            PayloadRef ref = EdiRuntime.current().payloads().put(PayloadKind.OUTBOUND, new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII)));
            return (SystemWrites.insert("edi_outbound_message", new QRecord().withValue("transportId", transport.getValue("id")).withValue("partnerId", transport.getValue("partnerId"))
               .withValue("payloadRef", ref.value()).withValue("payloadSha256", ref.sha256()).withValue("sizeBytes", ref.sizeBytes()).withValue("status", "QUEUED").withValue("attempts", 0)));
         }));
      }

      /*******************************************************************************
       ** The hook URL of a receiving transport.
       *******************************************************************************/
      String hook(String name)
      {
         return ("http://127.0.0.1:" + server.port() + "/qqq/hooks/edi/as2/" + name);
      }

      /*******************************************************************************
       ** Send everything queued and return the refreshed message.
       *******************************************************************************/
      QRecord send(QRecord message) throws Exception
      {
         new SendWorker().runOnce();
         return (row("edi_outbound_message", message.getValue("id")));
      }

      /*******************************************************************************
       ** A peer message to our hook with the usual identities.
       *******************************************************************************/
      As2Support.Reply peerSends(String transport, String messageId, byte[] payload, Party signer, java.security.cert.X509Certificate encryptTo, boolean asyncRequested) throws Exception
      {
         return (As2Support.peerSend(hook(transport), Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID), messageId, payload, signer, encryptTo, true, true, true, asyncRequested ? "async" : null));
      }
   }

   /*******************************************************************************
    ** Fresh copy of a row.
    *******************************************************************************/
   static QRecord row(String table, Object id) throws Exception
   {
      return (SystemWrites.call(() -> EdiRecords.query(table, EdiRecords.equal("id", (java.io.Serializable) id)).getFirst()));
   }

   /*******************************************************************************
    ** All rows of a table.
    *******************************************************************************/
   static List<QRecord> rows(String table) throws Exception
   {
      return (SystemWrites.call(() -> EdiRecords.query(table, new QQueryFilter())));
   }

   /*******************************************************************************
    ** Attempt an insert through the normal action and return its error messages.
    *******************************************************************************/
   static List<String> errors(String table, QRecord record) throws Exception
   {
      return (SystemWrites.call(() -> new InsertAction().execute(new InsertInput(table).withRecord(record)).getRecords().getFirst().getErrors().stream().map(e -> e.getMessage()).toList()));
   }

   /*******************************************************************************
    ** One interchange of synthetic content.
    *******************************************************************************/
   static String edi(String tag)
   {
      return ("ISA*00*          *00*          *ZZ*QQQTEST        *ZZ*PACIFIC        *261013*1200*U*00401*000000001*0*T*:~GS*PR*QQQTEST*PACIFIC*20261013*1200*1*X*004010~ST*855*0001~BAK*00*AD*" + tag + "*20261013~SE*3*0001~GE*1*1~IEA*1*000000001~");
   }

   /*******************************************************************************
    ** Stored text of a payload reference.
    *******************************************************************************/
   static String payload(String reference) throws Exception
   {
      try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(reference, null, 0)))
      {
         return (new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1));
      }
   }

   /*******************************************************************************
    ** Advance the clock the qbit sees.
    *******************************************************************************/
   static void clockAt(Instant instant)
   {
      EdiRuntime.current().config().withClock(Clock.fixed(instant, ZoneOffset.UTC));
   }

   /*******************************************************************************
    ** Run an EDI record action over the given rows.
    *******************************************************************************/
   static com.kingsrook.qbits.edi.processes.EdiProcesses.Execute action(String process, QRecord record, RunBackendStepOutput output) throws Exception
   {
      var execute = new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute();
      execute.runOnePage(new RunBackendStepInput().withProcessName(process).withRecords(List.of(record)), output);
      return (execute);
   }

   /*******************************************************************************
    ** Certificates are parsed on save; private keys and bad input are refused
    ** without echoing anything.
    *******************************************************************************/
   @Test
   void certificateParsingAndRejection() throws Exception
   {
      Fixture f = fixture();
      QRecord cert = f.certificate("parsed", f.pacific, f.partnerId, "ACTIVE", null);
      assertThat(cert.getValueString("subject")).contains("CN=pacific");
      assertThat(cert.getValueString("issuer")).contains("CN=pacific");
      assertThat(cert.getValueString("serialNumber")).isNotBlank();
      assertThat(cert.getValueString("fingerprintSha256")).matches("([0-9A-F]{2}:){31}[0-9A-F]{2}");
      assertThat(cert.getValueInstant("notBefore")).isBefore(Instant.now());
      assertThat(cert.getValueInstant("notAfter")).isAfter(Instant.now().plusSeconds(300 * 86400));
      List<String> pasted = errors("edi_certificate", new QRecord().withValue("alias", "bad").withValue("usage", "SIGNING").withValue("status", "ACTIVE")
         .withValue("certificatePem", f.pacific.certificatePem() + f.pacific.keyPem()));
      assertThat(pasted).hasSize(1);
      assertThat(pasted.getFirst()).contains("only the public certificate").doesNotContain("BEGIN").doesNotContain(f.pacific.keyPem().substring(30, 80));
      assertThat(errors("edi_certificate", new QRecord().withValue("alias", "junk").withValue("usage", "SIGNING").withValue("certificatePem", "not a certificate"))).anyMatch(e -> e.contains("PEM"));
      assertThat(errors("edi_certificate", new QRecord().withValue("alias", "keyed").withValue("partnerId", f.partnerId).withValue("usage", "SIGNING").withValue("privateKeyEnvVar", "SOME_KEY")
         .withValue("certificatePem", f.pacific.certificatePem()))).anyMatch(e -> e.contains("only for our own"));
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_certificate", new QRecord().withValue("id", cert.getValue("id")).withValue("certificatePem", f.ours.certificatePem()));
         return (null);
      });
      assertThat(row("edi_certificate", cert.getValue("id")).getValueString("subject")).contains("CN=qqq-local");
   }

   /*******************************************************************************
    ** Transport validation: owners, weak crypto flag, VAN with no partner.
    *******************************************************************************/
   @Test
   void transportValidation() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      assertThat(errors("edi_transport", new QRecord().withValue("name", "no-partner").withValue("type", "AS2").withValue("direction", "INBOUND").withValue("as2LocalId", LOCAL_ID).withValue("as2PartnerId", PEER_ID)
         .withValue("localCertificateId", f.oursLocalId).withValue("partnerCertificateId", f.pacificPublicId))).anyMatch(e -> e.contains("unless the transport is a VAN"));
      assertThat(errors("edi_transport", new QRecord().withValue("name", "http-out").withValue("type", "AS2").withValue("direction", "OUTBOUND").withValue("partnerId", f.partnerId)
         .withValue("as2LocalId", LOCAL_ID).withValue("as2PartnerId", PEER_ID).withValue("as2Url", "http://partner.example.com/as2"))).anyMatch(e -> e.contains("as2Url must use TLS"));
      assertThat(errors("edi_transport", new QRecord().withValue("name", "swapped").withValue("type", "AS2").withValue("direction", "INBOUND").withValue("partnerId", f.partnerId)
         .withValue("as2LocalId", LOCAL_ID).withValue("as2PartnerId", PEER_ID).withValue("signAlgorithm", "SHA256")
         .withValue("localCertificateId", f.pacificPublicId).withValue("partnerCertificateId", f.oursLocalId))).anyMatch(e -> e.contains("localCertificateId must be one of our own")).anyMatch(e -> e.contains("partnerCertificateId must be a certificate of the partner"));
      assertThat(errors("edi_transport", new QRecord().withValue("name", "needs-certs").withValue("type", "AS2").withValue("direction", "INBOUND").withValue("partnerId", f.partnerId)
         .withValue("as2LocalId", LOCAL_ID).withValue("as2PartnerId", PEER_ID).withValue("encryptAlgorithm", "AES256_CBC"))).anyMatch(e -> e.contains("localCertificateId is required")).anyMatch(e -> e.contains("partnerCertificateId is required"));
      assertThat(errors("edi_transport", new QRecord().withValue("name", "bad-algorithm").withValue("type", "AS2").withValue("direction", "INBOUND").withValue("partnerId", f.partnerId)
         .withValue("as2LocalId", LOCAL_ID).withValue("as2PartnerId", PEER_ID).withValue("signAlgorithm", "MD5").withValue("encryptAlgorithm", "RC4").withValue("mdnMode", "LATER")))
         .anyMatch(e -> e.contains("Unsupported signAlgorithm")).anyMatch(e -> e.contains("Unsupported encryptAlgorithm")).anyMatch(e -> e.contains("Unsupported mdnMode"));
      QRecord strong = f.transport("strong", "INBOUND", LOCAL_ID, PEER_ID, f.oursLocalId, f.pacificPublicId, f.partnerId, null, "SYNC", Map.of());
      assertThat(strong.getValueBoolean("weakCrypto")).isFalse();
      QRecord weak = f.transport("weak", "INBOUND", LOCAL_ID, PEER_ID, f.oursLocalId, f.pacificPublicId, f.partnerId, null, "SYNC", Map.of("signAlgorithm", "SHA1", "encryptAlgorithm", "3DES"));
      assertThat(row("edi_transport", weak.getValue("id")).getValueBoolean("weakCrypto")).isTrue();
      Integer vanPartner = f.partner("Synthetic VAN").getValueInteger("id");
      Integer vanCertificate = f.certificate("van", f.pacific, vanPartner, "ACTIVE", null).getValueInteger("id");
      QRecord van = f.transport("van", "BOTH", LOCAL_ID, "SYNTHETIC-VAN", f.oursLocalId, vanCertificate, null, "https://van.example.com/as2", "SYNC", Map.of("isVan", true, "vanName", "Synthetic VAN"));
      assertThat(row("edi_transport", van.getValue("id")).getValue("partnerId")).isNull();
      assertThat(errors("edi_transport", new QRecord().withValue("name", "van-own-cert").withValue("type", "AS2").withValue("direction", "BOTH").withValue("isVan", true)
         .withValue("as2LocalId", LOCAL_ID).withValue("as2PartnerId", "VAN").withValue("as2Url", "https://van.example.com/as2").withValue("signAlgorithm", "SHA256")
         .withValue("localCertificateId", f.oursLocalId).withValue("partnerCertificateId", f.oursLocalId))).anyMatch(e -> e.contains("partnerCertificateId must be a certificate of the partner"));
   }

   /*******************************************************************************
    ** Our sender against our own receiver: signed, encrypted, synchronous MDN.
    *******************************************************************************/
   @Test
   void roundTripSynchronous() throws Exception
   {
      Fixture f = fixture();
      QRecord out = f.loopback("SYNC", Map.of());
      QRecord message = f.outbound(out, edi("RT1"));
      assertThat(new SendWorker().runOnce()).isEqualTo(1);
      QRecord sent = row("edi_outbound_message", message.getValue("id"));
      assertThat(sent.getValueString("status")).as(sent.getValueString("lastError")).isEqualTo("DELIVERED");
      assertThat(sent.getValueString("transportMessageId")).startsWith("edi-" + message.getValue("id") + "-").endsWith("@QQQ-LOCAL");
      assertThat(sent.getValueString("transportMic")).endsWith(", sha-256");
      List<QRecord> inbox = rows("edi_inbox");
      assertThat(inbox).hasSize(1);
      assertThat(inbox.getFirst().getValueString("transportType")).isEqualTo("AS2");
      assertThat(inbox.getFirst().getValueString("externalId")).isEqualTo(sent.getValueString("transportMessageId"));
      assertThat(inbox.getFirst().getValueInteger("transportId")).isNotNull();
      assertThat(payload(inbox.getFirst().getValueString("payloadRef"))).isEqualTo(edi("RT1"));
      List<QRecord> mdns = rows("edi_as2_mdn");
      assertThat(mdns).hasSize(2);
      QRecord received = mdns.stream().filter(r -> "INBOUND".equals(r.getValueString("direction"))).findFirst().orElseThrow();
      QRecord answered = mdns.stream().filter(r -> "OUTBOUND".equals(r.getValueString("direction"))).findFirst().orElseThrow();
      assertThat(received.getValueBoolean("signed")).isTrue();
      assertThat(received.getValueBoolean("signatureValid")).isTrue();
      assertThat(received.getValueString("disposition")).endsWith("processed");
      assertThat(received.getValueString("micValue")).isEqualTo(answered.getValueString("micValue")).isNotBlank();
      assertThat(received.getValue("outboundMessageId")).isEqualTo(message.getValue("id"));
      assertThat(answered.getValue("inboxId")).isEqualTo(inbox.getFirst().getValue("id"));
      assertThat(payload(received.getValueString("payloadRef"))).contains("message/disposition-notification", "Original-Message-ID");
      assertThat(rows("edi_event")).extracting(r -> r.getValueString("type")).contains("MDN", "DELIVERED");
   }

   /*******************************************************************************
    ** Algorithm and compression combinations all round trip.
    *******************************************************************************/
   static Stream<Arguments> variants()
   {
      return (Stream.of(
         Arguments.of("SHA384", "AES128_GCM", true),
         Arguments.of("SHA512", "AES128_CBC", false),
         Arguments.of("SHA256", null, false),
         Arguments.of(null, "AES256_CBC", false),
         Arguments.of("SHA1", "3DES", false),
         Arguments.of(null, null, false)));
   }

   /*******************************************************************************
    ** Each combination of signing, encryption and compression is delivered with
    ** its MIC verified, and weak algorithms are flagged.
    *******************************************************************************/
   @ParameterizedTest
   @MethodSource("variants")
   void roundTripVariants(String sign, String encrypt, boolean compress) throws Exception
   {
      Fixture f = fixture();
      Map<String, Object> overrides = new LinkedHashMap<>();
      overrides.put("signAlgorithm", sign);
      overrides.put("encryptAlgorithm", encrypt);
      overrides.put("compress", compress);
      overrides.put("mdnSigned", sign != null);
      QRecord out = f.loopback("SYNC", overrides);
      QRecord sent = f.send(f.outbound(out, edi("VAR")));
      assertThat(sent.getValueString("status")).as(sent.getValueString("lastError")).isEqualTo("DELIVERED");
      assertThat(payload(rows("edi_inbox").getFirst().getValueString("payloadRef"))).isEqualTo(edi("VAR"));
      QRecord mdn = rows("edi_as2_mdn").stream().filter(r -> "INBOUND".equals(r.getValueString("direction"))).findFirst().orElseThrow();
      assertThat(mdn.getValueBoolean("signed")).isEqualTo(sign != null);
      assertThat(row("edi_transport", out.getValue("id")).getValueBoolean("weakCrypto")).isEqualTo("SHA1".equals(sign));
   }

   /*******************************************************************************
    ** Asynchronous MDN: the message stays SENT until the receipt is delivered to
    ** the address in Receipt-Delivery-Option; the overdue alert raises and resolves.
    *******************************************************************************/
   @Test
   void asynchronousMdn() throws Exception
   {
      Fixture f = fixture();
      QRecord out = f.loopback("ASYNC", Map.of());
      QRecord sent = f.send(f.outbound(out, edi("ASYNC1")));
      assertThat(sent.getValueString("status")).isEqualTo("SENT");
      assertThat(sent.getValueString("transportMic")).isNotBlank();
      assertThat(rows("edi_inbox")).hasSize(1);
      QRecord queued = rows("edi_as2_mdn").getFirst();
      assertThat(queued.getValueString("direction")).isEqualTo("OUTBOUND");
      assertThat(queued.getValueString("deliveryStatus")).isEqualTo("PENDING");
      assertThat(queued.getValueString("deliveryUrl")).isEqualTo(f.hook("pacific-out") + "/mdn");
      clockAt(Instant.now().plusSeconds(61 * 60));
      new AckMonitor().runOnce();
      assertThat(Alerts.RAISED).contains("edi.mdn.overdue|edi_outbound_message:" + sent.getValue("id"));
      assertThat(new As2MdnSender().runOnce()).isEqualTo(1);
      assertThat(row("edi_as2_mdn", queued.getValue("id")).getValueString("deliveryStatus")).isEqualTo("SENT");
      QRecord delivered = row("edi_outbound_message", sent.getValue("id"));
      assertThat(delivered.getValueString("status")).isEqualTo("DELIVERED");
      assertThat(delivered.getValueInstant("deliveredAt")).isNotNull();
      assertThat(Alerts.RESOLVED).contains("edi.mdn.overdue|edi_outbound_message:" + sent.getValue("id"));
      QRecord receipt = rows("edi_as2_mdn").stream().filter(r -> "INBOUND".equals(r.getValueString("direction"))).findFirst().orElseThrow();
      assertThat(receipt.getValueBoolean("signed")).isTrue();
      assertThat(receipt.getValueBoolean("signatureValid")).isTrue();
      assertThat(new As2MdnSender().runOnce()).isZero();
   }

   /*******************************************************************************
    ** Our sender against an independent peer: the peer decrypts, verifies and
    ** computes the same MIC; a wrong MIC and an error disposition fail the send.
    *******************************************************************************/
   @Test
   void senderAgainstIndependentPeer() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      try(PeerServer peer = new PeerServer(f.pacific, f.ours.certificate()))
      {
         QRecord out = f.toPeer(peer.url(), "SYNC");
         QRecord good = f.send(f.outbound(out, edi("PEER1")));
         assertThat(good.getValueString("status")).as(good.getValueString("lastError")).isEqualTo("DELIVERED");
         assertThat(peer.received).hasSize(1);
         assertThat(new String(peer.received.getFirst().payload(), StandardCharsets.US_ASCII)).isEqualTo(edi("PEER1"));
         assertThat(peer.received.getFirst().signed()).isTrue();
         assertThat(peer.received.getFirst().encrypted()).isTrue();
         assertThat(peer.received.getFirst().mic()).isEqualTo(good.getValueString("transportMic"));
         assertThat(peer.requests.getFirst()).containsEntry("as2-from", LOCAL_ID).containsEntry("as2-to", PEER_ID).containsEntry("as2-version", "1.2").containsKey("disposition-notification-options");
         peer.corruptMic.set(true);
         QRecord mismatch = f.send(f.outbound(out, edi("PEER2")));
         assertThat(mismatch.getValueString("status")).isEqualTo("FAILED");
         assertThat(mismatch.getValueString("lastError")).contains("MIC");
         peer.corruptMic.set(false);
         peer.disposition.set("automatic-action/MDN-sent-automatically; processed/error: decryption-failed");
         QRecord refused = f.send(f.outbound(out, edi("PEER3")));
         assertThat(refused.getValueString("status")).isEqualTo("FAILED");
         assertThat(refused.getValueString("lastError")).contains("decryption-failed");
         assertThat(rows("edi_as2_mdn")).filteredOn(r -> r.getValueString("disposition").contains("decryption-failed")).hasSize(1);
         peer.disposition.set("automatic-action/MDN-sent-automatically; processed");
         peer.signMdn.set(false);
         QRecord unsignedMdn = f.send(f.outbound(out, edi("PEER4")));
         assertThat(unsignedMdn.getValueString("lastError")).contains("not signed");
      }
   }

   /*******************************************************************************
    ** An asynchronous receipt posted by an independent peer to our MDN hook settles
    ** the message; unauthenticated, unknown and misaddressed receipts change nothing.
    *******************************************************************************/
   @Test
   void asynchronousMdnFromIndependentPeer() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      try(PeerServer peer = new PeerServer(f.pacific, f.ours.certificate()))
      {
         peer.async.set(true);
         QRecord out = f.toPeer(peer.url(), "ASYNC");
         QRecord sent = f.send(f.outbound(out, edi("ASYNC2")));
         assertThat(sent.getValueString("status")).isEqualTo("SENT");
         assertThat(peer.requests.getFirst().get("receipt-delivery-option")).isEqualTo(f.hook("pacific-out") + "/mdn");
         for(int i = 0; i < 100 && peer.asyncResult.get() == null; i++)
         {
            Thread.sleep(50);
         }
         assertThat(peer.asyncResult.get().status()).isEqualTo(200);
         QRecord delivered = row("edi_outbound_message", sent.getValue("id"));
         assertThat(delivered.getValueString("status")).isEqualTo("DELIVERED");
         assertThat(rows("edi_as2_mdn")).hasSize(1);
         String id = sent.getValueString("transportMessageId");
         String mic = sent.getValueString("transportMic");
         Map<String, String> ids = Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID, "AS2-Version", "1.2", "Message-ID", "<peer-async-x@peer>");
         String url = f.hook("pacific-out") + "/mdn";
         assertThat(As2Support.post(url, peer.mdn(id, "automatic-action/MDN-sent-automatically; processed", mic, false), ids).status()).as("unsigned").isEqualTo(401);
         assertThat(As2Support.post(url, peer.mdn("nobody@nowhere", "automatic-action/MDN-sent-automatically; processed", mic, true), ids).status()).as("unknown").isEqualTo(404);
         assertThat(As2Support.post(url, peer.mdn(id, "automatic-action/MDN-sent-automatically; processed", mic, true), Map.of("AS2-From", "SOMEONE", "AS2-To", LOCAL_ID)).status()).as("identity").isEqualTo(403);
         byte[] again = peer.mdn(id, "automatic-action/MDN-sent-automatically; processed", mic, true);
         assertThat(As2Support.post(url, again, ids).status()).isEqualTo(200);
         assertThat(As2Support.post(url, again, ids).status()).isEqualTo(200);
         assertThat(rows("edi_as2_mdn")).hasSize(2);
         assertThat(As2Support.post(f.hook("missing") + "/mdn", again, ids).status()).isEqualTo(404);
      }
   }

   /*******************************************************************************
    ** An independent peer sends to our route: signed, encrypted, base64 transfer
    ** encoding; we answer with a signed MDN whose MIC equals the peer's own.
    *******************************************************************************/
   @Test
   void peerSendsToUs() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      f.fromPeer(Map.of());
      As2Support.Reply reply = f.peerSends("pacific-in", "peer-msg-1@pacific", edi("INBOUND1").getBytes(StandardCharsets.US_ASCII), f.pacific, f.ours.certificate(), false);
      assertThat(reply.status()).isEqualTo(200);
      As2Support.PeerMdn mdn = As2Support.peerReadMdn(reply, f.ours.certificate());
      assertThat(mdn.disposition()).endsWith("processed").doesNotContain("error");
      assertThat(mdn.originalMessageId()).isEqualTo("<peer-msg-1@pacific>");
      assertThat(mdn.signatureValid()).isTrue();
      assertThat(mdn.receivedMic()).isEqualTo(reply.headers().get("x-sent-mic"));
      assertThat(reply.headers().get("as2-from")).isEqualTo(LOCAL_ID);
      assertThat(reply.headers().get("as2-to")).isEqualTo(PEER_ID);
      List<QRecord> inbox = rows("edi_inbox");
      assertThat(inbox).hasSize(1);
      assertThat(inbox.getFirst().getValueString("externalId")).isEqualTo("peer-msg-1@pacific");
      assertThat(inbox.getFirst().getValueString("fileName")).isEqualTo("peer.edi");
      assertThat(payload(inbox.getFirst().getValueString("payloadRef"))).isEqualTo(edi("INBOUND1"));
      QRecord receipt = rows("edi_as2_mdn").getFirst();
      assertThat(receipt.getValueString("direction")).isEqualTo("OUTBOUND");
      assertThat(receipt.getValue("inboxId")).isEqualTo(inbox.getFirst().getValue("id"));
      assertThat(receipt.getValueBoolean("signed")).isTrue();
   }

   /*******************************************************************************
    ** Bad signature, unencrypted, wrong recipient certificate: error MDNs and no
    ** inbox rows. Wrong AS2 identities and unknown transports are refused outright.
    *******************************************************************************/
   @Test
   void refusals() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      f.fromPeer(Map.of());
      byte[] payload = edi("BAD").getBytes(StandardCharsets.US_ASCII);
      Party intruder = As2Support.party("intruder", 365);
      As2Support.Reply badSignature = f.peerSends("pacific-in", "bad-signature@pacific", payload, intruder, f.ours.certificate(), false);
      assertThat(badSignature.status()).isEqualTo(200);
      assertThat(As2Support.peerReadMdn(badSignature, f.ours.certificate()).disposition()).contains("processed/error").endsWith("authentication-failed");
      assertThat(As2Support.peerReadMdn(f.peerSends("pacific-in", "plain@pacific", payload, f.pacific, null, false), f.ours.certificate()).disposition()).endsWith("insufficient-message-security");
      assertThat(As2Support.peerReadMdn(f.peerSends("pacific-in", "wrong-key@pacific", payload, f.pacific, intruder.certificate(), false), f.ours.certificate()).disposition()).endsWith("decryption-failed");
      assertThat(rows("edi_inbox")).isEmpty();
      assertThat(rows("edi_as2_mdn")).hasSize(3).allSatisfy(r ->
      {
         assertThat(r.getValueString("disposition")).contains("processed/error");
         assertThat(r.getValue("inboxId")).isNull();
      });
      Map<String, String> good = Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID);
      assertThat(As2Support.peerSend(f.hook("pacific-in"), Map.of("AS2-From", PEER_ID, "AS2-To", "SOMEONE-ELSE"), "wrong-to@pacific", payload, f.pacific, f.ours.certificate(), true, true, true, null).status()).isEqualTo(403);
      assertThat(As2Support.peerSend(f.hook("pacific-in"), Map.of("AS2-From", "SOMEONE-ELSE", "AS2-To", LOCAL_ID), "wrong-from@pacific", payload, f.pacific, f.ours.certificate(), true, true, true, null).status()).isEqualTo(403);
      assertThat(As2Support.peerSend(f.hook("missing"), good, "unknown@pacific", payload, f.pacific, f.ours.certificate(), true, true, true, null).status()).isEqualTo(404);
      assertThat(As2Support.peerSend(f.hook("pacific-in"), good, "", payload, f.pacific, f.ours.certificate(), true, true, true, null).status()).isEqualTo(400);
      assertThat(rows("edi_inbox")).isEmpty();
      assertThat(rows("edi_as2_mdn")).hasSize(3);
      assertThat(As2Support.peerSend(f.hook("pacific-in"), good, "no-mdn@pacific", payload, f.pacific, f.ours.certificate(), true, false, false, null).status()).isEqualTo(200);
      assertThat(rows("edi_inbox")).hasSize(1);
      assertThat(rows("edi_as2_mdn")).hasSize(3);
      assertThat(As2Support.peerSend(f.hook("pacific-in"), good, "no-mdn-bad@pacific", payload, intruder, f.ours.certificate(), true, false, false, null).status()).isEqualTo(400);
   }

   /*******************************************************************************
    ** A retransmitted Message-ID returns the original MDN, byte for byte, with no
    ** second inbox row and no second receipt row.
    *******************************************************************************/
   @Test
   void retransmitGetsOriginalMdn() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      f.fromPeer(Map.of());
      byte[] payload = edi("DUP").getBytes(StandardCharsets.US_ASCII);
      As2Support.Reply first = f.peerSends("pacific-in", "dup-1@pacific", payload, f.pacific, f.ours.certificate(), false);
      As2Support.Reply second = f.peerSends("pacific-in", "dup-1@pacific", payload, f.pacific, f.ours.certificate(), false);
      assertThat(second.status()).isEqualTo(200);
      assertThat(second.body()).isEqualTo(first.body());
      assertThat(second.headers().get("message-id")).isEqualTo(first.headers().get("message-id"));
      assertThat(second.headers().get("content-type")).isEqualTo(first.headers().get("content-type"));
      assertThat(As2Support.peerReadMdn(second, f.ours.certificate()).signatureValid()).isTrue();
      assertThat(rows("edi_inbox")).hasSize(1);
      assertThat(rows("edi_as2_mdn")).hasSize(1);
      assertThat(rows("edi_event")).filteredOn(r -> "RECEIVED".equals(r.getValueString("type"))).hasSize(1);
      As2Support.Reply other = f.peerSends("pacific-in", "dup-2@pacific", payload, f.pacific, f.ours.certificate(), false);
      assertThat(As2Support.peerReadMdn(other, f.ours.certificate()).disposition()).endsWith("processed");
      assertThat(rows("edi_inbox")).hasSize(2);
      assertThat(rows("edi_inbox")).filteredOn(r -> "DUPLICATE".equals(r.getValueString("status")) || "PENDING".equals(r.getValueString("status"))).hasSize(2);
   }

   /*******************************************************************************
    ** The same Message-ID from two different partners loses neither message, and a
    ** retransmission still gets its own original MDN.
    *******************************************************************************/
   @Test
   void sameMessageIdFromTwoPartners() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      f.fromPeer(Map.of());
      Party other = As2Support.party("other-partner", 365);
      Integer otherPartner = f.partner("Other Partner").getValueInteger("id");
      Integer otherCertificate = f.certificate("other-partner", other, otherPartner, "ACTIVE", null).getValueInteger("id");
      QRecord otherTransport = f.transport("other-in", "INBOUND", LOCAL_ID, "OTHER-WS", f.oursLocalId, otherCertificate, otherPartner, null, "SYNC", Map.of());
      As2Support.Reply first = f.peerSends("pacific-in", "shared-id@x", edi("ONE").getBytes(StandardCharsets.US_ASCII), f.pacific, f.ours.certificate(), false);
      Map<String, String> ids = Map.of("AS2-From", "OTHER-WS", "AS2-To", LOCAL_ID);
      byte[] second = edi("TWO").getBytes(StandardCharsets.US_ASCII);
      As2Support.Reply reply = As2Support.peerSend(f.hook("other-in"), ids, "shared-id@x", second, other, f.ours.certificate(), true, true, true, null);
      assertThat(As2Support.peerReadMdn(first, f.ours.certificate()).disposition()).endsWith("processed");
      assertThat(As2Support.peerReadMdn(reply, f.ours.certificate()).disposition()).endsWith("processed");
      assertThat(rows("edi_inbox")).extracting(r -> r.getValueString("externalId")).containsExactlyInAnyOrder("shared-id@x", otherTransport.getValue("id") + ":shared-id@x");
      assertThat(payload(rows("edi_inbox").stream().filter(r -> r.getValueString("externalId").contains(":")).findFirst().orElseThrow().getValueString("payloadRef"))).isEqualTo(edi("TWO"));
      As2Support.Reply again = As2Support.peerSend(f.hook("other-in"), ids, "shared-id@x", second, other, f.ours.certificate(), true, true, true, null);
      assertThat(again.body()).isEqualTo(reply.body());
      assertThat(rows("edi_inbox")).hasSize(2);
      assertThat(rows("edi_as2_mdn")).hasSize(2);
   }

   /*******************************************************************************
    ** An asynchronous receipt requested by a peer is queued, retried after a failed
    ** post, and delivered signed. Receipts for unauthenticated messages are never
    ** posted anywhere.
    *******************************************************************************/
   @Test
   void asynchronousReceiptQueueAndRetry() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      f.fromPeer(Map.of());
      AtomicInteger hits = new AtomicInteger();
      List<As2Support.Reply> posted = new CopyOnWriteArrayList<>();
      HttpServer collector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      closeables.add(() -> collector.stop(0));
      collector.createContext("/mdn", exchange ->
      {
         Map<String, String> headers = new LinkedHashMap<>();
         exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(java.util.Locale.ROOT), v.getFirst()));
         posted.add(new As2Support.Reply(hits.incrementAndGet(), headers, exchange.getRequestBody().readAllBytes()));
         exchange.sendResponseHeaders(hits.get() == 1 ? 500 : 200, -1);
         exchange.close();
      });
      collector.start();
      String target = "http://127.0.0.1:" + collector.getAddress().getPort() + "/mdn";
      byte[] payload = edi("ASYNC3").getBytes(StandardCharsets.US_ASCII);
      As2Support.Reply reply = As2Support.peerSend(f.hook("pacific-in"), Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID), "async-1@pacific", payload, f.pacific, f.ours.certificate(), true, true, true, target);
      assertThat(reply.status()).isEqualTo(200);
      assertThat(reply.body()).isEmpty();
      assertThat(rows("edi_inbox")).hasSize(1);
      QRecord queued = rows("edi_as2_mdn").getFirst();
      assertThat(queued.getValueString("deliveryStatus")).isEqualTo("PENDING");
      assertThat(queued.getValueString("deliveryUrl")).isEqualTo(target);
      Instant start = Instant.now();
      assertThat(new As2MdnSender().runOnce()).isEqualTo(1);
      QRecord retry = row("edi_as2_mdn", queued.getValue("id"));
      assertThat(retry.getValueString("deliveryStatus")).isEqualTo("PENDING");
      assertThat(retry.getValueInteger("deliveryAttempts")).isEqualTo(1);
      assertThat(retry.getValueInstant("nextAttemptAt")).isAfter(start.plusSeconds(30));
      assertThat(new As2MdnSender().runOnce()).isZero();
      clockAt(Instant.now().plusSeconds(120));
      assertThat(new As2MdnSender().runOnce()).isEqualTo(1);
      assertThat(row("edi_as2_mdn", queued.getValue("id")).getValueString("deliveryStatus")).isEqualTo("SENT");
      assertThat(posted).hasSize(2);
      As2Support.Reply delivered = posted.get(1);
      assertThat(delivered.headers()).containsEntry("as2-from", LOCAL_ID).containsEntry("as2-to", PEER_ID);
      As2Support.PeerMdn mdn = As2Support.peerReadMdn(new As2Support.Reply(200, delivered.headers(), delivered.body()), f.ours.certificate());
      assertThat(mdn.signatureValid()).isTrue();
      assertThat(mdn.disposition()).endsWith("processed");
      assertThat(mdn.receivedMic()).isEqualTo(reply.headers().get("x-sent-mic"));
      As2Support.Reply retransmit = As2Support.peerSend(f.hook("pacific-in"), Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID), "async-1@pacific", payload, f.pacific, f.ours.certificate(), true, true, true, target);
      assertThat(retransmit.status()).isEqualTo(200);
      assertThat(row("edi_as2_mdn", queued.getValue("id")).getValueString("deliveryStatus")).isEqualTo("PENDING");
      assertThat(rows("edi_inbox")).hasSize(1);
      Party intruder = As2Support.party("intruder", 365);
      int before = posted.size();
      As2Support.Reply unauthenticated = As2Support.peerSend(f.hook("pacific-in"), Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID), "async-evil@pacific", payload, intruder, f.ours.certificate(), true, true, true,
         "https://evil.example.com/collect");
      assertThat(As2Support.peerReadMdn(unauthenticated, f.ours.certificate()).disposition()).endsWith("authentication-failed");
      assertThat(rows("edi_as2_mdn")).noneMatch(r -> String.valueOf(r.getValueString("deliveryUrl")).contains("evil.example.com"));
      assertThat(new As2MdnSender().runOnce()).isEqualTo(1);
      assertThat(posted).hasSize(before + 1);
   }

   /*******************************************************************************
    ** Rollover: inbound accepts our ACTIVE and NEXT keys and the partner ACTIVE and
    ** NEXT certificates; Promote Certificate switches outbound and retires the old
    ** certificate, which inbound then refuses.
    *******************************************************************************/
   @Test
   void rolloverAndPromote() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      Party oursNext = As2Support.party("qqq-local-next", 365);
      Party pacificNext = As2Support.party("pacific-next", 365);
      As2Support.ENV.put("AS2_KEY_OURS_NEXT", oursNext.keyPem());
      QRecord oursNextRow = f.certificate("qqq-local-next", oursNext, null, "NEXT", "AS2_KEY_OURS_NEXT");
      QRecord pacificNextRow = f.certificate("pacific-next", pacificNext, f.partnerId, "NEXT", null);
      f.fromPeer(Map.of());
      QRecord out = f.toPeer("http://127.0.0.1:1/unused", "SYNC");
      byte[] payload = edi("ROLL").getBytes(StandardCharsets.US_ASCII);
      assertThat(As2Support.peerReadMdn(f.peerSends("pacific-in", "roll-active@pacific", payload, f.pacific, f.ours.certificate(), false), f.ours.certificate()).disposition()).endsWith("processed");
      As2Support.Reply viaNext = f.peerSends("pacific-in", "roll-next@pacific", payload, pacificNext, oursNext.certificate(), false);
      assertThat(As2Support.peerReadMdn(viaNext, f.ours.certificate()).disposition()).as("NEXT keys").endsWith("processed");
      assertThat(rows("edi_inbox")).hasSize(2);
      var warned = action("edi_certificate_promote", row("edi_certificate", f.oursLocalId), new RunBackendStepOutput());
      assertThat(warned.getProcessSummary(new RunBackendStepOutput(), true).toString()).contains("only a NEXT certificate can be promoted");
      assertThat(row("edi_certificate", f.oursLocalId).getValueString("status")).isEqualTo("ACTIVE");
      try(PeerServer peer = new PeerServer(f.pacific, f.ours.certificate()))
      {
         SystemWrites.call(() ->
         {
            SystemWrites.update("edi_transport", new QRecord().withValue("id", out.getValue("id")).withValue("as2Url", peer.url()));
            return (null);
         });
         assertThat(f.send(f.outbound(out, edi("BEFORE"))).getValueString("status")).isEqualTo("DELIVERED");
         assertThat(peer.received.getFirst().signed()).isTrue();
         action("edi_certificate_promote", row("edi_certificate", oursNextRow.getValue("id")), new RunBackendStepOutput());
         action("edi_certificate_promote", row("edi_certificate", pacificNextRow.getValue("id")), new RunBackendStepOutput());
         assertThat(row("edi_certificate", oursNextRow.getValue("id")).getValueString("status")).isEqualTo("ACTIVE");
         assertThat(row("edi_certificate", f.oursLocalId).getValueString("status")).isEqualTo("RETIRED");
         assertThat(row("edi_certificate", pacificNextRow.getValue("id")).getValueString("status")).isEqualTo("ACTIVE");
         assertThat(row("edi_certificate", f.pacificPublicId).getValueString("status")).isEqualTo("RETIRED");
         peer.senderCertificates.clear();
         peer.senderCertificates.add(oursNext.certificate());
         peer.decryptionKeys.clear();
         peer.decryptionKeys.add(pacificNext);
         peer.self = pacificNext;
         QRecord after = f.send(f.outbound(out, edi("AFTER")));
         assertThat(after.getValueString("status")).as(after.getValueString("lastError")).isEqualTo("DELIVERED");
         assertThat(peer.received).hasSize(2);
      }
      assertThat(As2Support.peerReadMdn(f.peerSends("pacific-in", "roll-retired@pacific", payload, pacificNext, f.ours.certificate(), false), oursNext.certificate()).disposition()).endsWith("decryption-failed");
      assertThat(As2Support.peerReadMdn(f.peerSends("pacific-in", "roll-promoted@pacific", payload, pacificNext, oursNext.certificate(), false), oursNext.certificate()).disposition()).endsWith("processed");
      assertThat(As2Support.peerReadMdn(f.peerSends("pacific-in", "roll-oldpartner@pacific", payload, f.pacific, oursNext.certificate(), false), oursNext.certificate()).disposition()).endsWith("authentication-failed");
   }

   /*******************************************************************************
    ** Expiry alerts cover ACTIVE and NEXT certificates (not RETIRED), per threshold.
    *******************************************************************************/
   @Test
   void expiryAlertsForActiveAndNext() throws Exception
   {
      Fixture f = fixture();
      QRecord active = f.certificate("active-soon", As2Support.party("active-soon", 10), f.partnerId, "ACTIVE", null);
      QRecord next = f.certificate("next-soon", As2Support.party("next-soon", 5), f.partnerId, "NEXT", null);
      QRecord retired = f.certificate("retired-soon", As2Support.party("retired-soon", 3), f.partnerId, "RETIRED", null);
      QRecord expired = f.certificate("expired", As2Support.create("expired", Instant.now().minusSeconds(10 * 86400), Instant.now().minusSeconds(86400)), f.partnerId, "ACTIVE", null);
      QRecord distant = f.certificate("distant", As2Support.party("distant", 300), f.partnerId, "NEXT", null);
      new AckMonitor().runOnce();
      assertThat(Alerts.RAISED).contains("edi.certificate.expiring|edi_certificate:" + active.getValue("id") + ":30", "edi.certificate.expiring|edi_certificate:" + active.getValue("id") + ":14")
         .doesNotContain("edi.certificate.expiring|edi_certificate:" + active.getValue("id") + ":7");
      assertThat(Alerts.RAISED).contains("edi.certificate.expiring|edi_certificate:" + next.getValue("id") + ":30", "edi.certificate.expiring|edi_certificate:" + next.getValue("id") + ":14",
         "edi.certificate.expiring|edi_certificate:" + next.getValue("id") + ":7");
      assertThat(Alerts.RAISED).contains("edi.certificate.expired|edi_certificate:" + expired.getValue("id") + ":expired");
      assertThat(Alerts.RAISED).noneMatch(a -> a.contains("edi_certificate:" + retired.getValue("id") + ":") || a.contains("edi_certificate:" + distant.getValue("id") + ":"));
   }

   /*******************************************************************************
    ** SENT AS2 messages with asynchronous receipts raise the overdue warning only
    ** after the due time, once per message, and the MDN resolves it.
    *******************************************************************************/
   @Test
   void overdueMdnAlert() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      try(PeerServer peer = new PeerServer(f.pacific, f.ours.certificate()))
      {
         peer.silent.set(true);
         QRecord out = f.toPeer(peer.url(), "ASYNC");
         QRecord sent = f.send(f.outbound(out, edi("SILENT")));
         assertThat(sent.getValueString("status")).isEqualTo("SENT");
         String key = "edi.mdn.overdue|edi_outbound_message:" + sent.getValue("id");
         clockAt(Instant.now().plusSeconds(59 * 60));
         new AckMonitor().runOnce();
         assertThat(Alerts.RAISED).doesNotContain(key);
         clockAt(Instant.now().plusSeconds(62 * 60));
         new AckMonitor().runOnce();
         assertThat(Alerts.RAISED).contains(key);
         Map<String, String> ids = Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID, "Message-ID", "<late-mdn@peer>");
         byte[] mdn = peer.mdn(sent.getValueString("transportMessageId"), "automatic-action/MDN-sent-automatically; processed", sent.getValueString("transportMic"), true);
         assertThat(As2Support.post(f.hook("pacific-out") + "/mdn", mdn, ids).status()).isEqualTo(200);
         assertThat(Alerts.RESOLVED).contains(key);
         assertThat(row("edi_outbound_message", sent.getValue("id")).getValueString("status")).isEqualTo("DELIVERED");
         QRecord failing = f.send(f.outbound(out, edi("SILENT2")));
         byte[] bad = peer.mdn(failing.getValueString("transportMessageId"), "automatic-action/MDN-sent-automatically; processed", Base64.getEncoder().encodeToString(new byte[32]) + ", sha-256", true);
         assertThat(As2Support.post(f.hook("pacific-out") + "/mdn", bad, Map.of("AS2-From", PEER_ID, "AS2-To", LOCAL_ID, "Message-ID", "<bad-mic@peer>")).status()).isEqualTo(200);
         QRecord failed = row("edi_outbound_message", failing.getValue("id"));
         assertThat(failed.getValueString("status")).isEqualTo("FAILED");
         assertThat(failed.getValueString("lastError")).contains("MIC");
         assertThat(Alerts.RAISED).contains("edi.send.failed|edi_outbound_message:" + failing.getValue("id"));
      }
   }

   /*******************************************************************************
    ** A missing or mismatched key fails plainly, naming only the variable.
    *******************************************************************************/
   @Test
   void missingEnvironmentVariable() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      Capture capture = new Capture();
      try(PeerServer peer = new PeerServer(f.pacific, f.ours.certificate()))
      {
         QRecord out = f.toPeer(peer.url(), "SYNC");
         As2Support.ENV.remove("AS2_KEY_OURS");
         QRecord message = f.outbound(out, edi("NOKEY"));
         QRecord missing = f.send(message);
         assertThat(missing.getValueString("status")).isEqualTo("RETRY_WAIT");
         assertThat(missing.getValueString("lastError")).isEqualTo("environment variable AS2_KEY_OURS is not set");
         As2Support.ENV.put("AS2_KEY_OURS", f.pacific.keyPem());
         clockAt(Instant.now().plusSeconds(120));
         QRecord mismatch = f.send(message);
         assertThat(mismatch.getValueString("lastError")).isEqualTo("The private key in environment variable AS2_KEY_OURS does not match certificate qqq-local");
         As2Support.ENV.put("AS2_KEY_OURS", "-----BEGIN PRIVATE KEY-----\nnot-a-key\n-----END PRIVATE KEY-----");
         clockAt(Instant.now().plusSeconds(600));
         assertThat(f.send(message).getValueString("lastError")).isEqualTo("The private key in environment variable AS2_KEY_OURS could not be read as a PEM PKCS#8 key or a base64 PKCS#12 store");
         assertThat(peer.received).isEmpty();
         f.fromPeer(Map.of());
         As2Support.ENV.remove("AS2_KEY_OURS");
         As2Support.Reply reply = f.peerSends("pacific-in", "nokey@pacific", edi("NOKEY").getBytes(StandardCharsets.US_ASCII), f.pacific, f.ours.certificate(), false);
         assertThat(reply.status()).isEqualTo(500);
         assertThat(rows("edi_inbox")).isEmpty();
         assertThat(capture.lines).noneMatch(l -> l.contains("PRIVATE KEY") || l.contains(f.pacific.keyPem().substring(40, 90)) || l.contains(f.ours.keyPem().substring(40, 90)));
         assertThat(capture.lines).anyMatch(l -> l.contains("AS2_KEY_OURS"));
      }
      finally
      {
         capture.detach();
      }
      assertThat(new As2Transport().test(new EdiTransport().withName("t").withDirection("INBOUND").withAs2LocalId(LOCAL_ID).withAs2PartnerId(PEER_ID).withSignAlgorithm("SHA256")
         .withLocalCertificateId(f.oursLocalId).withPartnerCertificateId(f.pacificPublicId)).summary()).contains("environment variable AS2_KEY_OURS is not set");
   }

   /*******************************************************************************
    ** Test Connection resolves keys and certificates and probes the endpoint
    ** without sending a document.
    *******************************************************************************/
   @Test
   void testConnection() throws Exception
   {
      Fixture f = fixture().peerCertificates();
      try(PeerServer peer = new PeerServer(f.pacific, f.ours.certificate()))
      {
         QRecord out = f.toPeer(peer.url(), "SYNC");
         var result = new As2Transport().test(out.toEntity(EdiTransport.class));
         assertThat(result.success()).isTrue();
         assertThat(result.summary()).contains("Local key qqq-local resolved", "1 partner certificate(s)", "Endpoint responded with HTTP 200");
         assertThat(peer.received).isEmpty();
         var weak = new As2Transport().test(out.toEntity(EdiTransport.class).withSignAlgorithm("SHA1"));
         assertThat(weak.summary()).contains("weak crypto");
         var down = new As2Transport().test(out.toEntity(EdiTransport.class).withAs2Url("http://127.0.0.1:1/as2"));
         assertThat(down.success()).isFalse();
         assertThat(down.summary()).contains("could not be reached");
         var execute = action("edi_transport_test", out, new RunBackendStepOutput());
         assertThat(execute.getProcessSummary(new RunBackendStepOutput(), true).toString()).contains("Endpoint responded");
      }
   }

   /*******************************************************************************
    ** The TLS pin accepts only the pinned certificate; a PKCS#12 key from the
    ** environment works with its password variable.
    *******************************************************************************/
   @Test
   void pinningAndPkcs12Keys() throws Exception
   {
      Fixture f = fixture();
      var pinned = As2Http.pinnedTrustManager(f.pacific.certificate());
      pinned.checkServerTrusted(new java.security.cert.X509Certificate[] {f.pacific.certificate()}, "RSA");
      assertThatThrownBy(() -> pinned.checkServerTrusted(new java.security.cert.X509Certificate[] {f.ours.certificate()}, "RSA")).isInstanceOf(CertificateException.class).hasMessageContaining("pinned");
      assertThat(As2Http.isAllowedUrl("https://partner.example.com/as2")).isTrue();
      assertThat(As2Http.isAllowedUrl("http://partner.example.com/as2")).isFalse();
      assertThat(As2Http.isAllowedUrl("https://user:secret@partner.example.com/as2")).isFalse();
      KeyStore store = KeyStore.getInstance("PKCS12");
      store.load(null, null);
      store.setKeyEntry("ours", f.ours.key(), "synthetic-pass".toCharArray(), new java.security.cert.Certificate[] {f.ours.certificate()});
      var bytes = new java.io.ByteArrayOutputStream();
      store.store(bytes, "synthetic-pass".toCharArray());
      As2Support.ENV.put("AS2_P12", Base64.getEncoder().encodeToString(bytes.toByteArray()));
      As2Support.ENV.put("AS2_P12_PASSWORD", "synthetic-pass");
      QRecord keyed = SystemWrites.call(() -> SystemWrites.insert("edi_certificate", new QRecord().withValue("alias", "p12").withValue("usage", "SIGNING_AND_ENCRYPTION").withValue("status", "ACTIVE")
         .withValue("certificatePem", f.ours.certificatePem()).withValue("privateKeyEnvVar", "AS2_P12").withValue("privateKeyPasswordEnvVar", "AS2_P12_PASSWORD")));
      var transport = new EdiTransport().withName("p12").withLocalCertificateId(keyed.getValueInteger("id"));
      var key = SystemWrites.call(() -> new com.kingsrook.qbits.edi.as2.CertificateService().activeLocal(transport));
      assertThat(key.certificate()).isEqualTo(f.ours.certificate());
      assertThat(key.toString()).doesNotContain("Private").doesNotContain("modulus");
      As2Support.ENV.remove("AS2_P12_PASSWORD");
      assertThatThrownBy(() -> SystemWrites.call(() -> new com.kingsrook.qbits.edi.as2.CertificateService().activeLocal(transport))).hasRootCauseMessage("environment variable AS2_P12_PASSWORD is not set");
   }

   /*******************************************************************************
    ** The record actions Promote Certificate and Export Public Certificate.
    *******************************************************************************/
   @Test
   void certificateActions() throws Exception
   {
      Fixture f = fixture();
      QRecord next = f.certificate("export-me", f.pacific, f.partnerId, "NEXT", null);
      RunBackendStepOutput output = new RunBackendStepOutput();
      var export = action("edi_certificate_export", next, output);
      assertThat(export.getProcessSummary(output, true).toString()).contains("public certificate exported");
      assertThat(output.getValueString("downloadFileName")).isEqualTo("export-me.pem");
      var storage = new StorageInput(output.getValueString("storageTableName")).withReference(output.getValueString("storageReference"));
      try(var download = new StorageAction().getInputStream(storage))
      {
         String pem = new String(download.readAllBytes(), StandardCharsets.UTF_8);
         assertThat(pem).isEqualTo(f.pacific.certificatePem()).doesNotContain("PRIVATE");
      }
      QRecord current = f.certificate("current", f.ours, f.partnerId, "ACTIVE", null);
      QRecord replacement = f.certificate("replacement", f.pacific, f.partnerId, "NEXT", null);
      action("edi_certificate_promote", replacement, new RunBackendStepOutput());
      assertThat(row("edi_certificate", replacement.getValue("id")).getValueString("status")).isEqualTo("ACTIVE");
      assertThat(row("edi_certificate", current.getValue("id")).getValueString("status")).isEqualTo("RETIRED");
      assertThat(row("edi_certificate", next.getValue("id")).getValueString("status")).as("another NEXT certificate is untouched").isEqualTo("NEXT");
   }

   /*******************************************************************************
    ** A VAN transport has no partner; many partners arrive over one AS2 connection
    ** and are routed by their ISA identities, not by the transport.
    *******************************************************************************/
   @Test
   void vanOverAs2() throws Exception
   {
      InboundTest inbound = new InboundTest();
      inbound.setup("004010");
      Fixture f = startHooks(QContext.getQInstance());
      Integer vanPartner = f.partner("Synthetic VAN").getValueInteger("id");
      Integer oursLocal = f.certificate("qqq-local", f.ours, null, "ACTIVE", "AS2_KEY_OURS").getValueInteger("id");
      Integer vanCertificate = f.certificate("van", f.pacific, vanPartner, "ACTIVE", null).getValueInteger("id");
      QRecord van = f.transport("van", "INBOUND", LOCAL_ID, "SYNTHETIC-VAN", oursLocal, vanCertificate, null, null, "SYNC", Map.of("isVan", true, "vanName", "Synthetic VAN"));
      assertThat(van.getValue("partnerId")).isNull();
      for(int seed : List.of(17, 18))
      {
         String text = Synthetic850.generate(seed, "004010", com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT).replace("000000001", String.format("%09d", seed));
         As2Support.Reply reply = As2Support.peerSend(f.hook("van"), Map.of("AS2-From", "SYNTHETIC-VAN", "AS2-To", LOCAL_ID), "van-" + seed + "@van", text.getBytes(StandardCharsets.US_ASCII), f.pacific, f.ours.certificate(), false, true, true, null);
         assertThat(As2Support.peerReadMdn(reply, f.ours.certificate()).disposition()).endsWith("processed");
      }
      assertThat(rows("edi_inbox")).hasSize(2).allSatisfy(r ->
      {
         assertThat(r.getValueString("transportType")).isEqualTo("AS2");
         assertThat(r.getValue("transportId")).isEqualTo(van.getValue("id"));
      });
      assertThat(new InboxWorker().runOnce()).isEqualTo(2);
      Integer synthetic = rows("edi_partner").stream().filter(r -> "SYNTH".equals(r.getValueString("code"))).findFirst().orElseThrow().getValueInteger("id");
      assertThat(rows("edi_interchange")).filteredOn(r -> "INBOUND".equals(r.getValueString("direction"))).hasSize(2).allSatisfy(r -> assertThat(r.getValueInteger("partnerId")).isEqualTo(synthetic));
      assertThat(rows("edi_as2_mdn")).hasSize(2).allSatisfy(r -> assertThat(r.getValue("partnerId")).isNull());
   }

   /*******************************************************************************
    ** A complete round trip creates no file anywhere under the JVM temp directory,
    ** and no secret or private key reaches a log line, a receipt row or an error.
    *******************************************************************************/
   @Test
   void noSecretsAndNoTemporaryFiles() throws Exception
   {
      Capture capture = new Capture();
      List<String> before = As2Support.tempNames();
      Fixture f = fixture();
      try
      {
         QRecord out = f.loopback("SYNC", Map.of("compress", true, "encryptAlgorithm", "AES256_GCM"));
         assertThat(f.send(f.outbound(out, edi("TMP1"))).getValueString("status")).isEqualTo("DELIVERED");
         QRecord async = SystemWrites.call(() -> SystemWrites.insert("edi_transport", new QRecord().withValue("name", "pacific-async").withValue("type", "AS2").withValue("direction", "OUTBOUND")
            .withValue("partnerId", f.partnerId).withValue("as2LocalId", LOCAL_ID).withValue("as2PartnerId", PEER_ID).withValue("as2Url", f.hook("pacific-in")).withValue("signAlgorithm", "SHA256")
            .withValue("encryptAlgorithm", "AES128_CBC").withValue("mdnMode", "ASYNC").withValue("mdnSigned", true).withValue("localCertificateId", f.oursLocalId)
            .withValue("partnerCertificateId", f.pacificPublicId)));
         assertThat(f.send(f.outbound(async, edi("TMP2"))).getValueString("status")).isEqualTo("SENT");
         As2Support.Reply refused = As2Support.peerSend(f.hook("pacific-in"), Map.of("AS2-From", LOCAL_ID, "AS2-To", PEER_ID), "tmp-bad@x", edi("TMP3").getBytes(StandardCharsets.US_ASCII),
            As2Support.party("intruder", 365), f.pacific.certificate(), true, true, true, null);
         assertThat(refused.status()).isEqualTo(200);
         assertThat(new As2MdnSender().runOnce()).isEqualTo(1);
         As2Support.ENV.remove("AS2_KEY_OURS");
         clockAt(Instant.now().plusSeconds(3600));
         f.send(f.outbound(out, edi("TMP4")));
      }
      finally
      {
         capture.detach();
      }
      List<String> secrets = new ArrayList<>();
      for(Party party : List.of(f.ours, f.pacific, As2Support.party("intruder", 365)))
      {
         secrets.add(Base64.getEncoder().encodeToString(party.key().getEncoded()).substring(20, 90));
      }
      secrets.add("PRIVATE KEY");
      for(String secret : secrets)
      {
         assertThat(capture.lines).as("log lines").noneMatch(l -> l.contains(secret));
         assertThat(rows("edi_as2_mdn")).as("receipt rows").noneMatch(r -> r.getValues().values().stream().anyMatch(v -> v != null && v.toString().contains(secret)));
         assertThat(rows("edi_outbound_message")).as("messages").noneMatch(r -> String.valueOf(r.getValueString("lastError")).contains(secret));
         for(QRecord mdn : rows("edi_as2_mdn"))
         {
            assertThat(payload(mdn.getValueString("payloadRef"))).as("stored receipts").doesNotContain(secret);
         }
      }
      List<String> created = new ArrayList<>(As2Support.tempNames());
      created.removeAll(before);
      assertThat(created).as("files created under java.io.tmpdir").allMatch(name -> name.startsWith("claude") || name.startsWith("surefire") || name.startsWith("jacoco") || name.startsWith("junit"));
   }
}
