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


import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;


/*******************************************************************************
 ** Generate an X12 999 functional acknowledgement.
 *******************************************************************************/
public class Ack999Builder
{
   /*******************************************************************************
    ** Build transaction detail at the configured acknowledgement level.
    *******************************************************************************/
   public X12TransactionSet build(X12FunctionalGroup group, Map<String, List<ValidationFinding>> findings, AckLevel level, String controlNumber)
   {
      return (FunctionalAckBuilder.build(true, group, findings, level, controlNumber));
   }
}
