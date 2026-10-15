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

package com.kingsrook.qbits.edi.api;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;

/*******************************************************************************
 ** Versioned wire types; schemas describe values without exposing QRecord internals.
 *******************************************************************************/
public final class EdiSchemas
{
   /*******************************************************************************
    ** Static schema catalog.
    *******************************************************************************/
   private EdiSchemas()
   {
   }

   /*******************************************************************************
    ** A reference to another versioned wire type.
    *******************************************************************************/
   public static Schema ref(String name)
   {
      return (new Schema().withRefToSchema(name));
   }

   /*******************************************************************************
    ** Array schema with an explicit item shape.
    *******************************************************************************/
   private static Schema array(Schema item)
   {
      return (new Schema().withType(Type.ARRAY).withItems(item));
   }

   /*******************************************************************************
    ** Ordered guide tree. Root segments are outside loops; nested loops retain parent scope.
    *******************************************************************************/
   public record EdiGuideTreeV1(List<EdiLoopV1> loops, List<EdiSegmentV1> segments) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Ordered guide tree. Root segments are outside loops; nested loops retain parent scope.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("loops", array(ref("EdiLoopV1"))),
               Map.entry("segments", array(ref("EdiSegmentV1")))))));
      }
   }

   /*******************************************************************************
    ** Guide loop, nested within its parent; repeat is the maximum occurrence count.
    *******************************************************************************/
   public record EdiLoopV1(String id, Integer repeat, List<EdiLoopV1> loops, List<EdiSegmentV1> segments) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Guide loop, nested within its parent; repeat is the maximum occurrence count.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("id", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("repeat", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("loops", array(ref("EdiLoopV1"))),
               Map.entry("segments", array(ref("EdiSegmentV1")))))));
      }
   }

   /*******************************************************************************
    ** Segment definition in guide sequence order.
    *******************************************************************************/
   public record EdiSegmentV1(String id, String position, String usage, Integer min, Integer max, String name, List<EdiElementV1> elements) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Segment definition in guide sequence order.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("id", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("position", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("usage", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("min", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("max", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("name", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("elements", array(ref("EdiElementV1")))))));
      }
   }

   /*******************************************************************************
    ** Element or composite component; codes contain at most 200 active codes in lexical order.
    *******************************************************************************/
   public record EdiElementV1(Integer position, Integer component, String id, String name, String type, Integer minLength, Integer maxLength, String usage, Integer codeListId, List<String> codes) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Element or composite component; codes contain at most 200 active codes in lexical order.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("position", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("component", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("id", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("name", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("type", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("minLength", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("maxLength", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("usage", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("codeListId", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("codes", array(new Schema().withType(Type.STRING).withNullable(true)))))));
      }
   }

   /*******************************************************************************
    ** Target metadata visible to the caller, current rules and registered transforms.
    *******************************************************************************/
   public record EdiTargetV1(String table, List<EdiFieldV1> fields, List<EdiAssociationV1> associations, List<EdiRuleV1> rules, List<EdiTransformV1> transforms) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Target metadata visible to the caller, current rules and registered transforms.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("table", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("fields", array(ref("EdiFieldV1"))),
               Map.entry("associations", array(ref("EdiAssociationV1"))),
               Map.entry("rules", array(ref("EdiRuleV1"))),
               Map.entry("transforms", array(ref("EdiTransformV1")))))));
      }
   }

   /*******************************************************************************
    ** QQQ target field metadata.
    *******************************************************************************/
   public record EdiFieldV1(String name, String label, String type, Boolean required, String possibleValueSourceName) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("QQQ target field metadata.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("name", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("label", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("type", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("required", new Schema().withType(Type.BOOLEAN)),
               Map.entry("possibleValueSourceName", new Schema().withType(Type.STRING).withNullable(true))))));
      }
   }

   /*******************************************************************************
    ** Direct QQQ association and its target fields.
    *******************************************************************************/
   public record EdiAssociationV1(String name, String table, String label, List<EdiFieldV1> fields) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Direct QQQ association and its target fields.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("name", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("table", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("label", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("fields", array(ref("EdiFieldV1")))))));
      }
   }

   /*******************************************************************************
    ** Map rule. id is optional for unsaved rules. transformArgs is a JSON object encoded as a string, as in core table CRUD.
    *******************************************************************************/
   public record EdiRuleV1(Integer id, Integer sequence, String ediPath, String qqqPath, String transform, String transformArgs, Boolean required, String condition, String defaultValue) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Map rule. id is optional for unsaved rules. transformArgs is a JSON object encoded as a string, as in core table CRUD.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("id", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("sequence", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("ediPath", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("qqqPath", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("transform", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("transformArgs", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("required", new Schema().withType(Type.BOOLEAN).withNullable(true)),
               Map.entry("condition", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("defaultValue", new Schema().withType(Type.STRING).withNullable(true))))));
      }
   }

   /*******************************************************************************
    ** Registered transform and JSON Schema describing its string-valued arguments.
    *******************************************************************************/
   public record EdiTransformV1(String name, Map<String, Object> argumentSchema) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Registered transform and JSON Schema describing its string-valued arguments.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("name", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("argumentSchema", new Schema().withType(Type.OBJECT).withAdditionalProperties(true).withDescription("JSON Schema: type, properties, required and additionalProperties. All transform arguments are strings."))))));
      }
   }

   /*******************************************************************************
    ** mapId is required. Supply exactly one of sampleId or sampleBase64. Uploads are at most 5 MiB decoded, stored encrypted. ruleOverrides replaces the whole rule set in memory. CURRENT (default) uses the newest active guide in the pinned guide family; ORIGINAL uses the pinned guide.
    *******************************************************************************/
   public record EdiPreviewRequestV1(Integer mapId, Integer sampleId, String sampleBase64, String sampleName, List<EdiRuleV1> ruleOverrides, String guideRevision) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("mapId is required. Supply exactly one of sampleId or sampleBase64. Uploads are at most 5 MiB decoded, stored encrypted. ruleOverrides replaces the whole rule set in memory. CURRENT (default) uses the newest active guide in the pinned guide family; ORIGINAL uses the pinned guide.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("mapId", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("sampleId", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("sampleBase64", new Schema().withType(Type.STRING).withNullable(true).withFormat("byte").withMaxLength(6990508)),
               Map.entry("sampleName", new Schema().withType(Type.STRING).withNullable(true).withMaxLength(255)),
               Map.entry("ruleOverrides", array(ref("EdiRuleV1")).withNullable(true)),
               Map.entry("guideRevision", new Schema().withType(Type.STRING).withNullable(true).withEnumValues(List.of("CURRENT", "ORIGINAL")))))));
      }
   }

   /*******************************************************************************
    ** Parse, validate, acknowledge and map without target writes. PHI suppresses result, ack text, and both diffs.
    *******************************************************************************/
   public record EdiPreviewV1(EdiParseV1 parse, List<EdiFindingV1> findings, EdiAckV1 ack, EdiResultV1 result, EdiDiffV1 diff, Boolean phiSuppressed) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Parse, validate, acknowledge and map without target writes. PHI suppresses result, ack text, and both diffs.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("parse", ref("EdiParseV1")),
               Map.entry("findings", array(ref("EdiFindingV1"))),
               Map.entry("ack", ref("EdiAckV1")),
               Map.entry("result", ref("EdiResultV1").withNullable(true)),
               Map.entry("diff", ref("EdiDiffV1")),
               Map.entry("phiSuppressed", new Schema().withType(Type.BOOLEAN))))));
      }
   }

   /*******************************************************************************
    ** Counts of wire interchanges, groups and documents.
    *******************************************************************************/
   public record EdiParseV1(Integer interchanges, Integer groups, Integer documents) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Counts of wire interchanges, groups and documents.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("interchanges", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("groups", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("documents", new Schema().withType(Type.INTEGER).withNullable(true))))));
      }
   }

   /*******************************************************************************
    ** Value-free diagnostic. PHI messages are generic; only positions and codes are retained.
    *******************************************************************************/
   public record EdiFindingV1(Integer snipLevel, String severity, Integer segmentPosition, String segmentId, String loopId, Integer elementPosition, String code, String message) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Value-free diagnostic. PHI messages are generic; only positions and codes are retained.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("snipLevel", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("severity", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("segmentPosition", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("segmentId", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("loopId", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("elementPosition", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("code", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("message", new Schema().withType(Type.STRING).withNullable(true))))));
      }
   }

   /*******************************************************************************
    ** Concatenated acknowledgement previews; type is 997, 999, TA1, CONTRL, NONE or MIXED. text is null for PHI.
    *******************************************************************************/
   public record EdiAckV1(String type, String text) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Concatenated acknowledgement previews; type is 997, 999, TA1, CONTRL, NONE or MIXED. text is null for PHI.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("type", new Schema().withType(Type.STRING).withNullable(true)),
               Map.entry("text", new Schema().withType(Type.STRING).withNullable(true))))));
      }
   }

   /*******************************************************************************
    ** Mapped records in document order; no target writes.
    *******************************************************************************/
   public record EdiResultV1(List<EdiRecordV1> records) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Mapped records in document order; no target writes.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("records", array(ref("EdiRecordV1")))))));
      }
   }

   /*******************************************************************************
    ** Field values and recursively associated records. Values use the target table field types.
    *******************************************************************************/
   public record EdiRecordV1(Map<String, Object> values, Map<String, List<EdiRecordV1>> associations) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Field values and recursively associated records. Values use the target table field types.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("values", new Schema().withType(Type.OBJECT).withAdditionalProperties(true)),
               Map.entry("associations", new Schema().withType(Type.OBJECT).withAdditionalProperties(array(ref("EdiRecordV1"))))))));
      }
   }

   /*******************************************************************************
    ** Only differing records are returned. Both lists are empty for PHI.
    *******************************************************************************/
   public record EdiDiffV1(List<EdiDifferenceV1> againstPreviousRevision, List<EdiDifferenceV1> againstExpected) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Only differing records are returned. Both lists are empty for PHI.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("againstPreviousRevision", array(ref("EdiDifferenceV1"))),
               Map.entry("againstExpected", array(ref("EdiDifferenceV1")))))));
      }
   }

   /*******************************************************************************
    ** One document comparison; documentIndex is zero based.
    *******************************************************************************/
   public record EdiDifferenceV1(Integer documentIndex, Object before, Object after) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("One document comparison; documentIndex is zero based.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("documentIndex", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("before", new Schema().withNullable(true).withDescription("Expected JSON or a previous mapped record.")),
               Map.entry("after", new Schema().withType(Type.OBJECT).withAdditionalProperties(true).withNullable(true))))));
      }
   }

   /*******************************************************************************
    ** rules is required and may be empty; rules are never persisted.
    *******************************************************************************/
   public record EdiValidateRequestV1(List<EdiRuleV1> rules) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("rules is required and may be empty; rules are never persisted.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("rules", array(ref("EdiRuleV1")))))));
      }
   }

   /*******************************************************************************
    ** One result per submitted rule, including rules with no errors.
    *******************************************************************************/
   public record EdiValidationV1(List<EdiRuleErrorsV1> rules) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("One result per submitted rule, including rules with no errors.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("rules", array(ref("EdiRuleErrorsV1")))))));
      }
   }

   /*******************************************************************************
    ** Correlate errors using zero-based index, optional id and sequence.
    *******************************************************************************/
   public record EdiRuleErrorsV1(Integer index, Integer id, Integer sequence, List<String> errors) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Correlate errors using zero-based index, optional id and sequence.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("index", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("id", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("sequence", new Schema().withType(Type.INTEGER).withNullable(true)),
               Map.entry("errors", array(new Schema().withType(Type.STRING).withNullable(true)))))));
      }
   }

   /*******************************************************************************
    ** Core middleware error body.
    *******************************************************************************/
   public record EdiErrorV1(String error) implements ToSchema
   {
      /*******************************************************************************
       ** Describe this wire object for generated clients.
       *******************************************************************************/
      @Override
      public Schema toSchema()
      {
         return (new Schema().withType(Type.OBJECT).withDescription("Deprecated since 1.1.0: errors use BasicErrorResponseV1.")
            .withProperties(new TreeMap<>(Map.ofEntries(
               Map.entry("error", new Schema().withType(Type.STRING).withNullable(true))))));
      }
   }

   /*******************************************************************************
    ** Complete deterministic schema catalog for all four operations.
    *******************************************************************************/
   public static Map<String, Schema> all()
   {
      Map<String, Schema> result = new TreeMap<>();
      result.put("EdiGuideTreeV1", new EdiGuideTreeV1(null, null).toSchema());
      result.put("EdiLoopV1", new EdiLoopV1(null, null, null, null).toSchema());
      result.put("EdiSegmentV1", new EdiSegmentV1(null, null, null, null, null, null, null).toSchema());
      result.put("EdiElementV1", new EdiElementV1(null, null, null, null, null, null, null, null, null, null).toSchema());
      result.put("EdiTargetV1", new EdiTargetV1(null, null, null, null, null).toSchema());
      result.put("EdiFieldV1", new EdiFieldV1(null, null, null, null, null).toSchema());
      result.put("EdiAssociationV1", new EdiAssociationV1(null, null, null, null).toSchema());
      result.put("EdiRuleV1", new EdiRuleV1(null, null, null, null, null, null, null, null, null).toSchema());
      result.put("EdiTransformV1", new EdiTransformV1(null, null).toSchema());
      result.put("EdiPreviewRequestV1", new EdiPreviewRequestV1(null, null, null, null, null, null).toSchema());
      result.put("EdiPreviewV1", new EdiPreviewV1(null, null, null, null, null, null).toSchema());
      result.put("EdiParseV1", new EdiParseV1(null, null, null).toSchema());
      result.put("EdiFindingV1", new EdiFindingV1(null, null, null, null, null, null, null, null).toSchema());
      result.put("EdiAckV1", new EdiAckV1(null, null).toSchema());
      result.put("EdiResultV1", new EdiResultV1(null).toSchema());
      result.put("EdiRecordV1", new EdiRecordV1(null, null).toSchema());
      result.put("EdiDiffV1", new EdiDiffV1(null, null).toSchema());
      result.put("EdiDifferenceV1", new EdiDifferenceV1(null, null, null).toSchema());
      result.put("EdiValidateRequestV1", new EdiValidateRequestV1(null).toSchema());
      result.put("EdiValidationV1", new EdiValidationV1(null).toSchema());
      result.put("EdiRuleErrorsV1", new EdiRuleErrorsV1(null, null, null, null).toSchema());
      result.put("EdiErrorV1", new EdiErrorV1(null).toSchema());
      return (result);
   }
}
