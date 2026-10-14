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
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.X12EnvelopeReader;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qbits.edi.x12.X12Writer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Synthetic warehouse and logistics schema, sample records and raw X12 builders
 ** shared by the phase d acceptance tests and the demo. Test sources only.
 *******************************************************************************/
public final class LogisticsSupport
{
   public static final String CARRIER = "SYNC";

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private LogisticsSupport()
   {
   }

   /*******************************************************************************
    ** Create the test-scope warehouse, status and load tables with real associations.
    *******************************************************************************/
   public static void tables() throws Exception
   {
      table("sample_wms_order", "orderNumber", "poNumber", "orderStatus", "shipToName", "shipToAddress", "shipToCity", "shipToState", "shipToPostal", "carrierScac", "note",
         "requestedShipAt:DATE_TIME", "totalQuantity:DECIMAL");
      table("sample_wms_order_line", "parentId:INTEGER", "lineNumber:INTEGER", "sku", "description", "quantity:DECIMAL", "unit");
      table("sample_wms_shipment", "orderNumber", "poNumber", "shipmentNumber", "carrierScac", "routing", "shipToName", "shipToAddress", "shipToCity", "shipToState", "shipToPostal",
         "shippedAt:DATE_TIME", "totalUnits:DECIMAL");
      table("sample_wms_shipment_line", "parentId:INTEGER", "lineNumber:INTEGER", "sku", "statusCode", "unit", "sscc", "orderedQuantity:DECIMAL", "shippedQuantity:DECIMAL", "shortQuantity:DECIMAL");
      table("sample_shipment_status", "shipmentId", "proNumber", "carrierScac", "statusCode", "reasonCode", "city", "state", "equipmentNumber", "eventAt:DATE_TIME");
      table("sample_load", "shipmentId", "carrierScac", "purpose", "paymentMethod", "bolNumber", "billToName", "totalWeight:DECIMAL");
      table("sample_load_stop", "parentId:INTEGER", "stopSequence:INTEGER", "stopReason", "name", "address", "city", "state", "postal", "orderNumber", "appointmentAt:DATE_TIME", "weight:DECIMAL");
      association("sample_wms_order", "sample_wms_order_line", "lines");
      association("sample_wms_shipment", "sample_wms_shipment_line", "lines");
      association("sample_load", "sample_load_stop", "stops");
      var instance = QContext.getQInstance();
      EdiRuntime.current().config().withServiceSessionSupplier(() -> new QSystemUserSession(instance));
   }

   /*******************************************************************************
    ** Define one table and its H2 schema; fields are STRING unless suffixed with a type.
    *******************************************************************************/
   private static void table(String name, String... fields) throws Exception
   {
      var instance = QContext.getQInstance();
      String backend = EdiRuntime.current().config().getBackendName();
      var table = new QTableMetaData().withName(name).withLabel(name).withBackendName(backend).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id"));
      for(String field : fields)
      {
         String[] parts = field.split(":");
         table.withField(new QFieldMetaData(parts[0], parts.length > 1 ? QFieldType.valueOf(parts[1]) : QFieldType.STRING).withBackendName(QInstanceEnricher.inferBackendName(parts[0])));
      }
      instance.addTable(table);
      try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) instance.getBackend(backend)); var statement = connection.createStatement())
      {
         statement.execute(H2Support.ddl(table, false));
      }
   }

   /*******************************************************************************
    ** Join-backed child records, as the retail samples do.
    *******************************************************************************/
   private static void association(String parent, String child, String name)
   {
      var instance = QContext.getQInstance();
      String join = parent + "_" + name;
      instance.getTable(parent).withAssociation(new Association().withName(name).withAssociatedTableName(child).withJoinName(join));
      instance.addJoin(new QJoinMetaData().withName(join).withType(JoinType.ONE_TO_MANY).withLeftTable(parent).withRightTable(child).withJoinOn(new JoinOn("id", "parentId")));
   }

   /*******************************************************************************
    ** Persist a bundled sample map (for example "945" or "945-in") against the installed guide.
    *******************************************************************************/
   public static QRecord installMap(String key, String version) throws Exception
   {
      JsonNode sample = RetailSupport.sample(key, version);
      String set = sample.get("map").get("transactionSet").asText();
      var guide = EdiRecords.query("edi_guide", EdiRecords.equal("transactionSet", set).withCriteria("version", QCriteriaOperator.EQUALS, version)).getFirst();
      QRecord map = SystemWrites.insert("edi_map", RetailSupport.record(sample.get("map")).withValue("guideId", guide.getValue("id")));
      for(var rule : sample.get("rules"))
      {
         SystemWrites.insert("edi_map_rule", RetailSupport.record(rule).withValue("mapId", map.getValue("id")));
      }
      return (map);
   }

   /*******************************************************************************
    ** The map record of a bundled sample, filling the supplied list with its rules; no database.
    *******************************************************************************/
   public static QRecord definition(String key, String version, List<QRecord> rules) throws Exception
   {
      JsonNode sample = RetailSupport.sample(key, version);
      sample.get("rules").forEach(r -> rules.add(RetailSupport.record(r)));
      return (RetailSupport.record(sample.get("map")));
   }

   /*******************************************************************************
    ** Shorthand for a segment whose position the reader assigns.
    *******************************************************************************/
   public static X12Segment s(String id, String... values)
   {
      return (X12Segment.of(id, 0, values));
   }

   /*******************************************************************************
    ** The functional group code for each logistics document.
    *******************************************************************************/
   public static String groupCode(String set)
   {
      return (switch(set)
      {
         case "940" -> "OW";
         case "945" -> "SW";
         case "214" -> "QM";
         default -> "SM";
      });
   }

   /*******************************************************************************
    ** Wrap a transaction body (without ST/SE) in a complete interchange from a sender to QQQTEST.
    *******************************************************************************/
   public static String interchange(String set, String version, String sender, String control, List<X12Segment> body)
   {
      List<X12Segment> all = new ArrayList<>();
      String group = control.replaceFirst("^0+", "");
      all.add(s("ISA", "00", "", "00", "", "ZZ", sender, "ZZ", "QQQTEST", "261015", "1200", version.equals("004010") ? "U" : "^", version.substring(0, 5), control, "0", "T", ":"));
      all.add(s("GS", groupCode(set), sender, "QQQTEST", "20261015", "1200", group, "X", version));
      all.add(s("ST", set, "0001"));
      all.addAll(body);
      all.add(s("SE", String.valueOf(body.size() + 2), "0001"));
      all.add(s("GE", "1", group));
      all.add(s("IEA", "1", control));
      return (new X12Writer().write(all, X12Separators.DEFAULT, ""));
   }

   /*******************************************************************************
    ** Parse an interchange without the database runtime.
    *******************************************************************************/
   public static X12Interchange read(String text) throws Exception
   {
      return (new X12EnvelopeReader(100000).read(new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII))).getFirst());
   }

   /*******************************************************************************
    ** The single transaction of a one-document interchange built from a body.
    *******************************************************************************/
   public static X12TransactionSet transaction(String set, String version, List<X12Segment> body) throws Exception
   {
      return (read(interchange(set, version, "HARBORLOG", "000000001", body)).groups().getFirst().transactions().getFirst());
   }

   /*******************************************************************************
    ** Shared N1/N3/N4 party block.
    *******************************************************************************/
   private static List<X12Segment> party(String code, String name, String street, String city, String state, String postal)
   {
      return (List.of(s("N1", code, name, "92", "SITE1"), s("N3", street), s("N4", city, state, postal)));
   }

   /*******************************************************************************
    ** A 3PL receives a shipping order: one line per quantity, total in W76.
    *******************************************************************************/
   public static List<X12Segment> body940(String order, String po, int... quantities)
   {
      List<X12Segment> body = new ArrayList<>();
      body.add(s("W05", "N", order, po));
      body.addAll(party("ST", "Harbor Test Dock", "100 Synthetic Way", "Testville", "IL", "60000"));
      body.add(s("G61", "IC", "Test Contact", "TE", "2025550100"));
      body.add(s("N9", "PO", po));
      body.add(s("G62", "10", "20261015", "Y", "1400", "CT"));
      body.add(s("NTE", "GEN", "Synthetic note"));
      body.add(s("W66", "PP", "M", "", "ROUTE-A", CARRIER));
      int total = 0;
      for(int i = 0; i < quantities.length; i++)
      {
         body.add(s("LX", String.valueOf(i + 1)));
         body.add(s("W01", String.valueOf(quantities[i]), "EA", "", "VN", "SKU-" + (100 * (i + 1))));
         body.add(s("G69", "Synthetic item " + (i + 1)));
         body.add(s("N9", "LI", String.valueOf(i + 1)));
         total += quantities[i];
      }
      body.add(s("W76", String.valueOf(total)));
      return (body);
   }

   /*******************************************************************************
    ** A 3PL confirms a shipment; lines shipped short report the difference.
    *******************************************************************************/
   public static List<X12Segment> body945(String order, String po, String shipment, int[] ordered, int[] shipped)
   {
      List<X12Segment> body = new ArrayList<>();
      body.add(s("W06", "F", order, "20261016", shipment, "", po));
      body.addAll(party("ST", "Harbor Test Dock", "100 Synthetic Way", "Testville", "IL", "60000"));
      body.add(s("N9", "PO", po));
      body.add(s("G62", "11", "20261016", "Y", "0930", "CT"));
      body.add(s("W27", "M", CARRIER, "ROUTE-A"));
      int total = 0;
      for(int i = 0; i < ordered.length; i++)
      {
         body.add(s("LX", String.valueOf(i + 1)));
         boolean complete = ordered[i] == shipped[i];
         body.add(s("W12", complete ? "CC" : "PR", String.valueOf(ordered[i]), String.valueOf(shipped[i]), complete ? "" : String.valueOf(ordered[i] - shipped[i]), "EA", "", "VN", "SKU-" + (100 * (i + 1))));
         body.add(s("MAN", "GM", "00012345678900001" + (i + 1)));
         total += shipped[i];
      }
      body.add(s("W03", String.valueOf(total)));
      return (body);
   }

   /*******************************************************************************
    ** A carrier status event with an arrival or departure location.
    *******************************************************************************/
   public static List<X12Segment> body214(String shipment, String pro, String status, String reason, String city)
   {
      List<X12Segment> body = new ArrayList<>();
      body.add(s("B10", pro, shipment, CARRIER));
      body.add(s("L11", "BOL-8800", "BM"));
      body.addAll(party("CN", "Harbor Test Dock", "100 Synthetic Way", "Testville", "IL", "60000"));
      body.add(s("LX", "1"));
      body.add(s("AT7", status, reason, "", "", "20261016", "1415", "CT"));
      body.add(s("MS1", city, "IL", "US"));
      body.add(s("MS2", CARRIER, "TRL12345"));
      body.add(s("AT8", "G", "L", "1250", "10"));
      return (body);
   }

   /*******************************************************************************
    ** A shipper tenders a two-stop load to a carrier.
    *******************************************************************************/
   public static List<X12Segment> body204(String shipment, String purpose)
   {
      List<X12Segment> body = new ArrayList<>();
      body.add(s("B2", "", CARRIER, "", shipment, "", "PP"));
      body.add(s("B2A", purpose, "LT"));
      body.add(s("L11", "BOL-8800", "BM"));
      body.add(s("MS3", CARRIER, "B", "Testville", "M"));
      body.add(s("N1", "BT", "Synthetic Billing"));
      body.add(s("S5", "1", "LD", "12500", "L", "20", "CA"));
      body.add(s("G62", "10", "20261015", "Y", "1400", "CT"));
      body.addAll(List.of(s("N1", "SF", "Synthetic Plant"), s("N3", "1 Origin Road"), s("N4", "Testville", "IL", "60000")));
      body.add(s("OID", "ORD-7001", "PO-5001", "", "EA", "20"));
      body.add(s("S5", "2", "UL", "12500", "L", "20", "CA"));
      body.add(s("G62", "02", "20261016", "Y", "0800", "CT"));
      body.addAll(List.of(s("N1", "ST", "Synthetic Receiver"), s("N3", "2 Dest Road"), s("N4", "Desttown", "TX", "75000")));
      body.add(s("OID", "ORD-7001", "PO-5001", "", "EA", "20"));
      body.add(s("L3", "12500", "G", "", "", "", "", "", "", "", "", "", "", "20"));
      return (body);
   }

   /*******************************************************************************
    ** An order as a 3PL would hold it after receiving a 940, with one EA line per quantity.
    *******************************************************************************/
   public static QRecord orderRecord(String order, String po, int... quantities)
   {
      List<QRecord> lines = new ArrayList<>();
      for(int i = 0; i < quantities.length; i++)
      {
         lines.add(new QRecord().withValue("lineNumber", i + 1).withValue("sku", "SKU-" + (100 * (i + 1))).withValue("description", "Synthetic item " + (i + 1))
            .withValue("quantity", new BigDecimal(quantities[i])).withValue("unit", "EA"));
      }
      return (new QRecord().withValue("orderNumber", order).withValue("poNumber", po).withValue("orderStatus", "NEW").withValue("shipToName", "Harbor Test Dock").withValue("shipToAddress", "100 Synthetic Way")
         .withValue("shipToCity", "Testville").withValue("shipToState", "IL").withValue("shipToPostal", "60000").withValue("carrierScac", CARRIER).withAssociatedRecords("lines", lines));
   }

   /*******************************************************************************
    ** Build the shipment a 3PL records after picking an order, optionally short on the last line.
    *******************************************************************************/
   public static QRecord shipmentFor(QRecord order, boolean shortLast)
   {
      List<QRecord> lines = new ArrayList<>();
      List<QRecord> orderLines = order.getAssociatedRecords().get("lines");
      BigDecimal total = BigDecimal.ZERO;
      for(int i = 0; i < orderLines.size(); i++)
      {
         QRecord line = orderLines.get(i);
         BigDecimal ordered = new BigDecimal(line.getValueString("quantity"));
         boolean shorted = shortLast && i == orderLines.size() - 1;
         BigDecimal shipped = shorted ? ordered.subtract(BigDecimal.ONE) : ordered;
         total = total.add(shipped);
         lines.add(new QRecord().withValue("lineNumber", i + 1).withValue("sku", line.getValueString("sku")).withValue("unit", line.getValueString("unit"))
            .withValue("statusCode", shorted ? "PR" : "CC").withValue("orderedQuantity", ordered).withValue("shippedQuantity", shipped)
            .withValue("shortQuantity", shorted ? BigDecimal.ONE : null).withValue("sscc", "00012345678900001" + (i + 1)));
      }
      return (new QRecord().withValue("orderNumber", order.getValueString("orderNumber")).withValue("poNumber", order.getValueString("poNumber")).withValue("shipmentNumber", "SHIP-" + order.getValueString("orderNumber"))
         .withValue("carrierScac", order.getValueString("carrierScac")).withValue("routing", "ROUTE-A").withValue("shipToName", order.getValueString("shipToName")).withValue("shipToAddress", order.getValueString("shipToAddress"))
         .withValue("shipToCity", order.getValueString("shipToCity")).withValue("shipToState", order.getValueString("shipToState")).withValue("shipToPostal", order.getValueString("shipToPostal"))
         .withValue("shippedAt", Instant.parse("2026-10-16T14:30:00Z")).withValue("totalUnits", total).withAssociatedRecords("lines", lines));
   }

   /*******************************************************************************
    ** A two-stop load tendered by a shipper to a carrier, with appointments in Chicago time.
    *******************************************************************************/
   public static QRecord load(String shipment)
   {
      QRecord pickup = new QRecord().withValue("stopSequence", 1).withValue("stopReason", "LD").withValue("name", "Synthetic Plant").withValue("address", "1 Origin Road")
         .withValue("city", "Testville").withValue("state", "IL").withValue("postal", "60000").withValue("orderNumber", "ORD-7001")
         .withValue("appointmentAt", Instant.parse("2026-10-15T19:00:00Z")).withValue("weight", new BigDecimal("12500"));
      QRecord delivery = new QRecord().withValue("stopSequence", 2).withValue("stopReason", "UL").withValue("name", "Synthetic Receiver").withValue("address", "2 Dest Road")
         .withValue("city", "Desttown").withValue("state", "TX").withValue("postal", "75000").withValue("orderNumber", "ORD-7001")
         .withValue("appointmentAt", Instant.parse("2026-10-16T13:00:00Z")).withValue("weight", new BigDecimal("12500"));
      return (new QRecord().withValue("shipmentId", shipment).withValue("carrierScac", CARRIER).withValue("purpose", "ORIGINAL").withValue("paymentMethod", "PP")
         .withValue("bolNumber", "BOL-8800").withValue("billToName", "Synthetic Billing").withValue("totalWeight", new BigDecimal("12500")).withAssociatedRecords("stops", List.of(pickup, delivery)));
   }
}
