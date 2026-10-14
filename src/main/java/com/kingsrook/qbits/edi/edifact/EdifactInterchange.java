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

import java.util.List;
import java.util.stream.Stream;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qbits.edi.x12.X12Segment;

/*******************************************************************************
 ** EDIFACT service envelope with optional groups and ungrouped messages.
 *******************************************************************************/
public record EdifactInterchange(EdifactSeparators separators, X12Segment unb, X12Segment unz,
   List<EdifactGroup> groups, List<EdifactMessage> messages, List<EnvelopeFinding> findings)
{
   /*******************************************************************************
    ** Visit every message without inventing a functional group on the wire.
    *******************************************************************************/
   public List<EdifactMessage> allMessages()
   {
      return (Stream.concat(messages.stream(), groups.stream().flatMap(g -> g.messages().stream())).toList());
   }

   /*******************************************************************************
    ** Normalize the UNB test flag to the agreement's usage vocabulary.
    *******************************************************************************/
   public String usage()
   {
      return ("1".equals(unb.value(11)) ? "T" : "P");
   }
}
