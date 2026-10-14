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
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;


/*******************************************************************************
 ** Resolves the certificates and keys an AS2 transport uses. Public certificates
 ** come from edi_certificate rows; our private keys come only from environment
 ** variables (a PEM PKCS#8 key or a base64 PKCS#12 store) through the replaceable
 ** secrets resolver, are matched to the certificate, and are never cached,
 ** logged, stored or returned. Callers run inside the system context.
 *******************************************************************************/
public final class CertificateService
{
   private static final QLogger LOG = QLogger.getLogger(CertificateService.class);

   public static final String TABLE = "edi_certificate";



   /*******************************************************************************
    ** Our certificate with its matched private key; the key is excluded from any
    ** string form of this object.
    *******************************************************************************/
   public record LocalKey(QRecord row, X509Certificate certificate, PrivateKey key)
   {
      /*******************************************************************************
       ** Never render key material.
       *******************************************************************************/
      @Override
      public String toString()
      {
         return ("LocalKey[" + row.getValueString("alias") + "]");
      }
   }



   /*******************************************************************************
    ** Local keys usable for decryption, plus the problems (naming environment
    ** variables only) for keys that could not be loaded.
    *******************************************************************************/
   public record KeySet(List<LocalKey> keys, List<String> problems)
   {
   }



   /*******************************************************************************
    ** True when the text contains any private key block.
    *******************************************************************************/
   public static boolean containsPrivateKey(String text)
   {
      return (text != null && text.toUpperCase().contains("PRIVATE KEY"));
   }



   /*******************************************************************************
    ** Parse the first X.509 certificate of a PEM text; private keys are refused
    ** without echoing any content.
    *******************************************************************************/
   public static X509Certificate parse(String pem) throws As2Exception
   {
      if(pem == null || pem.isBlank())
      {
         throw (new As2Exception("certificatePem is required"));
      }
      if(containsPrivateKey(pem))
      {
         throw (new As2Exception("certificatePem must hold only the public certificate; keep the private key in the environment variable named by privateKeyEnvVar"));
      }
      if(!pem.contains("-----BEGIN CERTIFICATE-----"))
      {
         throw (new As2Exception("certificatePem must be a PEM encoded X.509 certificate"));
      }
      Collection<?> certificates;
      try
      {
         certificates = CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
      }
      catch(Exception e)
      {
         throw (new As2Exception("certificatePem could not be parsed as an X.509 certificate"));
      }
      if(certificates.isEmpty() || !(certificates.iterator().next() instanceof X509Certificate certificate))
      {
         throw (new As2Exception("certificatePem does not contain an X.509 certificate"));
      }
      return (certificate);
   }



   /*******************************************************************************
    ** Upper-case colon separated SHA-256 fingerprint of the certificate.
    *******************************************************************************/
   public static String fingerprint(X509Certificate certificate) throws As2Exception
   {
      try
      {
         return (HexFormat.ofDelimiter(":").withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())));
      }
      catch(Exception e)
      {
         throw (new As2Exception("certificate fingerprint could not be computed"));
      }
   }



   /*******************************************************************************
    ** Resolve the key used to sign messages and receipts for the transport: the
    ** referenced certificate when it is ACTIVE, otherwise the ACTIVE certificate
    ** of ours with the same usage (so a promoted rollover takes effect at once).
    *******************************************************************************/
   public LocalKey activeLocal(EdiTransport transport) throws As2Exception, QException
   {
      QRecord referenced = byId(transport.getLocalCertificateId());
      if(referenced == null)
      {
         throw (new As2Exception("The local certificate of transport " + transport.getName() + " is not configured"));
      }
      QRecord active = "ACTIVE".equals(referenced.getValueString("status")) ? referenced : findActive(null, referenced.getValueString("usage"), true);
      if(active == null)
      {
         throw (new As2Exception("No ACTIVE local certificate is available for transport " + transport.getName()));
      }
      return (load(active));
   }



   /*******************************************************************************
    ** Every local key inbound decryption accepts: our ACTIVE and NEXT certificates
    ** that can decrypt. A key that cannot be loaded is skipped and reported by
    ** environment variable name only.
    *******************************************************************************/
   public KeySet localDecryptionKeys() throws QException
   {
      List<LocalKey> keys = new ArrayList<>();
      List<String> problems = new ArrayList<>();
      QQueryFilter filter = new QQueryFilter().withCriteria("status", QCriteriaOperator.IN, "ACTIVE", "NEXT")
         .withCriteria("partnerId", QCriteriaOperator.IS_BLANK).withCriteria("privateKeyEnvVar", QCriteriaOperator.IS_NOT_BLANK);
      for(QRecord row : EdiRecords.query(TABLE, filter))
      {
         if(!List.of("ENCRYPTION", "SIGNING_AND_ENCRYPTION").contains(row.getValueString("usage")))
         {
            continue;
         }
         try
         {
            keys.add(load(row));
         }
         catch(As2Exception e)
         {
            problems.add(e.getMessage());
            LOG.warn("Local AS2 key skipped: " + e.getMessage());
         }
      }
      return (new KeySet(keys, problems));
   }



   /*******************************************************************************
    ** The partner certificate outbound messages are encrypted to: the referenced
    ** certificate when ACTIVE, otherwise the partner ACTIVE certificate of the
    ** same usage.
    *******************************************************************************/
   public X509Certificate activePartnerCertificate(EdiTransport transport) throws As2Exception, QException
   {
      QRecord referenced = byId(transport.getPartnerCertificateId());
      if(referenced == null)
      {
         throw (new As2Exception("The partner certificate of transport " + transport.getName() + " is not configured"));
      }
      QRecord active = "ACTIVE".equals(referenced.getValueString("status")) ? referenced : findActive(referenced.getValueInteger("partnerId"), referenced.getValueString("usage"), false);
      if(active == null)
      {
         throw (new As2Exception("No ACTIVE partner certificate is available for transport " + transport.getName()));
      }
      return (parse(active.getValueString("certificatePem")));
   }



   /*******************************************************************************
    ** Partner certificates a signature may verify against: the referenced one and
    ** the same partner other ACTIVE or NEXT signing certificates. A partner
    ** certificate with no partner (a shared VAN) has no rollover candidates.
    *******************************************************************************/
   public List<X509Certificate> partnerVerificationCertificates(EdiTransport transport) throws As2Exception, QException
   {
      QRecord referenced = byId(transport.getPartnerCertificateId());
      if(referenced == null)
      {
         throw (new As2Exception("The partner certificate of transport " + transport.getName() + " is not configured"));
      }
      List<QRecord> rows = new ArrayList<>();
      if(List.of("ACTIVE", "NEXT").contains(referenced.getValueString("status")))
      {
         rows.add(referenced);
      }
      if(referenced.getValueInteger("partnerId") != null)
      {
         QQueryFilter filter = EdiRecords.equal("partnerId", referenced.getValueInteger("partnerId")).withCriteria("status", QCriteriaOperator.IN, "ACTIVE", "NEXT")
            .withCriteria("usage", QCriteriaOperator.IN, "SIGNING", "SIGNING_AND_ENCRYPTION").withCriteria("privateKeyEnvVar", QCriteriaOperator.IS_BLANK);
         for(QRecord row : EdiRecords.query(TABLE, filter))
         {
            if(rows.stream().noneMatch(existing -> Objects.equals(existing.getValue("id"), row.getValue("id"))))
            {
               rows.add(row);
            }
         }
      }
      List<X509Certificate> certificates = new ArrayList<>();
      for(QRecord row : rows)
      {
         certificates.add(parse(row.getValueString("certificatePem")));
      }
      return (certificates);
   }



   /*******************************************************************************
    ** The pinned TLS certificate of the partner endpoint, or null when no pin is set.
    *******************************************************************************/
   public X509Certificate tlsPin(EdiTransport transport) throws As2Exception, QException
   {
      QRecord row = byId(transport.getPartnerTlsCertificateId());
      if(transport.getPartnerTlsCertificateId() != null && row == null)
      {
         throw (new As2Exception("The partner TLS certificate of transport " + transport.getName() + " does not exist"));
      }
      return (row == null ? null : parse(row.getValueString("certificatePem")));
   }



   /*******************************************************************************
    ** Promote a NEXT certificate to ACTIVE and retire the previous ACTIVE one of
    ** the same partner and usage. Returns false when the certificate is not NEXT.
    *******************************************************************************/
   public boolean promote(QRecord row) throws QException
   {
      if(!"NEXT".equals(row.getValueString("status")))
      {
         return (false);
      }
      boolean local = row.getValueInteger("partnerId") == null && row.getValueString("privateKeyEnvVar") != null;
      QRecord previous = findActive(row.getValueInteger("partnerId"), row.getValueString("usage"), local);
      while(previous != null)
      {
         SystemWrites.update(TABLE, new QRecord().withValue("id", previous.getValue("id")).withValue("status", "RETIRED"));
         previous = findActive(row.getValueInteger("partnerId"), row.getValueString("usage"), local);
      }
      SystemWrites.update(TABLE, new QRecord().withValue("id", row.getValue("id")).withValue("status", "ACTIVE"));
      return (true);
   }



   /*******************************************************************************
    ** Certificate row by id, or null.
    *******************************************************************************/
   private QRecord byId(Integer id) throws QException
   {
      if(id == null)
      {
         return (null);
      }
      List<QRecord> rows = EdiRecords.query(TABLE, EdiRecords.equal("id", id));
      return (rows.isEmpty() ? null : rows.getFirst());
   }



   /*******************************************************************************
    ** The ACTIVE certificate of a partner (null partner means ours or a shared
    ** one) and usage, local ones being those with a private key variable.
    *******************************************************************************/
   private QRecord findActive(Integer partnerId, String usage, boolean local) throws QException
   {
      QQueryFilter filter = EdiRecords.equal("status", "ACTIVE").withCriteria("partnerId", partnerId == null ? QCriteriaOperator.IS_BLANK : QCriteriaOperator.EQUALS, partnerId)
         .withCriteria("usage", QCriteriaOperator.EQUALS, usage).withCriteria("privateKeyEnvVar", local ? QCriteriaOperator.IS_NOT_BLANK : QCriteriaOperator.IS_BLANK);
      List<QRecord> rows = EdiRecords.query(TABLE, filter);
      return (rows.stream().min((a, b) -> Integer.compare(a.getValueInteger("id"), b.getValueInteger("id"))).orElse(null));
   }



   /*******************************************************************************
    ** Parse the certificate, read its private key from the environment and prove
    ** the two belong together by signing and verifying a probe.
    *******************************************************************************/
   private LocalKey load(QRecord row) throws As2Exception
   {
      X509Certificate certificate = parse(row.getValueString("certificatePem"));
      String variable = row.getValueString("privateKeyEnvVar");
      if(variable == null || variable.isBlank())
      {
         throw (new As2Exception("Certificate " + row.getValueString("alias") + " has no privateKeyEnvVar"));
      }
      PrivateKey key = readKey(variable, row.getValueString("privateKeyPasswordEnvVar"));
      if(!matches(key, certificate))
      {
         throw (new As2Exception("The private key in environment variable " + variable + " does not match certificate " + row.getValueString("alias")));
      }
      return (new LocalKey(row, certificate, key));
   }



   /*******************************************************************************
    ** Read the key from the named environment variable; failures name only the
    ** variable.
    *******************************************************************************/
   private static PrivateKey readKey(String variable, String passwordVariable) throws As2Exception
   {
      var secrets = EdiRuntime.current().config().getSecretsResolver();
      String value = secrets.resolve(variable);
      if(value == null || value.isBlank())
      {
         throw (new As2Exception("environment variable " + variable + " is not set"));
      }
      char[] password = new char[0];
      if(passwordVariable != null && !passwordVariable.isBlank())
      {
         String resolved = secrets.resolve(passwordVariable);
         if(resolved == null)
         {
            throw (new As2Exception("environment variable " + passwordVariable + " is not set"));
         }
         password = resolved.toCharArray();
      }
      try
      {
         return (parseKey(value, password));
      }
      catch(Exception e)
      {
         throw (new As2Exception("The private key in environment variable " + variable + " could not be read as a PEM PKCS#8 key or a base64 PKCS#12 store"));
      }
   }



   /*******************************************************************************
    ** Parse a PEM key (PKCS#8, encrypted PKCS#8 or traditional) or a base64 PKCS#12
    ** store, entirely in memory.
    *******************************************************************************/
   static PrivateKey parseKey(String raw, char[] password) throws Exception
   {
      String text = raw.trim();
      if(text.indexOf('\n') < 0 && text.contains("\\n"))
      {
         text = text.replace("\\n", "\n");
      }
      if(text.contains("-----BEGIN"))
      {
         JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
         try(PEMParser parser = new PEMParser(new StringReader(text)))
         {
            Object object = parser.readObject();
            while(object != null)
            {
               if(object instanceof PrivateKeyInfo info)
               {
                  return (converter.getPrivateKey(info));
               }
               if(object instanceof PEMKeyPair pair)
               {
                  return (converter.getKeyPair(pair).getPrivate());
               }
               if(object instanceof PKCS8EncryptedPrivateKeyInfo encrypted)
               {
                  return (converter.getPrivateKey(encrypted.decryptPrivateKeyInfo(new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password))));
               }
               object = parser.readObject();
            }
         }
         throw (new IllegalArgumentException("no private key block"));
      }
      KeyStore store = KeyStore.getInstance("PKCS12");
      store.load(new ByteArrayInputStream(Base64.getMimeDecoder().decode(text)), password);
      for(String alias : Collections.list(store.aliases()))
      {
         if(store.isKeyEntry(alias))
         {
            return ((PrivateKey) store.getKey(alias, password));
         }
      }
      throw (new IllegalArgumentException("no key entry"));
   }



   /*******************************************************************************
    ** Sign a probe with the key and verify it with the certificate public key.
    *******************************************************************************/
   private static boolean matches(PrivateKey key, X509Certificate certificate)
   {
      try
      {
         String algorithm = "EC".equals(key.getAlgorithm()) ? "SHA256withECDSA" : "SHA256with" + key.getAlgorithm();
         byte[] probe = "qbit-edi-key-match".getBytes(StandardCharsets.UTF_8);
         Signature signer = Signature.getInstance(algorithm);
         signer.initSign(key);
         signer.update(probe);
         byte[] signature = signer.sign();
         Signature verifier = Signature.getInstance(algorithm);
         verifier.initVerify(certificate.getPublicKey());
         verifier.update(probe);
         return (verifier.verify(signature));
      }
      catch(Exception e)
      {
         return (false);
      }
   }
}
