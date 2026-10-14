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


import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qbits.edi.x12.X12Segment;


/*******************************************************************************
 ** Parser and evaluator for the closed EDI rule language. No scripting or reflection.
 *******************************************************************************/
public class RuleEvaluator
{
   private final List<X12Segment> segments;
   private final Map<String, List<List<X12Segment>>> loops;

   /*******************************************************************************
    ** Bind document segments and guide-derived loop occurrences.
    *******************************************************************************/
   public RuleEvaluator(List<X12Segment> segments, Map<String, List<List<X12Segment>>> loops)
   {
      this.segments = segments;
      this.loops = loops;
   }

   /*******************************************************************************
    ** Require the expression to return a boolean.
    *******************************************************************************/
   public boolean test(String expression)
   {
      Object result = evaluate(expression.trim());
      if(!(result instanceof Boolean))
      {
         throw new IllegalArgumentException("Rule must return a boolean");
      }
      return ((Boolean) result);
   }

   /*******************************************************************************
    ** Evaluate one expression recursively with explicit functions only.
    *******************************************************************************/
   public Object evaluate(String expression)
   {
      String text = expression.trim();
      if(text.startsWith("'") && text.endsWith("'"))
      {
         return (text.substring(1, text.length() - 1).replace("\'", "'"));
      }
      if(text.matches("-?[0-9]+(?:\\.[0-9]+)?"))
      {
         return (new BigDecimal(text));
      }
      int open = text.indexOf('(');
      if(open < 0)
      {
         return (values(text));
      }
      if(!text.endsWith(")"))
      {
         throw new IllegalArgumentException("Unclosed rule function");
      }
      String function = text.substring(0, open).trim();
      List<String> args = arguments(text.substring(open + 1, text.length() - 1));
      int arity = switch(function)
      {
         case "present", "absent", "count", "sum", "date", "not" -> 1;
         case "eq", "matches", "when" -> 2;
         case "in" -> -2;
         case "and", "or" -> -1;
         default -> throw new IllegalArgumentException("Unknown rule function");
      };
      if(arity > 0 && args.size() != arity || arity < 0 && args.size() < -arity)
      {
         throw new IllegalArgumentException("Invalid rule argument count");
      }
      return (switch(function)
      {
         case "present" -> !asValues(evaluate(arg(args, 0))).isEmpty();
         case "absent" -> asValues(evaluate(arg(args, 0))).isEmpty();
         case "count" -> BigDecimal.valueOf(asValues(evaluate(arg(args, 0))).size());
         case "sum" -> asValues(evaluate(arg(args, 0))).stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
         case "eq" -> equal(evaluate(arg(args, 0)), evaluate(arg(args, 1)));
         case "in" -> args.subList(1, args.size()).stream().anyMatch(a -> equal(evaluate(arg(args, 0)), evaluate(a)));
         case "matches" -> asValues(evaluate(arg(args, 0))).stream().allMatch(v -> v.matches(scalar(evaluate(arg(args, 1)))));
         case "date" -> validDate(scalar(evaluate(arg(args, 0))));
         case "and" -> args.stream().allMatch(this::test);
         case "or" -> args.stream().anyMatch(this::test);
         case "not" -> !test(arg(args, 0));
         case "when" -> !test(arg(args, 0)) || test(arg(args, 1));
         default -> throw new IllegalArgumentException("Unknown rule function");
      });
   }

   /*******************************************************************************
    ** Extract a required argument.
    *******************************************************************************/
   private static String arg(List<String> args, int index)
   {
      if(index >= args.size())
      {
         throw new IllegalArgumentException("Missing rule argument");
      }
      return (args.get(index));
   }

   /*******************************************************************************
    ** Split function arguments outside nested calls, predicates and quoted strings.
    *******************************************************************************/
   private static List<String> arguments(String text)
   {
      List<String> result = new ArrayList<>();
      boolean quoted = false;
      int depth = 0;
      int start = 0;
      for(int i = 0; i < text.length(); i++)
      {
         char c = text.charAt(i);
         if(c == '\'' && (i == 0 || text.charAt(i - 1) != '\\'))
         {
            quoted = !quoted;
         }
         if(!quoted)
         {
            if(c == '(' || c == '[')
            {
               depth++;
            }
            if(c == ')' || c == ']')
            {
               depth--;
            }
            if(c == ',' && depth == 0)
            {
               result.add(text.substring(start, i).trim());
               start = i + 1;
            }
         }
      }
      if(quoted || depth != 0)
      {
         throw new IllegalArgumentException("Invalid rule syntax");
      }
      result.add(text.substring(start).trim());
      return (result);
   }

   /*******************************************************************************
    ** Resolve loop paths, segment predicates, one-based indexes and components.
    *******************************************************************************/
   public List<String> values(String path)
   {
      List<X12Segment> scope = segments;
      String[] steps = path.split("/");
      for(int i = 0; i < steps.length; i++)
      {
         String step = steps[i];
         Matcher selector = Pattern.compile("([^\\[]+)(?:\\[(.*)])?").matcher(step);
         if(!selector.matches())
         {
            throw new IllegalArgumentException("Invalid EDI path");
         }
         String name = selector.group(1);
         String qualifier = selector.group(2);
         Matcher element = Pattern.compile("([A-Z][A-Z0-9]{1,2}?)([0-9]{2})(?:-([0-9]+))?").matcher(name);
         if(element.matches())
         {
            List<X12Segment> selected = select(scope, element.group(1), qualifier);
            int position = Integer.parseInt(element.group(2));
            int component = element.group(3) == null ? 1 : Integer.parseInt(element.group(3));
            List<String> result = new ArrayList<>();
            for(X12Segment segment : selected)
            {
               if(position <= segment.elements().size())
               {
                  for(int r = 1; r <= segment.elements().get(position - 1).size(); r++)
                  {
                     String value = segment.value(position, r, component);
                     if(!value.isEmpty())
                     {
                        result.add(value);
                     }
                  }
               }
            }
            return (result);
         }
         List<List<X12Segment>> occurrences = loops.get(name);
         if(occurrences == null)
         {
            List<List<X12Segment>> byAnchor = loops.values().stream().flatMap(List::stream)
               .filter(o -> !o.isEmpty() && o.getFirst().id().equals(name)).distinct()
               .sorted(java.util.Comparator.comparingInt(o -> o.getFirst().position())).toList();
            if(!byAnchor.isEmpty())
            {
               occurrences = byAnchor;
            }
         }
         if(occurrences != null)
         {
            List<X12Segment> priorScope = scope;
            occurrences = occurrences.stream().filter(o -> !o.isEmpty() && priorScope.contains(o.getFirst())).toList();
            List<X12Segment> heads = select(occurrences.stream().map(List::getFirst).toList(), null, qualifier);
            scope = occurrences.stream().filter(o -> heads.contains(o.getFirst())).flatMap(List::stream).toList();
            if(i == steps.length - 1)
            {
               return (heads.stream().map(X12Segment::id).toList());
            }
         }
         else
         {
            scope = select(scope, name, qualifier);
         }
      }
      return (scope.stream().map(X12Segment::id).toList());
   }

   /*******************************************************************************
    ** Apply segment selection without evaluating arbitrary expressions.
    *******************************************************************************/
   private static List<X12Segment> select(List<X12Segment> scope, String name, String qualifier)
   {
      List<X12Segment> result = scope.stream().filter(s -> name == null || name.equals(s.id())).toList();
      if(qualifier == null || qualifier.equals("*"))
      {
         return (result);
      }
      if(qualifier.matches("[0-9]+"))
      {
         int index = Integer.parseInt(qualifier) - 1;
         return (index >= 0 && index < result.size() ? List.of(result.get(index)) : List.of());
      }
      Matcher predicate = Pattern.compile("[A-Z][A-Z0-9]{1,2}?([0-9]{2})(?:-([0-9]+))?=(.*)").matcher(qualifier);
      if(!predicate.matches())
      {
         throw new IllegalArgumentException("Invalid path predicate");
      }
      int position = Integer.parseInt(predicate.group(1));
      int component = predicate.group(2) == null ? 1 : Integer.parseInt(predicate.group(2));
      String expected = predicate.group(3).replace("'", "");
      return (result.stream().filter(s -> s.value(position, 1, component).equals(expected)).toList());
   }

   /*******************************************************************************
    ** Convert values to a scalar for scalar-only functions.
    *******************************************************************************/
   private static String scalar(Object value)
   {
      List<String> values = asValues(value);
      return (values.isEmpty() ? "" : values.getFirst());
   }

   /*******************************************************************************
    ** Normalize scalar and path results.
    *******************************************************************************/
   private static List<String> asValues(Object value)
   {
      if(value instanceof List<?> list)
      {
         return (list.stream().map(Object::toString).toList());
      }
      return (List.of(value.toString()));
   }

   /*******************************************************************************
    ** Compare numerically when either argument is numeric, otherwise by value.
    *******************************************************************************/
   private static boolean equal(Object left, Object right)
   {
      if(left instanceof BigDecimal || right instanceof BigDecimal)
      {
         try
         {
            return (new BigDecimal(scalar(left)).compareTo(new BigDecimal(scalar(right))) == 0);
         }
         catch(NumberFormatException e)
         {
            return (false);
         }
      }
      return (asValues(left).stream().anyMatch(asValues(right)::contains));
   }

   /*******************************************************************************
    ** Validate six- or eight-digit calendar dates; YY uses the 1950-2049 window.
    *******************************************************************************/
   public static boolean validDate(String value)
   {
      if(!value.matches("[0-9]{6}|[0-9]{8}"))
      {
         return (false);
      }
      if(value.length() == 6)
      {
         value = (Integer.parseInt(value.substring(0, 2)) >= 50 ? "19" : "20") + value;
      }
      try
      {
         LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
         return (true);
      }
      catch(DateTimeException e)
      {
         return (false);
      }
   }

   /*******************************************************************************
    ** Locate the first present trigger referenced by a situational condition; values never leave the evaluator.
    *******************************************************************************/
   public AckContext context(String expression)
   {
      String condition = expression.trim();
      if(condition.startsWith("when("))
      {
         condition = arguments(condition.substring(5, condition.length() - 1)).getFirst();
      }
      // Remove literals before recognizing paths so quoted business text cannot become context.
      condition = condition.replaceAll("'[^']*'", "");
      Matcher matcher = Pattern.compile("([A-Z][A-Z0-9]{1,2}?)([0-9]{2})(?:-([0-9]+))?").matcher(condition);
      while(matcher.find())
      {
         String id = matcher.group(1);
         int position = Integer.parseInt(matcher.group(2));
         Integer component = matcher.group(3) == null ? null : Integer.valueOf(matcher.group(3));
         for(X12Segment segment : segments)
         {
            if(segment.id().equals(id) && !segment.value(position, 1, component == null ? 1 : component).isEmpty())
            {
               String loop = loops.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(o -> o.contains(segment))).map(Map.Entry::getKey).findFirst().orElse(null);
               return (new AckContext(id, segment.position(), loop, position, component));
            }
         }
      }
      return (null);
   }
}
