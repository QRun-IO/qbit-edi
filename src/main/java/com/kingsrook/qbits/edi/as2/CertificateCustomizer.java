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
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;


/*******************************************************************************
 ** Pre-insert and pre-update customizer for edi_certificate. It parses the pasted
 ** public certificate and fills subject, issuer, serial number, fingerprint and
 ** validity dates, and refuses a private key pasted into certificatePem (the
 ** private key belongs in the environment variable named by privateKeyEnvVar).
 *******************************************************************************/
public class CertificateCustomizer implements TableCustomizerInterface
{
   /*******************************************************************************
    ** Parse the certificate on save; reject private keys and key variables on
    ** partner certificates without echoing any value.
    *******************************************************************************/
   @Override
   public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords) throws QException
   {
      for(QRecord record : records)
      {
         QRecord merged = new QRecord();
         oldRecords.orElse(List.of()).stream().filter(old -> Objects.equals(old.getValue("id"), record.getValue("id")))
            .findFirst().ifPresent(old -> merged.getValues().putAll(old.getValues()));
         merged.getValues().putAll(record.getValues());
         boolean insert = oldRecords.isEmpty() || record.getValue("id") == null;
         if(insert || record.getValues().containsKey("certificatePem"))
         {
            try
            {
               X509Certificate certificate = CertificateService.parse(merged.getValueString("certificatePem"));
               record.setValue("subject", abbreviate(certificate.getSubjectX500Principal().getName()));
               record.setValue("issuer", abbreviate(certificate.getIssuerX500Principal().getName()));
               record.setValue("serialNumber", certificate.getSerialNumber().toString(16).toUpperCase());
               record.setValue("fingerprintSha256", CertificateService.fingerprint(certificate));
               record.setValue("notBefore", certificate.getNotBefore().toInstant());
               record.setValue("notAfter", certificate.getNotAfter().toInstant());
               merged.setValue("notAfter", certificate.getNotAfter().toInstant());
            }
            catch(As2Exception e)
            {
               record.addError(new BadInputStatusMessage(e.getMessage()));
            }
         }
         if(insert && merged.getValueString("status") == null)
         {
            record.setValue("status", "ACTIVE");
         }
         if(merged.getValueInteger("partnerId") != null && merged.getValueString("privateKeyEnvVar") != null && !merged.getValueString("privateKeyEnvVar").isBlank())
         {
            record.addError(new BadInputStatusMessage("privateKeyEnvVar is only for our own certificates; leave it blank on a partner certificate"));
         }
         Instant notAfter = merged.getValueInstant("notAfter");
         if(notAfter != null && merged.getValueInstant("notBefore") != null && !notAfter.isAfter(merged.getValueInstant("notBefore")))
         {
            record.addError(new BadInputStatusMessage("The certificate validity period is empty"));
         }
      }
      return (records);
   }



   /*******************************************************************************
    ** Keep parsed names within the column length.
    *******************************************************************************/
   private static String abbreviate(String value)
   {
      return (value.length() <= 255 ? value : value.substring(0, 255));
   }
}
