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
 ** Persistent edi_outbound_request record.
 *******************************************************************************/
public class EdiOutboundRequest extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_outbound_request";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "agreement_document_id", possibleValueSourceName = "edi_agreement_document")
   private Integer agreementDocumentId;

   @QField(backendName = "source_table_name", maxLength = 255)
   private String sourceTableName;

   @QField(backendName = "source_record_id", maxLength = 255)
   private String sourceRecordId;

   @QField(backendName = "event_id", maxLength = 255)
   private String eventId;

   @QField(backendName = "status", maxLength = 255)
   private String status;

   @QField(backendName = "attempts")
   private Integer attempts;

   @QField(backendName = "next_attempt_at")
   private Instant nextAttemptAt;

   @QField(backendName = "claimed_at")
   private Instant claimedAt;

   @QField(backendName = "last_error", maxLength = 255)
   private String lastError;

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
   public EdiOutboundRequest withId(Integer value)
   {
      id = value;
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
   public void setAgreementDocumentId(Integer value)
   {
      agreementDocumentId = value;
   }

   /*******************************************************************************
    ** Configure agreementDocumentId.
    *******************************************************************************/
   public EdiOutboundRequest withAgreementDocumentId(Integer value)
   {
      agreementDocumentId = value;
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
   public void setSourceTableName(String value)
   {
      sourceTableName = value;
   }

   /*******************************************************************************
    ** Configure sourceTableName.
    *******************************************************************************/
   public EdiOutboundRequest withSourceTableName(String value)
   {
      sourceTableName = value;
      return (this);
   }

   /*******************************************************************************
    ** Get sourceRecordId.
    *******************************************************************************/
   public String getSourceRecordId()
   {
      return (sourceRecordId);
   }

   /*******************************************************************************
    ** Set sourceRecordId.
    *******************************************************************************/
   public void setSourceRecordId(String value)
   {
      sourceRecordId = value;
   }

   /*******************************************************************************
    ** Configure sourceRecordId.
    *******************************************************************************/
   public EdiOutboundRequest withSourceRecordId(String value)
   {
      sourceRecordId = value;
      return (this);
   }

   /*******************************************************************************
    ** Get eventId.
    *******************************************************************************/
   public String getEventId()
   {
      return (eventId);
   }

   /*******************************************************************************
    ** Set eventId.
    *******************************************************************************/
   public void setEventId(String value)
   {
      eventId = value;
   }

   /*******************************************************************************
    ** Configure eventId.
    *******************************************************************************/
   public EdiOutboundRequest withEventId(String value)
   {
      eventId = value;
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
   public EdiOutboundRequest withStatus(String value)
   {
      status = value;
      return (this);
   }

   /*******************************************************************************
    ** Get attempts.
    *******************************************************************************/
   public Integer getAttempts()
   {
      return (attempts);
   }

   /*******************************************************************************
    ** Set attempts.
    *******************************************************************************/
   public void setAttempts(Integer value)
   {
      attempts = value;
   }

   /*******************************************************************************
    ** Configure attempts.
    *******************************************************************************/
   public EdiOutboundRequest withAttempts(Integer value)
   {
      attempts = value;
      return (this);
   }

   /*******************************************************************************
    ** Get nextAttemptAt.
    *******************************************************************************/
   public Instant getNextAttemptAt()
   {
      return (nextAttemptAt);
   }

   /*******************************************************************************
    ** Set nextAttemptAt.
    *******************************************************************************/
   public void setNextAttemptAt(Instant value)
   {
      nextAttemptAt = value;
   }

   /*******************************************************************************
    ** Configure nextAttemptAt.
    *******************************************************************************/
   public EdiOutboundRequest withNextAttemptAt(Instant value)
   {
      nextAttemptAt = value;
      return (this);
   }

   /*******************************************************************************
    ** Get claimedAt.
    *******************************************************************************/
   public Instant getClaimedAt()
   {
      return (claimedAt);
   }

   /*******************************************************************************
    ** Set claimedAt.
    *******************************************************************************/
   public void setClaimedAt(Instant value)
   {
      claimedAt = value;
   }

   /*******************************************************************************
    ** Configure claimedAt.
    *******************************************************************************/
   public EdiOutboundRequest withClaimedAt(Instant value)
   {
      claimedAt = value;
      return (this);
   }

   /*******************************************************************************
    ** Get lastError.
    *******************************************************************************/
   public String getLastError()
   {
      return (lastError);
   }

   /*******************************************************************************
    ** Set lastError.
    *******************************************************************************/
   public void setLastError(String value)
   {
      lastError = value;
   }

   /*******************************************************************************
    ** Configure lastError.
    *******************************************************************************/
   public EdiOutboundRequest withLastError(String value)
   {
      lastError = value;
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
   public EdiOutboundRequest withCreateDate(Instant value)
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
   public EdiOutboundRequest withModifyDate(Instant value)
   {
      modifyDate = value;
      return (this);
   }
}
