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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.guide.RuleEvaluator;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Builds ordered segments from record associations. Loop paths define hierarchy;
 ** rules within a loop share its association occurrence and sequence number.
 *******************************************************************************/
public class OutboundMapper
{
   private static final Pattern ELEMENT = Pattern.compile("([A-Z][A-Z0-9]{1,2})([0-9]{2})(?:-([0-9]+))?");

   /*******************************************************************************
    ** Load a persisted outbound map's ordered rules.
    *******************************************************************************/
   public X12TransactionSet build(QRecord source, QRecord map) throws Exception
   {
      return (build(source, map, EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id"))), "0001"));
   }

   /*******************************************************************************
    ** Pure mapping entry point; the envelope owns transaction control allocation.
    *******************************************************************************/
   public X12TransactionSet build(QRecord source, QRecord map, List<QRecord> rules, String control) throws Exception
   {
      if(!"OUTBOUND".equals(map.getValueString("direction")))
      {
         throw new IllegalArgumentException("An outbound map is required");
      }
      List<X12Segment> segments = new ArrayList<>();
      String version = map.getValueString("version");
      segments.add(version != null && version.startsWith("005010X")
         ? X12Segment.of("ST", 1, map.getValueString("transactionSet"), control, version)
         : X12Segment.of("ST", 1, map.getValueString("transactionSet"), control));
      List<QRecord> sorted = rules.stream().sorted(Comparator.comparing(r -> r.getValueInteger("sequence"))).toList();
      emit(source, sorted, "", "", 1, "", new int[] {0}, segments);
      segments.add(X12Segment.of("SE", segments.size() + 1, String.valueOf(segments.size() + 1), control));
      return (new X12TransactionSet(segments.getFirst(), segments.getLast(), List.copyOf(segments), List.of()));
   }

   /*******************************************************************************
    ** Walk each loop once per associated record, retaining the nearest HL parent.
    *******************************************************************************/
   protected void emit(QRecord source, List<QRecord> rules, String prefix, String qPrefix, int sequence, String parent,
      int[] hierarchy, List<X12Segment> output) throws Exception
   {
      Map<String, List<QRecord>> nodes = new LinkedHashMap<>();
      for(QRecord rule : rules)
      {
         String path = rule.getValueString("ediPath").substring(prefix.length());
         String key = path.contains("/") ? path.substring(0, path.indexOf('/') + 1) : path.replaceAll("[0-9]{2}(-[0-9]+)?$", "");
         nodes.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rule);
      }
      String currentParent = parent;
      for(var node : nodes.entrySet())
      {
         if(node.getKey().endsWith("/"))
         {
            String association = null;
            for(QRecord rule : node.getValue())
            {
               if(rule.getValueString("ediPath").substring((prefix + node.getKey()).length()).contains("/"))
               {
                  continue;
               }
               String path = rule.getValueString("qqqPath");
               if(path != null && path.startsWith(qPrefix) && path.substring(qPrefix.length()).contains("."))
               {
                  association = path.substring(qPrefix.length()).split("\\.")[0];
                  break;
               }
            }
            List<QRecord> children = association == null ? List.of(source) : Objects.requireNonNullElse(source.getAssociatedRecords() == null ? null : source.getAssociatedRecords().get(association), List.of());
            int index = 0;
            for(QRecord child : children)
            {
               emit(child, node.getValue(), prefix + node.getKey(), association == null ? qPrefix : qPrefix + association + ".", ++index, currentParent, hierarchy, output);
            }
            continue;
         }
         List<List<String>> elements = new ArrayList<>();
         String segmentId = null;
         for(QRecord rule : node.getValue())
         {
            String path = rule.getValueString("ediPath");
            var match = ELEMENT.matcher(path.substring(path.lastIndexOf('/') + 1));
            if(!match.matches())
            {
               throw new IllegalArgumentException("Unsupported outbound element path");
            }
            segmentId = match.group(1);
            int position = Integer.parseInt(match.group(2));
            if(position < 1 || position > 99)
            {
               throw new IllegalArgumentException("Invalid outbound element position");
            }
            while(elements.size() < position)
            {
               elements.add(new ArrayList<>());
            }
            String condition = rule.getValueString("condition");
            if(condition != null && !condition.isBlank() && !new RuleEvaluator(output, Map.of()).test(condition))
            {
               continue;
            }
            String qPath = rule.getValueString("qqqPath");
            Object raw = qPath == null ? null : source.getValue(qPath.startsWith(qPrefix) ? qPath.substring(qPrefix.length()) : qPath);
            String transform = Objects.requireNonNullElse(rule.getValueString("transform"), "");
            String argsJson = rule.getValueString("transformArgs");
            Map<String, String> args = argsJson == null ? Map.of() : new ObjectMapper().readValue(argsJson, new TypeReference<Map<String, String>>() {});
            String value;
            if(transform.equals("HL"))
            {
               value = switch(position)
               {
                  case 1 -> String.valueOf(++hierarchy[0]);
                  case 2 -> parent;
                  case 3 -> args.get("code");
                  case 4 -> source.getAssociatedRecords() != null && source.getAssociatedRecords().values().stream().anyMatch(v -> !v.isEmpty()) ? "1" : "0";
                  default -> throw new IllegalArgumentException("HL supports positions 1 through 4");
               };
            }
            else if(transform.equals("SEQUENCE"))
            {
               value = String.valueOf(sequence + Integer.parseInt(args.getOrDefault("start", "1")) - 1);
            }
            else if(raw == null && !transform.equals("CONSTANT"))
            {
               value = rule.getValueString("defaultValue");
            }
            else if(transform.equals("DATE"))
            {
               value = java.time.LocalDate.parse(raw.toString()).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            }
            else if(transform.equals("TIME"))
            {
               value = java.time.LocalTime.parse(raw.toString()).format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"));
            }
            else if(transform.equals("DECIMAL"))
            {
               value = decimal(new java.math.BigDecimal(raw.toString()).movePointRight(Integer.parseInt(args.getOrDefault("scale", "0"))).stripTrailingZeros().toPlainString());
            }
            else
            {
               Object transformed = EdiRuntime.current().transforms().apply(transform, argsJson, raw == null ? List.of() : List.of(raw.toString()));
               value = transformed == null ? rule.getValueString("defaultValue") : transformed.toString();
            }
            if((value == null || value.isBlank()) && Boolean.TRUE.equals(rule.getValueBoolean("required")))
            {
               throw new IllegalArgumentException("Required outbound field is missing");
            }
            int component = match.group(3) == null ? 1 : Integer.parseInt(match.group(3));
            if(component < 1 || component > 99)
            {
               throw new IllegalArgumentException("Invalid outbound component position");
            }
            var components = elements.get(position - 1);
            while(components.size() < component)
            {
               components.add("");
            }
            components.set(component - 1, Objects.requireNonNullElse(value, ""));
         }
         if(elements.stream().flatMap(List::stream).anyMatch(v -> !v.isEmpty()))
         {
            output.add(new X12Segment(segmentId, output.size() + 1, elements.stream().map(e -> List.of(e.isEmpty() ? List.of("") : e)).toList()));
            if("HL".equals(segmentId))
            {
               currentParent = elements.getFirst().getFirst();
            }
         }
      }
   }

   /*******************************************************************************
    ** X12 uses a period; EDIFACT can override the numeric service character.
    *******************************************************************************/
   protected String decimal(String value)
   {
      return (value);
   }

}
