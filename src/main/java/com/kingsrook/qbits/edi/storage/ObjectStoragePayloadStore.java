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
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qbits.objectstorage.api.DeleteRequest;
import com.kingsrook.qbits.objectstorage.api.ObjectStorage;
import com.kingsrook.qbits.objectstorage.api.ObjectStream;
import com.kingsrook.qbits.objectstorage.api.PutRequest;

/*******************************************************************************
 ** Optional public object-storage API adapter with the database envelope format.
 *******************************************************************************/
public class ObjectStoragePayloadStore implements PayloadStore
{
   private final PayloadCrypto crypto;
   private final String bucket;
   private final Clock clock;

   /*******************************************************************************
    ** Bucket alias is configuration, never a provider credential.
    *******************************************************************************/
   public ObjectStoragePayloadStore(PayloadCrypto crypto, String bucket, Clock clock)
   {
      this.crypto = crypto;
      this.bucket = bucket;
      this.clock = clock;
   }

   /*******************************************************************************
    ** Random object keys never contain document values or filenames.
    *******************************************************************************/
   public String key(PayloadKind kind)
   {
      return ("edi/" + LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + kind.name().toLowerCase(java.util.Locale.ROOT) + "/" + UUID.randomUUID());
   }

   /*******************************************************************************
    ** Only non-secret envelope metadata is stored as object headers.
    *******************************************************************************/
   public Map<String, String> metadata(PayloadCrypto.Encrypted encrypted)
   {
      return (Map.of("key-id", encrypted.keyId(), "sha256", encrypted.sha256(), "size-bytes", String.valueOf(encrypted.sizeBytes()), "envelope-version", "1"));
   }

   /*******************************************************************************
    ** Encrypt before passing bytes to the external storage API.
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
      String key = key(kind);
      ObjectStorage.asSystem("edi", UUID.randomUUID().toString()).bucket(bucket).put(new PutRequest().withKey(key)
         .withBytes(encrypted.ciphertext()).withContentType("application/octet-stream").withMetadata(metadata(encrypted)));
      return (new PayloadRef("objectstorage:" + bucket + ":" + key, encrypted.sha256(), encrypted.sizeBytes()));
   }

   /*******************************************************************************
    ** Authenticate the complete envelope before releasing plaintext.
    *******************************************************************************/
   @Override
   public InputStream open(PayloadRef ref) throws Exception
   {
      String[] parts = reference(ref);
      try(ObjectStream stream = ObjectStorage.asSystem("edi", UUID.randomUUID().toString()).bucket(parts[0]).get(parts[1], null))
      {
         Map<String, String> metadata = stream.getHeaders().getUserMetadata();
         return (new ByteArrayInputStream(crypto.decrypt(new PayloadCrypto.Encrypted(stream.readAllBytes(), metadata.get("key-id"), metadata.get("sha256"), Long.parseLong(metadata.get("size-bytes"))))));
      }
   }

   /*******************************************************************************
    ** Delete using the public system API and the bucket's retention semantics.
    *******************************************************************************/
   @Override
   public void delete(PayloadRef ref)
   {
      String[] parts = reference(ref);
      ObjectStorage.asSystem("edi", UUID.randomUUID().toString()).bucket(parts[0]).delete(new DeleteRequest().withKey(parts[1]));
   }

   /*******************************************************************************
    ** Refuse references outside the configured EDI bucket and key prefix.
    *******************************************************************************/
   private String[] reference(PayloadRef ref)
   {
      String prefix = "objectstorage:" + bucket + ":";
      if(!ref.value().startsWith(prefix + "edi/"))
      {
         throw new IllegalArgumentException("Invalid EDI object reference");
      }
      return (new String[] {bucket, ref.value().substring(prefix.length())});
   }
}
