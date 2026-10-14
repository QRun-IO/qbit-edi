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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiAssociationV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiElementV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiFieldV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiGuideTreeV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiLoopV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiPreviewRequestV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiPreviewV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiRuleErrorsV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiRuleV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiSegmentV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiTargetV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiTransformV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiValidateRequestV1;
import com.kingsrook.qbits.edi.api.EdiSchemas.EdiValidationV1;
import com.kingsrook.qbits.edi.map.MapPreviewService;
import com.kingsrook.qbits.edi.map.MapRuleValidator;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;

/*******************************************************************************
 ** Caller-authorized projections and sandbox orchestration, using normal QQQ actions.
 *******************************************************************************/
public class EdiApiService
{
   public static final int MAX_SAMPLE_BYTES = 5 * 1024 * 1024;
   private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

   /*******************************************************************************
    ** Enforced reads preserve table permissions, field locks and partner record locks.
    *******************************************************************************/
   private static List<QRecord> query(String table, QQueryFilter filter) throws Exception
   {
      return (EdiRecords.queryEnforced(table, filter));
   }

   /*******************************************************************************
    ** A missing or invisible record has the same response.
    *******************************************************************************/
   private static QRecord row(String table, Integer id) throws Exception
   {
      if(id == null || id < 1)
      {
         throw (new QBadRequestException("A positive identifier is required"));
      }
      var records = query(table, EdiRecords.equal("id", id));
      if(records.isEmpty())
      {
         throw (new QNotFoundException("Not found"));
      }
      return (records.getFirst());
   }

   /*******************************************************************************
    ** Read a guide as a nested tree, preserving duplicate loop ids in separate parents.
    *******************************************************************************/
   public EdiGuideTreeV1 tree(Integer id) throws Exception
   {
      row("edi_guide", id);
      List<EdiLoopV1> roots = new ArrayList<>();
      List<EdiSegmentV1> rootSegments = new ArrayList<>();
      Map<String, EdiLoopV1> loops = new LinkedHashMap<>();
      Map<String, String> latestPaths = new LinkedHashMap<>();
      var segments = query("edi_guide_segment", EdiRecords.equal("guideId", id));
      segments.sort(Comparator.comparing(r -> r.getValueInteger("sequence")));
      for(var segment : segments)
      {
         List<EdiElementV1> elements = new ArrayList<>();
         var rows = query("edi_guide_element", EdiRecords.equal("guideSegmentId", segment.getValue("id")));
         rows.sort(Comparator.comparing((QRecord r) -> r.getValueInteger("position")).thenComparing(r -> Objects.requireNonNullElse(r.getValueInteger("componentPosition"), 0)));
         for(var element : rows)
         {
            List<String> codes = List.of();
            Integer codeList = element.getValueInteger("codeListId");
            if(codeList != null)
            {
               row("edi_code_list", codeList);
               codes = query("edi_code_list_value", EdiRecords.equal("codeListId", codeList)
                  .withCriteria("isActive", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, true))
                  .stream().map(r -> r.getValueString("code")).sorted().limit(200).toList();
            }
            elements.add(new EdiElementV1(element.getValueInteger("position"), element.getValueInteger("componentPosition"),
               element.getValueString("elementId"), element.getValueString("name"), element.getValueString("dataType"),
               element.getValueInteger("minLength"), element.getValueInteger("maxLength"), element.getValueString("usage"), codeList, codes));
         }
         EdiSegmentV1 node = new EdiSegmentV1(segment.getValueString("segmentId"), segment.getValueString("position"), segment.getValueString("usage"),
            segment.getValueInteger("minUse"), segment.getValueInteger("maxUse"), segment.getValueString("name"), elements);
         String loopId = segment.getValueString("loopId");
         if(loopId == null || loopId.isBlank())
         {
            rootSegments.add(node);
         }
         else
         {
            String parent = latestPaths.get(segment.getValueString("parentLoopId"));
            String path = (parent == null ? "" : parent + "/") + loopId;
            EdiLoopV1 loop = loops.get(path);
            if(loop == null)
            {
               loop = new EdiLoopV1(loopId, segment.getValueInteger("loopMaxRepeat"), new ArrayList<>(), new ArrayList<>());
               loops.put(path, loop);
               (parent == null ? roots : loops.get(parent).loops()).add(loop);
            }
            latestPaths.put(loopId, path);
            loop.segments().add(node);
         }
      }
      return (new EdiGuideTreeV1(roots, rootSegments));
   }

   /*******************************************************************************
    ** Return visible target metadata, rules and the same registry used by mapping.
    *******************************************************************************/
   public EdiTargetV1 target(Integer id) throws Exception
   {
      QRecord map = row("edi_map", id);
      QTableMetaData table = targetTable(map);
      List<EdiAssociationV1> associations = new ArrayList<>();
      if(table.getAssociations() != null)
      {
         for(var association : table.getAssociations())
         {
            QTableMetaData child = QContext.getQInstance().getTable(association.getAssociatedTableName());
            associations.add(new EdiAssociationV1(association.getName(), child.getName(), Objects.requireNonNullElse(child.getLabel(), association.getName()), fields(child)));
         }
      }
      List<EdiRuleV1> rules = new ArrayList<>();
      for(QRecord rule : query("edi_map_rule", EdiRecords.equal("mapId", id)))
      {
         rules.add(rule(rule));
      }
      rules.sort(Comparator.comparing(r -> Objects.requireNonNullElse(r.sequence(), 0)));
      return (new EdiTargetV1(table.getName(), fields(table), associations, rules,
         EdiRuntime.current().transforms().argumentSchemas().entrySet().stream().map(e -> new EdiTransformV1(e.getKey(), e.getValue())).toList()));
   }

   /*******************************************************************************
    ** Resolve the configured target without accepting a client-supplied table.
    *******************************************************************************/
   private QTableMetaData targetTable(QRecord map) throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(map.getValueString("tableName"));
      if(table == null)
      {
         throw (new QBadRequestException("The map target table is unavailable"));
      }
      return (table);
   }

   /*******************************************************************************
    ** Table metadata is only exposed with the table's read permission.
    *******************************************************************************/
   private List<EdiFieldV1> fields(QTableMetaData table) throws Exception
   {
      QueryInput input = new QueryInput(table.getName());
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      PermissionsHelper.checkTablePermissionThrowing(input, TablePermissionSubType.READ);
      return (table.getFields().values().stream().map(f -> new EdiFieldV1(f.getName(), f.getLabel(), f.getType().name(), f.getIsRequired(), f.getPossibleValueSourceName())).toList());
   }

   /*******************************************************************************
    ** Copy only documented rule fields; do not leak persistence or diagnostic fields.
    *******************************************************************************/
   private EdiRuleV1 rule(QRecord r)
   {
      return (new EdiRuleV1(r.getValueInteger("id"), r.getValueInteger("sequence"), r.getValueString("ediPath"), r.getValueString("qqqPath"),
         r.getValueString("transform"), r.getValueString("transformArgs"), r.getValueBoolean("required"), r.getValueString("condition"), r.getValueString("defaultValue")));
   }

   /*******************************************************************************
    ** Convert unsaved edits to detached records; mapId is never accepted from a rule.
    *******************************************************************************/
   private List<QRecord> records(List<EdiRuleV1> rules) throws Exception
   {
      if(rules == null || rules.size() > 10000 || rules.stream().anyMatch(Objects::isNull))
      {
         throw (new QBadRequestException("Supply an array of at most 10000 rules"));
      }
      List<QRecord> result = new ArrayList<>();
      for(EdiRuleV1 rule : rules)
      {
         result.add(new QRecord().withValue("id", rule.id()).withValue("sequence", rule.sequence()).withValue("ediPath", rule.ediPath())
            .withValue("qqqPath", rule.qqqPath()).withValue("transform", rule.transform()).withValue("transformArgs", rule.transformArgs())
            .withValue("required", rule.required()).withValue("condition", rule.condition()).withValue("defaultValue", rule.defaultValue()));
      }
      return (result);
   }

   /*******************************************************************************
    ** Validate all submitted rules, with stable index correlation and no persistence.
    *******************************************************************************/
   public EdiValidationV1 validate(Integer id, EdiValidateRequestV1 request) throws Exception
   {
      QRecord map = row("edi_map", id);
      targetTable(map);
      var guide = MapPreviewService.guide(map, "ORIGINAL");
      List<QRecord> rules = records(request.rules());
      List<EdiRuleErrorsV1> results = new ArrayList<>();
      for(int i = 0; i < rules.size(); i++)
      {
         QRecord rule = rules.get(i);
         results.add(new EdiRuleErrorsV1(i, rule.getValueInteger("id"), rule.getValueInteger("sequence"),
            new MapRuleValidator().validate(rule, map, guide, EdiRuntime.current().transforms())));
      }
      return (new EdiValidationV1(results));
   }

   /*******************************************************************************
    ** Authorize before decoding or saving an upload; previews never change map rules.
    *******************************************************************************/
   public EdiPreviewV1 preview(EdiPreviewRequestV1 request) throws Exception
   {
      QRecord map = row("edi_map", request.mapId());
      AbstractActionInput permission = new AbstractActionInput();
      permission.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      PermissionsHelper.checkProcessPermissionThrowing(permission, "edi_map_preview");
      String revision = Objects.requireNonNullElse(request.guideRevision(), "CURRENT");
      if(!List.of("CURRENT", "ORIGINAL").contains(revision))
      {
         throw (new QBadRequestException("guideRevision must be CURRENT or ORIGINAL"));
      }
      if((request.sampleId() == null) == (request.sampleBase64() == null))
      {
         throw (new QBadRequestException("Supply exactly one of sampleId and sampleBase64"));
      }
      List<QRecord> overrides = request.ruleOverrides() == null ? null : records(request.ruleOverrides());
      QRecord sample;
      if(request.sampleId() != null)
      {
         sample = row("edi_map_sample", request.sampleId());
         if(!Objects.equals(sample.getValueInteger("mapId"), request.mapId()))
         {
            throw (new QNotFoundException("Not found"));
         }
      }
      else
      {
         String encoded = request.sampleBase64();
         if(encoded.length() > ((MAX_SAMPLE_BYTES + 2) / 3) * 4)
         {
            throw (new QBadRequestException("Sample exceeds the 5 MiB limit"));
         }
         byte[] bytes;
         try
         {
            bytes = Base64.getDecoder().decode(encoded);
         }
         catch(IllegalArgumentException e)
         {
            throw (new QBadRequestException("sampleBase64 must be valid base64"));
         }
         if(bytes.length == 0 || bytes.length > MAX_SAMPLE_BYTES)
         {
            throw (new QBadRequestException("Sample must contain 1 byte through 5 MiB"));
         }
         if(request.sampleName() != null && request.sampleName().length() > 255)
         {
            throw (new QBadRequestException("sampleName must be at most 255 characters"));
         }
         // As in the record action, only encrypted sample archival uses the EDI writer.
         sample = SystemWrites.call(() ->
         {
            var ref = EdiRuntime.current().payloads().put(PayloadKind.SAMPLE, new ByteArrayInputStream(bytes));
            try
            {
               return (SystemWrites.insert("edi_map_sample", new QRecord().withValue("mapId", request.mapId())
                  .withValue("name", Objects.requireNonNullElse(request.sampleName(), "Synthetic upload")).withValue("payloadRef", ref.value())));
            }
            catch(Exception e)
            {
               EdiRuntime.current().payloads().delete(ref);
               throw (e);
            }
         });
      }
      return (JSON.convertValue(new MapPreviewService().preview(map, sample, overrides, revision), EdiPreviewV1.class));
   }
}
