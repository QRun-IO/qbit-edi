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

package com.kingsrook.qbits.edi.x12;


import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 ** In-memory X12 serialization with strict fixed-width ISA formatting.
 *******************************************************************************/
public class X12Writer
{
   /*******************************************************************************
    ** Serialize with no additional line ending.
    *******************************************************************************/
   public String write(X12Interchange interchange)
   {
      return (write(interchange, ""));
   }

   /*******************************************************************************
    ** Serialize an interchange using its declared separators.
    *******************************************************************************/
   public String write(X12Interchange interchange, String lineEnding)
   {
      List<X12Segment> segments = new ArrayList<>();
      segments.add(interchange.isa());
      segments.addAll(interchange.serviceSegments());
      for(X12FunctionalGroup group : interchange.groups())
      {
         segments.add(group.gs());
         for(X12TransactionSet transaction : group.transactions())
         {
            segments.addAll(transaction.segments());
         }
         segments.add(group.ge());
      }
      segments.add(interchange.iea());
      return (write(segments, interchange.separators(), lineEnding));
   }

   /*******************************************************************************
    ** Serialize segments; element data cannot contain structural separators.
    *******************************************************************************/
   public String write(List<X12Segment> segments, X12Separators separators, String lineEnding)
   {
      if(!List.of("", "\n", "\r\n").contains(lineEnding))
      {
         throw new IllegalArgumentException("Unsupported line ending");
      }
      StringBuilder out = new StringBuilder();
      for(X12Segment segment : segments)
      {
         out.append(segment.id());
         if("ISA".equals(segment.id()))
         {
            int[] widths = {2, 10, 2, 10, 2, 15, 2, 15, 6, 4, 1, 5, 9, 1, 1, 1};
            for(int i = 1; i <= widths.length; i++)
            {
               String value = segment.value(i);
               if(i == 16)
               {
                  value = String.valueOf(separators.component());
               }
               if(i == 11 && segment.value(12).compareTo("00501") >= 0 && separators.repetition() != null)
               {
                  value = separators.repetition().toString();
               }
               if(value.length() > widths[i - 1])
               {
                  throw new IllegalArgumentException("ISA element exceeds fixed width");
               }
               if(i == 13)
               {
                  if(!value.matches("[0-9]{1,9}"))
                  {
                     throw new IllegalArgumentException("ISA control number must contain digits");
                  }
                  value = "0".repeat(widths[i - 1] - value.length()) + value;
               }
               out.append(separators.element()).append(value).append(" ".repeat(widths[i - 1] - value.length()));
            }
         }
         else
         {
            for(List<List<String>> element : segment.elements())
            {
               out.append(separators.element());
               for(int r = 0; r < element.size(); r++)
               {
                  if(r > 0)
                  {
                     if(separators.repetition() == null)
                     {
                        throw new IllegalArgumentException("Repetition requires 00501 or later");
                     }
                     out.append(separators.repetition());
                  }
                  for(int c = 0; c < element.get(r).size(); c++)
                  {
                     if(c > 0)
                     {
                        out.append(separators.component());
                     }
                     String value = element.get(r).get(c);
                     if(value.indexOf(separators.element()) >= 0 || value.indexOf(separators.segment()) >= 0
                        || value.indexOf(separators.component()) >= 0 || separators.repetition() != null && value.indexOf(separators.repetition()) >= 0)
                     {
                        throw new IllegalArgumentException("Element contains a separator");
                     }
                     out.append(value);
                  }
               }
            }
         }
         out.append(separators.segment()).append(lineEnding);
      }
      return (out.toString());
   }
}
