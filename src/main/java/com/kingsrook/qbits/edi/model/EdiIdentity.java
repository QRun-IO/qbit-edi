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
 ** Persistent edi_identity record.
 *******************************************************************************/
public class EdiIdentity extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_identity";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "standard", maxLength = 255, possibleValueSourceName = "edi_standard")
   private String standard;

   @QField(backendName = "interchange_qualifier", maxLength = 255)
   private String interchangeQualifier;

   @QField(backendName = "interchange_id", maxLength = 255)
   private String interchangeId;

   @QField(backendName = "application_code", maxLength = 255)
   private String applicationCode;

   @QField(backendName = "description", maxLength = 10000)
   private String description;

   @QField(backendName = "is_active", defaultValue = "true")
   private Boolean isActive;

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
   public EdiIdentity withId(Integer id)
   {
      this.id = id;
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
   public EdiIdentity withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
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
   public EdiIdentity withStandard(String standard)
   {
      this.standard = standard;
      return (this);
   }


   /*******************************************************************************
    ** Get interchangeQualifier.
    *******************************************************************************/
   public String getInterchangeQualifier()
   {
      return (interchangeQualifier);
   }


   /*******************************************************************************
    ** Set interchangeQualifier.
    *******************************************************************************/
   public void setInterchangeQualifier(String interchangeQualifier)
   {
      this.interchangeQualifier = interchangeQualifier;
   }


   /*******************************************************************************
    ** Set interchangeQualifier fluently.
    *******************************************************************************/
   public EdiIdentity withInterchangeQualifier(String interchangeQualifier)
   {
      this.interchangeQualifier = interchangeQualifier;
      return (this);
   }


   /*******************************************************************************
    ** Get interchangeId.
    *******************************************************************************/
   public String getInterchangeId()
   {
      return (interchangeId);
   }


   /*******************************************************************************
    ** Set interchangeId.
    *******************************************************************************/
   public void setInterchangeId(String interchangeId)
   {
      this.interchangeId = interchangeId;
   }


   /*******************************************************************************
    ** Set interchangeId fluently.
    *******************************************************************************/
   public EdiIdentity withInterchangeId(String interchangeId)
   {
      this.interchangeId = interchangeId;
      return (this);
   }


   /*******************************************************************************
    ** Get applicationCode.
    *******************************************************************************/
   public String getApplicationCode()
   {
      return (applicationCode);
   }


   /*******************************************************************************
    ** Set applicationCode.
    *******************************************************************************/
   public void setApplicationCode(String applicationCode)
   {
      this.applicationCode = applicationCode;
   }


   /*******************************************************************************
    ** Set applicationCode fluently.
    *******************************************************************************/
   public EdiIdentity withApplicationCode(String applicationCode)
   {
      this.applicationCode = applicationCode;
      return (this);
   }


   /*******************************************************************************
    ** Get description.
    *******************************************************************************/
   public String getDescription()
   {
      return (description);
   }


   /*******************************************************************************
    ** Set description.
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }


   /*******************************************************************************
    ** Set description fluently.
    *******************************************************************************/
   public EdiIdentity withDescription(String description)
   {
      this.description = description;
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
   public EdiIdentity withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
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
   public EdiIdentity withCreateDate(Instant createDate)
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
   public EdiIdentity withModifyDate(Instant modifyDate)
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
   public EdiIdentity withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
