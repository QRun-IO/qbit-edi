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
 ** Persistent edi_agreement_document record.
 *******************************************************************************/
public class EdiAgreementDocument extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_agreement_document";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "agreement_id", possibleValueSourceName = "edi_agreement")
   private Integer agreementId;

   @QField(backendName = "direction", maxLength = 255, possibleValueSourceName = "edi_direction")
   private String direction;

   @QField(backendName = "transaction_set", maxLength = 255)
   private String transactionSet;

   @QField(backendName = "functional_id_code", maxLength = 255)
   private String functionalIdCode;

   @QField(backendName = "version", maxLength = 255)
   private String version;

   @QField(backendName = "guide_id", possibleValueSourceName = "edi_guide")
   private Integer guideId;

   @QField(backendName = "map_id", possibleValueSourceName = "edi_map")
   private Integer mapId;

   @QField(backendName = "ack_expectation", maxLength = 255, possibleValueSourceName = "edi_ack_expectation")
   private String ackExpectation;

   @QField(backendName = "ack_due_minutes", defaultValue = "1440")
   private Integer ackDueMinutes;

   @QField(backendName = "ack_level", maxLength = 255, possibleValueSourceName = "edi_ack_level")
   private String ackLevel;

   @QField(backendName = "snip_level")
   private Integer snipLevel;

   @QField(backendName = "contains_phi")
   private Boolean containsPhi;

   @QField(backendName = "target_table_name", maxLength = 255)
   private String targetTableName;

   @QField(backendName = "source_table_name", maxLength = 255)
   private String sourceTableName;

   @QField(backendName = "outbound_trigger", maxLength = 255, possibleValueSourceName = "edi_outbound_trigger")
   private String outboundTrigger;

   @QField(backendName = "trigger_filter", maxLength = 10000)
   private String triggerFilter;

   @QField(backendName = "batch_mode", maxLength = 255, possibleValueSourceName = "edi_batch_mode")
   private String batchMode;

   @QField(backendName = "batch_schedule", maxLength = 255)
   private String batchSchedule;

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
   public EdiAgreementDocument withId(Integer id)
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
   public EdiAgreementDocument withAgreementId(Integer agreementId)
   {
      this.agreementId = agreementId;
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
   public void setDirection(String direction)
   {
      this.direction = direction;
   }


   /*******************************************************************************
    ** Set direction fluently.
    *******************************************************************************/
   public EdiAgreementDocument withDirection(String direction)
   {
      this.direction = direction;
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
   public EdiAgreementDocument withTransactionSet(String transactionSet)
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
   public EdiAgreementDocument withFunctionalIdCode(String functionalIdCode)
   {
      this.functionalIdCode = functionalIdCode;
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
   public EdiAgreementDocument withVersion(String version)
   {
      this.version = version;
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
   public EdiAgreementDocument withGuideId(Integer guideId)
   {
      this.guideId = guideId;
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
   public void setMapId(Integer mapId)
   {
      this.mapId = mapId;
   }


   /*******************************************************************************
    ** Set mapId fluently.
    *******************************************************************************/
   public EdiAgreementDocument withMapId(Integer mapId)
   {
      this.mapId = mapId;
      return (this);
   }


   /*******************************************************************************
    ** Get ackExpectation.
    *******************************************************************************/
   public String getAckExpectation()
   {
      return (ackExpectation);
   }


   /*******************************************************************************
    ** Set ackExpectation.
    *******************************************************************************/
   public void setAckExpectation(String ackExpectation)
   {
      this.ackExpectation = ackExpectation;
   }


   /*******************************************************************************
    ** Set ackExpectation fluently.
    *******************************************************************************/
   public EdiAgreementDocument withAckExpectation(String ackExpectation)
   {
      this.ackExpectation = ackExpectation;
      return (this);
   }


   /*******************************************************************************
    ** Get ackDueMinutes.
    *******************************************************************************/
   public Integer getAckDueMinutes()
   {
      return (ackDueMinutes);
   }


   /*******************************************************************************
    ** Set ackDueMinutes.
    *******************************************************************************/
   public void setAckDueMinutes(Integer ackDueMinutes)
   {
      this.ackDueMinutes = ackDueMinutes;
   }


   /*******************************************************************************
    ** Set ackDueMinutes fluently.
    *******************************************************************************/
   public EdiAgreementDocument withAckDueMinutes(Integer ackDueMinutes)
   {
      this.ackDueMinutes = ackDueMinutes;
      return (this);
   }


   /*******************************************************************************
    ** Get ackLevel.
    *******************************************************************************/
   public String getAckLevel()
   {
      return (ackLevel);
   }


   /*******************************************************************************
    ** Set ackLevel.
    *******************************************************************************/
   public void setAckLevel(String ackLevel)
   {
      this.ackLevel = ackLevel;
   }


   /*******************************************************************************
    ** Set ackLevel fluently.
    *******************************************************************************/
   public EdiAgreementDocument withAckLevel(String ackLevel)
   {
      this.ackLevel = ackLevel;
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
   public EdiAgreementDocument withSnipLevel(Integer snipLevel)
   {
      this.snipLevel = snipLevel;
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
   public EdiAgreementDocument withContainsPhi(Boolean containsPhi)
   {
      this.containsPhi = containsPhi;
      return (this);
   }


   /*******************************************************************************
    ** Get targetTableName.
    *******************************************************************************/
   public String getTargetTableName()
   {
      return (targetTableName);
   }


   /*******************************************************************************
    ** Set targetTableName.
    *******************************************************************************/
   public void setTargetTableName(String targetTableName)
   {
      this.targetTableName = targetTableName;
   }


   /*******************************************************************************
    ** Set targetTableName fluently.
    *******************************************************************************/
   public EdiAgreementDocument withTargetTableName(String targetTableName)
   {
      this.targetTableName = targetTableName;
      return (this);
   }


   /*******************************************************************************
    ** Get sourceTableName.
    *******************************************************************************/
   public String getSourceTableName()
   {
      return (sourceTableName);
   }


   /*******************************************************************************
    ** Set sourceTableName.
    *******************************************************************************/
   public void setSourceTableName(String sourceTableName)
   {
      this.sourceTableName = sourceTableName;
   }


   /*******************************************************************************
    ** Set sourceTableName fluently.
    *******************************************************************************/
   public EdiAgreementDocument withSourceTableName(String sourceTableName)
   {
      this.sourceTableName = sourceTableName;
      return (this);
   }


   /*******************************************************************************
    ** Get outboundTrigger.
    *******************************************************************************/
   public String getOutboundTrigger()
   {
      return (outboundTrigger);
   }


   /*******************************************************************************
    ** Set outboundTrigger.
    *******************************************************************************/
   public void setOutboundTrigger(String outboundTrigger)
   {
      this.outboundTrigger = outboundTrigger;
   }


   /*******************************************************************************
    ** Set outboundTrigger fluently.
    *******************************************************************************/
   public EdiAgreementDocument withOutboundTrigger(String outboundTrigger)
   {
      this.outboundTrigger = outboundTrigger;
      return (this);
   }


   /*******************************************************************************
    ** Get triggerFilter.
    *******************************************************************************/
   public String getTriggerFilter()
   {
      return (triggerFilter);
   }


   /*******************************************************************************
    ** Set triggerFilter.
    *******************************************************************************/
   public void setTriggerFilter(String triggerFilter)
   {
      this.triggerFilter = triggerFilter;
   }


   /*******************************************************************************
    ** Set triggerFilter fluently.
    *******************************************************************************/
   public EdiAgreementDocument withTriggerFilter(String triggerFilter)
   {
      this.triggerFilter = triggerFilter;
      return (this);
   }


   /*******************************************************************************
    ** Get batchMode.
    *******************************************************************************/
   public String getBatchMode()
   {
      return (batchMode);
   }


   /*******************************************************************************
    ** Set batchMode.
    *******************************************************************************/
   public void setBatchMode(String batchMode)
   {
      this.batchMode = batchMode;
   }


   /*******************************************************************************
    ** Set batchMode fluently.
    *******************************************************************************/
   public EdiAgreementDocument withBatchMode(String batchMode)
   {
      this.batchMode = batchMode;
      return (this);
   }


   /*******************************************************************************
    ** Get batchSchedule.
    *******************************************************************************/
   public String getBatchSchedule()
   {
      return (batchSchedule);
   }


   /*******************************************************************************
    ** Set batchSchedule.
    *******************************************************************************/
   public void setBatchSchedule(String batchSchedule)
   {
      this.batchSchedule = batchSchedule;
   }


   /*******************************************************************************
    ** Set batchSchedule fluently.
    *******************************************************************************/
   public EdiAgreementDocument withBatchSchedule(String batchSchedule)
   {
      this.batchSchedule = batchSchedule;
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
   public EdiAgreementDocument withIsActive(Boolean isActive)
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
   public EdiAgreementDocument withCreateDate(Instant createDate)
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
   public EdiAgreementDocument withModifyDate(Instant modifyDate)
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
   public EdiAgreementDocument withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }

}
