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

package com.kingsrook.qbits.edi.guide;


import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.document.EdiDocument;
import com.kingsrook.qbits.edi.x12.X12Segment;


/*******************************************************************************
 ** Original healthcare balancing and format checks, without licensed code content.
 *******************************************************************************/
public class HealthcareValidation
{
   /*******************************************************************************
    ** Exact decimal arithmetic; every diagnostic contains only positions and codes.
    *******************************************************************************/
   public List<ValidationFinding> validate(EdiDocument transaction, Guide guide, Map<String, List<List<X12Segment>>> loops)
   {
      List<ValidationFinding> findings = new ArrayList<>();
      List<X12Segment> segments = transaction.segments();
      if("837".equals(guide.transactionSet()))
      {
         for(List<X12Segment> claim : blocks(segments, "CLM"))
         {
            X12Segment head = claim.getFirst();
            try
            {
               BigDecimal lines = BigDecimal.ZERO;
               for(X12Segment line : claim)
               {
                  if(line.id().equals("SV1") || line.id().equals("SV2"))
                  {
                     lines = lines.add(amount(line, line.id().equals("SV1") ? 2 : 3));
                  }
               }
               if(amount(head, 2).compareTo(lines) != 0)
               {
                  findings.add(finding(head, "2300", 2, null, 3, "Claim charges do not balance"));
               }
            }
            catch(NumberFormatException e)
            {
               findings.add(finding(head, "2300", 2, null, 3, "Claim balance cannot be calculated"));
            }
         }
      }
      if("835".equals(guide.transactionSet()))
      {
         remittance(segments, findings);
      }
      for(X12Segment segment : segments)
      {
         String loop = loops.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(o -> o.contains(segment)))
            .min(java.util.Comparator.comparingInt(e -> e.getValue().stream().filter(o -> o.contains(segment)).mapToInt(List::size).min().orElse(Integer.MAX_VALUE)))
            .map(Map.Entry::getKey).orElse(null);
         if(segment.id().equals("NM1") && segment.value(8).equals("XX") && !validNpi(segment.value(9)))
         {
            findings.add(finding(segment, loop, 9, null, 5, "Invalid NPI format or check digit"));
         }
         if(segment.id().equals("N4") && !segment.value(3).isEmpty() && (segment.value(4).isEmpty() || segment.value(4).equals("US"))
            && !segment.value(3).matches("[0-9]{5}([0-9]{4}|-[0-9]{4})?"))
         {
            findings.add(finding(segment, loop, 3, null, 5, "Invalid postal format"));
         }
         if(segment.id().equals("HI"))
         {
            for(int position = 1; position <= segment.elements().size(); position++)
            {
               for(int repetition = 1; repetition <= segment.elements().get(position - 1).size(); repetition++)
               {
                  if(List.of("ABK", "ABF", "ABJ", "APR").contains(segment.value(position, repetition, 1))
                     && !segment.value(position, repetition, 2).matches("[A-Z][0-9][A-Z0-9](?:[A-Z0-9]{0,4}|\\.[A-Z0-9]{1,4})"))
                  {
                     findings.add(finding(segment, loop, position, 2, 5, "Invalid ICD-10 diagnosis format"));
                  }
               }
            }
         }
      }
      return (findings);
   }

   /*******************************************************************************
    ** Claim and line CAS monetary positions are 3, 6, 9, 12, 15 and 18.
    *******************************************************************************/
   private static void remittance(List<X12Segment> segments, List<ValidationFinding> findings)
   {
      for(List<X12Segment> claim : blocks(segments, "CLP"))
      {
         X12Segment head = claim.getFirst();
         try
         {
            BigDecimal adjustments = BigDecimal.ZERO;
            for(X12Segment segment : claim)
            {
               if(segment.id().equals("CAS"))
               {
                  for(int p = 3; p <= segment.elements().size(); p += 3)
                  {
                     adjustments = adjustments.add(amount(segment, p));
                  }
               }
            }
            if(amount(head, 3).subtract(amount(head, 4)).compareTo(adjustments) != 0)
            {
               findings.add(finding(head, "2100", 3, null, 3, "Claim adjustments do not balance"));
            }
         }
         catch(NumberFormatException e)
         {
            findings.add(finding(head, "2100", 3, null, 3, "Claim balance cannot be calculated"));
         }
      }
      for(X12Segment payment : segments.stream().filter(s -> s.id().equals("BPR")).toList())
      {
         try
         {
            BigDecimal total = BigDecimal.ZERO;
            for(X12Segment segment : segments)
            {
               if(segment.id().equals("CLP"))
               {
                  total = total.add(amount(segment, 4));
               }
               if(segment.id().equals("PLB"))
               {
                  for(int p = 4; p <= segment.elements().size(); p += 2)
                  {
                     total = total.subtract(amount(segment, p));
                  }
               }
            }
            if(amount(payment, 2).compareTo(total) != 0)
            {
               findings.add(finding(payment, null, 2, null, 3, "Payment does not balance"));
            }
         }
         catch(NumberFormatException e)
         {
            findings.add(finding(payment, null, 2, null, 3, "Payment balance cannot be calculated"));
         }
      }
   }

   /*******************************************************************************
    ** Isolate each claim so one claim cannot compensate for another's imbalance.
    *******************************************************************************/
   private static List<List<X12Segment>> blocks(List<X12Segment> segments, String anchor)
   {
      List<List<X12Segment>> blocks = new ArrayList<>();
      List<X12Segment> current = null;
      for(X12Segment segment : segments)
      {
         if(segment.id().equals(anchor))
         {
            current = new ArrayList<>();
            blocks.add(current);
         }
         if(current != null)
         {
            current.add(segment);
         }
      }
      return (blocks);
   }

   /*******************************************************************************
    ** Missing monetary elements fail balancing rather than silently becoming zero.
    *******************************************************************************/
   private static BigDecimal amount(X12Segment segment, int position)
   {
      return (new BigDecimal(segment.value(position)));
   }

   /*******************************************************************************
    ** Position-only finding with a positional trigger for the implementation ack.
    *******************************************************************************/
   private static ValidationFinding finding(X12Segment segment, String loop, Integer element, Integer component, int level, String message)
   {
      return (new ValidationFinding(segment.position(), segment.id(), loop, element, component, level,
         "8", "7", "8", "7", "ERROR", message, null)
         .withContext(level == 3 ? new AckContext(segment.id(), segment.position(), loop, element, component) : null));
   }

   /*******************************************************************************
    ** NPI uses the Luhn checksum over the 80840 prefix and all ten digits.
    *******************************************************************************/
   public static boolean validNpi(String value)
   {
      if(!value.matches("[12][0-9]{9}"))
      {
         return (false);
      }
      String digits = "80840" + value;
      int sum = 0;
      for(int i = digits.length() - 1, n = 0; i >= 0; i--, n++)
      {
         int digit = digits.charAt(i) - '0';
         if(n % 2 == 1)
         {
            digit *= 2;
         }
         sum += digit > 9 ? digit - 9 : digit;
      }
      return (sum % 10 == 0);
   }

   /*******************************************************************************
    ** Service date, then payment date, then the configured clock for undated documents.
    *******************************************************************************/
   public static LocalDate asOf(List<X12Segment> segments, Clock clock)
   {
      for(X12Segment segment : segments)
      {
         String date = segment.id().equals("DTP") && segment.value(1).equals("472") ? segment.value(3)
            : segment.id().equals("BPR") ? segment.value(16) : "";
         if(RuleEvaluator.validDate(date) && date.length() == 8)
         {
            return (LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE));
         }
      }
      return (LocalDate.now(clock));
   }
}
