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
 ** Persistent edi_code_list record.
 *******************************************************************************/
public class EdiCodeList extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_code_list";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "element_id", maxLength = 255)
   private String elementId;

   @QField(backendName = "source", maxLength = 255, possibleValueSourceName = "edi_code_list_source")
   private String source;

   @QField(backendName = "external_code_set", maxLength = 255)
   private String externalCodeSet;

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
   public EdiCodeList withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }


   /*******************************************************************************
    ** Set name.
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }


   /*******************************************************************************
    ** Set name fluently.
    *******************************************************************************/
   public EdiCodeList withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Get elementId.
    *******************************************************************************/
   public String getElementId()
   {
      return (elementId);
   }


   /*******************************************************************************
    ** Set elementId.
    *******************************************************************************/
   public void setElementId(String elementId)
   {
      this.elementId = elementId;
   }


   /*******************************************************************************
    ** Set elementId fluently.
    *******************************************************************************/
   public EdiCodeList withElementId(String elementId)
   {
      this.elementId = elementId;
      return (this);
   }


   /*******************************************************************************
    ** Get source.
    *******************************************************************************/
   public String getSource()
   {
      return (source);
   }


   /*******************************************************************************
    ** Set source.
    *******************************************************************************/
   public void setSource(String source)
   {
      this.source = source;
   }


   /*******************************************************************************
    ** Set source fluently.
    *******************************************************************************/
   public EdiCodeList withSource(String source)
   {
      this.source = source;
      return (this);
   }


   /*******************************************************************************
    ** Get externalCodeSet.
    *******************************************************************************/
   public String getExternalCodeSet()
   {
      return (externalCodeSet);
   }


   /*******************************************************************************
    ** Set externalCodeSet.
    *******************************************************************************/
   public void setExternalCodeSet(String externalCodeSet)
   {
      this.externalCodeSet = externalCodeSet;
   }


   /*******************************************************************************
    ** Set externalCodeSet fluently.
    *******************************************************************************/
   public EdiCodeList withExternalCodeSet(String externalCodeSet)
   {
      this.externalCodeSet = externalCodeSet;
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
   public EdiCodeList withCreateDate(Instant createDate)
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
   public EdiCodeList withModifyDate(Instant modifyDate)
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
   public EdiCodeList withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
