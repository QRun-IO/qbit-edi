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
 ** Persistent edi_payload record.
 *******************************************************************************/
public class EdiPayload extends QRecordEntity
{
   public static final String TABLE_NAME = "edi_payload";

   @QField(backendName = "id", isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(backendName = "kind", maxLength = 255, possibleValueSourceName = "edi_payload_kind")
   private String kind;

   @QField(backendName = "ciphertext")
   private byte[] ciphertext;

   @QField(backendName = "key_id", maxLength = 255)
   private String keyId;

   @QField(backendName = "sha256", maxLength = 255)
   private String sha256;

   @QField(backendName = "size_bytes")
   private Long sizeBytes;

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
   public EdiPayload withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Get kind.
    *******************************************************************************/
   public String getKind()
   {
      return (kind);
   }


   /*******************************************************************************
    ** Set kind.
    *******************************************************************************/
   public void setKind(String kind)
   {
      this.kind = kind;
   }


   /*******************************************************************************
    ** Set kind fluently.
    *******************************************************************************/
   public EdiPayload withKind(String kind)
   {
      this.kind = kind;
      return (this);
   }


   /*******************************************************************************
    ** Get ciphertext.
    *******************************************************************************/
   public byte[] getCiphertext()
   {
      return (ciphertext);
   }


   /*******************************************************************************
    ** Set ciphertext.
    *******************************************************************************/
   public void setCiphertext(byte[] ciphertext)
   {
      this.ciphertext = ciphertext;
   }


   /*******************************************************************************
    ** Set ciphertext fluently.
    *******************************************************************************/
   public EdiPayload withCiphertext(byte[] ciphertext)
   {
      this.ciphertext = ciphertext;
      return (this);
   }


   /*******************************************************************************
    ** Get keyId.
    *******************************************************************************/
   public String getKeyId()
   {
      return (keyId);
   }


   /*******************************************************************************
    ** Set keyId.
    *******************************************************************************/
   public void setKeyId(String keyId)
   {
      this.keyId = keyId;
   }


   /*******************************************************************************
    ** Set keyId fluently.
    *******************************************************************************/
   public EdiPayload withKeyId(String keyId)
   {
      this.keyId = keyId;
      return (this);
   }


   /*******************************************************************************
    ** Get sha256.
    *******************************************************************************/
   public String getSha256()
   {
      return (sha256);
   }


   /*******************************************************************************
    ** Set sha256.
    *******************************************************************************/
   public void setSha256(String sha256)
   {
      this.sha256 = sha256;
   }


   /*******************************************************************************
    ** Set sha256 fluently.
    *******************************************************************************/
   public EdiPayload withSha256(String sha256)
   {
      this.sha256 = sha256;
      return (this);
   }


   /*******************************************************************************
    ** Get sizeBytes.
    *******************************************************************************/
   public Long getSizeBytes()
   {
      return (sizeBytes);
   }


   /*******************************************************************************
    ** Set sizeBytes.
    *******************************************************************************/
   public void setSizeBytes(Long sizeBytes)
   {
      this.sizeBytes = sizeBytes;
   }


   /*******************************************************************************
    ** Set sizeBytes fluently.
    *******************************************************************************/
   public EdiPayload withSizeBytes(Long sizeBytes)
   {
      this.sizeBytes = sizeBytes;
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
   public EdiPayload withCreateDate(Instant createDate)
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
   public EdiPayload withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
