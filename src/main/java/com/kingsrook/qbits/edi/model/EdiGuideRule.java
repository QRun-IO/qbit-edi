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
 ** Persistent edi_guide_rule record.
 *******************************************************************************/
public class EdiGuideRule extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_guide_rule";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "guide_id", possibleValueSourceName = "edi_guide")
   private Integer guideId;

   @QField(backendName = "guide_segment_id", possibleValueSourceName = "edi_guide_segment")
   private Integer guideSegmentId;

   @QField(backendName = "type", maxLength = 255, possibleValueSourceName = "edi_rule_type")
   private String type;

   @QField(backendName = "positions", maxLength = 255)
   private String positions;

   @QField(backendName = "expression", maxLength = 10000)
   private String expression;

   @QField(backendName = "snip_level", defaultValue = "2")
   private Integer snipLevel;

   @QField(backendName = "severity", maxLength = 255, possibleValueSourceName = "edi_severity")
   private String severity;

   @QField(backendName = "message", maxLength = 10000)
   private String message;

   @QField(backendName = "ak3_code", maxLength = 255)
   private String ak3Code;

   @QField(backendName = "ak4_code", maxLength = 255)
   private String ak4Code;

   @QField(backendName = "ik3_code", maxLength = 255)
   private String ik3Code;

   @QField(backendName = "ik4_code", maxLength = 255)
   private String ik4Code;

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
   public EdiGuideRule withId(Integer id)
   {
      this.id = id;
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
   public void setGuideId(Integer guideId)
   {
      this.guideId = guideId;
   }


   /*******************************************************************************
    ** Set guideId fluently.
    *******************************************************************************/
   public EdiGuideRule withGuideId(Integer guideId)
   {
      this.guideId = guideId;
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
   public EdiGuideRule withGuideSegmentId(Integer guideSegmentId)
   {
      this.guideSegmentId = guideSegmentId;
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
   public EdiGuideRule withType(String type)
   {
      this.type = type;
      return (this);
   }


   /*******************************************************************************
    ** Get positions.
    *******************************************************************************/
   public String getPositions()
   {
      return (positions);
   }


   /*******************************************************************************
    ** Set positions.
    *******************************************************************************/
   public void setPositions(String positions)
   {
      this.positions = positions;
   }


   /*******************************************************************************
    ** Set positions fluently.
    *******************************************************************************/
   public EdiGuideRule withPositions(String positions)
   {
      this.positions = positions;
      return (this);
   }


   /*******************************************************************************
    ** Get expression.
    *******************************************************************************/
   public String getExpression()
   {
      return (expression);
   }


   /*******************************************************************************
    ** Set expression.
    *******************************************************************************/
   public void setExpression(String expression)
   {
      this.expression = expression;
   }


   /*******************************************************************************
    ** Set expression fluently.
    *******************************************************************************/
   public EdiGuideRule withExpression(String expression)
   {
      this.expression = expression;
      return (this);
   }


   /*******************************************************************************
    ** Get snipLevel.
    *******************************************************************************/
   public Integer getSnipLevel()
   {
      return (snipLevel);
   }


   /*******************************************************************************
    ** Set snipLevel.
    *******************************************************************************/
   public void setSnipLevel(Integer snipLevel)
   {
      this.snipLevel = snipLevel;
   }


   /*******************************************************************************
    ** Set snipLevel fluently.
    *******************************************************************************/
   public EdiGuideRule withSnipLevel(Integer snipLevel)
   {
      this.snipLevel = snipLevel;
      return (this);
   }


   /*******************************************************************************
    ** Get severity.
    *******************************************************************************/
   public String getSeverity()
   {
      return (severity);
   }


   /*******************************************************************************
    ** Set severity.
    *******************************************************************************/
   public void setSeverity(String severity)
   {
      this.severity = severity;
   }


   /*******************************************************************************
    ** Set severity fluently.
    *******************************************************************************/
   public EdiGuideRule withSeverity(String severity)
   {
      this.severity = severity;
      return (this);
   }


   /*******************************************************************************
    ** Get message.
    *******************************************************************************/
   public String getMessage()
   {
      return (message);
   }


   /*******************************************************************************
    ** Set message.
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }


   /*******************************************************************************
    ** Set message fluently.
    *******************************************************************************/
   public EdiGuideRule withMessage(String message)
   {
      this.message = message;
      return (this);
   }


   /*******************************************************************************
    ** Get ak3Code.
    *******************************************************************************/
   public String getAk3Code()
   {
      return (ak3Code);
   }


   /*******************************************************************************
    ** Set ak3Code.
    *******************************************************************************/
   public void setAk3Code(String ak3Code)
   {
      this.ak3Code = ak3Code;
   }


   /*******************************************************************************
    ** Set ak3Code fluently.
    *******************************************************************************/
   public EdiGuideRule withAk3Code(String ak3Code)
   {
      this.ak3Code = ak3Code;
      return (this);
   }


   /*******************************************************************************
    ** Get ak4Code.
    *******************************************************************************/
   public String getAk4Code()
   {
      return (ak4Code);
   }


   /*******************************************************************************
    ** Set ak4Code.
    *******************************************************************************/
   public void setAk4Code(String ak4Code)
   {
      this.ak4Code = ak4Code;
   }


   /*******************************************************************************
    ** Set ak4Code fluently.
    *******************************************************************************/
   public EdiGuideRule withAk4Code(String ak4Code)
   {
      this.ak4Code = ak4Code;
      return (this);
   }


   /*******************************************************************************
    ** Get ik3Code.
    *******************************************************************************/
   public String getIk3Code()
   {
      return (ik3Code);
   }


   /*******************************************************************************
    ** Set ik3Code.
    *******************************************************************************/
   public void setIk3Code(String ik3Code)
   {
      this.ik3Code = ik3Code;
   }


   /*******************************************************************************
    ** Set ik3Code fluently.
    *******************************************************************************/
   public EdiGuideRule withIk3Code(String ik3Code)
   {
      this.ik3Code = ik3Code;
      return (this);
   }


   /*******************************************************************************
    ** Get ik4Code.
    *******************************************************************************/
   public String getIk4Code()
   {
      return (ik4Code);
   }


   /*******************************************************************************
    ** Set ik4Code.
    *******************************************************************************/
   public void setIk4Code(String ik4Code)
   {
      this.ik4Code = ik4Code;
   }


   /*******************************************************************************
    ** Set ik4Code fluently.
    *******************************************************************************/
   public EdiGuideRule withIk4Code(String ik4Code)
   {
      this.ik4Code = ik4Code;
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
   public EdiGuideRule withCreateDate(Instant createDate)
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
   public EdiGuideRule withModifyDate(Instant modifyDate)
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
   public EdiGuideRule withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
