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
 ** Persistent edi_inbox record.
 *******************************************************************************/
public class EdiInbox extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_inbox";

   @QField(backendName = "gateway_user_id", maxLength = 255)
   private String gatewayUserId;

   @QField(backendName = "username", maxLength = 255)
   private String username;

   @QField(backendName = "reprocess")
   private Boolean reprocess;

   @QField(backendName = "use_current_revisions")
   private Boolean useCurrentRevisions;

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "transport_id", possibleValueSourceName = "edi_transport")
   private Integer transportId;

   @QField(backendName = "transport_type", maxLength = 255, possibleValueSourceName = "edi_transport_type")
   private String transportType;

   @QField(backendName = "external_id", maxLength = 255)
   private String externalId;

   @QField(backendName = "payload_ref", maxLength = 255)
   private String payloadRef;

   @QField(backendName = "payload_sha256", maxLength = 255)
   private String payloadSha256;

   @QField(backendName = "size_bytes")
   private Long sizeBytes;

   @QField(backendName = "file_name", maxLength = 255)
   private String fileName;

   @QField(backendName = "remote_address", maxLength = 255)
   private String remoteAddress;

   @QField(backendName = "received_at")
   private Instant receivedAt;

   @QField(backendName = "status", maxLength = 255, possibleValueSourceName = "edi_inbox_status")
   private String status;

   @QField(backendName = "attempts", defaultValue = "0")
   private Integer attempts;

   @QField(backendName = "next_attempt_at")
   private Instant nextAttemptAt;

   @QField(backendName = "claimed_at")
   private Instant claimedAt;

   @QField(backendName = "processed_at")
   private Instant processedAt;

   @QField(backendName = "last_error", maxLength = 255)
   private String lastError;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "create_date", isEditable = false)
   private Instant createDate;

   @QField(backendName = "modify_date", isEditable = false)
   private Instant modifyDate;


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
   public EdiInbox withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get transportId.
    *******************************************************************************/
   public Integer getTransportId()
   {
      return (transportId);
   }


   /*******************************************************************************
    ** Set transportId.
    *******************************************************************************/
   public void setTransportId(Integer transportId)
   {
      this.transportId = transportId;
   }


   /*******************************************************************************
    ** Set transportId fluently.
    *******************************************************************************/
   public EdiInbox withTransportId(Integer transportId)
   {
      this.transportId = transportId;
      return (this);
   }


   /*******************************************************************************
    ** Get transportType.
    *******************************************************************************/
   public String getTransportType()
   {
      return (transportType);
   }


   /*******************************************************************************
    ** Set transportType.
    *******************************************************************************/
   public void setTransportType(String transportType)
   {
      this.transportType = transportType;
   }


   /*******************************************************************************
    ** Set transportType fluently.
    *******************************************************************************/
   public EdiInbox withTransportType(String transportType)
   {
      this.transportType = transportType;
      return (this);
   }


   /*******************************************************************************
    ** Get externalId.
    *******************************************************************************/
   public String getExternalId()
   {
      return (externalId);
   }


   /*******************************************************************************
    ** Set externalId.
    *******************************************************************************/
   public void setExternalId(String externalId)
   {
      this.externalId = externalId;
   }


   /*******************************************************************************
    ** Set externalId fluently.
    *******************************************************************************/
   public EdiInbox withExternalId(String externalId)
   {
      this.externalId = externalId;
      return (this);
   }


   /*******************************************************************************
    ** Get payloadRef.
    *******************************************************************************/
   public String getPayloadRef()
   {
      return (payloadRef);
   }


   /*******************************************************************************
    ** Set payloadRef.
    *******************************************************************************/
   public void setPayloadRef(String payloadRef)
   {
      this.payloadRef = payloadRef;
   }


   /*******************************************************************************
    ** Set payloadRef fluently.
    *******************************************************************************/
   public EdiInbox withPayloadRef(String payloadRef)
   {
      this.payloadRef = payloadRef;
      return (this);
   }


   /*******************************************************************************
    ** Get payloadSha256.
    *******************************************************************************/
   public String getPayloadSha256()
   {
      return (payloadSha256);
   }


   /*******************************************************************************
    ** Set payloadSha256.
    *******************************************************************************/
   public void setPayloadSha256(String payloadSha256)
   {
      this.payloadSha256 = payloadSha256;
   }


   /*******************************************************************************
    ** Set payloadSha256 fluently.
    *******************************************************************************/
   public EdiInbox withPayloadSha256(String payloadSha256)
   {
      this.payloadSha256 = payloadSha256;
      return (this);
   }


   /*******************************************************************************
    ** Get sizeBytes.
    *******************************************************************************/
   public Long getSizeBytes()
   {
      return (sizeBytes);
   }


   /*******************************************************************************
    ** Set sizeBytes.
    *******************************************************************************/
   public void setSizeBytes(Long sizeBytes)
   {
      this.sizeBytes = sizeBytes;
   }


   /*******************************************************************************
    ** Set sizeBytes fluently.
    *******************************************************************************/
   public EdiInbox withSizeBytes(Long sizeBytes)
   {
      this.sizeBytes = sizeBytes;
      return (this);
   }


   /*******************************************************************************
    ** Get fileName.
    *******************************************************************************/
   public String getFileName()
   {
      return (fileName);
   }


   /*******************************************************************************
    ** Set fileName.
    *******************************************************************************/
   public void setFileName(String fileName)
   {
      this.fileName = fileName;
   }


   /*******************************************************************************
    ** Set fileName fluently.
    *******************************************************************************/
   public EdiInbox withFileName(String fileName)
   {
      this.fileName = fileName;
      return (this);
   }


   /*******************************************************************************
    ** Get remoteAddress.
    *******************************************************************************/
   public String getRemoteAddress()
   {
      return (remoteAddress);
   }


   /*******************************************************************************
    ** Set remoteAddress.
    *******************************************************************************/
   public void setRemoteAddress(String remoteAddress)
   {
      this.remoteAddress = remoteAddress;
   }


   /*******************************************************************************
    ** Set remoteAddress fluently.
    *******************************************************************************/
   public EdiInbox withRemoteAddress(String remoteAddress)
   {
      this.remoteAddress = remoteAddress;
      return (this);
   }


   /*******************************************************************************
    ** Get receivedAt.
    *******************************************************************************/
   public Instant getReceivedAt()
   {
      return (receivedAt);
   }


   /*******************************************************************************
    ** Set receivedAt.
    *******************************************************************************/
   public void setReceivedAt(Instant receivedAt)
   {
      this.receivedAt = receivedAt;
   }


   /*******************************************************************************
    ** Set receivedAt fluently.
    *******************************************************************************/
   public EdiInbox withReceivedAt(Instant receivedAt)
   {
      this.receivedAt = receivedAt;
      return (this);
   }


   /*******************************************************************************
    ** Get status.
    *******************************************************************************/
   public String getStatus()
   {
      return (status);
   }


   /*******************************************************************************
    ** Set status.
    *******************************************************************************/
   public void setStatus(String status)
   {
      this.status = status;
   }


   /*******************************************************************************
    ** Set status fluently.
    *******************************************************************************/
   public EdiInbox withStatus(String status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Get attempts.
    *******************************************************************************/
   public Integer getAttempts()
   {
      return (attempts);
   }


   /*******************************************************************************
    ** Set attempts.
    *******************************************************************************/
   public void setAttempts(Integer attempts)
   {
      this.attempts = attempts;
   }


   /*******************************************************************************
    ** Set attempts fluently.
    *******************************************************************************/
   public EdiInbox withAttempts(Integer attempts)
   {
      this.attempts = attempts;
      return (this);
   }


   /*******************************************************************************
    ** Get nextAttemptAt.
    *******************************************************************************/
   public Instant getNextAttemptAt()
   {
      return (nextAttemptAt);
   }


   /*******************************************************************************
    ** Set nextAttemptAt.
    *******************************************************************************/
   public void setNextAttemptAt(Instant nextAttemptAt)
   {
      this.nextAttemptAt = nextAttemptAt;
   }


   /*******************************************************************************
    ** Set nextAttemptAt fluently.
    *******************************************************************************/
   public EdiInbox withNextAttemptAt(Instant nextAttemptAt)
   {
      this.nextAttemptAt = nextAttemptAt;
      return (this);
   }


   /*******************************************************************************
    ** Get claimedAt.
    *******************************************************************************/
   public Instant getClaimedAt()
   {
      return (claimedAt);
   }


   /*******************************************************************************
    ** Set claimedAt.
    *******************************************************************************/
   public void setClaimedAt(Instant claimedAt)
   {
      this.claimedAt = claimedAt;
   }


   /*******************************************************************************
    ** Set claimedAt fluently.
    *******************************************************************************/
   public EdiInbox withClaimedAt(Instant claimedAt)
   {
      this.claimedAt = claimedAt;
      return (this);
   }


   /*******************************************************************************
    ** Get processedAt.
    *******************************************************************************/
   public Instant getProcessedAt()
   {
      return (processedAt);
   }


   /*******************************************************************************
    ** Set processedAt.
    *******************************************************************************/
   public void setProcessedAt(Instant processedAt)
   {
      this.processedAt = processedAt;
   }


   /*******************************************************************************
    ** Set processedAt fluently.
    *******************************************************************************/
   public EdiInbox withProcessedAt(Instant processedAt)
   {
      this.processedAt = processedAt;
      return (this);
   }


   /*******************************************************************************
    ** Get lastError.
    *******************************************************************************/
   public String getLastError()
   {
      return (lastError);
   }


   /*******************************************************************************
    ** Set lastError.
    *******************************************************************************/
   public void setLastError(String lastError)
   {
      this.lastError = lastError;
   }


   /*******************************************************************************
    ** Set lastError fluently.
    *******************************************************************************/
   public EdiInbox withLastError(String lastError)
   {
      this.lastError = lastError;
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
   public EdiInbox withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
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
   public EdiInbox withCreateDate(Instant createDate)
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
   public EdiInbox withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }


   /*******************************************************************************
    ** Get gatewayUserId.
    *******************************************************************************/
   public String getGatewayUserId()
   {
      return (gatewayUserId);
   }

   /*******************************************************************************
    ** Set gatewayUserId.
    *******************************************************************************/
   public void setGatewayUserId(String value)
   {
      gatewayUserId = value;
   }

   /*******************************************************************************
    ** Configure gatewayUserId.
    *******************************************************************************/
   public EdiInbox withGatewayUserId(String value)
   {
      gatewayUserId = value;
      return (this);
   }

   /*******************************************************************************
    ** Get username.
    *******************************************************************************/
   public String getUsername()
   {
      return (username);
   }

   /*******************************************************************************
    ** Set username.
    *******************************************************************************/
   public void setUsername(String value)
   {
      username = value;
   }

   /*******************************************************************************
    ** Configure username.
    *******************************************************************************/
   public EdiInbox withUsername(String value)
   {
      username = value;
      return (this);
   }

   /*******************************************************************************
    ** Get reprocess.
    *******************************************************************************/
   public Boolean getReprocess()
   {
      return (reprocess);
   }

   /*******************************************************************************
    ** Set reprocess.
    *******************************************************************************/
   public void setReprocess(Boolean value)
   {
      reprocess = value;
   }

   /*******************************************************************************
    ** Configure reprocess.
    *******************************************************************************/
   public EdiInbox withReprocess(Boolean value)
   {
      reprocess = value;
      return (this);
   }

   /*******************************************************************************
    ** Get useCurrentRevisions.
    *******************************************************************************/
   public Boolean getUseCurrentRevisions()
   {
      return (useCurrentRevisions);
   }

   /*******************************************************************************
    ** Set useCurrentRevisions.
    *******************************************************************************/
   public void setUseCurrentRevisions(Boolean value)
   {
      useCurrentRevisions = value;
   }

   /*******************************************************************************
    ** Configure useCurrentRevisions.
    *******************************************************************************/
   public EdiInbox withUseCurrentRevisions(Boolean value)
   {
      useCurrentRevisions = value;
      return (this);
   }
}
