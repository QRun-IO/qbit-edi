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

package com.kingsrook.qbits.edi.transport;

import java.util.List;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.PayloadRef;

/*******************************************************************************
 ** Explicit phase boundaries for AS2 and the unpublished email API.
 *******************************************************************************/
public record UnavailableTransport(String type) implements EdiTransportPlugin
{
   /*******************************************************************************
    ** Configured disabled transport type.
    *******************************************************************************/
   public String getType()
   {
      return (type);
   }

   /*******************************************************************************
    ** Keep future configuration editable while checking its required destination.
    *******************************************************************************/
   public void validate(EdiTransport transport, List<String> errors)
   {
      if("AS2".equals(type) ? transport.getAs2Url() == null : transport.getEmailTo() == null)
      {
         errors.add("Transport destination is required");
      }
   }

   /*******************************************************************************
    ** Permanent failure; never report these transports as operational.
    *******************************************************************************/
   public EdiSendResult send(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport)
   {
      return (new EdiSendResult("FAILED", null, "Not available until phase c / qbit-email API"));
   }
}
