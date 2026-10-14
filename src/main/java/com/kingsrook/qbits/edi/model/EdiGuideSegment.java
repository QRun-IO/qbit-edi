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
 ** Persistent edi_guide_segment record.
 *******************************************************************************/
public class EdiGuideSegment extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_guide_segment";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "guide_id", possibleValueSourceName = "edi_guide")
   private Integer guideId;

   @QField(backendName = "sequence")
   private Integer sequence;

   @QField(backendName = "loop_id", maxLength = 255)
   private String loopId;

   @QField(backendName = "parent_loop_id", maxLength = 255)
   private String parentLoopId;

   @QField(backendName = "loop_max_repeat")
   private Integer loopMaxRepeat;

   @QField(backendName = "segment_id", maxLength = 255)
   private String segmentId;

   @QField(backendName = "position", maxLength = 255)
   private String position;

   @QField(backendName = "usage", maxLength = 255, possibleValueSourceName = "edi_usage")
   private String usage;

   @QField(backendName = "min_use")
   private Integer minUse;

   @QField(backendName = "max_use")
   private Integer maxUse;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "situational_rule_id", possibleValueSourceName = "edi_guide_rule")
   private Integer situationalRuleId;

   @QField(backendName = "notes", maxLength = 10000)
   private String notes;

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
   public EdiGuideSegment withId(Integer id)
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
   public EdiGuideSegment withGuideId(Integer guideId)
   {
      this.guideId = guideId;
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
   public void setSequence(Integer sequence)
   {
      this.sequence = sequence;
   }


   /*******************************************************************************
    ** Set sequence fluently.
    *******************************************************************************/
   public EdiGuideSegment withSequence(Integer sequence)
   {
      this.sequence = sequence;
      return (this);
   }


   /*******************************************************************************
    ** Get loopId.
    *******************************************************************************/
   public String getLoopId()
   {
      return (loopId);
   }


   /*******************************************************************************
    ** Set loopId.
    *******************************************************************************/
   public void setLoopId(String loopId)
   {
      this.loopId = loopId;
   }


   /*******************************************************************************
    ** Set loopId fluently.
    *******************************************************************************/
   public EdiGuideSegment withLoopId(String loopId)
   {
      this.loopId = loopId;
      return (this);
   }


   /*******************************************************************************
    ** Get parentLoopId.
    *******************************************************************************/
   public String getParentLoopId()
   {
      return (parentLoopId);
   }


   /*******************************************************************************
    ** Set parentLoopId.
    *******************************************************************************/
   public void setParentLoopId(String parentLoopId)
   {
      this.parentLoopId = parentLoopId;
   }


   /*******************************************************************************
    ** Set parentLoopId fluently.
    *******************************************************************************/
   public EdiGuideSegment withParentLoopId(String parentLoopId)
   {
      this.parentLoopId = parentLoopId;
      return (this);
   }


   /*******************************************************************************
    ** Get loopMaxRepeat.
    *******************************************************************************/
   public Integer getLoopMaxRepeat()
   {
      return (loopMaxRepeat);
   }


   /*******************************************************************************
    ** Set loopMaxRepeat.
    *******************************************************************************/
   public void setLoopMaxRepeat(Integer loopMaxRepeat)
   {
      this.loopMaxRepeat = loopMaxRepeat;
   }


   /*******************************************************************************
    ** Set loopMaxRepeat fluently.
    *******************************************************************************/
   public EdiGuideSegment withLoopMaxRepeat(Integer loopMaxRepeat)
   {
      this.loopMaxRepeat = loopMaxRepeat;
      return (this);
   }


   /*******************************************************************************
    ** Get segmentId.
    *******************************************************************************/
   public String getSegmentId()
   {
      return (segmentId);
   }


   /*******************************************************************************
    ** Set segmentId.
    *******************************************************************************/
   public void setSegmentId(String segmentId)
   {
      this.segmentId = segmentId;
   }


   /*******************************************************************************
    ** Set segmentId fluently.
    *******************************************************************************/
   public EdiGuideSegment withSegmentId(String segmentId)
   {
      this.segmentId = segmentId;
      return (this);
   }


   /*******************************************************************************
    ** Get position.
    *******************************************************************************/
   public String getPosition()
   {
      return (position);
   }


   /*******************************************************************************
    ** Set position.
    *******************************************************************************/
   public void setPosition(String position)
   {
      this.position = position;
   }


   /*******************************************************************************
    ** Set position fluently.
    *******************************************************************************/
   public EdiGuideSegment withPosition(String position)
   {
      this.position = position;
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
   public EdiGuideSegment withUsage(String usage)
   {
      this.usage = usage;
      return (this);
   }


   /*******************************************************************************
    ** Get minUse.
    *******************************************************************************/
   public Integer getMinUse()
   {
      return (minUse);
   }


   /*******************************************************************************
    ** Set minUse.
    *******************************************************************************/
   public void setMinUse(Integer minUse)
   {
      this.minUse = minUse;
   }


   /*******************************************************************************
    ** Set minUse fluently.
    *******************************************************************************/
   public EdiGuideSegment withMinUse(Integer minUse)
   {
      this.minUse = minUse;
      return (this);
   }


   /*******************************************************************************
    ** Get maxUse.
    *******************************************************************************/
   public Integer getMaxUse()
   {
      return (maxUse);
   }


   /*******************************************************************************
    ** Set maxUse.
    *******************************************************************************/
   public void setMaxUse(Integer maxUse)
   {
      this.maxUse = maxUse;
   }


   /*******************************************************************************
    ** Set maxUse fluently.
    *******************************************************************************/
   public EdiGuideSegment withMaxUse(Integer maxUse)
   {
      this.maxUse = maxUse;
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
   public EdiGuideSegment withName(String name)
   {
      this.name = name;
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
   public EdiGuideSegment withSituationalRuleId(Integer situationalRuleId)
   {
      this.situationalRuleId = situationalRuleId;
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
   public EdiGuideSegment withNotes(String notes)
   {
      this.notes = notes;
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
   public EdiGuideSegment withCreateDate(Instant createDate)
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
   public EdiGuideSegment withModifyDate(Instant modifyDate)
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
   public EdiGuideSegment withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
