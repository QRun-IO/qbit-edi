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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.document.EdiDocument;
import com.kingsrook.qbits.edi.x12.X12Segment;


/*******************************************************************************
 ** SNIP 1-2 structural and semantic validation over StAEDI-tokenized segments.
 *******************************************************************************/
public class GuideValidator
{
   /*******************************************************************************
    ** Validate one transaction; diagnostic text never contains submitted values.
    *******************************************************************************/
   public List<ValidationFinding> validate(EdiDocument transaction, Guide guide, boolean containsPhi)
   {
      State state = new State(transaction.segments(), containsPhi, "EDIFACT".equals(guide.standard()));
      List<Node> roots = new ArrayList<>();
      Map<String, Node> nodes = new LinkedHashMap<>();
      for(GuideSegment segment : guide.segments())
      {
         List<Node> destination = roots;
         if(segment.loopId() != null && !segment.loopId().isBlank())
         {
            Node loop = nodes.get(segment.loopId());
            if(loop == null)
            {
               loop = new Node(segment.loopId(), null);
               nodes.put(segment.loopId(), loop);
               Node parent = nodes.get(segment.parentLoopId());
               (parent == null ? roots : parent.children).add(loop);
            }
            destination = loop.children;
         }
         destination.add(new Node(null, segment));
      }
      consume(roots, state, false);
      while(state.index < state.segments.size())
      {
         X12Segment segment = state.segments.get(state.index++);
         boolean known = guide.segments().stream().anyMatch(s -> s.segmentId().equals(segment.id()));
         state.add(segment, null, null, known ? "7" : "2", null, known ? "Segment out of order" : "Unexpected segment", null);
      }
      RuleEvaluator evaluator = new RuleEvaluator(state.segments, state.loops);
      for(GuideSegment definition : guide.segments())
      {
         if(definition.condition() != null)
         {
            List<List<X12Segment>> scopes = definition.loopId() == null ? List.of(state.segments) : state.loops.getOrDefault(definition.loopId(), List.of(state.segments));
            for(List<X12Segment> scope : scopes)
            {
               RuleEvaluator scoped = new RuleEvaluator(scope, state.loops);
               if(safeTest(scoped, definition.condition()) && state.bindings.stream().noneMatch(b -> b.definition() == definition && scope.contains(b.segment())))
               {
                  state.add(X12Segment.of(definition.segmentId(), scope.isEmpty() ? 1 : scope.getLast().position() + 1), definition.loopId(), null, "3", null, "Situational segment required", null);
                  int last = state.findings.size() - 1;
                  state.findings.set(last, state.findings.get(last).withContext(scoped.context(definition.condition())));
               }
            }
         }
      }
      for(GuideRule rule : list(guide.rules()))
      {
         rule(rule, transaction.header(), null, evaluator, state);
      }
      for(Binding binding : state.bindings)
      {
         GuideSegment definition = binding.definition();
         X12Segment segment = binding.segment();
         elements(segment, definition, evaluator, state);
         for(GuideRule rule : list(definition.rules()))
         {
            rule(rule, segment, definition.loopId(), evaluator, state);
         }
      }
      return (List.copyOf(state.findings));
   }

   /*******************************************************************************
    ** Resolve guide-defined loop occurrences for the mapper using the validator grammar.
    *******************************************************************************/
   public Map<String, List<List<X12Segment>>> loops(EdiDocument transaction, Guide guide)
   {
      State state = new State(transaction.segments(), true, "EDIFACT".equals(guide.standard()));
      List<Node> roots = new ArrayList<>();
      Map<String, Node> nodes = new LinkedHashMap<>();
      for(GuideSegment segment : guide.segments())
      {
         List<Node> destination = roots;
         if(segment.loopId() != null && !segment.loopId().isBlank())
         {
            Node loop = nodes.get(segment.loopId());
            if(loop == null)
            {
               loop = new Node(segment.loopId(), null);
               nodes.put(segment.loopId(), loop);
               Node parent = nodes.get(segment.parentLoopId());
               (parent == null ? roots : parent.children).add(loop);
            }
            destination = loop.children;
         }
         destination.add(new Node(null, segment));
      }
      consume(roots, state, false);
      return (state.loops);
   }

   /*******************************************************************************
    ** Consume ordered segments and nested loop occurrences.
    *******************************************************************************/
   private static void consume(List<Node> nodes, State state, boolean loopOccurrence)
   {
      for(Node node : nodes)
      {
         if(node.definition == null)
         {
            GuideSegment head = node.children.getFirst().definition;
            int count = 0;
            while(state.matches(head.segmentId()))
            {
               int start = state.index;
               consume(node.children, state, true);
               if(start == state.index)
               {
                  break;
               }
               state.loops.computeIfAbsent(node.loopId, key -> new ArrayList<>()).add(state.segments.subList(start, state.index));
               if(++count > number(head.loopMaxRepeat(), 1))
               {
                  state.add(state.segments.get(start), node.loopId, null, "4", null, "Loop repeat limit exceeded", null);
               }
            }
            if(count == 0 && "REQUIRED".equals(head.usage()))
            {
               state.add(X12Segment.of(head.segmentId(), state.position()), node.loopId, null, "3", null, "Required loop missing", null);
            }
         }
         else
         {
            GuideSegment definition = node.definition;
            int count = 0;
            int maximum = number(definition.maxUse(), 1);
            while(state.matches(definition.segmentId()) && (count == 0 || !loopOccurrence || node != nodes.getFirst()))
            {
               X12Segment segment = state.segments.get(state.index++);
               state.bindings.add(new Binding(segment, definition));
               count++;
               if("NOT_USED".equals(definition.usage()))
               {
                  state.add(segment, definition.loopId(), null, "6", null, "Segment is not used", null);
               }
               if(count > maximum)
               {
                  state.add(segment, definition.loopId(), null, "5", null, "Segment use limit exceeded", null);
               }
            }
            int minimum = Math.max(number(definition.minUse(), 0), "REQUIRED".equals(definition.usage()) ? 1 : 0);
            if(count < minimum)
            {
               state.add(X12Segment.of(definition.segmentId(), state.position()), definition.loopId(), null, "3", null, "Required segment missing", null);
            }
         }
      }
   }

   /*******************************************************************************
    ** Apply usage, lengths, data types, code lists, fixed values and repetition bounds.
    *******************************************************************************/
   private static void elements(X12Segment segment, GuideSegment definition, RuleEvaluator evaluator, State state)
   {
      for(GuideElement element : list(definition.elements()))
      {
         int repetitions = element.position() <= segment.elements().size() ? segment.elements().get(element.position() - 1).size() : 1;
         if(repetitions > number(element.repeatMax(), 1))
         {
            state.add(segment, definition.loopId(), element, "8", "12", "Element repetition limit exceeded", null);
         }
         for(int r = 1; r <= repetitions; r++)
         {
            String value = segment.value(element.position(), r, number(element.componentPosition(), 1));
            boolean required = "REQUIRED".equals(element.usage()) || element.condition() != null && safeTest(evaluator, element.condition());
            if(value.isEmpty())
            {
               if(required)
               {
                  state.add(segment, definition.loopId(), element, "8", "1", "Required element missing", null);
                  if(element.condition() != null)
                  {
                     int last = state.findings.size() - 1;
                     state.findings.set(last, state.findings.get(last).withContext(evaluator.context(element.condition())));
                  }
               }
               continue;
            }
            if("NOT_USED".equals(element.usage()))
            {
               state.add(segment, definition.loopId(), element, "8", "10", "Element is not used", value);
            }
            if(value.length() < number(element.minLength(), 0))
            {
               state.add(segment, definition.loopId(), element, "8", "4", "Element too short", value);
            }
            if(value.length() > number(element.maxLength(), Integer.MAX_VALUE))
            {
               state.add(segment, definition.loopId(), element, "8", "5", "Element too long", value);
            }
            if(!validType(element.dataType(), state.edifact && "R".equals(element.dataType()) ? value.replace(',', '.') : value))
            {
               String code = "DT".equals(element.dataType()) ? "8" : "TM".equals(element.dataType()) ? "9" : "6";
               state.add(segment, definition.loopId(), element, "8", code, "Invalid element data type", value);
            }
            if(element.fixedValue() != null && !element.fixedValue().equals(value)
               || "ID".equals(element.dataType()) && element.codes() != null && !element.codes().contains(value))
            {
               state.add(segment, definition.loopId(), element, "8", "7", "Invalid code or fixed value", value);
            }
         }
      }
   }

   /*******************************************************************************
    ** Validate X12 lexical data types without coercing bad values.
    *******************************************************************************/
   public static boolean validType(String type, String value)
   {
      if(type == null || type.equals("AN") || type.equals("ID") || type.equals("B"))
      {
         return (value.chars().noneMatch(Character::isISOControl));
      }
      if(type.matches("N[0-9]"))
      {
         return (value.matches("-?[0-9]+"));
      }
      if(type.equals("R"))
      {
         return (value.matches("-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"));
      }
      if(type.equals("DT"))
      {
         return (RuleEvaluator.validDate(value));
      }
      if(type.equals("TM"))
      {
         return (value.matches("(?:[01][0-9]|2[0-3])[0-5][0-9](?:[0-5][0-9](?:[0-9]{1,2})?)?"));
      }
      throw new IllegalArgumentException("Unknown guide data type");
   }

   /*******************************************************************************
    ** Evaluate syntax notes and explicit expressions; PHI rules use a safe message.
    *******************************************************************************/
   private static void rule(GuideRule rule, X12Segment segment, String loopId, RuleEvaluator evaluator, State state)
   {
      List<Integer> positions = list(rule.positions());
      long present = positions.stream().filter(p -> !segment.value(p).isEmpty()).count();
      boolean first = !positions.isEmpty() && !segment.value(positions.getFirst()).isEmpty();
      boolean passed = switch(rule.type())
      {
         case "PAIRED", "P" -> present == 0 || present == positions.size();
         case "REQUIRED", "R" -> present > 0;
         case "EXCLUSION", "E" -> present <= 1;
         case "CONDITIONAL", "C" -> !first || present == positions.size();
         case "LIST_CONDITIONAL", "L" -> !first || present > 1;
         case "SITUATIONAL" -> safeTest(evaluator, rule.expression());
         default -> throw new IllegalArgumentException("Unknown syntax note");
      };
      if(!passed)
      {
         state.findings.add(new ValidationFinding(segment.position(), segment.id(), loopId, positions.isEmpty() ? null : positions.getFirst(), null,
            number(rule.snipLevel(), 2), fallback(rule.ak3Code(), "8"), fallback(rule.ak4Code(), "2"), fallback(rule.ik3Code(), "8"), fallback(rule.ik4Code(), "2"),
            fallback(rule.severity(), "ERROR"), state.phi ? "Guide rule failed" : fallback(rule.message(), "Guide rule failed"), null)
            .withContext("SITUATIONAL".equals(rule.type()) ? evaluator.context(rule.expression()) : null));
      }
   }

   /*******************************************************************************
    ** Bad data cannot leak through numeric or regular-expression exception messages.
    *******************************************************************************/
   private static boolean safeTest(RuleEvaluator evaluator, String expression)
   {
      try
      {
         return (evaluator.test(expression));
      }
      catch(IllegalArgumentException e)
      {
         return (false);
      }
   }

   /*******************************************************************************
    ** Use a default for omitted optional numeric constraints.
    *******************************************************************************/
   private static int number(Integer value, int fallback)
   {
      return (value == null ? fallback : value);
   }

   /*******************************************************************************
    ** Use a default for omitted optional text.
    *******************************************************************************/
   private static String fallback(String value, String fallback)
   {
      return (value == null ? fallback : value);
   }

   /*******************************************************************************
    ** Normalize omitted JSON lists.
    *******************************************************************************/
   private static <T> List<T> list(List<T> value)
   {
      return (value == null ? List.of() : value);
   }

   /*******************************************************************************
    ** A guide tree node.
    *******************************************************************************/
   private static class Node
   {
      private final String loopId;
      private final GuideSegment definition;
      private final List<Node> children = new ArrayList<>();

      /*******************************************************************************
       ** Create a loop or segment node.
       *******************************************************************************/
      private Node(String loopId, GuideSegment definition)
      {
         this.loopId = loopId;
         this.definition = definition;
      }
   }

   /*******************************************************************************
    ** Bind a parsed segment to its guide definition.
    *******************************************************************************/
   private record Binding(X12Segment segment, GuideSegment definition)
   {
   }

   /*******************************************************************************
    ** Per-call state; validator instances are safe to share.
    *******************************************************************************/
   private static class State
   {
      private final List<X12Segment> segments;
      private final boolean phi;
      private final boolean edifact;
      private final List<ValidationFinding> findings = new ArrayList<>();
      private final List<Binding> bindings = new ArrayList<>();
      private final Map<String, List<List<X12Segment>>> loops = new LinkedHashMap<>();
      private int index;

      /*******************************************************************************
       ** Initialize a transaction validation.
       *******************************************************************************/
      private State(List<X12Segment> segments, boolean phi, boolean edifact)
      {
         this.segments = segments;
         this.phi = phi;
         this.edifact = edifact;
      }

      /*******************************************************************************
       ** Check the next segment.
       *******************************************************************************/
      private boolean matches(String id)
      {
         return (index < segments.size() && segments.get(index).id().equals(id));
      }

      /*******************************************************************************
       ** Locate a missing segment relative to the buffered transaction.
       *******************************************************************************/
      private int position()
      {
         return (index < segments.size() ? segments.get(index).position() : segments.size() + 1);
      }

      /*******************************************************************************
       ** Add a diagnostic with a bounded, optionally suppressed excerpt.
       *******************************************************************************/
      private void add(X12Segment segment, String loopId, GuideElement element, String segmentCode, String elementCode, String message, String value)
      {
         findings.add(new ValidationFinding(segment.position(), segment.id(), loopId, element == null ? null : element.position(), element == null ? null : element.componentPosition(),
            2, segmentCode, elementCode, segmentCode, elementCode, "ERROR", message, phi || value == null ? null : value.substring(0, Math.min(64, value.length()))));
      }
   }
}
