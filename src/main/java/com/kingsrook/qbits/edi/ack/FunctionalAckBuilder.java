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

package com.kingsrook.qbits.edi.ack;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;


/*******************************************************************************
 ** Shared 997/999 status, counts and detail generation.
 *******************************************************************************/
final class FunctionalAckBuilder
{
   /*******************************************************************************
    ** Build an acknowledgement transaction with its own ST control number.
    *******************************************************************************/
   static X12TransactionSet build(boolean implementation, X12FunctionalGroup group, Map<String, List<ValidationFinding>> findings, AckLevel level, String control)
   {
      List<X12Segment> segments = new ArrayList<>();
      if(implementation)
      {
         add(segments, "ST", "999", control, "005010X231A1");
         add(segments, "AK1", group.gs().value(1), group.gs().value(6), group.gs().value(8));
      }
      else
      {
         add(segments, "ST", "997", control);
         add(segments, "AK1", group.gs().value(1), group.gs().value(6));
      }
      int accepted = 0;
      boolean hasWarnings = false;
      for(X12TransactionSet transaction : group.transactions())
      {
         List<ValidationFinding> errors = findings.getOrDefault(transaction.st().value(2), List.of());
         boolean rejected = !transaction.findings().isEmpty() || errors.stream().anyMatch(f -> "ERROR".equals(f.severity()) && f.snipLevel() <= 2);
         String status = rejected ? "R" : errors.isEmpty() ? "A" : "E";
         if(!rejected)
         {
            accepted++;
         }
         hasWarnings |= status.equals("E");
         if(level == AckLevel.GROUP_ONLY || level == AckLevel.ERRORS_ONLY && status.equals("A"))
         {
            continue;
         }
         if(implementation && !transaction.st().value(3).isEmpty())
         {
            add(segments, "AK2", transaction.st().value(1), transaction.st().value(2), transaction.st().value(3));
         }
         else
         {
            add(segments, "AK2", transaction.st().value(1), transaction.st().value(2));
         }
         for(ValidationFinding finding : errors)
         {
            add(segments, implementation ? "IK3" : "AK3", finding.segmentId(), String.valueOf(finding.segmentPosition()),
               finding.loopId() == null ? "" : finding.loopId(), implementation ? finding.ik3Code() : finding.ak3Code());
            if(finding.elementPosition() != null)
            {
               List<String> position = new ArrayList<>(List.of(finding.elementPosition().toString()));
               if(finding.componentPosition() != null)
               {
                  position.add(finding.componentPosition().toString());
               }
               segments.add(new X12Segment(implementation ? "IK4" : "AK4", segments.size() + 1,
                  List.of(List.of(position), List.of(List.of("")), List.of(List.of(implementation ? finding.ik4Code() : finding.ak4Code())))));
            }
            if(implementation && finding.context() != null)
            {
               var context = finding.context();
               List<List<List<String>>> values = new ArrayList<>();
               for(String value : List.of("SITUATIONAL TRIGGER", context.segmentId(), String.valueOf(context.segmentPosition()), context.loopId() == null ? "" : context.loopId()))
               {
                  values.add(List.of(List.of(value)));
               }
               if(context.elementPosition() != null)
               {
                  List<String> position = new ArrayList<>(List.of(context.elementPosition().toString()));
                  if(context.componentPosition() != null)
                  {
                     position.add(context.componentPosition().toString());
                  }
                  values.add(List.of(position));
               }
               segments.add(new X12Segment("CTX", segments.size() + 1, values));
            }
         }
         List<String> statusElements = new ArrayList<>(List.of(status));
         transaction.findings().stream().map(EnvelopeFinding::code).distinct().limit(5).forEach(statusElements::add);
         if(rejected && statusElements.size() == 1)
         {
            statusElements.add("5");
         }
         add(segments, implementation ? "IK5" : "AK5", statusElements.toArray(String[]::new));
      }
      String status = !group.findings().isEmpty() || accepted == 0 && !group.transactions().isEmpty() ? "R"
         : accepted < group.transactions().size() ? "P" : hasWarnings ? "E" : "A";
      List<String> summary = new ArrayList<>(List.of(status, group.ge().value(1), String.valueOf(group.transactions().size()), String.valueOf(accepted)));
      group.findings().stream().map(EnvelopeFinding::code).distinct().limit(5).forEach(summary::add);
      add(segments, "AK9", summary.toArray(String[]::new));
      add(segments, "SE", String.valueOf(segments.size() + 1), control);
      return (new X12TransactionSet(segments.getFirst(), segments.getLast(), List.copyOf(segments), List.of()));
   }

   /*******************************************************************************
    ** Append a positioned simple segment.
    *******************************************************************************/
   private static void add(List<X12Segment> segments, String id, String... values)
   {
      segments.add(X12Segment.of(id, segments.size() + 1, values));
   }
}
