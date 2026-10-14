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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.crypto.MIC;
import com.helger.as2lib.disposition.DispositionOptions;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.as2.CertificateService.LocalKey;
import com.kingsrook.qbits.edi.flow.EdiJournal;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import jakarta.mail.internet.MimeBodyPart;


/*******************************************************************************
 ** Receives AS2 messages for one transport: checks the AS2 identities, decrypts
 ** with our ACTIVE or NEXT keys, verifies the signature against the partner
 ** certificates, computes the MIC, hands the document to the inbox, and answers
 ** with a synchronous MDN or queues an asynchronous one. A retransmitted
 ** Message-ID gets the original MDN again and is not reprocessed. Callers run
 ** inside the system context; failures are reported as RFC 4130 dispositions.
 *******************************************************************************/
public class As2Receiver
{
   private static final QLogger LOG = QLogger.getLogger(As2Receiver.class);



   /*******************************************************************************
    ** An HTTP answer: status, headers and body.
    *******************************************************************************/
   public record Result(int status, Map<String, String> headers, byte[] body)
   {
      /*******************************************************************************
       ** A plain-text answer.
       *******************************************************************************/
      static Result text(int status, String text)
      {
         return (new Result(status, Map.of("Content-Type", "text/plain; charset=utf-8"), text.getBytes(StandardCharsets.UTF_8)));
      }
   }



   /*******************************************************************************
    ** What was asked of us for this message.
    *******************************************************************************/
   private record Request(String messageId, boolean mdnRequested, String deliveryUrl, boolean signMdn, ECryptoAlgorithmSign micAlgorithm)
   {
      /*******************************************************************************
       ** The same request, answered in the HTTP response only. Used before the sender
       ** is authenticated, so an unauthenticated sender cannot aim a receipt at an
       ** address of its choosing.
       *******************************************************************************/
      Request synchronous()
      {
         return (new Request(messageId, mdnRequested, null, signMdn, micAlgorithm));
      }
   }



   /*******************************************************************************
    ** Handle one inbound message; header names are lower case.
    *******************************************************************************/
   public Result receive(EdiTransport transport, Map<String, String> headers, byte[] body, String remoteAddress) throws Exception
   {
      String messageId = As2Mdn.normalizeId(headers.get("message-id"));
      String from = As2Mdn.normalizeId(headers.get("as2-from"));
      String to = As2Mdn.normalizeId(headers.get("as2-to"));
      if(messageId == null || messageId.isBlank() || messageId.length() > 250 || !messageId.matches("[\\x21-\\x7E]+") || from == null || to == null)
      {
         return (Result.text(400, "The AS2-From, AS2-To and Message-ID headers are required"));
      }
      if(!from.equalsIgnoreCase(transport.getAs2PartnerId()) || !to.equalsIgnoreCase(transport.getAs2LocalId()))
      {
         LOG.warn("AS2 message refused for transport " + transport.getName() + ": unknown AS2 identity");
         return (Result.text(403, "Unknown AS2 identity"));
      }
      Request request = request(transport, headers, messageId);
      if(request.deliveryUrl() != null && !deliveryUrlAllowed(transport, request.deliveryUrl()))
      {
         return (Result.text(400, "The Receipt-Delivery-Option is not acceptable"));
      }
      CertificateService certificates = new CertificateService();
      CertificateService.KeySet keys = certificates.localDecryptionKeys();
      As2Crypto.Decoded decoded;
      try
      {
         List<X509Certificate> partnerCertificates = transport.getPartnerCertificateId() == null ? List.of() : certificates.partnerVerificationCertificates(transport);
         byte[] top = As2Crypto.entity(headers.get("content-type"), headers.get("content-transfer-encoding"), headers.get("content-disposition"), body);
         decoded = As2Crypto.decode(top, keys.keys(), partnerCertificates, request.micAlgorithm(), Date.from(EdiRuntime.current().config().getClock().instant()));
         if((transport.getSignAlgorithm() != null && !decoded.signed()) || (transport.getEncryptAlgorithm() != null && !decoded.encrypted()))
         {
            throw (new As2Exception("The message does not meet the transport security requirements", As2Exception.INSUFFICIENT_SECURITY));
         }
      }
      catch(As2Exception e)
      {
         boolean ourFault = e.getCode() == null || (As2Exception.DECRYPTION_FAILED.equals(e.getCode()) && !keys.problems().isEmpty());
         return (reject(transport, request.synchronous(), ourFault ? As2Exception.UNEXPECTED_PROCESSING : e.getCode(), certificates));
      }
      String externalId = externalId(transport, messageId);
      QRecord replay = alreadyAnswered(transport, request, externalId);
      if(replay != null)
      {
         return (replay(transport, request, replay));
      }
      QRecord inbox;
      try
      {
         inbox = new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("AS2", externalId, decoded.fileName(), remoteAddress, new ByteArrayInputStream(decoded.payload())));
         if(inbox.getValue("transportId") == null)
         {
            SystemWrites.update("edi_inbox", new QRecord().withValue("id", inbox.getValue("id")).withValue("transportId", transport.getId()));
         }
      }
      catch(Exception e)
      {
         LOG.warn("AS2 intake failed for transport " + transport.getName());
         return (reject(transport, request, As2Exception.UNEXPECTED_PROCESSING, certificates));
      }
      if(!request.mdnRequested())
      {
         return (new Result(200, Map.of(), new byte[0]));
      }
      try
      {
         return (answer(transport, request, As2Mdn.PROCESSED, decoded.mic(), inbox.getValueInteger("id"), certificates));
      }
      catch(As2Exception e)
      {
         LOG.warn("AS2 receipt could not be produced for transport " + transport.getName() + ": " + e.getMessage());
         return (Result.text(500, "The receipt could not be produced"));
      }
   }



   /*******************************************************************************
    ** Read what the message asks for: whether a receipt is wanted, where an
    ** asynchronous one goes, and whether and how to sign it.
    *******************************************************************************/
   private Request request(EdiTransport transport, Map<String, String> headers, String messageId)
   {
      DispositionOptions options = new DispositionOptions();
      try
      {
         options = DispositionOptions.createFromString(headers.get("disposition-notification-options"));
      }
      catch(Exception e)
      {
         LOG.warn("AS2 disposition options ignored for transport " + transport.getName());
      }
      ECryptoAlgorithmSign algorithm = options.getFirstMICAlg();
      if(algorithm == null)
      {
         algorithm = As2Crypto.signAlgorithm(transport.getSignAlgorithm()) == null ? ECryptoAlgorithmSign.DIGEST_SHA_256 : As2Crypto.signAlgorithm(transport.getSignAlgorithm());
      }
      String delivery = headers.get("receipt-delivery-option");
      return (new Request(messageId, headers.get("disposition-notification-to") != null, delivery == null || delivery.isBlank() ? null : delivery.trim(),
         options.getProtocol() != null || Boolean.TRUE.equals(transport.getMdnSigned()), algorithm));
   }



   /*******************************************************************************
    ** An asynchronous receipt address must use TLS (loopback excepted) and, unless
    ** the transport requires signed messages, must be on the partner's own host,
    ** so an unauthenticated sender cannot aim our receipts at third parties.
    *******************************************************************************/
   static boolean deliveryUrlAllowed(EdiTransport transport, String url)
   {
      if(!As2Http.isAllowedUrl(url))
      {
         return (false);
      }
      if(transport.getSignAlgorithm() != null)
      {
         return (true);
      }
      try
      {
         return (transport.getAs2Url() != null && java.net.URI.create(url).getHost().equalsIgnoreCase(java.net.URI.create(transport.getAs2Url()).getHost()));
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** The inbox dedupe key: the Message-ID. Message-IDs are unique per sender, so if
    ** the same Message-ID was already received on a different transport (another
    ** partner), the key is scoped by the transport id so neither message is lost.
    *******************************************************************************/
   private static String externalId(EdiTransport transport, String messageId) throws Exception
   {
      List<QRecord> rows = EdiRecords.query("edi_inbox", EdiRecords.equal("transportType", "AS2").withCriteria("externalId", QCriteriaOperator.EQUALS, messageId));
      boolean foreign = !rows.isEmpty() && rows.getFirst().getValue("transportId") != null && !rows.getFirst().getValue("transportId").equals(transport.getId());
      return (foreign ? transport.getId() + ":" + messageId : messageId);
   }



   /*******************************************************************************
    ** The receipt row we already sent for this Message-ID (a retransmission), or
    ** null when the original was never answered.
    *******************************************************************************/
   private QRecord alreadyAnswered(EdiTransport transport, Request request, String externalId) throws Exception
   {
      if(EdiRecords.query("edi_inbox", EdiRecords.equal("transportType", "AS2").withCriteria("externalId", QCriteriaOperator.EQUALS, externalId)).isEmpty())
      {
         return (null);
      }
      List<QRecord> rows = EdiRecords.query(As2Store.TABLE, EdiRecords.equal("direction", "OUTBOUND").withCriteria("originalMessageId", QCriteriaOperator.EQUALS, request.messageId())
         .withCriteria("transportId", QCriteriaOperator.EQUALS, transport.getId()));
      return (rows.isEmpty() ? null : rows.getFirst());
   }



   /*******************************************************************************
    ** Answer a retransmission with the original receipt, unchanged. An
    ** asynchronous receipt is queued to be delivered again.
    *******************************************************************************/
   private Result replay(EdiTransport transport, Request request, QRecord original) throws Exception
   {
      if(!request.mdnRequested())
      {
         return (new Result(200, Map.of(), new byte[0]));
      }
      if(request.deliveryUrl() != null)
      {
         SystemWrites.update(As2Store.TABLE, new QRecord().withValue("id", original.getValue("id")).withValue("deliveryUrl", request.deliveryUrl()).withValue("deliveryStatus", "PENDING")
            .withValue("deliveryAttempts", 0).withValue("nextAttemptAt", null));
         return (new Result(200, Map.of(), new byte[0]));
      }
      byte[] raw;
      try(InputStream stream = EdiRuntime.current().payloads().open(new PayloadRef(original.getValueString("payloadRef"), null, 0)))
      {
         raw = stream.readAllBytes();
      }
      return (syncResult(transport, original.getValueString("messageId"), As2Crypto.splitRaw(raw)));
   }



   /*******************************************************************************
    ** Reject a message that could not be accepted. With a receipt requested the
    ** failure is reported in an MDN so the partner can act or retry; otherwise an
    ** HTTP error answers. No inbox row exists for a rejected message.
    *******************************************************************************/
   private Result reject(EdiTransport transport, Request request, String code, CertificateService certificates) throws Exception
   {
      LOG.warn("AS2 message rejected for transport " + transport.getName() + ": " + code);
      if(!request.mdnRequested())
      {
         return (Result.text(As2Exception.UNEXPECTED_PROCESSING.equals(code) ? 500 : 400, "AS2 message rejected: " + code));
      }
      try
      {
         return (answer(transport, request, As2Mdn.failure(code), null, null, certificates));
      }
      catch(As2Exception e)
      {
         LOG.warn("AS2 receipt could not be produced for transport " + transport.getName() + ": " + e.getMessage());
         return (Result.text(500, "The receipt could not be produced"));
      }
   }



   /*******************************************************************************
    ** Build, store and answer or queue the MDN.
    *******************************************************************************/
   private Result answer(EdiTransport transport, Request request, String disposition, MIC mic, Integer inboxId, CertificateService certificates) throws Exception
   {
      LocalKey key = request.signMdn() ? certificates.activeLocal(transport) : null;
      String mdnId = "edi-mdn-" + UUID.randomUUID() + "@" + transport.getAs2LocalId().replaceAll("[^A-Za-z0-9.-]", "-");
      As2Mdn.Built built = As2Mdn.build(mdnId, transport.getAs2LocalId(), request.messageId(), disposition, mic, key, request.micAlgorithm());
      PayloadRef raw = As2Store.storeRaw(built.entity());
      Instant now = EdiRuntime.current().config().getClock().instant();
      QRecord row = As2Store.insert(new QRecord().withValue("direction", "OUTBOUND").withValue("messageId", mdnId).withValue("originalMessageId", request.messageId())
         .withValue("disposition", disposition).withValue("micAlgorithm", mic == null ? null : mic.getDigestAlgorithm().getID()).withValue("micValue", As2Store.micValue(mic))
         .withValue("signed", built.signed()).withValue("payloadRef", raw.value()).withValue("receivedAt", now).withValue("transportId", transport.getId())
         .withValue("partnerId", transport.getPartnerId()).withValue("inboxId", inboxId).withValue("deliveryStatus", request.deliveryUrl() == null ? "NONE" : "PENDING")
         .withValue("deliveryUrl", request.deliveryUrl()).withValue("deliveryAttempts", 0).withValue("nextAttemptAt", null));
      if(inboxId != null)
      {
         EdiJournal.event(inboxId, null, null, "MDN");
      }
      LOG.info("AS2 receipt " + row.getValue("id") + " " + (request.deliveryUrl() == null ? "returned" : "queued") + " for transport " + transport.getName());
      return (request.deliveryUrl() != null ? new Result(200, Map.of(), new byte[0]) : syncResult(transport, mdnId, built.wire()));
   }



   /*******************************************************************************
    ** The synchronous MDN response with its AS2 headers.
    *******************************************************************************/
   private Result syncResult(EdiTransport transport, String mdnId, As2Crypto.Wire wire)
   {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("AS2-Version", "1.2");
      headers.put("AS2-From", As2Transport.asId(transport.getAs2LocalId()));
      headers.put("AS2-To", As2Transport.asId(transport.getAs2PartnerId()));
      headers.put("Message-ID", "<" + mdnId + ">");
      headers.put("MIME-Version", "1.0");
      headers.put("Content-Type", wire.contentType());
      if(wire.transferEncoding() != null)
      {
         headers.put("Content-Transfer-Encoding", wire.transferEncoding());
      }
      return (new Result(200, headers, wire.body()));
   }
}
