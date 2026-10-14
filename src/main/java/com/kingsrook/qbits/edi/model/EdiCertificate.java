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

package com.kingsrook.qbits.edi.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Persistent edi_certificate record.
 *******************************************************************************/
public class EdiCertificate extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_certificate";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "alias", maxLength = 255)
   private String alias;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "usage", maxLength = 255, possibleValueSourceName = "edi_certificate_usage")
   private String usage;

   @QField(backendName = "certificate_pem", maxLength = 10000)
   private String certificatePem;

   @QField(backendName = "subject", maxLength = 255)
   private String subject;

   @QField(backendName = "issuer", maxLength = 255)
   private String issuer;

   @QField(backendName = "serial_number", maxLength = 255)
   private String serialNumber;

   @QField(backendName = "fingerprint_sha256", maxLength = 255)
   private String fingerprintSha256;

   @QField(backendName = "not_before")
   private Instant notBefore;

   @QField(backendName = "not_after")
   private Instant notAfter;

   @QField(backendName = "private_key_env_var", maxLength = 255)
   private String privateKeyEnvVar;

   @QField(backendName = "private_key_password_env_var", maxLength = 255)
   private String privateKeyPasswordEnvVar;

   @QField(backendName = "status", maxLength = 20, possibleValueSourceName = "edi_certificate_status")
   private String status;

   @QField(backendName = "create_date", isEditable = false)
   private Instant createDate;

   @QField(backendName = "modify_date", isEditable = false)
   private Instant modifyDate;

   @QField(backendName = "modified_by", maxLength = 255)
   private String modifiedBy;


   /*******************************************************************************
    ** Get id.
    *******************************************************************************/
   public Integer getId()
   {
      return (id);
   }


   /*******************************************************************************
    ** Set id.
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }


   /*******************************************************************************
    ** Set id fluently.
    *******************************************************************************/
   public EdiCertificate withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get alias.
    *******************************************************************************/
   public String getAlias()
   {
      return (alias);
   }


   /*******************************************************************************
    ** Set alias.
    *******************************************************************************/
   public void setAlias(String alias)
   {
      this.alias = alias;
   }


   /*******************************************************************************
    ** Set alias fluently.
    *******************************************************************************/
   public EdiCertificate withAlias(String alias)
   {
      this.alias = alias;
      return (this);
   }


   /*******************************************************************************
    ** Get partnerId.
    *******************************************************************************/
   public Integer getPartnerId()
   {
      return (partnerId);
   }


   /*******************************************************************************
    ** Set partnerId.
    *******************************************************************************/
   public void setPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
   }


   /*******************************************************************************
    ** Set partnerId fluently.
    *******************************************************************************/
   public EdiCertificate withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
      return (this);
   }


   /*******************************************************************************
    ** Get usage.
    *******************************************************************************/
   public String getUsage()
   {
      return (usage);
   }


   /*******************************************************************************
    ** Set usage.
    *******************************************************************************/
   public void setUsage(String usage)
   {
      this.usage = usage;
   }


   /*******************************************************************************
    ** Set usage fluently.
    *******************************************************************************/
   public EdiCertificate withUsage(String usage)
   {
      this.usage = usage;
      return (this);
   }


   /*******************************************************************************
    ** Get certificatePem.
    *******************************************************************************/
   public String getCertificatePem()
   {
      return (certificatePem);
   }


   /*******************************************************************************
    ** Set certificatePem.
    *******************************************************************************/
   public void setCertificatePem(String certificatePem)
   {
      this.certificatePem = certificatePem;
   }


   /*******************************************************************************
    ** Set certificatePem fluently.
    *******************************************************************************/
   public EdiCertificate withCertificatePem(String certificatePem)
   {
      this.certificatePem = certificatePem;
      return (this);
   }


   /*******************************************************************************
    ** Get subject.
    *******************************************************************************/
   public String getSubject()
   {
      return (subject);
   }


   /*******************************************************************************
    ** Set subject.
    *******************************************************************************/
   public void setSubject(String subject)
   {
      this.subject = subject;
   }


   /*******************************************************************************
    ** Set subject fluently.
    *******************************************************************************/
   public EdiCertificate withSubject(String subject)
   {
      this.subject = subject;
      return (this);
   }


   /*******************************************************************************
    ** Get issuer.
    *******************************************************************************/
   public String getIssuer()
   {
      return (issuer);
   }


   /*******************************************************************************
    ** Set issuer.
    *******************************************************************************/
   public void setIssuer(String issuer)
   {
      this.issuer = issuer;
   }


   /*******************************************************************************
    ** Set issuer fluently.
    *******************************************************************************/
   public EdiCertificate withIssuer(String issuer)
   {
      this.issuer = issuer;
      return (this);
   }


   /*******************************************************************************
    ** Get serialNumber.
    *******************************************************************************/
   public String getSerialNumber()
   {
      return (serialNumber);
   }


   /*******************************************************************************
    ** Set serialNumber.
    *******************************************************************************/
   public void setSerialNumber(String serialNumber)
   {
      this.serialNumber = serialNumber;
   }


   /*******************************************************************************
    ** Set serialNumber fluently.
    *******************************************************************************/
   public EdiCertificate withSerialNumber(String serialNumber)
   {
      this.serialNumber = serialNumber;
      return (this);
   }


   /*******************************************************************************
    ** Get fingerprintSha256.
    *******************************************************************************/
   public String getFingerprintSha256()
   {
      return (fingerprintSha256);
   }


   /*******************************************************************************
    ** Set fingerprintSha256.
    *******************************************************************************/
   public void setFingerprintSha256(String fingerprintSha256)
   {
      this.fingerprintSha256 = fingerprintSha256;
   }


   /*******************************************************************************
    ** Set fingerprintSha256 fluently.
    *******************************************************************************/
   public EdiCertificate withFingerprintSha256(String fingerprintSha256)
   {
      this.fingerprintSha256 = fingerprintSha256;
      return (this);
   }


   /*******************************************************************************
    ** Get notBefore.
    *******************************************************************************/
   public Instant getNotBefore()
   {
      return (notBefore);
   }


   /*******************************************************************************
    ** Set notBefore.
    *******************************************************************************/
   public void setNotBefore(Instant notBefore)
   {
      this.notBefore = notBefore;
   }


   /*******************************************************************************
    ** Set notBefore fluently.
    *******************************************************************************/
   public EdiCertificate withNotBefore(Instant notBefore)
   {
      this.notBefore = notBefore;
      return (this);
   }


   /*******************************************************************************
    ** Get notAfter.
    *******************************************************************************/
   public Instant getNotAfter()
   {
      return (notAfter);
   }


   /*******************************************************************************
    ** Set notAfter.
    *******************************************************************************/
   public void setNotAfter(Instant notAfter)
   {
      this.notAfter = notAfter;
   }


   /*******************************************************************************
    ** Set notAfter fluently.
    *******************************************************************************/
   public EdiCertificate withNotAfter(Instant notAfter)
   {
      this.notAfter = notAfter;
      return (this);
   }


   /*******************************************************************************
    ** Get privateKeyEnvVar.
    *******************************************************************************/
   public String getPrivateKeyEnvVar()
   {
      return (privateKeyEnvVar);
   }


   /*******************************************************************************
    ** Set privateKeyEnvVar.
    *******************************************************************************/
   public void setPrivateKeyEnvVar(String privateKeyEnvVar)
   {
      this.privateKeyEnvVar = privateKeyEnvVar;
   }


   /*******************************************************************************
    ** Set privateKeyEnvVar fluently.
    *******************************************************************************/
   public EdiCertificate withPrivateKeyEnvVar(String privateKeyEnvVar)
   {
      this.privateKeyEnvVar = privateKeyEnvVar;
      return (this);
   }


   /*******************************************************************************
    ** Get privateKeyPasswordEnvVar.
    *******************************************************************************/
   public String getPrivateKeyPasswordEnvVar()
   {
      return (privateKeyPasswordEnvVar);
   }


   /*******************************************************************************
    ** Set privateKeyPasswordEnvVar.
    *******************************************************************************/
   public void setPrivateKeyPasswordEnvVar(String privateKeyPasswordEnvVar)
   {
      this.privateKeyPasswordEnvVar = privateKeyPasswordEnvVar;
   }


   /*******************************************************************************
    ** Set privateKeyPasswordEnvVar fluently.
    *******************************************************************************/
   public EdiCertificate withPrivateKeyPasswordEnvVar(String privateKeyPasswordEnvVar)
   {
      this.privateKeyPasswordEnvVar = privateKeyPasswordEnvVar;
      return (this);
   }


   /*******************************************************************************
    ** Get createDate.
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (createDate);
   }


   /*******************************************************************************
    ** Set createDate.
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }


   /*******************************************************************************
    ** Set createDate fluently.
    *******************************************************************************/
   public EdiCertificate withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }


   /*******************************************************************************
    ** Get modifyDate.
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (modifyDate);
   }


   /*******************************************************************************
    ** Set modifyDate.
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }


   /*******************************************************************************
    ** Set modifyDate fluently.
    *******************************************************************************/
   public EdiCertificate withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }


   /*******************************************************************************
    ** Get modifiedBy.
    *******************************************************************************/
   public String getModifiedBy()
   {
      return (modifiedBy);
   }


   /*******************************************************************************
    ** Set modifiedBy.
    *******************************************************************************/
   public void setModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
   }


   /*******************************************************************************
    ** Set modifiedBy fluently.
    *******************************************************************************/
   public EdiCertificate withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }


   /*******************************************************************************
    ** Get certificate rollover status.
    *******************************************************************************/
   public String getStatus()
   {
      return (status);
   }

   /*******************************************************************************
    ** Set certificate rollover status.
    *******************************************************************************/
   public void setStatus(String status)
   {
      this.status = status;
   }

   /*******************************************************************************
    ** Set certificate rollover status fluently.
    *******************************************************************************/
   public EdiCertificate withStatus(String status)
   {
      this.status = status;
      return (this);
   }
}
