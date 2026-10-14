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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.ack.Ack997Builder;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.flow.InboxWorker;
import com.kingsrook.qbits.edi.flow.OutboundWorker;
import com.kingsrook.qbits.edi.flow.SendWorker;
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.map.InboundMapper;
import com.kingsrook.qbits.edi.map.OutboundMapper;
import com.kingsrook.qbits.edi.map.TransformRegistry;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayOutbox;
import com.kingsrook.qbits.edi.transport.gateway.standin.OutboxFileRef;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qbits.edi.x12.X12Writer;
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
 ** Warehouse and logistics baselines (940, 945, 214, 204) in both supported releases:
 ** guides, maps, round trips, crafted errors and partner routing through the pipeline.
 *******************************************************************************/
class LogisticsTest
{
   private final Map<String, byte[]> sent = new LinkedHashMap<>();

   /*******************************************************************************
    ** Restore thread context and the gateway stand-in.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      FileGatewayOutbox.register(null);
      QContext.clear();
   }

   /*******************************************************************************
    ** Validate a synthetic body against the baseline guide of that version.
    *******************************************************************************/
   private static List<ValidationFinding> validate(String set, String version, List<X12Segment> body) throws Exception
   {
      return (new GuideValidator().validate(LogisticsSupport.transaction(set, version, body), new BuiltInGuideLoader().resource(set + "-" + version), false));
   }

   /*******************************************************************************
    ** Map a synthetic body inbound with a bundled sample map.
    *******************************************************************************/
   private static InboundMapper.Result map(String key, String set, String version, List<X12Segment> body) throws Exception
   {
      List<QRecord> rules = new ArrayList<>();
      QRecord map = LogisticsSupport.definition(key, version, rules);
      return (new InboundMapper(new TransformRegistry()).map(LogisticsSupport.transaction(set, version, body), new BuiltInGuideLoader().resource(set + "-" + version), map, rules));
   }

   /*******************************************************************************
    ** Build an outbound document from a record, prove it is clean, and prove the writer/reader round trip.
    *******************************************************************************/
   private static List<X12Segment> outbound(String key, String set, String version, QRecord source) throws Exception
   {
      List<QRecord> rules = new ArrayList<>();
      QRecord map = LogisticsSupport.definition(key, version, rules);
      X12TransactionSet built = new OutboundMapper().build(source, map, rules, "0001");
      assertThat(EdiRuntime.current().validator().validate(built, new BuiltInGuideLoader().resource(set + "-" + version), false)).isEmpty();
      List<X12Segment> body = built.segments().subList(1, built.segments().size() - 1);
      X12TransactionSet parsed = LogisticsSupport.transaction(set, version, body);
      assertThat(parsed.findings()).isEmpty();
      assertThat(parsed.segments().stream().map(s -> s.id() + s.elements()).toList()).isEqualTo(built.segments().stream().map(s -> s.id() + s.elements()).toList());
      return (body);
   }

   /*******************************************************************************
    ** Compact segment text, for exact expectations.
    *******************************************************************************/
   private static String text(List<X12Segment> body)
   {
      return (new X12Writer().write(body, X12Separators.DEFAULT, ""));
   }

   /*******************************************************************************
    ** Copy a body replacing one element of the nth occurrence of a segment.
    *******************************************************************************/
   private static List<X12Segment> with(List<X12Segment> body, String id, int nth, int element, String value)
   {
      List<X12Segment> copy = new ArrayList<>();
      int seen = 0;
      for(X12Segment segment : body)
      {
         if(segment.id().equals(id) && ++seen == nth)
         {
            List<String> values = new ArrayList<>();
            for(int i = 1; i <= Math.max(segment.elements().size(), element); i++)
            {
               values.add(i == element ? value : segment.value(i));
            }
            copy.add(X12Segment.of(id, 0, values.toArray(String[]::new)));
         }
         else
         {
            copy.add(segment);
         }
      }
      return (copy);
   }

   /*******************************************************************************
    ** A crafted error yields the expected AK3/AK4 codes and a rejecting 997 carrying them.
    *******************************************************************************/
   private static void rejects(String set, String version, List<X12Segment> body, String ak3, String ak4, String ackText) throws Exception
   {
      var interchange = LogisticsSupport.read(LogisticsSupport.interchange(set, version, "HARBORLOG", "000000001", body));
      var group = interchange.groups().getFirst();
      var findings = new GuideValidator().validate(group.transactions().getFirst(), new BuiltInGuideLoader().resource(set + "-" + version), false);
      assertThat(findings).as(set + " " + ackText).anyMatch(f -> ak3.equals(f.ak3Code()) && ak4.equals(f.ak4Code()));
      String ack = new X12Writer().write(new Ack997Builder().build(group, Map.of("0001", findings), AckLevel.ALL_TRANSACTIONS, "0002").segments(), X12Separators.DEFAULT, "");
      assertThat(ack).as(set + " " + ackText).contains("AK2*" + set + "*0001~", "AK3*", "AK5*R").contains(ackText);
   }

   /*******************************************************************************
    ** A 3PL receives a shipping order and the sample map fills the order and its lines.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void shippingOrderInbound(String version) throws Exception
   {
      List<X12Segment> body = LogisticsSupport.body940("ORD-7001", "PO-5001", 10, 5);
      assertThat(validate("940", version, body)).isEmpty();
      var result = map("940", "940", version, body);
      QRecord order = result.record();
      assertThat(result.businessKey()).isEqualTo("ORD-7001");
      assertThat(result.secondaryKey()).isEqualTo("PO-5001");
      assertThat(order.getValueString("orderNumber")).isEqualTo("ORD-7001");
      assertThat(order.getValueString("poNumber")).isEqualTo("PO-5001");
      assertThat(order.getValueString("orderStatus")).isEqualTo("NEW");
      assertThat(order.getValueString("shipToName")).isEqualTo("Harbor Test Dock");
      assertThat(order.getValueString("shipToAddress")).isEqualTo("100 Synthetic Way");
      assertThat(order.getValueString("shipToCity")).isEqualTo("Testville");
      assertThat(order.getValueString("shipToState")).isEqualTo("IL");
      assertThat(order.getValueString("shipToPostal")).isEqualTo("60000");
      assertThat(order.getValueString("carrierScac")).isEqualTo("SYNC");
      assertThat(order.getValueString("note")).isEqualTo("Synthetic note");
      assertThat(order.getValueInstant("requestedShipAt")).isEqualTo(Instant.parse("2026-10-15T19:00:00Z"));
      assertThat(order.getValueBigDecimal("totalQuantity")).isEqualByComparingTo("15");
      var lines = order.getAssociatedRecords().get("lines");
      assertThat(lines).hasSize(2);
      assertThat(lines.get(1).getValueString("lineNumber")).isEqualTo("2");
      assertThat(lines.get(1).getValueString("sku")).isEqualTo("SKU-200");
      assertThat(lines.get(1).getValueString("description")).isEqualTo("Synthetic item 2");
      assertThat(lines.get(1).getValueBigDecimal("quantity")).isEqualByComparingTo("5");
      assertThat(lines.get(1).getValueString("unit")).isEqualTo("EA");
      assertThat(map("940", "940", version, with(body, "W05", 1, 1, "D")).record().getValueString("orderStatus")).isEqualTo("CANCEL");
   }

   /*******************************************************************************
    ** Order shipped: the 940 becomes a shipment, the 945 goes out clean and maps back to the same shipment.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void orderShippedRoundTrip(String version) throws Exception
   {
      H2Support.create();
      QRecord order = map("940", "940", version, LogisticsSupport.body940("ORD-7001", "PO-5001", 10, 5)).record();
      QRecord shipment = LogisticsSupport.shipmentFor(order, true);
      List<X12Segment> body = outbound("945", "945", version, shipment);
      String x12 = text(body);
      assertThat(x12).contains("W06*F*ORD-7001*20261016*SHIP-ORD-7001**PO-5001~", "G62*11*20261016*Y*093000~",
         "W27*M*SYNC*ROUTE-A~", "W12*CC*10*10**EA**VN*SKU-100~", "W12*PR*5*4*1*EA**VN*SKU-200~", "MAN*GM*000123456789000012~", "W03*14~");
      var tx = LogisticsSupport.transaction("945", version, body);
      assertThat(new com.kingsrook.qbits.edi.guide.RuleEvaluator(tx.segments(), Map.of()).values("W0602")).containsExactly("ORD-7001");
      var back = map("945-in", "945", version, body);
      assertThat(back.businessKey()).isEqualTo("ORD-7001");
      assertThat(back.secondaryKey()).isEqualTo("SHIP-ORD-7001");
      QRecord record = back.record();
      assertThat(record.getValueString("poNumber")).isEqualTo("PO-5001");
      assertThat(record.getValueInstant("shippedAt")).isEqualTo(Instant.parse("2026-10-16T14:30:00Z"));
      assertThat(record.getValueString("carrierScac")).isEqualTo("SYNC");
      assertThat(record.getValueString("routing")).isEqualTo("ROUTE-A");
      assertThat(record.getValueString("shipToCity")).isEqualTo("Testville");
      assertThat(record.getValueBigDecimal("totalUnits")).isEqualByComparingTo("14");
      var lines = record.getAssociatedRecords().get("lines");
      assertThat(lines).extracting(l -> l.getValueString("statusCode")).containsExactly("CC", "PR");
      assertThat(lines).extracting(l -> l.getValueString("sku")).containsExactly("SKU-100", "SKU-200");
      assertThat(lines.get(0).getValueBigDecimal("shippedQuantity")).isEqualByComparingTo("10");
      assertThat(lines.get(0).getValue("shortQuantity")).isNull();
      assertThat(lines.get(1).getValueBigDecimal("orderedQuantity")).isEqualByComparingTo("5");
      assertThat(lines.get(1).getValueBigDecimal("shippedQuantity")).isEqualByComparingTo("4");
      assertThat(lines.get(1).getValueBigDecimal("shortQuantity")).isEqualByComparingTo("1");
      assertThat(lines.get(1).getValueString("sscc")).isEqualTo("000123456789000012");
   }

   /*******************************************************************************
    ** A raw synthetic 945 (a shipper receiving from its 3PL) validates and maps.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void shipmentAdviceInbound(String version) throws Exception
   {
      List<X12Segment> body = LogisticsSupport.body945("ORD-7002", "PO-5002", "SHIP-9002", new int[] {4, 6}, new int[] {4, 6});
      assertThat(validate("945", version, body)).isEmpty();
      QRecord record = map("945-in", "945", version, body).record();
      assertThat(record.getValueString("shipmentNumber")).isEqualTo("SHIP-9002");
      assertThat(record.getValueBigDecimal("totalUnits")).isEqualByComparingTo("10");
      assertThat(record.getAssociatedRecords().get("lines")).extracting(l -> l.getValueString("statusCode")).containsExactly("CC", "CC");
   }

   /*******************************************************************************
    ** Carrier status events map to one status row each, using the first LX event.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void carrierStatusInbound(String version) throws Exception
   {
      List<X12Segment> body = LogisticsSupport.body214("SHIP-9001", "PRO1234567", "AF", "NS", "Testville");
      assertThat(validate("214", version, body)).isEmpty();
      var result = map("214", "214", version, body);
      assertThat(result.businessKey()).isEqualTo("SHIP-9001");
      assertThat(result.secondaryKey()).isEqualTo("PRO1234567");
      QRecord status = result.record();
      assertThat(status.getValueString("shipmentId")).isEqualTo("SHIP-9001");
      assertThat(status.getValueString("proNumber")).isEqualTo("PRO1234567");
      assertThat(status.getValueString("carrierScac")).isEqualTo("SYNC");
      assertThat(status.getValueString("statusCode")).isEqualTo("AF");
      assertThat(status.getValueString("reasonCode")).isEqualTo("NS");
      assertThat(status.getValueString("city")).isEqualTo("Testville");
      assertThat(status.getValueString("state")).isEqualTo("IL");
      assertThat(status.getValueString("equipmentNumber")).isEqualTo("TRL12345");
      assertThat(status.getValueInstant("eventAt")).isEqualTo(Instant.parse("2026-10-16T19:15:00Z"));
      List<X12Segment> events = new ArrayList<>(body.subList(0, body.size() - 1));
      events.addAll(List.of(LogisticsSupport.s("LX", "2"), LogisticsSupport.s("AT7", "X1", "NS", "", "", "20261017", "0800", "CT"), LogisticsSupport.s("MS1", "Desttown", "TX", "US")));
      assertThat(validate("214", version, events)).isEmpty();
      assertThat(map("214", "214", version, events).record().getValueString("statusCode")).isEqualTo("AF");
   }

   /*******************************************************************************
    ** A shipper tenders a load: out, clean, and back in to the carrier's load and stops.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void loadTenderRoundTrip(String version) throws Exception
   {
      H2Support.create();
      List<X12Segment> body = outbound("204-out", "204", version, LogisticsSupport.load("SHIP-3001"));
      assertThat(text(body)).contains("B2**SYNC**SHIP-3001**PP~", "B2A*00*LT~", "L11*BOL-8800*BM~", "MS3*SYNC*B**M~", "N1*BT*Synthetic Billing~", "S5*1*LD*12500*L~", "G62*10*20261015*Y*140000~",
         "N1*SF*Synthetic Plant~", "N4*Desttown*TX*75000~", "OID*ORD-7001~", "S5*2*UL*12500*L~", "G62*02*20261016*Y*080000~", "L3*12500*G~");
      for(List<X12Segment> raw : List.of(body, LogisticsSupport.body204("SHIP-3001", "00")))
      {
         assertThat(validate("204", version, raw)).isEmpty();
         var result = map("204", "204", version, raw);
         assertThat(result.businessKey()).isEqualTo("SHIP-3001");
         QRecord load = result.record();
         assertThat(load.getValueString("carrierScac")).isEqualTo("SYNC");
         assertThat(load.getValueString("purpose")).isEqualTo("ORIGINAL");
         assertThat(load.getValueString("paymentMethod")).isEqualTo("PP");
         assertThat(load.getValueString("bolNumber")).isEqualTo("BOL-8800");
         assertThat(load.getValueString("billToName")).isEqualTo("Synthetic Billing");
         assertThat(load.getValueBigDecimal("totalWeight")).isEqualByComparingTo("12500");
         var stops = load.getAssociatedRecords().get("stops");
         assertThat(stops).extracting(s -> s.getValueString("stopSequence")).containsExactly("1", "2");
         assertThat(stops).extracting(s -> s.getValueString("stopReason")).containsExactly("LD", "UL");
         assertThat(stops).extracting(s -> s.getValueString("name")).containsExactly("Synthetic Plant", "Synthetic Receiver");
         assertThat(stops).extracting(s -> s.getValueString("address")).containsExactly("1 Origin Road", "2 Dest Road");
         assertThat(stops).extracting(s -> s.getValueString("state")).containsExactly("IL", "TX");
         assertThat(stops).extracting(s -> s.getValueInstant("appointmentAt")).containsExactly(Instant.parse("2026-10-15T19:00:00Z"), Instant.parse("2026-10-16T13:00:00Z"));
         assertThat(stops).extracting(s -> s.getValueBigDecimal("weight")).allSatisfy(w -> assertThat(w).isEqualByComparingTo(new BigDecimal("12500")));
         assertThat(stops).extracting(s -> s.getValueString("orderNumber")).containsExactly("ORD-7001", "ORD-7001");
      }
      assertThat(map("204", "204", version, LogisticsSupport.body204("SHIP-3001", "04")).record().getValueString("purpose")).isEqualTo("CHANGE");
      assertThat(map("204", "204", version, LogisticsSupport.body204("SHIP-3001", "01")).record().getValueString("purpose")).isEqualTo("CANCEL");
   }

   /*******************************************************************************
    ** Crafted 940 errors produce the intended AK3/AK4 codes in a rejecting 997.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void invalidShippingOrder(String version) throws Exception
   {
      List<X12Segment> good = LogisticsSupport.body940("ORD-7001", "PO-5001", 10, 5);
      rejects("940", version, with(good, "W01", 1, 2, "ZZ"), "8", "7", "AK4*2**7~");
      rejects("940", version, with(good, "W76", 1, 1, "99"), "8", "7", "AK3*ST*");
      List<X12Segment> noShipTo = new ArrayList<>(good);
      noShipTo.subList(1, 4).clear();
      rejects("940", version, noShipTo, "3", "1", "AK3*ST*");
      rejects("940", version, with(good, "W05", 1, 2, ""), "8", "1", "AK4*2**1~");
      rejects("940", version, with(good, "G62", 1, 4, ""), "8", "2", "AK4*3**2~");
   }

   /*******************************************************************************
    ** Crafted 945 errors: shipped versus ordered quantities, dates and carton codes.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void invalidShipmentAdvice(String version) throws Exception
   {
      List<X12Segment> good = LogisticsSupport.body945("ORD-7001", "PO-5001", "SHIP-9001", new int[] {10, 5}, new int[] {10, 4});
      assertThat(validate("945", version, good)).isEmpty();
      rejects("945", version, with(good, "W03", 1, 1, "99"), "8", "7", "AK3*ST*");
      rejects("945", version, with(good, "W12", 2, 2, ""), "8", "2", "AK4*4**2~");
      rejects("945", version, with(good, "W06", 1, 3, "20261345"), "8", "8", "AK4*3**8~");
      rejects("945", version, with(good, "MAN", 1, 1, "XX"), "8", "7", "AK4*1**7~");
      rejects("945", version, with(good, "W12", 1, 1, "QQ"), "8", "7", "AK4*1**7~");
   }

   /*******************************************************************************
    ** Crafted 214 errors: status and reason pairing, bad status code, time without a date, missing location.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void invalidShipmentStatus(String version) throws Exception
   {
      List<X12Segment> good = LogisticsSupport.body214("SHIP-9001", "PRO1234567", "AF", "NS", "Testville");
      rejects("214", version, with(good, "AT7", 1, 2, ""), "8", "2", "AK4*1**2~");
      rejects("214", version, with(good, "AT7", 1, 1, "ZZ"), "8", "7", "AK4*1**7~");
      rejects("214", version, with(good, "AT7", 1, 5, ""), "8", "2", "AK4*5**2~");
      rejects("214", version, with(good, "AT7", 1, 7, "CTX"), "8", "5", "AK4*7**5~");
      List<X12Segment> noLocation = new ArrayList<>(good);
      noLocation.removeIf(s -> s.id().equals("MS1"));
      var findings = new GuideValidator().validate(LogisticsSupport.transaction("214", version, noLocation), new BuiltInGuideLoader().resource("214-" + version), false);
      assertThat(findings).anyMatch(f -> "3".equals(f.ak3Code()) && "MS1".equals(f.segmentId()));
      rejects("214", version, with(good, "B10", 1, 2, ""), "8", "1", "AK4*2**1~");
   }

   /*******************************************************************************
    ** Crafted 204 errors: stop reasons and ordering, missing shipment id, no delivery stop.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void invalidLoadTender(String version) throws Exception
   {
      List<X12Segment> good = LogisticsSupport.body204("SHIP-3001", "00");
      assertThat(validate("204", version, good)).isEmpty();
      rejects("204", version, with(good, "S5", 1, 2, "XX"), "8", "7", "AK4*2**7~");
      rejects("204", version, with(good, "S5", 1, 2, "UL"), "8", "7", "AK3*ST*");
      rejects("204", version, with(with(good, "S5", 2, 2, "LD"), "G62", 2, 1, "10"), "8", "7", "AK3*ST*");
      rejects("204", version, with(good, "B2", 1, 4, ""), "8", "1", "AK4*4**1~");
      rejects("204", version, with(good, "S5", 1, 4, ""), "8", "2", "AK4*3**2~");
      List<X12Segment> noStops = new ArrayList<>(good);
      noStops.removeIf(s -> !List.of("B2", "B2A", "L11", "MS3", "N1", "L3").contains(s.id()));
      noStops.removeIf(s -> s.id().equals("N1") && !"BT".equals(s.value(1)));
      var findings = new GuideValidator().validate(LogisticsSupport.transaction("204", version, noStops), new BuiltInGuideLoader().resource("204-" + version), false);
      assertThat(findings).anyMatch(f -> "3".equals(f.ak3Code()) && "S5".equals(f.segmentId()));
   }

   /*******************************************************************************
    ** DATETIME halves, default times, and joined paths including a plus inside a predicate.
    *******************************************************************************/
   @Test
   void dateTimeTransformsAndJoinedPaths() throws Exception
   {
      TransformRegistry registry = new TransformRegistry();
      String chicago = "{\"timezone\":\"America/Chicago\"}";
      assertThat(registry.apply("DATEPART", chicago, List.of("2026-10-16T01:30:00Z"))).isEqualTo("20261015");
      assertThat(registry.apply("TIMEPART", chicago, List.of("2026-10-16T01:30:00Z"))).isEqualTo("203000");
      assertThat(registry.apply("DATEPART", chicago, List.of("2026-11-02T05:30:00Z"))).isEqualTo("20261101");
      assertThat(registry.apply("TIMEPART", chicago, List.of("2026-11-02T05:30:00Z"))).isEqualTo("233000");
      assertThat(registry.apply("DATEPART", null, List.of("2026-10-15T19:00:00"))).isEqualTo("20261015");
      assertThat(registry.apply("TIMEPART", null, List.of("2026-10-15T19:00:00"))).isEqualTo("190000");
      assertThat(registry.apply("TIMEPART", null, List.of("2026-10-15T14:00:00-05:00"))).isEqualTo("190000");
      assertThatThrownBy(() -> registry.apply("DATEPART", null, List.of())).hasMessage("Transform input is missing");
      assertThat(registry.apply("DATETIME", "{\"time\":\"0800\"}", List.of("20261015"))).isEqualTo(Instant.parse("2026-10-15T08:00:00Z"));
      assertThat(registry.apply("DATETIME", chicago, List.of("20261015", "1400"))).isEqualTo(Instant.parse("2026-10-15T19:00:00Z"));
      List<X12Segment> body = new ArrayList<>(LogisticsSupport.body940("ORD-7001", "PO-5001", 3));
      body.set(1, LogisticsSupport.s("N1", "ST", "Dock+Annex", "92", "SITE1"));
      List<QRecord> rules = new ArrayList<>();
      QRecord map = LogisticsSupport.definition("940", "004010", rules);
      rules.clear();
      rules.add(new QRecord().withValue("sequence", 1).withValue("ediPath", "N1[N102=Dock+Annex]/N301").withValue("qqqPath", "shipToAddress"));
      rules.add(new QRecord().withValue("sequence", 2).withValue("ediPath", "W0502+W0503").withValue("qqqPath", "orderNumber").withValue("transform", "CONCAT").withValue("transformArgs", "{\"separator\":\"/\"}"));
      var result = new InboundMapper(new TransformRegistry()).map(LogisticsSupport.transaction("940", "004010", body), new BuiltInGuideLoader().resource("940-004010"), map, rules);
      assertThat(result.record().getValueString("shipToAddress")).isEqualTo("100 Synthetic Way");
      assertThat(result.record().getValueString("orderNumber")).isEqualTo("ORD-7001/PO-5001");
   }

   /*******************************************************************************
    ** Configure the local identity and a transport-backed partner with an agreement.
    *******************************************************************************/
   private QRecord partner(String name, String code, String identity, String endpoint, String version) throws Exception
   {
      return (SystemWrites.call(() ->
      {
         QRecord partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", name).withValue("code", code).withValue("status", "ACTIVE").withValue("defaultTimezone", "America/Chicago"));
         QRecord local = EdiRecords.query("edi_identity", EdiRecords.equal("interchangeId", "QQQTEST")).stream().findFirst()
            .orElseGet(() -> unchecked(() -> SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "QQQTEST").withValue("applicationCode", "QQQTEST"))));
         QRecord remote = SystemWrites.insert("edi_identity", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", identity).withValue("applicationCode", identity));
         QRecord transport = SystemWrites.insert("edi_transport", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("name", endpoint).withValue("type", "GATEWAY").withValue("direction", "BOTH")
            .withValue("gatewayEndpointName", endpoint).withValue("isActive", true).withValue("maxAttempts", 3));
         return (SystemWrites.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", local.getValue("id")).withValue("partnerIdentityId", remote.getValue("id"))
            .withValue("standard", "X12").withValue("usageIndicator", "T").withValue("applyTestDocuments", true).withValue("isActive", true).withValue("interchangeVersion", version.equals("004010") ? "00401" : "00501")
            .withValue("outboundTransportId", transport.getValue("id"))));
      }));
   }

   /*******************************************************************************
    ** Run a checked supplier from a stream lambda.
    *******************************************************************************/
   private static <T> T unchecked(SystemWrites.Work<T> work)
   {
      try
      {
         return (work.run());
      }
      catch(Exception e)
      {
         throw new IllegalStateException(e);
      }
   }

   /*******************************************************************************
    ** Attach an agreement document with an installed sample map.
    *******************************************************************************/
   private void document(QRecord agreement, String direction, String set, String mapKey, String version, String table) throws Exception
   {
      SystemWrites.call(() ->
      {
         QRecord map = LogisticsSupport.installMap(mapKey, version);
         QRecord document = new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", direction).withValue("transactionSet", set).withValue("version", version)
            .withValue("functionalIdCode", LogisticsSupport.groupCode(set)).withValue("guideId", map.getValue("guideId")).withValue("mapId", map.getValue("id")).withValue("isActive", true);
         if(direction.equals("INBOUND"))
         {
            document.withValue("ackExpectation", "997").withValue("ackLevel", "ALL_TRANSACTIONS");
         }
         else
         {
            document.withValue("sourceTableName", table).withValue("outboundTrigger", "RECORD_CHANGE").withValue("batchMode", "IMMEDIATE").withValue("ackExpectation", "997").withValue("ackDueMinutes", 1440);
         }
         return (SystemWrites.insert("edi_agreement_document", document));
      });
   }

   /*******************************************************************************
    ** Deliver raw bytes through the durable intake, then run the inbox worker once.
    *******************************************************************************/
   private static void receive(String text, String external) throws Exception
   {
      new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("GATEWAY", external, "synthetic.edi", "127.0.0.1", new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII))));
      new InboxWorker().runOnce();
   }

   /*******************************************************************************
    ** Capture whatever the sender hands to the gateway outbox, then send everything queued.
    *******************************************************************************/
   private void sendAll() throws Exception
   {
      FileGatewayOutbox.register((endpoint, name, input, size) ->
      {
         sent.put(name, input.readAllBytes());
         return (new OutboxFileRef(sent.size(), endpoint, name, name, size, null, Instant.now()));
      });
      while(new SendWorker().runOnce() > 0)
      {
         // drain
      }
   }

   /*******************************************************************************
    ** Read every query row of a table as the service session.
    *******************************************************************************/
   private static List<QRecord> rows(String table) throws Exception
   {
      return (SystemWrites.call(() -> EdiRecords.query(table, new QQueryFilter())));
   }

   /*******************************************************************************
    ** The captured outbound interchange carrying a transaction set.
    *******************************************************************************/
   private String sentWith(String marker)
   {
      return (sent.values().stream().map(b -> new String(b, StandardCharsets.US_ASCII)).filter(t -> t.contains(marker)).findFirst().orElseThrow());
   }

   /*******************************************************************************
    ** A 3PL partner: inbound 940 through routing and mapping, an unconfigured 214 and unknown sender
    ** rejected, then the 945 for the shipped order built, enveloped, validated and sent.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void threePlPartnerRouting(String version) throws Exception
   {
      H2Support.create();
      LogisticsSupport.tables();
      EdiRuntime.current().initialize();
      QRecord agreement = partner("Harbor Test 3PL", "HARBOR", "HARBORLOG", "harbor-outbox", version);
      document(agreement, "INBOUND", "940", "940", version, null);
      document(agreement, "OUTBOUND", "945", "945", version, "sample_wms_shipment");
      receive(LogisticsSupport.interchange("940", version, "HARBORLOG", "000000011", LogisticsSupport.body940("ORD-7001", "PO-5001", 10, 5)), "940-a");
      assertThat(rows("sample_wms_order")).singleElement().satisfies(o ->
      {
         assertThat(o.getValueString("orderNumber")).isEqualTo("ORD-7001");
         assertThat(o.getValueString("orderStatus")).isEqualTo("NEW");
         assertThat(o.getValueInstant("requestedShipAt")).isEqualTo(Instant.parse("2026-10-15T19:00:00Z"));
      });
      assertThat(rows("sample_wms_order_line")).extracting(l -> l.getValueString("sku")).containsExactlyInAnyOrder("SKU-100", "SKU-200");
      assertThat(rows("sample_wms_order_line")).extracting(l -> l.getValueInteger("lineNumber")).containsExactlyInAnyOrder(1, 2);
      assertThat(rows("edi_transaction")).anyMatch(t -> "INBOUND".equals(t.getValueString("direction")) && "940".equals(t.getValueString("transactionSet")) && "APPLIED".equals(t.getValueString("status"))
         && "ORD-7001".equals(t.getValueString("businessKey")) && "sample_wms_order".equals(t.getValueString("targetTableName")));
      receive(LogisticsSupport.interchange("940", version, "HARBORLOG", "000000012", LogisticsSupport.body940("ORD-7001", "PO-5001", 12, 5)), "940-b");
      assertThat(rows("sample_wms_order")).hasSize(1);
      assertThat(rows("sample_wms_order").getFirst().getValueBigDecimal("totalQuantity")).isEqualByComparingTo("17");
      receive(LogisticsSupport.interchange("214", version, "HARBORLOG", "000000013", LogisticsSupport.body214("SHIP-1", "PRO1", "AF", "NS", "Testville")), "214-unconfigured");
      assertThat(rows("edi_transaction")).anyMatch(t -> "214".equals(t.getValueString("transactionSet")) && "INVALID".equals(t.getValueString("status")));
      assertThat(rows("edi_exception")).extracting(e -> e.getValueString("type")).contains("VALIDATION");
      receive(LogisticsSupport.interchange("940", version, "HARBORLOG", "000000015", with(LogisticsSupport.body940("ORD-7004", "PO-5004", 2), "W01", 1, 2, "ZZ")), "940-invalid");
      assertThat(rows("edi_transaction")).anyMatch(t -> "940".equals(t.getValueString("transactionSet")) && "INVALID".equals(t.getValueString("status")));
      assertThat(rows("sample_wms_order")).hasSize(1);
      receive(LogisticsSupport.interchange("940", version, "NOSUCHPARTNER", "000000014", LogisticsSupport.body940("ORD-7003", "PO-5003", 1)), "940-unknown");
      assertThat(rows("edi_exception")).extracting(e -> e.getValueString("type")).contains("UNKNOWN_PARTNER");
      assertThat(rows("sample_wms_order")).hasSize(1);
      QRecord order = LogisticsSupport.shipmentFor(map("940", "940", version, LogisticsSupport.body940("ORD-7001", "PO-5001", 12, 5)).record(), false);
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_wms_shipment", order)));
      assertThat(new OutboundWorker().runOnce()).isEqualTo(1);
      assertThat(rows("edi_transaction")).anyMatch(t -> "OUTBOUND".equals(t.getValueString("direction")) && "945".equals(t.getValueString("transactionSet"))
         && "ORD-7001".equals(t.getValueString("businessKey")) && "sample_wms_shipment".equals(t.getValueString("targetTableName")));
      sendAll();
      assertThat(rows("edi_outbound_message")).allMatch(m -> "SENT".equals(m.getValueString("status")));
      String interchange = sentWith("ST*945*");
      var parsed = LogisticsSupport.read(interchange);
      assertThat(parsed.findings()).isEmpty();
      assertThat(parsed.groups().getFirst().gs().value(1)).isEqualTo("SW");
      assertThat(parsed.groups().getFirst().gs().value(8)).isEqualTo(version);
      assertThat(new GuideValidator().validate(parsed.groups().getFirst().transactions().getFirst(), new BuiltInGuideLoader().resource("945-" + version), false)).isEmpty();
      assertThat(interchange).contains("W06*F*ORD-7001*20261016*SHIP-ORD-7001**PO-5001~", "W03*17~");
      assertThat(sentWith("AK5*R")).contains("AK2*940*0001~", "AK4*2**7~");
   }

   /*******************************************************************************
    ** A carrier partner: a 204 tender goes out through the pipeline and 214 statuses build a history.
    *******************************************************************************/
   @Test
   void carrierPartnerFlow() throws Exception
   {
      H2Support.create();
      LogisticsSupport.tables();
      EdiRuntime.current().initialize();
      QRecord agreement = partner("Ridgeline Test Freight", "RIDGE", "RIDGELINE", "ridge-outbox", "004010");
      document(agreement, "OUTBOUND", "204", "204-out", "004010", "sample_load");
      document(agreement, "INBOUND", "214", "214", "004010", null);
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_load", LogisticsSupport.load("SHIP-3001"))));
      assertThat(new OutboundWorker().runOnce()).isEqualTo(1);
      sendAll();
      String tender = sentWith("ST*204*");
      var parsed = LogisticsSupport.read(tender);
      assertThat(parsed.groups().getFirst().gs().value(1)).isEqualTo("SM");
      assertThat(new GuideValidator().validate(parsed.groups().getFirst().transactions().getFirst(), new BuiltInGuideLoader().resource("204-004010"), false)).isEmpty();
      assertThat(rows("edi_transaction")).anyMatch(t -> "204".equals(t.getValueString("transactionSet")) && "SHIP-3001".equals(t.getValueString("businessKey")));
      receive(LogisticsSupport.interchange("214", "004010", "RIDGELINE", "000000021", LogisticsSupport.body214("SHIP-3001", "PRO7654321", "X3", "NS", "Testville")), "214-a");
      receive(LogisticsSupport.interchange("214", "004010", "RIDGELINE", "000000022", LogisticsSupport.body214("SHIP-3001", "PRO7654321", "AF", "NS", "Testville")), "214-b");
      assertThat(rows("sample_shipment_status")).extracting(r -> r.getValueString("statusCode")).containsExactlyInAnyOrder("X3", "AF");
      assertThat(rows("sample_shipment_status")).allSatisfy(r ->
      {
         assertThat(r.getValueString("shipmentId")).isEqualTo("SHIP-3001");
         assertThat(r.getValueString("proNumber")).isEqualTo("PRO7654321");
         assertThat(r.getValueInstant("eventAt")).isEqualTo(Instant.parse("2026-10-16T19:15:00Z"));
      });
      PayloadRef ref = new PayloadRef(rows("edi_outbound_message").getFirst().getValueString("payloadRef"), null, 0);
      try(var stream = EdiRuntime.current().payloads().open(ref))
      {
         assertThat(LogisticsSupport.read(new String(stream.readAllBytes(), StandardCharsets.US_ASCII)).findings()).isEmpty();
      }
   }
}
