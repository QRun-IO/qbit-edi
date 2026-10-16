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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.app.EdiSetupStatus;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** The Setup status on the EDI home: the steps in setup order, each with its
 ** state and next step, counting only the rows the runtime would use.
 *******************************************************************************/
class EdiSetupStatusTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** The pure logic: order, states and next steps.
    *******************************************************************************/
   @Test
   void stepsAreInSetupOrder()
   {
      List<EdiSetupStatus.Item> items = EdiSetupStatus.items(new EdiSetupStatus.Counts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 41, 20, 0, 0));
      assertThat(items).extracting(EdiSetupStatus.Item::step).containsExactly("1. Partner", "2. Identities", "3. Agreement", "4. Agreement documents", "5. Transport",
         "Certificates", "Guides and code lists", "Maps");
      assertThat(items.subList(0, 5)).allSatisfy(item -> assertThat(item.state()).isEqualTo(EdiSetupStatus.State.TODO));
      assertThat(items.get(5).state()).isEqualTo(EdiSetupStatus.State.OPTIONAL);
      assertThat(items.get(6).state()).isEqualTo(EdiSetupStatus.State.BUILT_IN);
      assertThat(items.get(7).state()).isEqualTo(EdiSetupStatus.State.OPTIONAL);
      assertThat(items.get(7).nextStep()).contains("Install sample maps");

      items = EdiSetupStatus.items(new EdiSetupStatus.Counts(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0));
      assertThat(items.get(1).nextStep()).contains("partner's");
      assertThat(items.get(6).state()).isEqualTo(EdiSetupStatus.State.TODO);
      assertThat(items.get(6).nextStep()).contains("EdiRuntime.initialize()");
      assertThat(items.get(7).state()).as("maps exist but none active").isEqualTo(EdiSetupStatus.State.TODO);

      items = EdiSetupStatus.items(new EdiSetupStatus.Counts(2, 1, 2, 2, 1, 3, 3, 1, 1, 1, 41, 20, 5, 2));
      assertThat(items).allSatisfy(item -> assertThat(item.state()).isIn(EdiSetupStatus.State.DONE, EdiSetupStatus.State.BUILT_IN));

      items = EdiSetupStatus.items(new EdiSetupStatus.Counts(1, 1, 1, 2, 0, 1, 1, 1, 1, 0, 41, 20, 0, 0));
      assertThat(items.get(2).state()).as("agreements exist but none is active, so the runtime ignores them").isEqualTo(EdiSetupStatus.State.TODO);

      items = EdiSetupStatus.items(new EdiSetupStatus.Counts(null, null, null, null, null, null, null, null, null, null, null, null, null, null));
      assertThat(items).allSatisfy(item -> assertThat(item.state()).isEqualTo(EdiSetupStatus.State.UNKNOWN));
      assertThat(EdiSetupStatus.State.UNKNOWN.label()).as("a failed count is never shown as a number").isEqualTo("Unavailable");
   }

   /*******************************************************************************
    ** The home widget moves from "needs setup" to "set up" as the rows are added.
    *******************************************************************************/
   @Test
   void homeWidgetFollowsTheData() throws Exception
   {
      QInstance instance = H2Support.create();
      EdiRuntime.current().initialize();
      assertThat(instance.getWidget("edi_home_setup")).isNotNull();
      assertThat(instance.getApp(EdiRuntime.current().config().getAppName()).getWidgets()).as("setup is for the Admin home").doesNotContain("edi_home_setup");
      assertThat(com.kingsrook.qbits.edi.app.EdiHome.ADMIN_WIDGET_NAMES).contains("edi_home_setup");

      Map<String, String> status = status(instance);
      assertThat(status).containsKeys("1. Partner", "5. Transport");
      assertThat(status.get("1. Partner")).startsWith("Needs setup");
      assertThat(status.get("2. Identities")).startsWith("Needs setup");
      assertThat(status.get("Guides and code lists")).startsWith("Built in: 41 guides");
      assertThat(status.get("Maps")).startsWith("Optional");

      SystemWrites.call(() ->
      {
         QRecord partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", "Synthetic Retailer").withValue("code", "SYNTH"));
         SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "QQQTEST")
            .withValue("applicationCode", "QQQTEST").withValue("isActive", true));
         SystemWrites.insert("edi_identity", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("standard", "X12").withValue("interchangeQualifier", "ZZ")
            .withValue("interchangeId", "SYNTHETIC").withValue("applicationCode", "SYNTHETIC").withValue("isActive", false));
         return (null);
      });
      status = status(instance);
      assertThat(status.get("1. Partner")).startsWith("Set up");
      assertThat(status.get("2. Identities")).as("the partner's identity is inactive, so the runtime would ignore it").startsWith("Needs setup: Yours is set; no partner has one");
      assertThat(status.get("3. Agreement")).startsWith("Needs setup");
   }

   /*******************************************************************************
    ** Render the widget and return step to "state: detail".
    *******************************************************************************/
   private Map<String, String> status(QInstance instance) throws Exception
   {
      var data = (TableData) new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(instance.getWidget("edi_home_setup"))).getWidgetData();
      Map<String, String> status = new LinkedHashMap<>();
      data.getRows().forEach(row -> status.put(String.valueOf(row.get("Step")), String.valueOf(row.get("Status"))));
      return (status);
   }
}
