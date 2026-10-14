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

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;

/*******************************************************************************
 ** Synthetic retail schema and resource loader shared by acceptance tests/demo.
 *******************************************************************************/
public final class RetailSupport
{
   /*******************************************************************************
    ** Create only test-scope host tables with real QQQ associations.
    *******************************************************************************/
   public static void tables() throws Exception
   {
      var instance = QContext.getQInstance();
      String backend = EdiRuntime.current().config().getBackendName();
      for(String name : List.of("sample_order", "sample_order_line", "sample_shipment", "sample_shipment_carton", "sample_shipment_item", "sample_invoice", "sample_invoice_line", "sample_inventory"))
      {
         var table = new QTableMetaData().withName(name).withLabel(name).withBackendName(backend).withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id"));
         for(String field : List.of("poNumber", "sku", "shipmentNumber", "shipTime", "sscc", "invoiceNumber", "reportNumber"))
         {
            table.withField(new QFieldMetaData(field, QFieldType.STRING).withBackendName(com.kingsrook.qqq.backend.core.instances.QInstanceEnricher.inferBackendName(field)));
         }
         for(String field : List.of("orderDate", "shipDate", "invoiceDate", "reportDate"))
         {
            table.withField(new QFieldMetaData(field, QFieldType.DATE).withBackendName(com.kingsrook.qqq.backend.core.instances.QInstanceEnricher.inferBackendName(field)));
         }
         for(String field : List.of("quantity", "unitPrice", "total"))
         {
            table.withField(new QFieldMetaData(field, QFieldType.DECIMAL).withBackendName(field.equals("unitPrice") ? "unit_price" : field));
         }
         table.withField(new QFieldMetaData("parentId", QFieldType.INTEGER).withBackendName("parent_id"));
         instance.addTable(table);
         try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) instance.getBackend(backend)); var statement = connection.createStatement())
         {
            statement.execute(H2Support.ddl(table, false));
         }
      }
      association("sample_order", "sample_order_line", "lines");
      association("sample_invoice", "sample_invoice_line", "lines");
      association("sample_shipment", "sample_shipment_carton", "cartons");
      association("sample_shipment_carton", "sample_shipment_item", "items");
      EdiRuntime.current().config().withServiceSessionSupplier(() -> new QSystemUserSession(instance));
   }

   /*******************************************************************************
    ** Join-backed child records, including nested carton/item associations.
    *******************************************************************************/
   private static void association(String parent, String child, String name)
   {
      var instance = QContext.getQInstance();
      String join = parent + "_" + name;
      instance.getTable(parent).withAssociation(new Association().withName(name).withAssociatedTableName(child).withJoinName(join));
      instance.addJoin(new QJoinMetaData().withName(join).withType(JoinType.ONE_TO_MANY).withLeftTable(parent).withRightTable(child).withJoinOn(new JoinOn("id", "parentId")));
   }

   /*******************************************************************************
    ** Load a synthetic sample map into an ordinary record.
    *******************************************************************************/
   public static QRecord record(JsonNode node)
   {
      QRecord record = new QRecord();
      node.fields().forEachRemaining(e -> record.withValue(e.getKey(), e.getValue().isNull() ? null : e.getValue().isInt() ? e.getValue().intValue() : e.getValue().isBoolean() ? e.getValue().booleanValue() : e.getValue().asText()));
      return (record);
   }

   /*******************************************************************************
    ** Resource includes map metadata and ordered map rules.
    *******************************************************************************/
   public static JsonNode sample(String document, String version) throws Exception
   {
      try(var input = RetailSupport.class.getResourceAsStream("/edi/maps/" + document + "-" + version + "-sample.json"))
      {
         return (new ObjectMapper().readTree(input));
      }
   }

   /*******************************************************************************
    ** Persist a sample map against the installed version-specific guide.
    *******************************************************************************/
   public static QRecord installMap(String document, String version) throws Exception
   {
      var sample = sample(document, version);
      var guide = EdiRecords.query("edi_guide", EdiRecords.equal("transactionSet", document).withCriteria("version", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, version)).getFirst();
      QRecord map = SystemWrites.insert("edi_map", record(sample.get("map")).withValue("guideId", guide.getValue("id")));
      for(var rule : sample.get("rules"))
      {
         SystemWrites.insert("edi_map_rule", record(rule).withValue("mapId", map.getValue("id")));
      }
      return (map);
   }

   /*******************************************************************************
    ** Rich source with two lines and two cartons, each containing two items.
    *******************************************************************************/
   public static QRecord source()
   {
      var source = new QRecord().withValue("poNumber", "DEMO-1001").withValue("orderDate", LocalDate.of(2026, 10, 13))
         .withValue("shipmentNumber", "SHIP-1001").withValue("shipDate", LocalDate.of(2026, 10, 13)).withValue("shipTime", "10:30:00")
         .withValue("invoiceNumber", "INV-1001").withValue("invoiceDate", LocalDate.of(2026, 10, 13)).withValue("total", "30.00")
         .withValue("reportNumber", "INV-REPORT").withValue("reportDate", LocalDate.of(2026, 10, 13)).withValue("sku", "SKU-100").withValue("quantity", "12");
      source.withAssociatedRecords("lines", List.of(item("SKU-100"), item("SKU-200")));
      source.withAssociatedRecords("cartons", List.of(new QRecord().withValue("sscc", "000123456789000001").withAssociatedRecords("items", List.of(item("SKU-100"), item("SKU-200"))),
         new QRecord().withValue("sscc", "000123456789000002").withAssociatedRecords("items", List.of(item("SKU-300"), item("SKU-400")))));
      return (source);
   }

   /*******************************************************************************
    ** Synthetic stock item shared by each document family.
    *******************************************************************************/
   private static QRecord item(String sku)
   {
      return (new QRecord().withValue("sku", sku).withValue("quantity", "2").withValue("unitPrice", "7.50"));
   }

   /*******************************************************************************
    ** Persist associated rows through normal QQQ actions.
    *******************************************************************************/
   public static QRecord saveSource(String table, QRecord source) throws Exception
   {
      var associations = source.getAssociatedRecords();
      QRecord copy = new QRecord();
      copy.getValues().putAll(source.getValues());
      QRecord saved = SystemWrites.insert(table, copy);
      if(associations != null && QContext.getQInstance().getTable(table).getAssociations() != null)
      {
         for(var association : QContext.getQInstance().getTable(table).getAssociations())
         {
            for(var child : associations.getOrDefault(association.getName(), List.of()))
            {
               saveSource(association.getAssociatedTableName(), child.withValue("parentId", saved.getValue("id")));
            }
         }
      }
      return (saved);
   }
}
