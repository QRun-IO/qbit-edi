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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.kingsrook.qbits.edi.document.EnvelopeReader;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qbits.edi.x12.X12Segment;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;

/*******************************************************************************
 ** StAEDI tokenizer with explicit UN envelope checks and bounded message retention.
 *******************************************************************************/
public class EdifactEnvelopeReader
{
   private final int maxSegments;

   /*******************************************************************************
    ** Bound retained message segments, including UNH and UNT.
    *******************************************************************************/
   public EdifactEnvelopeReader(int maxSegments)
   {
      if(maxSegments < 2)
      {
         throw new IllegalArgumentException("At least two segments are required");
      }
      this.maxSegments = maxSegments;
   }

   /*******************************************************************************
    ** Read concatenated EDIFACT interchanges without closing the caller stream.
    *******************************************************************************/
   public List<EdifactInterchange> read(InputStream input) throws IOException
   {
      List<EdifactInterchange> result = new ArrayList<>();
      EnvelopeReader framing = new EnvelopeReader();
      byte[] bytes;
      while((bytes = framing.next(input)) != null)
      {
         result.add(parse(bytes));
      }
      return (result);
   }

   /*******************************************************************************
    ** Parse one framed interchange; diagnostics never include submitted values.
    *******************************************************************************/
   private EdifactInterchange parse(byte[] bytes) throws IOException
   {
      String prefix = new String(bytes, 0, Math.min(9, bytes.length), StandardCharsets.US_ASCII);
      EdifactSeparators separators = prefix.startsWith("UNA") ? EdifactSeparators.fromUna(prefix) : EdifactSeparators.DEFAULT;
      List<EnvelopeFinding> errors = new ArrayList<>();
      List<EnvelopeFinding> groupErrors = new ArrayList<>();
      List<EnvelopeFinding> messageErrors = new ArrayList<>();
      List<EdifactGroup> groups = new ArrayList<>();
      List<EdifactMessage> messages = new ArrayList<>();
      List<EdifactMessage> grouped = new ArrayList<>();
      List<X12Segment> segments = new ArrayList<>();
      var references = new HashSet<String>();
      X12Segment unb = null;
      X12Segment unz = null;
      X12Segment ung = null;
      X12Segment unh = null;
      String tag = "";
      int count = 0;
      List<List<List<String>>> values = new ArrayList<>();
      EDIInputFactory factory = EDIInputFactory.newFactory();
      factory.setProperty(EDIInputFactory.EDI_IGNORE_EXTRANEOUS_CHARACTERS, true);
      factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);
      try(EDIStreamReader reader = factory.createEDIStreamReader(new ByteArrayInputStream(bytes)))
      {
         while(reader.hasNext())
         {
            EDIStreamEvent event = reader.next();
            if(event == EDIStreamEvent.START_SEGMENT)
            {
               tag = reader.getText();
               values = new ArrayList<>();
            }
            else if(event == EDIStreamEvent.ELEMENT_DATA)
            {
               int e = reader.getLocation().getElementPosition();
               int r = Math.max(1, reader.getLocation().getElementOccurrence());
               int c = Math.max(1, reader.getLocation().getComponentPosition());
               while(values.size() < e)
               {
                  values.add(new ArrayList<>());
               }
               while(values.get(e - 1).size() < r)
               {
                  values.get(e - 1).add(new ArrayList<>());
               }
               var components = values.get(e - 1).get(r - 1);
               while(components.size() < c)
               {
                  components.add("");
               }
               components.set(c - 1, reader.getText());
            }
            else if(event.isError() && reader.getErrorType() != EDIStreamValidationError.CONTROL_REFERENCE_MISMATCH
               && reader.getErrorType() != EDIStreamValidationError.CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT)
            {
               if(unh != null)
               {
                  messageErrors.add(error("UNH", Math.max(1, count), "12"));
               }
               else if(ung != null)
               {
                  groupErrors.add(error("UNG", count, "12"));
               }
               else
               {
                  errors.add(error("UNB", count, "12"));
               }
            }
            else if(event == EDIStreamEvent.END_SEGMENT)
            {
               if("UNA".equals(tag))
               {
                  continue;
               }
               count = "UNH".equals(tag) ? 1 : count + 1;
               X12Segment segment = new X12Segment(tag, count, values);
               switch(tag)
               {
                  case "UNB" ->
                  {
                     unb = segment;
                     if("4".equals(unb.value(1, 1, 2)) && !prefix.startsWith("UNA"))
                     {
                        separators = new EdifactSeparators(':', '+', '.', '?', '*', '\'');
                     }
                     if(!List.of("3", "4").contains(unb.value(1, 1, 2)) || !List.of("", "1").contains(unb.value(11))
                        || unb.value(5).isEmpty() || unb.value(5).length() > 14)
                     {
                        errors.add(error("UNB", 1, "12"));
                     }
                  }
                  case "UNG" ->
                  {
                     ung = segment;
                     grouped = new ArrayList<>();
                     groupErrors = new ArrayList<>();
                  }
                  case "UNH" ->
                  {
                     unh = segment;
                     segments = new ArrayList<>();
                     segments.add(segment);
                     messageErrors = new ArrayList<>();
                     if(!references.add(unh.value(1)))
                     {
                        messageErrors.add(error("UNH", 1, "19"));
                     }
                  }
                  case "UNT" ->
                  {
                     if(unh == null)
                     {
                        errors.add(error("UNB", count, "33"));
                        break;
                     }
                     check(messageErrors, "UNH", segment, segment.value(1), String.valueOf(count), "29");
                     check(messageErrors, "UNH", segment, segment.value(2), unh.value(1), "28");
                     if(segments.size() < maxSegments)
                     {
                        segments.add(segment);
                     }
                     (ung == null ? messages : grouped).add(new EdifactMessage(unh, segment, List.copyOf(segments), List.copyOf(messageErrors)));
                     unh = null;
                  }
                  case "UNE" ->
                  {
                     if(ung == null)
                     {
                        errors.add(error("UNB", count, "33"));
                        break;
                     }
                     check(groupErrors, "UNG", segment, segment.value(1), String.valueOf(grouped.size()), "29");
                     check(groupErrors, "UNG", segment, segment.value(2), ung.value(5), "28");
                     groups.add(new EdifactGroup(ung, segment, List.copyOf(grouped), List.copyOf(groupErrors)));
                     ung = null;
                  }
                  case "UNZ" ->
                  {
                     unz = segment;
                     check(errors, "UNB", segment, segment.value(1), String.valueOf(groups.isEmpty() ? messages.size() : groups.size()), "29");
                     check(errors, "UNB", segment, segment.value(2), unb == null ? "" : unb.value(5), "28");
                  }
                  default ->
                  {
                     if(unh == null)
                     {
                        errors.add(error("UNB", count, "33"));
                     }
                     else if(segments.size() < maxSegments - 1)
                     {
                        segments.add(segment);
                     }
                     else if(messageErrors.stream().noneMatch(f -> "35".equals(f.code())))
                     {
                        messageErrors.add(error("UNH", count, "35"));
                     }
                  }
               }
            }
         }
      }
      catch(EDIStreamException e)
      {
         errors.add(error("UNB", 0, "12"));
      }
      if(unb == null || unz == null || ung != null || unh != null || !groups.isEmpty() && !messages.isEmpty())
      {
         errors.add(error("UNB", 0, "13"));
      }
      return (new EdifactInterchange(separators, unb, unz, List.copyOf(groups), List.copyOf(messages), List.copyOf(errors)));
   }

   /*******************************************************************************
    ** Count fields accept zero padding; references retain their exact spelling.
    *******************************************************************************/
   private static void check(List<EnvelopeFinding> findings, String level, X12Segment segment, String actual, String expected, String code)
   {
      boolean equal = "29".equals(code) ? actual.matches("[0-9]+") && new java.math.BigInteger(actual).toString().equals(expected) : actual.equals(expected);
      if(!equal)
      {
         findings.add(error(level, segment.position(), code));
      }
   }

   /*******************************************************************************
    ** Service syntax error code 0085 with a value-free diagnostic.
    *******************************************************************************/
   private static EnvelopeFinding error(String level, int position, String code)
   {
      return (new EnvelopeFinding(level, position, code, "EDIFACT envelope failed validation"));
   }
}
