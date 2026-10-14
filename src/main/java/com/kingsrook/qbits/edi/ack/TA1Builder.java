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


import java.time.LocalDateTime;
import java.util.List;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;


/*******************************************************************************
 ** TA1 is an interchange-level service segment, never an ST/SE transaction.
 *******************************************************************************/
public class TA1Builder
{
   /*******************************************************************************
    ** Select the first interchange finding, or acceptance when there are none.
    *******************************************************************************/
   public X12Segment build(X12Segment isa, List<com.kingsrook.qbits.edi.x12.EnvelopeFinding> findings)
   {
      return (build(isa, findings.stream().filter(f -> "TA1".equals(f.level())).map(com.kingsrook.qbits.edi.x12.EnvelopeFinding::code).findFirst().orElse("000")));
   }

   /*******************************************************************************
    ** Acknowledge original ISA control, date and time with a TA1 note code.
    *******************************************************************************/
   public X12Segment build(X12Segment isa, String noteCode)
   {
      return (X12Segment.of("TA1", 2, isa.value(13), isa.value(9), isa.value(10), "000".equals(noteCode) ? "A" : "R", noteCode));
   }

   /*******************************************************************************
    ** Generate a reply interchange with swapped identities and its own ISA13.
    *******************************************************************************/
   public X12Interchange interchange(X12Interchange received, String control, String noteCode, LocalDateTime at)
   {
      X12Segment isa = AckEnvelope.replyIsa(received.isa(), control, at);
      return (new X12Interchange(received.separators(), isa, X12Segment.of("IEA", 3, "0", isa.value(13)), List.of(), List.of(build(received.isa(), noteCode)), List.of()));
   }
}
