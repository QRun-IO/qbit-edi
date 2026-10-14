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
 ** Persistent edi_functional_group record.
 *******************************************************************************/
public class EdiFunctionalGroup extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_functional_group";

   @QField(backendName = "inbound_position")
   private Integer inboundPosition;

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "interchange_id", possibleValueSourceName = "edi_interchange")
   private Integer interchangeId;

   @QField(backendName = "sender_id", maxLength = 255)
   private String senderId;

   @QField(backendName = "functional_id_code", maxLength = 255)
   private String functionalIdCode;

   @QField(backendName = "control_number", maxLength = 255)
   private String controlNumber;

   @QField(backendName = "version", maxLength = 255)
   private String version;

   @QField(backendName = "transaction_count")
   private Integer transactionCount;

   @QField(backendName = "ack_status", maxLength = 255, possibleValueSourceName = "edi_ack_status")
   private String ackStatus;

   @QField(backendName = "ack_due_at")
   private Instant ackDueAt;

   @QField(backendName = "ack_received_at")
   private Instant ackReceivedAt;

   @QField(backendName = "ack_transaction_id", possibleValueSourceName = "edi_transaction")
   private Integer ackTransactionId;

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
   public EdiFunctionalGroup withId(Integer id)
   {
      this.id = id;
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
   public void setInterchangeId(Integer interchangeId)
   {
      this.interchangeId = interchangeId;
   }


   /*******************************************************************************
    ** Set interchangeId fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withInterchangeId(Integer interchangeId)
   {
      this.interchangeId = interchangeId;
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
   public EdiFunctionalGroup withSenderId(String senderId)
   {
      this.senderId = senderId;
      return (this);
   }


   /*******************************************************************************
    ** Get functionalIdCode.
    *******************************************************************************/
   public String getFunctionalIdCode()
   {
      return (functionalIdCode);
   }


   /*******************************************************************************
    ** Set functionalIdCode.
    *******************************************************************************/
   public void setFunctionalIdCode(String functionalIdCode)
   {
      this.functionalIdCode = functionalIdCode;
   }


   /*******************************************************************************
    ** Set functionalIdCode fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withFunctionalIdCode(String functionalIdCode)
   {
      this.functionalIdCode = functionalIdCode;
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
   public EdiFunctionalGroup withControlNumber(String controlNumber)
   {
      this.controlNumber = controlNumber;
      return (this);
   }


   /*******************************************************************************
    ** Get version.
    *******************************************************************************/
   public String getVersion()
   {
      return (version);
   }


   /*******************************************************************************
    ** Set version.
    *******************************************************************************/
   public void setVersion(String version)
   {
      this.version = version;
   }


   /*******************************************************************************
    ** Set version fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withVersion(String version)
   {
      this.version = version;
      return (this);
   }


   /*******************************************************************************
    ** Get transactionCount.
    *******************************************************************************/
   public Integer getTransactionCount()
   {
      return (transactionCount);
   }


   /*******************************************************************************
    ** Set transactionCount.
    *******************************************************************************/
   public void setTransactionCount(Integer transactionCount)
   {
      this.transactionCount = transactionCount;
   }


   /*******************************************************************************
    ** Set transactionCount fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withTransactionCount(Integer transactionCount)
   {
      this.transactionCount = transactionCount;
      return (this);
   }


   /*******************************************************************************
    ** Get ackStatus.
    *******************************************************************************/
   public String getAckStatus()
   {
      return (ackStatus);
   }


   /*******************************************************************************
    ** Set ackStatus.
    *******************************************************************************/
   public void setAckStatus(String ackStatus)
   {
      this.ackStatus = ackStatus;
   }


   /*******************************************************************************
    ** Set ackStatus fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withAckStatus(String ackStatus)
   {
      this.ackStatus = ackStatus;
      return (this);
   }


   /*******************************************************************************
    ** Get ackDueAt.
    *******************************************************************************/
   public Instant getAckDueAt()
   {
      return (ackDueAt);
   }


   /*******************************************************************************
    ** Set ackDueAt.
    *******************************************************************************/
   public void setAckDueAt(Instant ackDueAt)
   {
      this.ackDueAt = ackDueAt;
   }


   /*******************************************************************************
    ** Set ackDueAt fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withAckDueAt(Instant ackDueAt)
   {
      this.ackDueAt = ackDueAt;
      return (this);
   }


   /*******************************************************************************
    ** Get ackReceivedAt.
    *******************************************************************************/
   public Instant getAckReceivedAt()
   {
      return (ackReceivedAt);
   }


   /*******************************************************************************
    ** Set ackReceivedAt.
    *******************************************************************************/
   public void setAckReceivedAt(Instant ackReceivedAt)
   {
      this.ackReceivedAt = ackReceivedAt;
   }


   /*******************************************************************************
    ** Set ackReceivedAt fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withAckReceivedAt(Instant ackReceivedAt)
   {
      this.ackReceivedAt = ackReceivedAt;
      return (this);
   }


   /*******************************************************************************
    ** Get ackTransactionId.
    *******************************************************************************/
   public Integer getAckTransactionId()
   {
      return (ackTransactionId);
   }


   /*******************************************************************************
    ** Set ackTransactionId.
    *******************************************************************************/
   public void setAckTransactionId(Integer ackTransactionId)
   {
      this.ackTransactionId = ackTransactionId;
   }


   /*******************************************************************************
    ** Set ackTransactionId fluently.
    *******************************************************************************/
   public EdiFunctionalGroup withAckTransactionId(Integer ackTransactionId)
   {
      this.ackTransactionId = ackTransactionId;
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
   public EdiFunctionalGroup withCreateDate(Instant createDate)
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
   public EdiFunctionalGroup withModifyDate(Instant modifyDate)
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
   public EdiFunctionalGroup withInboundPosition(Integer inboundPosition)
   {
      this.inboundPosition = inboundPosition;
      return (this);
   }
}
