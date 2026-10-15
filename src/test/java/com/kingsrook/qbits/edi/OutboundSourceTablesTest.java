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

import java.util.List;
import com.kingsrook.qbits.edi.flow.OutboundEnqueue;
import com.kingsrook.qbits.edi.flow.OutboundSourceTables;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** The record-change listener only costs queries for configured source tables,
 ** and its cache follows agreement and document changes.
 *******************************************************************************/
class OutboundSourceTablesTest
{
   private QRecord document;
   private QRecord agreement;

   /*******************************************************************************
    ** One active RECORD_CHANGE agreement document, for sample_order.
    *******************************************************************************/
   @BeforeEach
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
         QRecord transport = SystemWrites.insert("edi_transport", new QRecord().withValue("name", "retail-out").withValue("type", "GATEWAY").withValue("direction", "BOTH").withValue("gatewayEndpointName", "retail-outbox").withValue("isActive", true).withValue("maxAttempts", 3));
         agreement = SystemWrites.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", local.getValue("id")).withValue("partnerIdentityId", remote.getValue("id"))
            .withValue("standard", "X12").withValue("usageIndicator", "T").withValue("isActive", true).withValue("outboundTransportId", transport.getValue("id")));
         QRecord map = RetailSupport.installMap("855", "004010");
         document = SystemWrites.insert("edi_agreement_document", outboundDocument(map, "sample_order"));
         return (null);
      });
   }

   /*******************************************************************************
    ** Reset context.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Writes to a table without a definition neither apply nor reload definitions.
    *******************************************************************************/
   @Test
   void unconfiguredTableDoesNoWork() throws Exception
   {
      OutboundEnqueue listener = new OutboundEnqueue();
      assertThat(listener.appliesTo("sample_order", RecordChangeType.INSERT)).isTrue();
      long loads = sources().loadCount();

      assertThat(listener.appliesTo("sample_invoice", RecordChangeType.INSERT)).isFalse();
      assertThat(listener.appliesTo("sample_invoice", RecordChangeType.UPDATE)).isFalse();
      assertThat(listener.appliesTo("sample_order", RecordChangeType.DELETE)).isFalse();
      SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         for(int i = 0; i < 5; i++)
         {
            RetailSupport.saveSource("sample_invoice", new QRecord().withValue("invoiceNumber", "INV-" + i));
         }
         return (null);
      }));

      assertThat(sources().loadCount()).isEqualTo(loads);
      assertThat(rows("edi_outbound_request")).isEmpty();
   }

   /*******************************************************************************
    ** Configured tables still enqueue, once per record, from one cached load.
    *******************************************************************************/
   @Test
   void configuredTableEnqueues() throws Exception
   {
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source())));
      long loads = sources().loadCount();
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source().withValue("poNumber", "PO-2"))));
      assertThat(sources().loadCount()).isEqualTo(loads);
      assertThat(rows("edi_outbound_request")).hasSize(2);
   }

   /*******************************************************************************
    ** Document and agreement changes invalidate the cached tables.
    *******************************************************************************/
   @Test
   void definitionChangesInvalidate() throws Exception
   {
      OutboundEnqueue listener = new OutboundEnqueue();
      assertThat(listener.appliesTo("sample_order", RecordChangeType.INSERT)).isTrue();
      assertThat(listener.appliesTo("edi_agreement_document", RecordChangeType.DELETE)).isTrue();

      update("edi_agreement_document", document.getValue("id"), "isActive", false);
      assertThat(listener.appliesTo("sample_order", RecordChangeType.INSERT)).isFalse();

      update("edi_agreement_document", document.getValue("id"), "isActive", true);
      assertThat(listener.appliesTo("sample_order", RecordChangeType.INSERT)).isTrue();

      update("edi_agreement", agreement.getValue("id"), "isActive", false);
      assertThat(listener.appliesTo("sample_order", RecordChangeType.INSERT)).isFalse();
      SystemWrites.call(() -> SystemWrites.transaction(() -> RetailSupport.saveSource("sample_order", RetailSupport.source())));
      assertThat(rows("edi_outbound_request")).isEmpty();

      update("edi_agreement", agreement.getValue("id"), "isActive", true);
      assertThat(listener.appliesTo("sample_order", RecordChangeType.INSERT)).isTrue();
   }

   /*******************************************************************************
    ** A transaction that adds a definition sees it before commit; a rolled-back
    ** one leaves the cache on committed data.
    *******************************************************************************/
   @Test
   void changingTransactionSeesItsOwnDefinitions() throws Exception
   {
      QRecord map = SystemWrites.call(() -> EdiRecords.query("edi_map", new QQueryFilter()).getFirst());
      SystemWrites.call(() -> SystemWrites.transaction(() ->
      {
         SystemWrites.insert("edi_agreement_document", outboundDocument(map, "sample_invoice").withValue("transactionSet", "810"));
         RetailSupport.saveSource("sample_invoice", new QRecord().withValue("invoiceNumber", "INV-1"));
         return (null);
      }));
      assertThat(rows("edi_outbound_request")).hasSize(1);
      assertThat(new OutboundEnqueue().appliesTo("sample_invoice", RecordChangeType.INSERT)).isTrue();

      try
      {
         SystemWrites.call(() -> SystemWrites.transaction(() ->
         {
            SystemWrites.insert("edi_agreement_document", outboundDocument(map, "sample_inventory").withValue("transactionSet", "846"));
            throw new IllegalStateException("rollback synthetic write");
         }));
      }
      catch(Exception e)
      {
         ////////////////////////////
         // expected - rolled back //
         ////////////////////////////
      }
      assertThat(new OutboundEnqueue().appliesTo("sample_inventory", RecordChangeType.INSERT)).isFalse();
   }

   /*******************************************************************************
    ** An active outbound RECORD_CHANGE document for the source table.
    *******************************************************************************/
   private QRecord outboundDocument(QRecord map, String sourceTable)
   {
      return (new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", "OUTBOUND").withValue("transactionSet", "855")
         .withValue("version", "004010").withValue("functionalIdCode", "PR").withValue("guideId", map.getValue("guideId")).withValue("mapId", map.getValue("id"))
         .withValue("sourceTableName", sourceTable).withValue("outboundTrigger", "RECORD_CHANGE").withValue("batchMode", "IMMEDIATE").withValue("ackExpectation", "997").withValue("ackDueMinutes", 1).withValue("isActive", true));
   }

   /*******************************************************************************
    ** Update one field through QQQ, outside any transaction.
    *******************************************************************************/
   private static void update(String table, Object id, String field, Object value) throws Exception
   {
      SystemWrites.call(() ->
      {
         SystemWrites.update(table, new QRecord().withValue("id", (java.io.Serializable) id).withValue(field, (java.io.Serializable) value));
         return (null);
      });
   }

   /*******************************************************************************
    ** The runtime cache under test.
    *******************************************************************************/
   private static OutboundSourceTables sources()
   {
      return (EdiRuntime.current().outboundSources());
   }

   /*******************************************************************************
    ** All rows.
    *******************************************************************************/
   private static List<QRecord> rows(String table) throws Exception
   {
      return (SystemWrites.call(() -> EdiRecords.query(table, new QQueryFilter())));
   }
}
