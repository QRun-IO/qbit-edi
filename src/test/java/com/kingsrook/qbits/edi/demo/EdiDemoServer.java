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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.H2Support;
import com.kingsrook.qbits.edi.RetailSupport;
import com.kingsrook.qbits.edi.Synthetic850;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.flow.OutboundEnqueue;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.flow.SendWorker;
import com.kingsrook.qbits.edi.flow.AckMonitor;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayOutbox;
import com.kingsrook.qbits.edi.transport.gateway.standin.OutboxFileRef;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.FullyAnonymousAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import io.javalin.Javalin;

/*******************************************************************************
 ** Disposable, synthetic H2 demo. Real pipeline; in-memory gateway remote boundary.
 *******************************************************************************/
public class EdiDemoServer extends AbstractQQQApplication implements AutoCloseable
{
   private final QInstance instance;
   private QApplicationJavalinServer server;
   private Javalin javalin;
   private Map<String, String> authenticationModules;
   private String previousAuthentication;
   private final Map<String, byte[]> outbox = new java.util.concurrent.ConcurrentHashMap<>();

   /*******************************************************************************
    ** Allocate a fresh database; payload keys and certificate keys stay in memory.
    *******************************************************************************/
   public EdiDemoServer() throws Exception
   {
      instance = H2Support.create();
      RetailSupport.tables();
      instance.setHomeAppName("edi");
      for(String table : List.of("sample_order", "sample_shipment", "sample_invoice", "sample_inventory"))
      {
         EdiRuntime.current().config().withSendAsEdiTable(table);
      }
      com.kingsrook.qbits.edi.processes.EdiProcesses.processes(EdiRuntime.current().config()).stream().filter(process -> !instance.getProcesses().containsKey(process.getName())).forEach(instance::addProcess);
      installOutbox();
   }

   /*******************************************************************************
    ** Reconnect the in-memory gateway after the intentional dead-letter scenario.
    *******************************************************************************/
   private void installOutbox()
   {
      FileGatewayOutbox.register((endpoint, name, content, size) ->
      {
         byte[] bytes = content.readAllBytes();
         outbox.put(endpoint + "/" + name, bytes);
         return (new OutboxFileRef(Integer.valueOf(name.substring(4, name.indexOf('.'))), endpoint, name, name, size,
            com.kingsrook.qbits.edi.storage.PayloadCrypto.digest(bytes), Instant.now()));
      });
   }

   /*******************************************************************************
    ** Share the same metadata instance with the application server and workers.
    *******************************************************************************/
   @Override
   public QInstance defineQInstance()
   {
      return (instance);
   }

   /*******************************************************************************
    ** Idempotent setup with source records and documents processed through real flows.
    *******************************************************************************/
   public void seed() throws Exception
   {
      QContext.init(instance, new QSystemUserSession(instance));
      SystemWrites.call(() ->
      {
         if(!EdiRecords.query("edi_partner", new QQueryFilter()).isEmpty())
         {
            return (null);
         }
         EdiRuntime.current().initialize();
         QRecord local = SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "QQQTEST").withValue("applicationCode", "QQQTEST").withValue("isActive", true));
         List<QRecord> agreements = new ArrayList<>();
         String[] names = {"Northwind Retail", "Harbor Logistics", "Summit Market"};
         String[] identities = {"SYNTHETIC", "HARBORLOG", "SUMMITMKT"};
         String[] codes = {"NORTHWIND", "HARBOR", "SUMMIT"};
         for(int i = 0; i < names.length; i++)
         {
            QRecord partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", names[i]).withValue("code", codes[i]).withValue("contactName", names[i] + " Demo Contact")
               .withValue("contactEmail", codes[i].toLowerCase(java.util.Locale.ROOT) + "@example.invalid").withValue("contactPhone", "202-555-010" + i).withValue("status", "ACTIVE").withValue("defaultTimezone", "America/Chicago"));
            QRecord remote = SystemWrites.insert("edi_identity", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", identities[i]).withValue("applicationCode", identities[i]).withValue("isActive", true));
            QRecord settings = new QRecord().withValue("partnerId", partner.getValue("id")).withValue("name", List.of("northwind-gateway", "harbor-sftp", "summit-https").get(i))
               .withValue("type", List.of("GATEWAY", "SFTP", "HTTPS").get(i)).withValue("direction", "BOTH").withValue("isActive", true).withValue("maxAttempts", 1);
            if(i == 0)
            {
               settings.withValue("gatewayEndpointName", "northwind-outbox");
            }
            else if(i == 1)
            {
               settings.withValue("sftpHost", "sftp.example.invalid").withValue("sftpUsername", "synthetic").withValue("sftpPasswordEnvVar", "DEMO_SFTP_PASSWORD")
                  .withValue("sftpHostKeyFingerprint", "SHA256:demo-not-a-real-host").withValue("sftpOutboundPath", "/outbox").withValue("sftpInboundPath", "/inbox");
            }
            else
            {
               settings.withValue("httpsUrl", "https://example.invalid/edi").withValue("httpsAuthType", "BEARER").withValue("httpsInboundAuth", "BEARER").withValue("httpsSecretEnvVar", "DEMO_HTTPS_SECRET");
            }
            QRecord transport = SystemWrites.insert("edi_transport", settings);
            QRecord agreement = SystemWrites.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", local.getValue("id"))
               .withValue("partnerIdentityId", remote.getValue("id")).withValue("standard", "X12").withValue("usageIndicator", "T").withValue("interchangeVersion", "00401")
               .withValue("isActive", true).withValue("applyTestDocuments", true).withValue("outboundTransportId", i == 0 ? transport.getValue("id") : null));
            agreements.add(agreement);
            for(String document : List.of("850", "860", "997"))
            {
               QRecord map = document.equals("997") ? null : RetailSupport.installMap(document, "004010");
               QRecord guide = EdiRecords.query("edi_guide", EdiRecords.equal("transactionSet", document).withCriteria("version", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, "004010")).getFirst();
               SystemWrites.insert("edi_agreement_document", new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", "INBOUND")
                  .withValue("transactionSet", document).withValue("version", "004010").withValue("guideId", guide.getValue("id")).withValue("mapId", map == null ? null : map.getValue("id"))
                  .withValue("ackExpectation", document.equals("997") ? "NONE" : "997").withValue("ackLevel", "ALL_TRANSACTIONS").withValue("isActive", true));
            }
         }
         for(int i = 0; i < 40; i++)
         {
            String text = Synthetic850.generate(i, "004010", X12Separators.DEFAULT).replace("SYNTHETIC", identities[i % 3])
               .replace("000000001", String.format("%09d", 100 + i)).replace("*1200*1*X*", "*1200*" + (100 + i) + "*X*").replace("GE*1*1~", "GE*1*" + (100 + i) + "~");
            if(i % 5 == 1)
            {
               text = changeOrder(text);
            }
            if(i % 11 == 3)
            {
               text = text.replace("*2*EA*", "*BAD*EA*");
            }
            if(i == 38)
            {
               text = text.replace(identities[i % 3], "UNMATCHED");
            }
            if(i == 39)
            {
               text = Synthetic850.generate(0, "004010", X12Separators.DEFAULT).replace("000000001", "000000100").replace("*1200*1*X*", "*1200*100*X*").replace("GE*1*1~", "GE*1*100~");
            }
            receive(text, "demo-in-" + i);
         }
         while(new InboxWorker().runOnce() > 0)
         {
            // Drain the bounded inbox in the demo setup only.
         }
         for(String document : List.of("855", "856", "810", "846"))
         {
            QRecord map = RetailSupport.installMap(document, "004010");
            String table = map.getValueString("tableName");
            var guide = EdiRuntime.current().guides().resource(document + "-004010");
            SystemWrites.insert("edi_agreement_document", new QRecord().withValue("agreementId", agreements.getFirst().getValue("id")).withValue("direction", "OUTBOUND")
               .withValue("transactionSet", document).withValue("functionalIdCode", guide.functionalIdCode()).withValue("version", "004010").withValue("guideId", map.getValue("guideId"))
               .withValue("mapId", map.getValue("id")).withValue("sourceTableName", table).withValue("outboundTrigger", "MANUAL").withValue("batchMode", "IMMEDIATE")
               .withValue("ackExpectation", "997").withValue("ackDueMinutes", 1440).withValue("isActive", true));
            QRecord source = SystemWrites.transaction(() -> RetailSupport.saveSource(table, RetailSupport.source()));
            OutboundEnqueue.enqueue(table, source, "MANUAL", null);
         }
         new OutboundWorker().runOnce();
         // Only Northwind has outbound definitions; other partners have no send path.
         while(new SendWorker().runOnce() > 0)
         {
            // Deliver all queued gateway replies and retail documents.
         }
         var messages = EdiRecords.query("edi_outbound_message", new QQueryFilter().withCriteria("interchangeId", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IS_NOT_BLANK));
         for(int i = 0; i < Math.min(2, messages.size()); i++)
         {
            QRecord message = messages.get(i);
            try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(message.getValueString("payloadRef"), null, 0)))
            {
               var sent = EdiRuntime.current().reader().read(stream).getFirst();
               var ack = EdiRuntime.current().ack997().build(sent.groups().getFirst(), Map.of(), com.kingsrook.qbits.edi.ack.AckLevel.ALL_TRANSACTIONS, "0001");
               var reply = com.kingsrook.qbits.edi.ack.AckEnvelope.wrap(sent, sent.groups().getFirst(), ack, String.valueOf(900 + i), String.valueOf(900 + i), java.time.LocalDateTime.now(ZoneOffset.UTC));
               receive(EdiRuntime.current().writer().write(reply), "demo-ack-" + i);
            }
         }
         new InboxWorker().runOnce();
         var groups = EdiRecords.query("edi_functional_group", EdiRecords.equal("ackStatus", "PENDING"));
         if(!groups.isEmpty())
         {
            SystemWrites.update("edi_functional_group", new QRecord().withValue("id", groups.getFirst().getValue("id")).withValue("ackDueAt", Instant.now().minusSeconds(3600)));
         }
         certificate("demo-signing", 12);
         certificate("demo-expired", -1);
         new AckMonitor().runOnce();
         if(!messages.isEmpty())
         {
            FileGatewayOutbox.register(null);
            SystemWrites.update("edi_outbound_message", new QRecord().withValue("id", messages.getLast().getValue("id")).withValue("status", "QUEUED").withValue("attempts", 0));
            new SendWorker().runOnce();
            installOutbox();
         }
         return (null);
      });
   }

   /*******************************************************************************
    ** Synthetic change order retains a valid envelope and guide-compatible details.
    *******************************************************************************/
   public static String changeOrder(String text)
   {
      text = text.replace("GS*PO*", "GS*PC*").replace("ST*850*", "ST*860*")
         .replace("BEG*00*NE*", "BCH*04*NE*").replace("**20261013~", "***20261013~")
         .replace("PO1*1*2*EA*10.50**VN*SYNTH-1~", "POC*1*CA*3**EA*10.50**VN*SYNTH-1~")
         .replace("PID*F****Synthetic item~", "").replace("SE*9*", "SE*8*");
      return (text);
   }

   /*******************************************************************************
    ** Every seed document follows the public durable intake contract.
    *******************************************************************************/
   private static void receive(String text, String id) throws Exception
   {
      new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("GATEWAY", id, "synthetic.x12", "127.0.0.1", new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
   }

   /*******************************************************************************
    ** Generate self-signed synthetic certificates in memory, storing public PEM only.
    *******************************************************************************/
   private static void certificate(String alias, int days) throws Exception
   {
      var key = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair();
      var subject = new org.bouncycastle.asn1.x500.X500Name("CN=" + alias);
      Instant now = Instant.now();
      var builder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(subject, java.math.BigInteger.valueOf(Math.abs(alias.hashCode())),
         java.util.Date.from(now.minusSeconds(86400 * 365L)), java.util.Date.from(now.plusSeconds(days * 86400L)), subject, key.getPublic());
      var holder = builder.build(new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA").build(key.getPrivate()));
      String pem = "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(holder.getEncoded()) + "\n-----END CERTIFICATE-----\n";
      SystemWrites.insert("edi_certificate", new QRecord().withValue("alias", alias).withValue("usage", "SIGNING").withValue("certificatePem", pem).withValue("subject", subject.toString())
         .withValue("issuer", subject.toString()).withValue("serialNumber", holder.getSerialNumber().toString()).withValue("fingerprintSha256", com.kingsrook.qbits.edi.storage.PayloadCrypto.digest(holder.getEncoded()))
         .withValue("notBefore", holder.getNotBefore().toInstant()).withValue("notAfter", holder.getNotAfter().toInstant()).withValue("status", "ACTIVE"));
   }

   /*******************************************************************************
    ** Test-only anonymous system session. Never use this module in a production host.
    *******************************************************************************/
   public static class DemoAuthentication extends FullyAnonymousAuthenticationModule
   {
      /*******************************************************************************
       ** Grant access to synthetic records and all demo actions.
       *******************************************************************************/
      @Override
      public QSession createSession(QInstance instance, Map<String, String> context)
      {
         QSession anonymous = super.createSession(instance, context);
         QSession session = new QSystemUserSession(instance);
         session.setIdReference(anonymous.getIdReference());
         session.setUuid(anonymous.getUuid());
         session.setUser(anonymous.getUser());
         session.setValues(anonymous.getValues());
         return (session);
      }
   }

   /*******************************************************************************
    ** Launch on the requested port, including zero for acceptance tests.
    *******************************************************************************/
   public int start(int port) throws Exception
   {
      var field = QAuthenticationModuleDispatcher.class.getDeclaredField("authenticationTypeToModuleClassNameMap");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, String> modules = (Map<String, String>) field.get(null);
      authenticationModules = modules;
      previousAuthentication = modules.put(QAuthenticationType.FULLY_ANONYMOUS.getName(), DemoAuthentication.class.getName());
      new QInstanceValidator().validate(instance);
      server = new QApplicationJavalinServer(this).withPort(port).withServeFrontendNext(true).withJavalinConfigurationCustomizer(app -> javalin = app);
      server.start();
      return (javalin.port());
   }

   /*******************************************************************************
    ** Stop only resources owned by this demo.
    *******************************************************************************/
   @Override
   public void close()
   {
      if(server != null)
      {
         server.stop();
      }
      if(authenticationModules != null)
      {
         authenticationModules.put(QAuthenticationType.FULLY_ANONYMOUS.getName(), previousAuthentication);
      }
      FileGatewayOutbox.register(null);
      QContext.clear();
   }

   /*******************************************************************************
    ** Test-classpath launch, no external services or credentials required.
    *******************************************************************************/
   public static void main(String[] args) throws Exception
   {
      EdiDemoServer demo = new EdiDemoServer();
      demo.seed();
      int port = demo.start(args.length == 0 ? 8099 : Integer.parseInt(args[0]));
      Runtime.getRuntime().addShutdownHook(new Thread(demo::close));
      System.out.println("EDI demo: http://localhost:" + port + "/app/edi");
   }
}
