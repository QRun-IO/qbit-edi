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
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.crypto.MIC;
import com.kingsrook.qbits.edi.as2.CertificateService.LocalKey;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;


/*******************************************************************************
 ** Builds and parses AS2 message disposition notifications (RFC 4130 section 7,
 ** RFC 8098): multipart/report with a message/disposition-notification part,
 ** optionally wrapped in multipart/signed. Memory only.
 *******************************************************************************/
public final class As2Mdn
{
   public static final String PROCESSED = "automatic-action/MDN-sent-automatically; processed";



   /*******************************************************************************
    ** A receipt ready to send or store: HTTP-level parts and the complete MIME
    ** entity (headers and body) kept as the stored raw MDN.
    *******************************************************************************/
   public record Built(String messageId, As2Crypto.Wire wire, byte[] entity, Boolean signed)
   {
   }



   /*******************************************************************************
    ** A receipt read from a partner. Fields come from the disposition notification;
    ** signed and signatureValid describe the signature, when there was one.
    *******************************************************************************/
   public record Parsed(String originalMessageId, String disposition, String micText, MIC mic, Boolean signed, Boolean signatureValid)
   {
      /*******************************************************************************
       ** Success means the partner processed the message (a warning still counts).
       *******************************************************************************/
      public boolean processed()
      {
         if(disposition == null)
         {
            return (false);
         }
         int separator = disposition.indexOf(';');
         String status = (separator < 0 ? disposition : disposition.substring(separator + 1)).trim().toLowerCase(java.util.Locale.ROOT);
         return (status.equals("processed") || status.startsWith("processed/warning"));
      }
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private As2Mdn()
   {
   }



   /*******************************************************************************
    ** Disposition text for a failure, e.g. processed/error: authentication-failed.
    *******************************************************************************/
   public static String failure(String code)
   {
      return ("automatic-action/MDN-sent-automatically; processed/error: " + code);
   }



   /*******************************************************************************
    ** Normalize an AS2 identifier or Message-ID: trim, drop angle brackets and
    ** quotes.
    *******************************************************************************/
   public static String normalizeId(String value)
   {
      if(value == null)
      {
         return (null);
      }
      String id = value.trim();
      while(id.length() >= 2 && ((id.startsWith("<") && id.endsWith(">")) || (id.startsWith("\"") && id.endsWith("\""))))
      {
         id = id.substring(1, id.length() - 1).trim();
      }
      return (id);
   }



   /*******************************************************************************
    ** Keep only characters that are safe to show and store from a remote string.
    *******************************************************************************/
   public static String sanitize(String value)
   {
      if(value == null)
      {
         return (null);
      }
      String clean = value.replaceAll("[^A-Za-z0-9 /;:._,=+@<>-]", "");
      return (clean.length() <= 200 ? clean : clean.substring(0, 200));
   }



   /*******************************************************************************
    ** Build a receipt. The MIC is included for processed messages; a signing key
    ** and algorithm produce a signed receipt.
    *******************************************************************************/
   public static Built build(String messageId, String ourId, String originalMessageId, String disposition, MIC mic, LocalKey key, ECryptoAlgorithmSign algorithm) throws As2Exception
   {
      try
      {
         String fields = "Reporting-UA: qbit-edi\r\nOriginal-Recipient: rfc822; " + ourId + "\r\nFinal-Recipient: rfc822; " + ourId
            + "\r\nOriginal-Message-ID: <" + originalMessageId + ">\r\nDisposition: " + disposition + "\r\n"
            + (mic == null ? "" : "Received-Content-MIC: " + mic.getAsAS2String() + "\r\n");
         MimeMultipart report = new MimeMultipart("report; report-type=disposition-notification");
         MimeBodyPart text = new MimeBodyPart();
         text.setText("The AS2 message " + originalMessageId + " was processed with disposition: " + disposition + "\r\n", "us-ascii");
         text.setHeader("Content-Transfer-Encoding", "7bit");
         report.addBodyPart(text);
         InternetHeaders headers = new InternetHeaders();
         headers.addHeader("Content-Type", "message/disposition-notification");
         headers.addHeader("Content-Transfer-Encoding", "7bit");
         report.addBodyPart(new MimeBodyPart(headers, fields.getBytes(StandardCharsets.US_ASCII)));
         MimeBodyPart outer = new MimeBodyPart();
         outer.setContent(report);
         outer.setHeader("Content-Type", report.getContentType());
         MimeBodyPart result = key == null ? outer : As2Crypto.sign(outer, key, algorithm);
         byte[] entity = As2Crypto.toBytes(result);
         return (new Built(messageId, As2Crypto.splitRaw(entity), entity, key != null));
      }
      catch(As2Exception e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new As2Exception("The MDN could not be built", As2Exception.UNEXPECTED_PROCESSING));
      }
   }



   /*******************************************************************************
    ** Parse a receipt. A signature is verified against the candidate certificates
    ** and reported in signatureValid; an invalid one still yields the readable
    ** content so the failure can be recorded, but callers must not trust it.
    *******************************************************************************/
   public static Parsed parse(String contentType, String transferEncoding, byte[] body, List<X509Certificate> certificates, Date now) throws As2Exception
   {
      MimeBodyPart part = As2Crypto.fromHttp(contentType, transferEncoding, null, body);
      boolean signed = false;
      Boolean valid = null;
      try
      {
         MimeBodyPart content = part;
         if(part.isMimeType("multipart/signed"))
         {
            signed = true;
            try
            {
               content = As2Crypto.verify(part, certificates, now);
               valid = true;
            }
            catch(As2Exception e)
            {
               valid = false;
               content = (MimeBodyPart) new MimeMultipart(new ByteArrayDataSource(part.getRawInputStream().readAllBytes(), MimeUtility.unfold(part.getContentType()))).getBodyPart(0);
            }
         }
         if(!content.isMimeType("multipart/report"))
         {
            throw (new As2Exception("The response is not a message disposition notification"));
         }
         MimeMultipart report = new MimeMultipart(new ByteArrayDataSource(content.getRawInputStream().readAllBytes(), MimeUtility.unfold(content.getContentType())));
         for(int i = 0; i < report.getCount(); i++)
         {
            if(report.getBodyPart(i).isMimeType("message/disposition-notification"))
            {
               InternetHeaders fields = new InternetHeaders(new ByteArrayInputStream(report.getBodyPart(i).getInputStream().readAllBytes()));
               return (fromFields(fields, signed, valid));
            }
         }
         throw (new As2Exception("The response has no disposition notification"));
      }
      catch(As2Exception e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new As2Exception("The response is not a readable message disposition notification"));
      }
   }



   /*******************************************************************************
    ** Read the notification fields.
    *******************************************************************************/
   private static Parsed fromFields(InternetHeaders fields, boolean signed, Boolean valid)
   {
      String original = normalizeId(first(fields, "Original-Message-ID"));
      String disposition = sanitize(first(fields, "Disposition"));
      String micText = sanitize(first(fields, "Received-Content-MIC"));
      MIC mic = null;
      try
      {
         mic = MIC.parse(micText);
      }
      catch(Exception e)
      {
         // An unreadable MIC is reported as a mismatch by the caller.
         mic = null;
      }
      return (new Parsed(original == null ? null : sanitize(original), disposition, micText, mic, signed, valid));
   }



   /*******************************************************************************
    ** First value of a notification field, unfolded, or null.
    *******************************************************************************/
   private static String first(InternetHeaders fields, String name)
   {
      String[] values = fields.getHeader(name);
      return (values == null || values.length == 0 ? null : MimeUtility.unfold(values[0]).trim());
   }
}
