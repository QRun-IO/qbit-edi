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
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.helger.as2lib.crypto.BCCryptoHelper;
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.crypto.MIC;
import com.helger.mail.cte.EContentTransferEncoding;
import com.kingsrook.qbits.edi.as2.CertificateService.LocalKey;
import jakarta.activation.DataHandler;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.cms.jcajce.ZlibCompressor;
import org.bouncycastle.cms.jcajce.ZlibExpanderProvider;
import org.bouncycastle.mail.smime.SMIMECompressed;
import org.bouncycastle.mail.smime.SMIMECompressedGenerator;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;


/*******************************************************************************
 ** The AS2 cryptographic layer, entirely in memory. S/MIME signing and the
 ** algorithm tables use as2-lib's BouncyCastle helper; encryption, decryption,
 ** signature verification, compression and the MIC use BouncyCastle directly,
 ** because as2-lib's decrypt and verify stage content in temporary files and this
 ** qbit never writes local files. The MIC is computed over the exact bytes of the
 ** signed (or, when unsigned, the transmitted) MIME entity, headers included, so
 ** both ends agree whatever transfer encoding was used. Messages follow RFC 4130
 ** (AS2), RFC 5751 (S/MIME) and RFC 5402 (compression).
 *******************************************************************************/
public final class As2Crypto
{
   private static final BCCryptoHelper HELPER = new BCCryptoHelper();
   private static final String PROVIDER = BCCryptoHelper.DEFAULT_SECURITY_PROVIDER_NAME;
   private static final byte[] HEADER_END = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] LINE_END = "\r\n".getBytes(StandardCharsets.US_ASCII);

   public static final long MAX_EXPANDED_BYTES = 256L * 1024 * 1024;



   /*******************************************************************************
    ** The result of unwrapping an inbound message: the EDI payload, what protection
    ** was applied and the MIC of the signed (or received) content.
    *******************************************************************************/
   public record Decoded(byte[] payload, String fileName, Boolean encrypted, Boolean signed, Boolean compressed, MIC mic)
   {
   }



   /*******************************************************************************
    ** A MIME entity split for HTTP: the Content-Type, transfer encoding and
    ** disposition go in HTTP headers, the body is the HTTP body.
    *******************************************************************************/
   public record Wire(String contentType, String transferEncoding, String contentDisposition, byte[] body)
   {
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private As2Crypto()
   {
   }



   /*******************************************************************************
    ** Map the signAlgorithm setting; null means unsigned.
    *******************************************************************************/
   public static ECryptoAlgorithmSign signAlgorithm(String setting)
   {
      if(setting == null || setting.isBlank())
      {
         return (null);
      }
      return (switch(setting)
      {
         case "SHA256" -> ECryptoAlgorithmSign.DIGEST_SHA_256;
         case "SHA384" -> ECryptoAlgorithmSign.DIGEST_SHA_384;
         case "SHA512" -> ECryptoAlgorithmSign.DIGEST_SHA_512;
         case "SHA1" -> ECryptoAlgorithmSign.DIGEST_SHA_1;
         default -> throw new IllegalArgumentException("Unsupported signAlgorithm");
      });
   }



   /*******************************************************************************
    ** Map the encryptAlgorithm setting; null means unencrypted.
    *******************************************************************************/
   public static ECryptoAlgorithmCrypt encryptAlgorithm(String setting)
   {
      if(setting == null || setting.isBlank())
      {
         return (null);
      }
      return (switch(setting)
      {
         case "AES128_CBC" -> ECryptoAlgorithmCrypt.CRYPT_AES128_CBC;
         case "AES256_CBC" -> ECryptoAlgorithmCrypt.CRYPT_AES256_CBC;
         case "AES128_GCM" -> ECryptoAlgorithmCrypt.CRYPT_AES128_GCM;
         case "AES256_GCM" -> ECryptoAlgorithmCrypt.CRYPT_AES256_GCM;
         case "3DES" -> ECryptoAlgorithmCrypt.CRYPT_3DES;
         default -> throw new IllegalArgumentException("Unsupported encryptAlgorithm");
      });
   }



   /*******************************************************************************
    ** SHA1 signatures and 3DES encryption are accepted only when set explicitly,
    ** and are flagged as weak.
    *******************************************************************************/
   public static boolean isWeak(String sign, String encrypt)
   {
      return ("SHA1".equals(sign) || "3DES".equals(encrypt));
   }



   /*******************************************************************************
    ** Parse a complete MIME entity (headers, blank line, body) held in memory.
    *******************************************************************************/
   public static MimeBodyPart parse(byte[] entity) throws As2Exception
   {
      try
      {
         return (new MimeBodyPart(new ByteArrayInputStream(entity)));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 message is not a valid MIME entity", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Rebuild the MIME entity of an HTTP request or response from its Content-Type,
    ** Content-Transfer-Encoding and Content-Disposition headers and its body.
    *******************************************************************************/
   public static MimeBodyPart fromHttp(String contentType, String transferEncoding, String contentDisposition, byte[] body) throws As2Exception
   {
      return (parse(entity(contentType, transferEncoding, contentDisposition, body)));
   }



   /*******************************************************************************
    ** The MIME entity bytes (headers, blank line, body) for HTTP-level parts. Header
    ** values with line breaks are refused.
    *******************************************************************************/
   public static byte[] entity(String contentType, String transferEncoding, String contentDisposition, byte[] body) throws As2Exception
   {
      for(String value : Arrays.asList(contentType, transferEncoding, contentDisposition))
      {
         if(value != null && (value.contains("\r") || value.contains("\n")))
         {
            throw (new As2Exception("The AS2 message has an invalid header", As2Exception.UNEXPECTED_PROCESSING));
         }
      }
      if(contentType == null || contentType.isBlank())
      {
         throw (new As2Exception("The AS2 message has no valid Content-Type", As2Exception.UNEXPECTED_PROCESSING));
      }
      ByteArrayOutputStream entity = new ByteArrayOutputStream();
      String headers = "Content-Type: " + contentType + "\r\n" + (transferEncoding == null || transferEncoding.isBlank() ? "" : "Content-Transfer-Encoding: " + transferEncoding + "\r\n")
         + (contentDisposition == null || contentDisposition.isBlank() ? "" : "Content-Disposition: " + contentDisposition + "\r\n") + "\r\n";
      entity.writeBytes(headers.getBytes(StandardCharsets.ISO_8859_1));
      entity.writeBytes(body);
      return (entity.toByteArray());
   }



   /*******************************************************************************
    ** Serialize a MIME entity (headers and encoded body).
    *******************************************************************************/
   public static byte[] toBytes(MimeBodyPart part) throws As2Exception
   {
      try
      {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         part.writeTo(out);
         return (out.toByteArray());
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 message could not be serialized", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Split a MIME entity into the HTTP headers that describe it and its body.
    *******************************************************************************/
   public static Wire toWire(MimeBodyPart part) throws As2Exception
   {
      return (splitRaw(toBytes(part)));
   }



   /*******************************************************************************
    ** Split already serialized MIME entity bytes into the Content-Type,
    ** Content-Transfer-Encoding and Content-Disposition values and the body,
    ** without re-serializing (which could invalidate a signature).
    *******************************************************************************/
   public static Wire splitRaw(byte[] bytes) throws As2Exception
   {
      int split = indexOf(bytes, HEADER_END, 0);
      if(split < 0)
      {
         throw (new As2Exception("The AS2 message has no body", As2Exception.UNEXPECTED_PROCESSING));
      }
      String contentType = null;
      String encoding = null;
      String disposition = null;
      String current = null;
      StringBuilder value = new StringBuilder();
      for(String line : (new String(bytes, 0, split, StandardCharsets.ISO_8859_1) + "\r\nX-End: end").split("\r\n"))
      {
         if((line.startsWith(" ") || line.startsWith("\t")) && current != null)
         {
            value.append(' ').append(line.trim());
            continue;
         }
         if(current != null)
         {
            contentType = "content-type".equals(current) ? value.toString() : contentType;
            encoding = "content-transfer-encoding".equals(current) ? value.toString() : encoding;
            disposition = "content-disposition".equals(current) ? value.toString() : disposition;
         }
         int colon = line.indexOf(':');
         current = colon < 0 ? null : line.substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT);
         value = new StringBuilder(colon < 0 ? "" : line.substring(colon + 1).trim());
      }
      byte[] body = new byte[bytes.length - split - HEADER_END.length];
      System.arraycopy(bytes, split + HEADER_END.length, body, 0, body.length);
      return (new Wire(contentType, encoding, disposition, body));
   }



   /*******************************************************************************
    ** The EDI document as a binary MIME part.
    *******************************************************************************/
   public static MimeBodyPart payloadPart(byte[] payload, String contentType, String fileName) throws As2Exception
   {
      try
      {
         MimeBodyPart part = new MimeBodyPart();
         part.setDataHandler(new DataHandler(new ByteArrayDataSource(payload, contentType)));
         part.setHeader("Content-Type", contentType);
         part.setHeader("Content-Transfer-Encoding", "binary");
         part.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
         return (part);
      }
      catch(Exception e)
      {
         throw (new As2Exception("The EDI document could not be wrapped for AS2", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Compress a part (RFC 5402, zlib).
    *******************************************************************************/
   public static MimeBodyPart compress(MimeBodyPart part) throws As2Exception
   {
      try
      {
         SMIMECompressedGenerator generator = new SMIMECompressedGenerator();
         generator.setContentTransferEncoding("binary");
         return (generator.generate(part, new ZlibCompressor()));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 message could not be compressed", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** The MIC of a MIME entity: the digest of its exact bytes, headers included
    ** (RFC 4130 section 7.3.1).
    *******************************************************************************/
   public static MIC mic(byte[] entity, ECryptoAlgorithmSign algorithm) throws As2Exception
   {
      try
      {
         return (new MIC(MessageDigest.getInstance(algorithm.getID().toUpperCase(java.util.Locale.ROOT)).digest(entity), algorithm));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The message integrity check could not be computed", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** The exact bytes of the first body part of a multipart entity: the signed
    ** content of multipart/signed, without the delimiter lines around it.
    *******************************************************************************/
   public static byte[] firstPart(String contentType, byte[] body) throws As2Exception
   {
      try
      {
         String boundary = new ContentType(contentType).getParameter("boundary");
         int open = indexOf(body, ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1), 0);
         int start = open < 0 ? -1 : indexOf(body, LINE_END, open) + LINE_END.length;
         int end = start < 0 ? -1 : indexOf(body, ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1), start - LINE_END.length);
         if(end < 0)
         {
            throw (new IllegalArgumentException("no signed content"));
         }
         return (Arrays.copyOfRange(body, start, end));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The signed AS2 message is malformed", As2Exception.AUTHENTICATION_FAILED));
      }
   }



   /*******************************************************************************
    ** Sign a part into multipart/signed with the local key.
    *******************************************************************************/
   public static MimeBodyPart sign(MimeBodyPart part, LocalKey key, ECryptoAlgorithmSign algorithm) throws As2Exception
   {
      try
      {
         return (HELPER.sign(part, key.certificate(), key.key(), algorithm, true, algorithm == ECryptoAlgorithmSign.DIGEST_SHA_1, false, EContentTransferEncoding.BINARY));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 message could not be signed with certificate " + key.row().getValueString("alias") + ": " + e.getClass().getSimpleName(), As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Encrypt the exact bytes of a MIME entity to the partner certificate (CMS
    ** enveloped data), returning the application/pkcs7-mime wire message.
    *******************************************************************************/
   public static Wire encrypt(byte[] entity, X509Certificate certificate, ECryptoAlgorithmCrypt algorithm) throws As2Exception
   {
      try
      {
         CMSEnvelopedDataGenerator generator = new CMSEnvelopedDataGenerator();
         generator.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(certificate).setProvider(PROVIDER));
         CMSEnvelopedData enveloped = generator.generate(new CMSProcessableByteArray(entity), new JceCMSContentEncryptorBuilder(algorithm.getOID()).setProvider(PROVIDER).build());
         return (new Wire("application/pkcs7-mime; smime-type=enveloped-data; name=smime.p7m", "binary", "attachment; filename=\"smime.p7m\"", enveloped.getEncoded()));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 message could not be encrypted: " + e.getClass().getSimpleName(), As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Unwrap an inbound entity: decrypt with any of our keys, verify the signature
    ** against the partner certificates, decompress, and compute the MIC over the
    ** exact signed bytes (or, when unsigned, the entity after decryption). Layers
    ** may nest in any order (RFC 5402 allows compression before or after signing).
    *******************************************************************************/
   public static Decoded decode(byte[] entity, List<LocalKey> keys, List<X509Certificate> partnerCertificates, ECryptoAlgorithmSign micAlgorithm, Date now) throws As2Exception
   {
      byte[] raw = entity;
      byte[] micBytes = raw;
      MimeBodyPart part = parse(raw);
      boolean encrypted = false;
      boolean signed = false;
      boolean compressed = false;
      for(int depth = 0; depth < 6; depth++)
      {
         if(isEncrypted(part))
         {
            raw = decrypt(part, keys);
            part = parse(raw);
            micBytes = signed ? micBytes : raw;
            encrypted = true;
         }
         else if(isSigned(part))
         {
            verify(part, partnerCertificates, now);
            Wire wire = splitRaw(raw);
            raw = firstPart(wire.contentType(), wire.body());
            micBytes = raw;
            part = parse(raw);
            signed = true;
         }
         else if(isCompressed(part))
         {
            raw = expand(part);
            part = parse(raw);
            compressed = true;
         }
         else
         {
            break;
         }
      }
      try
      {
         MIC mic = micAlgorithm == null ? null : mic(micBytes, micAlgorithm);
         return (new Decoded(part.getInputStream().readAllBytes(), part.getFileName(), encrypted, signed, compressed, mic));
      }
      catch(As2Exception e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 payload could not be read", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Verify a multipart/signed entity against the candidate certificates and
    ** return the signed content. A digest that does not match the content is an
    ** integrity failure; a signature no candidate certificate verifies is an
    ** authentication failure. Expired certificates are not accepted.
    *******************************************************************************/
   public static MimeBodyPart verify(MimeBodyPart part, List<X509Certificate> certificates, Date now) throws As2Exception
   {
      SMIMESigned signed;
      try
      {
         signed = new SMIMESigned(new MimeMultipart(new ByteArrayDataSource(part.getRawInputStream().readAllBytes(), MimeUtility.unfold(part.getContentType()))));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The signed AS2 message is malformed", As2Exception.AUTHENTICATION_FAILED));
      }
      for(SignerInformation signer : signed.getSignerInfos().getSigners())
      {
         for(X509Certificate certificate : certificates)
         {
            if(certificate.getNotAfter().before(now))
            {
               continue;
            }
            try
            {
               if(signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(PROVIDER).build(certificate)))
               {
                  return (signed.getContent());
               }
            }
            catch(CMSSignerDigestMismatchException e)
            {
               throw (new As2Exception("The message digest does not match the signed content", As2Exception.INTEGRITY_CHECK_FAILED));
            }
            catch(Exception e)
            {
               // This certificate cannot verify the signature; try the next.
               continue;
            }
         }
      }
      throw (new As2Exception("The signature could not be verified with the partner certificates", As2Exception.AUTHENTICATION_FAILED));
   }



   /*******************************************************************************
    ** Decrypt with the first of our keys the message was encrypted to, returning
    ** the clear MIME entity bytes.
    *******************************************************************************/
   private static byte[] decrypt(MimeBodyPart part, List<LocalKey> keys) throws As2Exception
   {
      SMIMEEnveloped enveloped;
      try
      {
         enveloped = new SMIMEEnveloped(part);
      }
      catch(Exception e)
      {
         throw (new As2Exception("The encrypted AS2 message is malformed", As2Exception.DECRYPTION_FAILED));
      }
      for(LocalKey key : keys)
      {
         try
         {
            RecipientInformation recipient = enveloped.getRecipientInfos().get(new JceKeyTransRecipientId(key.certificate()));
            if(recipient != null)
            {
               return (recipient.getContent(new JceKeyTransEnvelopedRecipient(key.key()).setProvider(PROVIDER)));
            }
         }
         catch(Exception e)
         {
            // The key matched the recipient but could not decrypt; try the next key.
            continue;
         }
      }
      throw (new As2Exception("The message was not encrypted to an accepted certificate", As2Exception.DECRYPTION_FAILED));
   }



   /*******************************************************************************
    ** Inflate a compressed-data entity, bounded to prevent decompression bombs.
    *******************************************************************************/
   private static byte[] expand(MimeBodyPart part) throws As2Exception
   {
      try
      {
         return (new SMIMECompressed(part).getContent(new ZlibExpanderProvider(MAX_EXPANDED_BYTES)));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The compressed AS2 message could not be expanded", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** True for application/pkcs7-mime enveloped-data.
    *******************************************************************************/
   private static boolean isEncrypted(MimeBodyPart part) throws As2Exception
   {
      try
      {
         return (HELPER.isEncrypted(part));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 Content-Type is invalid", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** True for multipart/signed.
    *******************************************************************************/
   private static boolean isSigned(MimeBodyPart part) throws As2Exception
   {
      try
      {
         return (HELPER.isSigned(part));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 Content-Type is invalid", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** True for application/pkcs7-mime compressed-data.
    *******************************************************************************/
   private static boolean isCompressed(MimeBodyPart part) throws As2Exception
   {
      try
      {
         return (HELPER.isCompressed(part.getContentType()));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The AS2 Content-Type is invalid", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Index of a byte sequence at or after a position, or -1.
    *******************************************************************************/
   private static int indexOf(byte[] data, byte[] pattern, int from)
   {
      for(int i = Math.max(from, 0); i <= data.length - pattern.length; i++)
      {
         int j = 0;
         while(j < pattern.length && data[i + j] == pattern[j])
         {
            j++;
         }
         if(j == pattern.length)
         {
            return (i);
         }
      }
      return (-1);
   }
}
