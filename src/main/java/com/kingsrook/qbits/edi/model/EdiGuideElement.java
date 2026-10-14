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
 ** Persistent edi_guide_element record.
 *******************************************************************************/
public class EdiGuideElement extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_guide_element";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "guide_segment_id", possibleValueSourceName = "edi_guide_segment")
   private Integer guideSegmentId;

   @QField(backendName = "position")
   private Integer position;

   @QField(backendName = "component_position")
   private Integer componentPosition;

   @QField(backendName = "element_id", maxLength = 255)
   private String elementId;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "data_type", maxLength = 255, possibleValueSourceName = "edi_data_type")
   private String dataType;

   @QField(backendName = "min_length")
   private Integer minLength;

   @QField(backendName = "max_length")
   private Integer maxLength;

   @QField(backendName = "usage", maxLength = 255, possibleValueSourceName = "edi_usage")
   private String usage;

   @QField(backendName = "code_list_id", possibleValueSourceName = "edi_code_list")
   private Integer codeListId;

   @QField(backendName = "fixed_value", maxLength = 255)
   private String fixedValue;

   @QField(backendName = "repeat_max")
   private Integer repeatMax;

   @QField(backendName = "situational_rule_id", possibleValueSourceName = "edi_guide_rule")
   private Integer situationalRuleId;

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
   public EdiGuideElement withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get guideSegmentId.
    *******************************************************************************/
   public Integer getGuideSegmentId()
   {
      return (guideSegmentId);
   }


   /*******************************************************************************
    ** Set guideSegmentId.
    *******************************************************************************/
   public void setGuideSegmentId(Integer guideSegmentId)
   {
      this.guideSegmentId = guideSegmentId;
   }


   /*******************************************************************************
    ** Set guideSegmentId fluently.
    *******************************************************************************/
   public EdiGuideElement withGuideSegmentId(Integer guideSegmentId)
   {
      this.guideSegmentId = guideSegmentId;
      return (this);
   }


   /*******************************************************************************
    ** Get position.
    *******************************************************************************/
   public Integer getPosition()
   {
      return (position);
   }


   /*******************************************************************************
    ** Set position.
    *******************************************************************************/
   public void setPosition(Integer position)
   {
      this.position = position;
   }


   /*******************************************************************************
    ** Set position fluently.
    *******************************************************************************/
   public EdiGuideElement withPosition(Integer position)
   {
      this.position = position;
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
   public EdiGuideElement withComponentPosition(Integer componentPosition)
   {
      this.componentPosition = componentPosition;
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
   public EdiGuideElement withElementId(String elementId)
   {
      this.elementId = elementId;
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
   public EdiGuideElement withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Get dataType.
    *******************************************************************************/
   public String getDataType()
   {
      return (dataType);
   }


   /*******************************************************************************
    ** Set dataType.
    *******************************************************************************/
   public void setDataType(String dataType)
   {
      this.dataType = dataType;
   }


   /*******************************************************************************
    ** Set dataType fluently.
    *******************************************************************************/
   public EdiGuideElement withDataType(String dataType)
   {
      this.dataType = dataType;
      return (this);
   }


   /*******************************************************************************
    ** Get minLength.
    *******************************************************************************/
   public Integer getMinLength()
   {
      return (minLength);
   }


   /*******************************************************************************
    ** Set minLength.
    *******************************************************************************/
   public void setMinLength(Integer minLength)
   {
      this.minLength = minLength;
   }


   /*******************************************************************************
    ** Set minLength fluently.
    *******************************************************************************/
   public EdiGuideElement withMinLength(Integer minLength)
   {
      this.minLength = minLength;
      return (this);
   }


   /*******************************************************************************
    ** Get maxLength.
    *******************************************************************************/
   public Integer getMaxLength()
   {
      return (maxLength);
   }


   /*******************************************************************************
    ** Set maxLength.
    *******************************************************************************/
   public void setMaxLength(Integer maxLength)
   {
      this.maxLength = maxLength;
   }


   /*******************************************************************************
    ** Set maxLength fluently.
    *******************************************************************************/
   public EdiGuideElement withMaxLength(Integer maxLength)
   {
      this.maxLength = maxLength;
      return (this);
   }


   /*******************************************************************************
    ** Get usage.
    *******************************************************************************/
   public String getUsage()
   {
      return (usage);
   }


   /*******************************************************************************
    ** Set usage.
    *******************************************************************************/
   public void setUsage(String usage)
   {
      this.usage = usage;
   }


   /*******************************************************************************
    ** Set usage fluently.
    *******************************************************************************/
   public EdiGuideElement withUsage(String usage)
   {
      this.usage = usage;
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
   public EdiGuideElement withCodeListId(Integer codeListId)
   {
      this.codeListId = codeListId;
      return (this);
   }


   /*******************************************************************************
    ** Get fixedValue.
    *******************************************************************************/
   public String getFixedValue()
   {
      return (fixedValue);
   }


   /*******************************************************************************
    ** Set fixedValue.
    *******************************************************************************/
   public void setFixedValue(String fixedValue)
   {
      this.fixedValue = fixedValue;
   }


   /*******************************************************************************
    ** Set fixedValue fluently.
    *******************************************************************************/
   public EdiGuideElement withFixedValue(String fixedValue)
   {
      this.fixedValue = fixedValue;
      return (this);
   }


   /*******************************************************************************
    ** Get repeatMax.
    *******************************************************************************/
   public Integer getRepeatMax()
   {
      return (repeatMax);
   }


   /*******************************************************************************
    ** Set repeatMax.
    *******************************************************************************/
   public void setRepeatMax(Integer repeatMax)
   {
      this.repeatMax = repeatMax;
   }


   /*******************************************************************************
    ** Set repeatMax fluently.
    *******************************************************************************/
   public EdiGuideElement withRepeatMax(Integer repeatMax)
   {
      this.repeatMax = repeatMax;
      return (this);
   }


   /*******************************************************************************
    ** Get situationalRuleId.
    *******************************************************************************/
   public Integer getSituationalRuleId()
   {
      return (situationalRuleId);
   }


   /*******************************************************************************
    ** Set situationalRuleId.
    *******************************************************************************/
   public void setSituationalRuleId(Integer situationalRuleId)
   {
      this.situationalRuleId = situationalRuleId;
   }


   /*******************************************************************************
    ** Set situationalRuleId fluently.
    *******************************************************************************/
   public EdiGuideElement withSituationalRuleId(Integer situationalRuleId)
   {
      this.situationalRuleId = situationalRuleId;
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
   public EdiGuideElement withCreateDate(Instant createDate)
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
   public EdiGuideElement withModifyDate(Instant modifyDate)
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
   public EdiGuideElement withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
