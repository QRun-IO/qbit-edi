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
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.guide.Guide;
import com.kingsrook.qbits.edi.guide.GuideElement;
import com.kingsrook.qbits.edi.guide.GuideRule;
import com.kingsrook.qbits.edi.guide.GuideSegment;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.guide.RuleEvaluator;
import com.kingsrook.qbits.edi.guide.ValidationFinding;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Crafted failures for the guide engine, syntax notes and expression language.
 *******************************************************************************/
class GuideTest
{
   /*******************************************************************************
    ** A true condition requires an absent segment, including positional ack context.
    *******************************************************************************/
   @Test
   void missingSituationalSegment()
   {
      X12Segment n1 = X12Segment.of("N1", 1, "ST");
      GuideSegment first = new GuideSegment("N1", null, null, 1, "REQUIRED", 1, 1, List.of(), List.of(), null);
      GuideSegment required = new GuideSegment("REF", null, null, 1, "SITUATIONAL", 0, 1, List.of(), List.of(), "eq(N101, 'ST')");
      Guide guide = new Guide("test", "X12", "005010", "850", "PO", List.of(first, required), List.of());
      var findings = new GuideValidator().validate(new X12TransactionSet(n1, n1, List.of(n1), List.of()), guide, true);
      assertThat(findings).singleElement().satisfies(f ->
      {
         assertThat(f.ak3Code()).isEqualTo("3");
         assertThat(f.context().segmentId()).isEqualTo("N1");
         assertThat(f.valueExcerpt()).isNull();
      });
   }

   /*******************************************************************************
    ** Use a single-segment buffered transaction for focused rule cases.
    *******************************************************************************/
   private static List<ValidationFinding> validate(GuideSegment definition, X12Segment segment, boolean phi)
   {
      return (new GuideValidator().validate(new X12TransactionSet(segment, segment, List.of(segment), List.of()),
         new Guide("test", "X12", "005010", "850", "PO", List.of(definition), List.of()), phi));
   }

   /*******************************************************************************
    ** Create a segment with supplied elements and syntax notes.
    *******************************************************************************/
   private static GuideSegment definition(List<GuideElement> elements, List<GuideRule> rules)
   {
      return (new GuideSegment("REF", null, null, 1, "REQUIRED", 1, 1, elements, rules, null));
   }

   /*******************************************************************************
    ** Every syntax-note type has a crafted failure and a passing document.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"PAIRED,a,,a,b", "REQUIRED,,,a,", "EXCLUSION,a,b,a,", "CONDITIONAL,a,,a,b", "LIST_CONDITIONAL,a,,a,b"})
   void syntax(String type, String bad1, String bad2, String good1, String good2)
   {
      GuideRule rule = new GuideRule(type, List.of(1, 2), null, 2, "ERROR", "Synthetic rule", "8", "2", "8", "2");
      GuideSegment definition = definition(List.of(), List.of(rule));
      assertThat(validate(definition, X12Segment.of("REF", 1, empty(bad1), empty(bad2)), false)).hasSize(1);
      assertThat(validate(definition, X12Segment.of("REF", 1, empty(good1), empty(good2)), false)).isEmpty();
   }

   /*******************************************************************************
    ** Normalize CSV blank fields.
    *******************************************************************************/
   private static String empty(String value)
   {
      return (value == null ? "" : value);
   }

   /*******************************************************************************
    ** Lexical validation rejects invalid calendar dates, clock times and numeric forms.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"AN,Synthetic,true", "ID,ST,true", "N0,-123,true", "N9,12.3,false", "R,-12.3,true", "R,.5,true", "R,1e3,false", "DT,20260230,false", "DT,20240229,true", "DT,261013,true", "DT,991231,true", "TM,23595999,true", "TM,2460,false", "TM,1200,true", "B,abc,true"})
   void types(String type, String value, boolean valid)
   {
      assertThat(GuideValidator.validType(type, value)).isEqualTo(valid);
   }

   /*******************************************************************************
    ** Required, prohibited, lengths, types, codes, fixed values and repetition limits are enforced.
    *******************************************************************************/
   @Test
   void elements()
   {
      List<GuideElement> elements = List.of(
         new GuideElement(1, null, null, "ID", 2, 2, "REQUIRED", List.of("AA"), "AA", 1, null),
         new GuideElement(2, null, null, "N2", 2, 4, "NOT_USED", List.of(), null, 1, null),
         new GuideElement(3, 2, null, "AN", 1, 10, "REQUIRED", List.of(), null, 1, null));
      GuideSegment definition = definition(elements, List.of());
      List<ValidationFinding> findings = validate(definition, X12Segment.of("REF", 4, "X", "long-bad", "component"), false);
      assertThat(findings).extracting(ValidationFinding::ak4Code).contains("4", "7", "10", "5", "6", "1");
      assertThat(findings).anyMatch(f -> Integer.valueOf(2).equals(f.componentPosition()));
      assertThat(validate(definition, X12Segment.of("REF", 4, "", "", ""), false)).extracting(ValidationFinding::ak4Code).contains("1");
      X12Segment repeating = new X12Segment("REF", 1, List.of(List.of(List.of("AA"), List.of("AA"))));
      assertThat(validate(definition, repeating, false)).extracting(ValidationFinding::ak4Code).contains("12");
      assertThat(validate(definition, X12Segment.of("REF", 4, "CANARY-PHI-123", "secret", ""), true)).allSatisfy(f ->
      {
         assertThat(f.valueExcerpt()).isNull();
         assertThat(f.message()).doesNotContain("CANARY", "secret");
      });
   }

   /*******************************************************************************
    ** Segment usage, order, counts and loop occurrence limits are checked.
    *******************************************************************************/
   @Test
   void structure() throws Exception
   {
      var interchange = X12Test.read(Synthetic850.generate(1, "005010", X12Separators.DEFAULT));
      var transaction = interchange.groups().getFirst().transactions().getFirst();
      Guide guide = new BuiltInGuideLoader().resource("850-005010");
      List<X12Segment> bad = new ArrayList<>(transaction.segments());
      bad.removeIf(s -> s.id().equals("BEG"));
      bad.add(1, X12Segment.of("UNKNOWN", 2, ""));
      assertThat(new GuideValidator().validate(new X12TransactionSet(transaction.st(), transaction.se(), bad, List.of()), guide, false))
         .extracting(ValidationFinding::ak3Code).contains("3", "2");
      GuideSegment prohibited = new GuideSegment("REF", null, null, 1, "NOT_USED", 0, 0, List.of(), List.of(), null);
      assertThat(validate(prohibited, X12Segment.of("REF", 1, "X"), false)).extracting(ValidationFinding::ak3Code).contains("6", "5");
      GuideSegment loop = new GuideSegment("N1", "2000", null, 1, "REQUIRED", 1, 1, List.of(), List.of(), null);
      var tx = new X12TransactionSet(transaction.st(), transaction.se(), List.of(X12Segment.of("N1", 1, "ST"), X12Segment.of("N1", 2, "BT")), List.of());
      assertThat(new GuideValidator().validate(tx, new Guide("test", "X12", "005010", "850", "PO", List.of(loop), List.of()), false))
         .extracting(ValidationFinding::ak3Code).containsExactly("4");
   }

   /*******************************************************************************
    ** Functions compose with predicates, indexes, repeats, components and loop scopes.
    *******************************************************************************/
   @Test
   void expressions()
   {
      X12Segment first = X12Segment.of("N1", 1, "ST", "Synthetic");
      X12Segment second = X12Segment.of("N1", 2, "BT", "Synthetic");
      X12Segment quantity = X12Segment.of("PO1", 3, "1", "2.50");
      X12Segment date = X12Segment.of("DTM", 4, "002", "20261013");
      X12Segment composite = new X12Segment("REF", 5, List.of(List.of(List.of("A", "B"))));
      RuleEvaluator rules = new RuleEvaluator(List.of(first, second, quantity, date, composite), Map.of("N1", List.of(List.of(first), List.of(second)), "PO1", List.of(List.of(quantity))));
      for(String expression : List.of("present(N101)", "absent(BEG03)", "eq(count(N1), 2)", "eq(sum(PO102), 2.5)", "in(N101, 'ST', 'BT')", "matches(N102, 'S.*')",
         "date(DTM02)", "and(present(N1), not(absent(N1)))", "or(absent(N1), present(PO1))", "when(eq(N101, 'ST'), present(PO1))", "eq(N1[N101=ST]/N101, 'ST')",
         "eq(N1[2]/N101, 'BT')", "eq(count(N1[*]/N101), 2)", "eq(REF01-2, 'B')", "when(absent(N1), absent(PO1))"))
      {
         assertThat(rules.test(expression)).as(expression).isTrue();
      }
      assertThat(rules.test("eq(PO102, 9)")).isFalse();
      assertThat(rules.test("eq(N102, 9)")).isFalse();
      assertThat(rules.values("N1[9]/N101")).isEmpty();
      assertThatThrownBy(() -> rules.test("exec('anything')")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> rules.test("and(present(N1)")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> rules.test("N101")).isInstanceOf(IllegalArgumentException.class);
      GuideRule rule = new GuideRule("SITUATIONAL", List.of(), "when(present(REF01), present(BEG03))", 2, "ERROR", "CANARY-PHI-123", "8", "2", "8", "2");
      assertThat(validate(definition(List.of(), List.of(rule)), X12Segment.of("REF", 1, "X"), true)).singleElement().satisfies(f -> assertThat(f.message()).isEqualTo("Guide rule failed"));
   }
}
