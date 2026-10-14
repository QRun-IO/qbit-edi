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
import com.kingsrook.qbits.edi.x12.X12Segment;

/*******************************************************************************
 ** In-memory EDIFACT serialization; escape values, never composite structure.
 *******************************************************************************/
public class EdifactWriter
{
   /*******************************************************************************
    ** Render a full interchange with explicit UNA advice.
    *******************************************************************************/
   public String write(EdifactInterchange interchange)
   {
      return (write(interchange, ""));
   }

   /*******************************************************************************
    ** Retain optional groups and caller-selected line endings.
    *******************************************************************************/
   public String write(EdifactInterchange interchange, String ending)
   {
      List<X12Segment> segments = new ArrayList<>();
      segments.add(interchange.unb());
      interchange.messages().forEach(m -> segments.addAll(m.segments()));
      for(var group : interchange.groups())
      {
         segments.add(group.ung());
         group.messages().forEach(m -> segments.addAll(m.segments()));
         segments.add(group.une());
      }
      segments.add(interchange.unz());
      return (interchange.separators().una() + ending + write(segments, interchange.separators(), ending));
   }

   /*******************************************************************************
    ** Serialize component and repetition lists without losing released delimiters.
    *******************************************************************************/
   public String write(List<X12Segment> segments, EdifactSeparators separators, String ending)
   {
      if(!List.of("", "\n", "\r\n").contains(ending))
      {
         throw new IllegalArgumentException("Unsupported line ending");
      }
      StringBuilder out = new StringBuilder();
      for(var segment : segments)
      {
         out.append(segment.id());
         for(var element : segment.elements())
         {
            out.append(separators.element());
            for(int r = 0; r < element.size(); r++)
            {
               if(r > 0)
               {
                  if(separators.repetition() == ' ')
                  {
                     throw new IllegalArgumentException("Repetition requires syntax version 4");
                  }
                  out.append(separators.repetition());
               }
               for(int c = 0; c < element.get(r).size(); c++)
               {
                  if(c > 0)
                  {
                     out.append(separators.component());
                  }
                  for(char value : element.get(r).get(c).toCharArray())
                  {
                     if(value == separators.release() || value == separators.element() || value == separators.component()
                        || value == separators.segment() || separators.repetition() != ' ' && value == separators.repetition())
                     {
                        out.append(separators.release());
                     }
                     out.append(value);
                  }
               }
            }
         }
         out.append(separators.segment()).append(ending);
      }
      return (out.toString());
   }

   /*******************************************************************************
    ** Construct a segment from explicit component lists, with no delimiter parsing.
    *******************************************************************************/
   @SafeVarargs
   public static X12Segment segment(String tag, int position, List<String>... elements)
   {
      return (new X12Segment(tag, position, java.util.Arrays.stream(elements).map(List::of).toList()));
   }
}
