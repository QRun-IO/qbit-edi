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
 ** Persistent edi_as2_mdn record: an AS2 receipt we sent or received. Rows are
 ** written by the qbit only; the delivery columns queue asynchronous receipts.
 *******************************************************************************/
public class EdiAs2Mdn extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_as2_mdn";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "direction", maxLength = 20, possibleValueSourceName = "edi_direction")
   private String direction;

   @QField(backendName = "message_id", maxLength = 255)
   private String messageId;

   @QField(backendName = "original_message_id", maxLength = 255)
   private String originalMessageId;

   @QField(backendName = "disposition", maxLength = 255)
   private String disposition;

   @QField(backendName = "mic_algorithm", maxLength = 50)
   private String micAlgorithm;

   @QField(backendName = "mic_value", maxLength = 255)
   private String micValue;

   @QField(backendName = "signed")
   private Boolean signed;

   @QField(backendName = "signature_valid")
   private Boolean signatureValid;

   @QField(backendName = "payload_ref", maxLength = 255)
   private String payloadRef;

   @QField(backendName = "received_at")
   private Instant receivedAt;

   @QField(backendName = "transport_id", possibleValueSourceName = "edi_transport")
   private Integer transportId;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "outbound_message_id", possibleValueSourceName = "edi_outbound_message")
   private Integer outboundMessageId;

   @QField(backendName = "inbox_id", possibleValueSourceName = "edi_inbox")
   private Integer inboxId;

   @QField(backendName = "delivery_url", maxLength = 1000)
   private String deliveryUrl;

   @QField(backendName = "delivery_status", maxLength = 20, possibleValueSourceName = "edi_as2_mdn_delivery_status")
   private String deliveryStatus;

   @QField(backendName = "delivery_attempts")
   private Integer deliveryAttempts;

   @QField(backendName = "next_attempt_at")
   private Instant nextAttemptAt;

   @QField(backendName = "delivered_at")
   private Instant deliveredAt;

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
   public EdiAs2Mdn withId(Integer id)
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
   public EdiAs2Mdn withDirection(String direction)
   {
      this.direction = direction;
      return (this);
   }


   /*******************************************************************************
    ** Get messageId.
    *******************************************************************************/
   public String getMessageId()
   {
      return (messageId);
   }


   /*******************************************************************************
    ** Set messageId.
    *******************************************************************************/
   public void setMessageId(String messageId)
   {
      this.messageId = messageId;
   }


   /*******************************************************************************
    ** Set messageId fluently.
    *******************************************************************************/
   public EdiAs2Mdn withMessageId(String messageId)
   {
      this.messageId = messageId;
      return (this);
   }


   /*******************************************************************************
    ** Get originalMessageId.
    *******************************************************************************/
   public String getOriginalMessageId()
   {
      return (originalMessageId);
   }


   /*******************************************************************************
    ** Set originalMessageId.
    *******************************************************************************/
   public void setOriginalMessageId(String originalMessageId)
   {
      this.originalMessageId = originalMessageId;
   }


   /*******************************************************************************
    ** Set originalMessageId fluently.
    *******************************************************************************/
   public EdiAs2Mdn withOriginalMessageId(String originalMessageId)
   {
      this.originalMessageId = originalMessageId;
      return (this);
   }


   /*******************************************************************************
    ** Get disposition.
    *******************************************************************************/
   public String getDisposition()
   {
      return (disposition);
   }


   /*******************************************************************************
    ** Set disposition.
    *******************************************************************************/
   public void setDisposition(String disposition)
   {
      this.disposition = disposition;
   }


   /*******************************************************************************
    ** Set disposition fluently.
    *******************************************************************************/
   public EdiAs2Mdn withDisposition(String disposition)
   {
      this.disposition = disposition;
      return (this);
   }


   /*******************************************************************************
    ** Get micAlgorithm.
    *******************************************************************************/
   public String getMicAlgorithm()
   {
      return (micAlgorithm);
   }


   /*******************************************************************************
    ** Set micAlgorithm.
    *******************************************************************************/
   public void setMicAlgorithm(String micAlgorithm)
   {
      this.micAlgorithm = micAlgorithm;
   }


   /*******************************************************************************
    ** Set micAlgorithm fluently.
    *******************************************************************************/
   public EdiAs2Mdn withMicAlgorithm(String micAlgorithm)
   {
      this.micAlgorithm = micAlgorithm;
      return (this);
   }


   /*******************************************************************************
    ** Get micValue.
    *******************************************************************************/
   public String getMicValue()
   {
      return (micValue);
   }


   /*******************************************************************************
    ** Set micValue.
    *******************************************************************************/
   public void setMicValue(String micValue)
   {
      this.micValue = micValue;
   }


   /*******************************************************************************
    ** Set micValue fluently.
    *******************************************************************************/
   public EdiAs2Mdn withMicValue(String micValue)
   {
      this.micValue = micValue;
      return (this);
   }


   /*******************************************************************************
    ** Get signed.
    *******************************************************************************/
   public Boolean getSigned()
   {
      return (signed);
   }


   /*******************************************************************************
    ** Set signed.
    *******************************************************************************/
   public void setSigned(Boolean signed)
   {
      this.signed = signed;
   }


   /*******************************************************************************
    ** Set signed fluently.
    *******************************************************************************/
   public EdiAs2Mdn withSigned(Boolean signed)
   {
      this.signed = signed;
      return (this);
   }


   /*******************************************************************************
    ** Get signatureValid.
    *******************************************************************************/
   public Boolean getSignatureValid()
   {
      return (signatureValid);
   }


   /*******************************************************************************
    ** Set signatureValid.
    *******************************************************************************/
   public void setSignatureValid(Boolean signatureValid)
   {
      this.signatureValid = signatureValid;
   }


   /*******************************************************************************
    ** Set signatureValid fluently.
    *******************************************************************************/
   public EdiAs2Mdn withSignatureValid(Boolean signatureValid)
   {
      this.signatureValid = signatureValid;
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
   public EdiAs2Mdn withPayloadRef(String payloadRef)
   {
      this.payloadRef = payloadRef;
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
   public EdiAs2Mdn withReceivedAt(Instant receivedAt)
   {
      this.receivedAt = receivedAt;
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
   public EdiAs2Mdn withTransportId(Integer transportId)
   {
      this.transportId = transportId;
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
   public EdiAs2Mdn withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
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
   public EdiAs2Mdn withOutboundMessageId(Integer outboundMessageId)
   {
      this.outboundMessageId = outboundMessageId;
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
   public EdiAs2Mdn withInboxId(Integer inboxId)
   {
      this.inboxId = inboxId;
      return (this);
   }


   /*******************************************************************************
    ** Get deliveryUrl.
    *******************************************************************************/
   public String getDeliveryUrl()
   {
      return (deliveryUrl);
   }


   /*******************************************************************************
    ** Set deliveryUrl.
    *******************************************************************************/
   public void setDeliveryUrl(String deliveryUrl)
   {
      this.deliveryUrl = deliveryUrl;
   }


   /*******************************************************************************
    ** Set deliveryUrl fluently.
    *******************************************************************************/
   public EdiAs2Mdn withDeliveryUrl(String deliveryUrl)
   {
      this.deliveryUrl = deliveryUrl;
      return (this);
   }


   /*******************************************************************************
    ** Get deliveryStatus.
    *******************************************************************************/
   public String getDeliveryStatus()
   {
      return (deliveryStatus);
   }


   /*******************************************************************************
    ** Set deliveryStatus.
    *******************************************************************************/
   public void setDeliveryStatus(String deliveryStatus)
   {
      this.deliveryStatus = deliveryStatus;
   }


   /*******************************************************************************
    ** Set deliveryStatus fluently.
    *******************************************************************************/
   public EdiAs2Mdn withDeliveryStatus(String deliveryStatus)
   {
      this.deliveryStatus = deliveryStatus;
      return (this);
   }


   /*******************************************************************************
    ** Get deliveryAttempts.
    *******************************************************************************/
   public Integer getDeliveryAttempts()
   {
      return (deliveryAttempts);
   }


   /*******************************************************************************
    ** Set deliveryAttempts.
    *******************************************************************************/
   public void setDeliveryAttempts(Integer deliveryAttempts)
   {
      this.deliveryAttempts = deliveryAttempts;
   }


   /*******************************************************************************
    ** Set deliveryAttempts fluently.
    *******************************************************************************/
   public EdiAs2Mdn withDeliveryAttempts(Integer deliveryAttempts)
   {
      this.deliveryAttempts = deliveryAttempts;
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
   public EdiAs2Mdn withNextAttemptAt(Instant nextAttemptAt)
   {
      this.nextAttemptAt = nextAttemptAt;
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
   public void setDeliveredAt(Instant deliveredAt)
   {
      this.deliveredAt = deliveredAt;
   }


   /*******************************************************************************
    ** Set deliveredAt fluently.
    *******************************************************************************/
   public EdiAs2Mdn withDeliveredAt(Instant deliveredAt)
   {
      this.deliveredAt = deliveredAt;
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
   public EdiAs2Mdn withCreateDate(Instant createDate)
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
   public EdiAs2Mdn withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }
}
