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

package com.kingsrook.qbits.edi.guide;




/*******************************************************************************
 ** Structured validation result; PHI values must never appear in text or excerpts.
 *******************************************************************************/
public record ValidationFinding(int segmentPosition, String segmentId, String loopId, Integer elementPosition, Integer componentPosition, int snipLevel, String ak3Code, String ak4Code, String ik3Code, String ik4Code, String severity, String message, String valueExcerpt, AckContext context)
{
   /*******************************************************************************
    ** Construct a finding without situational trigger context.
    *******************************************************************************/
   public ValidationFinding(int segmentPosition, String segmentId, String loopId, Integer elementPosition, Integer componentPosition, int snipLevel,
      String ak3Code, String ak4Code, String ik3Code, String ik4Code, String severity, String message, String valueExcerpt)
   {
      this(segmentPosition, segmentId, loopId, elementPosition, componentPosition, snipLevel, ak3Code, ak4Code, ik3Code, ik4Code, severity, message, valueExcerpt, null);
   }

   /*******************************************************************************
    ** Attach only positional acknowledgement context.
    *******************************************************************************/
   public ValidationFinding withContext(AckContext context)
   {
      return (new ValidationFinding(segmentPosition, segmentId, loopId, elementPosition, componentPosition, snipLevel, ak3Code, ak4Code, ik3Code, ik4Code, severity, message, valueExcerpt, context));
   }
}
