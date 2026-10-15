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

import java.nio.file.Files;
import java.nio.file.Path;
import com.kingsrook.qbits.edi.api.EdiApi;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContract;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Generated extension fragment is the exact versioned contract shipped to clients.
 *******************************************************************************/
class EdiApiContractTest
{
   /*******************************************************************************
    ** Check the generated contract and compatibility against the committed baseline.
    *******************************************************************************/
   @Test
   void contract() throws Exception
   {
      var version = new MiddlewareVersionV1();
      var extension = EdiApi.extension();
      String generated = UiApiContract.generateExtensionYaml(version, extension);
      Path path = Path.of("src/main/resources/openapi/qqq-v1/edi.yaml").toAbsolutePath();
      if(Boolean.getBoolean("edi.openapi.update"))
      {
         Files.createDirectories(path.getParent());
         Files.writeString(path, generated);
      }
      String committed = Files.readString(path);
      assertThat(UiApiContract.check(generated, committed, committed, 1,
         "mvnw.cmd -B -Dtest=EdiApiContractTest -Dedi.openapi.update=true test")).isEmpty();
      var model = version.generateExtensionOpenAPIModel("qqq", extension);
      assertThat(model.getComponents().getSchemas().keySet()).allMatch(n -> (n.startsWith("Edi") && n.endsWith("V1")) || n.equals("BasicErrorResponseV1"));
      assertThat(model.getPaths()).hasSize(4);
      assertThat(model.getPaths().keySet()).allMatch(n -> n.startsWith("/qqq/v1/qbit/edi/"));
      assertThat(extension.getEffectivePathPrefix()).isEqualTo("/qbit/edi");
      assertThat(extension.getSemanticVersion()).isEqualTo("1.1.0");
   }

   /*******************************************************************************
    ** Producer registers the extension and all metadata validates with core middleware.
    *******************************************************************************/
   @Test
   void registration() throws Exception
   {
      try
      {
         var instance = H2Support.create();
         assertThat(MiddlewareSpecExtensionsMetaData.ofOrWithNew(instance).getExtensions()).hasSize(1);
      }
      finally
      {
         com.kingsrook.qqq.backend.core.context.QContext.clear();
      }
   }
}
