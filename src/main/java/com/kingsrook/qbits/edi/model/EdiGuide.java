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
 ** Persistent edi_guide record.
 *******************************************************************************/
public class EdiGuide extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_guide";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "standard", maxLength = 255, possibleValueSourceName = "edi_standard")
   private String standard;

   @QField(backendName = "version", maxLength = 255)
   private String version;

   @QField(backendName = "transaction_set", maxLength = 255)
   private String transactionSet;

   @QField(backendName = "functional_id_code", maxLength = 255)
   private String functionalIdCode;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "parent_guide_id", possibleValueSourceName = "edi_guide")
   private Integer parentGuideId;

   @QField(backendName = "revision")
   private Integer revision;

   @QField(backendName = "status", maxLength = 255, possibleValueSourceName = "edi_guide_status")
   private String status;

   @QField(backendName = "source", maxLength = 255, possibleValueSourceName = "edi_guide_source")
   private String source;

   @QField(backendName = "description", maxLength = 10000)
   private String description;

   @QField(backendName = "compiled_checksum", maxLength = 255)
   private String compiledChecksum;

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
   public EdiGuide withId(Integer id)
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
   public EdiGuide withName(String name)
   {
      this.name = name;
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
   public EdiGuide withStandard(String standard)
   {
      this.standard = standard;
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
   public EdiGuide withVersion(String version)
   {
      this.version = version;
      return (this);
   }


   /*******************************************************************************
    ** Get transactionSet.
    *******************************************************************************/
   public String getTransactionSet()
   {
      return (transactionSet);
   }


   /*******************************************************************************
    ** Set transactionSet.
    *******************************************************************************/
   public void setTransactionSet(String transactionSet)
   {
      this.transactionSet = transactionSet;
   }


   /*******************************************************************************
    ** Set transactionSet fluently.
    *******************************************************************************/
   public EdiGuide withTransactionSet(String transactionSet)
   {
      this.transactionSet = transactionSet;
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
   public EdiGuide withFunctionalIdCode(String functionalIdCode)
   {
      this.functionalIdCode = functionalIdCode;
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
   public EdiGuide withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
      return (this);
   }


   /*******************************************************************************
    ** Get parentGuideId.
    *******************************************************************************/
   public Integer getParentGuideId()
   {
      return (parentGuideId);
   }


   /*******************************************************************************
    ** Set parentGuideId.
    *******************************************************************************/
   public void setParentGuideId(Integer parentGuideId)
   {
      this.parentGuideId = parentGuideId;
   }


   /*******************************************************************************
    ** Set parentGuideId fluently.
    *******************************************************************************/
   public EdiGuide withParentGuideId(Integer parentGuideId)
   {
      this.parentGuideId = parentGuideId;
      return (this);
   }


   /*******************************************************************************
    ** Get revision.
    *******************************************************************************/
   public Integer getRevision()
   {
      return (revision);
   }


   /*******************************************************************************
    ** Set revision.
    *******************************************************************************/
   public void setRevision(Integer revision)
   {
      this.revision = revision;
   }


   /*******************************************************************************
    ** Set revision fluently.
    *******************************************************************************/
   public EdiGuide withRevision(Integer revision)
   {
      this.revision = revision;
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
   public EdiGuide withStatus(String status)
   {
      this.status = status;
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
   public EdiGuide withSource(String source)
   {
      this.source = source;
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
   public EdiGuide withDescription(String description)
   {
      this.description = description;
      return (this);
   }


   /*******************************************************************************
    ** Get compiledChecksum.
    *******************************************************************************/
   public String getCompiledChecksum()
   {
      return (compiledChecksum);
   }


   /*******************************************************************************
    ** Set compiledChecksum.
    *******************************************************************************/
   public void setCompiledChecksum(String compiledChecksum)
   {
      this.compiledChecksum = compiledChecksum;
   }


   /*******************************************************************************
    ** Set compiledChecksum fluently.
    *******************************************************************************/
   public EdiGuide withCompiledChecksum(String compiledChecksum)
   {
      this.compiledChecksum = compiledChecksum;
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
   public EdiGuide withCreateDate(Instant createDate)
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
   public EdiGuide withModifyDate(Instant modifyDate)
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
   public EdiGuide withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
