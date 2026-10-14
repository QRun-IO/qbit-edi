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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;


/*******************************************************************************
 ** Stream interchanges through StAEDI, retaining bounded transaction buffers and structured findings.
 *******************************************************************************/
public class X12EnvelopeReader
{
   private final int maxSegments;

   /*******************************************************************************
    ** Set the maximum retained segment count per transaction.
    *******************************************************************************/
   public X12EnvelopeReader(int maxSegments)
   {
      if(maxSegments < 2)
      {
         throw new IllegalArgumentException("At least two segments are required");
      }
      this.maxSegments = maxSegments;
   }

   /*******************************************************************************
    ** Read concatenated interchanges without closing the caller stream.
    *******************************************************************************/
   public List<X12Interchange> read(InputStream input) throws IOException
   {
      List<X12Interchange> results = new ArrayList<>();
      int first;
      while((first = input.read()) != -1)
      {
         if(Character.isWhitespace(first))
         {
            continue;
         }
         byte[] header = new byte[106];
         header[0] = (byte) first;
         int length = input.readNBytes(header, 1, 105) + 1;
         X12Separators separators;
         try
         {
            separators = X12Separators.fromIsa(new String(header, 0, length, StandardCharsets.US_ASCII));
         }
         catch(IllegalArgumentException e)
         {
            results.add(new X12Interchange(X12Separators.DEFAULT, null, null, List.of(), List.of(), List.of(new EnvelopeFinding("TA1", 1, "022", "Invalid fixed-width ISA"))));
            break;
         }
         InterchangeStream remainder = new InterchangeStream(input, separators.segment());
         InputStream stream = new SequenceInputStream(new ByteArrayInputStream(header), remainder);
         results.add(parse(stream, separators));
         while(remainder.read() != -1)
         {
            // Drain a malformed interchange to its IEA boundary before reading another.
         }
      }
      return (results);
   }

   /*******************************************************************************
    ** Tokenize one bounded stream; parser failures never include payload text.
    *******************************************************************************/
   private X12Interchange parse(InputStream input, X12Separators separators) throws IOException
   {
      EDIInputFactory factory = EDIInputFactory.newFactory();
      factory.setProperty(EDIInputFactory.EDI_IGNORE_EXTRANEOUS_CHARACTERS, true);
      factory.setProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, false);
      List<EnvelopeFinding> errors = new ArrayList<>();
      List<EnvelopeFinding> groupErrors = new ArrayList<>();
      List<EnvelopeFinding> transactionErrors = new ArrayList<>();
      List<X12FunctionalGroup> groups = new ArrayList<>();
      List<X12TransactionSet> transactions = new ArrayList<>();
      List<X12Segment> segments = new ArrayList<>();
      List<X12Segment> services = new ArrayList<>();
      X12Segment isa = null;
      X12Segment iea = null;
      X12Segment gs = null;
      X12Segment st = null;
      String tag = "";
      int count = 0;
      List<List<List<String>>> values = new ArrayList<>();
      try(EDIStreamReader reader = factory.createEDIStreamReader(input))
      {
         while(reader.hasNext())
         {
            EDIStreamEvent event = reader.next();
            if(event == EDIStreamEvent.START_SEGMENT)
            {
               tag = reader.getText();
               values = new ArrayList<>();
               if("GS".equals(tag))
               {
                  groupErrors = new ArrayList<>();
               }
               if("ST".equals(tag))
               {
                  transactionErrors = new ArrayList<>();
               }
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
               List<String> components = values.get(e - 1).get(r - 1);
               while(components.size() < c)
               {
                  components.add("");
               }
               components.set(c - 1, reader.getText());
            }
            else if(event == EDIStreamEvent.SEGMENT_ERROR)
            {
               errors.add(new EnvelopeFinding("TA1", reader.getLocation().getSegmentPosition(), "022", "Unrecognized control structure"));
            }
            else if(event.isError() && reader.getErrorType() != EDIStreamValidationError.CONTROL_REFERENCE_MISMATCH
               && reader.getErrorType() != EDIStreamValidationError.CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT)
            {
               int element = reader.getLocation().getElementPosition();
               if("ISA".equals(tag))
               {
                  String code = switch(element)
                  {
                     case 5 -> "005";
                     case 6 -> "006";
                     case 7, 8 -> "007";
                     case 9 -> "014";
                     case 10 -> "015";
                     case 11 -> "016";
                     case 12 -> "017";
                     case 13 -> "018";
                     case 14 -> "019";
                     case 15 -> "020";
                     default -> "022";
                  };
                  errors.add(new EnvelopeFinding("TA1", 1, code, "Invalid interchange header element"));
               }
               else if(st != null || "ST".equals(tag) || "SE".equals(tag))
               {
                  transactionErrors.add(new EnvelopeFinding("AK5", Math.max(1, count), "ST".equals(tag) && element == 2 ? "7" : "5", "Invalid transaction syntax"));
               }
               else if(gs != null || "GS".equals(tag))
               {
                  groupErrors.add(new EnvelopeFinding("AK9", reader.getLocation().getSegmentPosition(), "6", "Invalid group syntax"));
               }
               else
               {
                  errors.add(new EnvelopeFinding("TA1", reader.getLocation().getSegmentPosition(), "022", "Invalid control syntax"));
               }
            }
            else if(event == EDIStreamEvent.END_SEGMENT)
            {
               if("ST".equals(tag))
               {
                  count = 0;
               }
               X12Segment segment = new X12Segment(tag, st != null || "ST".equals(tag) ? ++count : reader.getLocation().getSegmentPosition(), values);
               switch(tag)
               {
                  case "ISA" -> isa = segment;
                  case "GS" ->
                  {
                     gs = segment;
                     transactions = new ArrayList<>();
                  }
                  case "ST" ->
                  {
                     st = segment;
                     segments = new ArrayList<>();
                     segments.add(segment);
                  }
                  case "SE" ->
                  {
                     if(st == null)
                     {
                        errors.add(new EnvelopeFinding("TA1", segment.position(), "022", "SE without ST"));
                        break;
                     }
                     check(transactionErrors, "AK5", segment, segment.value(1), String.valueOf(count), "4", "Transaction segment count mismatch");
                     check(transactionErrors, "AK5", segment, segment.value(2), st.value(2), "3", "Transaction control number mismatch");
                     if(count > maxSegments && transactionErrors.stream().noneMatch(f -> "5".equals(f.code())))
                     {
                        transactionErrors.add(new EnvelopeFinding("AK5", count, "5", "Transaction segment limit exceeded"));
                     }
                     if(segments.size() < maxSegments)
                     {
                        segments.add(segment);
                     }
                     transactions.add(new X12TransactionSet(st, segment, List.copyOf(segments), List.copyOf(transactionErrors)));
                     st = null;
                  }
                  case "GE" ->
                  {
                     if(gs != null)
                     {
                        check(groupErrors, "AK9", segment, segment.value(1), String.valueOf(transactions.size()), "5", "Group transaction count mismatch");
                        check(groupErrors, "AK9", segment, segment.value(2), gs.value(6), "4", "Group control number mismatch");
                        groups.add(new X12FunctionalGroup(gs, segment, List.copyOf(transactions), List.copyOf(groupErrors)));
                        gs = null;
                     }
                  }
                  case "IEA" ->
                  {
                     iea = segment;
                     check(errors, "TA1", segment, segment.value(1), String.valueOf(groups.size()), "021", "Interchange group count mismatch");
                     check(errors, "TA1", segment, segment.value(2), isa == null ? "" : isa.value(13), "001", "Interchange control number mismatch");
                  }
                  case "TA1" -> services.add(segment);
                  default ->
                  {
                     if(st != null)
                     {
                        if(segments.size() < maxSegments)
                        {
                           segments.add(segment);
                        }
                        else if(transactionErrors.stream().noneMatch(f -> "5".equals(f.code())))
                        {
                           transactionErrors.add(new EnvelopeFinding("AK5", count, "5", "Transaction segment limit exceeded"));
                        }
                     }
                     else
                     {
                        errors.add(new EnvelopeFinding("TA1", segment.position(), "022", "Segment outside transaction"));
                     }
                  }
               }
            }
         }
      }
      catch(EDIStreamException e)
      {
         errors.add(new EnvelopeFinding("TA1", 0, "022", "Invalid interchange syntax"));
      }
      if(iea == null || gs != null || st != null)
      {
         errors.add(new EnvelopeFinding("TA1", 0, "022", "Incomplete interchange"));
      }
      return (new X12Interchange(separators, isa, iea, List.copyOf(groups), List.copyOf(services), List.copyOf(errors)));
   }

   /*******************************************************************************
    ** Compare envelope fields while accepting zero-padded counts.
    *******************************************************************************/
   private static void check(List<EnvelopeFinding> errors, String level, X12Segment segment, String actual, String expected, String code, String message)
   {
      boolean equal = actual.equals(expected);
      if(code.equals("4") && level.equals("AK5") || code.equals("5") || code.equals("021"))
      {
         equal = actual.matches("[0-9]+") && new java.math.BigInteger(actual).toString().equals(expected);
      }
      if(!equal)
      {
         errors.add(new EnvelopeFinding(level, segment.position(), code, message));
      }
   }

   /*******************************************************************************
    ** Prevent parser read-ahead from consuming the following interchange.
    *******************************************************************************/
   private static class InterchangeStream extends InputStream
   {
      private final InputStream input;
      private final char terminator;
      private final StringBuilder prefix = new StringBuilder();
      private boolean ended;

      /*******************************************************************************
       ** Wrap the caller stream without owning it.
       *******************************************************************************/
      private InterchangeStream(InputStream input, char terminator)
      {
         this.input = input;
         this.terminator = terminator;
      }

      /*******************************************************************************
       ** Stop immediately after the IEA terminator.
       *******************************************************************************/
      @Override
      public int read() throws IOException
      {
         if(ended)
         {
            return (-1);
         }
         int value = input.read();
         if(value == terminator)
         {
            ended = prefix.toString().equals("IEA");
            prefix.setLength(0);
         }
         else if(value != -1 && prefix.length() < 3 && !(prefix.isEmpty() && Character.isWhitespace(value)))
         {
            prefix.append((char) value);
         }
         return (value);
      }
   }
}
