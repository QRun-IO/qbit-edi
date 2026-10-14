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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.guide.Guide;
import com.kingsrook.qbits.edi.guide.GuideSegment;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Static validation of unsaved rules; never runs transforms or writes records.
 *******************************************************************************/
public class MapRuleValidator
{
   private static final Pattern ELEMENT = Pattern.compile("([A-Z][A-Z0-9]{1,2}?)([0-9]{2})(?:-([0-9]+))?");
   private static final Pattern STEP = Pattern.compile("([^\\[]+)(?:\\[(\\*|[1-9][0-9]*|[^\\[\\]]+=[^\\[\\]]+)])?");

   /*******************************************************************************
    ** Report independent errors without echoing rule literals or default values.
    *******************************************************************************/
   public List<String> validate(QRecord rule, QRecord map, Guide guide, TransformRegistry registry)
   {
      List<String> errors = new ArrayList<>();
      if(rule.getValueInteger("sequence") == null || rule.getValueInteger("sequence") < 1)
      {
         errors.add("sequence must be a positive integer");
      }
      boolean outbound = "OUTBOUND".equals(map.getValueString("direction"));
      String ediPath = rule.getValueString("ediPath");
      String transform = Objects.requireNonNullElse(rule.getValueString("transform"), "").toUpperCase(Locale.ROOT);
      if(ediPath == null || ediPath.isBlank())
      {
         if(outbound || !transform.equals("CONSTANT") && rule.getValueString("defaultValue") == null)
         {
            errors.add("ediPath is required");
         }
      }
      else if(!resolves(ediPath, guide))
      {
         errors.add("ediPath does not resolve in the guide");
      }
      String target = rule.getValueString("qqqPath");
      QTableMetaData table = QContext.getQInstance().getTable(map.getValueString("tableName"));
      String[] fields = target == null ? new String[0] : target.split("\\.", -1);
      for(int i = 0; table != null && i < fields.length - 1; i++)
      {
         var associations = table.getAssociations();
         String name = fields[i];
         var association = associations == null ? null : associations.stream().filter(a -> a.getName().equals(name)).findFirst().orElse(null);
         table = association == null ? null : QContext.getQInstance().getTable(association.getAssociatedTableName());
      }
      boolean generated = outbound && (target == null || target.isBlank()) && List.of("CONSTANT", "SEQUENCE", "HL").contains(transform);
      if(!generated && (table == null || fields.length == 0 || !table.getFields().containsKey(fields[fields.length - 1])
         || !outbound && fields.length > 2))
      {
         errors.add("qqqPath does not resolve in the target table");
      }
      if(fields.length > 1 && !"OUTBOUND".equals(map.getValueString("direction")) && (ediPath == null || !ediPath.contains("[*]/")))
      {
         errors.add("An inbound association requires a wildcard loop");
      }
      Map<String, Object> schema = registry.argumentSchemas().get(transform);
      if(!transform.isEmpty() && schema == null)
      {
         errors.add("Unknown transform");
      }
      try
      {
         String raw = rule.getValueString("transformArgs");
         var args = new ObjectMapper().readTree(raw == null || raw.isBlank() ? "{}" : raw);
         if(!args.isObject() || java.util.stream.StreamSupport.stream(java.util.Spliterators.spliteratorUnknownSize(args.elements(), 0), false).anyMatch(v -> !v.isTextual()))
         {
            errors.add("transformArgs must be a JSON object of strings");
         }
         else if(schema != null && schema.get("required") instanceof List<?> required)
         {
            for(Object key : required)
            {
               if(!args.hasNonNull(key.toString()))
               {
                  errors.add("Missing required transform argument: " + key);
               }
            }
         }
      }
      catch(Exception e)
      {
         errors.add("transformArgs must be a JSON object of strings");
      }
      return (errors);
   }

   /*******************************************************************************
    ** Resolve every joined element path, including selectors and composite positions.
    *******************************************************************************/
   private boolean resolves(String path, Guide guide)
   {
      for(String part : path.split("\\+(?![^\\[]*])", -1))
      {
         List<GuideSegment> scope = guide.segments();
         String[] steps = part.trim().split("/", -1);
         for(int index = 0; index < steps.length; index++)
         {
            var step = STEP.matcher(steps[index]);
            if(!step.matches())
            {
               return (false);
            }
            String name = step.group(1);
            String qualifier = step.group(2);
            var element = ELEMENT.matcher(name);
            if(index == steps.length - 1)
            {
               if(!element.matches() || !element(scope, name))
               {
                  return (false);
               }
               scope = scope.stream().filter(s -> s.segmentId().equals(element.group(1))).toList();
            }
            else
            {
               List<GuideSegment> current = scope;
               scope = current.stream().filter(s -> s.segmentId().equals(name) || inLoop(s, name, guide, 0)).toList();
               if(scope.isEmpty())
               {
                  return (false);
               }
            }
            if(qualifier != null && qualifier.contains("=") && !element(scope, qualifier.substring(0, qualifier.indexOf('='))))
            {
               return (false);
            }
         }
      }
      return (true);
   }

   /*******************************************************************************
    ** Ancestor scope matters when different loops contain the same segment.
    *******************************************************************************/
   private boolean inLoop(GuideSegment segment, String loop, Guide guide, int depth)
   {
      if(loop.equals(segment.loopId()))
      {
         return (true);
      }
      if(segment.parentLoopId() == null || depth > guide.segments().size())
      {
         return (false);
      }
      return (loop.equals(segment.parentLoopId()) || guide.segments().stream().filter(s -> segment.parentLoopId().equals(s.loopId()))
         .anyMatch(parent -> inLoop(parent, loop, guide, depth + 1)));
   }

   /*******************************************************************************
    ** Match an element and its optional composite component.
    *******************************************************************************/
   private boolean element(List<GuideSegment> segments, String path)
   {
      var match = ELEMENT.matcher(path);
      if(!match.matches())
      {
         return (false);
      }
      int position = Integer.parseInt(match.group(2));
      Integer component;
      try
      {
         component = match.group(3) == null ? null : Integer.valueOf(match.group(3));
      }
      catch(NumberFormatException e)
      {
         return (false);
      }
      return (segments.stream().filter(s -> s.segmentId().equals(match.group(1))).flatMap(s -> s.elements().stream())
         .anyMatch(e -> e.position() == position && (component == null || component.equals(e.componentPosition()))));
   }
}
