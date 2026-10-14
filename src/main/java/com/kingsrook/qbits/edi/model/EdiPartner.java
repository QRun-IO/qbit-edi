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
 ** Persistent edi_partner record.
 *******************************************************************************/
public class EdiPartner extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_partner";

   @QField(backendName = "gateway_username", maxLength = 255)
   private String gatewayUsername;

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "code", maxLength = 255)
   private String code;

   @QField(backendName = "status", maxLength = 255, possibleValueSourceName = "edi_partner_status")
   private String status;

   @QField(backendName = "contact_name", maxLength = 255)
   private String contactName;

   @QField(backendName = "contact_email", maxLength = 255)
   private String contactEmail;

   @QField(backendName = "contact_phone", maxLength = 255)
   private String contactPhone;

   @QField(backendName = "notes", maxLength = 10000)
   private String notes;

   @QField(backendName = "default_timezone", maxLength = 255)
   private String defaultTimezone;

   @QField(backendName = "contains_phi", defaultValue = "false")
   private Boolean containsPhi;

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
   public EdiPartner withId(Integer id)
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
   public EdiPartner withName(String name)
   {
      this.name = name;
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
   public EdiPartner withCode(String code)
   {
      this.code = code;
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
   public EdiPartner withStatus(String status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Get contactName.
    *******************************************************************************/
   public String getContactName()
   {
      return (contactName);
   }


   /*******************************************************************************
    ** Set contactName.
    *******************************************************************************/
   public void setContactName(String contactName)
   {
      this.contactName = contactName;
   }


   /*******************************************************************************
    ** Set contactName fluently.
    *******************************************************************************/
   public EdiPartner withContactName(String contactName)
   {
      this.contactName = contactName;
      return (this);
   }


   /*******************************************************************************
    ** Get contactEmail.
    *******************************************************************************/
   public String getContactEmail()
   {
      return (contactEmail);
   }


   /*******************************************************************************
    ** Set contactEmail.
    *******************************************************************************/
   public void setContactEmail(String contactEmail)
   {
      this.contactEmail = contactEmail;
   }


   /*******************************************************************************
    ** Set contactEmail fluently.
    *******************************************************************************/
   public EdiPartner withContactEmail(String contactEmail)
   {
      this.contactEmail = contactEmail;
      return (this);
   }


   /*******************************************************************************
    ** Get contactPhone.
    *******************************************************************************/
   public String getContactPhone()
   {
      return (contactPhone);
   }


   /*******************************************************************************
    ** Set contactPhone.
    *******************************************************************************/
   public void setContactPhone(String contactPhone)
   {
      this.contactPhone = contactPhone;
   }


   /*******************************************************************************
    ** Set contactPhone fluently.
    *******************************************************************************/
   public EdiPartner withContactPhone(String contactPhone)
   {
      this.contactPhone = contactPhone;
      return (this);
   }


   /*******************************************************************************
    ** Get notes.
    *******************************************************************************/
   public String getNotes()
   {
      return (notes);
   }


   /*******************************************************************************
    ** Set notes.
    *******************************************************************************/
   public void setNotes(String notes)
   {
      this.notes = notes;
   }


   /*******************************************************************************
    ** Set notes fluently.
    *******************************************************************************/
   public EdiPartner withNotes(String notes)
   {
      this.notes = notes;
      return (this);
   }


   /*******************************************************************************
    ** Get defaultTimezone.
    *******************************************************************************/
   public String getDefaultTimezone()
   {
      return (defaultTimezone);
   }


   /*******************************************************************************
    ** Set defaultTimezone.
    *******************************************************************************/
   public void setDefaultTimezone(String defaultTimezone)
   {
      this.defaultTimezone = defaultTimezone;
   }


   /*******************************************************************************
    ** Set defaultTimezone fluently.
    *******************************************************************************/
   public EdiPartner withDefaultTimezone(String defaultTimezone)
   {
      this.defaultTimezone = defaultTimezone;
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
   public EdiPartner withContainsPhi(Boolean containsPhi)
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
   public EdiPartner withCreateDate(Instant createDate)
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
   public EdiPartner withModifyDate(Instant modifyDate)
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
   public EdiPartner withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }


   /*******************************************************************************
    ** Get gatewayUsername.
    *******************************************************************************/
   public String getGatewayUsername()
   {
      return (gatewayUsername);
   }

   /*******************************************************************************
    ** Set gatewayUsername.
    *******************************************************************************/
   public void setGatewayUsername(String value)
   {
      gatewayUsername = value;
   }

   /*******************************************************************************
    ** Configure gatewayUsername.
    *******************************************************************************/
   public EdiPartner withGatewayUsername(String value)
   {
      gatewayUsername = value;
      return (this);
   }
}
