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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.kingsrook.qbits.edi.flow.OutboundEnqueue;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.flow.OutboundActions;
import com.kingsrook.qbits.edi.flow.SendWorker;
import com.kingsrook.qbits.edi.flow.AckMonitor;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayOutbox;
import com.kingsrook.qbits.edi.transport.gateway.standin.OutboxFileRef;
import com.kingsrook.qbits.edi.transport.gateway.EdiFileGatewayConsumer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Database acceptance of triggers, envelope tracking, retries and operator actions.
 *******************************************************************************/
class OutboundTest
{
   private QRecord document;
   private QRecord agreement;
   private QRecord transport;

   /*******************************************************************************
    ** Real metadata and database, with only the remote delivery boundary replaced.
    *******************************************************************************/
   void setup() throws Exception
   {
      H2Support.create();
      RetailSupport.tables();
      EdiRuntime.current().initialize();
      SystemWrites.call(() ->
      {
         QRecord partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", "Retail test").withValue("code", "RETAIL").withValue("status", "ACTIVE"));
         QRecord local = SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "QQQTEST").withValue("applicationCode", "QQQTEST"));
         QRecord remote = SystemWrites.insert("edi_identity", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "SYNTHETIC").withValue("applicationCode", "SYNTHETIC"));
         transport = SystemWrites.insert("edi_transport", new QRecord().withValue("name", "retail-out").withValue("type", "GATEWAY").withValue("direction", "BOTH").withValue("gatewayEndpointName", "retail-outbox").withValue("isActive", true).withValue("maxAttempts", 3));
         agreement = SystemWrites.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", local.getValue("id")).withValue("partnerIdentityId", remote.getValue("id"))
            .withValue("standard", "X12").withValue("usageIndicator", "T").withValue("isActive", true).withValue("outboundTransportId", transport.getValue("id")));
         QRecord map = RetailSupport.installMap("855", "004010");
         document = SystemWrites.insert("edi_agreement_document", new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", "OUTBOUND").withValue("transactionSet", "855")
            .withValue("version", "004010").withValue("functionalIdCode", "PR").withValue("guideId", map.getValue("guideId")).withValue("mapId", map.getValue("id"))
            .withValue("sourceTableName", "sample_order").withValue("outboundTrigger", "RECORD_CHANGE").withValue("batchMode", "IMMEDIATE").withValue("ackExpectation", "997").withValue("ackDueMinutes", 1).withValue("isActive", true));
         return (null);
      });
   }

   /*******************************************************************************
    ** Reset test provider and context.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      FileGatewayOutbox.register(null);
      QContext.clear();
   }

   /*******************************************************************************
    ** Source rollback includes request rollback; source associations are loaded later.
    *******************************************************************************/
   @Test
   void rollbackValidationAndEnvelope() throws Exception
   {
      setup();
      assertThatThrownBy(() -> SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         RetailSupport.saveSource("sample_order", RetailSupport.source());
         assertThat(rows("edi_outbound_request")).hasSize(1);
         throw new IllegalStateException("rollback synthetic write");
      }))).isNotNull();
      assertThat(rows("edi_outbound_request")).isEmpty();
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source())));
      assertThat(new OutboundWorker().runOnce()).isEqualTo(1);
      assertThat(rows("edi_outbound_message")).hasSize(1);
      assertThat(rows("edi_transaction").getFirst().getValueString("businessKey")).isEqualTo("DEMO-1001");
      assertThat(rows("edi_functional_group").getFirst().getValueInstant("ackDueAt")).isNull();
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source().withValue("orderDate", null))));
      new OutboundWorker().runOnce();
      assertThat(rows("edi_outbound_message")).hasSize(1);
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("VALIDATION");
   }

   /*******************************************************************************
    ** Gateway delivery callback, overdue alert, reconciliation, resend and cancel.
    *******************************************************************************/
   @Test
   void sendAckAndActions() throws Exception
   {
      setup();
      var refs = new ArrayList<OutboxFileRef>();
      FileGatewayOutbox.register((endpoint, name, input, size) ->
      {
         assertThat(input.readAllBytes()).hasSize((int) size);
         var ref = new OutboxFileRef(refs.size() + 1, endpoint, name, name, size, null, Instant.now());
         refs.add(ref);
         return (ref);
      });
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source())));
      new OutboundWorker().runOnce();
      QRecord message = rows("edi_outbound_message").getFirst();
      String control = rows("edi_interchange").getFirst().getValueString("controlNumber");
      new SendWorker().runOnce();
      assertThat(rows("edi_outbound_message").getFirst().getValueString("status")).isEqualTo("SENT");
      new EdiFileGatewayConsumer(new InboundSink(EdiRuntime.current().payloads())).onDownloaded(refs.getFirst());
      assertThat(rows("edi_outbound_message").getFirst().getValueString("status")).isEqualTo("DELIVERED");
      assertThat(rows("edi_functional_group").getFirst().getValueInstant("ackDueAt")).isNotNull();
      EdiRuntime.current().config().withClock(Clock.fixed(Instant.now().plusSeconds(120), ZoneOffset.UTC));
      assertThat(new AckMonitor().runOnce()).isEqualTo(1);
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("OVERDUE_ACK");
      SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         OutboundActions.execute("edi_outbound_message", rows("edi_outbound_message").getFirst(), "resend");
         return (null);
      }));
      assertThat(rows("edi_outbound_message").getFirst().getValueString("payloadRef")).isEqualTo(message.getValueString("payloadRef"));
      assertThat(rows("edi_interchange").getFirst().getValueString("controlNumber")).isEqualTo(control);
      SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         OutboundActions.execute("edi_outbound_message", rows("edi_outbound_message").getFirst(), "new");
         OutboundActions.execute("edi_outbound_message", rows("edi_outbound_message").getFirst(), "cancel");
         return (null);
      }));
      assertThat(rows("edi_interchange")).hasSize(2);
      assertThat(rows("edi_interchange").getLast().getValueString("controlNumber")).isNotEqualTo(control);
      assertThat(rows("edi_interchange").getLast().getValueInteger("originalInterchangeId")).isEqualTo(message.getValueInteger("interchangeId"));
      assertThat(rows("edi_outbound_message").getFirst().getValueString("status")).isEqualTo("CANCELLED");
   }

   /*******************************************************************************
    ** Retries do not run early, keep bytes, and terminate at maxAttempts.
    *******************************************************************************/
   @Test
   void retriesAndBatch() throws Exception
   {
      setup();
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_agreement_document", new QRecord().withValue("id", document.getValue("id")).withValue("batchMode", "BATCH").withValue("batchSchedule", "0 0 * * * ?"));
         return (null);
      });
      for(int i = 0; i < 2; i++)
      {
         SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source())));
      }
      assertThat(new OutboundWorker().runOnce()).isZero();
      new OutboundWorker().runDocument(document.getValueInteger("id"));
      assertThat(rows("edi_outbound_message")).hasSize(1);
      assertThat(rows("edi_transaction")).hasSize(2);
      assertThat(rows("edi_transaction")).extracting(r -> r.getValueString("controlNumber")).containsExactly("0001", "0002");
      for(int attempt = 0; attempt < 3; attempt++)
      {
         assertThat(new SendWorker().runOnce()).isEqualTo(1);
         QRecord message = rows("edi_outbound_message").getFirst();
         if(attempt < 2)
         {
            assertThat(message.getValueString("status")).isEqualTo("RETRY_WAIT");
            assertThat(new SendWorker().runOnce()).isZero();
            EdiRuntime.current().config().withClock(Clock.fixed(message.getValueInstant("nextAttemptAt").plusSeconds(1), ZoneOffset.UTC));
         }
      }
      assertThat(rows("edi_outbound_message").getFirst().getValueString("status")).isEqualTo("DEAD_LETTER");
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("TRANSPORT");
      EdiRuntime.current().config().withSchedulerName("test-scheduler");
      com.kingsrook.qbits.edi.processes.OutboundProcesses.installSchedules();
      assertThat(QContext.getQInstance().getProcess("edi_batch_" + document.getValue("id")).getSchedule().getCronExpression()).isEqualTo("0 0 * * * ?");
   }

   /*******************************************************************************
    ** Trigger filters are evaluated in the source transaction; ESB is idempotent.
    *******************************************************************************/
   @Test
   void filtersManualAndEsbProcess() throws Exception
   {
      setup();
      SystemWrites.call(() ->
      {
         var filter = EdiRecords.equal("poNumber", "MATCH");
         SystemWrites.update("edi_agreement_document", new QRecord().withValue("id", document.getValue("id"))
            .withValue("triggerFilter", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(filter)));
         return (null);
      });
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source())));
      assertThat(rows("edi_outbound_request")).isEmpty();
      QRecord source = SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source().withValue("poNumber", "MATCH"))));
      assertThat(rows("edi_outbound_request")).hasSize(1);
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_agreement_document", new QRecord().withValue("id", document.getValue("id")).withValue("outboundTrigger", "ESB_EVENT"));
         return (null);
      });
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_outbound_enqueue")
         .withValues(Map.of("sourceTableName", "sample_order", "sourceRecordId", source.getValueString("id"), "eventId", "synthetic-event-1"));
      var process = new com.kingsrook.qbits.edi.processes.OutboundProcesses();
      var output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      process.run(input, output);
      process.run(input, output);
      assertThat(rows("edi_outbound_request")).hasSize(2);
      assertThat(OutboundEnqueue.enqueue("sample_order", source, "MANUAL", null)).isEqualTo(1);
      input.withProcessName("edi_outbound_worker");
      process.run(input, output);
      assertThat(rows("edi_outbound_message")).hasSize(3);
      input.withProcessName("edi_send_worker");
      process.run(input, output);
      input.withProcessName("edi_ack_monitor");
      process.run(input, output);
   }

   /*******************************************************************************
    ** A real 997 resolves the monitor's exact dedup key and open work-queue record.
    *******************************************************************************/
   @Test
   void monitorResolvesAndCertificates() throws Exception
   {
      setup();
      InboundTest.CapturedAlerts.raised.clear();
      InboundTest.CapturedAlerts.resolved.clear();
      com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance())
         .withSink(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(InboundTest.CapturedAlerts.class));
      FileGatewayOutbox.register((endpoint, name, input, size) -> new OutboxFileRef(1, endpoint, name, name, size, null, Instant.now()));
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source())));
      new OutboundWorker().runOnce();
      new SendWorker().runOnce();
      EdiRuntime.current().config().withClock(Clock.fixed(Instant.now().plusSeconds(120), ZoneOffset.UTC));
      new AckMonitor().runOnce();
      assertThat(InboundTest.CapturedAlerts.raised).contains("edi.ack.overdue");
      QRecord message = rows("edi_outbound_message").getFirst();
      try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(message.getValueString("payloadRef"), null, 0)))
      {
         var original = EdiRuntime.current().reader().read(stream).getFirst();
         var ack = EdiRuntime.current().ack997().build(original.groups().getFirst(), Map.of(), com.kingsrook.qbits.edi.ack.AckLevel.ALL_TRANSACTIONS, "0001");
         var reply = com.kingsrook.qbits.edi.ack.AckEnvelope.wrap(original, original.groups().getFirst(), ack, "999", "999", java.time.LocalDateTime.now(ZoneOffset.UTC));
         new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("HTTPS", "ack-test", null, null,
            new java.io.ByteArrayInputStream(EdiRuntime.current().writer().write(reply).getBytes(java.nio.charset.StandardCharsets.UTF_8))));
      }
      new InboxWorker().runOnce();
      assertThat(InboundTest.CapturedAlerts.resolved).contains("edi.ack.overdue");
      assertThat(rows("edi_exception")).filteredOn(r -> "OVERDUE_ACK".equals(r.getValueString("type"))).allMatch(r -> "RESOLVED".equals(r.getValueString("status")));
      SystemWrites.call(() ->
      {
         SystemWrites.insert("edi_certificate", new QRecord().withValue("alias", "synthetic-expiring").withValue("status", "ACTIVE").withValue("notAfter", Instant.now().plusSeconds(6 * 86400)));
         SystemWrites.insert("edi_certificate", new QRecord().withValue("alias", "synthetic-expired").withValue("status", "ACTIVE").withValue("notAfter", Instant.now().minusSeconds(1)));
         return (null);
      });
      new AckMonitor().runOnce();
      assertThat(InboundTest.CapturedAlerts.raised).contains("edi.certificate.expiring", "edi.certificate.expired");
   }

   /*******************************************************************************
    ** Archived acknowledgement resends retain the acknowledged inbound controls.
    *******************************************************************************/
   @Test
   void resendGeneratedAcknowledgements() throws Exception
   {
      InboundTest fixture = new InboundTest();
      fixture.setup("004010");
      fixture.receive(fixture.raw(), "resend-ack");
      new InboxWorker().runOnce();
      List<QRecord> originals = rows("edi_outbound_message");
      assertThat(originals).isNotEmpty();
      for(QRecord message : originals)
      {
         com.kingsrook.qbits.edi.x12.X12Interchange before;
         try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(message.getValueString("payloadRef"), null, 0)))
         {
            before = EdiRuntime.current().reader().read(stream).getFirst();
         }
         SystemWrites.call(() -> SystemWrites.transaction(() ->
         {
            OutboundActions.execute("edi_outbound_message", message, "new");
            return (null);
         }));
         QRecord resent = rows("edi_outbound_message").getLast();
         try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(resent.getValueString("payloadRef"), null, 0)))
         {
            var after = EdiRuntime.current().reader().read(stream).getFirst();
            assertThat(after.isa().value(13)).isNotEqualTo(before.isa().value(13));
            assertThat(after.serviceSegments()).extracting(sg -> sg.elements()).isEqualTo(before.serviceSegments().stream().map(sg -> sg.elements()).toList());
            for(int i = 0; i < before.groups().size(); i++)
            {
               assertThat(after.groups().get(i).gs().value(6)).isNotEqualTo(before.groups().get(i).gs().value(6));
               assertThat(after.groups().get(i).transactions().getFirst().segments()).extracting(sg -> sg.elements())
                  .isEqualTo(before.groups().get(i).transactions().getFirst().segments().stream().map(sg -> sg.elements()).toList());
            }
         }
      }
   }

   /*******************************************************************************
    ** Read under the system context created by the fixture.
    *******************************************************************************/
   private static List<QRecord> rows(String table) throws Exception
   {
      return (SystemWrites.call(() -> EdiRecords.query(table, new QQueryFilter())));
   }
}
