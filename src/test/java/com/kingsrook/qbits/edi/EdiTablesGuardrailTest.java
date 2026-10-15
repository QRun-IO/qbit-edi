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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.metadata.EdiHelp;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Guardrail (owner requirement 2026-10-14: "all tables should be populated and
 ** the qbit should be fully using them"). For every edi_* table:
 ** - runtime code (not just its model, metadata or home screen) reads or
 **   writes it;
 ** - it is either seeded by the qbit at startup, or listed here with why it
 **   starts empty;
 ** - it has bundle help that says what it is for and how its rows get created
 **   (shown on the table screen and when it is empty).
 ** A new table fails this test until it is wired, seeded or explained.
 *******************************************************************************/
class EdiTablesGuardrailTest
{
   private static final Path MAIN = Path.of("src", "main", "java", "com", "kingsrook", "qbits", "edi");

   /*******************************************************************************
    ** Tables the qbit seeds itself (EdiRuntime.initialize, idempotent).
    *******************************************************************************/
   static final Map<String, String> SEEDED = Map.of(
      "edi_guide", "BuiltInGuideLoader: 41 built-in guides",
      "edi_guide_segment", "with each built-in guide",
      "edi_guide_element", "with each built-in guide",
      "edi_guide_rule", "with each built-in guide",
      "edi_code_list", "with the built-in guides",
      "edi_code_list_value", "with the built-in guides");

   /*******************************************************************************
    ** Tables that legitimately start empty, and where their rows come from.
    *******************************************************************************/
   static final Map<String, String> STARTS_EMPTY = Map.ofEntries(
      Map.entry("edi_partner", "host data: the trading partners"),
      Map.entry("edi_identity", "host data: your ISA/UNB identity and each partner's"),
      Map.entry("edi_agreement", "host data: the agreement per partner"),
      Map.entry("edi_agreement_document", "host data: transaction sets per agreement"),
      Map.entry("edi_transport", "host connection config: addresses and secret variable names"),
      Map.entry("edi_certificate", "host data: AS2 certificates (optional)"),
      Map.entry("edi_map", "host data; the 'Install sample maps' action adds bundled examples as inactive, unbound drafts"),
      Map.entry("edi_map_rule", "with a map; 'Install sample maps' adds the rules of each sample"),
      Map.entry("edi_map_sample", "user content: synthetic documents uploaded with Preview Map"),
      Map.entry("edi_control_number", "runtime: allocated on the first send for a scope"),
      Map.entry("edi_inbox", "runtime: every received document"),
      Map.entry("edi_payload", "runtime: raw bytes, only when payload storage is DATABASE"),
      Map.entry("edi_interchange", "runtime: every envelope sent or received"),
      Map.entry("edi_functional_group", "runtime: every group"),
      Map.entry("edi_transaction", "runtime: every document"),
      Map.entry("edi_transaction_error", "runtime: validation findings"),
      Map.entry("edi_exception", "runtime: failures that need a person"),
      Map.entry("edi_event", "audit log: written at each processing step (EdiJournal), shown on the interchange record"),
      Map.entry("edi_as2_mdn", "runtime: AS2 receipts sent and received; read to ignore duplicates and replay a sent receipt"),
      Map.entry("edi_outbound_request", "runtime: a host record queued to be sent"),
      Map.entry("edi_outbound_message", "runtime: the outbound queue"));

   private QInstance instance;

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Every table is used by runtime code.
    *******************************************************************************/
   @Test
   void everyTableIsUsedAtRuntime() throws Exception
   {
      List<String> runtimeSource = new ArrayList<>();
      try(Stream<Path> files = Files.walk(MAIN))
      {
         files.filter(p -> p.toString().endsWith(".java") && !p.startsWith(MAIN.resolve("model")) && !p.startsWith(MAIN.resolve("metadata")) && !p.startsWith(MAIN.resolve("app")))
            .forEach(p -> runtimeSource.add(read(p)));
      }
      for(String table : tables())
      {
         Pattern use = Pattern.compile("\\b" + table + "\\b");
         assertThat(runtimeSource.stream().anyMatch(source -> use.matcher(source).find()))
            .as(table + " must be read or written by runtime code, not only declared").isTrue();
      }
   }

   /*******************************************************************************
    ** Every table is seeded or explained.
    *******************************************************************************/
   @Test
   void everyTableIsSeededOrExplained() throws Exception
   {
      for(String table : tables())
      {
         assertThat(SEEDED.containsKey(table) || STARTS_EMPTY.containsKey(table)).as(table + " must be seeded or listed in STARTS_EMPTY").isTrue();
         assertThat(SEEDED.containsKey(table) && STARTS_EMPTY.containsKey(table)).as(table + " cannot be both").isFalse();
      }
      assertThat(tables()).containsAll(STARTS_EMPTY.keySet()).containsAll(SEEDED.keySet());
   }

   /*******************************************************************************
    ** The built-in guides fill the seeded tables, a second start adds nothing, and
    ** an administrator's edit survives.
    *******************************************************************************/
   @Test
   void seedIsInsertOnly() throws Exception
   {
      H2Support.create();
      EdiRuntime.current().initialize();
      Map<String, Integer> first = counts();
      assertThat(first.get("edi_guide")).isEqualTo(BuiltInGuideLoader.RESOURCES.size()).isEqualTo(41);
      for(String table : SEEDED.keySet())
      {
         assertThat(first.get(table)).as(table + " is seeded").isGreaterThan(0);
      }
      QRecord guide = SystemWrites.query("edi_guide", new QQueryFilter()).getFirst();
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_guide", new QRecord().withValue("id", guide.getValue("id")).withValue("description", "Edited by an administrator"));
         return (null);
      });

      EdiRuntime.current().initialize();
      assertThat(counts()).isEqualTo(first);
      assertThat(EdiRecords.query("edi_guide", EdiRecords.equal("id", guide.getValue("id"))).getFirst().getValueString("description")).isEqualTo("Edited by an administrator");
   }

   /*******************************************************************************
    ** Every table and the app have bundle help that says how rows get created.
    *******************************************************************************/
   @Test
   void everyTableHasEmptyStateHelp() throws Exception
   {
      QInstance instance = H2Support.create();
      assertThat(EdiHelp.entries()).allSatisfy((key, entry) -> assertThat(entry.path("summary").asText().length()).as(key).isLessThanOrEqualTo(120));
      for(String table : tables())
      {
         List<QHelpContent> help = instance.getTable(table).getHelpContent() == null ? null : instance.getTable(table).getHelpContent().get(EdiHelp.SLOT);
         assertThat(help).as(table + " table help").isNotEmpty();
         assertThat(help.get(0).getContent()).as(table + " help says how rows get created").contains("How rows get created");
      }
      assertThat(instance.getApp(EdiRuntime.current().config().getAppName()).getHelpContent()).containsKey(EdiHelp.SLOT);
      assertThat(EdiHelp.entries().get("table:edi_map;slot:help").path("content").asText()).contains("Install sample maps");
      assertThat(EdiHelp.entries().get("app:edi;slot:help").path("content").asText().indexOf("Partner")).isLessThan(EdiHelp.entries().get("app:edi;slot:help").path("content").asText().indexOf("Transport"));
   }

   /*******************************************************************************
    ** Help a host put in the slot first is never replaced by the bundle.
    *******************************************************************************/
   @Test
   void hostHelpIsKept() throws Exception
   {
      QInstance instance = H2Support.create();
      String    appName  = EdiRuntime.current().config().getAppName();
      QHelpContent host  = new QHelpContent().withContentAsMarkdown("Host help for partners");
      instance.getTable("edi_partner").getHelpContent().put(EdiHelp.SLOT, new ArrayList<>(List.of(host)));
      instance.getApp(appName).getHelpContent().put(EdiHelp.SLOT, new ArrayList<>(List.of(host)));
      EdiHelp.apply(instance, appName);
      assertThat(instance.getTable("edi_partner").getHelpContent().get(EdiHelp.SLOT)).containsExactly(host);
      assertThat(instance.getApp(appName).getHelpContent().get(EdiHelp.SLOT)).containsExactly(host);
      assertThat(instance.getTable("edi_identity").getHelpContent().get(EdiHelp.SLOT).get(0).getContent()).contains("How rows get created");
   }

   /*******************************************************************************
    ** Row counts of the seeded tables.
    *******************************************************************************/
   private Map<String, Integer> counts() throws Exception
   {
      Map<String, Integer> counts = new java.util.TreeMap<>();
      for(String table : SEEDED.keySet())
      {
         counts.put(table, SystemWrites.count(table, new QQueryFilter()));
      }
      return (counts);
   }

   /*******************************************************************************
    ** The qbit's tables (the preview download backend table is not data).
    *******************************************************************************/
   private List<String> tables() throws Exception
   {
      if(instance == null)
      {
         instance = H2Support.create();
      }
      List<String> tables = instance.getTables().values().stream().map(QTableMetaData::getName)
         .filter(name -> name.startsWith("edi_") && !name.equals("edi_preview_download")).sorted().toList();
      assertThat(tables).hasSize(27);
      return (tables);
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   private static String read(Path path)
   {
      try
      {
         return (Files.readString(path, StandardCharsets.UTF_8));
      }
      catch(Exception e)
      {
         throw (new IllegalStateException(e));
      }
   }
}
