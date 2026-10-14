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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.EdiJournal;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Reconcile addressed CONTRL controls only within the routed outbound agreement.
 *******************************************************************************/
public class ContrlReader
{
   /*******************************************************************************
    ** Apply functional acceptance/rejection; receipt-only code 8 leaves deadlines open.
    *******************************************************************************/
   public void reconcile(EdifactMessage message, QRecord agreement, QRecord received) throws Exception
   {
      X12Segment uci = message.segments().stream().filter(s -> "UCI".equals(s.id())).findFirst().orElseThrow();
      if("8".equals(uci.value(4)))
      {
         return;
      }
      String overall = status(uci.value(4));
      var candidates = EdiRecords.query("edi_interchange", EdiRecords.equal("agreementId", agreement.getValue("id")).withCriteria("direction", QCriteriaOperator.EQUALS, "OUTBOUND")
         .withCriteria("standard", QCriteriaOperator.EQUALS, "EDIFACT").withCriteria("controlNumber", QCriteriaOperator.EQUALS, uci.value(1))
         .withCriteria("senderId", QCriteriaOperator.EQUALS, uci.value(2)).withCriteria("senderQualifier", QCriteriaOperator.EQUALS, uci.value(2, 1, 2))
         .withCriteria("receiverId", QCriteriaOperator.EQUALS, uci.value(3)).withCriteria("receiverQualifier", QCriteriaOperator.EQUALS, uci.value(3, 1, 2)));
      if(candidates.size() != 1)
      {
         throw new IllegalArgumentException("Acknowledgement reference is unknown or ambiguous");
      }
      QRecord interchange = candidates.getFirst();
      Map<String, X12Segment> details = new LinkedHashMap<>();
      Map<String, String> groupStatuses = new LinkedHashMap<>();
      String currentGroup = "";
      for(var segment : message.segments())
      {
         if("UCF".equals(segment.id()))
         {
            currentGroup = segment.value(1);
            groupStatuses.put(currentGroup, status(segment.value(4)));
         }
         if("UCM".equals(segment.id()))
         {
            String key = currentGroup + ":" + segment.value(1);
            if(details.put(key, segment) != null)
            {
               throw new IllegalArgumentException("Duplicate acknowledgement message reference");
            }
            status(segment.value(3));
         }
      }
      boolean rejected = "R".equals(overall);
      var groups = EdiRecords.query("edi_functional_group", EdiRecords.equal("interchangeId", interchange.getValue("id")));
      for(var group : groups)
      {
         String scope = interchange.getValueInteger("groupCount") == 0 ? "" : group.getValueString("controlNumber");
         String groupStatus = "R".equals(overall) ? "R" : groupStatuses.getOrDefault(scope, overall);
         boolean groupRejected = "R".equals(groupStatus);
         for(var tx : EdiRecords.query("edi_transaction", EdiRecords.equal("functionalGroupId", group.getValue("id"))))
         {
            X12Segment detail = details.remove(scope + ":" + tx.getValueString("controlNumber"));
            if(detail != null && (!detail.value(2).equals(tx.getValueString("transactionSet"))
               || !(detail.value(2, 1, 2) + detail.value(2, 1, 3)).equals(tx.getValueString("version"))))
            {
               throw new IllegalArgumentException("Acknowledgement message identification mismatch");
            }
            String result = "R".equals(groupStatus) || detail != null && "R".equals(status(detail.value(3))) ? "R" : "A";
            groupRejected |= "R".equals(result);
            SystemWrites.update("edi_transaction", new QRecord().withValue("id", tx.getValue("id")).withValue("ackStatus", result)
               .withValue("status", "R".equals(result) ? "REJECTED" : "ACKNOWLEDGED"));
         }
         rejected |= groupRejected;
         SystemWrites.update("edi_functional_group", new QRecord().withValue("id", group.getValue("id")).withValue("ackStatus", groupRejected ? "R" : "A")
            .withValue("ackReceivedAt", EdiRuntime.current().config().getClock().instant()).withValue("ackTransactionId", received.getValue("id")));
      }
      if(!details.isEmpty() || groupStatuses.keySet().stream().anyMatch(c -> groups.stream().noneMatch(g -> c.equals(g.getValueString("controlNumber")))))
      {
         throw new IllegalArgumentException("Acknowledgement contains an unknown message or group reference");
      }
      SystemWrites.update("edi_interchange", new QRecord().withValue("id", interchange.getValue("id")).withValue("status", rejected ? "REJECTED" : "ACKNOWLEDGED"));
      SystemWrites.update("edi_transaction", new QRecord().withValue("id", received.getValue("id")).withValue("status", "ACKNOWLEDGED").withValue("ackStatus", rejected ? "R" : "A"));
      for(var group : groups)
      {
         QAlerts.resolve("edi.ack.overdue", "edi_functional_group:" + group.getValue("id"), "CONTRL received");
      }
      for(var exception : EdiRecords.query("edi_exception", EdiRecords.equal("interchangeId", interchange.getValue("id")).withCriteria("type", QCriteriaOperator.EQUALS, "OVERDUE_ACK")
         .withCriteria("status", QCriteriaOperator.IN, "OPEN", "IN_PROGRESS")))
      {
         SystemWrites.update("edi_exception", new QRecord().withValue("id", exception.getValue("id")).withValue("status", "RESOLVED")
            .withValue("resolvedAt", EdiRuntime.current().config().getClock().instant()).withValue("resolution", "CONTRL received"));
      }
      if(rejected)
      {
         EdiJournal.exception(null, interchange.getValueInteger("id"), null, agreement.getValueInteger("partnerId"), "REJECTED_ACK", "Partner rejected an outbound EDIFACT document");
         QAlerts.raise(new AlertSignal("edi.ack.rejected").withSeverity(AlertSeverity.ERROR).withDedupKey("edi_interchange:" + interchange.getValue("id"))
            .withTitle("EDI acknowledgement rejected").withSummary("Review the outbound interchange"));
      }
      EdiJournal.event(null, interchange.getValueInteger("id"), received.getValueInteger("id"), "ACK_RECEIVED");
   }

   /*******************************************************************************
    ** Only functional acceptance and rejection can complete an expected response.
    *******************************************************************************/
   private String status(String action)
   {
      return (switch(action)
      {
         case "7" -> "A";
         case "4" -> "R";
         default -> throw new IllegalArgumentException("Unsupported CONTRL action");
      });
   }
}
