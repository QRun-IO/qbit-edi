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

package com.kingsrook.qbits.edi.as2;


import java.io.ByteArrayInputStream;
import java.time.Instant;
import com.helger.as2lib.crypto.MIC;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Persistence helpers for AS2 receipts: the raw MDN goes to the encrypted payload
 ** store and a read-only edi_as2_mdn row describes it. Callers run inside the
 ** system context.
 *******************************************************************************/
public final class As2Store
{
   public static final String TABLE = "edi_as2_mdn";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private As2Store()
   {
   }



   /*******************************************************************************
    ** Store the raw MDN entity (headers and body) encrypted.
    *******************************************************************************/
   public static PayloadRef storeRaw(byte[] entity) throws Exception
   {
      return (EdiRuntime.current().payloads().put(PayloadKind.MDN, new ByteArrayInputStream(entity)));
   }



   /*******************************************************************************
    ** The base64 value of a MIC without its algorithm.
    *******************************************************************************/
   public static String micValue(MIC mic)
   {
      return (mic == null ? null : java.util.Base64.getEncoder().encodeToString(mic.micBytes()));
   }



   /*******************************************************************************
    ** Insert a receipt row and return it.
    *******************************************************************************/
   public static QRecord insert(QRecord row) throws QException
   {
      if(row.getValue("receivedAt") == null)
      {
         row.setValue("receivedAt", Instant.now(EdiRuntime.current().config().getClock()));
      }
      return (SystemWrites.insert(TABLE, row));
   }
}
