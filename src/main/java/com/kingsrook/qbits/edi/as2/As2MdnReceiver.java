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

package com.kingsrook.qbits.edi.as2;


import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.helger.as2lib.crypto.MIC;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.EdiJournal;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Receives asynchronous MDNs for messages we sent: parses the receipt, verifies
 ** its signature against the partner certificates, matches Original-Message-ID to
 ** an outbound message, compares the MIC, then marks the message DELIVERED or
 ** FAILED, stores the raw MDN, writes a timeline event and resolves the overdue
 ** alert. Callers run inside the system context.
 *******************************************************************************/
public class As2MdnReceiver
{
   private static final QLogger LOG = QLogger.getLogger(As2MdnReceiver.class);



   /*******************************************************************************
    ** Handle one inbound MDN; header names are lower case. A receipt that cannot be
    ** authenticated changes nothing.
    *******************************************************************************/
   public As2Receiver.Result receive(EdiTransport transport, Map<String, String> headers, byte[] body) throws Exception
   {
      String from = As2Mdn.normalizeId(headers.get("as2-from"));
      String to = As2Mdn.normalizeId(headers.get("as2-to"));
      if(from == null || to == null || !from.equalsIgnoreCase(transport.getAs2PartnerId()) || !to.equalsIgnoreCase(transport.getAs2LocalId()))
      {
         LOG.warn("AS2 MDN refused for transport " + transport.getName() + ": unknown AS2 identity");
         return (As2Receiver.Result.text(403, "Unknown AS2 identity"));
      }
      As2Mdn.Parsed mdn;
      try
      {
         List<X509Certificate> certificates = transport.getPartnerCertificateId() == null ? List.of() : new CertificateService().partnerVerificationCertificates(transport);
         mdn = As2Mdn.parse(headers.get("content-type"), headers.get("content-transfer-encoding"), body, certificates, Date.from(EdiRuntime.current().config().getClock().instant()));
      }
      catch(As2Exception e)
      {
         return (As2Receiver.Result.text(400, "The MDN could not be read"));
      }
      if((Boolean.TRUE.equals(mdn.signed()) && !Boolean.TRUE.equals(mdn.signatureValid())) || (Boolean.TRUE.equals(transport.getMdnSigned()) && !Boolean.TRUE.equals(mdn.signed())))
      {
         LOG.warn("AS2 MDN refused for transport " + transport.getName() + ": signature check failed");
         return (As2Receiver.Result.text(401, "The MDN signature could not be verified"));
      }
      List<QRecord> messages = mdn.originalMessageId() == null ? List.of() : EdiRecords.query("edi_outbound_message", EdiRecords.equal("transportMessageId", mdn.originalMessageId())
         .withCriteria("transportId", QCriteriaOperator.EQUALS, transport.getId()));
      if(messages.isEmpty())
      {
         return (As2Receiver.Result.text(404, "No message was sent with that Original-Message-ID"));
      }
      String mdnId = As2Mdn.sanitize(As2Mdn.normalizeId(headers.get("message-id")));
      if(mdnId != null && !EdiRecords.query(As2Store.TABLE, EdiRecords.equal("direction", "INBOUND").withCriteria("messageId", QCriteriaOperator.EQUALS, mdnId)
         .withCriteria("originalMessageId", QCriteriaOperator.EQUALS, mdn.originalMessageId())).isEmpty())
      {
         return (new As2Receiver.Result(200, Map.of(), new byte[0]));
      }
      settle(transport, messages.getFirst(), mdn, mdnId, headers, body);
      return (new As2Receiver.Result(200, Map.of(), new byte[0]));
   }



   /*******************************************************************************
    ** Record the receipt and settle the message it answers.
    *******************************************************************************/
   private void settle(EdiTransport transport, QRecord message, As2Mdn.Parsed mdn, String mdnId, Map<String, String> headers, byte[] body) throws Exception
   {
      MIC ours = null;
      try
      {
         ours = MIC.parse(message.getValueString("transportMic"));
      }
      catch(Exception e)
      {
         LOG.warn("The MIC stored for outbound message " + message.getValue("id") + " could not be read");
      }
      String failure = As2Transport.judge(transport, mdn, ours);
      byte[] raw = As2Crypto.entity(headers.get("content-type"), headers.get("content-transfer-encoding"), null, body);
      SystemWrites.transaction(() ->
      {
         PayloadRef ref = As2Store.storeRaw(raw);
         As2Store.insert(new QRecord().withValue("direction", "INBOUND").withValue("messageId", mdnId).withValue("originalMessageId", mdn.originalMessageId())
            .withValue("disposition", mdn.disposition()).withValue("micAlgorithm", mdn.mic() == null ? null : mdn.mic().getDigestAlgorithm().getID())
            .withValue("micValue", As2Store.micValue(mdn.mic())).withValue("signed", mdn.signed()).withValue("signatureValid", mdn.signatureValid()).withValue("payloadRef", ref.value())
            .withValue("transportId", transport.getId()).withValue("partnerId", message.getValue("partnerId")).withValue("outboundMessageId", message.getValue("id")).withValue("deliveryStatus", "NONE"));
         Integer interchangeId = message.getValueInteger("interchangeId");
         EdiJournal.event(null, interchangeId, null, "MDN");
         if(SystemWrites.guard("UPDATE edi_outbound_message SET id=id WHERE id=? AND status='SENT'", message.getValue("id")))
         {
            var now = EdiRuntime.current().config().getClock().instant();
            String status = failure == null ? "DELIVERED" : "FAILED";
            QRecord update = new QRecord().withValue("id", message.getValue("id")).withValue("status", status).withValue("lastError", failure == null ? null : truncate(failure));
            if(failure == null)
            {
               update.withValue("deliveredAt", now);
            }
            SystemWrites.update("edi_outbound_message", update);
            if(interchangeId != null)
            {
               SystemWrites.update("edi_interchange", new QRecord().withValue("id", interchangeId).withValue("status", status));
            }
            EdiJournal.event(null, interchangeId, null, status);
            if(failure != null)
            {
               EdiJournal.exception(null, interchangeId, null, message.getValueInteger("partnerId"), "TRANSPORT", "Outbound delivery failed");
               QAlerts.raise(new AlertSignal("edi.send.failed").withSeverity(AlertSeverity.ERROR).withDedupKey("edi_outbound_message:" + message.getValue("id"))
                  .withTitle("EDI delivery failed").withSummary("Review the outbound message"));
            }
         }
         return (null);
      });
      QAlerts.resolve("edi.mdn.overdue", "edi_outbound_message:" + message.getValue("id"), "MDN received");
   }



   /*******************************************************************************
    ** Keep an error within its column.
    *******************************************************************************/
   private static String truncate(String text)
   {
      return (text.length() <= 255 ? text : text.substring(0, 255));
   }
}
