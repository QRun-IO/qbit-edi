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


import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.crypto.MIC;
import com.helger.as2lib.disposition.DispositionOptions;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.as2.CertificateService.LocalKey;
import com.kingsrook.qbits.edi.flow.EdiJournal;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.transport.EdiSendResult;
import com.kingsrook.qbits.edi.transport.EdiTestResult;
import com.kingsrook.qbits.edi.transport.EdiTransportPlugin;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** AS2 (RFC 4130) transport. Sending builds the message (compress, sign, then
 ** encrypt), POSTs it, and for a synchronous receipt verifies the MDN signature,
 ** the received-content MIC and the disposition. An asynchronous receipt leaves
 ** the message SENT until the MDN arrives at the receipt hook. No local files are
 ** used; keys come from environment variables through the secrets resolver.
 *******************************************************************************/
public class As2Transport implements EdiTransportPlugin
{
   public static final String TYPE = "AS2";

   private static final String ID_PATTERN = "[A-Za-z0-9 ._@:/-]{1,128}";
   private static final Duration TIMEOUT = Duration.ofSeconds(60);



   /*******************************************************************************
    ** The prepared wire message and the MIC computed for it.
    *******************************************************************************/
   private record Prepared(As2Crypto.Wire wire, MIC mic)
   {
   }



   /*******************************************************************************
    ** Registry key.
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (TYPE);
   }



   /*******************************************************************************
    ** Check identifiers, URL, algorithms, receipt mode and certificate ownership
    ** without resolving any secret. SHA1 and 3DES are accepted only when set
    ** explicitly; they set the weakCrypto flag on the record.
    *******************************************************************************/
   @Override
   public void validate(EdiTransport transport, List<String> errors)
   {
      boolean outbound = !"INBOUND".equals(transport.getDirection());
      if(transport.getAs2LocalId() == null || !transport.getAs2LocalId().matches(ID_PATTERN))
      {
         errors.add("as2LocalId is required (letters, digits, spaces and . _ @ : / - only)");
      }
      if(transport.getAs2PartnerId() == null || !transport.getAs2PartnerId().matches(ID_PATTERN))
      {
         errors.add("as2PartnerId is required (letters, digits, spaces and . _ @ : / - only)");
      }
      if(outbound && (transport.getAs2Url() == null || !As2Http.isAllowedUrl(transport.getAs2Url())))
      {
         errors.add("as2Url must use TLS (HTTP is allowed on loopback only) and carry no credentials");
      }
      try
      {
         As2Crypto.signAlgorithm(transport.getSignAlgorithm());
      }
      catch(IllegalArgumentException e)
      {
         errors.add("Unsupported signAlgorithm");
      }
      try
      {
         As2Crypto.encryptAlgorithm(transport.getEncryptAlgorithm());
      }
      catch(IllegalArgumentException e)
      {
         errors.add("Unsupported encryptAlgorithm");
      }
      if(transport.getMdnMode() != null && !List.of("SYNC", "ASYNC", "NONE").contains(transport.getMdnMode()))
      {
         errors.add("Unsupported mdnMode");
      }
      if(Boolean.TRUE.equals(transport.getCompress()) && transport.getSignAlgorithm() == null && transport.getEncryptAlgorithm() == null)
      {
         errors.add("compress requires signing or encryption");
      }
      if(transport.getPartnerId() == null && !Boolean.TRUE.equals(transport.getIsVan()))
      {
         errors.add("partnerId is required unless the transport is a VAN (isVan)");
      }
      if((transport.getHttpAuthUserEnvVar() == null) != (transport.getHttpAuthPasswordEnvVar() == null))
      {
         errors.add("httpAuthUserEnvVar and httpAuthPasswordEnvVar must be set together");
      }
      if(outbound && "ASYNC".equals(transport.getMdnMode()) && baseUrl() == null)
      {
         errors.add("ASYNC receipts need the qbit publicBaseUrl configuration");
      }
      try
      {
         validateCertificates(transport, errors);
      }
      catch(Exception e)
      {
         errors.add("Certificates could not be checked");
      }
   }



   /*******************************************************************************
    ** Certificate references must exist and have the right owner: ours (no
    ** partner, with a key variable) as the local certificate; the partner's own, or
    ** for a VAN a certificate that is not ours, as the partner certificate; a TLS
    ** certificate for pinning. Retired certificates are refused.
    *******************************************************************************/
   private static void validateCertificates(EdiTransport transport, List<String> errors) throws Exception
   {
      boolean secured = transport.getSignAlgorithm() != null || transport.getEncryptAlgorithm() != null || Boolean.TRUE.equals(transport.getMdnSigned());
      QRecord local = certificate(transport.getLocalCertificateId());
      QRecord partner = certificate(transport.getPartnerCertificateId());
      QRecord tls = certificate(transport.getPartnerTlsCertificateId());
      if(transport.getLocalCertificateId() == null ? secured : local == null)
      {
         errors.add(transport.getLocalCertificateId() == null ? "localCertificateId is required when signing or encrypting" : "localCertificateId does not exist");
      }
      else if(local != null && (local.getValue("partnerId") != null || local.getValueString("privateKeyEnvVar") == null || "RETIRED".equals(local.getValueString("status"))))
      {
         errors.add("localCertificateId must be one of our own certificates (no partner, with a private key variable) that is not retired");
      }
      if(transport.getPartnerCertificateId() == null ? secured : partner == null)
      {
         errors.add(transport.getPartnerCertificateId() == null ? "partnerCertificateId is required when signing or encrypting" : "partnerCertificateId does not exist");
      }
      else if(partner != null)
      {
         boolean ownedByPartner = transport.getPartnerId() == null || transport.getPartnerId().equals(partner.getValueInteger("partnerId"));
         if(!ownedByPartner || partner.getValueString("privateKeyEnvVar") != null || "RETIRED".equals(partner.getValueString("status")))
         {
            errors.add("partnerCertificateId must be a certificate of the partner (or, for a VAN, of the VAN) that is not retired and holds no private key");
         }
      }
      if(transport.getPartnerTlsCertificateId() != null && (tls == null || !"TLS".equals(tls.getValueString("usage"))))
      {
         errors.add("partnerTlsCertificateId must be a certificate with usage TLS");
      }
   }



   /*******************************************************************************
    ** Certificate row by id, or null.
    *******************************************************************************/
   private static QRecord certificate(Integer id) throws Exception
   {
      if(id == null)
      {
         return (null);
      }
      return (SystemWrites.call(() -> EdiRecords.query(CertificateService.TABLE, EdiRecords.equal("id", id)).stream().findFirst().orElse(null)));
   }



   /*******************************************************************************
    ** The deployment public base URL without a trailing slash, or null.
    *******************************************************************************/
   static String baseUrl()
   {
      try
      {
         String url = EdiRuntime.current().config().getPublicBaseUrl();
         return (url == null || url.isBlank() ? null : url.replaceAll("/+$", ""));
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** Build, sign, encrypt and POST the message; settle it from the receipt.
    *******************************************************************************/
   @Override
   public EdiSendResult send(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport) throws Exception
   {
      return (SystemWrites.call(() -> deliver(message, payload, transport)));
   }



   /*******************************************************************************
    ** The send itself, inside the system context. Configuration problems (a
    ** missing variable, an expired or missing certificate) are RETRY results that
    ** name only the problem, so the retry policy and alerts apply once fixed.
    *******************************************************************************/
   private EdiSendResult deliver(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport) throws Exception
   {
      byte[] edi;
      try(InputStream stream = EdiRuntime.current().payloads().open(payload))
      {
         edi = stream.readAllBytes();
      }
      String messageId = messageId(message.getId(), transport, edi);
      CertificateService certificates = new CertificateService();
      String mode = mode(transport);
      As2Http.Response response;
      Prepared prepared;
      try
      {
         prepared = prepare(message, transport, edi, messageId, certificates);
         HttpClient client = As2Http.client(certificates.tlsPin(transport));
         response = As2Http.post(client, URI.create(transport.getAs2Url()), headers(transport, messageId, prepared, mode), prepared.wire().body(), TIMEOUT);
      }
      catch(As2Exception e)
      {
         return (new EdiSendResult("RETRY", null, e.getMessage()));
      }
      catch(java.io.IOException e)
      {
         return (new EdiSendResult("RETRY", null, "The AS2 endpoint could not be reached"));
      }
      if(response.status() >= 500 || response.status() == 429)
      {
         return (new EdiSendResult("RETRY", null, "HTTP " + response.status()));
      }
      if(response.status() < 200 || response.status() >= 300)
      {
         return (new EdiSendResult("FAILED", null, "HTTP " + response.status()));
      }
      SystemWrites.update("edi_outbound_message", new QRecord().withValue("id", message.getId()).withValue("transportMic", prepared.mic().getAsAS2String()));
      if(!"SYNC".equals(mode))
      {
         return (new EdiSendResult("SENT", messageId, "ASYNC".equals(mode) ? "Awaiting the asynchronous MDN" : "No receipt requested"));
      }
      return (settleSync(message, transport, messageId, prepared, response, certificates));
   }



   /*******************************************************************************
    ** Compress, sign and encrypt as configured, computing the MIC over the content
    ** that is signed.
    *******************************************************************************/
   private Prepared prepare(EdiOutboundMessage message, EdiTransport transport, byte[] edi, String messageId, CertificateService certificates) throws Exception
   {
      ECryptoAlgorithmSign signAlgorithm = As2Crypto.signAlgorithm(transport.getSignAlgorithm());
      ECryptoAlgorithmCrypt encryptAlgorithm = As2Crypto.encryptAlgorithm(transport.getEncryptAlgorithm());
      LocalKey key = signAlgorithm == null ? null : certificates.activeLocal(transport);
      X509Certificate partner = encryptAlgorithm == null ? null : certificates.activePartnerCertificate(transport);
      checkValid("Local certificate", key == null ? null : key.certificate());
      checkValid("Partner certificate", partner);
      String contentType = edi.length >= 3 && edi[0] == 'U' && edi[1] == 'N' ? "application/edifact" : "application/edi-x12";
      var part = As2Crypto.payloadPart(edi, contentType, "edi-" + message.getId() + ".edi");
      if(Boolean.TRUE.equals(transport.getCompress()))
      {
         part = As2Crypto.compress(part);
      }
      ECryptoAlgorithmSign micAlgorithm = signAlgorithm == null ? ECryptoAlgorithmSign.DIGEST_SHA_256 : signAlgorithm;
      byte[] clear;
      MIC mic;
      if(signAlgorithm == null)
      {
         clear = As2Crypto.toBytes(part);
         mic = As2Crypto.mic(clear, micAlgorithm);
      }
      else
      {
         clear = As2Crypto.toBytes(As2Crypto.sign(part, key, signAlgorithm));
         As2Crypto.Wire signed = As2Crypto.splitRaw(clear);
         mic = As2Crypto.mic(As2Crypto.firstPart(signed.contentType(), signed.body()), micAlgorithm);
      }
      return (new Prepared(encryptAlgorithm == null ? As2Crypto.splitRaw(clear) : As2Crypto.encrypt(clear, partner, encryptAlgorithm), mic));
   }



   /*******************************************************************************
    ** Certificates outside their validity period are not used.
    *******************************************************************************/
   private static void checkValid(String what, X509Certificate certificate) throws As2Exception
   {
      if(certificate != null)
      {
         try
         {
            certificate.checkValidity();
         }
         catch(Exception e)
         {
            throw (new As2Exception(what + " " + certificate.getSerialNumber().toString(16) + " is expired or not yet valid"));
         }
      }
   }



   /*******************************************************************************
    ** The AS2 request headers.
    *******************************************************************************/
   private Map<String, String> headers(EdiTransport transport, String messageId, Prepared prepared, String mode) throws Exception
   {
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("AS2-Version", "1.2");
      headers.put("AS2-From", asId(transport.getAs2LocalId()));
      headers.put("AS2-To", asId(transport.getAs2PartnerId()));
      headers.put("Message-ID", "<" + messageId + ">");
      headers.put("Subject", subject(transport));
      headers.put("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(EdiRuntime.current().config().getClock().withZone(ZoneOffset.UTC))));
      headers.put("MIME-Version", "1.0");
      headers.put("User-Agent", "qbit-edi");
      headers.put("Content-Type", prepared.wire().contentType());
      if(prepared.wire().transferEncoding() != null)
      {
         headers.put("Content-Transfer-Encoding", prepared.wire().transferEncoding());
      }
      if(prepared.wire().contentDisposition() != null)
      {
         headers.put("Content-Disposition", prepared.wire().contentDisposition());
      }
      if(!"NONE".equals(mode))
      {
         headers.put("Disposition-Notification-To", transport.getAs2LocalId());
         if(Boolean.TRUE.equals(transport.getMdnSigned()))
         {
            ECryptoAlgorithmSign alg = As2Crypto.signAlgorithm(transport.getSignAlgorithm()) == null ? ECryptoAlgorithmSign.DIGEST_SHA_256 : As2Crypto.signAlgorithm(transport.getSignAlgorithm());
            headers.put("Disposition-Notification-Options", new DispositionOptions().setProtocolImportance(DispositionOptions.IMPORTANCE_REQUIRED)
               .setProtocol(DispositionOptions.PROTOCOL_PKCS7_SIGNATURE).setMICAlgImportance(DispositionOptions.IMPORTANCE_REQUIRED).setMICAlg(alg).getAsString());
         }
      }
      if("ASYNC".equals(mode))
      {
         headers.put("Receipt-Delivery-Option", baseUrl() + "/qqq/hooks/edi/as2/" + transport.getName() + "/mdn");
      }
      if(transport.getHttpAuthUserEnvVar() != null)
      {
         headers.put("Authorization", As2Http.basic(secret(transport.getHttpAuthUserEnvVar()), secret(transport.getHttpAuthPasswordEnvVar())));
      }
      return (headers);
   }



   /*******************************************************************************
    ** Evaluate a synchronous receipt: signature, disposition and MIC.
    *******************************************************************************/
   private EdiSendResult settleSync(EdiOutboundMessage message, EdiTransport transport, String messageId, Prepared prepared, As2Http.Response response, CertificateService certificates) throws Exception
   {
      String contentType = response.headers().get("content-type");
      if(response.body().length == 0 || contentType == null)
      {
         return (new EdiSendResult("RETRY", messageId, "The partner returned no MDN"));
      }
      As2Mdn.Parsed mdn;
      try
      {
         mdn = As2Mdn.parse(contentType, response.headers().get("content-transfer-encoding"), response.body(), certificates.partnerVerificationCertificates(transport),
            Date.from(EdiRuntime.current().config().getClock().instant()));
      }
      catch(As2Exception e)
      {
         return (new EdiSendResult("RETRY", messageId, e.getMessage()));
      }
      String failure = judge(transport, mdn, prepared.mic());
      PayloadRef raw = As2Store.storeRaw(As2Crypto.entity(contentType, response.headers().get("content-transfer-encoding"), null, response.body()));
      As2Store.insert(new QRecord().withValue("direction", "INBOUND").withValue("messageId", As2Mdn.sanitize(As2Mdn.normalizeId(response.headers().get("message-id"))))
         .withValue("originalMessageId", messageId).withValue("disposition", mdn.disposition()).withValue("micAlgorithm", mdn.mic() == null ? null : mdn.mic().getDigestAlgorithm().getID())
         .withValue("micValue", As2Store.micValue(mdn.mic())).withValue("signed", mdn.signed()).withValue("signatureValid", mdn.signatureValid()).withValue("payloadRef", raw.value())
         .withValue("transportId", transport.getId()).withValue("partnerId", message.getPartnerId()).withValue("outboundMessageId", message.getId()).withValue("deliveryStatus", "NONE"));
      EdiJournal.event(null, message.getInterchangeId(), null, "MDN");
      if(failure != null)
      {
         return (new EdiSendResult("FAILED", messageId, failure));
      }
      QAlerts.resolve("edi.mdn.overdue", "edi_outbound_message:" + message.getId(), "MDN received");
      return (new EdiSendResult("DELIVERED", messageId, "MDN processed"));
   }



   /*******************************************************************************
    ** The reason a receipt does not confirm delivery, or null when it does.
    *******************************************************************************/
   static String judge(EdiTransport transport, As2Mdn.Parsed mdn, MIC ours)
   {
      if(Boolean.TRUE.equals(mdn.signed()) && !Boolean.TRUE.equals(mdn.signatureValid()))
      {
         return ("The MDN signature could not be verified");
      }
      if(Boolean.TRUE.equals(transport.getMdnSigned()) && !Boolean.TRUE.equals(mdn.signed()))
      {
         return ("The MDN is not signed");
      }
      if(!mdn.processed())
      {
         return ("Partner disposition: " + mdn.disposition());
      }
      if(ours != null && (mdn.mic() == null ? Boolean.TRUE.equals(transport.getMdnSigned()) || mdn.micText() != null : !mdn.mic().equals(ours)))
      {
         return ("The MDN Received-Content-MIC does not match the message sent");
      }
      return (null);
   }



   /*******************************************************************************
    ** Probe configuration without sending any document: resolve keys and
    ** certificates, then OPTIONS the endpoint.
    *******************************************************************************/
   @Override
   public EdiTestResult test(EdiTransport transport)
   {
      try
      {
         return (SystemWrites.call(() -> probe(transport)));
      }
      catch(Exception e)
      {
         return (new EdiTestResult(false, "AS2 test could not run; check the database and configuration"));
      }
   }



   /*******************************************************************************
    ** Resolve keys and certificates and reach the endpoint.
    *******************************************************************************/
   private EdiTestResult probe(EdiTransport transport) throws Exception
   {
      CertificateService certificates = new CertificateService();
      List<String> notes = new java.util.ArrayList<>();
      try
      {
         boolean secured = transport.getSignAlgorithm() != null || transport.getEncryptAlgorithm() != null || Boolean.TRUE.equals(transport.getMdnSigned());
         if(secured || transport.getLocalCertificateId() != null)
         {
            LocalKey key = certificates.activeLocal(transport);
            notes.add("Local key " + key.row().getValueString("alias") + " resolved (valid until " + key.certificate().getNotAfter().toInstant() + ")");
         }
         if(secured || transport.getPartnerCertificateId() != null)
         {
            certificates.activePartnerCertificate(transport);
            notes.add(certificates.partnerVerificationCertificates(transport).size() + " partner certificate(s) accepted for verification");
         }
         HttpClient client = As2Http.client(certificates.tlsPin(transport));
         if(!"INBOUND".equals(transport.getDirection()) && transport.getAs2Url() != null)
         {
            int status = As2Http.options(client, URI.create(transport.getAs2Url()), Map.of(), Duration.ofSeconds(15)).status();
            notes.add("Endpoint responded with HTTP " + status);
            if(status >= 500)
            {
               return (new EdiTestResult(false, String.join("; ", notes)));
            }
         }
      }
      catch(As2Exception e)
      {
         notes.add(e.getMessage());
         return (new EdiTestResult(false, String.join("; ", notes)));
      }
      catch(java.io.IOException e)
      {
         notes.add("The AS2 endpoint could not be reached");
         return (new EdiTestResult(false, String.join("; ", notes)));
      }
      if(As2Crypto.isWeak(transport.getSignAlgorithm(), transport.getEncryptAlgorithm()))
      {
         notes.add("Warning: weak crypto (SHA1 or 3DES) is configured");
      }
      return (new EdiTestResult(true, String.join("; ", notes)));
   }



   /*******************************************************************************
    ** Receipt mode, defaulting to SYNC.
    *******************************************************************************/
   static String mode(EdiTransport transport)
   {
      return (transport.getMdnMode() == null ? "SYNC" : transport.getMdnMode());
   }



   /*******************************************************************************
    ** A stable Message-ID: retries of the same outbound message reuse it, so the
    ** partner can recognize them.
    *******************************************************************************/
   static String messageId(Integer messageId, EdiTransport transport, byte[] edi) throws Exception
   {
      String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(edi)).substring(0, 12);
      return ("edi-" + messageId + "-" + digest + "@" + transport.getAs2LocalId().replaceAll("[^A-Za-z0-9.-]", "-"));
   }



   /*******************************************************************************
    ** An AS2 identifier as a header value; identifiers with spaces are quoted.
    *******************************************************************************/
   static String asId(String id)
   {
      return (id.contains(" ") ? "\"" + id + "\"" : id);
   }



   /*******************************************************************************
    ** Subject header without line breaks.
    *******************************************************************************/
   private static String subject(EdiTransport transport)
   {
      String subject = transport.getAs2Subject() == null || transport.getAs2Subject().isBlank() ? "EDI interchange" : transport.getAs2Subject();
      return (subject.replaceAll("[\\r\\n]+", " "));
   }



   /*******************************************************************************
    ** Resolve a secret by environment variable name; failure names only the name.
    *******************************************************************************/
   private static String secret(String variable) throws As2Exception
   {
      String value = EdiRuntime.current().config().getSecretsResolver().resolve(variable);
      if(value == null)
      {
         throw (new As2Exception("environment variable " + variable + " is not set"));
      }
      return (value);
   }
}
