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

package com.kingsrook.qbits.edi.guide;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadCrypto;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Checksum-based installation of original baseline guide resources.
 *******************************************************************************/
public class BuiltInGuideLoader
{
   public static final List<String> RESOURCES = List.of("850-004010", "850-005010", "997-004010", "997-005010", "999-005010X231A1", "855-004010", "855-005010", "810-004010", "810-005010", "846-004010", "846-005010", "860-004010", "860-005010", "856-004010", "856-005010", "940-004010", "940-005010", "945-004010", "945-005010", "214-004010", "214-005010", "204-004010", "204-005010", "ORDERS-D96A", "ORDRSP-D96A", "DESADV-D96A", "INVOIC-D96A", "CONTRL-D96A", "ORDERS-D01B", "ORDRSP-D01B", "DESADV-D01B", "INVOIC-D01B", "CONTRL-D01B", "837P-005010X222A1", "837I-005010X223A2", "835-005010X221A1", "270-005010X279A1", "271-005010X279A1", "276-005010X212", "277-005010X212", "834-005010X220A1");

   /*******************************************************************************
    ** Read the documented JSON format in memory.
    *******************************************************************************/
   public Guide read(InputStream input) throws IOException
   {
      return (new ObjectMapper().readValue(input, Guide.class));
   }

   /*******************************************************************************
    ** Read one bundled baseline.
    *******************************************************************************/
   public Guide resource(String name) throws IOException
   {
      try(InputStream input = BuiltInGuideLoader.class.getResourceAsStream("/edi/guides/" + name + ".json"))
      {
         if(input == null)
         {
            throw new IOException("Unknown built-in guide");
         }
         return (read(input));
      }
   }

   /*******************************************************************************
    ** Install changed guides after the host schema and service session are ready.
    *******************************************************************************/
   public List<Integer> load() throws Exception
   {
      List<Integer> ids = new ArrayList<>();
      for(String resource : RESOURCES)
      {
         try(InputStream input = BuiltInGuideLoader.class.getResourceAsStream("/edi/guides/" + resource + ".json"))
         {
            ids.add(upsert(input.readAllBytes()));
         }
      }
      return (ids);
   }

   /*******************************************************************************
    ** Retain previous revisions; identical content is a no-op.
    *******************************************************************************/
   public Integer upsert(byte[] json) throws Exception
   {
      Guide guide = read(new ByteArrayInputStream(json));
      String checksum = PayloadCrypto.digest(json);
      List<QRecord> revisions = EdiRecords.query("edi_guide", EdiRecords.equal("name", guide.name()));
      int revision = 0;
      QRecord latest = null;
      for(QRecord row : revisions)
      {
         if("BUILT_IN".equals(row.getValueString("source")) && (latest == null || row.getValueInteger("revision") > latest.getValueInteger("revision")))
         {
            latest = row;
         }
         revision = Math.max(revision, row.getValueInteger("revision"));
      }
      if(latest != null && checksum.equals(latest.getValueString("compiledChecksum")))
      {
         return (latest.getValueInteger("id"));
      }
      return (new GuideRepository().save(guide, checksum, revision + 1));
   }
}
