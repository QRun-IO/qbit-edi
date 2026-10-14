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
 ** Persistent edi_transaction record.
 *******************************************************************************/
public class EdiTransaction extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_transaction";

   @QField(backendName = "inbound_position")
   private Integer inboundPosition;

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "interchange_id", possibleValueSourceName = "edi_interchange")
   private Integer interchangeId;

   @QField(backendName = "functional_group_id", possibleValueSourceName = "edi_functional_group")
   private Integer functionalGroupId;

   @QField(backendName = "direction", maxLength = 255, possibleValueSourceName = "edi_direction")
   private String direction;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "agreement_document_id", possibleValueSourceName = "edi_agreement_document")
   private Integer agreementDocumentId;

   @QField(backendName = "transaction_set", maxLength = 255)
   private String transactionSet;

   @QField(backendName = "version", maxLength = 255)
   private String version;

   @QField(backendName = "control_number", maxLength = 255)
   private String controlNumber;

   @QField(backendName = "business_key_type", maxLength = 255)
   private String businessKeyType;

   @QField(backendName = "business_key", maxLength = 255)
   private String businessKey;

   @QField(backendName = "secondary_key", maxLength = 255)
   private String secondaryKey;

   @QField(backendName = "phi_business_key", maxLength = 255)
   private String phiBusinessKey;

   @QField(backendName = "status", maxLength = 255, possibleValueSourceName = "edi_transaction_status")
   private String status;

   @QField(backendName = "ack_status", maxLength = 255, possibleValueSourceName = "edi_ack_status")
   private String ackStatus;

   @QField(backendName = "error_count")
   private Integer errorCount;

   @QField(backendName = "warning_count")
   private Integer warningCount;

   @QField(backendName = "guide_id", possibleValueSourceName = "edi_guide")
   private Integer guideId;

   @QField(backendName = "guide_revision")
   private Integer guideRevision;

   @QField(backendName = "map_id")
   private Integer mapId;

   @QField(backendName = "map_revision")
   private Integer mapRevision;

   @QField(backendName = "target_table_name", maxLength = 255)
   private String targetTableName;

   @QField(backendName = "target_record_id", maxLength = 255)
   private String targetRecordId;

   @QField(backendName = "segment_count")
   private Integer segmentCount;

   @QField(backendName = "contains_phi", defaultValue = "false")
   private Boolean containsPhi;

   @QField(backendName = "create_date", isEditable = false)
   private Instant createDate;

   @QField(backendName = "modify_date", isEditable = false)
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
   public void setId(Integer id)
   {
      this.id = id;
   }


   /*******************************************************************************
    ** Set id fluently.
    *******************************************************************************/
   public EdiTransaction withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get interchangeId.
    *******************************************************************************/
   public Integer getInterchangeId()
   {
      return (interchangeId);
   }


   /*******************************************************************************
    ** Set interchangeId.
    *******************************************************************************/
   public void setInterchangeId(Integer interchangeId)
   {
      this.interchangeId = interchangeId;
   }


   /*******************************************************************************
    ** Set interchangeId fluently.
    *******************************************************************************/
   public EdiTransaction withInterchangeId(Integer interchangeId)
   {
      this.interchangeId = interchangeId;
      return (this);
   }


   /*******************************************************************************
    ** Get functionalGroupId.
    *******************************************************************************/
   public Integer getFunctionalGroupId()
   {
      return (functionalGroupId);
   }


   /*******************************************************************************
    ** Set functionalGroupId.
    *******************************************************************************/
   public void setFunctionalGroupId(Integer functionalGroupId)
   {
      this.functionalGroupId = functionalGroupId;
   }


   /*******************************************************************************
    ** Set functionalGroupId fluently.
    *******************************************************************************/
   public EdiTransaction withFunctionalGroupId(Integer functionalGroupId)
   {
      this.functionalGroupId = functionalGroupId;
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
   public EdiTransaction withDirection(String direction)
   {
      this.direction = direction;
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
   public EdiTransaction withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
      return (this);
   }


   /*******************************************************************************
    ** Get agreementDocumentId.
    *******************************************************************************/
   public Integer getAgreementDocumentId()
   {
      return (agreementDocumentId);
   }


   /*******************************************************************************
    ** Set agreementDocumentId.
    *******************************************************************************/
   public void setAgreementDocumentId(Integer agreementDocumentId)
   {
      this.agreementDocumentId = agreementDocumentId;
   }


   /*******************************************************************************
    ** Set agreementDocumentId fluently.
    *******************************************************************************/
   public EdiTransaction withAgreementDocumentId(Integer agreementDocumentId)
   {
      this.agreementDocumentId = agreementDocumentId;
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
   public EdiTransaction withTransactionSet(String transactionSet)
   {
      this.transactionSet = transactionSet;
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
   public EdiTransaction withVersion(String version)
   {
      this.version = version;
      return (this);
   }


   /*******************************************************************************
    ** Get controlNumber.
    *******************************************************************************/
   public String getControlNumber()
   {
      return (controlNumber);
   }


   /*******************************************************************************
    ** Set controlNumber.
    *******************************************************************************/
   public void setControlNumber(String controlNumber)
   {
      this.controlNumber = controlNumber;
   }


   /*******************************************************************************
    ** Set controlNumber fluently.
    *******************************************************************************/
   public EdiTransaction withControlNumber(String controlNumber)
   {
      this.controlNumber = controlNumber;
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
   public void setBusinessKeyType(String businessKeyType)
   {
      this.businessKeyType = businessKeyType;
   }


   /*******************************************************************************
    ** Set businessKeyType fluently.
    *******************************************************************************/
   public EdiTransaction withBusinessKeyType(String businessKeyType)
   {
      this.businessKeyType = businessKeyType;
      return (this);
   }


   /*******************************************************************************
    ** Get businessKey.
    *******************************************************************************/
   public String getBusinessKey()
   {
      return (businessKey);
   }


   /*******************************************************************************
    ** Set businessKey.
    *******************************************************************************/
   public void setBusinessKey(String businessKey)
   {
      this.businessKey = businessKey;
   }


   /*******************************************************************************
    ** Set businessKey fluently.
    *******************************************************************************/
   public EdiTransaction withBusinessKey(String businessKey)
   {
      this.businessKey = businessKey;
      return (this);
   }


   /*******************************************************************************
    ** Get secondaryKey.
    *******************************************************************************/
   public String getSecondaryKey()
   {
      return (secondaryKey);
   }


   /*******************************************************************************
    ** Set secondaryKey.
    *******************************************************************************/
   public void setSecondaryKey(String secondaryKey)
   {
      this.secondaryKey = secondaryKey;
   }


   /*******************************************************************************
    ** Set secondaryKey fluently.
    *******************************************************************************/
   public EdiTransaction withSecondaryKey(String secondaryKey)
   {
      this.secondaryKey = secondaryKey;
      return (this);
   }


   /*******************************************************************************
    ** Get phiBusinessKey.
    *******************************************************************************/
   public String getPhiBusinessKey()
   {
      return (phiBusinessKey);
   }


   /*******************************************************************************
    ** Set phiBusinessKey.
    *******************************************************************************/
   public void setPhiBusinessKey(String phiBusinessKey)
   {
      this.phiBusinessKey = phiBusinessKey;
   }


   /*******************************************************************************
    ** Set phiBusinessKey fluently.
    *******************************************************************************/
   public EdiTransaction withPhiBusinessKey(String phiBusinessKey)
   {
      this.phiBusinessKey = phiBusinessKey;
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
   public EdiTransaction withStatus(String status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Get ackStatus.
    *******************************************************************************/
   public String getAckStatus()
   {
      return (ackStatus);
   }


   /*******************************************************************************
    ** Set ackStatus.
    *******************************************************************************/
   public void setAckStatus(String ackStatus)
   {
      this.ackStatus = ackStatus;
   }


   /*******************************************************************************
    ** Set ackStatus fluently.
    *******************************************************************************/
   public EdiTransaction withAckStatus(String ackStatus)
   {
      this.ackStatus = ackStatus;
      return (this);
   }


   /*******************************************************************************
    ** Get errorCount.
    *******************************************************************************/
   public Integer getErrorCount()
   {
      return (errorCount);
   }


   /*******************************************************************************
    ** Set errorCount.
    *******************************************************************************/
   public void setErrorCount(Integer errorCount)
   {
      this.errorCount = errorCount;
   }


   /*******************************************************************************
    ** Set errorCount fluently.
    *******************************************************************************/
   public EdiTransaction withErrorCount(Integer errorCount)
   {
      this.errorCount = errorCount;
      return (this);
   }


   /*******************************************************************************
    ** Get warningCount.
    *******************************************************************************/
   public Integer getWarningCount()
   {
      return (warningCount);
   }


   /*******************************************************************************
    ** Set warningCount.
    *******************************************************************************/
   public void setWarningCount(Integer warningCount)
   {
      this.warningCount = warningCount;
   }


   /*******************************************************************************
    ** Set warningCount fluently.
    *******************************************************************************/
   public EdiTransaction withWarningCount(Integer warningCount)
   {
      this.warningCount = warningCount;
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
   public EdiTransaction withGuideId(Integer guideId)
   {
      this.guideId = guideId;
      return (this);
   }


   /*******************************************************************************
    ** Get guideRevision.
    *******************************************************************************/
   public Integer getGuideRevision()
   {
      return (guideRevision);
   }


   /*******************************************************************************
    ** Set guideRevision.
    *******************************************************************************/
   public void setGuideRevision(Integer guideRevision)
   {
      this.guideRevision = guideRevision;
   }


   /*******************************************************************************
    ** Set guideRevision fluently.
    *******************************************************************************/
   public EdiTransaction withGuideRevision(Integer guideRevision)
   {
      this.guideRevision = guideRevision;
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
   public EdiTransaction withMapId(Integer mapId)
   {
      this.mapId = mapId;
      return (this);
   }


   /*******************************************************************************
    ** Get mapRevision.
    *******************************************************************************/
   public Integer getMapRevision()
   {
      return (mapRevision);
   }


   /*******************************************************************************
    ** Set mapRevision.
    *******************************************************************************/
   public void setMapRevision(Integer mapRevision)
   {
      this.mapRevision = mapRevision;
   }


   /*******************************************************************************
    ** Set mapRevision fluently.
    *******************************************************************************/
   public EdiTransaction withMapRevision(Integer mapRevision)
   {
      this.mapRevision = mapRevision;
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
   public EdiTransaction withTargetTableName(String targetTableName)
   {
      this.targetTableName = targetTableName;
      return (this);
   }


   /*******************************************************************************
    ** Get targetRecordId.
    *******************************************************************************/
   public String getTargetRecordId()
   {
      return (targetRecordId);
   }


   /*******************************************************************************
    ** Set targetRecordId.
    *******************************************************************************/
   public void setTargetRecordId(String targetRecordId)
   {
      this.targetRecordId = targetRecordId;
   }


   /*******************************************************************************
    ** Set targetRecordId fluently.
    *******************************************************************************/
   public EdiTransaction withTargetRecordId(String targetRecordId)
   {
      this.targetRecordId = targetRecordId;
      return (this);
   }


   /*******************************************************************************
    ** Get segmentCount.
    *******************************************************************************/
   public Integer getSegmentCount()
   {
      return (segmentCount);
   }


   /*******************************************************************************
    ** Set segmentCount.
    *******************************************************************************/
   public void setSegmentCount(Integer segmentCount)
   {
      this.segmentCount = segmentCount;
   }


   /*******************************************************************************
    ** Set segmentCount fluently.
    *******************************************************************************/
   public EdiTransaction withSegmentCount(Integer segmentCount)
   {
      this.segmentCount = segmentCount;
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
   public EdiTransaction withContainsPhi(Boolean containsPhi)
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
   public EdiTransaction withCreateDate(Instant createDate)
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
   public EdiTransaction withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

   /*******************************************************************************
    ** One-based occurrence within the parent payload, interchange or group.
    *******************************************************************************/
   public Integer getInboundPosition()
   {
      return (inboundPosition);
   }

   /*******************************************************************************
    ** Set the stable source position used for retry checkpoints.
    *******************************************************************************/
   public void setInboundPosition(Integer inboundPosition)
   {
      this.inboundPosition = inboundPosition;
   }

   /*******************************************************************************
    ** Fluently set the source position.
    *******************************************************************************/
   public EdiTransaction withInboundPosition(Integer inboundPosition)
   {
      this.inboundPosition = inboundPosition;
      return (this);
   }
}
