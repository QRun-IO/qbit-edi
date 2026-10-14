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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.document.EdiDocument;
import com.kingsrook.qbits.edi.guide.Guide;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.guide.RuleEvaluator;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Inbound mapping preserving loop occurrences, including missing optional values.
 *******************************************************************************/
public class InboundMapper
{
   private final TransformRegistry transforms;

   /*******************************************************************************
    ** Use the host's transform registry.
    *******************************************************************************/
   public InboundMapper(TransformRegistry transforms)
   {
      this.transforms = transforms;
   }

   /*******************************************************************************
    ** Mapped records and extracted business identifiers; never log this object.
    *******************************************************************************/
   public record Result(QRecord record, String businessKey, String secondaryKey)
   {
   }

   /*******************************************************************************
    ** Load ordered rules for a persisted map.
    *******************************************************************************/
   public Result map(EdiDocument transaction, Guide guide, QRecord map) throws Exception
   {
      List<QRecord> rules = new ArrayList<>(EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id"))));
      rules.sort(Comparator.comparing(r -> r.getValueInteger("sequence")));
      return (map(transaction, guide, map, rules));
   }

   /*******************************************************************************
    ** Map in memory; wildcard rules sharing an association use the same loop scope.
    *******************************************************************************/
   public Result map(EdiDocument transaction, Guide guide, QRecord map, List<QRecord> rules) throws Exception
   {
      Map<String, List<List<X12Segment>>> loops = new GuideValidator().loops(transaction, guide);
      RuleEvaluator evaluator = new RuleEvaluator(transaction.segments(), loops);
      QRecord result = new QRecord();
      for(QRecord rule : rules)
      {
         try
         {
            String path = rule.getValueString("ediPath");
            String target = rule.getValueString("qqqPath");
            String[] fields = target.split("\\.");
            if(fields.length > 2 || fields.length == 0)
            {
               throw new IllegalArgumentException("Invalid target path");
            }
            if(fields.length == 1)
            {
               apply(rule, evaluator, result, fields[0], path, "EDIFACT".equals(guide.standard()));
            }
            else
            {
               if(path == null || !path.contains("[*]/"))
               {
                  throw new IllegalArgumentException("Association path requires a wildcard loop");
               }
               String anchor = path.substring(0, path.indexOf("[*]/"));
               String relative = path.substring(path.indexOf("[*]/") + 4);
               List<List<X12Segment>> occurrences = loops.get(anchor);
               if(occurrences == null)
               {
                  occurrences = loops.values().stream().flatMap(List::stream).filter(l -> !l.isEmpty() && l.getFirst().id().equals(anchor)).distinct().toList();
               }
               List<QRecord> children = result.getAssociatedRecords() == null ? null : result.getAssociatedRecords().get(fields[0]);
               if(children == null)
               {
                  children = new ArrayList<>();
                  for(int i = 0; i < occurrences.size(); i++)
                  {
                     children.add(new QRecord());
                  }
                  result.withAssociatedRecords(fields[0], children);
               }
               if(children.size() != occurrences.size())
               {
                  throw new IllegalArgumentException("Association rules use inconsistent loops");
               }
               for(int i = 0; i < occurrences.size(); i++)
               {
                  apply(rule, new RuleEvaluator(occurrences.get(i), loops), children.get(i), fields[1], relative, "EDIFACT".equals(guide.standard()));
               }
            }
         }
         catch(Exception e)
         {
            throw new IllegalArgumentException("Mapping failed at rule " + rule.getValueInteger("sequence") + " (" + rule.getValueString("ediPath") + " -> " + rule.getValueString("qqqPath") + ")");
         }
      }
      return (new Result(result, key(evaluator, map.getValueString("businessKeyPath")), key(evaluator, map.getValueString("secondaryKeyPath"))));
   }

   /*******************************************************************************
    ** Conditions and defaults are evaluated within the current loop occurrence.
    *******************************************************************************/
   private void apply(QRecord rule, RuleEvaluator evaluator, QRecord target, String field, String path, boolean edifact) throws Exception
   {
      String condition = rule.getValueString("condition");
      if(condition != null && !condition.isBlank() && !evaluator.test(condition))
      {
         return;
      }
      List<String> values = path == null || path.isBlank() ? List.of() : values(evaluator, path);
      if(values.isEmpty() && rule.getValueString("defaultValue") != null)
      {
         values = List.of(rule.getValueString("defaultValue"));
      }
      if(edifact && List.of("DECIMAL", "UNIT").contains(java.util.Objects.requireNonNullElse(rule.getValueString("transform"), "")))
      {
         values = values.stream().map(v -> v.replace(',', '.')).toList();
      }
      Serializable value = transforms.apply(rule.getValueString("transform"), rule.getValueString("transformArgs"), values);
      if(Boolean.TRUE.equals(rule.getValueBoolean("required")) && (value == null || value.toString().isBlank()))
      {
         throw new IllegalArgumentException("Required mapping input is absent");
      }
      target.withValue(field, value);
   }

   /*******************************************************************************
    ** A path may join several element paths with a plus outside any predicate, in order, so
    ** a transform such as DATETIME receives a date followed by a time from separate elements.
    *******************************************************************************/
   private static List<String> values(RuleEvaluator evaluator, String path)
   {
      List<String> values = new ArrayList<>();
      for(String part : path.split("\\+(?![^\\[]*\\])"))
      {
         values.addAll(evaluator.values(part.trim()));
      }
      return (values);
   }

   /*******************************************************************************
    ** Missing optional business keys remain null.
    *******************************************************************************/
   private static String key(RuleEvaluator evaluator, String path)
   {
      List<String> values = path == null || path.isBlank() ? List.of() : evaluator.values(path);
      return (values.isEmpty() ? null : values.getFirst());
   }
}
