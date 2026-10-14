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
 ** Persistent edi_map record.
 *******************************************************************************/
public class EdiMap extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_map";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "direction", maxLength = 255, possibleValueSourceName = "edi_direction")
   private String direction;

   @QField(backendName = "standard", maxLength = 255, possibleValueSourceName = "edi_standard")
   private String standard;

   @QField(backendName = "version", maxLength = 255)
   private String version;

   @QField(backendName = "transaction_set", maxLength = 255)
   private String transactionSet;

   @QField(backendName = "guide_id", possibleValueSourceName = "edi_guide")
   private Integer guideId;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "table_name", maxLength = 255)
   private String tableName;

   @QField(backendName = "match_key_fields", maxLength = 255)
   private String matchKeyFields;

   @QField(backendName = "on_match", maxLength = 255, possibleValueSourceName = "edi_on_match")
   private String onMatch;

   @QField(backendName = "business_key_path", maxLength = 255)
   private String businessKeyPath;

   @QField(backendName = "business_key_type", maxLength = 255)
   private String businessKeyType;

   @QField(backendName = "secondary_key_path", maxLength = 255)
   private String secondaryKeyPath;

   @QField(backendName = "revision")
   private Integer revision;

   @QField(backendName = "status", maxLength = 255, possibleValueSourceName = "edi_guide_status")
   private String status;

   @QField(backendName = "parent_map_id", possibleValueSourceName = "edi_map")
   private Integer parentMapId;

   @QField(backendName = "description", maxLength = 10000)
   private String description;

   @QField(backendName = "create_date")
   private Instant createDate;

   @QField(backendName = "modify_date")
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
   public void setId(Integer value)
   {
      id = value;
   }

   /*******************************************************************************
    ** Configure id.
    *******************************************************************************/
   public EdiMap withId(Integer value)
   {
      id = value;
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
   public void setName(String value)
   {
      name = value;
   }

   /*******************************************************************************
    ** Configure name.
    *******************************************************************************/
   public EdiMap withName(String value)
   {
      name = value;
      return (this);
   }

   /*******************************************************************************
    ** Get direction.
    *******************************************************************************/
   public String getDirection()
   {
      return (direction);
   }

   /*******************************************************************************
    ** Set direction.
    *******************************************************************************/
   public void setDirection(String value)
   {
      direction = value;
   }

   /*******************************************************************************
    ** Configure direction.
    *******************************************************************************/
   public EdiMap withDirection(String value)
   {
      direction = value;
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
   public void setStandard(String value)
   {
      standard = value;
   }

   /*******************************************************************************
    ** Configure standard.
    *******************************************************************************/
   public EdiMap withStandard(String value)
   {
      standard = value;
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
   public void setVersion(String value)
   {
      version = value;
   }

   /*******************************************************************************
    ** Configure version.
    *******************************************************************************/
   public EdiMap withVersion(String value)
   {
      version = value;
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
   public void setTransactionSet(String value)
   {
      transactionSet = value;
   }

   /*******************************************************************************
    ** Configure transactionSet.
    *******************************************************************************/
   public EdiMap withTransactionSet(String value)
   {
      transactionSet = value;
      return (this);
   }

   /*******************************************************************************
    ** Get guideId.
    *******************************************************************************/
   public Integer getGuideId()
   {
      return (guideId);
   }

   /*******************************************************************************
    ** Set guideId.
    *******************************************************************************/
   public void setGuideId(Integer value)
   {
      guideId = value;
   }

   /*******************************************************************************
    ** Configure guideId.
    *******************************************************************************/
   public EdiMap withGuideId(Integer value)
   {
      guideId = value;
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
   public void setPartnerId(Integer value)
   {
      partnerId = value;
   }

   /*******************************************************************************
    ** Configure partnerId.
    *******************************************************************************/
   public EdiMap withPartnerId(Integer value)
   {
      partnerId = value;
      return (this);
   }

   /*******************************************************************************
    ** Get tableName.
    *******************************************************************************/
   public String getTableName()
   {
      return (tableName);
   }

   /*******************************************************************************
    ** Set tableName.
    *******************************************************************************/
   public void setTableName(String value)
   {
      tableName = value;
   }

   /*******************************************************************************
    ** Configure tableName.
    *******************************************************************************/
   public EdiMap withTableName(String value)
   {
      tableName = value;
      return (this);
   }

   /*******************************************************************************
    ** Get matchKeyFields.
    *******************************************************************************/
   public String getMatchKeyFields()
   {
      return (matchKeyFields);
   }

   /*******************************************************************************
    ** Set matchKeyFields.
    *******************************************************************************/
   public void setMatchKeyFields(String value)
   {
      matchKeyFields = value;
   }

   /*******************************************************************************
    ** Configure matchKeyFields.
    *******************************************************************************/
   public EdiMap withMatchKeyFields(String value)
   {
      matchKeyFields = value;
      return (this);
   }

   /*******************************************************************************
    ** Get onMatch.
    *******************************************************************************/
   public String getOnMatch()
   {
      return (onMatch);
   }

   /*******************************************************************************
    ** Set onMatch.
    *******************************************************************************/
   public void setOnMatch(String value)
   {
      onMatch = value;
   }

   /*******************************************************************************
    ** Configure onMatch.
    *******************************************************************************/
   public EdiMap withOnMatch(String value)
   {
      onMatch = value;
      return (this);
   }

   /*******************************************************************************
    ** Get businessKeyPath.
    *******************************************************************************/
   public String getBusinessKeyPath()
   {
      return (businessKeyPath);
   }

   /*******************************************************************************
    ** Set businessKeyPath.
    *******************************************************************************/
   public void setBusinessKeyPath(String value)
   {
      businessKeyPath = value;
   }

   /*******************************************************************************
    ** Configure businessKeyPath.
    *******************************************************************************/
   public EdiMap withBusinessKeyPath(String value)
   {
      businessKeyPath = value;
      return (this);
   }

   /*******************************************************************************
    ** Get businessKeyType.
    *******************************************************************************/
   public String getBusinessKeyType()
   {
      return (businessKeyType);
   }

   /*******************************************************************************
    ** Set businessKeyType.
    *******************************************************************************/
   public void setBusinessKeyType(String value)
   {
      businessKeyType = value;
   }

   /*******************************************************************************
    ** Configure businessKeyType.
    *******************************************************************************/
   public EdiMap withBusinessKeyType(String value)
   {
      businessKeyType = value;
      return (this);
   }

   /*******************************************************************************
    ** Get secondaryKeyPath.
    *******************************************************************************/
   public String getSecondaryKeyPath()
   {
      return (secondaryKeyPath);
   }

   /*******************************************************************************
    ** Set secondaryKeyPath.
    *******************************************************************************/
   public void setSecondaryKeyPath(String value)
   {
      secondaryKeyPath = value;
   }

   /*******************************************************************************
    ** Configure secondaryKeyPath.
    *******************************************************************************/
   public EdiMap withSecondaryKeyPath(String value)
   {
      secondaryKeyPath = value;
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
   public void setRevision(Integer value)
   {
      revision = value;
   }

   /*******************************************************************************
    ** Configure revision.
    *******************************************************************************/
   public EdiMap withRevision(Integer value)
   {
      revision = value;
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
   public void setStatus(String value)
   {
      status = value;
   }

   /*******************************************************************************
    ** Configure status.
    *******************************************************************************/
   public EdiMap withStatus(String value)
   {
      status = value;
      return (this);
   }

   /*******************************************************************************
    ** Get parentMapId.
    *******************************************************************************/
   public Integer getParentMapId()
   {
      return (parentMapId);
   }

   /*******************************************************************************
    ** Set parentMapId.
    *******************************************************************************/
   public void setParentMapId(Integer value)
   {
      parentMapId = value;
   }

   /*******************************************************************************
    ** Configure parentMapId.
    *******************************************************************************/
   public EdiMap withParentMapId(Integer value)
   {
      parentMapId = value;
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
   public void setDescription(String value)
   {
      description = value;
   }

   /*******************************************************************************
    ** Configure description.
    *******************************************************************************/
   public EdiMap withDescription(String value)
   {
      description = value;
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
   public void setCreateDate(Instant value)
   {
      createDate = value;
   }

   /*******************************************************************************
    ** Configure createDate.
    *******************************************************************************/
   public EdiMap withCreateDate(Instant value)
   {
      createDate = value;
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
   public void setModifyDate(Instant value)
   {
      modifyDate = value;
   }

   /*******************************************************************************
    ** Configure modifyDate.
    *******************************************************************************/
   public EdiMap withModifyDate(Instant value)
   {
      modifyDate = value;
      return (this);
   }
}
