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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Load and persist versioned normalized guide rows through standard QQQ actions.
 *******************************************************************************/
public class GuideRepository
{
   /*******************************************************************************
    ** Read a guide and all its ordered definitions.
    *******************************************************************************/
   public Guide load(Integer id) throws Exception
   {
      QRecord guide = rows("edi_guide", "id", id).getFirst();
      List<QRecord> definitions = new ArrayList<>(rows("edi_guide_segment", "guideId", id));
      definitions.sort(Comparator.comparing(r -> r.getValueInteger("sequence")));
      List<GuideSegment> segments = new ArrayList<>();
      List<QRecord> rules = rows("edi_guide_rule", "guideId", id);
      for(QRecord definition : definitions)
      {
         Integer segmentId = definition.getValueInteger("id");
         List<GuideElement> elements = new ArrayList<>();
         for(QRecord element : rows("edi_guide_element", "guideSegmentId", segmentId))
         {
            List<String> codes = null;
            QRecord codeList = null;
            if(element.getValueInteger("codeListId") != null)
            {
               codeList = rows("edi_code_list", "id", element.getValueInteger("codeListId")).getFirst();
               codes = new ArrayList<>();
               for(QRecord code : rows("edi_code_list_value", "codeListId", element.getValueInteger("codeListId")))
               {
                  if(Boolean.TRUE.equals(code.getValueBoolean("isActive")))
                  {
                     codes.add(code.getValueString("code"));
                  }
               }
            }
            elements.add(new GuideElement(element.getValueInteger("position"), element.getValueInteger("componentPosition"), element.getValueString("elementId"),
               element.getValueString("dataType"), element.getValueInteger("minLength"), element.getValueInteger("maxLength"), element.getValueString("usage"), codes,
               element.getValueString("fixedValue"), element.getValueInteger("repeatMax"), condition(rules, element.getValueInteger("situationalRuleId")), codeList == null ? null : codeList.getValueString("source"), codeList == null ? null : codeList.getValueString("externalCodeSet")));
         }
         List<GuideRule> segmentRules = rules.stream().filter(r -> segmentId.equals(r.getValueInteger("guideSegmentId")) && !"condition".equals(r.getValueString("message"))).map(GuideRepository::rule).toList();
         segments.add(new GuideSegment(definition.getValueString("segmentId"), definition.getValueString("loopId"), definition.getValueString("parentLoopId"),
            definition.getValueInteger("loopMaxRepeat"), definition.getValueString("usage"), definition.getValueInteger("minUse"), definition.getValueInteger("maxUse"),
            elements, segmentRules, condition(rules, definition.getValueInteger("situationalRuleId"))));
      }
      return (new Guide(guide.getValueString("name"), guide.getValueString("standard"), guide.getValueString("version"), guide.getValueString("transactionSet"),
         guide.getValueString("functionalIdCode"), segments, rules.stream().filter(r -> r.getValueInteger("guideSegmentId") == null).map(GuideRepository::rule).toList()));
   }

   /*******************************************************************************
    ** Insert a complete new revision atomically, retaining every previous revision.
    *******************************************************************************/
   public Integer save(Guide guide, String checksum, int revision) throws Exception
   {
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput("edi_guide")))
      {
         try
         {
            QRecord row = insert("edi_guide", new QRecord().withValue("name", guide.name()).withValue("standard", guide.standard()).withValue("version", guide.version())
               .withValue("transactionSet", guide.transactionSet()).withValue("functionalIdCode", guide.functionalIdCode()).withValue("revision", revision)
               .withValue("status", "ACTIVE").withValue("source", "BUILT_IN").withValue("compiledChecksum", checksum), transaction);
            Integer id = row.getValueInteger("id");
            int sequence = 0;
            for(GuideSegment segment : guide.segments())
            {
               QRecord segmentRow = insert("edi_guide_segment", new QRecord().withValue("guideId", id).withValue("sequence", ++sequence)
                  .withValue("segmentId", segment.segmentId()).withValue("name", segment.segmentId()).withValue("position", String.format("%04d", sequence * 10))
                  .withValue("loopId", segment.loopId()).withValue("parentLoopId", segment.parentLoopId()).withValue("loopMaxRepeat", segment.loopMaxRepeat())
                  .withValue("usage", segment.usage()).withValue("minUse", segment.minUse()).withValue("maxUse", segment.maxUse()), transaction);
               Integer segmentId = segmentRow.getValueInteger("id");
               for(GuideElement element : list(segment.elements()))
               {
                  Integer codeListId = null;
                  if(element.codes() != null || "EXTERNAL".equals(element.source()))
                  {
                     codeListId = insert("edi_code_list", new QRecord().withValue("name", guide.name() + "/" + revision + "/" + sequence + "/" + element.position())
                        .withValue("elementId", element.elementId()).withValue("source", element.source() == null ? "BUILT_IN" : element.source()).withValue("externalCodeSet", element.externalCodeSet()), transaction).getValueInteger("id");
                     for(String code : list(element.codes()))
                     {
                        insert("edi_code_list_value", new QRecord().withValue("codeListId", codeListId).withValue("code", code).withValue("isActive", true), transaction);
                     }
                  }
                  insert("edi_guide_element", new QRecord().withValue("guideSegmentId", segmentId).withValue("position", element.position())
                     .withValue("componentPosition", element.componentPosition()).withValue("elementId", element.elementId()).withValue("dataType", element.dataType())
                     .withValue("minLength", element.minLength()).withValue("maxLength", element.maxLength()).withValue("usage", element.usage())
                     .withValue("codeListId", codeListId).withValue("fixedValue", element.fixedValue()).withValue("repeatMax", element.repeatMax())
                     .withValue("situationalRuleId", saveCondition(id, segmentId, element.condition(), transaction)), transaction);
               }
               if(segment.condition() != null)
               {
                  new UpdateAction().execute(new UpdateInput("edi_guide_segment").withTransaction(transaction).withRecord(new QRecord().withValue("id", segmentId)
                     .withValue("situationalRuleId", saveCondition(id, segmentId, segment.condition(), transaction))));
               }
               for(GuideRule rule : list(segment.rules()))
               {
                  saveRule(id, segmentId, rule, transaction);
               }
            }
            for(GuideRule rule : list(guide.rules()))
            {
               saveRule(id, null, rule, transaction);
            }
            for(QRecord previous : EdiRecords.query("edi_guide", EdiRecords.equal("name", guide.name())))
            {
               if("BUILT_IN".equals(previous.getValueString("source")) && "ACTIVE".equals(previous.getValueString("status")) && !id.equals(previous.getValueInteger("id")))
               {
                  QRecord updated = new UpdateAction().execute(new UpdateInput("edi_guide").withTransaction(transaction)
                     .withRecord(new QRecord().withValue("id", previous.getValueInteger("id")).withValue("status", "RETIRED"))).getRecords().getFirst();
                  if(updated.getErrors() != null && !updated.getErrors().isEmpty())
                  {
                     throw new QException("Previous guide revision could not be retired");
                  }
               }
            }
            transaction.commit();
            return (id);
         }
         catch(Exception e)
         {
            transaction.rollback();
            throw e;
         }
      }
   }

   /*******************************************************************************
    ** Store a condition as a referenced guide rule.
    *******************************************************************************/
   private static Integer saveCondition(Integer guideId, Integer segmentId, String expression, QBackendTransaction transaction) throws Exception
   {
      if(expression == null)
      {
         return (null);
      }
      return (saveRule(guideId, segmentId, new GuideRule("SITUATIONAL", List.of(), expression, 2, "ERROR", "condition", "8", "2", "8", "2"), transaction));
   }

   /*******************************************************************************
    ** Store syntax and situational rules without losing ack codes.
    *******************************************************************************/
   private static Integer saveRule(Integer guideId, Integer segmentId, GuideRule rule, QBackendTransaction transaction) throws Exception
   {
      return (insert("edi_guide_rule", new QRecord().withValue("guideId", guideId).withValue("guideSegmentId", segmentId).withValue("type", rule.type())
         .withValue("positions", list(rule.positions()).stream().map(Object::toString).collect(Collectors.joining(","))).withValue("expression", rule.expression())
         .withValue("snipLevel", rule.snipLevel()).withValue("severity", rule.severity()).withValue("message", rule.message())
         .withValue("ak3Code", rule.ak3Code()).withValue("ak4Code", rule.ak4Code()).withValue("ik3Code", rule.ik3Code()).withValue("ik4Code", rule.ik4Code()), transaction).getValueInteger("id"));
   }

   /*******************************************************************************
    ** Reconstruct a syntax or situational rule.
    *******************************************************************************/
   private static GuideRule rule(QRecord row)
   {
      String positions = row.getValueString("positions");
      return (new GuideRule(row.getValueString("type"), positions == null || positions.isBlank() ? List.of() : Arrays.stream(positions.split(",")).map(Integer::valueOf).toList(),
         row.getValueString("expression"), row.getValueInteger("snipLevel"), row.getValueString("severity"), row.getValueString("message"), row.getValueString("ak3Code"),
         row.getValueString("ak4Code"), row.getValueString("ik3Code"), row.getValueString("ik4Code")));
   }

   /*******************************************************************************
    ** Resolve a condition by its explicit row id.
    *******************************************************************************/
   private static String condition(List<QRecord> rules, Integer id)
   {
      return (rules.stream().filter(r -> Objects.equals(r.getValueInteger("id"), id)).map(r -> r.getValueString("expression")).findFirst().orElse(null));
   }

   /*******************************************************************************
    ** Query normalized child records.
    *******************************************************************************/
   private static List<QRecord> rows(String table, String field, Integer id) throws Exception
   {
      return (EdiRecords.query(table, EdiRecords.equal(field, id)));
   }

   /*******************************************************************************
    ** Insert within the owning guide transaction, checking validation errors.
    *******************************************************************************/
   private static QRecord insert(String table, QRecord row, QBackendTransaction transaction) throws Exception
   {
      QRecord result = new InsertAction().execute(new InsertInput(table).withTransaction(transaction).withRecord(row)).getRecords().getFirst();
      if(result.getErrors() != null && !result.getErrors().isEmpty())
      {
         throw new QException("Guide record validation failed for " + table);
      }
      return (result);
   }

   /*******************************************************************************
    ** Treat omitted optional lists as empty.
    *******************************************************************************/
   private static <T> List<T> list(List<T> list)
   {
      return (list == null ? List.of() : list);
   }
}
