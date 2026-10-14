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


import java.io.InputStream;


/*******************************************************************************
 ** Payload storage contract; implementations must never write local files.
 *******************************************************************************/
public interface PayloadStore
{
   /*******************************************************************************
    ** Encrypt and store input without taking ownership of its stream.
    *******************************************************************************/
   PayloadRef put(PayloadKind kind, InputStream input) throws Exception;

   /*******************************************************************************
    ** Open decrypted content; callers own the returned stream.
    *******************************************************************************/
   InputStream open(PayloadRef ref) throws Exception;

   /*******************************************************************************
    ** Delete stored content.
    *******************************************************************************/
   void delete(PayloadRef ref) throws Exception;
}
