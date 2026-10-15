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

package com.kingsrook.qbits.edi.metadata;


import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Applies help/qbit-edi.yaml (the QQQ help-bundle format) to the Edi app
 ** and its edi_* tables: what each table is for and how its rows get created,
 ** shown on the table screen and when it is empty.
 ** Fallback for hosts without the helpContent table. Hosts with that table use
 ** the core bundle registry and startup sync, including HOST and USER overrides.
 *******************************************************************************/
public final class EdiHelp
{
   public static final String RESOURCE = "/help/qbit-edi.yaml";
   public static final String SLOT     = "help";
   public static final String APP_KEY  = "edi";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private EdiHelp()
   {
   }



   /*******************************************************************************
    ** The bundle's entries by key, validated (fails startup if malformed).
    *******************************************************************************/
   public static Map<String, JsonNode> entries() throws QException
   {
      Map<String, JsonNode> entries = new LinkedHashMap<>();
      ObjectMapper mapper = new ObjectMapper();
      for(var entry : HelpBundles.load("help/qbit-edi.yaml", EdiHelp.class.getClassLoader()).getEntries())
      {
         entries.put(entry.getKey(), mapper.valueToTree(entry));
      }
      return (entries);
   }



   /*******************************************************************************
    ** Apply the help to the app (by its configured name) and every edi_* table. A
    ** host's own help on a table is kept (the bundle only fills the slot).
    *******************************************************************************/
   public static void apply(QInstance qInstance, String appName) throws QException
   {
      Map<String, JsonNode> entries = entries();
      for(QTableMetaData table : qInstance.getTables().values())
      {
         JsonNode entry = entries.get("table:" + table.getName() + ";slot:" + SLOT);
         if(entry != null && (table.getHelpContent() == null || !table.getHelpContent().containsKey(SLOT)))
         {
            table.withHelpContent(SLOT, content(entry));
         }
      }
      QAppMetaData app      = qInstance.getApp(appName);
      JsonNode     appEntry = entries.get("app:" + APP_KEY + ";slot:" + SLOT);
      if(app != null && appEntry != null && (app.getHelpContent() == null || !app.getHelpContent().containsKey(SLOT)))
      {
         app.withHelpContent(SLOT, content(appEntry));
      }
   }



   /*******************************************************************************
    ** Preserve the tooltip summary separately from the Markdown long help.
    *******************************************************************************/
   static QHelpContent content(JsonNode entry)
   {
      return (new QHelpContent().withSummary(entry.path("summary").asText().strip()).withContentAsMarkdown(entry.path("content").asText().strip()));
   }

}
