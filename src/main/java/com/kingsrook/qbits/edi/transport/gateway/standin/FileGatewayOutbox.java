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

package com.kingsrook.qbits.edi.transport.gateway.standin;

import java.io.InputStream;

/*******************************************************************************
 ** STAND-IN(core=QRun-IO/qbit-file-gateway, removeBy=qbit-file-gateway 0.1.0-RC.1)
 *******************************************************************************/
public final class FileGatewayOutbox
{
   private static volatile Provider provider;

   /*******************************************************************************
    ** Provider consumes the stream synchronously before returning its durable ref.
    *******************************************************************************/
   @FunctionalInterface
   public interface Provider
   {
      /*******************************************************************************
       ** Persist to the named OUTBOX.
       *******************************************************************************/
      OutboxFileRef put(String endpointName, String fileName, InputStream content, long sizeBytes) throws Exception;
   }

   /*******************************************************************************
    ** Host/test registration; null unregisters.
    *******************************************************************************/
   public static void register(Provider implementation)
   {
      provider = implementation;
   }

   /*******************************************************************************
    ** Whether the host has installed a provider.
    *******************************************************************************/
   public static boolean isAvailable()
   {
      return (provider != null);
   }

   /*******************************************************************************
    ** Same facade contract as the agreed gateway API.
    *******************************************************************************/
   public static OutboxFileRef put(String endpointName, String fileName, InputStream content, long sizeBytes) throws Exception
   {
      Provider implementation = provider;
      if(implementation == null)
      {
         throw new FileGatewayUnavailableException();
      }
      return (implementation.put(endpointName, fileName, content, sizeBytes));
   }
}
