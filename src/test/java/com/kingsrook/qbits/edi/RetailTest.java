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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.edi.map.OutboundMapper;
import com.kingsrook.qbits.edi.map.InboundMapper;
import com.kingsrook.qbits.edi.guide.RuleEvaluator;
import com.kingsrook.qbits.edi.x12.X12Writer;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Realistic retail baselines validate maps in both supported releases.
 *******************************************************************************/
class RetailTest
{
   /*******************************************************************************
    ** Restore thread context.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Map associations, validate, serialize and parse all outbound retail documents.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void retailRoundTrips(String version) throws Exception
   {
      H2Support.create();
      for(String document : List.of("855", "856", "810", "846"))
      {
         var definition = RetailSupport.sample(document, version);
         List<QRecord> rules = new ArrayList<>();
         definition.get("rules").forEach(r -> rules.add(RetailSupport.record(r)));
         var set = new OutboundMapper().build(RetailSupport.source(), RetailSupport.record(definition.get("map")), rules, "0001");
         var guide = EdiRuntime.current().guides().resource(document + "-" + version);
         assertThat(EdiRuntime.current().validator().validate(set, guide, false)).as(document).isEmpty();
         String text = new X12Writer().write(set.segments(), com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT, "");
         String envelope = Synthetic850.generate(1, version, com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT);
         envelope = envelope.substring(0, envelope.indexOf("~ST*") + 1) + text + envelope.substring(envelope.indexOf("GE*"));
         var parsed = EdiRuntime.current().reader().read(new ByteArrayInputStream(envelope.getBytes(StandardCharsets.UTF_8))).getFirst().groups().getFirst().transactions().getFirst();
         assertThat(parsed.findings()).isEmpty();
         assertThat(parsed.segments().stream().map(s -> s.id() + s.elements()).toList()).isEqualTo(set.segments().stream().map(s -> s.id() + s.elements()).toList());
         if(document.equals("856"))
         {
            assertThat(new RuleEvaluator(parsed.segments(), java.util.Map.of()).values("HL01")).containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
            assertThat(new RuleEvaluator(parsed.segments(), java.util.Map.of()).values("HL02")).containsExactly("1", "2", "3", "3", "2", "6", "6");
         }
      }
   }
   /*******************************************************************************
    ** Both inbound retail samples retain the expected business record values.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void inboundRetail(String version) throws Exception
   {
      H2Support.create();
      for(String document : List.of("850", "860"))
      {
         String text = Synthetic850.generate(1, version, com.kingsrook.qbits.edi.x12.X12Separators.DEFAULT);
         if(document.equals("860"))
         {
            text = com.kingsrook.qbits.edi.demo.EdiDemoServer.changeOrder(text);
         }
         var set = EdiRuntime.current().reader().read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))).getFirst().groups().getFirst().transactions().getFirst();
         var guide = EdiRuntime.current().guides().resource(document + "-" + version);
         assertThat(EdiRuntime.current().validator().validate(set, guide, false)).isEmpty();
         var definition = RetailSupport.sample(document, version);
         List<QRecord> rules = new ArrayList<>();
         definition.get("rules").forEach(r -> rules.add(RetailSupport.record(r)));
         var result = new InboundMapper(EdiRuntime.current().transforms()).map(set, guide, RetailSupport.record(definition.get("map")), rules);
         assertThat(result.record().getValueString("poNumber")).startsWith("TEST-");
         assertThat(result.record().getAssociatedRecords().get("lines").getFirst().getValueString("sku")).isEqualTo("SYNTH-1");
      }
   }
}
