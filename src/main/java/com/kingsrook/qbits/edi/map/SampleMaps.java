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

package com.kingsrook.qbits.edi.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** The bundled sample maps (synthetic, one per built-in document and version)
 ** and their insert-only installation. Installed maps are drafts, bound to no
 ** partner and to no agreement document, so installing one never changes how
 ** any document is processed; the target table in each is an example name the
 ** host replaces before activating.
 *******************************************************************************/
public final class SampleMaps
{
   public static final List<String> RESOURCES = List.of("204-004010", "204-005010", "204-out-004010", "204-out-005010", "214-004010", "214-005010",
      "810-004010", "810-005010", "846-004010", "846-005010", "850-004010", "850-005010", "855-004010", "855-005010", "856-004010", "856-005010",
      "860-004010", "860-005010", "940-004010", "940-005010", "945-004010", "945-005010", "945-in-004010", "945-in-005010",
      "DESADV-D01B", "DESADV-D96A", "INVOIC-D01B", "INVOIC-D96A", "ORDERS-D01B", "ORDERS-D96A", "ORDRSP-D01B", "ORDRSP-D96A");

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private SampleMaps()
   {
   }

   /*******************************************************************************
    ** One bundled sample: the map's values and its rules.
    *******************************************************************************/
   public static JsonNode read(String resource) throws IOException
   {
      try(InputStream input = SampleMaps.class.getResourceAsStream("/edi/maps/" + resource + "-sample.json"))
      {
         if(input == null)
         {
            throw (new IOException("Unknown sample map " + resource));
         }
         return (new ObjectMapper().readTree(input));
      }
   }

   /*******************************************************************************
    ** The bundled samples whose map name does not exist yet (any revision, any
    ** partner), in bundle order. Reads the map table as the processing user.
    *******************************************************************************/
   public static List<String> missing() throws QException, IOException
   {
      Set<String> existing = new HashSet<>();
      for(QRecord map : SystemWrites.query("edi_map", new QQueryFilter()))
      {
         existing.add(map.getValueString("name"));
      }
      List<String> missing = new ArrayList<>();
      for(String resource : RESOURCES)
      {
         if(!existing.contains(read(resource).path("map").path("name").asText()))
         {
            missing.add(resource);
         }
      }
      return (missing);
   }

   /*******************************************************************************
    ** The map record a sample installs as: a draft, revision 0, no partner,
    ** bound to the built-in guide for its document and version when present.
    *******************************************************************************/
   public static QRecord mapRecord(String resource) throws IOException, QException
   {
      JsonNode map    = read(resource).path("map");
      QRecord  record = new QRecord();
      map.fields().forEachRemaining(e ->
      {
         if(!e.getValue().isNull() && !List.of("status", "revision", "partnerId", "guideId", "parentMapId").contains(e.getKey()))
         {
            record.withValue(e.getKey(), e.getValue().asText());
         }
      });
      record.withValue("status", "DRAFT").withValue("revision", 0);
      Integer guideId  = null;
      int     revision = -1;
      for(QRecord guide : SystemWrites.query("edi_guide", new QQueryFilter().withCriteria("transactionSet", QCriteriaOperator.EQUALS, map.path("transactionSet").asText())
         .withCriteria("version", QCriteriaOperator.EQUALS, map.path("version").asText()).withCriteria("source", QCriteriaOperator.EQUALS, "BUILT_IN")
         .withCriteria("status", QCriteriaOperator.EQUALS, "ACTIVE")))
      {
         int candidate = guide.getValueInteger("revision") == null ? 0 : guide.getValueInteger("revision");
         if(candidate > revision)
         {
            revision = candidate;
            guideId = guide.getValueInteger("id");
         }
      }
      return (record.withValue("guideId", guideId));
   }

   /*******************************************************************************
    ** Insert one sample and its ordered rules. Skips (returns null) a sample
    ** whose map name now exists, so a repeated run adds nothing. It is an
    ** administrator action; two people running it at the same moment is not
    ** guarded against (edi_map has no unique name, since revisions share one).
    *******************************************************************************/
   public static Integer install(String resource) throws Exception
   {
      JsonNode sample = read(resource);
      String   name   = sample.path("map").path("name").asText();
      if(!EdiRecords.query("edi_map", EdiRecords.equal("name", name)).isEmpty())
      {
         return (null);
      }
      QRecord map = SystemWrites.insert("edi_map", mapRecord(resource));
      for(JsonNode rule : sample.path("rules"))
      {
         QRecord row = new QRecord();
         rule.fields().forEachRemaining(e ->
         {
            if(!e.getValue().isNull())
            {
               row.withValue(e.getKey(), e.getValue().isInt() ? (java.io.Serializable) e.getValue().intValue() : e.getValue().isBoolean() ? (java.io.Serializable) e.getValue().booleanValue() : e.getValue().asText());
            }
         });
         SystemWrites.insert("edi_map_rule", row.withValue("mapId", map.getValue("id")));
      }
      return (map.getValueInteger("id"));
   }
}
