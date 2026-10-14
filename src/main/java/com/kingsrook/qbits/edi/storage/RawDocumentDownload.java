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
import java.util.List;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Stream an authorized archived document, with a value-free access event per download.
 *******************************************************************************/
public final class RawDocumentDownload
{
   /*******************************************************************************
    ** Recheck permission and partner locks on every access, including registered URLs.
    *******************************************************************************/
   public static InputStream open(String reference) throws QException
   {
      String[] parts = reference.split(":");
      if(parts.length != 3 || !parts[0].equals("raw") || !List.of("edi_inbox", "edi_interchange").contains(parts[1])
         || QContext.getQSession() == null || !QContext.getQSession().hasPermission("edi.raw.download"))
      {
         throw new QException("Raw document download is not authorized");
      }
      Integer id;
      try
      {
         id = Integer.valueOf(parts[2]);
      }
      catch(NumberFormatException e)
      {
         throw new QException("Invalid raw document reference");
      }
      QueryInput query = new QueryInput(parts[1]).withFilter(EdiRecords.equal("id", id));
      query.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      List<QRecord> rows = new QueryAction().execute(query).getRecords();
      if(rows.size() != 1 || rows.getFirst().getValueString("payloadRef") == null)
      {
         throw new QException("Raw document is unavailable");
      }
      QRecord row = rows.getFirst();
      String user = QContext.getQSession().getUser() == null ? "system" : java.util.Objects.toString(QContext.getQSession().getUser().getIdReference(), "system");
      return (SystemWrites.call(() ->
      {
         SystemWrites.insert("edi_event", new QRecord().withValue("type", "RAW_DOWNLOADED").withValue("userName", user)
            .withValue("inboxId", parts[1].equals("edi_inbox") ? id : row.getValueInteger("inboxId"))
            .withValue("interchangeId", parts[1].equals("edi_interchange") ? id : null)
            .withValue("at", EdiRuntime.current().config().getClock().instant()).withValue("detail", "Raw document download authorized"));
         return (EdiRuntime.current().payloads().open(new PayloadRef(row.getValueString("payloadRef"), null, 0)));
      }));
   }
}
