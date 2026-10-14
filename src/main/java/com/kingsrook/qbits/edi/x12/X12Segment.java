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


import java.util.Arrays;
import java.util.List;


/*******************************************************************************
 ** Segment position is relative to ST for transaction segments. Elements retain repetitions and components.
 *******************************************************************************/
public record X12Segment(String id, int position, List<List<List<String>>> elements)
{
   /*******************************************************************************
    ** Deeply copy the segment values.
    *******************************************************************************/
   public X12Segment
   {
      elements = elements.stream().map(e -> e.stream().map(List::copyOf).toList()).toList();
   }

   /*******************************************************************************
    ** Build a segment with simple elements.
    *******************************************************************************/
   public static X12Segment of(String id, int position, String... values)
   {
      return (new X12Segment(id, position, Arrays.stream(values).map(v -> List.of(List.of(v))).toList()));
   }

   /*******************************************************************************
    ** Read the first repetition and component, or the empty value.
    *******************************************************************************/
   public String value(int element)
   {
      return (value(element, 1, 1));
   }

   /*******************************************************************************
    ** Read a one-based element, repetition and component.
    *******************************************************************************/
   public String value(int element, int repetition, int component)
   {
      if(element < 1 || element > elements.size() || repetition < 1 || repetition > elements.get(element - 1).size()
         || component < 1 || component > elements.get(element - 1).get(repetition - 1).size())
      {
         return ("");
      }
      return (elements.get(element - 1).get(repetition - 1).get(component - 1));
   }
}
