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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.edifact.ContrlBuilder;
import com.kingsrook.qbits.edi.edifact.EdifactEnvelopeReader;
import com.kingsrook.qbits.edi.edifact.EdifactInterchange;
import com.kingsrook.qbits.edi.edifact.EdifactMapper;
import com.kingsrook.qbits.edi.edifact.EdifactOutbound;
import com.kingsrook.qbits.edi.edifact.EdifactSeparators;
import com.kingsrook.qbits.edi.edifact.EdifactWriter;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.flow.OutboundEnqueue;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.guide.RuleEvaluator;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.map.InboundMapper;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Native envelope, guide, map and durable pipeline acceptance with synthetic data.
 *******************************************************************************/
class EdifactTest
{
   /*******************************************************************************
    ** Restore test context.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Directory baselines validate and mapping retains segment groups and components.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"D96A", "D01B"})
   void orders(String version) throws Exception
   {
      H2Support.create();
      var input = EdifactSupport.read(EdifactSupport.orders("100", version));
      assertThat(input.findings()).isEmpty();
      var message = input.allMessages().getFirst();
      assertThat(message.findings()).isEmpty();
      var guide = new BuiltInGuideLoader().resource("ORDERS-" + version);
      assertThat(new GuideValidator().validate(message, guide, false)).isEmpty();
      var sample = RetailSupport.sample("ORDERS", version);
      List<QRecord> rules = new ArrayList<>();
      sample.get("rules").forEach(r -> rules.add(RetailSupport.record(r)));
      var mapped = new InboundMapper(EdiRuntime.current().transforms()).map(message, guide, RetailSupport.record(sample.get("map")), rules);
      assertThat(mapped.record().getValueString("buyerId")).isEqualTo("BUY+ER");
      assertThat(mapped.record().getAssociatedRecords().get("lines")).extracting(r -> r.getValueString("sku")).containsExactly("SKU:ONE", "SKU'");
      assertThat(new RuleEvaluator(message.segments(), new GuideValidator().loops(message, guide)).values("SG25/LIN/LIN03-1")).containsExactly("SKU:ONE", "SKU'");
      assertThat(EdifactSupport.read(new EdifactWriter().write(input))).isEqualTo(input);
   }

   /*******************************************************************************
    ** Each interchange can change UNA; released terminators never split the payload.
    *******************************************************************************/
   @Test
   void multiInterchangeAndSyntaxFour() throws Exception
   {
      var first = EdifactSupport.read(EdifactSupport.orders("100", "D96A"));
      var custom = new EdifactInterchange(new EdifactSeparators('>', '|', '.', '!', ' ', '~'), first.unb(), first.unz(), first.groups(), first.messages(), List.of());
      String text = new EdifactWriter().write(first) + "\r\n" + new EdifactWriter().write(custom, "\r\n");
      var parsed = new EdifactEnvelopeReader(1000).read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
      assertThat(parsed).containsExactly(first, custom);
      var four = EdifactSupport.read(EdifactSupport.orders("101", "D01B").replace("UNA:+.? '", "UNA:+.?*'").replace("UNOC:3", "UNOC:4").replace("261014:1200", "20261014:1200"));
      assertThat(four.findings()).isEmpty();
      assertThat(four.allMessages().getFirst().findings()).isEmpty();
      assertThat(EdifactSupport.read(new EdifactWriter().write(four))).isEqualTo(four);
      assertThat(EdifactSupport.read(EdifactSupport.orders("100", "D96A").substring(9)).findings()).isEmpty();
   }

   /*******************************************************************************
    ** Native group counts, references, message limits and incomplete envelopes fail safely.
    *******************************************************************************/
   @Test
   void findingsAndGroups() throws Exception
   {
      String grouped = EdifactSupport.orders("100", "D96A").replace("UNH+", "UNG+ORDERS+LINDQVIST+QQQTEST+261014:1200+G1+UN+D:96A'UNH+").replace("UNZ+", "UNE+1+G1'UNZ+");
      var valid = EdifactSupport.read(grouped);
      assertThat(valid.findings()).isEmpty();
      assertThat(valid.groups()).hasSize(1);
      assertThat(valid.groups().getFirst().findings()).isEmpty();
      assertThat(EdifactSupport.read(new EdifactWriter().write(valid))).isEqualTo(valid);
      var groupAck = new ContrlBuilder().build(valid, Map.of(), AckLevel.ALL_TRANSACTIONS, "A1", "D96A");
      assertThat(new EdifactWriter().write(groupAck.segments(), EdifactSeparators.DEFAULT, "")).contains("UCF+G1+LINDQVIST+QQQTEST+7'");
      assertThat(new GuideValidator().validate(groupAck, new BuiltInGuideLoader().resource("CONTRL-D96A"), false)).isEmpty();
      var invalid = EdifactSupport.read(grouped.replace("UNT+14+M1", "UNT+13+M2").replace("UNE+1+G1", "UNE+2+G2").replace("UNZ+1+100", "UNZ+2+101"));
      assertThat(invalid.findings()).extracting(EnvelopeFinding::code).contains("29", "28");
      assertThat(invalid.groups().getFirst().findings()).extracting(EnvelopeFinding::code).containsExactly("29", "28");
      assertThat(invalid.allMessages().getFirst().findings()).extracting(EnvelopeFinding::code).containsExactly("29", "28");
      assertThat(EdifactSupport.read(grouped.replace("UNZ+1+100'", "")).findings()).isNotEmpty();
      var bounded = new EdifactEnvelopeReader(3).read(new ByteArrayInputStream(grouped.getBytes(StandardCharsets.UTF_8))).getFirst();
      assertThat(bounded.allMessages().getFirst().segments()).hasSize(3);
      assertThat(bounded.allMessages().getFirst().findings()).extracting(EnvelopeFinding::code).contains("35");
      assertThatThrownBy(() -> new EdifactEnvelopeReader(1)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> EdifactSeparators.fromUna("bad")).isInstanceOf(IllegalArgumentException.class);
   }

   /*******************************************************************************
    ** Exact CONTRL output for acceptance, rejected data and envelope syntax errors.
    *******************************************************************************/
   @Test
   void acknowledgements() throws Exception
   {
      var input = EdifactSupport.read(EdifactSupport.orders("100", "D96A"));
      var builder = new ContrlBuilder();
      var accepted = builder.build(input, Map.of(), AckLevel.ALL_TRANSACTIONS, "A1", "D96A");
      assertThat(new EdifactWriter().write(accepted.segments(), EdifactSeparators.DEFAULT, "")).isEqualTo("UNH+A1+CONTRL:D:96A:UN'UCI+100+LINDQVIST:14+QQQTEST:14+7'UCM+M1+ORDERS:D:96A:UN:EAN008+7'UNT+4+A1'");
      var error = new ValidationFinding(7, "QTY", "SG25", 1, 2, 2, "8", "6", "8", "6", "ERROR", "Invalid number", null);
      var rejected = builder.build(input, Map.of("M1", List.of(error)), AckLevel.ERRORS_ONLY, "A2", "D96A");
      assertThat(new EdifactWriter().write(rejected.segments(), EdifactSeparators.DEFAULT, "")).isEqualTo("UNH+A2+CONTRL:D:96A:UN'UCI+100+LINDQVIST:14+QQQTEST:14+7'UCM+M1+ORDERS:D:96A:UN:EAN008+4'UCS+7'UCD+12+1:2'UNT+6+A2'");
      var broken = EdifactSupport.read(EdifactSupport.orders("100", "D96A").replace("UNZ+1+100", "UNZ+2+100"));
      assertThat(new EdifactWriter().write(builder.build(broken, Map.of(), AckLevel.GROUP_ONLY, "A3", "D96A").segments(), EdifactSeparators.DEFAULT, ""))
         .isEqualTo("UNH+A3+CONTRL:D:96A:UN'UCI+100+LINDQVIST:14+QQQTEST:14+4+29'UNT+3+A3'");
      for(var ack : List.of(accepted, rejected))
      {
         assertThat(new GuideValidator().validate(ack, new BuiltInGuideLoader().resource("CONTRL-D96A"), false)).isEmpty();
      }
   }

   /*******************************************************************************
    ** All outbound retail documents serialize, validate and parse with two line occurrences.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"D96A", "D01B"})
   void outboundRoundTrips(String version) throws Exception
   {
      H2Support.create();
      for(String doc : List.of("ORDRSP", "DESADV", "INVOIC"))
      {
         var sample = RetailSupport.sample(doc, version);
         List<QRecord> rules = new ArrayList<>();
         sample.get("rules").forEach(r -> rules.add(RetailSupport.record(r)));
         var message = new EdifactMapper().message(RetailSupport.source(), RetailSupport.record(sample.get("map")), rules, "1");
         assertThat(new GuideValidator().validate(message, new BuiltInGuideLoader().resource(doc + "-" + version), false)).as(doc).isEmpty();
         var envelope = EdifactOutbound.wrap(new QRecord().withValue("usageIndicator", "T"), new QRecord().withValue("interchangeId", "LOCAL").withValue("interchangeQualifier", "14"),
            new QRecord().withValue("interchangeId", "REMOTE").withValue("interchangeQualifier", "14"), List.of(message), "100", java.time.LocalDateTime.of(2026, 10, 14, 12, 0));
         var parsed = EdifactSupport.read(new EdifactWriter().write(envelope));
         assertThat(parsed.findings()).isEmpty();
         assertThat(parsed.allMessages().getFirst().findings()).isEmpty();
         assertThat(parsed.allMessages().getFirst().segments()).isEqualTo(message.segments());
      }
   }

   /*******************************************************************************
    ** ORDERS is applied through the durable inbox; repeat controls cannot reapply it.
    *******************************************************************************/
   @Test
   void inboxAndDuplicates() throws Exception
   {
      setup();
      EdifactSupport.receive(EdifactSupport.orders("100", "D96A"), "orders-1");
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      assertThat(rows("edi_inbox")).extracting(r -> r.getValueString("status")).containsExactly("PROCESSED");
      assertThat(rows("edi_transaction")).anyMatch(r -> "ORDERS".equals(r.getValueString("transactionSet")) && "APPLIED".equals(r.getValueString("status")));
      assertThat(rows("sample_order")).hasSize(1);
      assertThat(rows("sample_order_line")).hasSize(2);
      assertThat(rows("edi_outbound_message")).hasSize(1);
      EdifactSupport.receive(EdifactSupport.orders("100", "D96A").replace("TEST-100", "TEST-OTHER"), "orders-2");
      new InboxWorker().runOnce();
      assertThat(rows("sample_order")).hasSize(1);
      assertThat(rows("edi_exception")).anyMatch(r -> "DUPLICATE".equals(r.getValueString("type")));
   }

   /*******************************************************************************
    ** Worker maps persisted retail records, including nested shipment associations.
    *******************************************************************************/
   @Test
   void outboundWorker() throws Exception
   {
      setup();
      SystemWrites.call(() ->
      {
         for(String table : List.of("sample_order", "sample_shipment", "sample_invoice"))
         {
            QRecord source = SystemWrites.transaction(() -> RetailSupport.saveSource(table, RetailSupport.source()));
            OutboundEnqueue.enqueue(table, source, "MANUAL", null);
         }
         return (null);
      });
      assertThat(new OutboundWorker().runOnce()).isEqualTo(3);
      assertThat(rows("edi_outbound_message")).hasSize(3);
      for(var row : rows("edi_outbound_message"))
      {
         try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(row.getValueString("payloadRef"), null, 0)))
         {
            var parsed = new EdifactEnvelopeReader(10000).read(stream).getFirst();
            assertThat(parsed.findings()).isEmpty();
            assertThat(parsed.allMessages().getFirst().findings()).isEmpty();
         }
      }
   }

   /*******************************************************************************
    ** CONTRL resolves deadlines and reconciles accepted and rejected outbound messages.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = {false, true})
   void reconcile(boolean reject) throws Exception
   {
      QRecord agreement = setup();
      SystemWrites.call(() ->
      {
         QRecord source = SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source()));
         OutboundEnqueue.enqueue("sample_order", source, "MANUAL", null);
         return (null);
      });
      new OutboundWorker().runOnce();
      QRecord outgoing = rows("edi_outbound_message").getFirst();
      com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayOutbox.register((endpoint, name, stream, size) ->
         new com.kingsrook.qbits.edi.transport.gateway.standin.OutboxFileRef(1, endpoint, name, name, size, null, java.time.Instant.now()));
      try
      {
         new com.kingsrook.qbits.edi.flow.SendWorker().runOnce();
         assertThat(rows("edi_functional_group").getFirst().getValueInstant("ackDueAt")).isNotNull();
         EdiRuntime.current().config().withClock(java.time.Clock.fixed(java.time.Instant.now().plusSeconds(120), java.time.ZoneOffset.UTC));
         assertThat(new com.kingsrook.qbits.edi.flow.AckMonitor().runOnce()).isEqualTo(1);
         EdifactInterchange original;
         try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(outgoing.getValueString("payloadRef"), null, 0)))
         {
            original = new EdifactEnvelopeReader(10000).read(stream).getFirst();
         }
         var message = original.allMessages().getFirst();
         var error = new ValidationFinding(2, "BGM", null, 2, null, 2, "8", "1", "8", "1", "ERROR", "Missing number", null);
         var ack = new ContrlBuilder().build(original, reject ? Map.of(message.reference(), List.of(error)) : Map.of(), AckLevel.ALL_TRANSACTIONS, "A1", "D96A");
         var reply = EdifactOutbound.wrap(agreement, SystemWrites.call(() -> OutboundWorker.row("edi_identity", agreement.getValue("partnerIdentityId"))),
            SystemWrites.call(() -> OutboundWorker.row("edi_identity", agreement.getValue("localIdentityId"))), List.of(ack), "ACK100", java.time.LocalDateTime.of(2026, 10, 14, 12, 0));
         EdifactSupport.receive(new EdifactWriter().write(reply), "ack-1");
         assertThat(new InboxWorker().runOnce()).isEqualTo(1);
         assertThat(rows("edi_interchange").stream().filter(r -> r.getValue("id").equals(outgoing.getValue("interchangeId"))).findFirst().orElseThrow().getValueString("status"))
            .isEqualTo(reject ? "REJECTED" : "ACKNOWLEDGED");
         assertThat(rows("edi_transaction").stream().filter(r -> "OUTBOUND".equals(r.getValueString("direction"))).findFirst().orElseThrow().getValueString("ackStatus")).isEqualTo(reject ? "R" : "A");
         assertThat(rows("edi_exception")).anyMatch(r -> "OVERDUE_ACK".equals(r.getValueString("type")) && "RESOLVED".equals(r.getValueString("status")));
         assertThat(rows("edi_exception").stream().anyMatch(r -> "REJECTED_ACK".equals(r.getValueString("type")))).isEqualTo(reject);
         assertThat(rows("edi_outbound_message")).hasSize(1);
      }
      finally
      {
         com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayOutbox.register(null);
      }
   }

   /*******************************************************************************
    ** A single payload can contain X12 and EDIFACT and route each independently.
    *******************************************************************************/
   @Test
   void mixedStandards() throws Exception
   {
      setup();
      SystemWrites.call(() ->
      {
         QRecord partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", "X12 mixed").withValue("code", "MIXED"));
         QRecord local = SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "QQQTEST"));
         QRecord remote = SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "SYNTHETIC").withValue("partnerId", partner.getValue("id")));
         QRecord agreement = SystemWrites.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", local.getValue("id"))
            .withValue("partnerIdentityId", remote.getValue("id")).withValue("standard", "X12").withValue("usageIndicator", "T").withValue("applyTestDocuments", true));
         QRecord map = RetailSupport.installMap("850", "004010");
         SystemWrites.insert("edi_agreement_document", new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", "INBOUND").withValue("transactionSet", "850")
            .withValue("version", "004010").withValue("guideId", map.getValue("guideId")).withValue("mapId", map.getValue("id")).withValue("ackExpectation", "NONE"));
         return (null);
      });
      String x12 = Synthetic850.generate(1, "004010", com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT);
      EdifactSupport.receive(x12 + "\n" + EdifactSupport.orders("200", "D96A"), "mixed");
      assertThat(new InboxWorker().runOnce()).isEqualTo(1);
      assertThat(rows("edi_transaction").stream().filter(r -> "APPLIED".equals(r.getValueString("status"))).map(r -> r.getValueString("transactionSet"))).containsExactlyInAnyOrder("850", "ORDERS");
      assertThat(rows("sample_order")).hasSize(2);
   }

   /*******************************************************************************
    ** All service characters in data survive custom UNA and release escaping.
    *******************************************************************************/
   @Test
   void releasedCharactersAndRepetition() throws Exception
   {
      var input = EdifactSupport.read(EdifactSupport.orders("100", "D96A"));
      var message = input.allMessages().getFirst();
      var segments = new ArrayList<>(message.segments());
      segments.set(1, com.kingsrook.qbits.edi.x12.X12Segment.of("BGM", 2, "220", "A|B>C!D~E?F", "9"));
      var changed = new com.kingsrook.qbits.edi.edifact.EdifactMessage(message.header(), message.unt(), segments, List.of());
      var custom = new EdifactInterchange(new EdifactSeparators('>', '|', '.', '!', ' ', '~'), input.unb(), input.unz(), List.of(), List.of(changed), List.of());
      String encoded = new EdifactWriter().write(custom);
      assertThat(encoded).contains("A!|B!>C!!D!~E?F");
      assertThat(EdifactSupport.read(encoded).allMessages().getFirst().segments()).isEqualTo(segments);
      var standard = new EdifactInterchange(EdifactSeparators.DEFAULT, input.unb(), input.unz(), List.of(), List.of(changed), List.of());
      assertThat(new EdifactWriter().write(standard)).contains("E??F");
      String four = EdifactSupport.orders("100", "D01B").replace("UNA:+.? '", "UNA:+.?*'").replace("UNOC:3", "UNOC:4").replace("261014:1200", "20261014:1200").replace("TEST-100", "FIRST*SECOND");
      var parsed = EdifactSupport.read(four);
      assertThat(parsed.allMessages().getFirst().segments().get(1).value(2, 2, 1)).isEqualTo("SECOND");
      assertThat(EdifactSupport.read(new EdifactWriter().write(parsed))).isEqualTo(parsed);
      var withoutAdvice = EdifactSupport.read(four.substring(9));
      assertThat(withoutAdvice.separators().repetition()).isEqualTo('*');
      assertThat(EdifactSupport.read(new EdifactWriter().write(withoutAdvice))).isEqualTo(withoutAdvice);
   }

   /*******************************************************************************
    ** Resend retains bytes, while resend-as-new preserves archived content and changes controls.
    *******************************************************************************/
   @Test
   void archivedResendAndPreview() throws Exception
   {
      setup();
      SystemWrites.call(() ->
      {
         var source = SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source()));
         OutboundEnqueue.enqueue("sample_order", source, "MANUAL", null);
         return (null);
      });
      new OutboundWorker().runOnce();
      QRecord original = rows("edi_outbound_message").getFirst();
      SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         com.kingsrook.qbits.edi.flow.OutboundActions.execute("edi_outbound_message", original, "resend");
         SystemWrites.update("sample_order", new QRecord().withValue("id", rows("sample_order").getFirst().getValue("id")).withValue("poNumber", "CHANGED-AFTER-SEND"));
         com.kingsrook.qbits.edi.flow.OutboundActions.execute("edi_outbound_message", original, "new");
         return (null);
      }));
      assertThat(rows("edi_outbound_message")).hasSize(2);
      assertThat(rows("edi_outbound_message").getFirst().getValueString("payloadRef")).isEqualTo(original.getValueString("payloadRef"));
      var rebuilt = rows("edi_outbound_message").getLast();
      try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(rebuilt.getValueString("payloadRef"), null, 0)))
      {
         var parsed = new EdifactEnvelopeReader(10000).read(stream).getFirst();
         assertThat(parsed.allMessages().getFirst().segments().get(1).value(2)).isEqualTo("DEMO-1001");
         assertThat(parsed.allMessages().getFirst().reference()).isEqualTo("2");
         assertThat(parsed.unb().value(5)).isEqualTo("2");
      }
      SystemWrites.call(() ->
      {
         QRecord map = rows("edi_map").stream().filter(r -> "ORDERS".equals(r.getValueString("transactionSet"))).findFirst().orElseThrow();
         var payload = EdiRuntime.current().payloads().put(com.kingsrook.qbits.edi.storage.PayloadKind.SAMPLE, new ByteArrayInputStream(EdifactSupport.orders("300", "D96A").getBytes(StandardCharsets.UTF_8)));
         var lines = new com.kingsrook.qbits.edi.map.MapPreview().preview(map, new QRecord().withValue("payloadRef", payload.value()));
         assertThat(lines).isNotEmpty();
         assertThat(rows("sample_order")).hasSize(1);
         return (null);
      });
   }

   /*******************************************************************************
    ** UN control levels allow fourteen digits without relaxing X12 bounds.
    *******************************************************************************/
   @Test
   void controlLevels() throws Exception
   {
      QRecord agreement = setup();
      var allocator = EdiRuntime.current().controls();
      assertThat(allocator.allocate(agreement.getValueInteger("id"), null, "UNB", 99999999999998L, 99999999999999L, false)).isEqualTo(99999999999998L);
      assertThat(allocator.allocate(agreement.getValueInteger("id"), null, "UNB", 1, 99999999999999L, false)).isEqualTo(99999999999999L);
      assertThatThrownBy(() -> allocator.allocate(agreement.getValueInteger("id"), null, "UNB", 1, 99999999999999L, false)).isInstanceOf(IllegalStateException.class);
      assertThat(allocator.allocate(agreement.getValueInteger("id"), null, "UNH", 1, 99999999999999L, true)).isEqualTo(1);
      assertThatThrownBy(() -> allocator.allocate(agreement.getValueInteger("id"), null, "ISA", 1, 99999999999999L, true)).isInstanceOf(IllegalArgumentException.class);
   }

   /*******************************************************************************
    ** Decimal commas follow UNA in inbound mapping and outbound numeric serialization.
    *******************************************************************************/
   @Test
   void decimalComma() throws Exception
   {
      H2Support.create();
      var input = EdifactSupport.read(EdifactSupport.orders("400", "D96A").replace("UNA:+.? '", "UNA:+,? '").replace("AAA:7.5", "AAA:7,5"));
      var guide = new BuiltInGuideLoader().resource("ORDERS-D96A");
      var message = input.allMessages().getFirst();
      assertThat(new GuideValidator().validate(message, guide, false)).isEmpty();
      var sample = RetailSupport.sample("ORDERS", "D96A");
      List<QRecord> rules = new ArrayList<>();
      sample.get("rules").forEach(r -> rules.add(RetailSupport.record(r)));
      var result = new InboundMapper(EdiRuntime.current().transforms()).map(message, guide, RetailSupport.record(sample.get("map")), rules);
      assertThat(result.record().getAssociatedRecords().get("lines").getFirst().getValueBigDecimal("unitPrice")).isEqualByComparingTo("7.5");
      var outbound = RetailSupport.sample("INVOIC", "D96A");
      rules.clear();
      outbound.get("rules").forEach(r -> rules.add(RetailSupport.record(r)));
      var invoice = new EdifactMapper(',').message(RetailSupport.source(), RetailSupport.record(outbound.get("map")), rules, "1");
      assertThat(new EdifactWriter().write(invoice.segments(), new EdifactSeparators(':', '+', ',', '?', ' ', '\''), "")).contains("PRI+AAA:7,5'");
   }

   /*******************************************************************************
    ** Invalid data and message counts are rejected; unknown senders cannot trigger replies.
    *******************************************************************************/
   @Test
   void rejectsInvalidAndUnknownDocuments() throws Exception
   {
      setup();
      EdifactSupport.receive(EdifactSupport.orders("500", "D96A").replace("QTY+21:2:PCE", "QTY+21:BAD:PCE"), "bad-value");
      EdifactSupport.receive(EdifactSupport.orders("501", "D96A").replace("UNT+14", "UNT+13"), "bad-count");
      EdifactSupport.receive(EdifactSupport.orders("502", "D96A").replace("LINDQVIST:14", "UNKNOWN:14"), "unknown");
      assertThat(new InboxWorker().runOnce()).isEqualTo(3);
      assertThat(rows("sample_order")).isEmpty();
      assertThat(rows("edi_transaction").stream().filter(r -> "ORDERS".equals(r.getValueString("transactionSet")))).allMatch(r -> "INVALID".equals(r.getValueString("status")));
      assertThat(rows("edi_outbound_message")).hasSize(2);
      assertThat(rows("edi_exception")).anyMatch(r -> "UNKNOWN_PARTNER".equals(r.getValueString("type")));
      for(var row : rows("edi_outbound_message"))
      {
         try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(row.getValueString("payloadRef"), null, 0)))
         {
            String ack = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(ack).contains("+4").doesNotContain("BAD");
            if(ack.contains("UCI+501+"))
            {
               assertThat(ack).contains("UCM+M1+ORDERS:D:96A:UN:EAN008+4+29'").doesNotContain("UCS+");
            }
         }
      }
   }

   /*******************************************************************************
    ** Invalid CONTRL references roll back reconciliation; receipt-only responses leave it pending.
    *******************************************************************************/
   @Test
   void invalidAcknowledgements() throws Exception
   {
      QRecord agreement = setup();
      SystemWrites.call(() ->
      {
         var source = SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source()));
         OutboundEnqueue.enqueue("sample_order", source, "MANUAL", null);
         return (null);
      });
      new OutboundWorker().runOnce();
      QRecord sent = rows("edi_outbound_message").getFirst();
      String ackText = "UNA:+.? 'UNB+UNOC:3+LINDQVIST:14+QQQTEST:14+261014:1200+ACK1++++++1'UNH+A1+CONTRL:D:96A:UN'UCI+1+QQQTEST:14+LINDQVIST:14+7'UCM+BOGUS+ORDRSP:D:96A:UN+7'UNT+4+A1'UNZ+1+ACK1'";
      var invalid = EdifactSupport.read(ackText).allMessages().getFirst();
      QRecord received = new QRecord().withValue("id", rows("edi_transaction").getFirst().getValue("id"));
      assertThatThrownBy(() -> SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         new com.kingsrook.qbits.edi.edifact.ContrlReader().reconcile(invalid, agreement, received);
         return (null);
      }))).isNotNull();
      assertThat(rows("edi_functional_group").getFirst().getValueString("ackStatus")).isEqualTo("PENDING");
      assertThat(rows("edi_transaction").getFirst().getValueString("status")).isEqualTo("QUEUED");
      var receipt = EdifactSupport.read(ackText.replace("LINDQVIST:14+7", "LINDQVIST:14+8")).allMessages().getFirst();
      SystemWrites.call(() ->
      {
         new com.kingsrook.qbits.edi.edifact.ContrlReader().reconcile(receipt, agreement, received);
         return (null);
      });
      assertThat(rows("edi_interchange").getFirst().getValueString("status")).isEqualTo("ENVELOPED");
      assertThat(rows("edi_outbound_message").getFirst().getValueString("payloadRef")).isEqualTo(sent.getValueString("payloadRef"));
   }

   /*******************************************************************************
    ** Build actual QQQ tables and install the same definitions used in the demo.
    *******************************************************************************/
   private QRecord setup() throws Exception
   {
      H2Support.create();
      RetailSupport.tables();
      EdiRuntime.current().initialize();
      return (SystemWrites.call(() -> EdifactSupport.install("D96A")));
   }

   /*******************************************************************************
    ** Read test evidence with the service boundary.
    *******************************************************************************/
   private List<QRecord> rows(String table) throws Exception
   {
      return (SystemWrites.call(() -> EdiRecords.query(table, new QQueryFilter())));
   }
}
