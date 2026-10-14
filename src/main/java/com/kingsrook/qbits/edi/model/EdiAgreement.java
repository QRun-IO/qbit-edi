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
 ** Persistent edi_agreement record.
 *******************************************************************************/
public class EdiAgreement extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_agreement";

   @QField(backendName = "release_character", maxLength = 255, defaultValue = "?")
   private String releaseCharacter;

   @QField(backendName = "decimal_mark", maxLength = 255, defaultValue = ".")
   private String decimalMark;

   @QField(backendName = "syntax_identifier", maxLength = 255, defaultValue = "UNOC")
   private String syntaxIdentifier;

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "partner_id", possibleValueSourceName = "edi_partner")
   private Integer partnerId;

   @QField(backendName = "local_identity_id", possibleValueSourceName = "edi_identity")
   private Integer localIdentityId;

   @QField(backendName = "partner_identity_id", possibleValueSourceName = "edi_identity")
   private Integer partnerIdentityId;

   @QField(backendName = "standard", maxLength = 255, possibleValueSourceName = "edi_standard")
   private String standard;

   @QField(backendName = "usage_indicator", maxLength = 255, possibleValueSourceName = "edi_usage_indicator")
   private String usageIndicator;

   @QField(backendName = "interchange_version", maxLength = 255)
   private String interchangeVersion;

   @QField(backendName = "element_separator", maxLength = 255, defaultValue = "*")
   private String elementSeparator;

   @QField(backendName = "segment_terminator", maxLength = 255, defaultValue = "~")
   private String segmentTerminator;

   @QField(backendName = "component_separator", maxLength = 255, defaultValue = ":")
   private String componentSeparator;

   @QField(backendName = "repetition_separator", maxLength = 255, defaultValue = "^")
   private String repetitionSeparator;

   @QField(backendName = "line_ending", maxLength = 255, possibleValueSourceName = "edi_line_ending", defaultValue = "NONE")
   private String lineEnding;

   @QField(backendName = "request_ta1")
   private Boolean requestTa1;

   @QField(backendName = "send_ta1", maxLength = 255, possibleValueSourceName = "edi_send_ta1", defaultValue = "ON_ERROR")
   private String sendTa1;

   @QField(backendName = "inbound_transport_id", possibleValueSourceName = "edi_transport")
   private Integer inboundTransportId;

   @QField(backendName = "outbound_transport_id", possibleValueSourceName = "edi_transport")
   private Integer outboundTransportId;

   @QField(backendName = "control_number_scope", maxLength = 255, possibleValueSourceName = "edi_control_number_scope", defaultValue = "AGREEMENT")
   private String controlNumberScope;

   @QField(backendName = "duplicate_window_days", defaultValue = "365")
   private Integer duplicateWindowDays;

   @QField(backendName = "on_duplicate", maxLength = 255, possibleValueSourceName = "edi_on_duplicate", defaultValue = "REJECT_TA1")
   private String onDuplicate;

   @QField(backendName = "max_interchange_bytes", defaultValue = "52428800")
   private Long maxInterchangeBytes;

   @QField(backendName = "apply_test_documents", defaultValue = "false")
   private Boolean applyTestDocuments;

   @QField(backendName = "is_active", defaultValue = "true")
   private Boolean isActive;

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
   public EdiAgreement withId(Integer id)
   {
      this.id = id;
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
   public EdiAgreement withPartnerId(Integer partnerId)
   {
      this.partnerId = partnerId;
      return (this);
   }


   /*******************************************************************************
    ** Get localIdentityId.
    *******************************************************************************/
   public Integer getLocalIdentityId()
   {
      return (localIdentityId);
   }


   /*******************************************************************************
    ** Set localIdentityId.
    *******************************************************************************/
   public void setLocalIdentityId(Integer localIdentityId)
   {
      this.localIdentityId = localIdentityId;
   }


   /*******************************************************************************
    ** Set localIdentityId fluently.
    *******************************************************************************/
   public EdiAgreement withLocalIdentityId(Integer localIdentityId)
   {
      this.localIdentityId = localIdentityId;
      return (this);
   }


   /*******************************************************************************
    ** Get partnerIdentityId.
    *******************************************************************************/
   public Integer getPartnerIdentityId()
   {
      return (partnerIdentityId);
   }


   /*******************************************************************************
    ** Set partnerIdentityId.
    *******************************************************************************/
   public void setPartnerIdentityId(Integer partnerIdentityId)
   {
      this.partnerIdentityId = partnerIdentityId;
   }


   /*******************************************************************************
    ** Set partnerIdentityId fluently.
    *******************************************************************************/
   public EdiAgreement withPartnerIdentityId(Integer partnerIdentityId)
   {
      this.partnerIdentityId = partnerIdentityId;
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
   public EdiAgreement withStandard(String standard)
   {
      this.standard = standard;
      return (this);
   }


   /*******************************************************************************
    ** Get usageIndicator.
    *******************************************************************************/
   public String getUsageIndicator()
   {
      return (usageIndicator);
   }


   /*******************************************************************************
    ** Set usageIndicator.
    *******************************************************************************/
   public void setUsageIndicator(String usageIndicator)
   {
      this.usageIndicator = usageIndicator;
   }


   /*******************************************************************************
    ** Set usageIndicator fluently.
    *******************************************************************************/
   public EdiAgreement withUsageIndicator(String usageIndicator)
   {
      this.usageIndicator = usageIndicator;
      return (this);
   }


   /*******************************************************************************
    ** Get interchangeVersion.
    *******************************************************************************/
   public String getInterchangeVersion()
   {
      return (interchangeVersion);
   }


   /*******************************************************************************
    ** Set interchangeVersion.
    *******************************************************************************/
   public void setInterchangeVersion(String interchangeVersion)
   {
      this.interchangeVersion = interchangeVersion;
   }


   /*******************************************************************************
    ** Set interchangeVersion fluently.
    *******************************************************************************/
   public EdiAgreement withInterchangeVersion(String interchangeVersion)
   {
      this.interchangeVersion = interchangeVersion;
      return (this);
   }


   /*******************************************************************************
    ** Get elementSeparator.
    *******************************************************************************/
   public String getElementSeparator()
   {
      return (elementSeparator);
   }


   /*******************************************************************************
    ** Set elementSeparator.
    *******************************************************************************/
   public void setElementSeparator(String elementSeparator)
   {
      this.elementSeparator = elementSeparator;
   }


   /*******************************************************************************
    ** Set elementSeparator fluently.
    *******************************************************************************/
   public EdiAgreement withElementSeparator(String elementSeparator)
   {
      this.elementSeparator = elementSeparator;
      return (this);
   }


   /*******************************************************************************
    ** Get segmentTerminator.
    *******************************************************************************/
   public String getSegmentTerminator()
   {
      return (segmentTerminator);
   }


   /*******************************************************************************
    ** Set segmentTerminator.
    *******************************************************************************/
   public void setSegmentTerminator(String segmentTerminator)
   {
      this.segmentTerminator = segmentTerminator;
   }


   /*******************************************************************************
    ** Set segmentTerminator fluently.
    *******************************************************************************/
   public EdiAgreement withSegmentTerminator(String segmentTerminator)
   {
      this.segmentTerminator = segmentTerminator;
      return (this);
   }


   /*******************************************************************************
    ** Get componentSeparator.
    *******************************************************************************/
   public String getComponentSeparator()
   {
      return (componentSeparator);
   }


   /*******************************************************************************
    ** Set componentSeparator.
    *******************************************************************************/
   public void setComponentSeparator(String componentSeparator)
   {
      this.componentSeparator = componentSeparator;
   }


   /*******************************************************************************
    ** Set componentSeparator fluently.
    *******************************************************************************/
   public EdiAgreement withComponentSeparator(String componentSeparator)
   {
      this.componentSeparator = componentSeparator;
      return (this);
   }


   /*******************************************************************************
    ** Get repetitionSeparator.
    *******************************************************************************/
   public String getRepetitionSeparator()
   {
      return (repetitionSeparator);
   }


   /*******************************************************************************
    ** Set repetitionSeparator.
    *******************************************************************************/
   public void setRepetitionSeparator(String repetitionSeparator)
   {
      this.repetitionSeparator = repetitionSeparator;
   }


   /*******************************************************************************
    ** Set repetitionSeparator fluently.
    *******************************************************************************/
   public EdiAgreement withRepetitionSeparator(String repetitionSeparator)
   {
      this.repetitionSeparator = repetitionSeparator;
      return (this);
   }


   /*******************************************************************************
    ** Get lineEnding.
    *******************************************************************************/
   public String getLineEnding()
   {
      return (lineEnding);
   }


   /*******************************************************************************
    ** Set lineEnding.
    *******************************************************************************/
   public void setLineEnding(String lineEnding)
   {
      this.lineEnding = lineEnding;
   }


   /*******************************************************************************
    ** Set lineEnding fluently.
    *******************************************************************************/
   public EdiAgreement withLineEnding(String lineEnding)
   {
      this.lineEnding = lineEnding;
      return (this);
   }


   /*******************************************************************************
    ** Get requestTa1.
    *******************************************************************************/
   public Boolean getRequestTa1()
   {
      return (requestTa1);
   }


   /*******************************************************************************
    ** Set requestTa1.
    *******************************************************************************/
   public void setRequestTa1(Boolean requestTa1)
   {
      this.requestTa1 = requestTa1;
   }


   /*******************************************************************************
    ** Set requestTa1 fluently.
    *******************************************************************************/
   public EdiAgreement withRequestTa1(Boolean requestTa1)
   {
      this.requestTa1 = requestTa1;
      return (this);
   }


   /*******************************************************************************
    ** Get sendTa1.
    *******************************************************************************/
   public String getSendTa1()
   {
      return (sendTa1);
   }


   /*******************************************************************************
    ** Set sendTa1.
    *******************************************************************************/
   public void setSendTa1(String sendTa1)
   {
      this.sendTa1 = sendTa1;
   }


   /*******************************************************************************
    ** Set sendTa1 fluently.
    *******************************************************************************/
   public EdiAgreement withSendTa1(String sendTa1)
   {
      this.sendTa1 = sendTa1;
      return (this);
   }


   /*******************************************************************************
    ** Get inboundTransportId.
    *******************************************************************************/
   public Integer getInboundTransportId()
   {
      return (inboundTransportId);
   }


   /*******************************************************************************
    ** Set inboundTransportId.
    *******************************************************************************/
   public void setInboundTransportId(Integer inboundTransportId)
   {
      this.inboundTransportId = inboundTransportId;
   }


   /*******************************************************************************
    ** Set inboundTransportId fluently.
    *******************************************************************************/
   public EdiAgreement withInboundTransportId(Integer inboundTransportId)
   {
      this.inboundTransportId = inboundTransportId;
      return (this);
   }


   /*******************************************************************************
    ** Get outboundTransportId.
    *******************************************************************************/
   public Integer getOutboundTransportId()
   {
      return (outboundTransportId);
   }


   /*******************************************************************************
    ** Set outboundTransportId.
    *******************************************************************************/
   public void setOutboundTransportId(Integer outboundTransportId)
   {
      this.outboundTransportId = outboundTransportId;
   }


   /*******************************************************************************
    ** Set outboundTransportId fluently.
    *******************************************************************************/
   public EdiAgreement withOutboundTransportId(Integer outboundTransportId)
   {
      this.outboundTransportId = outboundTransportId;
      return (this);
   }


   /*******************************************************************************
    ** Get controlNumberScope.
    *******************************************************************************/
   public String getControlNumberScope()
   {
      return (controlNumberScope);
   }


   /*******************************************************************************
    ** Set controlNumberScope.
    *******************************************************************************/
   public void setControlNumberScope(String controlNumberScope)
   {
      this.controlNumberScope = controlNumberScope;
   }


   /*******************************************************************************
    ** Set controlNumberScope fluently.
    *******************************************************************************/
   public EdiAgreement withControlNumberScope(String controlNumberScope)
   {
      this.controlNumberScope = controlNumberScope;
      return (this);
   }


   /*******************************************************************************
    ** Get duplicateWindowDays.
    *******************************************************************************/
   public Integer getDuplicateWindowDays()
   {
      return (duplicateWindowDays);
   }


   /*******************************************************************************
    ** Set duplicateWindowDays.
    *******************************************************************************/
   public void setDuplicateWindowDays(Integer duplicateWindowDays)
   {
      this.duplicateWindowDays = duplicateWindowDays;
   }


   /*******************************************************************************
    ** Set duplicateWindowDays fluently.
    *******************************************************************************/
   public EdiAgreement withDuplicateWindowDays(Integer duplicateWindowDays)
   {
      this.duplicateWindowDays = duplicateWindowDays;
      return (this);
   }


   /*******************************************************************************
    ** Get onDuplicate.
    *******************************************************************************/
   public String getOnDuplicate()
   {
      return (onDuplicate);
   }


   /*******************************************************************************
    ** Set onDuplicate.
    *******************************************************************************/
   public void setOnDuplicate(String onDuplicate)
   {
      this.onDuplicate = onDuplicate;
   }


   /*******************************************************************************
    ** Set onDuplicate fluently.
    *******************************************************************************/
   public EdiAgreement withOnDuplicate(String onDuplicate)
   {
      this.onDuplicate = onDuplicate;
      return (this);
   }


   /*******************************************************************************
    ** Get maxInterchangeBytes.
    *******************************************************************************/
   public Long getMaxInterchangeBytes()
   {
      return (maxInterchangeBytes);
   }


   /*******************************************************************************
    ** Set maxInterchangeBytes.
    *******************************************************************************/
   public void setMaxInterchangeBytes(Long maxInterchangeBytes)
   {
      this.maxInterchangeBytes = maxInterchangeBytes;
   }


   /*******************************************************************************
    ** Set maxInterchangeBytes fluently.
    *******************************************************************************/
   public EdiAgreement withMaxInterchangeBytes(Long maxInterchangeBytes)
   {
      this.maxInterchangeBytes = maxInterchangeBytes;
      return (this);
   }


   /*******************************************************************************
    ** Get applyTestDocuments.
    *******************************************************************************/
   public Boolean getApplyTestDocuments()
   {
      return (applyTestDocuments);
   }


   /*******************************************************************************
    ** Set applyTestDocuments.
    *******************************************************************************/
   public void setApplyTestDocuments(Boolean applyTestDocuments)
   {
      this.applyTestDocuments = applyTestDocuments;
   }


   /*******************************************************************************
    ** Set applyTestDocuments fluently.
    *******************************************************************************/
   public EdiAgreement withApplyTestDocuments(Boolean applyTestDocuments)
   {
      this.applyTestDocuments = applyTestDocuments;
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
   public EdiAgreement withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
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
   public EdiAgreement withNotes(String notes)
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
   public EdiAgreement withCreateDate(Instant createDate)
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
   public EdiAgreement withModifyDate(Instant modifyDate)
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
   public EdiAgreement withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }


   /*******************************************************************************
    ** Get releaseCharacter.
    *******************************************************************************/
   public String getReleaseCharacter()
   {
      return (releaseCharacter);
   }

   /*******************************************************************************
    ** Set releaseCharacter.
    *******************************************************************************/
   public void setReleaseCharacter(String releaseCharacter)
   {
      this.releaseCharacter = releaseCharacter;
   }

   /*******************************************************************************
    ** Set releaseCharacter fluently.
    *******************************************************************************/
   public EdiAgreement withReleaseCharacter(String releaseCharacter)
   {
      this.releaseCharacter = releaseCharacter;
      return (this);
   }

   /*******************************************************************************
    ** Get decimalMark.
    *******************************************************************************/
   public String getDecimalMark()
   {
      return (decimalMark);
   }

   /*******************************************************************************
    ** Set decimalMark.
    *******************************************************************************/
   public void setDecimalMark(String decimalMark)
   {
      this.decimalMark = decimalMark;
   }

   /*******************************************************************************
    ** Set decimalMark fluently.
    *******************************************************************************/
   public EdiAgreement withDecimalMark(String decimalMark)
   {
      this.decimalMark = decimalMark;
      return (this);
   }

   /*******************************************************************************
    ** Get syntaxIdentifier.
    *******************************************************************************/
   public String getSyntaxIdentifier()
   {
      return (syntaxIdentifier);
   }

   /*******************************************************************************
    ** Set syntaxIdentifier.
    *******************************************************************************/
   public void setSyntaxIdentifier(String syntaxIdentifier)
   {
      this.syntaxIdentifier = syntaxIdentifier;
   }

   /*******************************************************************************
    ** Set syntaxIdentifier fluently.
    *******************************************************************************/
   public EdiAgreement withSyntaxIdentifier(String syntaxIdentifier)
   {
      this.syntaxIdentifier = syntaxIdentifier;
      return (this);
   }
}
