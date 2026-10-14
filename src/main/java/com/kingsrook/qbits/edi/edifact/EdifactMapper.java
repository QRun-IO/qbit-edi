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
import java.util.Comparator;
import java.util.List;
import com.kingsrook.qbits.edi.map.OutboundMapper;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Reuse mapping transforms and associations with native UNH/UNT envelopes.
 *******************************************************************************/
public class EdifactMapper extends OutboundMapper
{
   private final char decimalMark;

   /*******************************************************************************
    ** Default numeric formatting follows the default UNA decimal mark.
    *******************************************************************************/
   public EdifactMapper()
   {
      this('.');
   }

   /*******************************************************************************
    ** Use the agreement's decimal service character for outbound numeric transforms.
    *******************************************************************************/
   public EdifactMapper(char decimalMark)
   {
      this.decimalMark = decimalMark;
   }

   /*******************************************************************************
    ** Render numeric values using the negotiated decimal mark.
    *******************************************************************************/
   @Override
   protected String decimal(String value)
   {
      return (value.replace('.', decimalMark));
   }

   /*******************************************************************************
    ** Build one directory-specific message with an allocated UNH reference.
    *******************************************************************************/
   public EdifactMessage message(QRecord source, QRecord map, List<QRecord> rules, String reference) throws Exception
   {
      if(!"OUTBOUND".equals(map.getValueString("direction")))
      {
         throw new IllegalArgumentException("An outbound map is required");
      }
      String version = map.getValueString("version");
      List<X12Segment> segments = new ArrayList<>();
      segments.add(EdifactWriter.segment("UNH", 1, List.of(reference), List.of(map.getValueString("transactionSet"), version.substring(0, 1), version.substring(1), "UN")));
      emit(source, rules.stream().sorted(Comparator.comparing(r -> r.getValueInteger("sequence"))).toList(), "", "", 1, "", new int[] {0}, segments);
      segments.add(X12Segment.of("UNT", segments.size() + 1, String.valueOf(segments.size() + 1), reference));
      return (new EdifactMessage(segments.getFirst(), segments.getLast(), List.copyOf(segments), List.of()));
   }
}
