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
import java.io.OutputStream;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;

/*******************************************************************************
 ** QQQ storage bridge for session-authorized encrypted synthetic uploads and previews.
 ** This has no filesystem implementation and stores nothing outside PayloadStore.
 *******************************************************************************/
public class PreviewDownloadBackend implements QBackendModuleInterface, QStorageInterface
{
   public static final String TABLE = "edi_preview_download";

   static
   {
      com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher.registerBackendModule(new PreviewDownloadBackend());
   }

   /*******************************************************************************
    ** Dedicated storage-only backend; operational tables retain the host backend.
    *******************************************************************************/
   @Override
   public String getBackendType()
   {
      return ("edi-preview-download");
   }

   /*******************************************************************************
    ** Expose standard storage streams without filesystem staging.
    *******************************************************************************/
   @Override
   public QStorageInterface getStorageInterface()
   {
      return (this);
   }

   /*******************************************************************************
    ** Only a completed preview may grant its exact reference to this viewing session.
    *******************************************************************************/
   @Override
   public InputStream getInputStream(StorageInput input) throws QException
   {
      if(!TABLE.equals(input.getTableName()) || !ProcessFileDownload.isStorageRegistered(input))
      {
         throw new QException("Preview download is not authorized for this session");
      }
      return (SystemWrites.call(() -> EdiRuntime.current().payloads().open(new PayloadRef(input.getReference(), null, 0))));
   }

   /*******************************************************************************
    ** The host may use this alias as its process upload archive for synthetic samples.
    ** Rebind RC.5's mutable StorageInput after close to the encrypted payload reference.
    *******************************************************************************/
   @Override
   public OutputStream createOutputStream(StorageInput input) throws QException
   {
      var session = com.kingsrook.qqq.backend.core.context.QContext.getQSession();
      if(!TABLE.equals(input.getTableName()) || session == null || !session.hasPermission("edi.map.preview"))
      {
         throw new QException("Synthetic sample upload is not authorized");
      }
      return (new java.io.ByteArrayOutputStream()
      {
         private boolean closed;

         /*******************************************************************************
          ** Persist once, then authorize the resulting reference for this session.
          *******************************************************************************/
         @Override
         public void close() throws java.io.IOException
         {
            if(closed)
            {
               return;
            }
            closed = true;
            try
            {
               PayloadRef ref = SystemWrites.call(() -> EdiRuntime.current().payloads().put(PayloadKind.SAMPLE, new java.io.ByteArrayInputStream(toByteArray())));
               input.setReference(ref.value());
               ProcessFileDownload.registerStorage(input);
            }
            catch(Exception e)
            {
               throw new java.io.IOException("Could not store encrypted synthetic sample");
            }
            finally
            {
               reset();
               super.close();
            }
         }
      });
   }
}
