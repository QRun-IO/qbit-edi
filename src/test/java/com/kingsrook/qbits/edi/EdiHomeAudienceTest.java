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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.edi.app.EdiHome;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.AlertData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** The EDI home is for business users: system widgets live in a separate list
 ** for the host's Admin home, gated to the EDI administrator; a stalled inbound
 ** flow shows as one business item; every widget is permission-protected.
 *******************************************************************************/
class EdiHomeAudienceTest
{
   private static final Instant RECEIVED = Instant.parse("2026-10-16T12:00:00Z");

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** The app's widget list is the business list; system widgets stay defined,
    ** are gated to edi.admin and hidden, and nothing is NOT_PROTECTED.
    *******************************************************************************/
   @Test
   void homeWidgetListExcludesSystemWidgets() throws Exception
   {
      QInstance    instance   = H2Support.create();
      List<String> appWidgets = instance.getApp(EdiRuntime.current().config().getAppName()).getWidgets();
      assertThat(appWidgets).containsExactlyElementsOf(EdiHome.HOME_WIDGET_NAMES).doesNotContainAnyElementsOf(EdiHome.ADMIN_WIDGET_NAMES);
      assertThat(EdiHome.ADMIN_WIDGET_NAMES).containsExactlyInAnyOrder("edi_home_system", "edi_home_dead", "edi_home_certificates", "edi_home_setup");
      assertThat(appWidgets.getFirst()).as("attention comes first").isEqualTo("edi_home_delayed");

      for(String name : EdiHome.ADMIN_WIDGET_NAMES)
      {
         QWidgetMetaData widget = (QWidgetMetaData) instance.getWidget(name);
         assertThat(widget).as(name).isNotNull();
         assertThat(widget.getPermissionRules().getPermissionBaseName()).as(name).isEqualTo("edi.admin");
         assertThat(widget.getGridColumns()).as(name + " is full width in its Admin placement").isEqualTo(12);
      }
      for(String name : EdiHome.HOME_WIDGET_NAMES)
      {
         assertThat(((QWidgetMetaData) instance.getWidget(name)).getPermissionRules().getPermissionBaseName()).as(name).isEqualTo("edi");
      }
      instance.getWidgets().values().stream().filter(w -> w.getName().startsWith("edi_home_")).forEach(widget ->
      {
         assertThat(widget.getPermissionRules().getLevel()).as(widget.getName()).isEqualTo(PermissionLevel.HAS_ACCESS_PERMISSION);
         assertThat(widget.getPermissionRules().getDenyBehavior()).as(widget.getName()).isEqualTo(DenyBehavior.HIDDEN);
      });
   }

   /*******************************************************************************
    ** A business EDI user (edi.hasAccess, no edi.admin) gets no system widget, in
    ** metadata or by rendering; an EDI administrator gets them; a user without
    ** edi.hasAccess gets no home widget at all.
    *******************************************************************************/
   @Test
   void businessUserCannotRenderSystemWidgets() throws Exception
   {
      QInstance instance = H2Support.create();

      QContext.setQSession(readers(new QSession().withPermissions("edi.hasAccess")));
      assertThat(new MetaDataAction().execute(new MetaDataInput()).getWidgets().keySet())
         .containsAll(EdiHome.HOME_WIDGET_NAMES).doesNotContainAnyElementsOf(EdiHome.ADMIN_WIDGET_NAMES);
      for(String name : EdiHome.ADMIN_WIDGET_NAMES)
      {
         assertThatThrownBy(() -> new RenderWidgetAction().execute(render(instance, name)))
            .as(name).isInstanceOf(QPermissionDeniedException.class);
      }
      assertThat(new RenderWidgetAction().execute(render(instance, "edi_home_queue")).getWidgetData()).isInstanceOf(TableData.class);

      QContext.setQSession(readers(new QSession().withPermissions("edi.hasAccess", "edi.admin.hasAccess")));
      assertThat(new MetaDataAction().execute(new MetaDataInput()).getWidgets().keySet()).containsAll(EdiHome.ADMIN_WIDGET_NAMES);
      for(String name : EdiHome.ADMIN_WIDGET_NAMES)
      {
         assertThat(new RenderWidgetAction().execute(render(instance, name)).getWidgetData()).as(name).isNotNull();
      }

      QContext.setQSession(readers(new QSession()));
      assertThat(new MetaDataAction().execute(new MetaDataInput()).getWidgets().keySet()).noneMatch(name -> name.startsWith("edi_home_"));
   }

   /*******************************************************************************
    ** "Inbound documents are delayed" appears only past the threshold, with the
    ** age and a link to the filtered inbound documents; hidden otherwise.
    *******************************************************************************/
   @Test
   void delayedInboundOnlyPastThreshold() throws Exception
   {
      QInstance instance = H2Support.create();
      assertThat(EdiRuntime.current().config().getInboundDelayedAfterMinutes()).as("single-node default").isEqualTo(60);
      QContext.setQSession(readers(new QSession().withPermissions("edi.hasAccess")));
      assertThat(delayed(instance, RECEIVED).getHideWidget()).as("nothing waiting").isTrue();

      SystemWrites.call(() -> SystemWrites.insert("edi_inbox", new QRecord().withValue("transportType", "SFTP").withValue("externalId", "synthetic-delayed")
         .withValue("fileName", "synthetic.edi").withValue("payloadRef", "synthetic").withValue("payloadSha256", "0").withValue("sizeBytes", 1)
         .withValue("receivedAt", RECEIVED).withValue("status", "PENDING").withValue("attempts", 0).withValue("nextAttemptAt", RECEIVED)));

      assertThat(delayed(instance, RECEIVED.plus(30, ChronoUnit.MINUTES)).getHideWidget()).as("within the threshold").isTrue();

      AlertData alert = delayed(instance, RECEIVED.plus(95, ChronoUnit.MINUTES));
      assertThat(alert.getHideWidget()).isFalse();
      assertThat(alert.getAlertType()).isEqualTo(AlertData.AlertType.WARNING);
      assertThat(alert.getHtml()).contains("Inbound documents are delayed", "1 document has waited more than 1 h", "the oldest has waited 1 h 35 m")
         .contains("href=\"/app/edi_inbox?filter=").doesNotContain("-1");
      String filter = URLDecoder.decode(alert.getHtml().replaceAll(".*\\?filter=([^\"]*)\".*", "$1"), StandardCharsets.UTF_8);
      assertThat(filter).contains("receivedAt", "LESS_THAN", "PENDING");

      EdiRuntime.current().config().withInboundDelayedAfterMinutes(120);
      assertThat(delayed(instance, RECEIVED.plus(95, ChronoUnit.MINUTES)).getHideWidget()).as("configurable threshold").isTrue();
      EdiRuntime.current().config().withInboundDelayedAfterMinutes(60);

      QContext.setQSession(new QSession().withPermissions("edi.hasAccess").withSecurityKeyValue("edi.admin", true));
      assertThat(delayed(instance, RECEIVED.plus(95, ChronoUnit.MINUTES)).getHideWidget()).as("viewer cannot read inbound documents").isTrue();
   }

   /*******************************************************************************
    ** A setup count that fails shows "Unavailable", never -1.
    *******************************************************************************/
   @Test
   void setupCountFailureIsUnavailable() throws Exception
   {
      QInstance instance = H2Support.create();
      QSession  session  = readers(new QSession().withPermissions("edi.hasAccess", "edi.admin.hasAccess"));
      session.getPermissions().remove("edi.transport.read");
      QContext.setQSession(session);
      var data = (TableData) new RenderWidgetAction().execute(render(instance, "edi_home_setup")).getWidgetData();
      assertThat(data.getRows()).anySatisfy(row -> assertThat(row).containsEntry("Step", "5. Transport").containsEntry("Status", "Unavailable"));
      assertThat(data.getRows()).allSatisfy(row -> assertThat(row.values()).noneMatch(value -> String.valueOf(value).contains("-1")));
   }

   /*******************************************************************************
    ** Render the delayed-inbound alert at a given time.
    *******************************************************************************/
   private static AlertData delayed(QInstance instance, Instant now) throws Exception
   {
      EdiRuntime.current().config().withClock(Clock.fixed(now, ZoneOffset.UTC));
      return ((AlertData) new RenderWidgetAction().execute(render(instance, "edi_home_delayed")).getWidgetData());
   }

   /*******************************************************************************
    ** A render input that enforces the viewer's permissions, as the UI API does.
    *******************************************************************************/
   private static RenderWidgetInput render(QInstance instance, String widgetName)
   {
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(instance.getWidget(widgetName));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (input);
   }

   /*******************************************************************************
    ** Add read on every EDI table and all-partner row access.
    *******************************************************************************/
   private static QSession readers(QSession session)
   {
      QInstance instance = QContext.getQInstance();
      for(QTableMetaData table : instance.getTables().values())
      {
         if(table.getName().startsWith("edi_"))
         {
            session.withPermissions(PermissionsHelper.getEffectivePermissionBaseName(PermissionsHelper.getEffectivePermissionRules(table, instance), table.getName()) + ".read");
         }
      }
      return (session.withSecurityKeyValue("edi.admin", true));
   }
}
