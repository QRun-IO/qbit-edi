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

package com.kingsrook.qbits.edi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qbits.edi.security.EdiSecrets;
import com.sun.net.httpserver.HttpServer;
import jakarta.activation.DataHandler;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/*******************************************************************************
 ** Synthetic in-memory certificates and a BouncyCastle-only AS2 peer, written
 ** independently of the production classes so that both directions are tested
 ** against a second implementation. Nothing here touches the filesystem.
 *******************************************************************************/
public final class As2Support
{
   public static final Map<String, String> ENV = new ConcurrentHashMap<>();
   private static final Map<String, Party> PARTIES = new ConcurrentHashMap<>();

   /*******************************************************************************
    ** A synthetic self-signed certificate and its key pair.
    *******************************************************************************/
   public record Party(KeyPair pair, X509Certificate certificate)
   {
      /*******************************************************************************
       ** PEM of the public certificate.
       *******************************************************************************/
      public String certificatePem()
      {
         try
         {
            return ("-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(certificate.getEncoded()) + "\n-----END CERTIFICATE-----\n");
         }
         catch(Exception e)
         {
            throw new IllegalStateException(e);
         }
      }

      /*******************************************************************************
       ** PEM PKCS#8 of the private key (synthetic, memory only).
       *******************************************************************************/
      public String keyPem()
      {
         return ("-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(pair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n");
      }

      /*******************************************************************************
       ** The private key.
       *******************************************************************************/
      public PrivateKey key()
      {
         return (pair.getPrivate());
      }
   }

   /*******************************************************************************
    ** A signed or unsigned reply from the peer or from the system under test.
    *******************************************************************************/
   public record Reply(int status, Map<String, String> headers, byte[] body)
   {
      /*******************************************************************************
       ** Body as text.
       *******************************************************************************/
      public String text()
      {
         return (new String(body, StandardCharsets.ISO_8859_1));
      }
   }

   /*******************************************************************************
    ** What a peer learned from an MDN.
    *******************************************************************************/
   public record PeerMdn(String disposition, String originalMessageId, String receivedMic, Boolean signatureValid, String raw)
   {
   }

   /*******************************************************************************
    ** A message the peer server received and unwrapped.
    *******************************************************************************/
   public record PeerReceived(String messageId, byte[] payload, String mic, boolean signed, boolean encrypted)
   {
   }

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private As2Support()
   {
   }

   /*******************************************************************************
    ** A named synthetic party, created once per test run. Validity runs from one
    ** day ago for the given number of days.
    *******************************************************************************/
   public static Party party(String name, long validDays)
   {
      return (PARTIES.computeIfAbsent(name + ":" + validDays, k -> create(name, Instant.now().minusSeconds(86400), Instant.now().plusSeconds(validDays * 86400))));
   }

   /*******************************************************************************
    ** Generate an RSA 2048 key pair and a self-signed certificate.
    *******************************************************************************/
   public static Party create(String name, Instant notBefore, Instant notAfter)
   {
      try
      {
         Security.addProvider(new BouncyCastleProvider());
         KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
         generator.initialize(2048);
         KeyPair pair = generator.generateKeyPair();
         X500Name subject = new X500Name("CN=" + name + ",O=Synthetic Test");
         X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(subject, BigInteger.valueOf(System.nanoTime()), Date.from(notBefore), Date.from(notAfter), subject, pair.getPublic());
         X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
         return (new Party(pair, certificate));
      }
      catch(Exception e)
      {
         throw new IllegalStateException(e);
      }
   }

   /*******************************************************************************
    ** Resolver for environment variables held only in memory; the payload key is
    ** always available.
    *******************************************************************************/
   public static EdiSecrets secrets()
   {
      return (name -> name.startsWith("QQQ_EDI_PAYLOAD") ? Base64.getEncoder().encodeToString(new byte[32]) : ENV.get(name));
   }

   /*******************************************************************************
    ** Base64 SHA-256 of bytes.
    *******************************************************************************/
   public static String sha256(byte[] bytes) throws Exception
   {
      return (Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes)));
   }

   /*******************************************************************************
    ** Send an AS2 message built with BouncyCastle only.
    *******************************************************************************/
   public static Reply peerSend(String url, Map<String, String> ids, String messageId, byte[] payload, Party signer, X509Certificate encryptTo, boolean base64, boolean requestMdn, boolean signedMdn,
      String asyncUrl) throws Exception
   {
      MimeBodyPart part = new MimeBodyPart();
      part.setDataHandler(new DataHandler(new ByteArrayDataSource(payload, "application/edi-x12")));
      part.setHeader("Content-Type", "application/edi-x12");
      part.setHeader("Content-Transfer-Encoding", base64 ? "base64" : "binary");
      part.setHeader("Content-Disposition", "attachment; filename=\"peer.edi\"");
      String mic = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(serialize(part))) + ", sha-256";
      MimeBodyPart outer = part;
      if(signer != null)
      {
         SMIMESignedGenerator generator = new SMIMESignedGenerator();
         generator.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC").build("SHA256withRSA", signer.key(), signer.certificate()));
         generator.addCertificates(new JcaCertStore(List.of(signer.certificate())));
         MimeMultipart multipart = generator.generate(part);
         outer = new MimeBodyPart();
         outer.setContent(multipart);
         outer.setHeader("Content-Type", multipart.getContentType());
      }
      if(encryptTo != null)
      {
         SMIMEEnvelopedGenerator generator = new SMIMEEnvelopedGenerator();
         generator.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(encryptTo).setProvider("BC"));
         outer = generator.generate(outer, new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider("BC").build());
      }
      byte[] wire = serialize(outer);
      int split = indexOf(wire, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.ofByteArray(java.util.Arrays.copyOfRange(wire, split + 4, wire.length)))
         .header("Content-Type", MimeUtility.unfold(outer.getHeader("Content-Type", null)));
      if(outer.getHeader("Content-Transfer-Encoding", null) != null)
      {
         request.header("Content-Transfer-Encoding", outer.getHeader("Content-Transfer-Encoding", null));
      }
      ids.forEach(request::header);
      request.header("AS2-Version", "1.2").header("Message-ID", "<" + messageId + ">");
      if(requestMdn)
      {
         request.header("Disposition-Notification-To", "peer@example.invalid");
         if(signedMdn)
         {
            request.header("Disposition-Notification-Options", "signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required, sha-256");
         }
         if(asyncUrl != null)
         {
            request.header("Receipt-Delivery-Option", asyncUrl);
         }
      }
      try(HttpClient client = HttpClient.newHttpClient())
      {
         HttpResponse<byte[]> response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
         Map<String, String> headers = new LinkedHashMap<>();
         response.headers().map().forEach((k, v) -> headers.put(k.toLowerCase(java.util.Locale.ROOT), v.getFirst()));
         headers.put("x-sent-mic", mic);
         return (new Reply(response.statusCode(), headers, response.body()));
      }
   }

   /*******************************************************************************
    ** Read an MDN with BouncyCastle, verifying a signature with the given
    ** certificate when the MDN is signed.
    *******************************************************************************/
   public static PeerMdn peerReadMdn(Reply reply, X509Certificate signer) throws Exception
   {
      String contentType = reply.headers().get("content-type");
      byte[] body = reply.body();
      Boolean valid = null;
      MimeMultipart report;
      if(contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/signed"))
      {
         SMIMESigned signed = new SMIMESigned(new MimeMultipart(new ByteArrayDataSource(body, contentType)));
         valid = false;
         for(SignerInformation info : signed.getSignerInfos().getSigners())
         {
            valid = info.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(signer));
         }
         MimeBodyPart content = signed.getContent();
         report = new MimeMultipart(new ByteArrayDataSource(content.getRawInputStream().readAllBytes(), MimeUtility.unfold(content.getContentType())));
      }
      else
      {
         report = new MimeMultipart(new ByteArrayDataSource(body, contentType));
      }
      for(int i = 0; i < report.getCount(); i++)
      {
         if(report.getBodyPart(i).getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("message/disposition-notification"))
         {
            InternetHeaders fields = new InternetHeaders(new ByteArrayInputStream(report.getBodyPart(i).getInputStream().readAllBytes()));
            String[] mic = fields.getHeader("Received-Content-MIC");
            return (new PeerMdn(fields.getHeader("Disposition")[0].trim(), fields.getHeader("Original-Message-ID")[0].trim(), mic == null ? null : mic[0].trim(), valid, new String(body, StandardCharsets.ISO_8859_1)));
         }
      }
      throw new IllegalStateException("no disposition notification");
   }

   /*******************************************************************************
    ** A BouncyCastle AS2 receiver on a random loopback port. It records what it
    ** decrypts and verifies, and answers with an MDN it builds itself.
    *******************************************************************************/
   public static final class PeerServer implements AutoCloseable
   {
      public final HttpServer server;
      public final List<PeerReceived> received = new CopyOnWriteArrayList<>();
      public final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();
      public final AtomicReference<String> disposition = new AtomicReference<>("automatic-action/MDN-sent-automatically; processed");
      public final AtomicReference<Boolean> corruptMic = new AtomicReference<>(false);
      public final AtomicReference<Boolean> signMdn = new AtomicReference<>(true);
      public final AtomicReference<Boolean> async = new AtomicReference<>(false);
      public final AtomicReference<Boolean> silent = new AtomicReference<>(false);
      public final AtomicReference<Reply> asyncResult = new AtomicReference<>();
      public final List<Party> decryptionKeys = new CopyOnWriteArrayList<>();
      public final List<X509Certificate> senderCertificates = new CopyOnWriteArrayList<>();
      public volatile Party self;

      /*******************************************************************************
       ** Start listening; self signs MDNs and decrypts messages.
       *******************************************************************************/
      public PeerServer(Party self, X509Certificate sender) throws Exception
      {
         this.self = self;
         decryptionKeys.add(self);
         senderCertificates.add(sender);
         server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
         server.createContext("/as2", exchange ->
         {
            try
            {
               if("OPTIONS".equals(exchange.getRequestMethod()))
               {
                  exchange.sendResponseHeaders(200, -1);
                  return;
               }
               byte[] body = exchange.getRequestBody().readAllBytes();
               Map<String, String> headers = new LinkedHashMap<>();
               exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(java.util.Locale.ROOT), v.getFirst()));
               requests.add(headers);
               byte[] response = handle(headers, body);
               if(response == null)
               {
                  exchange.sendResponseHeaders(200, -1);
               }
               else
               {
                  int split = indexOf(response, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                  String head = new String(response, 0, split, StandardCharsets.ISO_8859_1);
                  for(String line : head.split("\r\n"))
                  {
                     if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:"))
                     {
                        exchange.getResponseHeaders().add("Content-Type", line.substring(13).trim());
                     }
                  }
                  exchange.getResponseHeaders().add("AS2-Version", "1.2");
                  exchange.getResponseHeaders().add("Message-ID", "<peer-mdn-" + System.nanoTime() + "@peer>");
                  byte[] out = java.util.Arrays.copyOfRange(response, split + 4, response.length);
                  exchange.sendResponseHeaders(200, out.length);
                  exchange.getResponseBody().write(out);
               }
            }
            catch(Exception e)
            {
               exchange.sendResponseHeaders(500, -1);
            }
            finally
            {
               exchange.close();
            }
         });
         server.start();
      }

      /*******************************************************************************
       ** URL of the AS2 endpoint.
       *******************************************************************************/
      public String url()
      {
         return ("http://127.0.0.1:" + server.getAddress().getPort() + "/as2");
      }

      /*******************************************************************************
       ** Unwrap, verify and answer one message.
       *******************************************************************************/
      private byte[] handle(Map<String, String> headers, byte[] body) throws Exception
      {
         String contentType = headers.get("content-type");
         String encoding = headers.get("content-transfer-encoding");
         MimeBodyPart part = new MimeBodyPart(new ByteArrayInputStream(concat(("Content-Type: " + contentType + "\r\n" + (encoding == null ? "" : "Content-Transfer-Encoding: " + encoding + "\r\n") + "\r\n").getBytes(StandardCharsets.ISO_8859_1), body)));
         boolean encrypted = false;
         boolean signed = false;
         if(part.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/pkcs7-mime"))
         {
            SMIMEEnveloped enveloped = new SMIMEEnveloped(part);
            byte[] decrypted = null;
            for(Party key : decryptionKeys)
            {
               var recipient = enveloped.getRecipientInfos().get(new JceKeyTransRecipientId(key.certificate()));
               if(recipient != null)
               {
                  decrypted = recipient.getContent(new JceKeyTransEnvelopedRecipient(key.key()).setProvider("BC"));
               }
            }
            if(decrypted == null)
            {
               throw new IllegalStateException("not encrypted to this peer");
            }
            part = new MimeBodyPart(new ByteArrayInputStream(decrypted));
            encrypted = true;
         }
         MimeBodyPart content = part;
         if(part.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/signed"))
         {
            SMIMESigned smime = new SMIMESigned(new MimeMultipart(new ByteArrayDataSource(part.getRawInputStream().readAllBytes(), MimeUtility.unfold(part.getContentType()))));
            boolean verified = false;
            for(SignerInformation info : smime.getSignerInfos().getSigners())
            {
               for(X509Certificate certificate : senderCertificates)
               {
                  verified |= info.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certificate));
               }
            }
            if(!verified)
            {
               throw new IllegalStateException("bad signature");
            }
            content = smime.getContent();
            signed = true;
         }
         String mic = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(serialize(content))) + ", sha-256";
         String messageId = headers.get("message-id").replaceAll("[<>]", "");
         received.add(new PeerReceived(messageId, content.getInputStream().readAllBytes(), mic, signed, encrypted));
         String reportedMic = corruptMic.get() ? Base64.getEncoder().encodeToString(new byte[32]) + ", sha-256" : mic;
         byte[] mdn = mdn(messageId, disposition.get(), reportedMic, signMdn.get() && headers.containsKey("disposition-notification-options"));
         if(silent.get())
         {
            return (null);
         }
         if(async.get())
         {
            String target = headers.get("receipt-delivery-option");
            Thread.startVirtualThread(() ->
            {
               try
               {
                  asyncResult.set(postMdn(target, mdn, headers));
               }
               catch(Exception e)
               {
                  asyncResult.set(new Reply(-1, Map.of(), e.toString().getBytes(StandardCharsets.UTF_8)));
               }
            });
            return (null);
         }
         return (mdn);
      }

      /*******************************************************************************
       ** Post an asynchronous MDN to the address from the request.
       *******************************************************************************/
      private Reply postMdn(String target, byte[] mdn, Map<String, String> request) throws Exception
      {
         int split = indexOf(mdn, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
         String head = new String(mdn, 0, split, StandardCharsets.ISO_8859_1);
         String contentType = head.lines().filter(l -> l.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:")).findFirst().orElseThrow().substring(13).trim();
         HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target)).header("Content-Type", contentType).header("AS2-From", request.get("as2-to")).header("AS2-To", request.get("as2-from"))
            .header("AS2-Version", "1.2").header("Message-ID", "<peer-async-" + System.nanoTime() + "@peer>")
            .POST(HttpRequest.BodyPublishers.ofByteArray(java.util.Arrays.copyOfRange(mdn, split + 4, mdn.length)));
         try(HttpClient client = HttpClient.newHttpClient())
         {
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return (new Reply(response.statusCode(), Map.of(), response.body()));
         }
      }

      /*******************************************************************************
       ** An MDN entity (headers and body), built with BouncyCastle.
       *******************************************************************************/
      public byte[] mdn(String originalMessageId, String disposition, String mic, boolean signed) throws Exception
      {
         MimeMultipart report = new MimeMultipart("report; report-type=disposition-notification");
         MimeBodyPart text = new MimeBodyPart();
         text.setText("Received by the synthetic peer", "us-ascii");
         report.addBodyPart(text);
         InternetHeaders headers = new InternetHeaders();
         headers.addHeader("Content-Type", "message/disposition-notification");
         headers.addHeader("Content-Transfer-Encoding", "7bit");
         String fields = "Reporting-UA: synthetic-peer\r\nOriginal-Recipient: rfc822; peer\r\nFinal-Recipient: rfc822; peer\r\nOriginal-Message-ID: <" + originalMessageId + ">\r\nDisposition: " + disposition + "\r\n"
            + (disposition.contains("error") ? "" : "Received-Content-MIC: " + mic + "\r\n");
         report.addBodyPart(new MimeBodyPart(headers, fields.getBytes(StandardCharsets.US_ASCII)));
         MimeBodyPart outer = new MimeBodyPart();
         outer.setContent(report);
         outer.setHeader("Content-Type", report.getContentType());
         if(signed)
         {
            SMIMESignedGenerator generator = new SMIMESignedGenerator();
            generator.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC").build("SHA256withRSA", self.key(), self.certificate()));
            MimeMultipart multipart = generator.generate(outer);
            outer = new MimeBodyPart();
            outer.setContent(multipart);
            outer.setHeader("Content-Type", multipart.getContentType());
         }
         return (serialize(outer));
      }

      /*******************************************************************************
       ** Stop the server.
       *******************************************************************************/
      @Override
      public void close()
      {
         server.stop(0);
      }
   }

   /*******************************************************************************
    ** POST a MIME entity (headers and body) the way an AS2 partner does: the entity
    ** Content-Type becomes the HTTP Content-Type and the body the HTTP body.
    *******************************************************************************/
   public static Reply post(String url, byte[] entity, Map<String, String> headers) throws Exception
   {
      int split = indexOf(entity, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
      String head = new String(entity, 0, split, StandardCharsets.ISO_8859_1);
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.ofByteArray(java.util.Arrays.copyOfRange(entity, split + 4, entity.length)));
      for(String line : head.split("\r\n"))
      {
         if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:"))
         {
            request.header("Content-Type", line.substring(13).trim());
         }
      }
      headers.forEach(request::header);
      try(HttpClient client = HttpClient.newHttpClient())
      {
         HttpResponse<byte[]> response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
         Map<String, String> responseHeaders = new LinkedHashMap<>();
         response.headers().map().forEach((k, v) -> responseHeaders.put(k.toLowerCase(java.util.Locale.ROOT), v.getFirst()));
         return (new Reply(response.statusCode(), responseHeaders, response.body()));
      }
   }

   /*******************************************************************************
    ** Serialize a MIME entity to bytes.
    *******************************************************************************/
   public static byte[] serialize(MimeBodyPart part) throws Exception
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      part.writeTo(out);
      return (out.toByteArray());
   }

   /*******************************************************************************
    ** Concatenate byte arrays.
    *******************************************************************************/
   public static byte[] concat(byte[] first, byte[] second)
   {
      byte[] all = new byte[first.length + second.length];
      System.arraycopy(first, 0, all, 0, first.length);
      System.arraycopy(second, 0, all, first.length, second.length);
      return (all);
   }

   /*******************************************************************************
    ** Index of a sequence.
    *******************************************************************************/
   public static int indexOf(byte[] data, byte[] pattern)
   {
      for(int i = 0; i <= data.length - pattern.length; i++)
      {
         if(java.util.Arrays.equals(data, i, i + pattern.length, pattern, 0, pattern.length))
         {
            return (i);
         }
      }
      return (-1);
   }

   /*******************************************************************************
    ** Names of everything directly under the JVM temp directory.
    *******************************************************************************/
   public static List<String> tempNames() throws Exception
   {
      List<String> names = new ArrayList<>();
      try(var stream = java.nio.file.Files.list(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"))))
      {
         stream.forEach(p -> names.add(p.getFileName().toString()));
      }
      return (names);
   }
}
