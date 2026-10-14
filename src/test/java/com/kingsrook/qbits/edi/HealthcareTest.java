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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.ack.Ack999Builder;
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.guide.HealthcareValidation;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.map.InboundMapper;
import com.kingsrook.qbits.edi.map.OutboundMapper;
import com.kingsrook.qbits.edi.map.TransformRegistry;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12Writer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Original healthcare baselines, balances, situational failures and classified flows.
 *******************************************************************************/
class HealthcareTest
{
   /*******************************************************************************
    ** Restore the calling thread after database tests.
    *******************************************************************************/
   @AfterEach
   void clear()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Validate a synthetic body including its real envelopes and transaction controls.
    *******************************************************************************/
   private static List<ValidationFinding> validate(String key, String body) throws Exception
   {
      var tx = LogisticsSupport.read(HealthcareSupport.interchange(key, 1, body)).groups().getFirst().transactions().getFirst();
      assertThat(tx.findings()).isEmpty();
      return (new GuideValidator().validate(tx, new BuiltInGuideLoader().resource(key + "-" + HealthcareSupport.version(key)), true));
   }

   /*******************************************************************************
    ** All eight baseline guides accept their seeded fixtures, including format checks.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"837P", "837I", "835", "270", "271", "276", "277", "834"})
   void baseline(String key) throws Exception
   {
      assertThat(validate(key, HealthcareSupport.body(key, 123))).isEmpty();
   }

   /*******************************************************************************
    ** Every inbound map writes the synthetic identifiers and associated records.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"837P", "837I", "835", "271", "277", "834"})
   void inboundMap(String key) throws Exception
   {
      var tx = LogisticsSupport.read(HealthcareSupport.interchange(key, 1, HealthcareSupport.body(key, 123))).groups().getFirst().transactions().getFirst();
      List<QRecord> rules = new ArrayList<>();
      var map = LogisticsSupport.definition(key, HealthcareSupport.version(key), rules);
      var result = new InboundMapper(new TransformRegistry()).map(tx, new BuiltInGuideLoader().resource(key + "-" + HealthcareSupport.version(key)), map, rules);
      assertThat(result.businessKey()).startsWith("SYN");
      if(key.startsWith("837"))
      {
         assertThat(result.record().getValueString("patientName")).isEqualTo("Testpatient Alpha");
         assertThat(result.record().getValueBigDecimal("totalCharge")).isEqualByComparingTo("150");
         assertThat(result.record().getAssociatedRecords().get("lines")).hasSize(2);
         assertThat(result.record().getAssociatedRecords().get("lines").getLast().getValueBigDecimal("charge")).isEqualByComparingTo("50");
      }
      if(key.equals("835"))
      {
         assertThat(result.record().getAssociatedRecords().get("claims")).hasSize(1);
      }
      if(key.equals("834"))
      {
         assertThat(result.record().getAssociatedRecords().get("members")).hasSize(1);
      }
   }

   /*******************************************************************************
    ** Missing situational data produces SNIP 4 findings, never submitted values.
    *******************************************************************************/
   @Test
   void situationalFailures() throws Exception
   {
      String body = HealthcareSupport.body("837P", 123);
      String dependent = body.replace("SBR*P*18", "SBR*P*19").replace("~CLM*", "~HL*3*2*23*0~PAT*19~NM1*QC*1*Testpatient Beta~CLM*");
      assertThat(validate("837P", dependent)).isEmpty();
      List<QRecord> dependentRules = new ArrayList<>();
      var fixture = LogisticsSupport.definition("837P", "005010X222A1", dependentRules);
      var dependentTx = LogisticsSupport.read(HealthcareSupport.interchange("837P", 1, dependent)).groups().getFirst().transactions().getFirst();
      var dependentMap = new InboundMapper(new TransformRegistry()).map(dependentTx, new BuiltInGuideLoader().resource("837P-005010X222A1"), fixture, dependentRules);
      assertThat(dependentMap.record().getValueString("patientName")).isEqualTo("Testpatient Beta");
      for(String invalid : List.of(body.replace("SBR*P*18", "SBR*P*19"), dependent.replace("SBR*P*19", "SBR*P*18"),
         dependent.replace("PAT*19~", ""), body.replace("HI*ABK:A123~", ""), body.replaceFirst("DTP\\*472\\*D8\\*20261015~", "")))
      {
         assertThat(validate("837P", invalid)).anyMatch(f -> f.snipLevel() == 4);
      }
      assertThat(validate("837I", HealthcareSupport.body("837I", 123).replace("DTP*434*RD8*20261014-20261015~", ""))).anyMatch(f -> f.snipLevel() == 4);
      assertThat(validate("834", HealthcareSupport.body("834", 123).replace("DMG*D8*19800101*U~", ""))).anyMatch(f -> f.snipLevel() == 4);
      String claimDate = body.replace("HI*", "DTP*472*D8*20261015~HI*").replace("~DTP*472*D8*20261015~LX", "~LX");
      assertThat(validate("837P", claimDate)).isEmpty();
   }

   /*******************************************************************************
    ** Exact decimal balancing and implementation acknowledgement text with no values.
    *******************************************************************************/
   @Test
   void balanceAndExact999() throws Exception
   {
      String body = HealthcareSupport.body("837P", 123).replace("*150***", "*150.01***");
      var findings = validate("837P", body);
      assertThat(findings).singleElement().satisfies(f -> assertThat(f.snipLevel()).isEqualTo(3));
      var group = LogisticsSupport.read(HealthcareSupport.interchange("837P", 1, body)).groups().getFirst();
      var ack = new Ack999Builder().build(group, Map.of("0001", findings), AckLevel.ALL_TRANSACTIONS, "0002");
      assertThat(new X12Writer().write(ack.segments(), X12Separators.DEFAULT, "")).isEqualTo(
         "ST*999*0002*005010X231A1~AK1*HC*1*005010X222A1~AK2*837*0001*005010X222A1~IK3*CLM*14*2300*8~IK4*2**7~CTX*SITUATIONAL TRIGGER*CLM*14*2300*2~IK5*E~AK9*E*1*1*1~SE*9*0002~");
      assertThat(new GuideValidator().validate(ack, new BuiltInGuideLoader().resource("999-005010X231A1"), false)).isEmpty();
      assertThat(validate("837I", HealthcareSupport.body("837I", 123).replace("*150***", "*149.99***"))).anyMatch(f -> f.snipLevel() == 3);
      String remit = HealthcareSupport.body("835", 123);
      assertThat(validate("835", remit.replace("BPR*I*115", "BPR*I*115.01"))).anyMatch(f -> f.snipLevel() == 3 && f.segmentId().equals("BPR"));
      assertThat(validate("835", remit.replace("CAS*CO*45*20", "CAS*CO*45*19.99"))).anyMatch(f -> f.snipLevel() == 3 && f.segmentId().equals("CLP"));
      assertThat(validate("835", remit.replace("BPR*I*115", "BPR*I*125").replace("WO:SYNADJ*5", "WO:SYNADJ*-5"))).isEmpty();
      assertThat(validate("835", remit.replace("CAS*CO*45*20", "CAS*CO*45*10*1*94*10"))).isEmpty();
   }

   /*******************************************************************************
    ** Licensed membership is optional, format checks and NPI checksum are deterministic.
    *******************************************************************************/
   @Test
   void formatsAndExternalCodes() throws Exception
   {
      assertThat(HealthcareValidation.validNpi("1234567893")).isTrue();
      assertThat(HealthcareValidation.validNpi("1234567890")).isFalse();
      String body = HealthcareSupport.body("837P", 123);
      for(String invalid : List.of(body.replace("1234567893", "1234567890"), body.replace("60000", "ZIPBAD"), body.replace("ABK:A123", "ABK:BAD!")))
      {
         assertThat(validate("837P", invalid)).anyMatch(f -> f.snipLevel() == 5);
      }
      var calls = new ArrayList<String>();
      var config = new EdiQBitConfig().withExternalCodeSetProvider((set, code, date) ->
      {
         calls.add(set + ":" + date);
         return (!set.equals("ICD10CM"));
      });
      var tx = LogisticsSupport.read(HealthcareSupport.interchange("837P", 1, body)).groups().getFirst().transactions().getFirst();
      var result = new GuideValidator(config).validate(tx, new BuiltInGuideLoader().resource("837P-005010X222A1"), true);
      assertThat(result).anyMatch(f -> f.snipLevel() == 5 && f.segmentId().equals("HI"));
      assertThat(calls).contains("ICD10CM:2026-10-15", "HCPCS:2026-10-15");
   }

   /*******************************************************************************
    ** Outbound eligibility and claim status requests use only their classified sources.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"270", "276"})
   void outboundMap(String key) throws Exception
   {
      H2Support.create();
      List<QRecord> rules = new ArrayList<>();
      QRecord map = LogisticsSupport.definition(key, HealthcareSupport.version(key), rules);
      var tx = new OutboundMapper().build(new QRecord().withValue("trace", "SYNTRACE123").withValue("memberId", "SYN000123")
         .withValue("patientName", "Testpatient Alpha").withValue("claimNumber", "SYNCLM123"), map, rules, "0001");
      assertThat(new GuideValidator().validate(tx, new BuiltInGuideLoader().resource(key + "-" + HealthcareSupport.version(key)), true)).isEmpty();
   }

   /*******************************************************************************
    ** A claim cannot borrow charges, dates or patient loops from a neighboring claim.
    *******************************************************************************/
   @Test
   void independentClaimsAndSituational999() throws Exception
   {
      String body = HealthcareSupport.body("837P", 123);
      String claim = body.substring(body.indexOf("CLM*"));
      String two = body + claim.replace("*150***", "*151***");
      assertThat(validate("837P", two)).singleElement().satisfies(f -> assertThat(f.snipLevel()).isEqualTo(3));
      String offsetting = body.replace("*150***", "*149***") + claim.replace("*150***", "*151***");
      assertThat(validate("837P", offsetting).stream().filter(f -> f.snipLevel() == 3)).hasSize(2);
      String missingDate = body + claim.replace("DTP*472*D8*20261015~", "");
      assertThat(validate("837P", missingDate).stream().filter(f -> f.snipLevel() == 4)).hasSize(2);
      String missingPatient = body.replace("SBR*P*18", "SBR*P*19");
      var group = LogisticsSupport.read(HealthcareSupport.interchange("837P", 1, missingPatient)).groups().getFirst();
      String ack = new X12Writer().write(new Ack999Builder().build(group, Map.of("0001", validate("837P", missingPatient)), AckLevel.ALL_TRANSACTIONS, "0002").segments(), X12Separators.DEFAULT, "");
      assertThat(ack).isEqualTo("ST*999*0002*005010X231A1~AK1*HC*1*005010X222A1~AK2*837*0001*005010X222A1~IK3*SBR*10*2000B*8~CTX*SITUATIONAL TRIGGER*SBR*10*2000B*2~IK5*E~AK9*E*1*1*1~SE*8*0002~");
      assertThat(validate("271", HealthcareSupport.body("271", 123).replace("EB*1**30~", ""))).anyMatch(f -> f.snipLevel() == 4);
      assertThat(validate("271", HealthcareSupport.body("271", 123).replace("EB*1**30~", "AAA*Y**72*C~"))).isEmpty();
   }

   /*******************************************************************************
    ** External code-list source and name survive normalization; the home reflects the SPI.
    *******************************************************************************/
   @Test
   void externalRepositoryAndHome() throws Exception
   {
      H2Support.create();
      var source = new BuiltInGuideLoader().resource("837P-005010X222A1");
      Integer id = new com.kingsrook.qbits.edi.guide.GuideRepository().save(source, "synthetic", 1);
      var guide = new com.kingsrook.qbits.edi.guide.GuideRepository().load(id);
      assertThat(guide.segments().stream().flatMap(s -> s.elements().stream()).filter(e -> "EXTERNAL".equals(e.source())))
         .allMatch(e -> e.externalCodeSet() != null && e.codes().isEmpty());
      var runtime = EdiRuntime.current();
      var tx = LogisticsSupport.read(HealthcareSupport.interchange("837P", 1, HealthcareSupport.body("837P", 123))).groups().getFirst().transactions().getFirst();
      assertThat(runtime.validator().validate(tx, guide, true)).isEmpty();
      runtime.config().withExternalCodeSetProvider((set, code, date) -> false);
      assertThat(runtime.validator().validate(tx, guide, true)).anyMatch(f -> f.snipLevel() == 5);
      var widget = QContext.getQInstance().getWidget("edi_home_system");
      String json = com.kingsrook.qqq.backend.core.utils.JsonUtils.toJson(new com.kingsrook.qbits.edi.app.EdiHome()
         .render(new com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput().withWidgetMetaData(widget)));
      assertThat(json).contains("Provider configured");
      runtime.config().withExternalCodeSetProvider(null);
      json = com.kingsrook.qqq.backend.core.utils.JsonUtils.toJson(new com.kingsrook.qbits.edi.app.EdiHome()
         .render(new com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput().withWidgetMetaData(widget)));
      assertThat(json).contains("Format only");
   }

   /*******************************************************************************
    ** Real persistence honors defaults, PHI keys, 999 routing and accepted-with-error apply.
    *******************************************************************************/
   @Test
   void healthcarePipeline() throws Exception
   {
      H2Support.create();
      HealthcareSupport.tables();
      EdiRuntime.current().initialize();
      HealthcareSupport.configure();
      int control = 100;
      for(String key : List.of("837P", "837I", "835", "271", "277", "834"))
      {
         HealthcareSupport.receive(key, ++control, HealthcareSupport.body(key, control));
      }
      HealthcareSupport.receive("837P", ++control, HealthcareSupport.body("837P", control).replace("*150***", "*151***"));
      var rows = EdiRecords.query("edi_transaction", new QQueryFilter());
      assertThat(rows.stream().filter(r -> "INBOUND".equals(r.getValueString("direction"))).toList()).hasSize(7).allSatisfy(r ->
      {
         assertThat(r.getValueString("status")).isEqualTo("APPLIED");
         assertThat(r.getValueString("phiBusinessKey")).startsWith("SYN");
         assertThat(r.getValueString("businessKey")).isNull();
         assertThat(r.getValueBoolean("containsPhi")).isTrue();
      });
      assertThat(rows).anyMatch(r -> "E".equals(r.getValueString("ackStatus")));
      assertThat(rows.stream().filter(r -> "OUTBOUND".equals(r.getValueString("direction"))).toList()).hasSize(7).allMatch(r -> "999".equals(r.getValueString("transactionSet")));
      assertThat(EdiRecords.query("sample_claim_line", new QQueryFilter())).hasSize(6);
      assertThat(EdiRecords.query("edi_transaction_error", new QQueryFilter())).allMatch(r -> r.getValue("valueExcerpt") == null);
      assertThat(EdiRecords.query("edi_agreement_document", new QQueryFilter())).allMatch(r -> Boolean.TRUE.equals(r.getValueBoolean("containsPhi")));
   }
}
