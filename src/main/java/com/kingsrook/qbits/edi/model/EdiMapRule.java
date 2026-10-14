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
 ** Persistent edi_map_rule record.
 *******************************************************************************/
public class EdiMapRule extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_map_rule";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "map_id", possibleValueSourceName = "edi_map")
   private Integer mapId;

   @QField(backendName = "sequence")
   private Integer sequence;

   @QField(backendName = "edi_path", maxLength = 255)
   private String ediPath;

   @QField(backendName = "qqq_path", maxLength = 255)
   private String qqqPath;

   @QField(backendName = "transform", maxLength = 255)
   private String transform;

   @QField(backendName = "transform_args", maxLength = 10000)
   private String transformArgs;

   @QField(backendName = "required")
   private Boolean required;

   @QField(backendName = "condition", maxLength = 10000)
   private String condition;

   @QField(backendName = "default_value", maxLength = 255)
   private String defaultValue;

   @QField(backendName = "notes", maxLength = 10000)
   private String notes;

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
   public EdiMapRule withId(Integer value)
   {
      id = value;
      return (this);
   }

   /*******************************************************************************
    ** Get mapId.
    *******************************************************************************/
   public Integer getMapId()
   {
      return (mapId);
   }

   /*******************************************************************************
    ** Set mapId.
    *******************************************************************************/
   public void setMapId(Integer value)
   {
      mapId = value;
   }

   /*******************************************************************************
    ** Configure mapId.
    *******************************************************************************/
   public EdiMapRule withMapId(Integer value)
   {
      mapId = value;
      return (this);
   }

   /*******************************************************************************
    ** Get sequence.
    *******************************************************************************/
   public Integer getSequence()
   {
      return (sequence);
   }

   /*******************************************************************************
    ** Set sequence.
    *******************************************************************************/
   public void setSequence(Integer value)
   {
      sequence = value;
   }

   /*******************************************************************************
    ** Configure sequence.
    *******************************************************************************/
   public EdiMapRule withSequence(Integer value)
   {
      sequence = value;
      return (this);
   }

   /*******************************************************************************
    ** Get ediPath.
    *******************************************************************************/
   public String getEdiPath()
   {
      return (ediPath);
   }

   /*******************************************************************************
    ** Set ediPath.
    *******************************************************************************/
   public void setEdiPath(String value)
   {
      ediPath = value;
   }

   /*******************************************************************************
    ** Configure ediPath.
    *******************************************************************************/
   public EdiMapRule withEdiPath(String value)
   {
      ediPath = value;
      return (this);
   }

   /*******************************************************************************
    ** Get qqqPath.
    *******************************************************************************/
   public String getQqqPath()
   {
      return (qqqPath);
   }

   /*******************************************************************************
    ** Set qqqPath.
    *******************************************************************************/
   public void setQqqPath(String value)
   {
      qqqPath = value;
   }

   /*******************************************************************************
    ** Configure qqqPath.
    *******************************************************************************/
   public EdiMapRule withQqqPath(String value)
   {
      qqqPath = value;
      return (this);
   }

   /*******************************************************************************
    ** Get transform.
    *******************************************************************************/
   public String getTransform()
   {
      return (transform);
   }

   /*******************************************************************************
    ** Set transform.
    *******************************************************************************/
   public void setTransform(String value)
   {
      transform = value;
   }

   /*******************************************************************************
    ** Configure transform.
    *******************************************************************************/
   public EdiMapRule withTransform(String value)
   {
      transform = value;
      return (this);
   }

   /*******************************************************************************
    ** Get transformArgs.
    *******************************************************************************/
   public String getTransformArgs()
   {
      return (transformArgs);
   }

   /*******************************************************************************
    ** Set transformArgs.
    *******************************************************************************/
   public void setTransformArgs(String value)
   {
      transformArgs = value;
   }

   /*******************************************************************************
    ** Configure transformArgs.
    *******************************************************************************/
   public EdiMapRule withTransformArgs(String value)
   {
      transformArgs = value;
      return (this);
   }

   /*******************************************************************************
    ** Get required.
    *******************************************************************************/
   public Boolean getRequired()
   {
      return (required);
   }

   /*******************************************************************************
    ** Set required.
    *******************************************************************************/
   public void setRequired(Boolean value)
   {
      required = value;
   }

   /*******************************************************************************
    ** Configure required.
    *******************************************************************************/
   public EdiMapRule withRequired(Boolean value)
   {
      required = value;
      return (this);
   }

   /*******************************************************************************
    ** Get condition.
    *******************************************************************************/
   public String getCondition()
   {
      return (condition);
   }

   /*******************************************************************************
    ** Set condition.
    *******************************************************************************/
   public void setCondition(String value)
   {
      condition = value;
   }

   /*******************************************************************************
    ** Configure condition.
    *******************************************************************************/
   public EdiMapRule withCondition(String value)
   {
      condition = value;
      return (this);
   }

   /*******************************************************************************
    ** Get defaultValue.
    *******************************************************************************/
   public String getDefaultValue()
   {
      return (defaultValue);
   }

   /*******************************************************************************
    ** Set defaultValue.
    *******************************************************************************/
   public void setDefaultValue(String value)
   {
      defaultValue = value;
   }

   /*******************************************************************************
    ** Configure defaultValue.
    *******************************************************************************/
   public EdiMapRule withDefaultValue(String value)
   {
      defaultValue = value;
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
   public void setNotes(String value)
   {
      notes = value;
   }

   /*******************************************************************************
    ** Configure notes.
    *******************************************************************************/
   public EdiMapRule withNotes(String value)
   {
      notes = value;
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
   public EdiMapRule withCreateDate(Instant value)
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
   public EdiMapRule withModifyDate(Instant value)
   {
      modifyDate = value;
      return (this);
   }
}
