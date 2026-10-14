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
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.PayloadRef;

/*******************************************************************************
 ** Streaming transport boundary, registered in the host instance's metadata.
 *******************************************************************************/
public interface EdiTransportPlugin
{
   /*******************************************************************************
    ** Stable transport type.
    *******************************************************************************/
   String getType();

   /*******************************************************************************
    ** Validate settings without resolving or exposing secrets.
    *******************************************************************************/
   void validate(EdiTransport transport, List<String> errors);

   /*******************************************************************************
    ** Send the already encrypted, durably queued payload.
    *******************************************************************************/
   EdiSendResult send(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport) throws Exception;

   /*******************************************************************************
    ** Pull transports acknowledge remote pickup only after intake commits.
    *******************************************************************************/
   default void poll(EdiTransport transport, InboundSink sink) throws Exception
   {
   }

   /*******************************************************************************
    ** Unsupported tests never claim a successful connection.
    *******************************************************************************/
   default EdiTestResult test(EdiTransport transport)
   {
      return (new EdiTestResult(false, "Connection test is unavailable for this transport"));
   }
}
