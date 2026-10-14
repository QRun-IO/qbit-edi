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
 ** Persistent edi_interchange record.
 *******************************************************************************/
public class EdiInterchange extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_interchange";

   @QField(backendName = "inbound_position")
   private Integer inboundPosition;

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "direction", maxLength = 255, possibleValueSourceName = "edi_direction")
   private String direction;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "agreement_id", possibleValueSourceName = "edi_agreement")
   private Integer agreementId;

   @QField(backendName = "transport_id", possibleValueSourceName = "edi_transport")
   private Integer transportId;

   @QField(backendName = "inbox_id", possibleValueSourceName = "edi_inbox")
   private Integer inboxId;

   @QField(backendName = "outbound_message_id")
   private Integer outboundMessageId;

   @QField(backendName = "standard", maxLength = 255, possibleValueSourceName = "edi_standard")
   private String standard;

   @QField(backendName = "sender_qualifier", maxLength = 255)
   private String senderQualifier;

   @QField(backendName = "sender_id", maxLength = 255)
   private String senderId;

   @QField(backendName = "receiver_qualifier", maxLength = 255)
   private String receiverQualifier;

   @QField(backendName = "receiver_id", maxLength = 255)
   private String receiverId;

   @QField(backendName = "control_number", maxLength = 255)
   private String controlNumber;

   @QField(backendName = "interchange_version", maxLength = 255)
   private String interchangeVersion;

   @QField(backendName = "usage_indicator", maxLength = 255, possibleValueSourceName = "edi_usage_indicator")
   private String usageIndicator;

   @QField(backendName = "interchange_date")
   private Instant interchangeDate;

   @QField(backendName = "group_count")
   private Integer groupCount;

   @QField(backendName = "payload_ref", maxLength = 255)
   private String payloadRef;

   @QField(backendName = "payload_sha256", maxLength = 255)
   private String payloadSha256;

   @QField(backendName = "size_bytes")
   private Long sizeBytes;

   @QField(backendName = "status", maxLength = 255, possibleValueSourceName = "edi_interchange_status")
   private String status;

   @QField(backendName = "ta1_status", maxLength = 255)
   private String ta1Status;

   @QField(backendName = "contains_phi", defaultValue = "false")
   private Boolean containsPhi;

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
   public EdiInterchange withId(Integer id)
   {
      this.id = id;
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
   public EdiInterchange withDirection(String direction)
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
   public EdiInterchange withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
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
   public void setAgreementId(Integer agreementId)
   {
      this.agreementId = agreementId;
   }


   /*******************************************************************************
    ** Set agreementId fluently.
    *******************************************************************************/
   public EdiInterchange withAgreementId(Integer agreementId)
   {
      this.agreementId = agreementId;
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
   public EdiInterchange withTransportId(Integer transportId)
   {
      this.transportId = transportId;
      return (this);
   }


   /*******************************************************************************
    ** Get inboxId.
    *******************************************************************************/
   public Integer getInboxId()
   {
      return (inboxId);
   }


   /*******************************************************************************
    ** Set inboxId.
    *******************************************************************************/
   public void setInboxId(Integer inboxId)
   {
      this.inboxId = inboxId;
   }


   /*******************************************************************************
    ** Set inboxId fluently.
    *******************************************************************************/
   public EdiInterchange withInboxId(Integer inboxId)
   {
      this.inboxId = inboxId;
      return (this);
   }


   /*******************************************************************************
    ** Get outboundMessageId.
    *******************************************************************************/
   public Integer getOutboundMessageId()
   {
      return (outboundMessageId);
   }


   /*******************************************************************************
    ** Set outboundMessageId.
    *******************************************************************************/
   public void setOutboundMessageId(Integer outboundMessageId)
   {
      this.outboundMessageId = outboundMessageId;
   }


   /*******************************************************************************
    ** Set outboundMessageId fluently.
    *******************************************************************************/
   public EdiInterchange withOutboundMessageId(Integer outboundMessageId)
   {
      this.outboundMessageId = outboundMessageId;
      return (this);
   }


   /*******************************************************************************
    ** Get standard.
    *******************************************************************************/
   public String getStandard()
   {
      return (standard);
   }


   /*******************************************************************************
    ** Set standard.
    *******************************************************************************/
   public void setStandard(String standard)
   {
      this.standard = standard;
   }


   /*******************************************************************************
    ** Set standard fluently.
    *******************************************************************************/
   public EdiInterchange withStandard(String standard)
   {
      this.standard = standard;
      return (this);
   }


   /*******************************************************************************
    ** Get senderQualifier.
    *******************************************************************************/
   public String getSenderQualifier()
   {
      return (senderQualifier);
   }


   /*******************************************************************************
    ** Set senderQualifier.
    *******************************************************************************/
   public void setSenderQualifier(String senderQualifier)
   {
      this.senderQualifier = senderQualifier;
   }


   /*******************************************************************************
    ** Set senderQualifier fluently.
    *******************************************************************************/
   public EdiInterchange withSenderQualifier(String senderQualifier)
   {
      this.senderQualifier = senderQualifier;
      return (this);
   }


   /*******************************************************************************
    ** Get senderId.
    *******************************************************************************/
   public String getSenderId()
   {
      return (senderId);
   }


   /*******************************************************************************
    ** Set senderId.
    *******************************************************************************/
   public void setSenderId(String senderId)
   {
      this.senderId = senderId;
   }


   /*******************************************************************************
    ** Set senderId fluently.
    *******************************************************************************/
   public EdiInterchange withSenderId(String senderId)
   {
      this.senderId = senderId;
      return (this);
   }


   /*******************************************************************************
    ** Get receiverQualifier.
    *******************************************************************************/
   public String getReceiverQualifier()
   {
      return (receiverQualifier);
   }


   /*******************************************************************************
    ** Set receiverQualifier.
    *******************************************************************************/
   public void setReceiverQualifier(String receiverQualifier)
   {
      this.receiverQualifier = receiverQualifier;
   }


   /*******************************************************************************
    ** Set receiverQualifier fluently.
    *******************************************************************************/
   public EdiInterchange withReceiverQualifier(String receiverQualifier)
   {
      this.receiverQualifier = receiverQualifier;
      return (this);
   }


   /*******************************************************************************
    ** Get receiverId.
    *******************************************************************************/
   public String getReceiverId()
   {
      return (receiverId);
   }


   /*******************************************************************************
    ** Set receiverId.
    *******************************************************************************/
   public void setReceiverId(String receiverId)
   {
      this.receiverId = receiverId;
   }


   /*******************************************************************************
    ** Set receiverId fluently.
    *******************************************************************************/
   public EdiInterchange withReceiverId(String receiverId)
   {
      this.receiverId = receiverId;
      return (this);
   }


   /*******************************************************************************
    ** Get controlNumber.
    *******************************************************************************/
   public String getControlNumber()
   {
      return (controlNumber);
   }


   /*******************************************************************************
    ** Set controlNumber.
    *******************************************************************************/
   public void setControlNumber(String controlNumber)
   {
      this.controlNumber = controlNumber;
   }


   /*******************************************************************************
    ** Set controlNumber fluently.
    *******************************************************************************/
   public EdiInterchange withControlNumber(String controlNumber)
   {
      this.controlNumber = controlNumber;
      return (this);
   }


   /*******************************************************************************
    ** Get interchangeVersion.
    *******************************************************************************/
   public String getInterchangeVersion()
   {
      return (interchangeVersion);
   }


   /*******************************************************************************
    ** Set interchangeVersion.
    *******************************************************************************/
   public void setInterchangeVersion(String interchangeVersion)
   {
      this.interchangeVersion = interchangeVersion;
   }


   /*******************************************************************************
    ** Set interchangeVersion fluently.
    *******************************************************************************/
   public EdiInterchange withInterchangeVersion(String interchangeVersion)
   {
      this.interchangeVersion = interchangeVersion;
      return (this);
   }


   /*******************************************************************************
    ** Get usageIndicator.
    *******************************************************************************/
   public String getUsageIndicator()
   {
      return (usageIndicator);
   }


   /*******************************************************************************
    ** Set usageIndicator.
    *******************************************************************************/
   public void setUsageIndicator(String usageIndicator)
   {
      this.usageIndicator = usageIndicator;
   }


   /*******************************************************************************
    ** Set usageIndicator fluently.
    *******************************************************************************/
   public EdiInterchange withUsageIndicator(String usageIndicator)
   {
      this.usageIndicator = usageIndicator;
      return (this);
   }


   /*******************************************************************************
    ** Get interchangeDate.
    *******************************************************************************/
   public Instant getInterchangeDate()
   {
      return (interchangeDate);
   }


   /*******************************************************************************
    ** Set interchangeDate.
    *******************************************************************************/
   public void setInterchangeDate(Instant interchangeDate)
   {
      this.interchangeDate = interchangeDate;
   }


   /*******************************************************************************
    ** Set interchangeDate fluently.
    *******************************************************************************/
   public EdiInterchange withInterchangeDate(Instant interchangeDate)
   {
      this.interchangeDate = interchangeDate;
      return (this);
   }


   /*******************************************************************************
    ** Get groupCount.
    *******************************************************************************/
   public Integer getGroupCount()
   {
      return (groupCount);
   }


   /*******************************************************************************
    ** Set groupCount.
    *******************************************************************************/
   public void setGroupCount(Integer groupCount)
   {
      this.groupCount = groupCount;
   }


   /*******************************************************************************
    ** Set groupCount fluently.
    *******************************************************************************/
   public EdiInterchange withGroupCount(Integer groupCount)
   {
      this.groupCount = groupCount;
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
   public EdiInterchange withPayloadRef(String payloadRef)
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
   public EdiInterchange withPayloadSha256(String payloadSha256)
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
   public EdiInterchange withSizeBytes(Long sizeBytes)
   {
      this.sizeBytes = sizeBytes;
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
   public EdiInterchange withStatus(String status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Get ta1Status.
    *******************************************************************************/
   public String getTa1Status()
   {
      return (ta1Status);
   }


   /*******************************************************************************
    ** Set ta1Status.
    *******************************************************************************/
   public void setTa1Status(String ta1Status)
   {
      this.ta1Status = ta1Status;
   }


   /*******************************************************************************
    ** Set ta1Status fluently.
    *******************************************************************************/
   public EdiInterchange withTa1Status(String ta1Status)
   {
      this.ta1Status = ta1Status;
      return (this);
   }


   /*******************************************************************************
    ** Get containsPhi.
    *******************************************************************************/
   public Boolean getContainsPhi()
   {
      return (containsPhi);
   }


   /*******************************************************************************
    ** Set containsPhi.
    *******************************************************************************/
   public void setContainsPhi(Boolean containsPhi)
   {
      this.containsPhi = containsPhi;
   }


   /*******************************************************************************
    ** Set containsPhi fluently.
    *******************************************************************************/
   public EdiInterchange withContainsPhi(Boolean containsPhi)
   {
      this.containsPhi = containsPhi;
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
   public EdiInterchange withCreateDate(Instant createDate)
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
   public EdiInterchange withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

   /*******************************************************************************
    ** One-based occurrence within the parent payload, interchange or group.
    *******************************************************************************/
   public Integer getInboundPosition()
   {
      return (inboundPosition);
   }

   /*******************************************************************************
    ** Set the stable source position used for retry checkpoints.
    *******************************************************************************/
   public void setInboundPosition(Integer inboundPosition)
   {
      this.inboundPosition = inboundPosition;
   }

   /*******************************************************************************
    ** Fluently set the source position.
    *******************************************************************************/
   public EdiInterchange withInboundPosition(Integer inboundPosition)
   {
      this.inboundPosition = inboundPosition;
      return (this);
   }
}
