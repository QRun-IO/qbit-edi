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
 ** Persistent edi_code_list_value record.
 *******************************************************************************/
public class EdiCodeListValue extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_code_list_value";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "code_list_id", possibleValueSourceName = "edi_code_list")
   private Integer codeListId;

   @QField(backendName = "code", maxLength = 255)
   private String code;

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
   public EdiCodeListValue withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get codeListId.
    *******************************************************************************/
   public Integer getCodeListId()
   {
      return (codeListId);
   }


   /*******************************************************************************
    ** Set codeListId.
    *******************************************************************************/
   public void setCodeListId(Integer codeListId)
   {
      this.codeListId = codeListId;
   }


   /*******************************************************************************
    ** Set codeListId fluently.
    *******************************************************************************/
   public EdiCodeListValue withCodeListId(Integer codeListId)
   {
      this.codeListId = codeListId;
      return (this);
   }


   /*******************************************************************************
    ** Get code.
    *******************************************************************************/
   public String getCode()
   {
      return (code);
   }


   /*******************************************************************************
    ** Set code.
    *******************************************************************************/
   public void setCode(String code)
   {
      this.code = code;
   }


   /*******************************************************************************
    ** Set code fluently.
    *******************************************************************************/
   public EdiCodeListValue withCode(String code)
   {
      this.code = code;
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
   public EdiCodeListValue withDescription(String description)
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
   public EdiCodeListValue withIsActive(Boolean isActive)
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
   public EdiCodeListValue withCreateDate(Instant createDate)
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
   public EdiCodeListValue withModifyDate(Instant modifyDate)
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
   public EdiCodeListValue withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
