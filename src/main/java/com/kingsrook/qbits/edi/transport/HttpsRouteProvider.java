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

import java.io.ByteArrayInputStream;
import java.util.UUID;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/*******************************************************************************
 ** Authenticated machine ingress outside the UI API; intake is durable before 202.
 *******************************************************************************/
public class HttpsRouteProvider implements QJavalinRouteProviderInterface
{
   private QInstance instance;
   public static final int MAX_BODY_BYTES = 50 * 1024 * 1024;

   /*******************************************************************************
    ** Host instance supplied by Javalin.
    *******************************************************************************/
   public void setQInstance(QInstance instance)
   {
      this.instance = instance;
   }

   /*******************************************************************************
    ** Register the non-UI hook.
    *******************************************************************************/
   public void acceptJavalinConfig(JavalinConfig config)
   {
      config.routes.post("/qqq/hooks/edi/https/{transport}", this::receive);
   }

   /*******************************************************************************
    ** Read at most limit plus one byte, authenticate exact bytes, then commit intake.
    *******************************************************************************/
   private void receive(Context context)
   {
      var previous = QContext.capture();
      try
      {
         QContext.init(instance, null);
         var rows = SystemWrites.call(() -> EdiRecords.query("edi_transport", EdiRecords.equal("name", context.pathParam("transport"))
            .withCriteria("type", QCriteriaOperator.EQUALS, "HTTPS").withCriteria("isActive", QCriteriaOperator.EQUALS, true)));
         if(rows.size() != 1 || "OUTBOUND".equals(rows.getFirst().getValueString("direction")))
         {
            context.status(404);
            return;
         }
         if(context.req().getContentLengthLong() > MAX_BODY_BYTES)
         {
            context.status(413);
            return;
         }
         byte[] bytes = context.req().getInputStream().readNBytes(MAX_BODY_BYTES + 1);
         if(bytes.length > MAX_BODY_BYTES)
         {
            context.status(413);
            return;
         }
         EdiTransport transport = rows.getFirst().toEntity(EdiTransport.class);
         if(!HttpsTransport.authenticate(transport, context.headerMap(), bytes))
         {
            context.status(401);
            return;
         }
         String id = "HMAC".equals(transport.getHttpsInboundAuth()) ? context.header("webhook-id") : UUID.randomUUID().toString();
         new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("HTTPS", transport.getId() + ":" + id, null, context.ip(), new ByteArrayInputStream(bytes)));
         context.status(202);
      }
      catch(Exception e)
      {
         context.status(503);
      }
      finally
      {
         QContext.init(previous);
      }
   }
}
