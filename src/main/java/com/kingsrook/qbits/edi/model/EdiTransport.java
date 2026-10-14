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
 ** Persistent edi_transport record.
 *******************************************************************************/
public class EdiTransport extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_transport";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "type", maxLength = 255, possibleValueSourceName = "edi_transport_type")
   private String type;

   @QField(backendName = "direction", maxLength = 255, possibleValueSourceName = "edi_transport_direction")
   private String direction;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "is_active", defaultValue = "true")
   private Boolean isActive;

   @QField(backendName = "notes", maxLength = 10000)
   private String notes;

   @QField(backendName = "as2_local_id", maxLength = 255)
   private String as2LocalId;

   @QField(backendName = "as2_partner_id", maxLength = 255)
   private String as2PartnerId;

   @QField(backendName = "as2_url", maxLength = 255)
   private String as2Url;

   @QField(backendName = "as2_subject", maxLength = 255)
   private String as2Subject;

   @QField(backendName = "sign_algorithm", maxLength = 255, possibleValueSourceName = "edi_sign_algorithm")
   private String signAlgorithm;

   @QField(backendName = "encrypt_algorithm", maxLength = 255, possibleValueSourceName = "edi_encrypt_algorithm")
   private String encryptAlgorithm;

   @QField(backendName = "compress")
   private Boolean compress;

   @QField(backendName = "mdn_mode", maxLength = 255, possibleValueSourceName = "edi_mdn_mode")
   private String mdnMode;

   @QField(backendName = "mdn_signed")
   private Boolean mdnSigned;

   @QField(backendName = "weak_crypto", isEditable = false)
   private Boolean weakCrypto;

   @QField(backendName = "local_certificate_id", possibleValueSourceName = "edi_certificate")
   private Integer localCertificateId;

   @QField(backendName = "partner_certificate_id", possibleValueSourceName = "edi_certificate")
   private Integer partnerCertificateId;

   @QField(backendName = "partner_tls_certificate_id", possibleValueSourceName = "edi_certificate")
   private Integer partnerTlsCertificateId;

   @QField(backendName = "http_auth_user_env_var", maxLength = 255)
   private String httpAuthUserEnvVar;

   @QField(backendName = "http_auth_password_env_var", maxLength = 255)
   private String httpAuthPasswordEnvVar;

   @QField(backendName = "sftp_host", maxLength = 255)
   private String sftpHost;

   @QField(backendName = "sftp_port")
   private Integer sftpPort;

   @QField(backendName = "sftp_username", maxLength = 255)
   private String sftpUsername;

   @QField(backendName = "sftp_password_env_var", maxLength = 255)
   private String sftpPasswordEnvVar;

   @QField(backendName = "sftp_private_key_env_var", maxLength = 255)
   private String sftpPrivateKeyEnvVar;

   @QField(backendName = "sftp_host_key_fingerprint", maxLength = 255)
   private String sftpHostKeyFingerprint;

   @QField(backendName = "sftp_outbound_path", maxLength = 255)
   private String sftpOutboundPath;

   @QField(backendName = "sftp_inbound_path", maxLength = 255)
   private String sftpInboundPath;

   @QField(backendName = "sftp_poll_cron", maxLength = 255)
   private String sftpPollCron;

   @QField(backendName = "sftp_after_pickup", maxLength = 255, possibleValueSourceName = "edi_sftp_after_pickup")
   private String sftpAfterPickup;

   @QField(backendName = "sftp_archive_path", maxLength = 255)
   private String sftpArchivePath;

   @QField(backendName = "sftp_file_pattern", maxLength = 255)
   private String sftpFilePattern;

   @QField(backendName = "sftp_temp_suffix", maxLength = 255)
   private String sftpTempSuffix;

   @QField(backendName = "gateway_endpoint_name", maxLength = 255)
   private String gatewayEndpointName;

   @QField(backendName = "https_url", maxLength = 255)
   private String httpsUrl;

   @QField(backendName = "https_auth_type", maxLength = 255, possibleValueSourceName = "edi_https_auth_type")
   private String httpsAuthType;

   @QField(backendName = "https_secret_env_var", maxLength = 255)
   private String httpsSecretEnvVar;

   @QField(backendName = "https_headers_json", maxLength = 10000)
   private String httpsHeadersJson;

   @QField(backendName = "https_inbound_auth", maxLength = 255, possibleValueSourceName = "edi_https_inbound_auth")
   private String httpsInboundAuth;

   @QField(backendName = "email_provider_name", maxLength = 255)
   private String emailProviderName;

   @QField(backendName = "email_to", maxLength = 255)
   private String emailTo;

   @QField(backendName = "email_from_alias", maxLength = 255)
   private String emailFromAlias;

   @QField(backendName = "email_attachment_name", maxLength = 255)
   private String emailAttachmentName;

   @QField(backendName = "is_van")
   private Boolean isVan;

   @QField(backendName = "van_name", maxLength = 255)
   private String vanName;

   @QField(backendName = "max_attempts", defaultValue = "8")
   private Integer maxAttempts;

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
   public EdiTransport withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }


   /*******************************************************************************
    ** Set name.
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }


   /*******************************************************************************
    ** Set name fluently.
    *******************************************************************************/
   public EdiTransport withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Get type.
    *******************************************************************************/
   public String getType()
   {
      return (type);
   }


   /*******************************************************************************
    ** Set type.
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;
   }


   /*******************************************************************************
    ** Set type fluently.
    *******************************************************************************/
   public EdiTransport withType(String type)
   {
      this.type = type;
      return (this);
   }


   /*******************************************************************************
    ** Get direction.
    *******************************************************************************/
   public String getDirection()
   {
      return (direction);
   }


   /*******************************************************************************
    ** Set direction.
    *******************************************************************************/
   public void setDirection(String direction)
   {
      this.direction = direction;
   }


   /*******************************************************************************
    ** Set direction fluently.
    *******************************************************************************/
   public EdiTransport withDirection(String direction)
   {
      this.direction = direction;
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
   public EdiTransport withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
      return (this);
   }


   /*******************************************************************************
    ** Get isActive.
    *******************************************************************************/
   public Boolean getIsActive()
   {
      return (isActive);
   }


   /*******************************************************************************
    ** Set isActive.
    *******************************************************************************/
   public void setIsActive(Boolean isActive)
   {
      this.isActive = isActive;
   }


   /*******************************************************************************
    ** Set isActive fluently.
    *******************************************************************************/
   public EdiTransport withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }


   /*******************************************************************************
    ** Get notes.
    *******************************************************************************/
   public String getNotes()
   {
      return (notes);
   }


   /*******************************************************************************
    ** Set notes.
    *******************************************************************************/
   public void setNotes(String notes)
   {
      this.notes = notes;
   }


   /*******************************************************************************
    ** Set notes fluently.
    *******************************************************************************/
   public EdiTransport withNotes(String notes)
   {
      this.notes = notes;
      return (this);
   }


   /*******************************************************************************
    ** Get as2LocalId.
    *******************************************************************************/
   public String getAs2LocalId()
   {
      return (as2LocalId);
   }


   /*******************************************************************************
    ** Set as2LocalId.
    *******************************************************************************/
   public void setAs2LocalId(String as2LocalId)
   {
      this.as2LocalId = as2LocalId;
   }


   /*******************************************************************************
    ** Set as2LocalId fluently.
    *******************************************************************************/
   public EdiTransport withAs2LocalId(String as2LocalId)
   {
      this.as2LocalId = as2LocalId;
      return (this);
   }


   /*******************************************************************************
    ** Get as2PartnerId.
    *******************************************************************************/
   public String getAs2PartnerId()
   {
      return (as2PartnerId);
   }


   /*******************************************************************************
    ** Set as2PartnerId.
    *******************************************************************************/
   public void setAs2PartnerId(String as2PartnerId)
   {
      this.as2PartnerId = as2PartnerId;
   }


   /*******************************************************************************
    ** Set as2PartnerId fluently.
    *******************************************************************************/
   public EdiTransport withAs2PartnerId(String as2PartnerId)
   {
      this.as2PartnerId = as2PartnerId;
      return (this);
   }


   /*******************************************************************************
    ** Get as2Url.
    *******************************************************************************/
   public String getAs2Url()
   {
      return (as2Url);
   }


   /*******************************************************************************
    ** Set as2Url.
    *******************************************************************************/
   public void setAs2Url(String as2Url)
   {
      this.as2Url = as2Url;
   }


   /*******************************************************************************
    ** Set as2Url fluently.
    *******************************************************************************/
   public EdiTransport withAs2Url(String as2Url)
   {
      this.as2Url = as2Url;
      return (this);
   }


   /*******************************************************************************
    ** Get as2Subject.
    *******************************************************************************/
   public String getAs2Subject()
   {
      return (as2Subject);
   }


   /*******************************************************************************
    ** Set as2Subject.
    *******************************************************************************/
   public void setAs2Subject(String as2Subject)
   {
      this.as2Subject = as2Subject;
   }


   /*******************************************************************************
    ** Set as2Subject fluently.
    *******************************************************************************/
   public EdiTransport withAs2Subject(String as2Subject)
   {
      this.as2Subject = as2Subject;
      return (this);
   }


   /*******************************************************************************
    ** Get signAlgorithm.
    *******************************************************************************/
   public String getSignAlgorithm()
   {
      return (signAlgorithm);
   }


   /*******************************************************************************
    ** Set signAlgorithm.
    *******************************************************************************/
   public void setSignAlgorithm(String signAlgorithm)
   {
      this.signAlgorithm = signAlgorithm;
   }


   /*******************************************************************************
    ** Set signAlgorithm fluently.
    *******************************************************************************/
   public EdiTransport withSignAlgorithm(String signAlgorithm)
   {
      this.signAlgorithm = signAlgorithm;
      return (this);
   }


   /*******************************************************************************
    ** Get encryptAlgorithm.
    *******************************************************************************/
   public String getEncryptAlgorithm()
   {
      return (encryptAlgorithm);
   }


   /*******************************************************************************
    ** Set encryptAlgorithm.
    *******************************************************************************/
   public void setEncryptAlgorithm(String encryptAlgorithm)
   {
      this.encryptAlgorithm = encryptAlgorithm;
   }


   /*******************************************************************************
    ** Set encryptAlgorithm fluently.
    *******************************************************************************/
   public EdiTransport withEncryptAlgorithm(String encryptAlgorithm)
   {
      this.encryptAlgorithm = encryptAlgorithm;
      return (this);
   }


   /*******************************************************************************
    ** Get compress.
    *******************************************************************************/
   public Boolean getCompress()
   {
      return (compress);
   }


   /*******************************************************************************
    ** Set compress.
    *******************************************************************************/
   public void setCompress(Boolean compress)
   {
      this.compress = compress;
   }


   /*******************************************************************************
    ** Set compress fluently.
    *******************************************************************************/
   public EdiTransport withCompress(Boolean compress)
   {
      this.compress = compress;
      return (this);
   }


   /*******************************************************************************
    ** Get mdnMode.
    *******************************************************************************/
   public String getMdnMode()
   {
      return (mdnMode);
   }


   /*******************************************************************************
    ** Set mdnMode.
    *******************************************************************************/
   public void setMdnMode(String mdnMode)
   {
      this.mdnMode = mdnMode;
   }


   /*******************************************************************************
    ** Set mdnMode fluently.
    *******************************************************************************/
   public EdiTransport withMdnMode(String mdnMode)
   {
      this.mdnMode = mdnMode;
      return (this);
   }


   /*******************************************************************************
    ** Get mdnSigned.
    *******************************************************************************/
   public Boolean getMdnSigned()
   {
      return (mdnSigned);
   }


   /*******************************************************************************
    ** Set mdnSigned.
    *******************************************************************************/
   public void setMdnSigned(Boolean mdnSigned)
   {
      this.mdnSigned = mdnSigned;
   }


   /*******************************************************************************
    ** Set mdnSigned fluently.
    *******************************************************************************/
   public EdiTransport withMdnSigned(Boolean mdnSigned)
   {
      this.mdnSigned = mdnSigned;
      return (this);
   }


   /*******************************************************************************
    ** Get localCertificateId.
    *******************************************************************************/
   public Integer getLocalCertificateId()
   {
      return (localCertificateId);
   }


   /*******************************************************************************
    ** Set localCertificateId.
    *******************************************************************************/
   public void setLocalCertificateId(Integer localCertificateId)
   {
      this.localCertificateId = localCertificateId;
   }


   /*******************************************************************************
    ** Set localCertificateId fluently.
    *******************************************************************************/
   public EdiTransport withLocalCertificateId(Integer localCertificateId)
   {
      this.localCertificateId = localCertificateId;
      return (this);
   }


   /*******************************************************************************
    ** Get partnerCertificateId.
    *******************************************************************************/
   public Integer getPartnerCertificateId()
   {
      return (partnerCertificateId);
   }


   /*******************************************************************************
    ** Set partnerCertificateId.
    *******************************************************************************/
   public void setPartnerCertificateId(Integer partnerCertificateId)
   {
      this.partnerCertificateId = partnerCertificateId;
   }


   /*******************************************************************************
    ** Set partnerCertificateId fluently.
    *******************************************************************************/
   public EdiTransport withPartnerCertificateId(Integer partnerCertificateId)
   {
      this.partnerCertificateId = partnerCertificateId;
      return (this);
   }


   /*******************************************************************************
    ** Get partnerTlsCertificateId.
    *******************************************************************************/
   public Integer getPartnerTlsCertificateId()
   {
      return (partnerTlsCertificateId);
   }


   /*******************************************************************************
    ** Set partnerTlsCertificateId.
    *******************************************************************************/
   public void setPartnerTlsCertificateId(Integer partnerTlsCertificateId)
   {
      this.partnerTlsCertificateId = partnerTlsCertificateId;
   }


   /*******************************************************************************
    ** Set partnerTlsCertificateId fluently.
    *******************************************************************************/
   public EdiTransport withPartnerTlsCertificateId(Integer partnerTlsCertificateId)
   {
      this.partnerTlsCertificateId = partnerTlsCertificateId;
      return (this);
   }


   /*******************************************************************************
    ** Get httpAuthUserEnvVar.
    *******************************************************************************/
   public String getHttpAuthUserEnvVar()
   {
      return (httpAuthUserEnvVar);
   }


   /*******************************************************************************
    ** Set httpAuthUserEnvVar.
    *******************************************************************************/
   public void setHttpAuthUserEnvVar(String httpAuthUserEnvVar)
   {
      this.httpAuthUserEnvVar = httpAuthUserEnvVar;
   }


   /*******************************************************************************
    ** Set httpAuthUserEnvVar fluently.
    *******************************************************************************/
   public EdiTransport withHttpAuthUserEnvVar(String httpAuthUserEnvVar)
   {
      this.httpAuthUserEnvVar = httpAuthUserEnvVar;
      return (this);
   }


   /*******************************************************************************
    ** Get httpAuthPasswordEnvVar.
    *******************************************************************************/
   public String getHttpAuthPasswordEnvVar()
   {
      return (httpAuthPasswordEnvVar);
   }


   /*******************************************************************************
    ** Set httpAuthPasswordEnvVar.
    *******************************************************************************/
   public void setHttpAuthPasswordEnvVar(String httpAuthPasswordEnvVar)
   {
      this.httpAuthPasswordEnvVar = httpAuthPasswordEnvVar;
   }


   /*******************************************************************************
    ** Set httpAuthPasswordEnvVar fluently.
    *******************************************************************************/
   public EdiTransport withHttpAuthPasswordEnvVar(String httpAuthPasswordEnvVar)
   {
      this.httpAuthPasswordEnvVar = httpAuthPasswordEnvVar;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpHost.
    *******************************************************************************/
   public String getSftpHost()
   {
      return (sftpHost);
   }


   /*******************************************************************************
    ** Set sftpHost.
    *******************************************************************************/
   public void setSftpHost(String sftpHost)
   {
      this.sftpHost = sftpHost;
   }


   /*******************************************************************************
    ** Set sftpHost fluently.
    *******************************************************************************/
   public EdiTransport withSftpHost(String sftpHost)
   {
      this.sftpHost = sftpHost;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpPort.
    *******************************************************************************/
   public Integer getSftpPort()
   {
      return (sftpPort);
   }


   /*******************************************************************************
    ** Set sftpPort.
    *******************************************************************************/
   public void setSftpPort(Integer sftpPort)
   {
      this.sftpPort = sftpPort;
   }


   /*******************************************************************************
    ** Set sftpPort fluently.
    *******************************************************************************/
   public EdiTransport withSftpPort(Integer sftpPort)
   {
      this.sftpPort = sftpPort;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpUsername.
    *******************************************************************************/
   public String getSftpUsername()
   {
      return (sftpUsername);
   }


   /*******************************************************************************
    ** Set sftpUsername.
    *******************************************************************************/
   public void setSftpUsername(String sftpUsername)
   {
      this.sftpUsername = sftpUsername;
   }


   /*******************************************************************************
    ** Set sftpUsername fluently.
    *******************************************************************************/
   public EdiTransport withSftpUsername(String sftpUsername)
   {
      this.sftpUsername = sftpUsername;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpPasswordEnvVar.
    *******************************************************************************/
   public String getSftpPasswordEnvVar()
   {
      return (sftpPasswordEnvVar);
   }


   /*******************************************************************************
    ** Set sftpPasswordEnvVar.
    *******************************************************************************/
   public void setSftpPasswordEnvVar(String sftpPasswordEnvVar)
   {
      this.sftpPasswordEnvVar = sftpPasswordEnvVar;
   }


   /*******************************************************************************
    ** Set sftpPasswordEnvVar fluently.
    *******************************************************************************/
   public EdiTransport withSftpPasswordEnvVar(String sftpPasswordEnvVar)
   {
      this.sftpPasswordEnvVar = sftpPasswordEnvVar;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpPrivateKeyEnvVar.
    *******************************************************************************/
   public String getSftpPrivateKeyEnvVar()
   {
      return (sftpPrivateKeyEnvVar);
   }


   /*******************************************************************************
    ** Set sftpPrivateKeyEnvVar.
    *******************************************************************************/
   public void setSftpPrivateKeyEnvVar(String sftpPrivateKeyEnvVar)
   {
      this.sftpPrivateKeyEnvVar = sftpPrivateKeyEnvVar;
   }


   /*******************************************************************************
    ** Set sftpPrivateKeyEnvVar fluently.
    *******************************************************************************/
   public EdiTransport withSftpPrivateKeyEnvVar(String sftpPrivateKeyEnvVar)
   {
      this.sftpPrivateKeyEnvVar = sftpPrivateKeyEnvVar;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpHostKeyFingerprint.
    *******************************************************************************/
   public String getSftpHostKeyFingerprint()
   {
      return (sftpHostKeyFingerprint);
   }


   /*******************************************************************************
    ** Set sftpHostKeyFingerprint.
    *******************************************************************************/
   public void setSftpHostKeyFingerprint(String sftpHostKeyFingerprint)
   {
      this.sftpHostKeyFingerprint = sftpHostKeyFingerprint;
   }


   /*******************************************************************************
    ** Set sftpHostKeyFingerprint fluently.
    *******************************************************************************/
   public EdiTransport withSftpHostKeyFingerprint(String sftpHostKeyFingerprint)
   {
      this.sftpHostKeyFingerprint = sftpHostKeyFingerprint;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpOutboundPath.
    *******************************************************************************/
   public String getSftpOutboundPath()
   {
      return (sftpOutboundPath);
   }


   /*******************************************************************************
    ** Set sftpOutboundPath.
    *******************************************************************************/
   public void setSftpOutboundPath(String sftpOutboundPath)
   {
      this.sftpOutboundPath = sftpOutboundPath;
   }


   /*******************************************************************************
    ** Set sftpOutboundPath fluently.
    *******************************************************************************/
   public EdiTransport withSftpOutboundPath(String sftpOutboundPath)
   {
      this.sftpOutboundPath = sftpOutboundPath;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpInboundPath.
    *******************************************************************************/
   public String getSftpInboundPath()
   {
      return (sftpInboundPath);
   }


   /*******************************************************************************
    ** Set sftpInboundPath.
    *******************************************************************************/
   public void setSftpInboundPath(String sftpInboundPath)
   {
      this.sftpInboundPath = sftpInboundPath;
   }


   /*******************************************************************************
    ** Set sftpInboundPath fluently.
    *******************************************************************************/
   public EdiTransport withSftpInboundPath(String sftpInboundPath)
   {
      this.sftpInboundPath = sftpInboundPath;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpPollCron.
    *******************************************************************************/
   public String getSftpPollCron()
   {
      return (sftpPollCron);
   }


   /*******************************************************************************
    ** Set sftpPollCron.
    *******************************************************************************/
   public void setSftpPollCron(String sftpPollCron)
   {
      this.sftpPollCron = sftpPollCron;
   }


   /*******************************************************************************
    ** Set sftpPollCron fluently.
    *******************************************************************************/
   public EdiTransport withSftpPollCron(String sftpPollCron)
   {
      this.sftpPollCron = sftpPollCron;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpAfterPickup.
    *******************************************************************************/
   public String getSftpAfterPickup()
   {
      return (sftpAfterPickup);
   }


   /*******************************************************************************
    ** Set sftpAfterPickup.
    *******************************************************************************/
   public void setSftpAfterPickup(String sftpAfterPickup)
   {
      this.sftpAfterPickup = sftpAfterPickup;
   }


   /*******************************************************************************
    ** Set sftpAfterPickup fluently.
    *******************************************************************************/
   public EdiTransport withSftpAfterPickup(String sftpAfterPickup)
   {
      this.sftpAfterPickup = sftpAfterPickup;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpArchivePath.
    *******************************************************************************/
   public String getSftpArchivePath()
   {
      return (sftpArchivePath);
   }


   /*******************************************************************************
    ** Set sftpArchivePath.
    *******************************************************************************/
   public void setSftpArchivePath(String sftpArchivePath)
   {
      this.sftpArchivePath = sftpArchivePath;
   }


   /*******************************************************************************
    ** Set sftpArchivePath fluently.
    *******************************************************************************/
   public EdiTransport withSftpArchivePath(String sftpArchivePath)
   {
      this.sftpArchivePath = sftpArchivePath;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpFilePattern.
    *******************************************************************************/
   public String getSftpFilePattern()
   {
      return (sftpFilePattern);
   }


   /*******************************************************************************
    ** Set sftpFilePattern.
    *******************************************************************************/
   public void setSftpFilePattern(String sftpFilePattern)
   {
      this.sftpFilePattern = sftpFilePattern;
   }


   /*******************************************************************************
    ** Set sftpFilePattern fluently.
    *******************************************************************************/
   public EdiTransport withSftpFilePattern(String sftpFilePattern)
   {
      this.sftpFilePattern = sftpFilePattern;
      return (this);
   }


   /*******************************************************************************
    ** Get sftpTempSuffix.
    *******************************************************************************/
   public String getSftpTempSuffix()
   {
      return (sftpTempSuffix);
   }


   /*******************************************************************************
    ** Set sftpTempSuffix.
    *******************************************************************************/
   public void setSftpTempSuffix(String sftpTempSuffix)
   {
      this.sftpTempSuffix = sftpTempSuffix;
   }


   /*******************************************************************************
    ** Set sftpTempSuffix fluently.
    *******************************************************************************/
   public EdiTransport withSftpTempSuffix(String sftpTempSuffix)
   {
      this.sftpTempSuffix = sftpTempSuffix;
      return (this);
   }


   /*******************************************************************************
    ** Get gatewayEndpointName.
    *******************************************************************************/
   public String getGatewayEndpointName()
   {
      return (gatewayEndpointName);
   }


   /*******************************************************************************
    ** Set gatewayEndpointName.
    *******************************************************************************/
   public void setGatewayEndpointName(String gatewayEndpointName)
   {
      this.gatewayEndpointName = gatewayEndpointName;
   }


   /*******************************************************************************
    ** Set gatewayEndpointName fluently.
    *******************************************************************************/
   public EdiTransport withGatewayEndpointName(String gatewayEndpointName)
   {
      this.gatewayEndpointName = gatewayEndpointName;
      return (this);
   }


   /*******************************************************************************
    ** Get httpsUrl.
    *******************************************************************************/
   public String getHttpsUrl()
   {
      return (httpsUrl);
   }


   /*******************************************************************************
    ** Set httpsUrl.
    *******************************************************************************/
   public void setHttpsUrl(String httpsUrl)
   {
      this.httpsUrl = httpsUrl;
   }


   /*******************************************************************************
    ** Set httpsUrl fluently.
    *******************************************************************************/
   public EdiTransport withHttpsUrl(String httpsUrl)
   {
      this.httpsUrl = httpsUrl;
      return (this);
   }


   /*******************************************************************************
    ** Get httpsAuthType.
    *******************************************************************************/
   public String getHttpsAuthType()
   {
      return (httpsAuthType);
   }


   /*******************************************************************************
    ** Set httpsAuthType.
    *******************************************************************************/
   public void setHttpsAuthType(String httpsAuthType)
   {
      this.httpsAuthType = httpsAuthType;
   }


   /*******************************************************************************
    ** Set httpsAuthType fluently.
    *******************************************************************************/
   public EdiTransport withHttpsAuthType(String httpsAuthType)
   {
      this.httpsAuthType = httpsAuthType;
      return (this);
   }


   /*******************************************************************************
    ** Get httpsSecretEnvVar.
    *******************************************************************************/
   public String getHttpsSecretEnvVar()
   {
      return (httpsSecretEnvVar);
   }


   /*******************************************************************************
    ** Set httpsSecretEnvVar.
    *******************************************************************************/
   public void setHttpsSecretEnvVar(String httpsSecretEnvVar)
   {
      this.httpsSecretEnvVar = httpsSecretEnvVar;
   }


   /*******************************************************************************
    ** Set httpsSecretEnvVar fluently.
    *******************************************************************************/
   public EdiTransport withHttpsSecretEnvVar(String httpsSecretEnvVar)
   {
      this.httpsSecretEnvVar = httpsSecretEnvVar;
      return (this);
   }


   /*******************************************************************************
    ** Get httpsHeadersJson.
    *******************************************************************************/
   public String getHttpsHeadersJson()
   {
      return (httpsHeadersJson);
   }


   /*******************************************************************************
    ** Set httpsHeadersJson.
    *******************************************************************************/
   public void setHttpsHeadersJson(String httpsHeadersJson)
   {
      this.httpsHeadersJson = httpsHeadersJson;
   }


   /*******************************************************************************
    ** Set httpsHeadersJson fluently.
    *******************************************************************************/
   public EdiTransport withHttpsHeadersJson(String httpsHeadersJson)
   {
      this.httpsHeadersJson = httpsHeadersJson;
      return (this);
   }


   /*******************************************************************************
    ** Get httpsInboundAuth.
    *******************************************************************************/
   public String getHttpsInboundAuth()
   {
      return (httpsInboundAuth);
   }


   /*******************************************************************************
    ** Set httpsInboundAuth.
    *******************************************************************************/
   public void setHttpsInboundAuth(String httpsInboundAuth)
   {
      this.httpsInboundAuth = httpsInboundAuth;
   }


   /*******************************************************************************
    ** Set httpsInboundAuth fluently.
    *******************************************************************************/
   public EdiTransport withHttpsInboundAuth(String httpsInboundAuth)
   {
      this.httpsInboundAuth = httpsInboundAuth;
      return (this);
   }


   /*******************************************************************************
    ** Get emailProviderName.
    *******************************************************************************/
   public String getEmailProviderName()
   {
      return (emailProviderName);
   }


   /*******************************************************************************
    ** Set emailProviderName.
    *******************************************************************************/
   public void setEmailProviderName(String emailProviderName)
   {
      this.emailProviderName = emailProviderName;
   }


   /*******************************************************************************
    ** Set emailProviderName fluently.
    *******************************************************************************/
   public EdiTransport withEmailProviderName(String emailProviderName)
   {
      this.emailProviderName = emailProviderName;
      return (this);
   }


   /*******************************************************************************
    ** Get emailTo.
    *******************************************************************************/
   public String getEmailTo()
   {
      return (emailTo);
   }


   /*******************************************************************************
    ** Set emailTo.
    *******************************************************************************/
   public void setEmailTo(String emailTo)
   {
      this.emailTo = emailTo;
   }


   /*******************************************************************************
    ** Set emailTo fluently.
    *******************************************************************************/
   public EdiTransport withEmailTo(String emailTo)
   {
      this.emailTo = emailTo;
      return (this);
   }


   /*******************************************************************************
    ** Get emailFromAlias.
    *******************************************************************************/
   public String getEmailFromAlias()
   {
      return (emailFromAlias);
   }


   /*******************************************************************************
    ** Set emailFromAlias.
    *******************************************************************************/
   public void setEmailFromAlias(String emailFromAlias)
   {
      this.emailFromAlias = emailFromAlias;
   }


   /*******************************************************************************
    ** Set emailFromAlias fluently.
    *******************************************************************************/
   public EdiTransport withEmailFromAlias(String emailFromAlias)
   {
      this.emailFromAlias = emailFromAlias;
      return (this);
   }


   /*******************************************************************************
    ** Get emailAttachmentName.
    *******************************************************************************/
   public String getEmailAttachmentName()
   {
      return (emailAttachmentName);
   }


   /*******************************************************************************
    ** Set emailAttachmentName.
    *******************************************************************************/
   public void setEmailAttachmentName(String emailAttachmentName)
   {
      this.emailAttachmentName = emailAttachmentName;
   }


   /*******************************************************************************
    ** Set emailAttachmentName fluently.
    *******************************************************************************/
   public EdiTransport withEmailAttachmentName(String emailAttachmentName)
   {
      this.emailAttachmentName = emailAttachmentName;
      return (this);
   }


   /*******************************************************************************
    ** Get isVan.
    *******************************************************************************/
   public Boolean getIsVan()
   {
      return (isVan);
   }


   /*******************************************************************************
    ** Set isVan.
    *******************************************************************************/
   public void setIsVan(Boolean isVan)
   {
      this.isVan = isVan;
   }


   /*******************************************************************************
    ** Set isVan fluently.
    *******************************************************************************/
   public EdiTransport withIsVan(Boolean isVan)
   {
      this.isVan = isVan;
      return (this);
   }


   /*******************************************************************************
    ** Get vanName.
    *******************************************************************************/
   public String getVanName()
   {
      return (vanName);
   }


   /*******************************************************************************
    ** Set vanName.
    *******************************************************************************/
   public void setVanName(String vanName)
   {
      this.vanName = vanName;
   }


   /*******************************************************************************
    ** Set vanName fluently.
    *******************************************************************************/
   public EdiTransport withVanName(String vanName)
   {
      this.vanName = vanName;
      return (this);
   }


   /*******************************************************************************
    ** Get maxAttempts.
    *******************************************************************************/
   public Integer getMaxAttempts()
   {
      return (maxAttempts);
   }


   /*******************************************************************************
    ** Set maxAttempts.
    *******************************************************************************/
   public void setMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
   }


   /*******************************************************************************
    ** Set maxAttempts fluently.
    *******************************************************************************/
   public EdiTransport withMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
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
   public EdiTransport withCreateDate(Instant createDate)
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
   public EdiTransport withModifyDate(Instant modifyDate)
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
   public EdiTransport withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }


   /*******************************************************************************
    ** Get weakCrypto: true when SHA1 or 3DES is configured. Set by the qbit on save.
    *******************************************************************************/
   public Boolean getWeakCrypto()
   {
      return (weakCrypto);
   }

   /*******************************************************************************
    ** Set weakCrypto.
    *******************************************************************************/
   public void setWeakCrypto(Boolean weakCrypto)
   {
      this.weakCrypto = weakCrypto;
   }

   /*******************************************************************************
    ** Set weakCrypto fluently.
    *******************************************************************************/
   public EdiTransport withWeakCrypto(Boolean weakCrypto)
   {
      this.weakCrypto = weakCrypto;
      return (this);
   }
}
