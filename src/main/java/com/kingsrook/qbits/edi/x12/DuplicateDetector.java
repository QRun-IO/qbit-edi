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


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Windowed interchange and sender-group checks plus in-memory ST02 detection.
 *******************************************************************************/
public class DuplicateDetector
{
   /*******************************************************************************
    ** Check the agreement and complete interchange identity within a rolling window.
    *******************************************************************************/
   public boolean interchange(Integer agreementId, X12Segment isa, int days, Instant now) throws QException
   {
      QQueryFilter filter = EdiRecords.equal("agreementId", agreementId)
         .withCriteria(new QFilterCriteria("senderQualifier", QCriteriaOperator.EQUALS, isa.value(5)))
         .withCriteria(new QFilterCriteria("senderId", QCriteriaOperator.EQUALS, isa.value(6).trim()))
         .withCriteria(new QFilterCriteria("receiverQualifier", QCriteriaOperator.EQUALS, isa.value(7)))
         .withCriteria(new QFilterCriteria("receiverId", QCriteriaOperator.EQUALS, isa.value(8).trim()))
         .withCriteria(new QFilterCriteria("controlNumber", QCriteriaOperator.EQUALS, isa.value(13)))
         .withCriteria(new QFilterCriteria("usageIndicator", QCriteriaOperator.EQUALS, isa.value(15)))
         .withCriteria(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(days, ChronoUnit.DAYS)));
      return (!EdiRecords.query("edi_interchange", filter).isEmpty());
   }

   /*******************************************************************************
    ** EDIFACT duplicates use complete UNB identities, reference and test indicator.
    *******************************************************************************/
   public boolean edifact(Integer agreementId, com.kingsrook.qbits.edi.edifact.EdifactInterchange input, int days, Instant now) throws QException
   {
      return (!EdiRecords.query("edi_interchange", EdiRecords.equal("agreementId", agreementId).withCriteria("direction", QCriteriaOperator.EQUALS, "INBOUND")
         .withCriteria("standard", QCriteriaOperator.EQUALS, "EDIFACT").withCriteria("senderId", QCriteriaOperator.EQUALS, input.unb().value(2))
         .withCriteria("senderQualifier", QCriteriaOperator.EQUALS, input.unb().value(2, 1, 2)).withCriteria("receiverId", QCriteriaOperator.EQUALS, input.unb().value(3))
         .withCriteria("receiverQualifier", QCriteriaOperator.EQUALS, input.unb().value(3, 1, 2)).withCriteria("controlNumber", QCriteriaOperator.EQUALS, input.unb().value(5))
         .withCriteria("usageIndicator", QCriteriaOperator.EQUALS, input.usage()).withCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(days, ChronoUnit.DAYS))).isEmpty());
   }

   /*******************************************************************************
    ** Check GS06 for a sender in the duplicate window.
    *******************************************************************************/
   public boolean group(String senderId, String controlNumber, int days, Instant now) throws QException
   {
      return (!EdiRecords.query("edi_functional_group", EdiRecords.equal("senderId", senderId)
         .withCriteria(new QFilterCriteria("controlNumber", QCriteriaOperator.EQUALS, controlNumber))
         .withCriteria(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, now.minus(days, ChronoUnit.DAYS)))).isEmpty());
   }

   /*******************************************************************************
    ** Return each repeated ST02 once, preserving encounter order.
    *******************************************************************************/
   public List<String> transactions(X12FunctionalGroup group)
   {
      Set<String> seen = new HashSet<>();
      Set<String> duplicates = new LinkedHashSet<>();
      for(X12TransactionSet transaction : group.transactions())
      {
         if(!seen.add(transaction.st().value(2)))
         {
            duplicates.add(transaction.st().value(2));
         }
      }
      return (List.copyOf(duplicates));
   }
}
