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
 ** Persistent edi_control_number record.
 *******************************************************************************/
public class EdiControlNumber extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_control_number";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "agreement_id", possibleValueSourceName = "edi_agreement")
   private Integer agreementId;

   @QField(backendName = "local_identity_id", possibleValueSourceName = "edi_identity")
   private Integer localIdentityId;

   @QField(backendName = "scope_key", maxLength = 255)
   private String scopeKey;

   @QField(backendName = "level", maxLength = 255, possibleValueSourceName = "edi_level")
   private String level;

   @QField(backendName = "last_value", defaultValue = "0")
   private Long lastValue;

   @QField(backendName = "min_value", defaultValue = "1")
   private Long minValue;

   @QField(backendName = "max_value", defaultValue = "999999999")
   private Long maxValue;

   @QField(backendName = "wrap_allowed", defaultValue = "true")
   private Boolean wrapAllowed;

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
   public EdiControlNumber withId(Integer id)
   {
      this.id = id;
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
   public EdiControlNumber withAgreementId(Integer agreementId)
   {
      this.agreementId = agreementId;
      return (this);
   }


   /*******************************************************************************
    ** Get localIdentityId.
    *******************************************************************************/
   public Integer getLocalIdentityId()
   {
      return (localIdentityId);
   }


   /*******************************************************************************
    ** Set localIdentityId.
    *******************************************************************************/
   public void setLocalIdentityId(Integer localIdentityId)
   {
      this.localIdentityId = localIdentityId;
   }


   /*******************************************************************************
    ** Set localIdentityId fluently.
    *******************************************************************************/
   public EdiControlNumber withLocalIdentityId(Integer localIdentityId)
   {
      this.localIdentityId = localIdentityId;
      return (this);
   }


   /*******************************************************************************
    ** Get scopeKey.
    *******************************************************************************/
   public String getScopeKey()
   {
      return (scopeKey);
   }


   /*******************************************************************************
    ** Set scopeKey.
    *******************************************************************************/
   public void setScopeKey(String scopeKey)
   {
      this.scopeKey = scopeKey;
   }


   /*******************************************************************************
    ** Set scopeKey fluently.
    *******************************************************************************/
   public EdiControlNumber withScopeKey(String scopeKey)
   {
      this.scopeKey = scopeKey;
      return (this);
   }


   /*******************************************************************************
    ** Get level.
    *******************************************************************************/
   public String getLevel()
   {
      return (level);
   }


   /*******************************************************************************
    ** Set level.
    *******************************************************************************/
   public void setLevel(String level)
   {
      this.level = level;
   }


   /*******************************************************************************
    ** Set level fluently.
    *******************************************************************************/
   public EdiControlNumber withLevel(String level)
   {
      this.level = level;
      return (this);
   }


   /*******************************************************************************
    ** Get lastValue.
    *******************************************************************************/
   public Long getLastValue()
   {
      return (lastValue);
   }


   /*******************************************************************************
    ** Set lastValue.
    *******************************************************************************/
   public void setLastValue(Long lastValue)
   {
      this.lastValue = lastValue;
   }


   /*******************************************************************************
    ** Set lastValue fluently.
    *******************************************************************************/
   public EdiControlNumber withLastValue(Long lastValue)
   {
      this.lastValue = lastValue;
      return (this);
   }


   /*******************************************************************************
    ** Get minValue.
    *******************************************************************************/
   public Long getMinValue()
   {
      return (minValue);
   }


   /*******************************************************************************
    ** Set minValue.
    *******************************************************************************/
   public void setMinValue(Long minValue)
   {
      this.minValue = minValue;
   }


   /*******************************************************************************
    ** Set minValue fluently.
    *******************************************************************************/
   public EdiControlNumber withMinValue(Long minValue)
   {
      this.minValue = minValue;
      return (this);
   }


   /*******************************************************************************
    ** Get maxValue.
    *******************************************************************************/
   public Long getMaxValue()
   {
      return (maxValue);
   }


   /*******************************************************************************
    ** Set maxValue.
    *******************************************************************************/
   public void setMaxValue(Long maxValue)
   {
      this.maxValue = maxValue;
   }


   /*******************************************************************************
    ** Set maxValue fluently.
    *******************************************************************************/
   public EdiControlNumber withMaxValue(Long maxValue)
   {
      this.maxValue = maxValue;
      return (this);
   }


   /*******************************************************************************
    ** Get wrapAllowed.
    *******************************************************************************/
   public Boolean getWrapAllowed()
   {
      return (wrapAllowed);
   }


   /*******************************************************************************
    ** Set wrapAllowed.
    *******************************************************************************/
   public void setWrapAllowed(Boolean wrapAllowed)
   {
      this.wrapAllowed = wrapAllowed;
   }


   /*******************************************************************************
    ** Set wrapAllowed fluently.
    *******************************************************************************/
   public EdiControlNumber withWrapAllowed(Boolean wrapAllowed)
   {
      this.wrapAllowed = wrapAllowed;
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
   public EdiControlNumber withCreateDate(Instant createDate)
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
   public EdiControlNumber withModifyDate(Instant modifyDate)
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
   public EdiControlNumber withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
