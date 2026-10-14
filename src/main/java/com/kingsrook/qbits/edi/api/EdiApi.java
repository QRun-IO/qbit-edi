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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.HttpStatus;

/*******************************************************************************
 ** The EDI 1.0.0 UI API: four spec'd operations in the default qbit namespace.
 *******************************************************************************/
public final class EdiApi
{
   /*******************************************************************************
    ** Static registration only.
    *******************************************************************************/
   private EdiApi()
   {
   }

   /*******************************************************************************
    ** Install with optional Javalin from the qbit producer.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      MiddlewareSpecExtensionsMetaData.ofOrWithNew(instance).withExtension(extension());
   }

   /*******************************************************************************
    ** Shared descriptor for route binding and the committed contract.
    *******************************************************************************/
   public static MiddlewareSpecExtension extension()
   {
      return (new MiddlewareSpecExtension().withName("edi").withSemanticVersion("1.0.0")
         .withDescription("EDI sandbox and mapper, using the signed-in user's permissions.")
         .withSpec(GuideTreeSpec.class).withSpec(MapTargetSpec.class).withSpec(PreviewSpec.class).withSpec(ValidateSpec.class));
   }

   /*******************************************************************************
    ** Common typed JSON contract; core owns authentication and error mapping.
    *******************************************************************************/
   public abstract static class EdiSpec extends AbstractJsonEndpointSpec
   {
      private final String path;
      private final String response;
      private final String request;
      private final String summary;

      /*******************************************************************************
       ** Immutable operation metadata.
       *******************************************************************************/
      protected EdiSpec(String path, String response, String request, String summary)
      {
         this.path = path;
         this.response = response;
         this.request = request;
         this.summary = summary;
      }

      /*******************************************************************************
       ** Publish the method, path and permission expectations.
       *******************************************************************************/
      @Override
      public BasicOperation defineBasicOperation()
      {
         return (new BasicOperation().withPath("/qbit/edi" + path).withHttpMethod(request == null ? HttpMethod.GET : HttpMethod.POST)
            .withShortSummary(summary).withLongDescription(summary + ". Uses the caller session and table read permissions. Preview also requires edi.map.preview. Rule writes use core table CRUD."));
      }

      /*******************************************************************************
       ** Declare positive integer record identifiers.
       *******************************************************************************/
      @Override
      public List<Parameter> defineRequestParameters()
      {
         String name = path.contains("{guideId}") ? "guideId" : path.contains("{mapId}") ? "mapId" : null;
         return (name == null ? List.of() : List.of(new Parameter().withName(name).withIn(In.PATH).withRequired(true)
            .withDescription("Positive record identifier.").withSchema(new Schema().withType(Type.INTEGER))));
      }

      /*******************************************************************************
       ** All schemas use the Edi prefix and V1 suffix.
       *******************************************************************************/
      @Override
      public Map<String, Schema> defineComponentSchemas()
      {
         return (path.startsWith("/guides/") ? EdiSchemas.all() : Map.of());
      }

      /*******************************************************************************
       ** Response component name.
       *******************************************************************************/
      @Override
      protected String getResponseSchemaName()
      {
         return (response);
      }

      /*******************************************************************************
       ** Optional request body contract.
       *******************************************************************************/
      @Override
      protected Schema defineRequestBodySchema()
      {
         return (request == null ? null : EdiSchemas.ref(request));
      }

      /*******************************************************************************
       ** Core middleware errors are explicitly documented.
       *******************************************************************************/
      @Override
      public List<BasicResponse> defineAdditionalBasicResponses()
      {
         return (List.of(new BasicResponse(HttpStatus.BAD_REQUEST, "Invalid request or sample exceeds 5 MiB", "EdiErrorV1"),
            new BasicResponse(HttpStatus.UNAUTHORIZED, "Sign in required", "EdiErrorV1"),
            new BasicResponse(HttpStatus.FORBIDDEN, "Permission denied", "EdiErrorV1"),
            new BasicResponse(HttpStatus.NOT_FOUND, "Missing or invisible record", "EdiErrorV1"),
            new BasicResponse(HttpStatus.CONTENT_TOO_LARGE, "Host HTTP request size limit exceeded", "EdiErrorV1")));
      }

      /*******************************************************************************
       ** Reject malformed or overflowing identifiers without echoing request content.
       *******************************************************************************/
      protected Integer id(JsonInput input, String name) throws QBadRequestException
      {
         try
         {
            int value = Integer.parseInt(input.getPathParam(name));
            if(value < 1)
            {
               throw (new NumberFormatException());
            }
            return (value);
         }
         catch(NumberFormatException e)
         {
            throw (new QBadRequestException("A positive integer identifier is required"));
         }
      }

      /*******************************************************************************
       ** Strict scalar typing prevents coercion of unsaved edits and sample identifiers.
       *******************************************************************************/
      protected <T> T body(JsonInput input, Class<T> type) throws QBadRequestException
      {
         ObjectMapper mapper = JsonMapper.builder().disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).build();
         mapper.coercionConfigFor(LogicalType.Textual).setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail).setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
         try
         {
            if(input.getBody() == null)
            {
               throw (new IllegalArgumentException("Object body required"));
            }
            return (mapper.convertValue(input.getBody(), type));
         }
         catch(IllegalArgumentException e)
         {
            throw (new QBadRequestException("Invalid body; check property names and types"));
         }
      }
   }

   /*******************************************************************************
    ** Read the guide tree.
    *******************************************************************************/
   public static class GuideTreeSpec extends EdiSpec
   {
      /*******************************************************************************
       ** Public constructor for core spec discovery.
       *******************************************************************************/
      public GuideTreeSpec()
      {
         super("/guides/{guideId}/tree", "EdiGuideTreeV1", null, "Read the guide tree");
      }

      /*******************************************************************************
       ** Execute with the authenticated caller.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (new EdiApiService().tree(id(input, "guideId")));
      }
   }

   /*******************************************************************************
    ** Read target metadata and map rules.
    *******************************************************************************/
   public static class MapTargetSpec extends EdiSpec
   {
      /*******************************************************************************
       ** Public constructor for core spec discovery.
       *******************************************************************************/
      public MapTargetSpec()
      {
         super("/maps/{mapId}/target", "EdiTargetV1", null, "Read target metadata and map rules");
      }

      /*******************************************************************************
       ** Execute with the authenticated caller.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (new EdiApiService().target(id(input, "mapId")));
      }
   }

   /*******************************************************************************
    ** Preview an encrypted sample with optional unsaved rules.
    *******************************************************************************/
   public static class PreviewSpec extends EdiSpec
   {
      /*******************************************************************************
       ** Public constructor for core spec discovery.
       *******************************************************************************/
      public PreviewSpec()
      {
         super("/sandbox/preview", "EdiPreviewV1", "EdiPreviewRequestV1", "Preview an encrypted sample with optional unsaved rules");
      }

      /*******************************************************************************
       ** Execute with the authenticated caller.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (new EdiApiService().preview(body(input, EdiSchemas.EdiPreviewRequestV1.class)));
      }
   }

   /*******************************************************************************
    ** Validate unsaved map rules.
    *******************************************************************************/
   public static class ValidateSpec extends EdiSpec
   {
      /*******************************************************************************
       ** Public constructor for core spec discovery.
       *******************************************************************************/
      public ValidateSpec()
      {
         super("/maps/{mapId}/validate", "EdiValidationV1", "EdiValidateRequestV1", "Validate unsaved map rules");
      }

      /*******************************************************************************
       ** Execute with the authenticated caller.
       *******************************************************************************/
      @Override
      protected Object handle(JsonInput input) throws Exception
      {
         return (new EdiApiService().validate(id(input, "mapId"), body(input, EdiSchemas.EdiValidateRequestV1.class)));
      }
   }
}
