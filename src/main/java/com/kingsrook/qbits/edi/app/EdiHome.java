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

package com.kingsrook.qbits.edi.app;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiQBitConfig;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Typed EDI home. Every data query explicitly enforces the viewing user's access.
 *******************************************************************************/
public class EdiHome extends AbstractWidgetRenderer
{
   public static final List<String> ACTIVITY = List.of("edi_transaction", "edi_interchange", "edi_exception");
   public static final List<String> SETUP = List.of("edi_partner", "edi_agreement", "edi_transport", "edi_certificate", "edi_guide", "edi_code_list", "edi_map");
   private static final Map<String, String> LABELS = new LinkedHashMap<>();
   private static final Map<String, String> DOCUMENT_NAMES = new HashMap<>();
   private static final List<String> TABLES = List.of("setup", "queue", "acks", "errors", "volume", "system");

   static
   {
      DOCUMENT_NAMES.putAll(Map.ofEntries(Map.entry("810", "Invoice"), Map.entry("846", "Inventory Advice"), Map.entry("850", "Purchase Order"),
         Map.entry("855", "PO Acknowledgment"), Map.entry("856", "Ship Notice"), Map.entry("860", "PO Change"), Map.entry("940", "Warehouse Shipping Order"),
         Map.entry("945", "Warehouse Shipping Advice"), Map.entry("214", "Shipment Status"), Map.entry("204", "Load Tender"), Map.entry("837", "Claim"),
         Map.entry("835", "Remittance"), Map.entry("270", "Eligibility Inquiry"), Map.entry("271", "Eligibility Response"), Map.entry("276", "Claim Status Inquiry"),
         Map.entry("277", "Claim Status Response"), Map.entry("834", "Enrollment"), Map.entry("997", "Functional Ack"), Map.entry("999", "Implementation Ack"),
         Map.entry("TA1", "Interchange Ack")));
      LABELS.put("in_today", "Inbound today");
      LABELS.put("out_today", "Outbound today");
      LABELS.put("exceptions", "Open exceptions");
      LABELS.put("overdue", "Overdue acks");
      LABELS.put("dead", "Dead letters");
      LABELS.put("certificates", "Certificates expiring (30 days)");
      LABELS.put("setup", "Setup status");
      LABELS.put("queue", "Exception queue");
      LABELS.put("acks", "Overdue acknowledgements");
      LABELS.put("errors", "Recent errors (24 hours)");
      LABELS.put("volume", "Volume by partner and document (7 days)");
      LABELS.put("system", "System");
   }

   /*******************************************************************************
    ** Activity and setup only; all other tables remain parent record children.
    *******************************************************************************/
   public static QAppMetaData app(EdiQBitConfig config, List<QTableMetaData> tables)
   {
      QAppMetaData app = new QAppMetaData().withName(config.getAppName()).withLabel(config.getAppLabel()).withIcon(new QIcon("swap_horiz"))
         .withWidgets(LABELS.keySet().stream().map(n -> "edi_home_" + n).toList())
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withSection(new QAppSection().withName("activity").withLabel("Activity").withTables(ACTIVITY))
         .withSection(new QAppSection().withName("setup").withLabel("Setup").withTables(SETUP));
      app.setParentAppName(config.getParentAppName());
      tables.stream().filter(t -> ACTIVITY.contains(t.getName()) || SETUP.contains(t.getName())).forEach(app::withChild);
      return (app);
   }

   /*******************************************************************************
    ** RC.5 typed statistics and tables; no raw markup in metadata or widget data.
    *******************************************************************************/
   public static List<QWidgetMetaData> widgets()
   {
      List<QWidgetMetaData> widgets = new ArrayList<>();
      LABELS.forEach((name, label) -> widgets.add(new QWidgetMetaData().withName("edi_home_" + name).withLabel(label)
         .withType((TABLES.contains(name) ? WidgetType.TABLE : WidgetType.STATISTICS).getType())
         .withGridColumns(TABLES.contains(name) ? 12 : 2).withCodeReference(new QCodeReference(EdiHome.class))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))));
      return (widgets);
   }

   /*******************************************************************************
    ** Direct rendering also enforces table permissions, including nested process calls.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      String  name = input.getWidgetMetaData().getName().substring("edi_home_".length());
      Instant now  = EdiRuntime.current().config().getClock().instant();
      return (switch(name)
      {
         case "in_today", "out_today" -> count("edi_transaction", new QQueryFilter()
            .withCriteria("direction", QCriteriaOperator.EQUALS, name.startsWith("in_") ? "INBOUND" : "OUTBOUND")
            .withCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.truncatedTo(ChronoUnit.DAYS)));
         case "exceptions" -> count("edi_exception", openExceptions());
         case "overdue" -> count("edi_functional_group", overdueAcks(now));
         case "dead" -> count("edi_exception", openExceptions().withCriteria("type", QCriteriaOperator.IN, "DEAD_LETTER", "TRANSPORT"));
         case "certificates" -> count("edi_certificate", new QQueryFilter().withCriteria("notAfter", QCriteriaOperator.LESS_THAN_OR_EQUALS, now.plus(30, ChronoUnit.DAYS))
            .withCriteria("status", QCriteriaOperator.EQUALS, "ACTIVE"));
         case "setup" -> setup();
         case "queue" -> queue(now);
         case "acks" -> acks(now);
         case "errors" -> errors(now);
         case "volume" -> volume(now);
         case "system" -> system(now);
         default -> throw new QException("Unknown EDI home widget " + name);
      });
   }



   /*******************************************************************************
    ** A counted statistic linking to the same filter.
    *******************************************************************************/
   private static RenderWidgetOutput count(String table, QQueryFilter filter) throws QException
   {
      return (new RenderWidgetOutput(new StatisticsData().withCount(countOf(table, filter)).withCountURL(link(table, filter))));
   }



   /*******************************************************************************
    ** Count with enforcement.
    *******************************************************************************/
   private static Integer countOf(String table, QQueryFilter filter) throws QException
   {
      CountInput countInput = new CountInput(table).withFilter(filter);
      countInput.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new CountAction().execute(countInput).getCount());
   }



   /*******************************************************************************
    ** Open and in-progress exceptions.
    *******************************************************************************/
   private static QQueryFilter openExceptions()
   {
      return (new QQueryFilter().withCriteria("status", QCriteriaOperator.IN, "OPEN", "IN_PROGRESS"));
   }



   /*******************************************************************************
    ** Outbound groups whose expected acknowledgement is past due.
    *******************************************************************************/
   private static QQueryFilter overdueAcks(Instant now)
   {
      return (new QQueryFilter().withCriteria("ackStatus", QCriteriaOperator.EQUALS, "PENDING").withCriteria("ackDueAt", QCriteriaOperator.LESS_THAN, now));
   }



   /*******************************************************************************
    ** The setup steps in order, with their status and the next step. Counts are
    ** taken as the viewer; a table the viewer cannot read shows as not visible.
    *******************************************************************************/
   private static RenderWidgetOutput setup() throws QException
   {
      QQueryFilter active = new QQueryFilter().withCriteria("isActive", QCriteriaOperator.EQUALS, true);
      EdiSetupStatus.Counts counts = new EdiSetupStatus.Counts(safeCount("edi_partner", new QQueryFilter()),
         safeCount("edi_identity", new QQueryFilter().withCriteria("isActive", QCriteriaOperator.EQUALS, true).withCriteria("partnerId", QCriteriaOperator.IS_BLANK)),
         safeCount("edi_identity", new QQueryFilter().withCriteria("isActive", QCriteriaOperator.EQUALS, true).withCriteria("partnerId", QCriteriaOperator.IS_NOT_BLANK)),
         safeCount("edi_agreement", new QQueryFilter()), safeCount("edi_agreement", active),
         safeCount("edi_agreement_document", new QQueryFilter()), safeCount("edi_agreement_document", active),
         safeCount("edi_transport", new QQueryFilter()), safeCount("edi_transport", active),
         safeCount("edi_certificate", new QQueryFilter()), safeCount("edi_guide", new QQueryFilter()), safeCount("edi_code_list", new QQueryFilter()),
         safeCount("edi_map", new QQueryFilter()), safeCount("edi_map", new QQueryFilter().withCriteria("status", QCriteriaOperator.EQUALS, "ACTIVE")));
      List<Map<String, Object>> rows = new ArrayList<>();
      for(EdiSetupStatus.Item item : EdiSetupStatus.items(counts))
      {
         rows.add(row("Step", item.step(), "Status", item.state().label() + (item.detail().isEmpty() ? "" : ": " + item.detail()), "Next step", item.nextStep().isEmpty() ? "Nothing to do." : item.nextStep()));
      }
      return (table(List.of("Step", "Status", "Next step"), rows, "edi_partner", new QQueryFilter(), "Open partners"));
   }



   /*******************************************************************************
    ** A count the viewer may not be allowed to read: -1 instead of an error.
    *******************************************************************************/
   private static int safeCount(String table, QQueryFilter filter)
   {
      try
      {
         return (countOf(table, filter));
      }
      catch(QException | RuntimeException e)
      {
         return (-1);
      }
   }



   /*******************************************************************************
    ** Open exceptions, oldest first, in four narrow columns.
    *******************************************************************************/
   private static RenderWidgetOutput queue(Instant now) throws QException
   {
      Map<Integer, String>      partners = partnerNames();
      List<Map<String, Object>> rows     = new ArrayList<>();
      for(QRecord row : query("edi_exception", openExceptions().withOrderBy(new QFilterOrderBy("createDate", true)).withOrderBy(new QFilterOrderBy("id", true)).withLimit(20)))
      {
         rows.add(row("Exception", plain(row.getValueString("type")) + ": " + row.getValueString("summary"),
            "Partner", partnerName(partners, row.getValueInteger("partnerId")), "Age", humanize(Duration.between(row.getValueInstant("createDate"), now))));
      }
      return (table(List.of("Exception", "Partner", "Age"), rows, "edi_exception", openExceptions(), "View all exceptions"));
   }



   /*******************************************************************************
    ** Overdue acknowledgements with partner names and humanized ages.
    *******************************************************************************/
   private static RenderWidgetOutput acks(Instant now) throws QException
   {
      Map<Integer, String>      partners = partnerNames();
      List<Map<String, Object>> rows     = new ArrayList<>();
      for(QRecord group : query("edi_functional_group", overdueAcks(now).withOrderBy(new QFilterOrderBy("ackDueAt", true)).withLimit(20)))
      {
         Integer       partnerId    = null;
         Instant       sent         = null;
         List<QRecord> interchanges = query("edi_interchange", new QQueryFilter().withCriteria("id", QCriteriaOperator.EQUALS, group.getValue("interchangeId")));
         if(!interchanges.isEmpty())
         {
            partnerId = interchanges.getFirst().getValueInteger("partnerId");
            Serializable messageId = interchanges.getFirst().getValue("outboundMessageId");
            if(messageId != null)
            {
               List<QRecord> messages = query("edi_outbound_message", new QQueryFilter().withCriteria("id", QCriteriaOperator.EQUALS, messageId));
               sent = messages.isEmpty() ? null : messages.getFirst().getValueInstant("sentAt");
            }
         }
         Instant due = group.getValueInstant("ackDueAt");
         rows.add(row("Partner", partnerName(partners, partnerId), "Group", "GS " + group.getValueString("controlNumber"),
            "Sent", sent == null ? "Not sent" : humanize(Duration.between(sent, now)) + " ago", "Overdue by", due == null ? "" : humanize(Duration.between(due, now))));
      }
      return (table(List.of("Partner", "Group", "Sent", "Overdue by"), rows, "edi_functional_group", overdueAcks(now), "View functional groups"));
   }



   /*******************************************************************************
    ** Exceptions raised in the last day, newest first.
    *******************************************************************************/
   private static RenderWidgetOutput errors(Instant now) throws QException
   {
      Map<Integer, String>      partners = partnerNames();
      List<Map<String, Object>> rows     = new ArrayList<>();
      for(QRecord row : query("edi_exception", lastDay(now).withOrderBy(new QFilterOrderBy("createDate", false)).withOrderBy(new QFilterOrderBy("id", false)).withLimit(20)))
      {
         rows.add(row("Error", plain(row.getValueString("type")) + ": " + row.getValueString("summary"),
            "Partner", partnerName(partners, row.getValueInteger("partnerId")), "When", humanize(Duration.between(row.getValueInstant("createDate"), now)) + " ago"));
      }
      return (table(List.of("Error", "Partner", "When"), rows, "edi_exception", lastDay(now), "View exceptions"));
   }



   /*******************************************************************************
    ** Created within the last day.
    *******************************************************************************/
   private static QQueryFilter lastDay(Instant now)
   {
      return (new QQueryFilter().withCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(1, ChronoUnit.DAYS)));
   }



   /*******************************************************************************
    ** Documents by partner and transaction set over seven days, with a total row.
    *******************************************************************************/
   private static RenderWidgetOutput volume(Instant now) throws QException
   {
      Map<Integer, String> partners = partnerNames();
      QQueryFilter         filter   = new QQueryFilter().withCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(7, ChronoUnit.DAYS));
      Map<List<String>, int[]> counts = new TreeMap<>((a, b) -> a.get(0).equals(b.get(0)) ? a.get(1).compareTo(b.get(1)) : a.get(0).compareTo(b.get(0)));
      for(QRecord row : query("edi_transaction", filter))
      {
         int[] c = counts.computeIfAbsent(List.of(partnerName(partners, row.getValueInteger("partnerId")), documentName(row.getValueString("transactionSet"))), k -> new int[3]);
         c["INBOUND".equals(row.getValueString("direction")) ? 0 : 1]++;
         if(List.of("INVALID", "REJECTED").contains(row.getValueString("status")))
         {
            c[2]++;
         }
      }
      List<Map<String, Object>> rows   = new ArrayList<>();
      int[]                     totals = new int[3];
      for(Map.Entry<List<String>, int[]> entry : counts.entrySet())
      {
         int[] c = entry.getValue();
         rows.add(row("Partner", entry.getKey().get(0), "Document", entry.getKey().get(1), "In / out / rejected", c[0] + " / " + c[1] + " / " + c[2]));
         for(int i = 0; i < 3; i++)
         {
            totals[i] += c[i];
         }
      }
      rows.add(row("Partner", "Total", "Document", "", "In / out / rejected", totals[0] + " / " + totals[1] + " / " + totals[2]));
      return (table(List.of("Partner", "Document", "In / out / rejected"), rows, "edi_transaction", filter, "View documents"));
   }



   /*******************************************************************************
    ** Integration and queue state as one compact two-column table.
    *******************************************************************************/
   private static RenderWidgetOutput system(Instant now) throws QException
   {
      boolean       secured   = com.kingsrook.qqq.backend.core.context.QContext.getQInstance().getQBits().values().stream().anyMatch(q -> "qbit-secured-data".equals(q.getArtifactId()));
      String        store     = EdiRuntime.current().config().getPayloadStore();
      List<QRecord> oldestIn  = query("edi_inbox", inboxBacklog().withOrderBy(new QFilterOrderBy("receivedAt", true)).withLimit(1));
      List<QRecord> oldestOut = query("edi_outbound_message", outboundQueue().withOrderBy(new QFilterOrderBy("createDate", true)).withLimit(1));
      List<Map<String, Object>> rows = new ArrayList<>();
      rows.add(row("Item", "Payload storage", "Status", "DATABASE".equals(store) ? "Database, encrypted (demos and tests)" : "Object storage, encrypted"));
      rows.add(row("Item", "Secured data", "Status", secured ? "Installed" : "Not installed: PHI fields are not masked"));
      rows.add(row("Item", "External code sets", "Status", EdiRuntime.current().config().getExternalCodeSetProvider() == null ? "Format only: no provider configured" : "Provider configured"));
      rows.add(row("Item", "Inbound backlog", "Status", countOf("edi_inbox", inboxBacklog())
         + (oldestIn.isEmpty() ? "" : ", oldest " + humanize(Duration.between(oldestIn.getFirst().getValueInstant("receivedAt"), now)))));
      rows.add(row("Item", "Outbound queue", "Status", countOf("edi_outbound_message", outboundQueue())
         + (oldestOut.isEmpty() ? "" : ", oldest " + humanize(Duration.between(oldestOut.getFirst().getValueInstant("createDate"), now)))));
      return (table(List.of("Item", "Status"), rows, "edi_outbound_message", outboundQueue(), "View outbound queue"));
   }



   /*******************************************************************************
    ** Inbox rows not yet processed.
    *******************************************************************************/
   private static QQueryFilter inboxBacklog()
   {
      return (new QQueryFilter().withCriteria("status", QCriteriaOperator.IN, "PENDING", "PROCESSING", "RETRY_WAIT"));
   }



   /*******************************************************************************
    ** Outbound messages waiting to send.
    *******************************************************************************/
   private static QQueryFilter outboundQueue()
   {
      return (new QQueryFilter().withCriteria("status", QCriteriaOperator.IN, "QUEUED", "RETRY_WAIT"));
   }



   /*******************************************************************************
    ** Partner names readable by the viewing user.
    *******************************************************************************/
   private static Map<Integer, String> partnerNames() throws QException
   {
      Map<Integer, String> names = new HashMap<>();
      for(QRecord partner : query("edi_partner", new QQueryFilter()))
      {
         names.put(partner.getValueInteger("id"), partner.getValueString("name"));
      }
      return (names);
   }



   /*******************************************************************************
    ** A partner's name, or a plain placeholder.
    *******************************************************************************/
   private static String partnerName(Map<Integer, String> names, Integer id)
   {
      return (id == null ? "Unrouted" : names.getOrDefault(id, "Partner " + id));
   }



   /*******************************************************************************
    ** A transaction set with its plain name.
    *******************************************************************************/
   static String documentName(String transactionSet)
   {
      String name = DOCUMENT_NAMES.get(transactionSet);
      return (name == null ? String.valueOf(transactionSet) : transactionSet + " " + name);
   }



   /*******************************************************************************
    ** UPPER_SNAKE enum value as a sentence-case label.
    *******************************************************************************/
   static String plain(String value)
   {
      if(value == null || value.isEmpty())
      {
         return ("");
      }
      String lower = value.toLowerCase().replace('_', ' ');
      return (Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
   }



   /*******************************************************************************
    ** Compact human duration: 45 s, 12 m, 3 h 5 m, 2 d 4 h.
    *******************************************************************************/
   static String humanize(Duration duration)
   {
      long seconds = Math.max(0L, duration.getSeconds());
      if(seconds < 60)
      {
         return (seconds + " s");
      }
      long minutes = seconds / 60;
      if(minutes < 60)
      {
         return (minutes + " m");
      }
      long hours = minutes / 60;
      if(hours < 24)
      {
         return (hours + " h" + (minutes % 60 == 0 ? "" : " " + (minutes % 60) + " m"));
      }
      return (hours / 24 + " d" + (hours % 24 == 0 ? "" : " " + (hours % 24) + " h"));
   }



   /*******************************************************************************
    ** An ordered row from alternating keys and values.
    *******************************************************************************/
   private static Map<String, Object> row(Object... keysAndValues)
   {
      Map<String, Object> row = new LinkedHashMap<>();
      for(int i = 0; i < keysAndValues.length; i += 2)
      {
         row.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
      }
      return (row);
   }



   /*******************************************************************************
    ** A typed table with a link to the filtered table.
    *******************************************************************************/
   private static RenderWidgetOutput table(List<String> columns, List<Map<String, Object>> rows, String linkTable, QQueryFilter linkFilter, String linkText) throws QException
   {
      return (new RenderWidgetOutput(tableData(columns, rows).withLinkURL(link(linkTable, linkFilter)).withLinkText(linkText)));
   }



   /*******************************************************************************
    ** Plain cells allow typed rendering and never contain HTML.
    *******************************************************************************/
   private static TableData tableData(List<String> columns, List<Map<String, Object>> rows)
   {
      return (new TableData(null, columns.stream().map(c -> new TableData.Column("text", c, c, null, "left")).toList(), rows));
   }

   /*******************************************************************************
    ** Explicit permission enforcement is required inside widget renderers.
    *******************************************************************************/
   private static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      QueryInput query = new QueryInput(table).withFilter(filter);
      query.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(query).getRecords());
   }

   /*******************************************************************************
    ** Standard filtered-table URL used by statistic and table navigation.
    *******************************************************************************/
   private static String link(String table, QQueryFilter filter) throws QException
   {
      try
      {
         return ("/app/" + table + "?filter=" + URLEncoder.encode(new ObjectMapper().findAndRegisterModules().writeValueAsString(filter), StandardCharsets.UTF_8));
      }
      catch(Exception e)
      {
         throw new QException("Could not create EDI table link");
      }
   }
}
