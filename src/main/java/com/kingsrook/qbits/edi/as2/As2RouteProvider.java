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

package com.kingsrook.qbits.edi.as2;


import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;


/*******************************************************************************
 ** AS2 machine endpoints outside the UI API, authenticated per transport by AS2
 ** identity, S/MIME signature and certificates (never a user session):
 ** POST /qqq/hooks/edi/as2/{transport} receives messages and answers with the
 ** MDN, and POST /qqq/hooks/edi/as2/{transport}/mdn receives asynchronous MDNs.
 *******************************************************************************/
public class As2RouteProvider implements QJavalinRouteProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(As2RouteProvider.class);

   public static final long DEFAULT_MAX_BODY_BYTES = 50L * 1024 * 1024;
   public static final long ABSOLUTE_MAX_BODY_BYTES = 200L * 1024 * 1024;

   private QInstance instance;



   /*******************************************************************************
    ** Host instance supplied by Javalin.
    *******************************************************************************/
   public void setQInstance(QInstance instance)
   {
      this.instance = instance;
   }



   /*******************************************************************************
    ** Register the non-UI hooks.
    *******************************************************************************/
   public void acceptJavalinConfig(JavalinConfig config)
   {
      config.routes.post("/qqq/hooks/edi/as2/{transport}", context -> handle(context, false));
      config.routes.post("/qqq/hooks/edi/as2/{transport}/mdn", context -> handle(context, true));
   }



   /*******************************************************************************
    ** Find the active AS2 transport, read the bounded body and delegate.
    *******************************************************************************/
   private void handle(Context context, boolean mdn)
   {
      var previous = QContext.capture();
      try
      {
         QContext.init(instance, null);
         As2Receiver.Result result = SystemWrites.call(() -> process(context, mdn));
         context.status(result.status());
         result.headers().forEach(context::header);
         context.result(result.body());
      }
      catch(Exception e)
      {
         LOG.warn("AS2 hook failed: " + e.getClass().getSimpleName());
         context.status(503);
         context.result("The AS2 request could not be processed".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      finally
      {
         QContext.init(previous);
      }
   }



   /*******************************************************************************
    ** The hook body, inside the system context.
    *******************************************************************************/
   private As2Receiver.Result process(Context context, boolean mdn) throws Exception
   {
      List<QRecord> rows = EdiRecords.query("edi_transport", EdiRecords.equal("name", context.pathParam("transport")).withCriteria("type", QCriteriaOperator.EQUALS, "AS2")
         .withCriteria("isActive", QCriteriaOperator.EQUALS, true));
      if(rows.size() != 1 || (mdn ? "INBOUND" : "OUTBOUND").equals(rows.getFirst().getValueString("direction")))
      {
         return (As2Receiver.Result.text(404, "Unknown AS2 transport"));
      }
      EdiTransport transport = rows.getFirst().toEntity(EdiTransport.class);
      long limit = mdn ? As2Http.MAX_RESPONSE_BYTES : maxBodyBytes(transport);
      if(context.req().getContentLengthLong() > limit)
      {
         return (As2Receiver.Result.text(413, "The message is too large"));
      }
      byte[] body = context.req().getInputStream().readNBytes((int) limit + 1);
      if(body.length > limit)
      {
         return (As2Receiver.Result.text(413, "The message is too large"));
      }
      Map<String, String> headers = new TreeMap<>();
      context.headerMap().forEach((name, value) -> headers.put(name.toLowerCase(Locale.ROOT), value));
      return (mdn ? new As2MdnReceiver().receive(transport, headers, body) : new As2Receiver().receive(transport, headers, body, context.ip()));
   }



   /*******************************************************************************
    ** The largest interchange any agreement on this transport allows, or 50 MB.
    *******************************************************************************/
   private static long maxBodyBytes(EdiTransport transport) throws Exception
   {
      long limit = 0;
      for(QRecord agreement : EdiRecords.query("edi_agreement", EdiRecords.equal("inboundTransportId", transport.getId())))
      {
         limit = Math.max(limit, agreement.getValueLong("maxInterchangeBytes") == null ? 0 : agreement.getValueLong("maxInterchangeBytes"));
      }
      return (limit <= 0 ? DEFAULT_MAX_BODY_BYTES : Math.min(limit + limit / 2 + 65536, ABSOLUTE_MAX_BODY_BYTES));
   }
}
