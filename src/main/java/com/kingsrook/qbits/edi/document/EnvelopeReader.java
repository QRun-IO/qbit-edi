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

package com.kingsrook.qbits.edi.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.kingsrook.qbits.edi.edifact.EdifactSeparators;

/*******************************************************************************
 ** Frame concatenated X12 and EDIFACT envelopes without delimiter guessing in data.
 *******************************************************************************/
public class EnvelopeReader
{
   /*******************************************************************************
    ** Read one envelope, respecting UNA and released terminators; null means EOF.
    *******************************************************************************/
   public byte[] next(InputStream input) throws IOException
   {
      int first;
      do
      {
         first = input.read();
      }
      while(first != -1 && Character.isWhitespace(first));
      if(first == -1)
      {
         return (null);
      }
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      bytes.write(first);
      bytes.write(input.readNBytes(2));
      String tag = bytes.toString(StandardCharsets.US_ASCII);
      char terminator;
      char release = 0;
      String end;
      StringBuilder prefix = new StringBuilder();
      if("ISA".equals(tag))
      {
         bytes.write(input.readNBytes(103));
         if(bytes.size() != 106)
         {
            return (bytes.toByteArray());
         }
         terminator = (char) bytes.toByteArray()[105];
         end = "IEA";
      }
      else if("UNA".equals(tag) || "UNB".equals(tag))
      {
         EdifactSeparators separators = EdifactSeparators.DEFAULT;
         if("UNA".equals(tag))
         {
            bytes.write(input.readNBytes(6));
            separators = EdifactSeparators.fromUna(bytes.toString(StandardCharsets.US_ASCII));
         }
         else
         {
            prefix.append("UNB");
         }
         terminator = separators.segment();
         release = separators.release();
         end = "UNZ";
      }
      else
      {
         throw new IOException("Unknown EDI envelope");
      }
      boolean escaped = false;
      int value;
      while((value = input.read()) != -1)
      {
         bytes.write(value);
         if(bytes.size() > 50 * 1024 * 1024)
         {
            throw new IOException("EDI envelope exceeds size limit");
         }
         if(escaped)
         {
            escaped = false;
            continue;
         }
         if(value == release)
         {
            escaped = true;
         }
         else if(value == terminator)
         {
            if(prefix.toString().equals(end))
            {
               break;
            }
            prefix.setLength(0);
         }
         else if(prefix.length() < 3 && !(prefix.isEmpty() && Character.isWhitespace(value)))
         {
            prefix.append((char) value);
         }
      }
      return (bytes.toByteArray());
   }
}
