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
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;


/*******************************************************************************
 ** Address acknowledgements to the original sender using newly allocated controls.
 *******************************************************************************/
public final class AckEnvelope
{
   /*******************************************************************************
    ** Swap interchange identities while preserving standard version and usage.
    *******************************************************************************/
   public static X12Segment replyIsa(X12Segment original, String control, LocalDateTime at)
   {
      return (X12Segment.of("ISA", 1, "00", "", "00", "", original.value(7), original.value(8), original.value(5), original.value(6),
         at.format(DateTimeFormatter.ofPattern("yyMMdd")), at.format(DateTimeFormatter.ofPattern("HHmm")), original.value(11), original.value(12),
         "0".repeat(Math.max(0, 9 - control.length())) + control, "0", original.value(15), original.value(16)));
   }

   /*******************************************************************************
    ** Wrap an acknowledgement transaction in a reply functional group and interchange.
    *******************************************************************************/
   public static X12Interchange wrap(X12Interchange received, X12FunctionalGroup group, X12TransactionSet acknowledgement, String isaControl, String gsControl, LocalDateTime at)
   {
      X12Segment isa = replyIsa(received.isa(), isaControl, at);
      String version = acknowledgement.st().value(1).equals("999") ? "005010X231A1" : group.gs().value(8);
      X12Segment gs = X12Segment.of("GS", 2, "FA", group.gs().value(3), group.gs().value(2), at.format(DateTimeFormatter.BASIC_ISO_DATE),
         at.format(DateTimeFormatter.ofPattern("HHmm")), gsControl, "X", version);
      X12FunctionalGroup reply = new X12FunctionalGroup(gs, X12Segment.of("GE", 0, "1", gsControl), List.of(acknowledgement), List.of());
      return (new X12Interchange(received.separators(), isa, X12Segment.of("IEA", 0, "1", isa.value(13)), List.of(reply), List.of(), List.of()));
   }
}
