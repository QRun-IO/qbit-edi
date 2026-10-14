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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;


/*******************************************************************************
 ** Read acknowledgement controls and outcomes for later reconciliation.
 *******************************************************************************/
public class InboundAckReader
{
   /*******************************************************************************
    ** Parsed functional or interchange acknowledgement.
    *******************************************************************************/
   public record Result(String type, String controlNumber, String status, Map<String, String> transactionStatuses, String noteCode)
   {
   }

   /*******************************************************************************
    ** Read all received acknowledgements in an interchange.
    *******************************************************************************/
   public List<Result> read(X12Interchange interchange)
   {
      List<Result> results = new ArrayList<>();
      for(X12Segment segment : interchange.serviceSegments())
      {
         if(segment.id().equals("TA1"))
         {
            results.add(new Result("TA1", segment.value(1), segment.value(4), Map.of(), segment.value(5)));
         }
      }
      for(X12FunctionalGroup group : interchange.groups())
      {
         for(X12TransactionSet transaction : group.transactions())
         {
            if(List.of("997", "999").contains(transaction.st().value(1)))
            {
               results.add(read(transaction));
            }
         }
      }
      return (results);
   }

   /*******************************************************************************
    ** Extract AK1, AK2, AK5/IK5 and AK9 without acknowledging the ack.
    *******************************************************************************/
   public Result read(X12TransactionSet transaction)
   {
      String control = null;
      String status = null;
      String stControl = null;
      Map<String, String> statuses = new LinkedHashMap<>();
      for(X12Segment segment : transaction.segments())
      {
         switch(segment.id())
         {
            case "AK1" -> control = segment.value(2);
            case "AK2" -> stControl = segment.value(2);
            case "AK5", "IK5" ->
            {
               if(stControl != null)
               {
                  statuses.put(stControl, segment.value(1));
               }
            }
            case "AK9" -> status = segment.value(1);
            default ->
            {
               // Detail segments do not change reconciliation status.
            }
         }
      }
      if(control == null || status == null)
      {
         throw new IllegalArgumentException("Incomplete functional acknowledgement");
      }
      return (new Result(transaction.st().value(1), control, status, Map.copyOf(statuses), null));
   }
}
