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
import java.util.List;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.transport.EdiSendResult;
import com.kingsrook.qbits.edi.transport.EdiTestResult;
import com.kingsrook.qbits.edi.transport.EdiTransportPlugin;
import com.kingsrook.qbits.edi.transport.gateway.standin.FileGatewayOutbox;

/*******************************************************************************
 ** Partner-pull gateway delivery: put is SENT; download notification is DELIVERED.
 *******************************************************************************/
public class GatewayTransport implements EdiTransportPlugin
{
   /*******************************************************************************
    ** Registry key.
    *******************************************************************************/
   public String getType()
   {
      return ("GATEWAY");
   }

   /*******************************************************************************
    ** An endpoint is required for both directions.
    *******************************************************************************/
   public void validate(EdiTransport transport, List<String> errors)
   {
      if(transport.getGatewayEndpointName() == null || transport.getGatewayEndpointName().isBlank())
      {
         errors.add("gatewayEndpointName is required");
      }
   }

   /*******************************************************************************
    ** Stable file names let the gateway make retries idempotent.
    *******************************************************************************/
   public EdiSendResult send(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport) throws Exception
   {
      try(InputStream stream = EdiRuntime.current().payloads().open(payload))
      {
         var ref = FileGatewayOutbox.put(transport.getGatewayEndpointName(), "edi-" + message.getId() + ".x12", stream, payload.sizeBytes());
         return (new EdiSendResult("SENT", String.valueOf(ref.outboxFileId()), null));
      }
   }

   /*******************************************************************************
    ** The facade's availability is the gateway connection boundary.
    *******************************************************************************/
   public EdiTestResult test(EdiTransport transport)
   {
      return (new EdiTestResult(FileGatewayOutbox.isAvailable(), FileGatewayOutbox.isAvailable() ? "Gateway provider available" : "Gateway provider unavailable"));
   }
}
