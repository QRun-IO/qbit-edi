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
import java.time.Instant;
import java.util.function.Supplier;

/*******************************************************************************
 ** STAND-IN(core=QRun-IO/qbit-file-gateway, removeBy=qbit-file-gateway 0.1.0-RC.1)
 *******************************************************************************/
public record InboundFile(Integer uploadJobId, String endpointName, String username, String userId, String remoteAddress, String path,
   String fileName, long sizeBytes, String sha256, Instant receivedAt, String storageBucketAlias, String storageKey, Supplier<InputStream> streamSupplier)
{
   /*******************************************************************************
    ** Open the gateway-managed source stream.
    *******************************************************************************/
   public InputStream openStream()
   {
      return (streamSupplier.get());
   }
}
