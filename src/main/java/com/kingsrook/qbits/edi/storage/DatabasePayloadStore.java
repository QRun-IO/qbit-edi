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

package com.kingsrook.qbits.edi.storage;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Encrypted database fallback. All content remains in memory and QQQ-managed rows.
 *******************************************************************************/
public class DatabasePayloadStore implements PayloadStore
{
   private final PayloadCrypto crypto;

   /*******************************************************************************
    ** Configure envelope encryption.
    *******************************************************************************/
   public DatabasePayloadStore(PayloadCrypto crypto)
   {
      this.crypto = crypto;
   }

   /*******************************************************************************
    ** Store ciphertext through a normal insert action.
    *******************************************************************************/
   @Override
   public PayloadRef put(PayloadKind kind, InputStream input) throws Exception
   {
      byte[] plaintext = input.readAllBytes();
      PayloadCrypto.Encrypted encrypted;
      try
      {
         encrypted = crypto.encrypt(plaintext);
      }
      finally
      {
         Arrays.fill(plaintext, (byte) 0);
      }
      QRecord row = EdiRecords.insert("edi_payload", new QRecord().withValue("kind", kind.name()).withValue("ciphertext", encrypted.ciphertext())
         .withValue("keyId", encrypted.keyId()).withValue("sha256", encrypted.sha256()).withValue("sizeBytes", encrypted.sizeBytes()));
      return (new PayloadRef("database:" + row.getValueInteger("id"), encrypted.sha256(), encrypted.sizeBytes()));
   }

   /*******************************************************************************
    ** Authenticate ciphertext before returning any plaintext.
    *******************************************************************************/
   @Override
   public InputStream open(PayloadRef ref) throws Exception
   {
      List<QRecord> records = EdiRecords.query("edi_payload", EdiRecords.equal("id", id(ref)));
      if(records.isEmpty())
      {
         throw new IllegalArgumentException("Payload does not exist");
      }
      QRecord row = records.getFirst();
      return (new ByteArrayInputStream(crypto.decrypt(new PayloadCrypto.Encrypted((byte[]) row.getValue("ciphertext"), row.getValueString("keyId"),
         row.getValueString("sha256"), row.getValueLong("sizeBytes")))));
   }

   /*******************************************************************************
    ** Delete through the standard table action.
    *******************************************************************************/
   @Override
   public void delete(PayloadRef ref) throws Exception
   {
      new DeleteAction().execute(new DeleteInput("edi_payload").withPrimaryKey(id(ref)));
   }

   /*******************************************************************************
    ** Accept database references only.
    *******************************************************************************/
   private static Integer id(PayloadRef ref)
   {
      if(!ref.value().matches("database:[0-9]+"))
      {
         throw new IllegalArgumentException("Not a database payload reference");
      }
      return (Integer.valueOf(ref.value().substring(9)));
   }
}
