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

package com.kingsrook.qbits.edi.transport.gateway;

import java.io.InputStream;
import java.time.Duration;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.transport.gateway.standin.FileAcceptance;
import com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayConsumer;
import com.kingsrook.qbits.edi.transport.gateway.standin.InboundFile;

/*******************************************************************************
 ** Gateway adapter: copy into encrypted durable intake, then let the worker parse.
 *******************************************************************************/
public class EdiFileGatewayConsumer implements FileGatewayConsumer
{
   private final InboundSink sink;

   /*******************************************************************************
    ** Inject the durable receiving boundary.
    *******************************************************************************/
   public EdiFileGatewayConsumer(InboundSink sink)
   {
      this.sink = sink;
   }

   /*******************************************************************************
    ** Stable gateway consumer name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("edi");
   }

   /*******************************************************************************
    ** No parsing and no payload values in retry diagnostics.
    *******************************************************************************/
   @Override
   public FileAcceptance accept(InboundFile file)
   {
      try(InputStream stream = file.openStream())
      {
         var row = sink.receive(new InboundPayload("GATEWAY", file.uploadJobId() + ":" + file.sha256(), file.fileName(), file.remoteAddress(), stream, file.userId(), file.username()));
         return (FileAcceptance.accepted("edi_inbox:" + row.getValueInteger("id")));
      }
      catch(Exception e)
      {
         return (FileAcceptance.retryLater("EDI durable intake is unavailable", Duration.ofMinutes(1)));
      }
   }

   /*******************************************************************************
    ** Gateway download notification completes the durable outbound message.
    *******************************************************************************/
   @Override
   public void onDownloaded(com.kingsrook.qbits.edi.transport.gateway.standin.OutboxFileRef ref) throws Exception
   {
      com.kingsrook.qbits.edi.support.SystemWrites.call(() ->
      {
         for(var message : com.kingsrook.qbits.edi.storage.EdiRecords.query("edi_outbound_message",
            com.kingsrook.qbits.edi.storage.EdiRecords.equal("transportMessageId", String.valueOf(ref.outboxFileId()))))
         {
            var transport = com.kingsrook.qbits.edi.flow.OutboundWorker.row("edi_transport", message.getValue("transportId"));
            if(!"GATEWAY".equals(transport.getValueString("type")) || !ref.endpointName().equals(transport.getValueString("gatewayEndpointName")))
            {
               continue;
            }
            com.kingsrook.qbits.edi.support.SystemWrites.transaction(() ->
            {
               if(com.kingsrook.qbits.edi.support.SystemWrites.guard("UPDATE edi_outbound_message SET id=id WHERE id=? AND status='SENT'", message.getValue("id")))
               {
                  com.kingsrook.qbits.edi.support.SystemWrites.update("edi_outbound_message", new com.kingsrook.qqq.backend.core.model.data.QRecord()
                     .withValue("id", message.getValue("id")).withValue("status", "DELIVERED").withValue("deliveredAt", com.kingsrook.qbits.edi.EdiRuntime.current().config().getClock().instant()));
                  if(message.getValue("interchangeId") != null)
                  {
                     com.kingsrook.qbits.edi.support.SystemWrites.update("edi_interchange", new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("id", message.getValue("interchangeId")).withValue("status", "DELIVERED"));
                  }
                  com.kingsrook.qbits.edi.flow.EdiJournal.event(null, message.getValueInteger("interchangeId"), null, "DELIVERED");
               }
               return (null);
            });
         }
         return (null);
      });
   }
}
