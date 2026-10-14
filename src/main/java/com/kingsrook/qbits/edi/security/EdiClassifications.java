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

package com.kingsrook.qbits.edi.security;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QSupplementalFieldMetaData;


/*******************************************************************************
 ** STAND-IN(core=QRun-IO/qqq#1142, removeBy=4.1.0-RC.6). Field classifications on RC.5.
 *******************************************************************************/
public class EdiClassifications extends QSupplementalFieldMetaData
{
   private final String classification;

   /*******************************************************************************
    ** Retain classification as metadata until the core enum is available.
    *******************************************************************************/
   public EdiClassifications(String classification)
   {
      this.classification = classification;
   }

   /*******************************************************************************
    ** Stable supplemental metadata key.
    *******************************************************************************/
   @Override
   public String getType()
   {
      return ("edi.classification");
   }

   /*******************************************************************************
    ** Read the classification without requiring an RC.6 class.
    *******************************************************************************/
   public String getClassification()
   {
      return (classification);
   }

   /*******************************************************************************
    ** Bridge when a host supplies the newer field classification API.
    *******************************************************************************/
   @Override
   public void enrich(QInstance instance, QFieldMetaData field)
   {
      try
      {
         Class<?> type = Class.forName("com.kingsrook.qqq.backend.core.actions.ai.DataClassification");
         Object value = type.getField(classification).get(null);
         field.getClass().getMethod("setDataClassification", type).invoke(field, value);
      }
      catch(ClassNotFoundException e)
      {
         // RC.5 keeps the explicit supplemental classification below.
      }
      catch(ReflectiveOperationException e)
      {
         throw new IllegalStateException("Update the EDI classification bridge for this core version", e);
      }
   }
}
