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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

   static
   {
      LABELS.put("in_today", "Inbound today");
      LABELS.put("out_today", "Outbound today");
      LABELS.put("in_week", "Inbound over 7 days");
      LABELS.put("out_week", "Outbound over 7 days");
      LABELS.put("exceptions", "Open exceptions");
      LABELS.put("overdue", "Overdue acknowledgements");
      LABELS.put("dead", "Dead letters");
      LABELS.put("certificates", "Certificates expiring within 30 days");
      LABELS.put("backlog", "Inbox backlog");
      LABELS.put("oldest", "Oldest pending age (seconds)");
      LABELS.put("storage", "Payload storage");
      LABELS.put("secured", "Secured-data integration");
      LABELS.put("external", "External code-set validation");
      LABELS.put("volume", "Volume by partner and document over 7 days");
      LABELS.put("acks", "Overdue acknowledgement queue");
      LABELS.put("errors", "Recent errors (24 hours)");
      LABELS.put("queue", "Exception queue");
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
         .withType((name.equals("queue") ? WidgetType.CHILD_RECORD_LIST : List.of("volume", "acks", "errors").contains(name) ? WidgetType.TABLE : WidgetType.STATISTICS).getType())
         .withGridColumns(List.of("volume", "acks", "errors", "queue").contains(name) ? 12 : 3).withCodeReference(new QCodeReference(EdiHome.class))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))));
      return (widgets);
   }

   /*******************************************************************************
    ** Direct rendering also enforces table permissions, including nested process calls.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      String name = input.getWidgetMetaData().getName().substring("edi_home_".length());
      Instant now = EdiRuntime.current().config().getClock().instant();
      String table = "edi_transaction";
      QQueryFilter filter = new QQueryFilter();
      if(name.startsWith("in_") || name.startsWith("out_"))
      {
         filter.withCriteria("direction", QCriteriaOperator.EQUALS, name.startsWith("in_") ? "INBOUND" : "OUTBOUND")
            .withCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, name.endsWith("today") ? now.truncatedTo(ChronoUnit.DAYS) : now.minus(7, ChronoUnit.DAYS));
      }
      else if(List.of("exceptions", "queue", "errors").contains(name))
      {
         table = "edi_exception";
         if(!name.equals("errors"))
         {
            filter.withCriteria("status", QCriteriaOperator.IN, "OPEN", "IN_PROGRESS");
         }
         if(name.equals("errors"))
         {
            filter.withCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(1, ChronoUnit.DAYS));
         }
      }
      else if(List.of("overdue", "acks").contains(name))
      {
         table = "edi_functional_group";
         filter.withCriteria("ackStatus", QCriteriaOperator.EQUALS, "PENDING").withCriteria("ackDueAt", QCriteriaOperator.LESS_THAN, now);
      }
      else if(name.equals("certificates"))
      {
         table = "edi_certificate";
         filter.withCriteria("notAfter", QCriteriaOperator.LESS_THAN_OR_EQUALS, now.plus(30, ChronoUnit.DAYS)).withCriteria("status", QCriteriaOperator.EQUALS, "ACTIVE");
      }
      else if(List.of("dead", "backlog", "oldest").contains(name))
      {
         table = "edi_inbox";
         filter.withCriteria("status", QCriteriaOperator.IN, name.equals("dead") ? List.of("DEAD_LETTER") : List.of("PENDING", "PROCESSING", "RETRY_WAIT"));
      }
      if(name.equals("secured"))
      {
         boolean present = com.kingsrook.qqq.backend.core.context.QContext.getQInstance().getQBits().values().stream().anyMatch(q -> "qbit-secured-data".equals(q.getArtifactId()));
         return (new RenderWidgetOutput(new StatisticsData().withCount(present ? "Installed" : "Not installed").withCountContext("PHI field protection depends on host configuration")));
      }
      if(name.equals("external"))
      {
         return (new RenderWidgetOutput(new StatisticsData().withCount("Not configured").withCountContext("Baseline and configured guide code lists only")));
      }
      if(name.equals("storage"))
      {
         return (new RenderWidgetOutput(new StatisticsData().withCount(EdiRuntime.current().config().getPayloadStore()).withCountContext("Encrypted; database fallback is intended for demos and tests")));
      }
      if(name.equals("volume"))
      {
         filter.withCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(7, ChronoUnit.DAYS));
         Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
         for(QRecord row : query(table, filter))
         {
            String key = row.getValueString("partnerId") + "/" + row.getValueString("transactionSet");
            Map<String, Object> counts = groups.computeIfAbsent(key, k -> new LinkedHashMap<>(Map.of("partner", row.getValueString("partnerId") == null ? "Unrouted" : row.getValueString("partnerId"),
               "document", row.getValueString("transactionSet"), "in", 0, "out", 0, "rejected", 0)));
            String direction = "INBOUND".equals(row.getValueString("direction")) ? "in" : "out";
            counts.put(direction, (Integer) counts.get(direction) + 1);
            if(List.of("INVALID", "REJECTED").contains(row.getValueString("status")))
            {
               counts.put("rejected", (Integer) counts.get("rejected") + 1);
            }
         }
         return (new RenderWidgetOutput(tableData(List.of("partner", "document", "in", "out", "rejected"), new ArrayList<>(groups.values())).withLinkURL(link(table, filter)).withLinkText("View documents")));
      }
      if(name.equals("queue"))
      {
         CountInput count = new CountInput(table).withFilter(filter);
         count.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         Integer total = new CountAction().execute(count).getCount();
         QueryInput query = new QueryInput(table).withFilter(filter.withOrderBy(new QFilterOrderBy("createDate")).withLimit(20));
         query.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         return (new RenderWidgetOutput(new com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData("Exception queue", new QueryAction().execute(query),
            com.kingsrook.qqq.backend.core.context.QContext.getQInstance().getTable(table), "/app/" + table, link(table, filter), total).withCanAddChildRecord(false)));
      }
      if(List.of("errors", "acks").contains(name))
      {
         List<String> columns = name.equals("acks") ? List.of("partner", "group", "sent", "due", "ageSeconds") : List.of("id", "type", "status", "createDate");
         List<Map<String, Object>> rows = new ArrayList<>();
         for(QRecord row : query(table, filter.withOrderBy(new QFilterOrderBy("createDate")).withLimit(20)))
         {
            Map<String, Object> values = new LinkedHashMap<>();
            if(name.equals("acks"))
            {
               values.put("group", row.getValue("controlNumber"));
               values.put("due", row.getValue("ackDueAt"));
               var interchanges = query("edi_interchange", new QQueryFilter().withCriteria("id", QCriteriaOperator.EQUALS, row.getValue("interchangeId")));
               if(!interchanges.isEmpty())
               {
                  QRecord interchange = interchanges.getFirst();
                  values.put("partner", interchange.getValue("partnerId"));
                  if(interchange.getValue("outboundMessageId") != null)
                  {
                     var messages = query("edi_outbound_message", new QQueryFilter().withCriteria("id", QCriteriaOperator.EQUALS, interchange.getValue("outboundMessageId")));
                     if(!messages.isEmpty())
                     {
                        Instant sent = messages.getFirst().getValueInstant("sentAt");
                        values.put("sent", sent);
                        values.put("ageSeconds", sent == null ? null : Math.max(0L, Duration.between(sent, now).getSeconds()));
                     }
                  }
               }
            }
            else
            {
               columns.forEach(c -> values.put(c, row.getValue(c)));
            }
            rows.add(values);
         }
         return (new RenderWidgetOutput(tableData(columns, rows).withLinkURL(link(table, filter)).withLinkText("View records and actions")));
      }
      Serializable count;
      if(name.equals("oldest"))
      {
         var rows = query(table, filter.withOrderBy(new QFilterOrderBy("receivedAt")).withLimit(1));
         count = rows.isEmpty() ? 0L : Math.max(0L, Duration.between(rows.getFirst().getValueInstant("receivedAt"), now).getSeconds());
      }
      else
      {
         CountInput countInput = new CountInput(table).withFilter(filter);
         countInput.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         count = new CountAction().execute(countInput).getCount();
      }
      return (new RenderWidgetOutput(new StatisticsData().withCount(count).withCountURL(link(table, filter))));
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
