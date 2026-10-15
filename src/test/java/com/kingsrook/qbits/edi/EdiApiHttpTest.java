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

package com.kingsrook.qbits.edi;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qbits.edi.api.EdiApiService;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Real HTTP against strict-mode QQQ, real SQL, real encrypted samples and caller sessions.
 *******************************************************************************/
class EdiApiHttpTest
{
   private static final ObjectMapper JSON = new ObjectMapper();
   private QApplicationJavalinServer server;
   private HttpClient client;
   private QInstance instance;
   private QRecord map;
   private QRecord sample;
   private String baseUrl;
   private String suffix;

   /*******************************************************************************
    ** The existing synthetic vertical-slice fixture supplies tables, guide and map.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      new InboundTest().setup("004010");
      instance = QContext.getQInstance();
      map = EdiRecords.query("edi_map", new QQueryFilter()).getFirst();
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_map", new QRecord().withValue("id", map.getValue("id")).withValue("partnerId", 1));
         SystemWrites.update("edi_guide", new QRecord().withValue("id", map.getValue("guideId")).withValue("partnerId", 1));
         return (null);
      });
      map.withValue("partnerId", 1);
      sample = saveSample(Synthetic850.generate(17, "004010", com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT), map.getValueInteger("id"));
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK)
         .withCustomizer(new QCodeReference(Sessions.class)));
      int port;
      try(ServerSocket socket = new ServerSocket(0))
      {
         port = socket.getLocalPort();
      }
      baseUrl = "http://127.0.0.1:" + port + "/qqq/v1/qbit/edi";
      suffix = "-" + UUID.randomUUID();
      client = HttpClient.newHttpClient();
      server = new QApplicationJavalinServer(new AbstractQQQApplication()
      {
         @Override
         public QInstance defineQInstance()
         {
            return (instance);
         }
      }).withPort(port).withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false).withStrictUiApiRoutes(true)
         .withJavalinConfigCustomizer(config -> config.http.maxRequestSize = 8 * 1024 * 1024);
      server.start();
      QContext.init(instance, new QSystemUserSession(instance));
   }

   /*******************************************************************************
    ** Release server globals and thread context.
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      if(server != null)
      {
         server.stop();
      }
      if(client != null)
      {
         client.close();
      }
      QJavalinImplementation.setSessionCookieHttpOnly(false);
      QContext.clear();
   }

   /*******************************************************************************
    ** Ordinary users receive selected reads; none receives raw-payload access.
    *******************************************************************************/
   public static class Sessions implements QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         String role = session.getUuid().split("-")[0];
         session.withUser(new QUser().withIdReference(role));
         session.withSecurityKeyValue("edi_partner", 1);
         if(!role.equals("denied"))
         {
            for(String table : instance.getTables().keySet())
            {
               if(!table.equals("edi_payload") && !table.equals("edi_preview_download")
                  && !(role.equals("noguide") && table.equals("edi_guide"))
                  && !(role.equals("norules") && table.equals("edi_map_rule"))
                  && !(role.equals("notarget") && table.equals("sample_order")))
               {
                  String permission = table.startsWith("edi_") ? "edi." + table.substring(4) + ".read"
                     : table.equals("sample_order") ? "sample.order.read" : "sample.line.read";
                  session.withPermissions(permission);
               }
            }
            if(!role.equals("reader"))
            {
               session.withPermissions("edi.map.preview");
            }
         }
      }
   }

   /*******************************************************************************
    ** Transport helper retains the exact response for egress scans.
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path, Object body, String role) throws Exception
   {
      String text = body == null ? "" : body instanceof String s ? s : JSON.writeValueAsString(body);
      var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Cookie", "sessionUUID=" + role + suffix)
         .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(text)).build(), HttpResponse.BodyHandlers.ofString());
      if(response.statusCode() >= 400)
      {
         JsonNode error = JSON.readTree(response.body());
         for(String field : List.of("error", "code", "message", "traceId"))
         {
            assertThat(error.path(field).isTextual()).as(field + " in " + response.body()).isTrue();
            assertThat(error.path(field).asText()).isNotBlank();
         }
      }
      return (response);
   }

   /*******************************************************************************
    ** Assert success before parsing the wire JSON.
    *******************************************************************************/
   private JsonNode ok(String method, String path, Object body) throws Exception
   {
      var response = request(method, path, body, "user");
      assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
      return (JSON.readTree(response.body()));
   }

   /*******************************************************************************
    ** An authorized fixture writer archives synthetic bytes only.
    *******************************************************************************/
   private QRecord saveSample(String text, Integer mapId) throws Exception
   {
      return (SystemWrites.call(() ->
      {
         var ref = EdiRuntime.current().payloads().put(PayloadKind.SAMPLE, InboundTest.bytes(text));
         return (EdiRecords.insert("edi_map_sample", new QRecord().withValue("name", "Synthetic HTTP sample").withValue("mapId", mapId).withValue("payloadRef", ref.value())));
      }));
   }

   /*******************************************************************************
    ** Shared stored-sample request.
    *******************************************************************************/
   private Map<String, Object> preview()
   {
      return (Map.of("mapId", map.getValueInteger("id"), "sampleId", sample.getValueInteger("id")));
   }

   /*******************************************************************************
    ** Guide structure and target schema include associations, rules and argument contracts.
    *******************************************************************************/
   @Test
   void guideAndTarget() throws Exception
   {
      var tree = ok("GET", "/guides/" + map.getValue("guideId") + "/tree", null);
      assertThat(tree.get("loops")).isNotEmpty();
      assertThat(tree.toString()).contains("\"id\":\"BEG\"", "\"position\":\"0030\"", "codeListId", "minLength");
      var target = ok("GET", "/maps/" + map.getValue("id") + "/target", null);
      assertThat(target.get("table").asText()).isEqualTo("sample_order");
      assertThat(target.get("fields").toString()).contains("poNumber", "possibleValueSourceName");
      assertThat(target.get("associations").toString()).contains("lines", "sample_order_line", "quantity");
      assertThat(target.get("rules")).hasSize(5);
      assertThat(target.get("transforms").toString()).contains("LOOKUP", "argumentSchema", "required", "table", "key");
   }

   /*******************************************************************************
    ** Each operation returns real 403 responses for denied table or action permissions.
    *******************************************************************************/
   @Test
   void permissions() throws Exception
   {
      String target = "/maps/" + map.getValue("id") + "/target";
      String validate = "/maps/" + map.getValue("id") + "/validate";
      assertThat(request("GET", "/guides/" + map.getValue("guideId") + "/tree", null, "denied").statusCode()).isEqualTo(403);
      assertThat(request("GET", target, null, "denied").statusCode()).isEqualTo(403);
      assertThat(request("POST", validate, Map.of("rules", List.of()), "denied").statusCode()).isEqualTo(403);
      assertThat(request("POST", "/sandbox/preview", preview(), "denied").statusCode()).isEqualTo(403);
      assertThat(request("POST", "/sandbox/preview", preview(), "reader").statusCode()).isEqualTo(403);
      assertThat(request("GET", target, null, "notarget").statusCode()).isEqualTo(403);
      assertThat(request("GET", target, null, "norules").statusCode()).isEqualTo(403);
      assertThat(request("GET", "/guides/" + map.getValue("guideId") + "/tree", null, "noguide").statusCode()).isEqualTo(403);
   }

   /*******************************************************************************
    ** Preview returns the mapped structure and ack while leaving target tables untouched.
    *******************************************************************************/
   @Test
   void storedPreview() throws Exception
   {
      var result = ok("POST", "/sandbox/preview", preview());
      assertThat(result.at("/parse/interchanges").asInt()).isEqualTo(1);
      assertThat(result.at("/parse/groups").asInt()).isEqualTo(1);
      assertThat(result.at("/parse/documents").asInt()).isEqualTo(1);
      assertThat(result.at("/result/records/0/values/poNumber").asText()).isNotBlank();
      assertThat(result.at("/result/records/0/associations/lines")).hasSize(1);
      assertThat(result.at("/ack/type").asText()).isEqualTo("997");
      assertThat(result.at("/ack/text").asText()).contains("AK9");
      assertThat(result.get("phiSuppressed").asBoolean()).isFalse();
      assertThat(EdiRecords.query("sample_order", new QQueryFilter())).isEmpty();
   }

   /*******************************************************************************
    ** Unsaved overrides replace rules in memory and custom transforms see the caller.
    *******************************************************************************/
   @Test
   void unsavedOverridesAndCallerSession() throws Exception
   {
      EdiRuntime.current().transforms().register("SESSION", (values, args) -> QContext.getQSession().getUser().getIdReference(),
         Map.of("type", "object", "properties", Map.of(), "required", List.of()));
      var rule = Map.of("sequence", 1, "ediPath", "BEG03", "qqqPath", "poNumber", "transform", "SESSION");
      var body = new java.util.LinkedHashMap<>(preview());
      body.put("ruleOverrides", List.of(rule));
      var result = ok("POST", "/sandbox/preview", body);
      assertThat(result.at("/result/records/0/values/poNumber").asText()).isEqualTo("user");
      assertThat(EdiRecords.query("edi_map_rule", EdiRecords.equal("mapId", map.getValue("id")))).hasSize(5);
      assertThat(ok("POST", "/sandbox/preview", preview()).at("/result/records/0/values/poNumber").asText()).isNotEqualTo("user");
      body.put("ruleOverrides", List.of());
      assertThat(ok("POST", "/sandbox/preview", body).at("/result/records/0/values").size()).isZero();
   }

   /*******************************************************************************
    ** Base64 uploads become encrypted samples, with no plaintext in the payload table.
    *******************************************************************************/
   @Test
   void upload() throws Exception
   {
      String raw = Synthetic850.generate(18, "004010", com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT);
      int count = EdiRecords.query("edi_map_sample", new QQueryFilter()).size();
      var result = ok("POST", "/sandbox/preview", Map.of("mapId", map.getValueInteger("id"), "sampleBase64",
         Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.US_ASCII)), "sampleName", "Uploaded sample"));
      assertThat(result.at("/parse/documents").asInt()).isEqualTo(1);
      var samples = EdiRecords.query("edi_map_sample", new QQueryFilter());
      assertThat(samples).hasSize(count + 1);
      var uploaded = samples.stream().filter(r -> "Uploaded sample".equals(r.getValueString("name"))).findFirst().orElseThrow();
      try(var stream = EdiRuntime.current().payloads().open(new PayloadRef(uploaded.getValueString("payloadRef"), null, 0)))
      {
         assertThat(new String(stream.readAllBytes(), StandardCharsets.US_ASCII)).isEqualTo(raw);
      }
      for(var payload : EdiRecords.query("edi_payload", new QQueryFilter()))
      {
         assertThat(new String((byte[]) payload.getValue("ciphertext"), StandardCharsets.ISO_8859_1)).doesNotContain("SYNTH-1", raw);
      }
   }

   /*******************************************************************************
    ** Reject oversized decoded bytes as well as encoded lengths before persistence.
    *******************************************************************************/
   @Test
   void uploadLimitAndBadRequests() throws Exception
   {
      int count = EdiRecords.query("edi_map_sample", new QQueryFilter()).size();
      for(int size : List.of(EdiApiService.MAX_SAMPLE_BYTES + 1, EdiApiService.MAX_SAMPLE_BYTES + 4))
      {
         var response = request("POST", "/sandbox/preview", Map.of("mapId", map.getValueInteger("id"), "sampleBase64",
            Base64.getEncoder().encodeToString(new byte[size])), "user");
         assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
      }
      for(Object body : List.of(Map.of("mapId", map.getValueInteger("id"), "sampleBase64", "!invalid!"),
         Map.of("mapId", map.getValueInteger("id")), Map.of("mapId", map.getValueInteger("id"), "sampleId", sample.getValueInteger("id"), "sampleBase64", "AAAA"),
         Map.of("mapId", map.getValueInteger("id"), "sampleId", sample.getValueInteger("id"), "guideRevision", "BAD"),
         "{\"mapId\":1.2}", "{\"mapId\":\"1\"}", "{\"mapId\":1,\"ruleOverrides\":[null]}", "[]", "null", "invalid"))
      {
         assertThat(request("POST", "/sandbox/preview", body, "user").statusCode()).isEqualTo(400);
      }
      assertThat(EdiRecords.query("edi_map_sample", new QQueryFilter())).hasSize(count);
      assertThat(request("GET", "/maps/invalid/target", null, "user").statusCode()).isEqualTo(400);
      assertThat(request("GET", "/guides/999999/tree", null, "user").statusCode()).isEqualTo(404);
   }

   /*******************************************************************************
    ** PHI is detected through hidden agreement data, and canaries never reach responses.
    *******************************************************************************/
   @Test
   void phiSuppression() throws Exception
   {
      String canary = "CANARY_PRIVATE_VALUE";
      SystemWrites.call(() ->
      {
         var document = EdiRecords.query("edi_agreement_document", EdiRecords.equal("mapId", map.getValue("id"))).getFirst();
         SystemWrites.update("edi_agreement_document", new QRecord().withValue("id", document.getValue("id")).withValue("containsPhi", true));
         return (null);
      });
      var body = new java.util.LinkedHashMap<>(preview());
      body.put("ruleOverrides", List.of(Map.of("sequence", 1, "qqqPath", "poNumber", "transform", "CONSTANT", "transformArgs", "{\"value\":\"" + canary + "\"}")));
      var response = request("POST", "/sandbox/preview", body, "user");
      assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
      assertThat(response.body()).doesNotContain(canary, "SYNTH-1", "poNumber");
      var json = JSON.readTree(response.body());
      assertThat(json.get("phiSuppressed").asBoolean()).isTrue();
      assertThat(json.get("result").isNull()).isTrue();
      assertThat(json.at("/ack/text").isNull()).isTrue();
      assertThat(json.at("/diff/againstExpected")).isEmpty();
      assertThat(json.at("/diff/againstPreviousRevision")).isEmpty();
   }

   /*******************************************************************************
    ** Rule errors are independently correlated; valid paths and transforms succeed.
    *******************************************************************************/
   @Test
   void validateRules() throws Exception
   {
      var rules = ok("GET", "/maps/" + map.getValue("id") + "/target", null).get("rules");
      var valid = ok("POST", "/maps/" + map.getValue("id") + "/validate", Map.of("rules", rules));
      assertThat(valid.get("rules")).hasSize(5);
      valid.get("rules").forEach(rule -> assertThat(rule.get("errors")).as(rule.toString()).isEmpty());
      var invalid = List.of(
         Map.of("sequence", 1, "ediPath", "BAD99", "qqqPath", "unknown", "transform", "NO_SUCH_TRANSFORM"),
         Map.of("sequence", 2, "ediPath", "BEG03", "qqqPath", "poNumber", "transform", "LOOKUP", "transformArgs", "{}"),
         Map.of("sequence", 3, "ediPath", "PO1[*]/PO199", "qqqPath", "lines.sku", "transformArgs", "[]"),
         Map.of("sequence", 4, "ediPath", "BEG03", "qqqPath", "lines.sku", "transformArgs", "{\"index\":1}"),
         Map.of("sequence", 0, "qqqPath", "poNumber"));
      var errors = ok("POST", "/maps/" + map.getValue("id") + "/validate", Map.of("rules", invalid));
      assertThat(errors.at("/rules/0/errors")).hasSize(3);
      assertThat(errors.at("/rules/1/errors").toString()).contains("table", "key");
      assertThat(errors.at("/rules/2/errors").toString()).contains("ediPath", "transformArgs");
      assertThat(errors.at("/rules/3/errors").toString()).contains("wildcard", "transformArgs");
      assertThat(errors.at("/rules/4/errors").toString()).contains("sequence", "ediPath");
      assertThat(request("POST", "/maps/" + map.getValue("id") + "/validate", Map.of(), "user").statusCode()).isEqualTo(400);
   }

   /*******************************************************************************
    ** Existing samples must belong to the selected map, and partner locks apply first.
    *******************************************************************************/
   @Test
   void sampleAndPartnerIsolation() throws Exception
   {
      QRecord another = SystemWrites.call(() -> EdiRecords.insert("edi_map", new QRecord().withValue("name", "Other map").withValue("partnerId", 1)
         .withValue("guideId", map.getValue("guideId")).withValue("tableName", "sample_order").withValue("direction", "INBOUND").withValue("transactionSet", "850")));
      assertThat(request("POST", "/sandbox/preview", Map.of("mapId", another.getValueInteger("id"), "sampleId", sample.getValueInteger("id")), "user").statusCode()).isEqualTo(404);
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_map", new QRecord().withValue("id", map.getValue("id")).withValue("partnerId", 2));
         return (null);
      });
      assertThat(request("GET", "/maps/" + map.getValue("id") + "/target", null, "user").statusCode()).isEqualTo(404);
      assertThat(request("POST", "/sandbox/preview", preview(), "user").statusCode()).isEqualTo(404);
   }

   /*******************************************************************************
    ** A runtime transform failure returns a safe diagnostic instead of an exception value.
    *******************************************************************************/
   @Test
   void mappingFailureIsValueFree() throws Exception
   {
      EdiRuntime.current().transforms().register("FAIL", (values, args) ->
      {
         throw new IllegalArgumentException("SECRET_CANARY");
      });
      var body = new java.util.LinkedHashMap<>(preview());
      body.put("ruleOverrides", List.of(Map.of("sequence", 1, "ediPath", "BEG03", "qqqPath", "poNumber", "transform", "FAIL")));
      var response = request("POST", "/sandbox/preview", body, "user");
      assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
      assertThat(response.body()).doesNotContain("SECRET_CANARY").contains("\"code\":\"MAP\"");
   }

   /*******************************************************************************
    ** The exact decoded limit is accepted, including base64 expansion.
    *******************************************************************************/
   @Test
   void exactUploadLimit() throws Exception
   {
      String raw = Synthetic850.generate(19, "004010", com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT);
      String padded = raw + " ".repeat(EdiApiService.MAX_SAMPLE_BYTES - raw.length());
      var result = ok("POST", "/sandbox/preview", Map.of("mapId", map.getValueInteger("id"), "sampleBase64",
         Base64.getEncoder().encodeToString(padded.getBytes(StandardCharsets.US_ASCII))));
      assertThat(result.at("/parse/documents").asInt()).isEqualTo(1);
      assertThat(EdiRecords.query("edi_payload", new QQueryFilter())).anyMatch(r -> r.getValueLong("sizeBytes") == EdiApiService.MAX_SAMPLE_BYTES);
   }

   /*******************************************************************************
    ** Required LOOKUP permissions are not borrowed from the configured service session.
    *******************************************************************************/
   @Test
   void lookupCannotElevate() throws Exception
   {
      var body = new java.util.LinkedHashMap<>(preview());
      body.put("ruleOverrides", List.of(Map.of("sequence", 1, "ediPath", "BEG03", "qqqPath", "poNumber", "transform", "LOOKUP",
         "transformArgs", "{\"table\":\"sample_order\",\"key\":\"poNumber\"}")));
      var response = request("POST", "/sandbox/preview", body, "notarget");
      assertThat(response.statusCode()).as(response.body()).isEqualTo(403);
      assertThat(request("POST", "/sandbox/preview", preview(), "norules").statusCode()).isEqualTo(403);
      assertThat(request("POST", "/maps/" + map.getValue("id") + "/validate", Map.of("rules", List.of()), "noguide").statusCode()).isEqualTo(403);
   }

   /*******************************************************************************
    ** A later active guide changes CURRENT findings while ORIGINAL keeps the pinned guide.
    *******************************************************************************/
   @Test
   void guideRevisionSelection() throws Exception
   {
      var repository = new com.kingsrook.qbits.edi.guide.GuideRepository();
      var guide = repository.load(map.getValueInteger("guideId"));
      var segments = new ArrayList<>(guide.segments());
      segments.add(segments.size() - 1, new com.kingsrook.qbits.edi.guide.GuideSegment("XYZ", null, null, 1, "REQUIRED", 1, 1, List.of(), List.of(), null));
      var changed = new com.kingsrook.qbits.edi.guide.Guide(guide.name(), guide.standard(), guide.version(), guide.transactionSet(), guide.functionalIdCode(), segments, guide.rules());
      SystemWrites.call(() ->
      {
         Integer id = repository.save(changed, "synthetic-http-revision", 2);
         SystemWrites.update("edi_guide", new QRecord().withValue("id", id).withValue("partnerId", 1));
         return (null);
      });
      var body = new java.util.LinkedHashMap<>(preview());
      body.put("guideRevision", "ORIGINAL");
      var original = ok("POST", "/sandbox/preview", body);
      body.put("guideRevision", "CURRENT");
      var current = ok("POST", "/sandbox/preview", body);
      assertThat(current.get("findings").size()).isGreaterThan(original.get("findings").size());
      assertThat(current.get("findings").toString()).contains("XYZ");
   }

   /*******************************************************************************
    ** Previous-revision and expected diffs identify the changed document and values.
    *******************************************************************************/
   @Test
   void revisionAndExpectedDiffs() throws Exception
   {
      QRecord revised = SystemWrites.call(() ->
      {
         QRecord next = new QRecord().withValue("revision", 2).withValue("partnerId", 1).withValue("guideId", map.getValue("guideId"));
         for(String field : List.of("name", "direction", "standard", "version", "transactionSet", "tableName", "status"))
         {
            next.withValue(field, map.getValue(field));
         }
         return (EdiRecords.insert("edi_map", next));
      });
      var nextSample = saveSample(Synthetic850.generate(17, "004010", com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT), revised.getValueInteger("id"));
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_map_sample", new QRecord().withValue("id", nextSample.getValue("id")).withValue("expectedResultJson", "{\"values\":{\"poNumber\":\"EXPECTED\"},\"associations\":{}}"));
         return (null);
      });
      var result = ok("POST", "/sandbox/preview", Map.of("mapId", revised.getValueInteger("id"), "sampleId", nextSample.getValueInteger("id"),
         "ruleOverrides", List.of(Map.of("sequence", 1, "qqqPath", "poNumber", "transform", "CONSTANT", "transformArgs", "{\"value\":\"EDITED\"}"))));
      assertThat(result.at("/diff/againstPreviousRevision")).hasSize(1);
      assertThat(result.at("/diff/againstExpected")).hasSize(1);
      assertThat(result.at("/diff/againstPreviousRevision/0/after/values/poNumber").asText()).isEqualTo("EDITED");
      assertThat(result.at("/diff/againstExpected/0/before/values/poNumber").asText()).isEqualTo("EXPECTED");
   }

   /*******************************************************************************
    ** Large code lists are deterministically capped in the guide-tree projection.
    *******************************************************************************/
   @Test
   void codeListLimit() throws Exception
   {
      var definitions = EdiRecords.query("edi_guide_segment", EdiRecords.equal("guideId", map.getValue("guideId")));
      Integer codeListId = null;
      for(var definition : definitions)
      {
         for(var element : EdiRecords.query("edi_guide_element", EdiRecords.equal("guideSegmentId", definition.getValue("id"))))
         {
            if(element.getValueInteger("codeListId") != null)
            {
               codeListId = element.getValueInteger("codeListId");
               break;
            }
         }
         if(codeListId != null)
         {
            break;
         }
      }
      final Integer listId = codeListId;
      SystemWrites.call(() ->
      {
         for(int i = 0; i < 205; i++)
         {
            EdiRecords.insert("edi_code_list_value", new QRecord().withValue("codeListId", listId).withValue("code", "SYNTH" + i).withValue("isActive", true));
         }
         return (null);
      });
      var tree = ok("GET", "/guides/" + map.getValue("guideId") + "/tree", null);
      assertThat(tree.findValues("codes")).allSatisfy(codes -> assertThat(codes.size()).isLessThanOrEqualTo(200));
      assertThat(tree.findValues("codes")).anyMatch(codes -> codes.size() == 200);
   }

   /*******************************************************************************
    ** Transaction and interchange envelope errors are represented in findings and acknowledgements.
    *******************************************************************************/
   @Test
   void envelopeFindings() throws Exception
   {
      String raw = Synthetic850.generate(20, "004010", com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT);
      var invalid = saveSample(raw.replace("SE*9*", "SE*99*").replace("IEA*1*", "IEA*2*"), map.getValueInteger("id"));
      var response = ok("POST", "/sandbox/preview", Map.of("mapId", map.getValueInteger("id"), "sampleId", invalid.getValueInteger("id")));
      assertThat(response.get("findings")).hasSizeGreaterThanOrEqualTo(2);
      assertThat(response.get("findings").toString()).contains("\"snipLevel\":1");
      assertThat(response.at("/ack/type").asText()).isEqualTo("MIXED");
      assertThat(response.at("/ack/text").asText()).contains("TA1*", "ISA*", "AK9*");
   }

   /*******************************************************************************
    ** A caller cannot inspect another partner's guide via a visible map.
    *******************************************************************************/
   @Test
   void hiddenGuide() throws Exception
   {
      SystemWrites.call(() ->
      {
         SystemWrites.update("edi_guide", new QRecord().withValue("id", map.getValue("guideId")).withValue("partnerId", 2));
         return (null);
      });
      assertThat(request("GET", "/guides/" + map.getValue("guideId") + "/tree", null, "user").statusCode()).isEqualTo(404);
      assertThat(request("POST", "/sandbox/preview", preview(), "user").statusCode()).isEqualTo(404);
      assertThat(request("POST", "/maps/" + map.getValue("id") + "/validate", Map.of("rules", List.of()), "user").statusCode()).isEqualTo(404);
   }
}
