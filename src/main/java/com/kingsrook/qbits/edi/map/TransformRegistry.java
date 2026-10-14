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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;

/*******************************************************************************
 ** Named, host-extensible transforms. Arguments are a JSON object, never code.
 *******************************************************************************/
public class TransformRegistry
{
   private final Map<String, Transform> transforms = new ConcurrentHashMap<>();

   /*******************************************************************************
    ** Transform a selected path's values in the service session.
    *******************************************************************************/
   @FunctionalInterface
   public interface Transform
   {
      /*******************************************************************************
       ** Return a QQQ-compatible field value.
       *******************************************************************************/
      Serializable apply(List<String> values, Map<String, String> args) throws Exception;
   }

   /*******************************************************************************
    ** Install built-in inbound transforms and explicit outbound placeholders.
    *******************************************************************************/
   public TransformRegistry()
   {
      register("CONSTANT", (v, a) -> a.get("value"));
      register("TRIM", (v, a) -> first(v).trim());
      register("UPPER", (v, a) -> first(v).toUpperCase(Locale.ROOT));
      register("DATE", (v, a) -> date(first(v)));
      register("TIME", (v, a) -> time(first(v)).toString());
      register("DATETIME", (v, a) -> date(first(v)).atTime(time(v.size() > 1 ? v.get(1) : a.getOrDefault("time", "0000")))
         .atZone(ZoneId.of(a.getOrDefault("timezone", "UTC"))).toInstant());
      register("DATEPART", (v, a) -> zoned(first(v), a).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
      register("TIMEPART", (v, a) -> zoned(first(v), a).format(DateTimeFormatter.ofPattern("HHmmss")));
      register("DECIMAL", (v, a) -> new BigDecimal(first(v)).movePointLeft(Integer.parseInt(a.getOrDefault("scale", "0"))));
      register("CONCAT", (v, a) -> String.join(a.getOrDefault("separator", ""), v));
      register("SPLIT", (v, a) -> first(v).split(java.util.regex.Pattern.quote(a.getOrDefault("separator", ",")), -1)[Integer.parseInt(a.getOrDefault("index", "0"))]);
      register("SUBSTRING", (v, a) -> first(v).substring(Integer.parseInt(a.getOrDefault("start", "0")), Integer.parseInt(a.getOrDefault("end", String.valueOf(first(v).length())))));
      register("CODE", (v, a) ->
      {
         String translated = a.get(first(v));
         if(translated == null)
         {
            throw new IllegalArgumentException("Code is not mapped");
         }
         return (translated);
      });
      register("QUALIFIER", (v, a) -> first(v));
      register("UNIT", (v, a) -> new BigDecimal(first(v)).multiply(new BigDecimal(a.getOrDefault("factor", "1")))
         .divide(new BigDecimal(a.getOrDefault("divisor", "1")), Integer.parseInt(a.getOrDefault("scale", "6")), RoundingMode.HALF_UP));
      register("LOOKUP", (v, a) ->
      {
         QueryInput input = new QueryInput(a.get("table")).withFilter(EdiRecords.equal(a.get("key"), first(v)).withLimit(2));
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         var rows = new QueryAction().execute(input).getRecords();
         if(rows.size() != 1)
         {
            throw new IllegalArgumentException("Lookup must match exactly one record");
         }
         return (rows.getFirst().getValue(a.getOrDefault("result", "id")));
      });
      for(String name : List.of("SEQUENCE", "HL"))
      {
         register(name, (v, a) ->
         {
            throw new UnsupportedOperationException("Outbound transform is not available in phase a");
         });
      }
   }

   /*******************************************************************************
    ** Register or replace a named host transform.
    *******************************************************************************/
   public TransformRegistry register(String name, Transform transform)
   {
      transforms.put(name.toUpperCase(Locale.ROOT), transform);
      return (this);
   }

   /*******************************************************************************
    ** Execute a closed registry entry; unknown names fail explicitly.
    *******************************************************************************/
   public Serializable apply(String name, String args, List<String> values) throws Exception
   {
      if(name == null || name.isBlank())
      {
         return (values.isEmpty() ? null : values.getFirst());
      }
      Transform transform = transforms.get(name.toUpperCase(Locale.ROOT));
      if(transform == null)
      {
         throw new IllegalArgumentException("Unknown transform");
      }
      Map<String, String> arguments = args == null || args.isBlank() ? Map.of() : new ObjectMapper().readValue(args, new TypeReference<Map<String, String>>() {});
      return (transform.apply(values, arguments));
   }

   /*******************************************************************************
    ** Require one input value without echoing it in diagnostics.
    *******************************************************************************/
   private static String first(List<String> values)
   {
      if(values.isEmpty())
      {
         throw new IllegalArgumentException("Transform input is missing");
      }
      return (values.getFirst());
   }

   /*******************************************************************************
    ** Outbound DATETIME halves: an instant, or a zone-less timestamp read as UTC, in the partner zone.
    *******************************************************************************/
   private static ZonedDateTime zoned(String value, Map<String, String> args)
   {
      Instant instant = value.endsWith("Z") || value.matches(".*[+-][0-9]{2}:[0-9]{2}$") ? OffsetDateTime.parse(value).toInstant() : LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
      return (instant.atZone(ZoneId.of(args.getOrDefault("timezone", "UTC"))));
   }

   /*******************************************************************************
    ** Six-digit dates use the fixed 1950-2049 century window.
    *******************************************************************************/
   private static LocalDate date(String value)
   {
      DateTimeFormatter format = value.length() == 6 ? new DateTimeFormatterBuilder().appendValueReduced(ChronoField.YEAR, 2, 2, 1950)
         .appendPattern("MMdd").toFormatter() : DateTimeFormatter.BASIC_ISO_DATE;
      return (LocalDate.parse(value, format));
   }

   /*******************************************************************************
    ** X12 HHMM, HHMMSS and fractional seconds.
    *******************************************************************************/
   private static LocalTime time(String value)
   {
      String normalized = value.substring(0, 2) + ":" + value.substring(2, 4);
      if(value.length() > 4)
      {
         normalized += ":" + value.substring(4, 6);
         if(value.length() > 6)
         {
            normalized += "." + value.substring(6).replace(".", "");
         }
      }
      return (LocalTime.parse(normalized));
   }
}
