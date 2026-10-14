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
 ** Persistent edi_outbound_message record.
 *******************************************************************************/
public class EdiOutboundMessage extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_outbound_message";

   @QField(backendName = "interchange_id", possibleValueSourceName = "edi_interchange")
   private Integer interchangeId;

   @QField(backendName = "transport_message_id", maxLength = 255)
   private String transportMessageId;

   @QField(backendName = "transport_mic", maxLength = 255)
   private String transportMic;

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "agreement_id", possibleValueSourceName = "edi_agreement")
   private Integer agreementId;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "transport_id", possibleValueSourceName = "edi_transport")
   private Integer transportId;

   @QField(backendName = "outbound_request_id", possibleValueSourceName = "edi_outbound_request")
   private Integer outboundRequestId;

   @QField(backendName = "payload_ref", maxLength = 255)
   private String payloadRef;

   @QField(backendName = "payload_sha256", maxLength = 255)
   private String payloadSha256;

   @QField(backendName = "size_bytes")
   private Long sizeBytes;

   @QField(backendName = "status", maxLength = 255)
   private String status;

   @QField(backendName = "attempts")
   private Integer attempts;

   @QField(backendName = "next_attempt_at")
   private Instant nextAttemptAt;

   @QField(backendName = "claimed_at")
   private Instant claimedAt;

   @QField(backendName = "sent_at")
   private Instant sentAt;

   @QField(backendName = "delivered_at")
   private Instant deliveredAt;

   @QField(backendName = "last_error", maxLength = 255)
   private String lastError;

   @QField(backendName = "ack_for_group_id", possibleValueSourceName = "edi_functional_group")
   private Integer ackForGroupId;

   @QField(backendName = "ack_for_interchange_id", possibleValueSourceName = "edi_interchange")
   private Integer ackForInterchangeId;

   @QField(backendName = "create_date")
   private Instant createDate;

   @QField(backendName = "modify_date")
   private Instant modifyDate;


   /*******************************************************************************
    ** Get transportMic.
    *******************************************************************************/
   public String getTransportMic()
   {
      return (transportMic);
   }

   /*******************************************************************************
    ** Set transportMic.
    *******************************************************************************/
   public void setTransportMic(String value)
   {
      transportMic = value;
   }

   /*******************************************************************************
    ** Configure transportMic.
    *******************************************************************************/
   public EdiOutboundMessage withTransportMic(String value)
   {
      transportMic = value;
      return (this);
   }

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
   public void setId(Integer value)
   {
      id = value;
   }

   /*******************************************************************************
    ** Configure id.
    *******************************************************************************/
   public EdiOutboundMessage withId(Integer value)
   {
      id = value;
      return (this);
   }

   /*******************************************************************************
    ** Get agreementId.
    *******************************************************************************/
   public Integer getAgreementId()
   {
      return (agreementId);
   }

   /*******************************************************************************
    ** Set agreementId.
    *******************************************************************************/
   public void setAgreementId(Integer value)
   {
      agreementId = value;
   }

   /*******************************************************************************
    ** Configure agreementId.
    *******************************************************************************/
   public EdiOutboundMessage withAgreementId(Integer value)
   {
      agreementId = value;
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
   public void setPartnerId(Integer value)
   {
      partnerId = value;
   }

   /*******************************************************************************
    ** Configure partnerId.
    *******************************************************************************/
   public EdiOutboundMessage withPartnerId(Integer value)
   {
      partnerId = value;
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
   public void setTransportId(Integer value)
   {
      transportId = value;
   }

   /*******************************************************************************
    ** Configure transportId.
    *******************************************************************************/
   public EdiOutboundMessage withTransportId(Integer value)
   {
      transportId = value;
      return (this);
   }

   /*******************************************************************************
    ** Get outboundRequestId.
    *******************************************************************************/
   public Integer getOutboundRequestId()
   {
      return (outboundRequestId);
   }

   /*******************************************************************************
    ** Set outboundRequestId.
    *******************************************************************************/
   public void setOutboundRequestId(Integer value)
   {
      outboundRequestId = value;
   }

   /*******************************************************************************
    ** Configure outboundRequestId.
    *******************************************************************************/
   public EdiOutboundMessage withOutboundRequestId(Integer value)
   {
      outboundRequestId = value;
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
   public void setPayloadRef(String value)
   {
      payloadRef = value;
   }

   /*******************************************************************************
    ** Configure payloadRef.
    *******************************************************************************/
   public EdiOutboundMessage withPayloadRef(String value)
   {
      payloadRef = value;
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
   public void setPayloadSha256(String value)
   {
      payloadSha256 = value;
   }

   /*******************************************************************************
    ** Configure payloadSha256.
    *******************************************************************************/
   public EdiOutboundMessage withPayloadSha256(String value)
   {
      payloadSha256 = value;
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
   public void setSizeBytes(Long value)
   {
      sizeBytes = value;
   }

   /*******************************************************************************
    ** Configure sizeBytes.
    *******************************************************************************/
   public EdiOutboundMessage withSizeBytes(Long value)
   {
      sizeBytes = value;
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
   public void setStatus(String value)
   {
      status = value;
   }

   /*******************************************************************************
    ** Configure status.
    *******************************************************************************/
   public EdiOutboundMessage withStatus(String value)
   {
      status = value;
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
   public void setAttempts(Integer value)
   {
      attempts = value;
   }

   /*******************************************************************************
    ** Configure attempts.
    *******************************************************************************/
   public EdiOutboundMessage withAttempts(Integer value)
   {
      attempts = value;
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
   public void setNextAttemptAt(Instant value)
   {
      nextAttemptAt = value;
   }

   /*******************************************************************************
    ** Configure nextAttemptAt.
    *******************************************************************************/
   public EdiOutboundMessage withNextAttemptAt(Instant value)
   {
      nextAttemptAt = value;
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
   public void setClaimedAt(Instant value)
   {
      claimedAt = value;
   }

   /*******************************************************************************
    ** Configure claimedAt.
    *******************************************************************************/
   public EdiOutboundMessage withClaimedAt(Instant value)
   {
      claimedAt = value;
      return (this);
   }

   /*******************************************************************************
    ** Get sentAt.
    *******************************************************************************/
   public Instant getSentAt()
   {
      return (sentAt);
   }

   /*******************************************************************************
    ** Set sentAt.
    *******************************************************************************/
   public void setSentAt(Instant value)
   {
      sentAt = value;
   }

   /*******************************************************************************
    ** Configure sentAt.
    *******************************************************************************/
   public EdiOutboundMessage withSentAt(Instant value)
   {
      sentAt = value;
      return (this);
   }

   /*******************************************************************************
    ** Get deliveredAt.
    *******************************************************************************/
   public Instant getDeliveredAt()
   {
      return (deliveredAt);
   }

   /*******************************************************************************
    ** Set deliveredAt.
    *******************************************************************************/
   public void setDeliveredAt(Instant value)
   {
      deliveredAt = value;
   }

   /*******************************************************************************
    ** Configure deliveredAt.
    *******************************************************************************/
   public EdiOutboundMessage withDeliveredAt(Instant value)
   {
      deliveredAt = value;
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
   public void setLastError(String value)
   {
      lastError = value;
   }

   /*******************************************************************************
    ** Configure lastError.
    *******************************************************************************/
   public EdiOutboundMessage withLastError(String value)
   {
      lastError = value;
      return (this);
   }

   /*******************************************************************************
    ** Get ackForGroupId.
    *******************************************************************************/
   public Integer getAckForGroupId()
   {
      return (ackForGroupId);
   }

   /*******************************************************************************
    ** Set ackForGroupId.
    *******************************************************************************/
   public void setAckForGroupId(Integer value)
   {
      ackForGroupId = value;
   }

   /*******************************************************************************
    ** Configure ackForGroupId.
    *******************************************************************************/
   public EdiOutboundMessage withAckForGroupId(Integer value)
   {
      ackForGroupId = value;
      return (this);
   }

   /*******************************************************************************
    ** Get ackForInterchangeId.
    *******************************************************************************/
   public Integer getAckForInterchangeId()
   {
      return (ackForInterchangeId);
   }

   /*******************************************************************************
    ** Set ackForInterchangeId.
    *******************************************************************************/
   public void setAckForInterchangeId(Integer value)
   {
      ackForInterchangeId = value;
   }

   /*******************************************************************************
    ** Configure ackForInterchangeId.
    *******************************************************************************/
   public EdiOutboundMessage withAckForInterchangeId(Integer value)
   {
      ackForInterchangeId = value;
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
   public void setCreateDate(Instant value)
   {
      createDate = value;
   }

   /*******************************************************************************
    ** Configure createDate.
    *******************************************************************************/
   public EdiOutboundMessage withCreateDate(Instant value)
   {
      createDate = value;
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
   public void setModifyDate(Instant value)
   {
      modifyDate = value;
   }

   /*******************************************************************************
    ** Configure modifyDate.
    *******************************************************************************/
   public EdiOutboundMessage withModifyDate(Instant value)
   {
      modifyDate = value;
      return (this);
   }

   /*******************************************************************************
    ** Get interchangeId.
    *******************************************************************************/
   public Integer getInterchangeId()
   {
      return (interchangeId);
   }

   /*******************************************************************************
    ** Set interchangeId.
    *******************************************************************************/
   public void setInterchangeId(Integer value)
   {
      interchangeId = value;
   }

   /*******************************************************************************
    ** Configure interchangeId.
    *******************************************************************************/
   public EdiOutboundMessage withInterchangeId(Integer value)
   {
      interchangeId = value;
      return (this);
   }

   /*******************************************************************************
    ** Get transportMessageId.
    *******************************************************************************/
   public String getTransportMessageId()
   {
      return (transportMessageId);
   }

   /*******************************************************************************
    ** Set transportMessageId.
    *******************************************************************************/
   public void setTransportMessageId(String value)
   {
      transportMessageId = value;
   }

   /*******************************************************************************
    ** Configure transportMessageId.
    *******************************************************************************/
   public EdiOutboundMessage withTransportMessageId(String value)
   {
      transportMessageId = value;
      return (this);
   }
}
