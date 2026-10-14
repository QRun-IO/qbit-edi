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
 ** Persistent edi_transaction_error record.
 *******************************************************************************/
public class EdiTransactionError extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_transaction_error";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "transaction_id", possibleValueSourceName = "edi_transaction")
   private Integer transactionId;

   @QField(backendName = "segment_position")
   private Integer segmentPosition;

   @QField(backendName = "segment_id", maxLength = 255)
   private String segmentId;

   @QField(backendName = "loop_id", maxLength = 255)
   private String loopId;

   @QField(backendName = "element_position")
   private Integer elementPosition;

   @QField(backendName = "component_position")
   private Integer componentPosition;

   @QField(backendName = "snip_level", defaultValue = "2")
   private Integer snipLevel;

   @QField(backendName = "ack_code", maxLength = 255)
   private String ackCode;

   @QField(backendName = "severity", maxLength = 255, possibleValueSourceName = "edi_severity")
   private String severity;

   @QField(backendName = "message", maxLength = 10000)
   private String message;

   @QField(backendName = "value_excerpt", maxLength = 255)
   private String valueExcerpt;

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
   public EdiTransactionError withId(Integer id)
   {
      this.id = id;
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
   public EdiTransactionError withTransactionId(Integer transactionId)
   {
      this.transactionId = transactionId;
      return (this);
   }


   /*******************************************************************************
    ** Get segmentPosition.
    *******************************************************************************/
   public Integer getSegmentPosition()
   {
      return (segmentPosition);
   }


   /*******************************************************************************
    ** Set segmentPosition.
    *******************************************************************************/
   public void setSegmentPosition(Integer segmentPosition)
   {
      this.segmentPosition = segmentPosition;
   }


   /*******************************************************************************
    ** Set segmentPosition fluently.
    *******************************************************************************/
   public EdiTransactionError withSegmentPosition(Integer segmentPosition)
   {
      this.segmentPosition = segmentPosition;
      return (this);
   }


   /*******************************************************************************
    ** Get segmentId.
    *******************************************************************************/
   public String getSegmentId()
   {
      return (segmentId);
   }


   /*******************************************************************************
    ** Set segmentId.
    *******************************************************************************/
   public void setSegmentId(String segmentId)
   {
      this.segmentId = segmentId;
   }


   /*******************************************************************************
    ** Set segmentId fluently.
    *******************************************************************************/
   public EdiTransactionError withSegmentId(String segmentId)
   {
      this.segmentId = segmentId;
      return (this);
   }


   /*******************************************************************************
    ** Get loopId.
    *******************************************************************************/
   public String getLoopId()
   {
      return (loopId);
   }


   /*******************************************************************************
    ** Set loopId.
    *******************************************************************************/
   public void setLoopId(String loopId)
   {
      this.loopId = loopId;
   }


   /*******************************************************************************
    ** Set loopId fluently.
    *******************************************************************************/
   public EdiTransactionError withLoopId(String loopId)
   {
      this.loopId = loopId;
      return (this);
   }


   /*******************************************************************************
    ** Get elementPosition.
    *******************************************************************************/
   public Integer getElementPosition()
   {
      return (elementPosition);
   }


   /*******************************************************************************
    ** Set elementPosition.
    *******************************************************************************/
   public void setElementPosition(Integer elementPosition)
   {
      this.elementPosition = elementPosition;
   }


   /*******************************************************************************
    ** Set elementPosition fluently.
    *******************************************************************************/
   public EdiTransactionError withElementPosition(Integer elementPosition)
   {
      this.elementPosition = elementPosition;
      return (this);
   }


   /*******************************************************************************
    ** Get componentPosition.
    *******************************************************************************/
   public Integer getComponentPosition()
   {
      return (componentPosition);
   }


   /*******************************************************************************
    ** Set componentPosition.
    *******************************************************************************/
   public void setComponentPosition(Integer componentPosition)
   {
      this.componentPosition = componentPosition;
   }


   /*******************************************************************************
    ** Set componentPosition fluently.
    *******************************************************************************/
   public EdiTransactionError withComponentPosition(Integer componentPosition)
   {
      this.componentPosition = componentPosition;
      return (this);
   }


   /*******************************************************************************
    ** Get snipLevel.
    *******************************************************************************/
   public Integer getSnipLevel()
   {
      return (snipLevel);
   }


   /*******************************************************************************
    ** Set snipLevel.
    *******************************************************************************/
   public void setSnipLevel(Integer snipLevel)
   {
      this.snipLevel = snipLevel;
   }


   /*******************************************************************************
    ** Set snipLevel fluently.
    *******************************************************************************/
   public EdiTransactionError withSnipLevel(Integer snipLevel)
   {
      this.snipLevel = snipLevel;
      return (this);
   }


   /*******************************************************************************
    ** Get ackCode.
    *******************************************************************************/
   public String getAckCode()
   {
      return (ackCode);
   }


   /*******************************************************************************
    ** Set ackCode.
    *******************************************************************************/
   public void setAckCode(String ackCode)
   {
      this.ackCode = ackCode;
   }


   /*******************************************************************************
    ** Set ackCode fluently.
    *******************************************************************************/
   public EdiTransactionError withAckCode(String ackCode)
   {
      this.ackCode = ackCode;
      return (this);
   }


   /*******************************************************************************
    ** Get severity.
    *******************************************************************************/
   public String getSeverity()
   {
      return (severity);
   }


   /*******************************************************************************
    ** Set severity.
    *******************************************************************************/
   public void setSeverity(String severity)
   {
      this.severity = severity;
   }


   /*******************************************************************************
    ** Set severity fluently.
    *******************************************************************************/
   public EdiTransactionError withSeverity(String severity)
   {
      this.severity = severity;
      return (this);
   }


   /*******************************************************************************
    ** Get message.
    *******************************************************************************/
   public String getMessage()
   {
      return (message);
   }


   /*******************************************************************************
    ** Set message.
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }


   /*******************************************************************************
    ** Set message fluently.
    *******************************************************************************/
   public EdiTransactionError withMessage(String message)
   {
      this.message = message;
      return (this);
   }


   /*******************************************************************************
    ** Get valueExcerpt.
    *******************************************************************************/
   public String getValueExcerpt()
   {
      return (valueExcerpt);
   }


   /*******************************************************************************
    ** Set valueExcerpt.
    *******************************************************************************/
   public void setValueExcerpt(String valueExcerpt)
   {
      this.valueExcerpt = valueExcerpt;
   }


   /*******************************************************************************
    ** Set valueExcerpt fluently.
    *******************************************************************************/
   public EdiTransactionError withValueExcerpt(String valueExcerpt)
   {
      this.valueExcerpt = valueExcerpt;
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
   public EdiTransactionError withCreateDate(Instant createDate)
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
   public EdiTransactionError withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
