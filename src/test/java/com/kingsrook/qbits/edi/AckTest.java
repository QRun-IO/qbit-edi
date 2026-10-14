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


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.ack.Ack997Builder;
import com.kingsrook.qbits.edi.ack.Ack999Builder;
import com.kingsrook.qbits.edi.ack.AckEnvelope;
import com.kingsrook.qbits.edi.ack.AckLevel;
import com.kingsrook.qbits.edi.ack.InboundAckReader;
import com.kingsrook.qbits.edi.ack.TA1Builder;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qbits.edi.x12.X12Writer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Exact acknowledgement output, levels, envelope replies and inbound status parsing.
 *******************************************************************************/
class AckTest
{
   /*******************************************************************************
    ** Situational errors include position-only CTX; PHI values never enter an ack.
    *******************************************************************************/
   @Test
   void situationalContext() throws Exception
   {
      var group = X12Test.read(Synthetic850.generate(1, "005010", X12Separators.DEFAULT)).groups().getFirst();
      var trigger = new com.kingsrook.qbits.edi.guide.RuleEvaluator(group.transactions().getFirst().segments(), Map.of()).context("when(eq(N101, 'ST'), present(REF01))");
      assertThat(trigger.segmentId()).isEqualTo("N1");
      ValidationFinding finding = new ValidationFinding(4, "REF", "N1", 2, null, 2, "8", "1", "8", "1", "ERROR", "Missing", "CANARY-PHI-123").withContext(trigger);
      var ack = new Ack999Builder().build(group, Map.of("0001", List.of(finding)), AckLevel.ERRORS_ONLY, "0002");
      assertThat(new X12Writer().write(ack.segments(), X12Separators.DEFAULT, "")).contains("CTX*SITUATIONAL TRIGGER*N1*3**1~").doesNotContain("CANARY");
   }

   /*******************************************************************************
    ** All accepted and rejected variants have exact segment counts at every ack level.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(AckLevel.class)
   void exact(AckLevel level) throws Exception
   {
      X12Interchange interchange = X12Test.read(Synthetic850.generate(1, "005010", X12Separators.DEFAULT));
      X12FunctionalGroup group = interchange.groups().getFirst();
      for(boolean implementation : List.of(false, true))
      {
         String type = implementation ? "999" : "997";
         String prefix = implementation ? "IK" : "AK";
         var clean = implementation ? new Ack999Builder().build(group, Map.of(), level, "0002") : new Ack997Builder().build(group, Map.of(), level, "0002");
         String detail = level == AckLevel.ALL_TRANSACTIONS ? "AK2*850*0001~" + prefix + "5*A~" : "";
         assertThat(new X12Writer().write(clean.segments(), X12Separators.DEFAULT, "")).isEqualTo("ST*" + type + "*0002" + (implementation ? "*005010X231A1" : "") + "~AK1*PO*1" + (implementation ? "*005010" : "") + "~" + detail + "AK9*A*1*1*1~SE*" + (detail.isEmpty() ? 4 : 6) + "*0002~");
         ValidationFinding error = new ValidationFinding(2, "BEG", null, 3, null, 2, "8", "1", "8", "1", "ERROR", "Required missing", null);
         var failed = implementation ? new Ack999Builder().build(group, Map.of("0001", List.of(error)), level, "0002") : new Ack997Builder().build(group, Map.of("0001", List.of(error)), level, "0002");
         detail = level == AckLevel.GROUP_ONLY ? "" : "AK2*850*0001~" + prefix + "3*BEG*2**8~" + prefix + "4*3**1~" + prefix + "5*R*5~";
         assertThat(new X12Writer().write(failed.segments(), X12Separators.DEFAULT, "")).isEqualTo("ST*" + type + "*0002" + (implementation ? "*005010X231A1" : "") + "~AK1*PO*1" + (implementation ? "*005010" : "") + "~" + detail + "AK9*R*1*1*0~SE*" + (detail.isEmpty() ? 4 : 8) + "*0002~");
         var result = new InboundAckReader().read(failed);
         var guide = new com.kingsrook.qbits.edi.guide.BuiltInGuideLoader().resource(implementation ? "999-005010X231A1" : "997-005010");
         assertThat(new com.kingsrook.qbits.edi.guide.GuideValidator().validate(failed, guide, false)).isEmpty();
         assertThat(new com.kingsrook.qbits.edi.guide.GuideValidator().validate(clean, guide, false)).isEmpty();
         assertThat(result.status()).isEqualTo("R");
         assertThat(result.controlNumber()).isEqualTo("1");
         assertThat(result.transactionStatuses()).hasSize(level == AckLevel.GROUP_ONLY ? 0 : 1);
         var wrapped = AckEnvelope.wrap(interchange, group, clean, "2", "2", LocalDateTime.of(2026, 10, 13, 12, 5));
         var parsed = X12Test.read(new X12Writer().write(wrapped, ""));
         assertThat(parsed.findings()).isEmpty();
         assertThat(new InboundAckReader().read(parsed)).singleElement().satisfies(r -> assertThat(r.status()).isEqualTo("A"));
      }
   }

   /*******************************************************************************
    ** Duplicate and receiver rejection TA1s refer to the original interchange.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"025", "007", "000"})
   void ta1(String code) throws Exception
   {
      var interchange = X12Test.read(Synthetic850.generate(1, "005010", X12Separators.DEFAULT));
      var reply = new TA1Builder().interchange(interchange, "2", code, LocalDateTime.of(2026, 10, 13, 12, 5));
      String text = new X12Writer().write(reply, "");
      assertThat(text).contains("TA1*000000001*261013*1200*" + (code.equals("000") ? "A" : "R") + "*" + code + "~IEA*0*000000002~");
      var result = new InboundAckReader().read(X12Test.read(text)).getFirst();
      assertThat(result.type()).isEqualTo("TA1");
      assertThat(result.noteCode()).isEqualTo(code);
      assertThat(reply.isa().value(6).trim()).isEqualTo("QQQTEST");
   }

   /*******************************************************************************
    ** Warnings accept with errors; mixed transaction results are partial.
    *******************************************************************************/
   @Test
   void statuses() throws Exception
   {
      var group = X12Test.read(Synthetic850.generate(1, "005010", X12Separators.DEFAULT)).groups().getFirst();
      ValidationFinding warning = new ValidationFinding(2, "BEG", null, null, null, 2, "8", "2", "8", "2", "WARNING", "Synthetic warning", null);
      var ack = new Ack997Builder().build(group, Map.of("0001", List.of(warning)), AckLevel.ERRORS_ONLY, "0002");
      assertThat(new InboundAckReader().read(ack).status()).isEqualTo("E");
      var original = group.transactions().getFirst();
      var bad = new X12TransactionSet(X12Segment.of("ST", 1, "850", "0003"), original.se(), original.segments(), List.of(new EnvelopeFinding("AK5", 9, "4", "count")));
      var mixed = new X12FunctionalGroup(group.gs(), X12Segment.of("GE", 0, "2", "1"), List.of(original, bad), List.of());
      ack = new Ack999Builder().build(mixed, Map.of(), AckLevel.ALL_TRANSACTIONS, "0002");
      assertThat(new X12Writer().write(ack.segments(), X12Separators.DEFAULT, "")).contains("IK5*R*4~AK9*P*2*2*1~");
      var failedGroup = new X12FunctionalGroup(group.gs(), group.ge(), group.transactions(), List.of(new EnvelopeFinding("AK9", 0, "5", "count")));
      assertThat(new InboundAckReader().read(new Ack997Builder().build(failedGroup, Map.of(), AckLevel.GROUP_ONLY, "0002")).status()).isEqualTo("R");
   }
}
