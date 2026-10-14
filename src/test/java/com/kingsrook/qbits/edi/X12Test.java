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
import java.util.List;
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.guide.Guide;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.x12.EnvelopeFinding;
import com.kingsrook.qbits.edi.x12.X12EnvelopeReader;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qbits.edi.x12.X12Writer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Envelope, separator, bounds, round-trip and baseline validation acceptance tests.
 *******************************************************************************/
class X12Test
{
   /*******************************************************************************
    ** Read an in-memory synthetic payload.
    *******************************************************************************/
   static X12Interchange read(String text) throws Exception
   {
      return (new X12EnvelopeReader(100000).read(new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII))).getFirst());
   }

   /*******************************************************************************
    ** Both versions validate and preserve values through writer-reader round trips.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"004010", "005010"})
   void roundTrip(String version) throws Exception
   {
      X12Separators separators = new X12Separators('*', '~', ':', version.equals("004010") ? null : '^');
      X12Interchange parsed = read(Synthetic850.generate(5, version, separators));
      assertThat(parsed.findings()).isEmpty();
      assertThat(parsed.groups()).hasSize(1);
      assertThat(parsed.groups().getFirst().findings()).isEmpty();
      assertThat(parsed.groups().getFirst().transactions().getFirst().findings()).isEmpty();
      Guide guide = new BuiltInGuideLoader().resource("850-" + version);
      assertThat(new GuideValidator().validate(parsed.groups().getFirst().transactions().getFirst(), guide, false)).isEmpty();
      assertThat(read(new X12Writer().write(parsed, "\r\n"))).isEqualTo(parsed);
      assertThat(read(new X12Writer().write(parsed))).isEqualTo(parsed);
      if(version.equals("004010"))
      {
         assertThat(Synthetic850.generate(5, version, X12Separators.DEFAULT).charAt(82)).isEqualTo('U');
      }
   }

   /*******************************************************************************
    ** Multiple interchanges may each declare distinct separators.
    *******************************************************************************/
   @Test
   void multiInterchange() throws Exception
   {
      String first = Synthetic850.generate(4, "005010", X12Separators.DEFAULT);
      X12Separators custom = new X12Separators('|', '!', '>', '%');
      String second = Synthetic850.generate(5, "005010", custom);
      List<X12Interchange> parsed = new X12EnvelopeReader(100000).read(new ByteArrayInputStream((first + "\r\n " + second).getBytes(StandardCharsets.US_ASCII)));
      assertThat(parsed).hasSize(2);
      assertThat(parsed.get(1).separators()).isEqualTo(custom);
      assertThat(parsed).allSatisfy(i -> assertThat(i.findings()).isEmpty());
   }

   /*******************************************************************************
    ** All count and control mismatch codes are retained as structured errors.
    *******************************************************************************/
   @Test
   void envelopeErrors() throws Exception
   {
      String text = Synthetic850.generate(4, "005010", X12Separators.DEFAULT).replace("SE*9*0001", "SE*8*0002").replace("GE*1*1", "GE*2*2").replace("IEA*1*000000001", "IEA*2*000000002");
      X12Interchange parsed = read(text);
      assertThat(parsed.findings()).extracting(EnvelopeFinding::code).contains("021", "001");
      assertThat(parsed.groups().getFirst().findings()).extracting(EnvelopeFinding::code).containsExactly("5", "4");
      assertThat(parsed.groups().getFirst().transactions().getFirst().findings()).extracting(EnvelopeFinding::code).containsExactly("4", "3");
      assertThat(read("ISA*bad").findings()).extracting(EnvelopeFinding::code).contains("022");
      assertThat(read(Synthetic850.generate(4, "005010", X12Separators.DEFAULT).replace("IEA*1*000000001~", "")).findings()).isNotEmpty();
   }

   /*******************************************************************************
    ** Oversized transactions are rejected while bounded buffers are retained.
    *******************************************************************************/
   @Test
   void bound() throws Exception
   {
      String text = Synthetic850.generate(4, "005010", X12Separators.DEFAULT);
      X12TransactionSet transaction = new X12EnvelopeReader(3).read(new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII))).getFirst().groups().getFirst().transactions().getFirst();
      assertThat(transaction.segments()).hasSize(3);
      assertThat(transaction.findings()).extracting(EnvelopeFinding::code).contains("5");
      assertThatThrownBy(() -> new X12EnvelopeReader(1)).isInstanceOf(IllegalArgumentException.class);
   }

   /*******************************************************************************
    ** Components and repetitions survive tokenization and serialization.
    *******************************************************************************/
   @Test
   void repetitions() throws Exception
   {
      String text = Synthetic850.generate(4, "005010", X12Separators.DEFAULT).replace("BEG*00*NE", "BEG*00:1^01:2*NE");
      X12Segment segment = read(text).groups().getFirst().transactions().getFirst().segments().get(1);
      assertThat(segment.value(1, 2, 2)).isEqualTo("2");
      assertThat(segment.value(100)).isEmpty();
      assertThat(new X12Writer().write(List.of(segment), X12Separators.DEFAULT, "")).contains("00:1^01:2");
      assertThatThrownBy(() -> new X12Writer().write(List.of(segment), new X12Separators('*', '~', ':', null), "")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new X12Writer().write(List.of(X12Segment.of("REF", 1, "bad*value")), X12Separators.DEFAULT, "")).isInstanceOf(IllegalArgumentException.class);
   }
}
