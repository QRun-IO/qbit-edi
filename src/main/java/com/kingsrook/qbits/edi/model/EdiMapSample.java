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
 ** Persistent edi_map_sample record.
 *******************************************************************************/
public class EdiMapSample extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_map_sample";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "map_id", possibleValueSourceName = "edi_map")
   private Integer mapId;

   @QField(backendName = "name", maxLength = 255)
   private String name;

   @QField(backendName = "payload_ref", maxLength = 255)
   private String payloadRef;

   @QField(backendName = "expected_result_json", maxLength = 10000)
   private String expectedResultJson;

   @QField(backendName = "last_preview_at")
   private Instant lastPreviewAt;

   @QField(backendName = "last_preview_status", maxLength = 255)
   private String lastPreviewStatus;

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
   public EdiMapSample withId(Integer value)
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
   public EdiMapSample withMapId(Integer value)
   {
      mapId = value;
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
   public EdiMapSample withName(String value)
   {
      name = value;
      return (this);
   }

   /*******************************************************************************
    ** Get payloadRef.
    *******************************************************************************/
   public String getPayloadRef()
   {
      return (payloadRef);
   }

   /*******************************************************************************
    ** Set payloadRef.
    *******************************************************************************/
   public void setPayloadRef(String value)
   {
      payloadRef = value;
   }

   /*******************************************************************************
    ** Configure payloadRef.
    *******************************************************************************/
   public EdiMapSample withPayloadRef(String value)
   {
      payloadRef = value;
      return (this);
   }

   /*******************************************************************************
    ** Get expectedResultJson.
    *******************************************************************************/
   public String getExpectedResultJson()
   {
      return (expectedResultJson);
   }

   /*******************************************************************************
    ** Set expectedResultJson.
    *******************************************************************************/
   public void setExpectedResultJson(String value)
   {
      expectedResultJson = value;
   }

   /*******************************************************************************
    ** Configure expectedResultJson.
    *******************************************************************************/
   public EdiMapSample withExpectedResultJson(String value)
   {
      expectedResultJson = value;
      return (this);
   }

   /*******************************************************************************
    ** Get lastPreviewAt.
    *******************************************************************************/
   public Instant getLastPreviewAt()
   {
      return (lastPreviewAt);
   }

   /*******************************************************************************
    ** Set lastPreviewAt.
    *******************************************************************************/
   public void setLastPreviewAt(Instant value)
   {
      lastPreviewAt = value;
   }

   /*******************************************************************************
    ** Configure lastPreviewAt.
    *******************************************************************************/
   public EdiMapSample withLastPreviewAt(Instant value)
   {
      lastPreviewAt = value;
      return (this);
   }

   /*******************************************************************************
    ** Get lastPreviewStatus.
    *******************************************************************************/
   public String getLastPreviewStatus()
   {
      return (lastPreviewStatus);
   }

   /*******************************************************************************
    ** Set lastPreviewStatus.
    *******************************************************************************/
   public void setLastPreviewStatus(String value)
   {
      lastPreviewStatus = value;
   }

   /*******************************************************************************
    ** Configure lastPreviewStatus.
    *******************************************************************************/
   public EdiMapSample withLastPreviewStatus(String value)
   {
      lastPreviewStatus = value;
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
   public EdiMapSample withCreateDate(Instant value)
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
   public EdiMapSample withModifyDate(Instant value)
   {
      modifyDate = value;
      return (this);
   }
}
