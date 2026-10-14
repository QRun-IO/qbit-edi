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
import com.kingsrook.qbits.edi.document.EdiDocument;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qbits.edi.x12.X12Segment;

/*******************************************************************************
 ** UNH/UNT message with its complete directory and association identification.
 *******************************************************************************/
public record EdifactMessage(X12Segment header, X12Segment unt, List<X12Segment> segments, List<EnvelopeFinding> findings) implements EdiDocument
{
   /*******************************************************************************
    ** Agreement document key, for example ORDERS.
    *******************************************************************************/
   public String type()
   {
      return (header.value(2));
   }

   /*******************************************************************************
    ** Agreement directory key, for example D96A.
    *******************************************************************************/
   public String version()
   {
      return (header.value(2, 1, 2) + header.value(2, 1, 3));
   }

   /*******************************************************************************
    ** Message reference, unique within its interchange.
    *******************************************************************************/
   public String reference()
   {
      return (header.value(1));
   }
}
