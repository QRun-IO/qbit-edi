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


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.edi.metadata.EdiTables;
import com.kingsrook.qbits.edi.security.EdiClassifications;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Metadata contracts and record labels as consumed by the operator UI.
 *******************************************************************************/
class EdiPresentationTest
{
   /*******************************************************************************
    ** All registered tables have readable labels and linked internal references.
    *******************************************************************************/
   @Test
   void labelsAndReferencesAreConsistent() throws Exception
   {
      var instance = H2Support.create();
      Set<String> externalIds = Set.of("interchangeId", "senderId", "receiverId", "elementId", "segmentId", "loopId", "parentLoopId",
         "as2LocalId", "as2PartnerId", "messageId", "originalMessageId", "externalId", "transportMessageId", "eventId", "sourceRecordId", "targetRecordId", "keyId", "gatewayUserId", "assignedToUserId");
      for(var table : instance.getTables().values())
      {
         assertThat(table.getLabel()).as(table.getName()).matches("[A-Z][A-Za-z0-9]*( [A-Z][A-Za-z0-9]*)*");
         for(var field : table.getFields().values())
         {
            assertThat(field.getLabel()).as(table.getName() + "." + field.getName())
               .doesNotContainPattern("\\b(Id|Phi|Pii|Url|Isa|Gs|St|As2|Sftp|Https|Http|Mdn|Mic|Tls|Ta1|Edi|Upc|Npi|Qqq|Json|Pem|Snip|Van)\\b");
            if(field.getName().endsWith("Id") && !(field.getType() == QFieldType.STRING && externalIds.contains(field.getName())))
            {
               assertThat(field.getPossibleValueSourceName()).as(table.getName() + "." + field.getName()).isNotBlank();
               var source = instance.getPossibleValueSource(field.getPossibleValueSourceName());
               assertThat(source.getType()).isEqualTo(QPossibleValueSourceType.TABLE);
               assertThat(instance.getTable(source.getTableName())).isNotNull();
               assertThat(field.getLabel()).doesNotEndWith(" ID");
            }
         }
         for(String name : table.getRecordLabelFields() == null ? List.<String>of() : table.getRecordLabelFields())
         {
            var classification = (EdiClassifications) table.getField(name).getSupplementalMetaData().get("edi.classification");
            assertThat(classification.getClassification()).as(table.getName() + "." + name).isNotEqualTo("PHI");
         }
      }
      assertThat(instance.getTable("edi_exception").getField("assignedToUserId").getLabel()).isEqualTo("Assigned To");
      assertThat(instance.getTable("edi_exception").getField("outboundMessageId").getPossibleValueSourceName()).isEqualTo("edi_outbound_message");
      assertThat(EdiTables.childWidgets()).extracting(widget -> widget.getLabel()).contains("AS2 MDNs");
      assertThat(instance.getTable("edi_as2_mdn").getLabel()).isEqualTo("AS2 MDN");
      assertThat(instance.getTable("edi_as2_mdn").getField("micValue").getLabel()).isEqualTo("MIC Value");
      assertThat(EdiTables.childWidgets()).allSatisfy(widget -> assertThat(widget.getLabel()).doesNotContain("(", "_", "transaction", "identity"));
   }



   /*******************************************************************************
    ** List columns and first record section use the same operator-first order.
    *******************************************************************************/
   @Test
   void fieldsAndSectionsLeadWithOperatorKeys() throws Exception
   {
      var tables = EdiTables.tables(new EdiQBitConfig().withBackendName("memory"));
      Map<String, List<String>> expected = Map.of(
         "edi_exception", List.of("type", "status", "summary", "partnerId", "transactionId", "interchangeId", "assignedToUserId", "occurrences"),
         "edi_transaction", List.of("transactionSet", "businessKeyType", "businessKey", "partnerId", "direction", "status", "ackStatus", "version", "interchangeId"),
         "edi_interchange", List.of("direction", "partnerId", "controlNumber", "status", "interchangeDate", "transportId", "groupCount"));
      for(var table : tables)
      {
         assertThat(table.getFields().keySet().iterator().next()).as(table.getName()).isNotEqualTo("id");
         assertThat(table.getField("id").getIsHidden()).isNotEqualTo(true);
         List<String> sections = table.getSections().stream().filter(section -> section.getFieldNames() != null).flatMap(section -> section.getFieldNames().stream()).toList();
         assertThat(sections).doesNotHaveDuplicates();
         assertThat(sections).containsExactlyElementsOf(table.getFields().values().stream()
            .filter(field -> !Boolean.TRUE.equals(field.getIsHidden())).map(field -> field.getName()).toList());
         if(expected.containsKey(table.getName()))
         {
            assertThat(table.getFields().keySet().stream().toList()).startsWith(expected.get(table.getName()).toArray(String[]::new));
            assertThat(table.getSections().getFirst().getFieldNames()).containsExactlyElementsOf(expected.get(table.getName()));
         }
      }
   }



   /*******************************************************************************
    ** Real get, query and possible-value actions agree on business-key fallbacks.
    *******************************************************************************/
   @Test
   void documentLabelsUseBusinessKeysAndProtectPhi() throws Exception
   {
      var instance = H2Support.create();
      SystemWrites.call(() ->
      {
         List<QRecord> records = List.of(
            new QRecord().withValue("transactionSet", "850").withValue("businessKeyType", "PO").withValue("businessKey", "DEMO-100").withValue("controlNumber", "0001"),
            new QRecord().withValue("transactionSet", "997").withValue("controlNumber", "0002"),
            new QRecord().withValue("transactionSet", "837").withValue("businessKeyType", "CLAIM").withValue("businessKey", "SYNTHETIC-PHI").withValue("containsPhi", true).withValue("controlNumber", "0003"),
            new QRecord().withValue("transactionSet", "850").withValue("businessKeyType", "PO").withValue("businessKey", " ").withValue("controlNumber", "0004"),
            new QRecord().withValue("transactionSet", "850").withValue("businessKey", "DEMO-200").withValue("controlNumber", "0005"));
         List<String> labels = List.of("850 PO DEMO-100", "997 ST 0002", "837 ST 0003", "850 ST 0004", "850 ST 0005");
         for(int i = 0; i < records.size(); i++)
         {
            QRecord inserted = SystemWrites.insert("edi_transaction", records.get(i).withValue("phiBusinessKey", "SYNTHETIC-PRIVATE-FIELD"));
            var fetched = new GetAction().execute(new GetInput("edi_transaction").withPrimaryKey(inserted.getValue("id")).withShouldGenerateDisplayValues(true)).getRecord();
            assertThat(fetched.getRecordLabel()).isEqualTo(labels.get(i));
            var values = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput().withPossibleValueSourceName("edi_transaction")
               .withIdList(List.of(inserted.getValue("id")))).getResults();
            assertThat(values).hasSize(1);
            assertThat(values.getFirst().getLabel()).isEqualTo(labels.get(i));
         }
         var matches = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput().withPossibleValueSourceName("edi_transaction")
            .withSearchTerm("DEMO-100").withLimit(10)).getResults();
         assertThat(matches).extracting(value -> value.getLabel()).containsExactly("850 PO DEMO-100");
         assertThat(EdiRecords.query("edi_transaction", new QQueryFilter())).extracting(QRecord::getRecordLabel).containsExactlyInAnyOrderElementsOf(labels);
         return (null);
      });
      QContext.clear();
   }



   /*******************************************************************************
    ** Activity children sort by creation time; the home queue lists open work oldest first.
    *******************************************************************************/
   @Test
   void activityWidgetsShowNewestFirst() throws Exception
   {
      var instance = H2Support.create();
      SystemWrites.call(() ->
      {
         var partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", "Sort Demo").withValue("code", "SORT"));
         var day = Instant.parse("2026-01-01T00:00:00Z");
         for(int i : List.of(2, 0, 1))
         {
            for(String table : List.of("edi_transaction", "edi_interchange", "edi_exception"))
            {
               instance.getTable(table).getField("createDate")
                  .withBehavior(DynamicDefaultValueBehavior.NONE).withDefaultValue(day.plusSeconds(i));
               QRecord row = new QRecord().withValue("partnerId", partner.getValue("id"));
               if(table.equals("edi_exception"))
               {
                  row.withValue("type", "VALIDATION").withValue("status", i == 2 ? "IN_PROGRESS" : "OPEN").withValue("summary", "Synthetic " + i);
               }
               SystemWrites.insert(table, row);
            }
         }
         for(String table : List.of("edi_transaction", "edi_interchange", "edi_exception"))
         {
            var widget = instance.getWidget("edi_partner_partner_id_" + table);
            var input = new RenderWidgetInput().withWidgetMetaData(widget);
            input.addQueryParam("tableName", "edi_partner");
            input.addQueryParam("id", partner.getValueString("id"));
            var data = (ChildRecordListData) new RenderWidgetAction().execute(input).getWidgetData();
            assertThat(data.getQueryOutput().getRecords()).extracting(row -> row.getValueInstant("createDate")).containsExactly(day.plusSeconds(2), day.plusSeconds(1), day);
         }
         var input = new RenderWidgetInput().withWidgetMetaData(instance.getWidget("edi_home_queue"));
         var queue = (com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData) new RenderWidgetAction().execute(input).getWidgetData();
         assertThat(queue.getRows()).extracting(row -> row.get("Exception")).containsExactly("Validation: Synthetic 0", "Validation: Synthetic 1", "Validation: Synthetic 2");
         return (null);
      });
   }



   /*******************************************************************************
    ** Native label formats retain the vocabulary in record links and pickers.
    *******************************************************************************/
   @Test
   void recordFormatsUseReadableContext() throws Exception
   {
      for(var table : EdiTables.tables(new EdiQBitConfig().withBackendName("memory")))
      {
         String expected = switch(table.getName())
         {
            case "edi_interchange" -> "INBOUND ISA 000001";
            case "edi_functional_group" -> "GS 000001";
            case "edi_exception" -> "VALIDATION: Synthetic summary";
            case "edi_inbox" -> "SFTP demo-message";
            case "edi_outbound_message" -> "Demo SFTP SENT";
            case "edi_map", "edi_guide" -> "Retail r2";
            case "edi_as2_mdn" -> "INBOUND MDN <synthetic-message@peer>";
            default -> null;
         };
         if(expected != null)
         {
            QRecord row = new QRecord().withValue("direction", "INBOUND").withValue("controlNumber", "000001").withValue("type", "VALIDATION")
               .withValue("summary", "Synthetic summary").withValue("transportType", "SFTP").withValue("externalId", "demo-message")
               .withValue("fileName", "SYNTHETIC-PRIVATE-FILENAME").withValue("transportId", 1).withValue("status", "SENT").withValue("name", "Retail").withValue("revision", 2)
               .withValue("originalMessageId", "<synthetic-message@peer>");
            row.setDisplayValue("transportId", "Demo SFTP");
            assertThat(QValueFormatter.formatRecordLabel(table, row)).as(table.getName()).isEqualTo(expected);
         }
      }
   }
}
