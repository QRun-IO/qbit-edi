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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.app.EdiHome;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.map.MapPreview;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.storage.PayloadStore;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.gateway.EdiFileGatewayConsumer;
import com.kingsrook.qbits.edi.transport.gateway.standin.InboundFile;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Synthetic H2 vertical slice, including permission, retry and UI boundaries.
 *******************************************************************************/
class InboundTest
{
   private QRecord agreement;
   private QRecord map;
   private QRecord partner;

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   void setup(String version) throws Exception
   {
      var instance = H2Support.create();
      var backend = (RDBMSBackendMetaData) instance.getBackend(EdiRuntime.current().config().getBackendName());
      QTableMetaData order = new QTableMetaData().withName("sample_order").withBackendName(backend.getName()).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id"))
         .withField(new QFieldMetaData("poNumber", QFieldType.STRING).withBackendName("po_number"))
         .withField(new QFieldMetaData("orderDate", QFieldType.DATE).withBackendName("order_date"))
         .withUniqueKey(new UniqueKey("poNumber")).withPermissionRules(new QPermissionRules().withPermissionBaseName("sample.order").withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      QTableMetaData line = new QTableMetaData().withName("sample_order_line").withBackendName(backend.getName()).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id"))
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER).withBackendName("order_id"))
         .withField(new QFieldMetaData("sku", QFieldType.STRING).withBackendName("sku"))
         .withField(new QFieldMetaData("quantity", QFieldType.DECIMAL).withBackendName("quantity"))
         .withField(new QFieldMetaData("unitPrice", QFieldType.DECIMAL).withBackendName("unit_price"))
         .withPermissionRules(new QPermissionRules().withPermissionBaseName("sample.line").withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      order.withAssociation(new Association().withName("lines").withAssociatedTableName("sample_order_line").withJoinName("sample_lines"));
      instance.addTable(order);
      instance.addTable(line);
      instance.addJoin(new QJoinMetaData().withName("sample_lines").withType(JoinType.ONE_TO_MANY).withLeftTable("sample_order").withRightTable("sample_order_line").withJoinOn(new JoinOn("id", "orderId")));
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement())
      {
         statement.execute(H2Support.ddl(order, false));
         statement.execute(H2Support.ddl(line, false));
      }
      EdiRuntime.current().config().withServiceSessionSupplier(() -> new QSession().withPermissions("sample.order.read", "sample.order.insert", "sample.order.edit", "sample.line.read", "sample.line.insert", "sample.line.edit", "sample.line.delete"));
      EdiRuntime.current().initialize();
      SystemWrites.call(() ->
      {
         partner = EdiRecords.insert("edi_partner", new QRecord().withValue("name", "Synthetic Retailer").withValue("code", "SYNTH").withValue("gatewayUsername", "synthetic"));
         QRecord sender = EdiRecords.insert("edi_identity", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "SYNTHETIC").withValue("applicationCode", "SYNTHETIC"));
         QRecord receiver = EdiRecords.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "QQQTEST").withValue("applicationCode", "QQQTEST"));
         agreement = EdiRecords.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", receiver.getValue("id"))
            .withValue("partnerIdentityId", sender.getValue("id")).withValue("standard", "X12").withValue("usageIndicator", "T").withValue("applyTestDocuments", true));
         QRecord guide = EdiRecords.query("edi_guide", EdiRecords.equal("transactionSet", "850").withCriteria("version", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, version)).getFirst();
         ObjectMapper json = new ObjectMapper();
         var definition = json.readTree(InboundTest.class.getResourceAsStream("/edi/maps/850-" + version + "-sample.json"));
         QRecord definitionRecord = new QRecord();
         definition.get("map").fields().forEachRemaining(e -> definitionRecord.withValue(e.getKey(), e.getValue().isInt() ? e.getValue().intValue() : e.getValue().asText()));
         map = EdiRecords.insert("edi_map", definitionRecord.withValue("guideId", guide.getValue("id")));
         for(var rule : definition.get("rules"))
         {
            QRecord r = new QRecord().withValue("mapId", map.getValue("id"));
            rule.fields().forEachRemaining(e -> r.withValue(e.getKey(), e.getValue().isInt() ? e.getValue().intValue() : e.getValue().isBoolean() ? e.getValue().booleanValue() : e.getValue().asText()));
            EdiRecords.insert("edi_map_rule", r);
         }
         EdiRecords.insert("edi_agreement_document", new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", "INBOUND")
            .withValue("transactionSet", "850").withValue("version", version).withValue("guideId", guide.getValue("id")).withValue("mapId", map.getValue("id")).withValue("ackExpectation", "997").withValue("ackLevel", "ALL_TRANSACTIONS"));
         return (null);
      });
   }

   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   QRecord receive(String text, String external) throws Exception
   {
      return (new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("GATEWAY", external, "synthetic.edi", "127.0.0.1", bytes(text))));
   }

   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   static InputStream bytes(String text)
   {
      return (new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII)));
   }

   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   static List<QRecord> rows(String table) throws Exception
   {
      return (EdiRecords.query(table, new QQueryFilter()));
   }

   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   String raw() 
   {
      return (Synthetic850.generate(17, "004010", X12Separators.DEFAULT));
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void gatewayToOrderAndAck(String version) throws Exception
   {
      setup(version);
      String text = Synthetic850.generate(17, version, X12Separators.DEFAULT);
      var file = new InboundFile(1, "test", "synthetic", "test-user", "127.0.0.1", "/", "synthetic.edi", text.length(), "synthetic-sha", Instant.now(), "gateway", "in/test", () -> bytes(text));
      var consumer = new EdiFileGatewayConsumer(new InboundSink(EdiRuntime.current().payloads()));
      assertThat(consumer.getName()).isEqualTo("edi");
      assertThat(consumer.accept(file).getConsumerReference()).isEqualTo("edi_inbox:1");
      assertThat(consumer.accept(file).getConsumerReference()).isEqualTo("edi_inbox:1");
      assertThat(rows("sample_order")).isEmpty();
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      assertThat(rows("edi_inbox").getFirst().getValueString("status")).isEqualTo("PROCESSED");
      assertThat(rows("sample_order")).hasSize(1);
      assertThat(rows("sample_order_line")).hasSize(1);
      assertThat(rows("sample_order_line").getFirst().getValueString("sku")).isEqualTo("SYNTH-1");
      assertThat(rows("edi_transaction")).extracting(r -> r.getValueString("status")).contains("APPLIED", "QUEUED");
      assertThat(rows("edi_exception")).isEmpty();
      QRecord message = rows("edi_outbound_message").getFirst();
      assertThat(message.getValueString("status")).isEqualTo("QUEUED");
      try(InputStream stream = EdiRuntime.current().payloads().open(new PayloadRef(message.getValueString("payloadRef"), null, 0)))
      {
         var ack = EdiRuntime.current().reader().read(stream).getFirst();
         assertThat(ack.findings()).isEmpty();
         assertThat(ack.isa().value(6).trim()).isEqualTo("QQQTEST");
         assertThat(ack.isa().value(8).trim()).isEqualTo("SYNTHETIC");
         assertThat(ack.groups().getFirst().gs().value(1)).isEqualTo("FA");
         assertThat(ack.groups().getFirst().transactions().getFirst().st().value(1)).isEqualTo("997");
      }
      assertThat(rows("edi_event")).extracting(r -> r.getValueString("type")).contains("RECEIVED", "STORED", "PARSED", "VALIDATED", "MAPPED", "APPLIED", "ACK_QUEUED");
      assertThat(new InboxWorker().runOnce()).isZero();
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void invalidAndUnknownAndMismatch() throws Exception
   {
      setup("004010");
      receive(raw().replace("*NE*", "*INVALID*"), "bad");
      new InboxWorker().runOnce();
      assertThat(rows("sample_order")).isEmpty();
      assertThat(rows("edi_transaction")).extracting(r -> r.getValueString("status")).contains("INVALID");
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("VALIDATION");
      QRecord message = rows("edi_outbound_message").getFirst();
      try(InputStream stream = EdiRuntime.current().payloads().open(new PayloadRef(message.getValueString("payloadRef"), null, 0)))
      {
         assertThat(new String(stream.readAllBytes(), StandardCharsets.US_ASCII)).contains("AK3", "AK4", "AK5*R");
      }
      receive(raw().replace("SYNTHETIC", "UNMATCHED"), "unknown");
      new InboxWorker().runOnce();
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("UNKNOWN_PARTNER");
      SystemWrites.call(() -> EdiRecords.insert("edi_partner", new QRecord().withValue("name", "Other partner").withValue("code", "OTHER").withValue("gatewayUsername", "other")));
      new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("GATEWAY", "mismatch", "test.edi", "127.0.0.1", bytes(raw()), "other", "other"));
      new InboxWorker().runOnce();
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("PARTNER_MISMATCH");
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void duplicatesAndPermissionDenial() throws Exception
   {
      setup("004010");
      EdiRuntime.current().config().withServiceSessionSupplier(QSession::new);
      receive(raw(), "first");
      new InboxWorker().runOnce();
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("APPLY");
      assertThat(rows("sample_order")).isEmpty();
      assertThat(receive(raw(), "byte-duplicate").getValueString("status")).isEqualTo("DUPLICATE");
      receive(raw().replace("SYNTH-1", "SYNTH-2"), "control-duplicate");
      new InboxWorker().runOnce();
      assertThat(rows("edi_interchange")).extracting(r -> r.getValueString("status")).contains("DUPLICATE");
      assertThat(rows("edi_outbound_message")).hasSize(2);
      QRecord message = rows("edi_outbound_message").getLast();
      try(InputStream stream = EdiRuntime.current().payloads().open(new PayloadRef(message.getValueString("payloadRef"), null, 0)))
      {
         assertThat(new String(stream.readAllBytes(), StandardCharsets.US_ASCII)).contains("*R*025~");
      }
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void retryScheduleDeadLetterAndStaleClaim() throws Exception
   {
      setup("004010");
      QRecord row = receive(raw(), "retry");
      Instant at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).plusSeconds(1);
      PayloadStore failing = new PayloadStore()
      {
         /*******************************************************************************
          ** Test fixture operation.
          *******************************************************************************/
         public PayloadRef put(PayloadKind kind, InputStream stream) throws Exception
         {
            throw new Exception("SENSITIVE-CANARY");
         }
         /*******************************************************************************
          ** Test fixture operation.
          *******************************************************************************/
         public InputStream open(PayloadRef ref) throws Exception
         {
            throw new Exception("SENSITIVE-CANARY");
         }
         public void delete(PayloadRef ref) { }
      };
      for(long delay : List.of(60L, 300L, 900L, 3600L, 21600L))
      {
         EdiRuntime.current().config().withClock(Clock.fixed(at, ZoneOffset.UTC));
         new InboxWorker(failing).runOnce();
         QRecord stored = rows("edi_inbox").getFirst();
         assertThat(stored.getValueString("status")).isEqualTo("RETRY_WAIT");
         assertThat(stored.getValueInstant("nextAttemptAt")).isEqualTo(at.plusSeconds(delay));
         assertThat(stored.getValueString("lastError")).doesNotContain("SENSITIVE");
         at = at.plusSeconds(delay);
      }
      EdiRuntime.current().config().withClock(Clock.fixed(at, ZoneOffset.UTC));
      new InboxWorker(failing).runOnce();
      assertThat(rows("edi_inbox").getFirst().getValueString("status")).isEqualTo("DEAD_LETTER");
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("DEAD_LETTER");
      Instant staleAt = at.minusSeconds(301);
      SystemWrites.call(() -> 
      {
         SystemWrites.update("edi_inbox", new QRecord().withValue("id", row.getValue("id")).withValue("status", "PROCESSING").withValue("claimedAt", staleAt));
         return (null);
      });
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      assertThat(rows("sample_order")).hasSize(1);
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void previewAndTypedHome() throws Exception
   {
      setup("004010");
      PayloadRef ref = SystemWrites.call(() -> EdiRuntime.current().payloads().put(PayloadKind.SAMPLE, bytes(raw())));
      QRecord sample = new QRecord().withValue("payloadRef", ref.value());
      var lines = new MapPreview().preview(map, sample);
      assertThat(lines.toString()).contains("poNumber", "SNIP", "AK9");
      assertThat(rows("sample_order")).isEmpty();
      receive(raw(), "home");
      for(var widget : EdiHome.widgets())
      {
         assertThat(widget.getType()).isNotEqualTo("html");
         assertThat(new EdiHome().render(new RenderWidgetInput().withWidgetMetaData(widget))).isNotNull();
      }
      assertThat(QContext.getQInstance().getWidgets().values()).noneMatch(w -> "html".equals(w.getType()));
   }

   /*******************************************************************************
    ** Synthetic alert capture.
    *******************************************************************************/
   public static class CapturedAlerts implements com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface
   {
      static final List<com.kingsrook.qqq.backend.core.alerts.NotificationSignal> notifications = new ArrayList<>();
      /*******************************************************************************
       ** Capture actual core notification dispatch.
       *******************************************************************************/
      @Override
      public void notify(com.kingsrook.qqq.backend.core.alerts.NotificationSignal signal)
      {
         notifications.add(signal);
      }
      static final List<String> raised = new ArrayList<>();
      static final List<String> resolved = new ArrayList<>();
      /*******************************************************************************
       ** Test fixture operation.
       *******************************************************************************/
      public void raise(com.kingsrook.qqq.backend.core.alerts.AlertSignal signal)
      {
         raised.add(signal.getAlertTypeName());
      }
      /*******************************************************************************
       ** Test fixture operation.
       *******************************************************************************/
      public void resolve(com.kingsrook.qqq.backend.core.alerts.AlertResolution resolution)
      {
         resolved.add(resolution.getAlertTypeName());
      }
      public void heartbeat(String key, Instant at) { }
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"A", "R"})
   void reconcileIncoming997(String status) throws Exception
   {
      setup("004010");
      CapturedAlerts.raised.clear();
      CapturedAlerts.resolved.clear();
      com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(CapturedAlerts.class));
      QRecord outgoing = SystemWrites.call(() -> EdiRecords.insert("edi_interchange", new QRecord().withValue("direction", "OUTBOUND").withValue("agreementId", agreement.getValue("id"))
         .withValue("partnerId", partner.getValue("id")).withValue("controlNumber", "000000077")));
      QRecord group = SystemWrites.call(() -> EdiRecords.insert("edi_functional_group", new QRecord().withValue("interchangeId", outgoing.getValue("id")).withValue("controlNumber", "77").withValue("ackStatus", "PENDING")));
      String text = raw();
      text = text.substring(0, text.indexOf("~ST*") + 1) + "ST*997*0001~AK1*PO*77~AK9*" + status + "*1*1*" + (status.equals("A") ? "1" : "0") + "~SE*4*0001~GE*1*1~IEA*1*000000001~";
      receive(text.replace("GS*PO", "GS*FA"), "ack");
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      assertThat(EdiRecords.query("edi_functional_group", EdiRecords.equal("id", group.getValue("id"))).getFirst().getValueString("ackStatus")).isEqualTo(status);
      if(status.equals("R"))
      {
         assertThat(CapturedAlerts.raised).contains("edi.ack.rejected");
         assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("REJECTED_ACK");
      }
      assertThat(CapturedAlerts.resolved).contains("edi.ack.overdue");
      assertThat(rows("edi_outbound_message")).isEmpty();
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void actionsReprocessAndUpsert() throws Exception
   {
      setup("004010");
      receive(raw(), "actions");
      new InboxWorker().runOnce();
      var tx = EdiRecords.query("edi_transaction", EdiRecords.equal("direction", "INBOUND")).getFirst();
      var execute = new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute();
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_transaction_reprocess").withRecords(List.of(tx));
      var output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      execute.runOnePage(input, output);
      assertThat(execute.getProcessSummary(output, true).toString()).contains("record processed");
      new InboxWorker().runOnce();
      assertThat(rows("sample_order")).hasSize(1);
      assertThat(rows("sample_order_line")).hasSize(1);
      assertThat(rows("edi_outbound_message")).hasSize(1);
      SystemWrites.call(() -> 
      {
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(1, 1, 1, partner.getValueInteger("id"), "APPLY", "Synthetic failure");
         return (null);
      });
      QRecord exception = rows("edi_exception").getFirst();
      CapturedAlerts.notifications.clear();
      com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance())
         .withSink(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(CapturedAlerts.class));
      for(String action : List.of("assign", "ignore", "resolve"))
      {
         input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_exception_" + action).withRecords(List.of(exception))
            .withValues(Map.of("assignedToUserId", "synthetic-user"));
         new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute().runOnePage(input, new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput());
      }
      assertThat(rows("edi_exception").getFirst().getValueString("status")).isEqualTo("RESOLVED");
      assertThat(rows("edi_exception").getFirst().getValueString("assignedToUserId")).isEqualTo("synthetic-user");
      assertThat(CapturedAlerts.notifications).singleElement().satisfies(signal ->
      {
         assertThat(signal.getAudience().getValues()).containsExactly("synthetic-user");
         assertThat(signal.getTitle()).isEqualTo("EDI exception assigned");
         assertThat(signal.getBody()).isEqualTo("An EDI exception has been assigned to you.");
         assertThat(signal.getSourceTableName()).isEqualTo("edi_exception");
         assertThat(signal.getSourceRecordId()).isEqualTo(exception.getValueString("id"));
      });
      input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_exception_assign")
         .withRecords(List.of(exception)).withValues(Map.of("assignedToUserId", " "));
      output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      execute.runOnePage(input, output);
      assertThat(execute.getProcessSummary(output, true).toString()).contains("record could not be processed");
      assertThat(CapturedAlerts.notifications).hasSize(1);
      assertThat(rows("edi_exception").getFirst().getValueString("status")).isEqualTo("RESOLVED");
   }

   /*******************************************************************************
    ** Newly opened partner failures notify current assignees once, after commit only.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"REJECTED_ACK", "DEAD_LETTER"})
   void notifyPartnerAssignees(String type) throws Exception
   {
      setup("004010");
      CapturedAlerts.notifications.clear();
      com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance())
         .withSink(new com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference(CapturedAlerts.class));
      Integer partnerId = partner.getValueInteger("id");
      SystemWrites.call(() ->
      {
         for(String status : List.of("OPEN", "IN_PROGRESS", "RESOLVED", "IGNORED"))
         {
            SystemWrites.insert("edi_exception", new QRecord().withValue("partnerId", partnerId).withValue("type", "APPLY")
               .withValue("status", status).withValue("assignedToUserId", List.of("OPEN", "IN_PROGRESS").contains(status) ? "current-user" : "closed-user"));
         }
         SystemWrites.insert("edi_exception", new QRecord().withValue("partnerId", partnerId + 1).withValue("type", "APPLY")
            .withValue("status", "OPEN").withValue("assignedToUserId", "other-partner-user"));
         return (null);
      });
      SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(100, null, null, partnerId, type, "Synthetic private text");
         assertThat(CapturedAlerts.notifications).isEmpty();
         return (null);
      }));
      assertThat(CapturedAlerts.notifications).singleElement().satisfies(signal ->
      {
         assertThat(signal.getAudience().getValues()).containsExactly("current-user");
         assertThat(signal.getTitle()).isEqualTo("EDI exception opened");
         assertThat(signal.getBody()).isEqualTo("An acknowledgement rejection or dead-letter exception needs attention.");
         assertThat(signal.getSourceTableName()).isEqualTo("edi_exception");
         assertThat(signal.getSourceRecordId()).isNotBlank();
      });
      CapturedAlerts.notifications.clear();
      SystemWrites.call(() ->
      {
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(100, null, null, partnerId, type, "Repeated failure");
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(101, null, null, null, type, "No partner");
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(102, null, null, partnerId, "VALIDATION", "Other type");
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(103, null, null, partnerId + 2, type, "No assignees");
         return (null);
      });
      assertThat(CapturedAlerts.notifications).isEmpty();
      assertThatThrownBy(() -> SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(104, null, null, partnerId, type, "Rolled back");
         throw new IllegalStateException("Synthetic rollback");
      }))).isInstanceOf(Exception.class);
      assertThat(CapturedAlerts.notifications).isEmpty();
      assertThat(EdiRecords.query("edi_exception", EdiRecords.equal("inboxId", 104))).isEmpty();
      SystemWrites.call(() ->
      {
         QRecord row = EdiRecords.query("edi_exception", EdiRecords.equal("inboxId", 100)).getFirst();
         SystemWrites.update("edi_exception", new QRecord().withValue("id", row.getValue("id")).withValue("status", "RESOLVED"));
         com.kingsrook.qbits.edi.flow.EdiJournal.exception(100, null, null, partnerId, type, "Reopened");
         return (null);
      });
      assertThat(CapturedAlerts.notifications).hasSize(1);
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void previewActionDownloadAndRevisionActions() throws Exception
   {
      setup("004010");
      var ref = SystemWrites.call(() -> EdiRuntime.current().payloads().put(PayloadKind.SAMPLE, bytes(raw())));
      QRecord sample = SystemWrites.call(() -> EdiRecords.insert("edi_map_sample", new QRecord().withValue("mapId", map.getValue("id")).withValue("name", "Synthetic preview").withValue("payloadRef", ref.value())));
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_map_sample_preview").withRecords(List.of(sample));
      var output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      var action = new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute();
      action.runOnePage(input, output);
      assertThat(action.getProcessSummary(output, true).toString()).contains("poNumber");
      var storage = new com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput(output.getValueString("storageTableName")).withReference(output.getValueString("storageReference"));
      try(InputStream download = new com.kingsrook.qqq.backend.core.actions.tables.StorageAction().getInputStream(storage))
      {
         assertThat(new String(download.readAllBytes(), StandardCharsets.UTF_8)).contains("poNumber");
      }
      assertThat(rows("sample_order")).isEmpty();
      var previous = QContext.getQSession();
      QContext.setQSession(new QSession());
      org.assertj.core.api.Assertions.assertThatThrownBy(() -> new com.kingsrook.qbits.edi.storage.PreviewDownloadBackend().getInputStream(storage)).hasMessageContaining("not authorized");
      QContext.setQSession(previous);
      var reviewer = new com.kingsrook.qbits.edi.processes.EdiProcesses.Review();
      reviewer.runOnePage(input, new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput());
      assertThat(reviewer.getProcessSummary(output, false).toString()).contains("will be processed");
      var guide = EdiRecords.query("edi_guide", EdiRecords.equal("id", map.getValue("guideId"))).getFirst();
      SystemWrites.call(() -> SystemWrites.transaction(() -> 
      {
         new com.kingsrook.qbits.edi.processes.RevisionActions().execute("edi_guide", guide, "override", partner.getValueInteger("id"));
         return (null);
      }));
      var override = EdiRecords.query("edi_guide", EdiRecords.equal("source", "CUSTOM")).getFirst();
      assertThat(new com.kingsrook.qbits.edi.guide.GuideRepository().load(override.getValueInteger("id")).segments()).hasSize(new com.kingsrook.qbits.edi.guide.GuideRepository().load(guide.getValueInteger("id")).segments().size());
      SystemWrites.call(() -> SystemWrites.transaction(() -> 
      {
         new com.kingsrook.qbits.edi.processes.RevisionActions().execute("edi_guide", override, "activate", null);
         return (null);
      }));
      assertThat(EdiRecords.query("edi_guide", EdiRecords.equal("id", override.getValue("id"))).getFirst().getValueString("status")).isEqualTo("ACTIVE");
      var draft = new QRecord();
      draft.setValues(new java.util.HashMap<>(map.getValues()));
      draft.getValues().remove("id");
      draft.withValue("status", "DRAFT").withValue("revision", 0);
      var next = SystemWrites.call(() -> EdiRecords.insert("edi_map", draft));
      for(QRecord rule : EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id"))))
      {
         QRecord copy = new QRecord();
         copy.setValues(new java.util.HashMap<>(rule.getValues()));
         copy.getValues().remove("id");
         copy.withValue("mapId", next.getValue("id"));
         SystemWrites.call(() -> EdiRecords.insert("edi_map_rule", copy));
      }
      SystemWrites.call(() -> SystemWrites.transaction(() -> 
      {
         new com.kingsrook.qbits.edi.processes.RevisionActions().execute("edi_map", next, "activate", null);
         return (null);
      }));
      assertThat(EdiRecords.query("edi_map", EdiRecords.equal("id", next.getValue("id"))).getFirst().getValueInteger("revision")).isEqualTo(2);
      assertThat(EdiRecords.query("edi_map", EdiRecords.equal("id", map.getValue("id"))).getFirst().getValueString("status")).isEqualTo("RETIRED");
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void phiPreviewAndHomePermissions() throws Exception
   {
      setup("004010");
      SystemWrites.call(() -> 
      {
         SystemWrites.update("edi_agreement_document", new QRecord().withValue("id", 1).withValue("containsPhi", true));
         return (null);
      });
      var ref = SystemWrites.call(() -> EdiRuntime.current().payloads().put(PayloadKind.SAMPLE, bytes(raw().replace("TEST-", "CANARY-PHI-"))));
      var lines = new MapPreview().preview(map, new QRecord().withValue("payloadRef", ref.value()));
      assertThat(lines.toString()).doesNotContain("CANARY", "poNumber", "SYNTH-1").contains("suppressed");
      receive(raw(), "counts");
      var backlog = QContext.getQInstance().getWidget("edi_home_system");
      var data = (com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData) new EdiHome().render(new RenderWidgetInput().withWidgetMetaData(backlog)).getWidgetData();
      assertThat(data.getRows()).anySatisfy(row -> assertThat(row).containsEntry("Item", "Inbound backlog").hasEntrySatisfying("Status", status -> assertThat(String.valueOf(status)).startsWith("1")));
      assertThat(data.getLinkURL()).startsWith("/app/edi_outbound_message?filter=");
      QContext.setQSession(new QSession());
      org.assertj.core.api.Assertions.assertThatThrownBy(() -> new EdiHome().render(new RenderWidgetInput().withWidgetMetaData(backlog))).isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QException.class);
   }
   /*******************************************************************************
    ** Each repeated interchange retains a separate checkpoint and duplicate result.
    *******************************************************************************/
   @Test
   void repeatedInterchangesAndMissingService() throws Exception
   {
      setup("004010");
      EdiRuntime.current().config().withServiceSessionSupplier(null);
      receive(raw() + raw(), "two-interchanges");
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      assertThat(EdiRecords.query("edi_interchange", EdiRecords.equal("direction", "INBOUND"))).hasSize(2);
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("summary")).contains("no service session configured");
      assertThat(rows("sample_order")).isEmpty();
      assertThat(rows("edi_outbound_message")).hasSize(2);
   }

   /*******************************************************************************
    ** Replay repairs unknown routing once setup is corrected, without another intake.
    *******************************************************************************/
   @Test
   void reprocessUnknownPartnerAndCurrentMapRevision() throws Exception
   {
      setup("004010");
      receive(raw().replace("SYNTHETIC", "UNMATCHED"), "initially-unknown");
      new InboxWorker().runOnce();
      assertThat(rows("sample_order")).isEmpty();
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_identity", new QRecord().withValue("id", agreement.getValue("partnerIdentityId")).withValue("interchangeId", "UNMATCHED"));
         return (null);
      });
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_inbox_reprocess").withRecords(rows("edi_inbox"));
      new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute().runOnePage(input, new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput());
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      assertThat(rows("sample_order")).hasSize(1);
      QRecord original = EdiRecords.query("edi_transaction", EdiRecords.equal("direction", "INBOUND")).getFirst();
      QRecord next = new QRecord();
      next.setValues(new java.util.HashMap<>(map.getValues()));
      next.getValues().remove("id");
      next.withValue("revision", 2).withValue("status", "ACTIVE");
      var active = SystemWrites.call(() -> EdiRecords.insert("edi_map", next));
      for(QRecord rule : EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id"))))
      {
         QRecord copy = new QRecord();
         copy.setValues(new java.util.HashMap<>(rule.getValues()));
         copy.getValues().remove("id");
         copy.withValue("mapId", active.getValue("id"));
         SystemWrites.call(() -> EdiRecords.insert("edi_map_rule", copy));
      }
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_map", new QRecord().withValue("id", map.getValue("id")).withValue("status", "RETIRED"));
         return (null);
      });
      input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_transaction_reprocess").withRecords(List.of(original));
      new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute().runOnePage(input, new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput());
      new InboxWorker().runOnce();
      assertThat(EdiRecords.query("edi_transaction", EdiRecords.equal("id", original.getValue("id"))).getFirst().getValueInteger("mapRevision")).isEqualTo(1);
      input.withValues(Map.of("useCurrentRevisions", true));
      new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute().runOnePage(input, new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput());
      new InboxWorker().runOnce();
      assertThat(EdiRecords.query("edi_transaction", EdiRecords.equal("id", original.getValue("id"))).getFirst().getValueInteger("mapRevision")).isEqualTo(2);
      assertThat(rows("sample_order")).hasSize(1);
   }

   /*******************************************************************************
    ** Failed gateway persistence requests a retry without parsing or leaking details.
    *******************************************************************************/
   @Test
   void gatewayStorageFailureAndPhi999() throws Exception
   {
      setup("005010");
      var failing = new PayloadStore()
      {
         /*******************************************************************************
          ** Synthetic storage failure.
          *******************************************************************************/
         public PayloadRef put(PayloadKind kind, InputStream stream) throws Exception
         {
            throw new Exception("SENSITIVE-CANARY");
         }

         /*******************************************************************************
          ** Unused read operation.
          *******************************************************************************/
         public InputStream open(PayloadRef ref)
         {
            return (null);
         }

         /*******************************************************************************
          ** Unused delete operation.
          *******************************************************************************/
         public void delete(PayloadRef ref)
         {
         }
      };
      var file = new InboundFile(99, "test", "synthetic", "test", "127.0.0.1", "/", "test.edi", raw().length(), "hash", Instant.now(), "test", "test", () -> bytes(raw()));
      var acceptance = new EdiFileGatewayConsumer(new InboundSink(failing)).accept(file);
      assertThat(acceptance.getRetryAfter()).isEqualTo(java.time.Duration.ofMinutes(1));
      assertThat(acceptance.getReason()).doesNotContain("SENSITIVE");
      assertThat(rows("edi_inbox")).isEmpty();
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_agreement_document", new QRecord().withValue("id", 1).withValue("version", "005010X222A1").withValue("containsPhi", true).withValue("ackExpectation", "999"));
         return (null);
      });
      receive(Synthetic850.generate(23, "005010", X12Separators.DEFAULT).replace("*005010~", "*005010X222A1~").replace("*NE*", "*INVALID*"), "phi999");
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      QRecord message = rows("edi_outbound_message").getFirst();
      try(InputStream stream = EdiRuntime.current().payloads().open(new PayloadRef(message.getValueString("payloadRef"), null, 0)))
      {
         String acknowledgement = new String(stream.readAllBytes(), StandardCharsets.US_ASCII);
         assertThat(acknowledgement).contains("ST*999", "IK3", "IK4", "IK5*R").doesNotContain("INVALID");
      }
      assertThat(rows("sample_order")).isEmpty();
      assertThat(rows("edi_transaction_error")).allMatch(r -> r.getValue("valueExcerpt") == null && !r.getValueString("message").contains("INVALID"));
   }
   /*******************************************************************************
    ** Upload, sample persistence and preview use encrypted storage streams only.
    *******************************************************************************/
   @Test
   void uploadedSampleAndAssociationPermissionRollback() throws Exception
   {
      setup("004010");
      var storage = new com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput(com.kingsrook.qbits.edi.storage.PreviewDownloadBackend.TABLE).withReference("synthetic-upload");
      try(var stream = new com.kingsrook.qqq.backend.core.actions.tables.StorageAction().createOutputStream(storage))
      {
         stream.write(raw().getBytes(StandardCharsets.US_ASCII));
      }
      assertThat(storage.getReference()).isNotEqualTo("synthetic-upload");
      var input = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput().withProcessName("edi_map_preview").withRecords(List.of(map))
         .withValues(Map.of("sampleUpload", new ArrayList<>(List.of(storage))));
      var action = new com.kingsrook.qbits.edi.processes.EdiProcesses.Execute();
      var output = new com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput();
      action.runOnePage(input, output);
      assertThat(action.getProcessSummary(output, true).toString()).contains("poNumber");
      assertThat(rows("edi_map_sample")).hasSize(1);
      assertThat(rows("edi_map_sample").getFirst().getValueString("lastPreviewStatus")).isEqualTo("SUCCESS");
      assertThat(rows("sample_order")).isEmpty();
      EdiRuntime.current().config().withServiceSessionSupplier(() -> new QSession().withPermissions("sample.order.read", "sample.order.insert"));
      receive(raw(), "denied-child");
      new InboxWorker().runOnce();
      assertThat(rows("sample_order")).isEmpty();
      assertThat(rows("sample_order_line")).isEmpty();
      assertThat(rows("edi_exception")).extracting(r -> r.getValueString("type")).contains("APPLY");
   }
}
