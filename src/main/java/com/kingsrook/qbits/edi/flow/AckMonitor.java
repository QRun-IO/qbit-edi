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

package com.kingsrook.qbits.edi.flow;

import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Five-minute acknowledgement and public-certificate expiry monitor.
 *******************************************************************************/
public class AckMonitor
{
   /*******************************************************************************
    ** Deduplicate alerts and open exceptions without embedding document values.
    *******************************************************************************/
   public Integer runOnce() throws QException
   {
      return (SystemWrites.call(() ->
      {
         int count = 0;
         var now = EdiRuntime.current().config().getClock().instant();
         for(QRecord group : EdiRecords.query("edi_functional_group", EdiRecords.equal("ackStatus", "PENDING")
            .withCriteria("ackDueAt", QCriteriaOperator.LESS_THAN, now).withCriteria("ackReceivedAt", QCriteriaOperator.IS_BLANK)))
         {
            QRecord interchange = OutboundWorker.row("edi_interchange", group.getValue("interchangeId"));
            if(!"OUTBOUND".equals(interchange.getValueString("direction")))
            {
               continue;
            }
            String key = "edi_functional_group:" + group.getValue("id");
            QAlerts.raise(new AlertSignal("edi.ack.overdue").withSeverity(AlertSeverity.WARNING).withDedupKey(key)
               .withRecordRef(new com.kingsrook.qqq.backend.core.alerts.AlertRecordRef("edi_functional_group", group.getValue("id")))
               .withTitle("EDI acknowledgement overdue").withSummary("Review the outbound functional group"));
            if(EdiRecords.query("edi_exception", EdiRecords.equal("interchangeId", interchange.getValue("id")).withCriteria("type", QCriteriaOperator.EQUALS, "OVERDUE_ACK")
               .withCriteria("status", QCriteriaOperator.IN, "OPEN", "IN_PROGRESS")).isEmpty())
            {
               EdiJournal.exception(null, interchange.getValueInteger("id"), null, interchange.getValueInteger("partnerId"), "OVERDUE_ACK", "Expected acknowledgement has not arrived");
            }
            count++;
         }
         for(QRecord certificate : EdiRecords.query("edi_certificate", EdiRecords.equal("status", "ACTIVE")))
         {
            if(certificate.getValueInstant("notAfter") == null)
            {
               continue;
            }
            long days = ChronoUnit.DAYS.between(now, certificate.getValueInstant("notAfter"));
            if(!certificate.getValueInstant("notAfter").isAfter(now))
            {
               expiry(certificate, "edi.certificate.expired", "expired", AlertSeverity.ERROR);
            }
            else
            {
               for(int threshold : List.of(30, 14, 7))
               {
                  if(days < threshold)
                  {
                     expiry(certificate, "edi.certificate.expiring", String.valueOf(threshold), AlertSeverity.WARNING);
                  }
               }
            }
         }
         return (count);
      }));
   }

   /*******************************************************************************
    ** Each certificate threshold has its own stable deduplication key.
    *******************************************************************************/
   private static void expiry(QRecord certificate, String type, String threshold, AlertSeverity severity)
   {
      QAlerts.raise(new AlertSignal(type).withSeverity(severity).withDedupKey("edi_certificate:" + certificate.getValue("id") + ":" + threshold)
         .withRecordRef(new com.kingsrook.qqq.backend.core.alerts.AlertRecordRef("edi_certificate", certificate.getValue("id")))
         .withTitle("EDI certificate expiry").withSummary("Review the certificate validity dates"));
   }
}
