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


import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import com.kingsrook.qbits.edi.security.EdiSecrets;


/*******************************************************************************
 ** AES-256-GCM envelope encryption with independent random data keys and key rotation.
 *******************************************************************************/
public class PayloadCrypto
{
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final byte[] AAD = "QQQ-EDI-PAYLOAD-1".getBytes(StandardCharsets.US_ASCII);
   private final EdiSecrets secrets;

   /*******************************************************************************
    ** Resolve master keys only during cryptographic operations.
    *******************************************************************************/
   public PayloadCrypto(EdiSecrets secrets)
   {
      this.secrets = Objects.requireNonNull(secrets);
   }

   /*******************************************************************************
    ** Encrypted envelope; byte layout: version, wrap nonce, wrapped key, data nonce, ciphertext.
    *******************************************************************************/
   public record Encrypted(byte[] ciphertext, String keyId, String sha256, long sizeBytes)
   {
   }

   /*******************************************************************************
    ** Encrypt with a new data key and two fresh nonces.
    *******************************************************************************/
   public Encrypted encrypt(byte[] plaintext) throws GeneralSecurityException
   {
      byte[] master = key("QQQ_EDI_PAYLOAD_KEY");
      byte[] dataKey = random(32);
      try
      {
         byte[] wrapNonce = random(12);
         byte[] nonce = random(12);
         byte[] wrapped = crypt(Cipher.ENCRYPT_MODE, master, wrapNonce, dataKey);
         byte[] ciphertext = crypt(Cipher.ENCRYPT_MODE, dataKey, nonce, plaintext);
         byte[] envelope = ByteBuffer.allocate(1 + 12 + 48 + 12 + ciphertext.length).put((byte) 1).put(wrapNonce).put(wrapped).put(nonce).put(ciphertext).array();
         return (new Encrypted(envelope, digest(master).substring(0, 32), digest(plaintext), plaintext.length));
      }
      finally
      {
         Arrays.fill(master, (byte) 0);
         Arrays.fill(dataKey, (byte) 0);
      }
   }

   /*******************************************************************************
    ** Authenticate both layers and plaintext metadata; try the previous key only for its key id.
    *******************************************************************************/
   public byte[] decrypt(Encrypted encrypted) throws GeneralSecurityException
   {
      byte[] envelope = encrypted.ciphertext();
      if(envelope == null || envelope.length < 89 || envelope[0] != 1)
      {
         throw new GeneralSecurityException("Invalid payload envelope");
      }
      byte[] master = key("QQQ_EDI_PAYLOAD_KEY");
      if(!digest(master).substring(0, 32).equals(encrypted.keyId()))
      {
         Arrays.fill(master, (byte) 0);
         master = key("QQQ_EDI_PAYLOAD_KEY_PREVIOUS");
      }
      byte[] dataKey = null;
      try
      {
         if(!digest(master).substring(0, 32).equals(encrypted.keyId()))
         {
            throw new GeneralSecurityException("Payload key is unavailable");
         }
         dataKey = crypt(Cipher.DECRYPT_MODE, master, Arrays.copyOfRange(envelope, 1, 13), Arrays.copyOfRange(envelope, 13, 61));
         byte[] plaintext = crypt(Cipher.DECRYPT_MODE, dataKey, Arrays.copyOfRange(envelope, 61, 73), Arrays.copyOfRange(envelope, 73, envelope.length));
         if(plaintext.length != encrypted.sizeBytes() || !digest(plaintext).equals(encrypted.sha256()))
         {
            Arrays.fill(plaintext, (byte) 0);
            throw new GeneralSecurityException("Payload integrity check failed");
         }
         return (plaintext);
      }
      finally
      {
         Arrays.fill(master, (byte) 0);
         if(dataKey != null)
         {
            Arrays.fill(dataKey, (byte) 0);
         }
      }
   }

   /*******************************************************************************
    ** Decode a 256-bit key with diagnostics naming only the variable.
    *******************************************************************************/
   private byte[] key(String name) throws GeneralSecurityException
   {
      String value = secrets.resolve(name);
      if(value == null)
      {
         throw new GeneralSecurityException("Environment variable " + name + " is not set");
      }
      try
      {
         byte[] key = Base64.getDecoder().decode(value);
         if(key.length == 32)
         {
            return (key);
         }
         Arrays.fill(key, (byte) 0);
      }
      catch(IllegalArgumentException e)
      {
         // Discard decoder messages, which could include secret input.
      }
      throw new GeneralSecurityException("Environment variable " + name + " must encode 32 bytes");
   }

   /*******************************************************************************
    ** Run authenticated encryption with a version-bound associated-data tag.
    *******************************************************************************/
   private static byte[] crypt(int mode, byte[] key, byte[] nonce, byte[] input) throws GeneralSecurityException
   {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
      cipher.updateAAD(AAD);
      return (cipher.doFinal(input));
   }

   /*******************************************************************************
    ** Generate fresh key or nonce bytes.
    *******************************************************************************/
   private static byte[] random(int size)
   {
      byte[] bytes = new byte[size];
      RANDOM.nextBytes(bytes);
      return (bytes);
   }

   /*******************************************************************************
    ** Hex SHA-256 digest for plaintext integrity or master key identification.
    *******************************************************************************/
   public static String digest(byte[] value) throws GeneralSecurityException
   {
      return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)));
   }
}
