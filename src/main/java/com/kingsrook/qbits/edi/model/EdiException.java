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
 ** Persistent edi_exception record.
 *******************************************************************************/
public class EdiException extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_exception";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "type", maxLength = 255, possibleValueSourceName = "edi_exception_type")
   private String type;

   @QField(backendName = "status", maxLength = 255, possibleValueSourceName = "edi_exception_status")
   private String status;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "inbox_id", possibleValueSourceName = "edi_inbox")
   private Integer inboxId;

   @QField(backendName = "interchange_id", possibleValueSourceName = "edi_interchange")
   private Integer interchangeId;

   @QField(backendName = "transaction_id", possibleValueSourceName = "edi_transaction")
   private Integer transactionId;

   @QField(backendName = "outbound_message_id", possibleValueSourceName = "edi_outbound_message")
   private Integer outboundMessageId;

   @QField(backendName = "summary", maxLength = 255)
   private String summary;

   @QField(backendName = "detail", maxLength = 10000)
   private String detail;

   @QField(backendName = "assigned_to_user_id", maxLength = 255)
   private String assignedToUserId;

   @QField(backendName = "resolved_by", maxLength = 255)
   private String resolvedBy;

   @QField(backendName = "resolved_at")
   private Instant resolvedAt;

   @QField(backendName = "resolution", maxLength = 10000)
   private String resolution;

   @QField(backendName = "occurrences")
   private Integer occurrences;

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
   public EdiException withId(Integer id)
   {
      this.id = id;
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
   public EdiException withType(String type)
   {
      this.type = type;
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
   public EdiException withStatus(String status)
   {
      this.status = status;
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
   public EdiException withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
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
   public EdiException withInboxId(Integer inboxId)
   {
      this.inboxId = inboxId;
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
   public EdiException withInterchangeId(Integer interchangeId)
   {
      this.interchangeId = interchangeId;
      return (this);
   }


   /*******************************************************************************
    ** Get transactionId.
    *******************************************************************************/
   public Integer getTransactionId()
   {
      return (transactionId);
   }


   /*******************************************************************************
    ** Set transactionId.
    *******************************************************************************/
   public void setTransactionId(Integer transactionId)
   {
      this.transactionId = transactionId;
   }


   /*******************************************************************************
    ** Set transactionId fluently.
    *******************************************************************************/
   public EdiException withTransactionId(Integer transactionId)
   {
      this.transactionId = transactionId;
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
   public EdiException withOutboundMessageId(Integer outboundMessageId)
   {
      this.outboundMessageId = outboundMessageId;
      return (this);
   }


   /*******************************************************************************
    ** Get summary.
    *******************************************************************************/
   public String getSummary()
   {
      return (summary);
   }


   /*******************************************************************************
    ** Set summary.
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }


   /*******************************************************************************
    ** Set summary fluently.
    *******************************************************************************/
   public EdiException withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }


   /*******************************************************************************
    ** Get detail.
    *******************************************************************************/
   public String getDetail()
   {
      return (detail);
   }


   /*******************************************************************************
    ** Set detail.
    *******************************************************************************/
   public void setDetail(String detail)
   {
      this.detail = detail;
   }


   /*******************************************************************************
    ** Set detail fluently.
    *******************************************************************************/
   public EdiException withDetail(String detail)
   {
      this.detail = detail;
      return (this);
   }


   /*******************************************************************************
    ** Get assignedToUserId.
    *******************************************************************************/
   public String getAssignedToUserId()
   {
      return (assignedToUserId);
   }


   /*******************************************************************************
    ** Set assignedToUserId.
    *******************************************************************************/
   public void setAssignedToUserId(String assignedToUserId)
   {
      this.assignedToUserId = assignedToUserId;
   }


   /*******************************************************************************
    ** Set assignedToUserId fluently.
    *******************************************************************************/
   public EdiException withAssignedToUserId(String assignedToUserId)
   {
      this.assignedToUserId = assignedToUserId;
      return (this);
   }


   /*******************************************************************************
    ** Get resolvedBy.
    *******************************************************************************/
   public String getResolvedBy()
   {
      return (resolvedBy);
   }


   /*******************************************************************************
    ** Set resolvedBy.
    *******************************************************************************/
   public void setResolvedBy(String resolvedBy)
   {
      this.resolvedBy = resolvedBy;
   }


   /*******************************************************************************
    ** Set resolvedBy fluently.
    *******************************************************************************/
   public EdiException withResolvedBy(String resolvedBy)
   {
      this.resolvedBy = resolvedBy;
      return (this);
   }


   /*******************************************************************************
    ** Get resolvedAt.
    *******************************************************************************/
   public Instant getResolvedAt()
   {
      return (resolvedAt);
   }


   /*******************************************************************************
    ** Set resolvedAt.
    *******************************************************************************/
   public void setResolvedAt(Instant resolvedAt)
   {
      this.resolvedAt = resolvedAt;
   }


   /*******************************************************************************
    ** Set resolvedAt fluently.
    *******************************************************************************/
   public EdiException withResolvedAt(Instant resolvedAt)
   {
      this.resolvedAt = resolvedAt;
      return (this);
   }


   /*******************************************************************************
    ** Get resolution.
    *******************************************************************************/
   public String getResolution()
   {
      return (resolution);
   }


   /*******************************************************************************
    ** Set resolution.
    *******************************************************************************/
   public void setResolution(String resolution)
   {
      this.resolution = resolution;
   }


   /*******************************************************************************
    ** Set resolution fluently.
    *******************************************************************************/
   public EdiException withResolution(String resolution)
   {
      this.resolution = resolution;
      return (this);
   }


   /*******************************************************************************
    ** Get occurrences.
    *******************************************************************************/
   public Integer getOccurrences()
   {
      return (occurrences);
   }


   /*******************************************************************************
    ** Set occurrences.
    *******************************************************************************/
   public void setOccurrences(Integer occurrences)
   {
      this.occurrences = occurrences;
   }


   /*******************************************************************************
    ** Set occurrences fluently.
    *******************************************************************************/
   public EdiException withOccurrences(Integer occurrences)
   {
      this.occurrences = occurrences;
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
   public EdiException withCreateDate(Instant createDate)
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
   public EdiException withModifyDate(Instant modifyDate)
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
   public EdiException withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
