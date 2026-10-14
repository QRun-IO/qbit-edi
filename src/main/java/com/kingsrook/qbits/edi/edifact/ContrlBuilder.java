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

package com.kingsrook.qbits.edi.edifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.x12.X12Segment;

/*******************************************************************************
 ** Positional CONTRL syntax responses; business application failures are not syntax errors.
 *******************************************************************************/
public class ContrlBuilder
{
   /*******************************************************************************
    ** Build directory-specific acknowledgement details without echoing data values.
    *******************************************************************************/
   public EdifactMessage build(EdifactInterchange input, Map<String, List<ValidationFinding>> findings, AckLevel level, String reference, String version)
   {
      List<X12Segment> segments = new ArrayList<>();
      segments.add(EdifactWriter.segment("UNH", 1, List.of(reference), List.of("CONTRL", version.substring(0, 1), version.substring(1), "UN")));
      boolean rejected = !input.findings().isEmpty() || level == AckLevel.GROUP_ONLY && (input.groups().stream().anyMatch(g -> !g.findings().isEmpty())
         || input.allMessages().stream().anyMatch(m -> rejected(m, findings)));
      var uci = new ArrayList<List<String>>();
      uci.add(List.of(input.unb().value(5)));
      uci.add(input.unb().elements().get(1).getFirst());
      uci.add(input.unb().elements().get(2).getFirst());
      uci.add(List.of(rejected ? "4" : "7"));
      if(!input.findings().isEmpty())
      {
         uci.add(List.of(input.findings().getFirst().code()));
      }
      segments.add(new X12Segment("UCI", 2, uci.stream().map(List::of).toList()));
      if(level != AckLevel.GROUP_ONLY && input.findings().isEmpty())
      {
         messages(input.messages(), findings, level, segments);
         for(var group : input.groups())
         {
            var response = EdifactWriter.segment("UCF", segments.size() + 1, List.of(group.ung().value(5)), group.ung().elements().get(1).getFirst(),
               group.ung().elements().get(2).getFirst(), List.of(group.findings().isEmpty() ? "7" : "4"));
            if(!group.findings().isEmpty())
            {
               var fields = new ArrayList<>(response.elements());
               fields.add(List.of(List.of(group.findings().getFirst().code())));
               response = new X12Segment("UCF", response.position(), fields);
            }
            segments.add(response);
            messages(group.messages(), findings, level, segments);
         }
      }
      segments.add(X12Segment.of("UNT", segments.size() + 1, String.valueOf(segments.size() + 1), reference));
      return (new EdifactMessage(segments.getFirst(), segments.getLast(), List.copyOf(segments), List.of()));
   }

   /*******************************************************************************
    ** Suppress accepted message detail only under ERRORS_ONLY.
    *******************************************************************************/
   private void messages(List<EdifactMessage> messages, Map<String, List<ValidationFinding>> findings, AckLevel level, List<X12Segment> segments)
   {
      for(var message : messages)
      {
         if("CONTRL".equals(message.type()))
         {
            continue;
         }
         boolean rejected = rejected(message, findings);
         if(level == AckLevel.ERRORS_ONLY && !rejected)
         {
            continue;
         }
         var fields = new ArrayList<List<List<String>>>();
         fields.add(List.of(List.of(message.reference())));
         fields.add(message.header().elements().get(1));
         fields.add(List.of(List.of(rejected ? "4" : "7")));
         if(!message.findings().isEmpty())
         {
            fields.add(List.of(List.of(message.findings().getFirst().code())));
         }
         segments.add(new X12Segment("UCM", segments.size() + 1, fields));
         for(var finding : findings.getOrDefault(message.reference(), List.of()))
         {
            if(!"ERROR".equals(finding.severity()))
            {
               continue;
            }
            String code = syntaxCode(finding);
            segments.add(finding.elementPosition() == null
               ? X12Segment.of("UCS", segments.size() + 1, String.valueOf(finding.segmentPosition()), code)
               : X12Segment.of("UCS", segments.size() + 1, String.valueOf(finding.segmentPosition())));
            if(finding.elementPosition() != null)
            {
               List<String> position = finding.componentPosition() == null ? List.of(finding.elementPosition().toString())
                  : List.of(finding.elementPosition().toString(), finding.componentPosition().toString());
               segments.add(EdifactWriter.segment("UCD", segments.size() + 1, List.of(code), position));
            }
         }
      }
   }

   /*******************************************************************************
    ** Envelope and structural errors reject the message, warnings do not.
    *******************************************************************************/
   private boolean rejected(EdifactMessage message, Map<String, List<ValidationFinding>> findings)
   {
      return (!message.findings().isEmpty() || findings.getOrDefault(message.reference(), List.of()).stream().anyMatch(f -> "ERROR".equals(f.severity())));
   }

   /*******************************************************************************
    ** Translate shared validator diagnostics to service syntax codes (0085).
    *******************************************************************************/
   public static String syntaxCode(ValidationFinding finding)
   {
      String code = finding.elementPosition() == null ? finding.ak3Code() : finding.ak4Code();
      if(code == null)
      {
         return ("12");
      }
      return (finding.elementPosition() == null ? switch(code)
      {
         case "3" -> "13";
         case "4", "5" -> "35";
         case "2", "6", "7" -> "33";
         default -> "12";
      } : switch(code)
      {
         case "1", "2" -> "13";
         case "4" -> "14";
         case "5" -> "15";
         default -> "12";
      });
   }
}
