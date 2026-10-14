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
 ** Persistent edi_event record.
 *******************************************************************************/
public class EdiEvent extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_event";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "interchange_id", possibleValueSourceName = "edi_interchange")
   private Integer interchangeId;

   @QField(backendName = "transaction_id", possibleValueSourceName = "edi_transaction")
   private Integer transactionId;

   @QField(backendName = "at")
   private Instant at;

   @QField(backendName = "type", maxLength = 255, possibleValueSourceName = "edi_event_type")
   private String type;

   @QField(backendName = "detail", maxLength = 10000)
   private String detail;

   @QField(backendName = "user_name", maxLength = 255)
   private String userName;

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
   public EdiEvent withId(Integer id)
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
   public EdiEvent withInterchangeId(Integer interchangeId)
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
   public EdiEvent withTransactionId(Integer transactionId)
   {
      this.transactionId = transactionId;
      return (this);
   }


   /*******************************************************************************
    ** Get at.
    *******************************************************************************/
   public Instant getAt()
   {
      return (at);
   }


   /*******************************************************************************
    ** Set at.
    *******************************************************************************/
   public void setAt(Instant at)
   {
      this.at = at;
   }


   /*******************************************************************************
    ** Set at fluently.
    *******************************************************************************/
   public EdiEvent withAt(Instant at)
   {
      this.at = at;
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
   public EdiEvent withType(String type)
   {
      this.type = type;
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
   public EdiEvent withDetail(String detail)
   {
      this.detail = detail;
      return (this);
   }


   /*******************************************************************************
    ** Get userName.
    *******************************************************************************/
   public String getUserName()
   {
      return (userName);
   }


   /*******************************************************************************
    ** Set userName.
    *******************************************************************************/
   public void setUserName(String userName)
   {
      this.userName = userName;
   }


   /*******************************************************************************
    ** Set userName fluently.
    *******************************************************************************/
   public EdiEvent withUserName(String userName)
   {
      this.userName = userName;
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
   public EdiEvent withCreateDate(Instant createDate)
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
   public EdiEvent withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
