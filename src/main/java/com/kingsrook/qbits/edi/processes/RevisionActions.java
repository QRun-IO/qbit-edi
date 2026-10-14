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

package com.kingsrook.qbits.edi.processes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.edi.guide.GuideRepository;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Version activation and partner override copies retain historical definitions.
 *******************************************************************************/
public class RevisionActions
{
   /*******************************************************************************
    ** Validate before activating and retire the previous active revision.
    *******************************************************************************/
   public void execute(String table, QRecord row, String operation, Integer partner) throws Exception
   {
      if("BUILT_IN".equals(row.getValueString("source")) && !operation.equals("override"))
      {
         throw new IllegalArgumentException("Built-in guides are immutable; create an override");
      }
      if(operation.equals("override"))
      {
         if(partner == null)
         {
            throw new IllegalArgumentException("Partner is required");
         }
         QRecord copy = copy(row).withValue("name", row.getValueString("name") + " partner " + partner).withValue("partnerId", partner)
            .withValue("parentGuideId", row.getValue("id")).withValue("status", "DRAFT").withValue("source", "CUSTOM").withValue("revision", 0);
         Integer id = SystemWrites.insert("edi_guide", copy).getValueInteger("id");
         Map<Integer, Integer> segments = new HashMap<>();
         for(QRecord segment : EdiRecords.query("edi_guide_segment", EdiRecords.equal("guideId", row.getValue("id"))))
         {
            Integer newId = SystemWrites.insert("edi_guide_segment", copy(segment).withValue("guideId", id)).getValueInteger("id");
            segments.put(segment.getValueInteger("id"), newId);
            for(QRecord element : EdiRecords.query("edi_guide_element", EdiRecords.equal("guideSegmentId", segment.getValue("id"))))
            {
               SystemWrites.insert("edi_guide_element", copy(element).withValue("guideSegmentId", newId));
            }
         }
         Map<Integer, Integer> ruleIds = new HashMap<>();
         for(QRecord rule : EdiRecords.query("edi_guide_rule", EdiRecords.equal("guideId", row.getValue("id"))))
         {
            Integer ruleId = SystemWrites.insert("edi_guide_rule", copy(rule).withValue("guideId", id).withValue("guideSegmentId", segments.get(rule.getValueInteger("guideSegmentId")))).getValueInteger("id");
            ruleIds.put(rule.getValueInteger("id"), ruleId);
         }
         for(QRecord segment : EdiRecords.query("edi_guide_segment", EdiRecords.equal("guideId", id)))
         {
            SystemWrites.update("edi_guide_segment", new QRecord().withValue("id", segment.getValue("id")).withValue("situationalRuleId", ruleIds.get(segment.getValueInteger("situationalRuleId"))));
            for(QRecord element : EdiRecords.query("edi_guide_element", EdiRecords.equal("guideSegmentId", segment.getValue("id"))))
            {
               SystemWrites.update("edi_guide_element", new QRecord().withValue("id", element.getValue("id")).withValue("situationalRuleId", ruleIds.get(element.getValueInteger("situationalRuleId"))));
            }
         }
         return;
      }
      if(!"DRAFT".equals(row.getValueString("status")))
      {
         throw new IllegalArgumentException("Only draft revisions can be activated");
      }
      if(table.equals("edi_guide"))
      {
         new GuideRepository().load(row.getValueInteger("id"));
      }
      else
      {
         var target = QContext.getQInstance().getTable(row.getValueString("tableName"));
         if(target == null)
         {
            throw new IllegalArgumentException("Map target table does not exist");
         }
         for(QRecord rule : EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", row.getValue("id"))))
         {
            String qqqPath = rule.getValueString("qqqPath");
            if((qqqPath == null || qqqPath.isBlank()) && "OUTBOUND".equals(row.getValueString("direction"))
               && List.of("CONSTANT", "SEQUENCE", "HL").contains(rule.getValueString("transform")))
            {
               continue;
            }
            String[] path = Objects.requireNonNull(qqqPath, "Map field path is required").split("\\.");
            var destination = target;
            for(int i = 0; i < path.length - 1; i++)
            {
               destination = QContext.getQInstance().getTable(destination.getAssociationByName(path[i]).orElseThrow().getAssociatedTableName());
            }
            if(!"OUTBOUND".equals(row.getValueString("direction")) && path.length > 2 || destination.getField(path[path.length - 1]) == null)
            {
               throw new IllegalArgumentException("Invalid map target path");
            }
         }
      }
      int revision = 0;
      for(QRecord previous : EdiRecords.query(table, EdiRecords.equal("name", row.getValue("name"))))
      {
         if(Objects.equals(previous.getValue("partnerId"), row.getValue("partnerId")))
         {
            revision = Math.max(revision, Objects.requireNonNullElse(previous.getValueInteger("revision"), 0));
            if("ACTIVE".equals(previous.getValueString("status")))
            {
               SystemWrites.update(table, new QRecord().withValue("id", previous.getValue("id")).withValue("status", "RETIRED"));
            }
         }
      }
      SystemWrites.update(table, new QRecord().withValue("id", row.getValue("id")).withValue("revision", revision + 1).withValue("status", "ACTIVE"));
   }

   /*******************************************************************************
    ** Copy values without identity and audit timestamps.
    *******************************************************************************/
   private static QRecord copy(QRecord row)
   {
      QRecord copy = new QRecord();
      copy.setValues(new HashMap<>(row.getValues()));
      for(String key : List.of("id", "createDate", "modifyDate"))
      {
         copy.getValues().remove(key);
      }
      return (copy);
   }
}
