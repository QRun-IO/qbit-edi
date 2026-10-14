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

package com.kingsrook.qbits.edi.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Standard record-action presentation of the shared sandbox preview.
 *******************************************************************************/
public class MapPreview
{
   /*******************************************************************************
    ** Render the same pipeline as HTTP using ordinary Review/Result lines.
    *******************************************************************************/
   public ArrayList<ProcessSummaryLineInterface> preview(QRecord map, QRecord sample) throws Exception
   {
      Map<String, Object> preview = new MapPreviewService().preview(map, sample, null, "ORIGINAL");
      var json = new ObjectMapper().findAndRegisterModules();
      ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
      lines.add(new ProcessSummaryLine(Status.INFO, "Parsed: " + json.writeValueAsString(preview.get("parse"))));
      lines.add(new ProcessSummaryLine(Status.INFO, "SNIP findings: " + json.writeValueAsString(preview.get("findings"))));
      if(Boolean.TRUE.equals(preview.get("phiSuppressed")))
      {
         lines.add(new ProcessSummaryLine(Status.INFO, "PHI map result suppressed; no target records written"));
         lines.add(new ProcessSummaryLine(Status.INFO, "Acknowledgement preview suppressed for PHI document"));
      }
      else
      {
         lines.add(new ProcessSummaryLine(Status.INFO, json.writeValueAsString(preview.get("result"))));
         lines.add(new ProcessSummaryLine(Status.INFO, json.writeValueAsString(preview.get("ack"))));
         Map<?, ?> diff = (Map<?, ?>) preview.get("diff");
         for(String key : List.of("againstExpected", "againstPreviousRevision"))
         {
            String label = key.equals("againstExpected") ? "Expected result" : "Previous revision";
            List<?> differences = (List<?>) diff.get(key);
            lines.add(new ProcessSummaryLine(Status.INFO, label + (differences.isEmpty() ? " matches" : " differs: " + json.writeValueAsString(differences))));
         }
      }
      return (lines);
   }
}
