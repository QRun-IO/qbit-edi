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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.edi.map.TransformRegistry;
import com.kingsrook.qbits.edi.storage.ObjectStoragePayloadStore;
import com.kingsrook.qbits.edi.storage.PayloadCrypto;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Transform semantics and encrypted object metadata without provider internals.
 *******************************************************************************/
class MappingTest
{
   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void builtInTransforms() throws Exception
   {
      TransformRegistry registry = new TransformRegistry();
      assertThat(registry.apply("DATE", null, List.of("20261013"))).isEqualTo(LocalDate.of(2026, 10, 13));
      assertThat(registry.apply("DATE", null, List.of("491013"))).isEqualTo(LocalDate.of(2049, 10, 13));
      assertThat(registry.apply("DATE", null, List.of("501013"))).isEqualTo(LocalDate.of(1950, 10, 13));
      assertThat(registry.apply("TIME", null, List.of("1234"))).isEqualTo("12:34");
      assertThat(registry.apply("TIME", null, List.of("12345678"))).isEqualTo("12:34:56.780");
      assertThat(registry.apply("DATETIME", "{\"timezone\":\"America/Chicago\"}", List.of("20261013", "1200"))).isEqualTo(Instant.parse("2026-10-13T17:00:00Z"));
      assertThat(registry.apply("DECIMAL", "{\"scale\":\"2\"}", List.of("1250"))).isEqualTo(new BigDecimal("12.50"));
      assertThat(registry.apply("CONSTANT", "{\"value\":\"synthetic\"}", List.of())).isEqualTo("synthetic");
      assertThat(registry.apply("CONCAT", "{\"separator\":\"-\"}", List.of("A", "B"))).isEqualTo("A-B");
      assertThat(registry.apply("SPLIT", "{\"separator\":\"-\",\"index\":\"1\"}", List.of("A-B"))).isEqualTo("B");
      assertThat(registry.apply("SUBSTRING", "{\"start\":\"1\",\"end\":\"3\"}", List.of("ABCD"))).isEqualTo("BC");
      assertThat(registry.apply("TRIM", null, List.of(" A "))).isEqualTo("A");
      assertThat(registry.apply("UPPER", null, List.of("abc"))).isEqualTo("ABC");
      assertThat(registry.apply("CODE", "{\"EA\":\"each\"}", List.of("EA"))).isEqualTo("each");
      assertThat(registry.apply("QUALIFIER", null, List.of("selected"))).isEqualTo("selected");
      assertThat(registry.apply("UNIT", "{\"factor\":\"12\",\"scale\":\"0\"}", List.of("2"))).isEqualTo(new BigDecimal("24"));
      assertThat(registry.register("HOST", (v, a) -> "custom").apply("HOST", null, List.of())).isEqualTo("custom");
      assertThatThrownBy(() -> registry.apply("MISSING", null, List.of())).hasMessage("Unknown transform");
      assertThatThrownBy(() -> registry.apply("HL", null, List.of())).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> registry.apply("SEQUENCE", null, List.of())).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> registry.apply("CODE", "{}", List.of("CANARY"))).hasMessage("Code is not mapped");
      assertThatThrownBy(() -> registry.apply("DATE", null, List.of())).hasMessage("Transform input is missing");
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void objectStorageEnvelopeMetadata() throws Exception
   {
      var crypto = new PayloadCrypto(k -> Base64.getEncoder().encodeToString(new byte[32]));
      var store = new ObjectStoragePayloadStore(crypto, "edi", Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC));
      String key = store.key(PayloadKind.INBOUND);
      assertThat(key).matches("edi/2026/10/13/inbound/[a-f0-9-]{36}");
      assertThat(store.key(PayloadKind.INBOUND)).isNotEqualTo(key);
      var encrypted = crypto.encrypt("SYNTHETIC-ONLY".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
      assertThat(store.metadata(encrypted)).containsEntry("envelope-version", "1").containsEntry("sha256", encrypted.sha256()).containsEntry("size-bytes", "14");
      assertThat(store.metadata(encrypted).toString()).doesNotContain("SYNTHETIC");
      assertThatThrownBy(() -> store.delete(new com.kingsrook.qbits.edi.storage.PayloadRef("objectstorage:other:edi/test", null, 0))).hasMessageContaining("Invalid EDI object reference");
   }
   /*******************************************************************************
    ** Core ESB's full-record envelope must never receive PHI business identifiers.
    *******************************************************************************/
   @Test
   void documentEventsExcludePhiFields() throws Exception
   {
      var record = new com.kingsrook.qqq.backend.core.model.data.QRecord().withValue("id", 42).withValue("status", "APPLIED")
         .withValue("phiBusinessKey", "PHI-CANARY").withValue("businessKey", "ALSO-SENSITIVE");
      var safe = com.kingsrook.qbits.edi.support.EdiDocumentEvents.identifiers(record);
      assertThat(safe.getValues()).containsOnlyKeys("id", "status");
      assertThat(safe.getValues().toString()).doesNotContain("CANARY", "SENSITIVE");
      assertThat(new com.kingsrook.qbits.edi.support.EdiDocumentEvents().appliesTo("edi_transaction", com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType.UPDATE)).isTrue();
      assertThat(new com.kingsrook.qbits.edi.support.EdiDocumentEvents().appliesTo("edi_map", com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType.UPDATE)).isFalse();
      var tables = com.kingsrook.qbits.edi.metadata.EdiTables.tables(new EdiQBitConfig().withBackendName("test").withEsbDestinationName("synthetic"));
      assertThat(com.kingsrook.qqq.esb.model.EsbTableMetaData.of(tables.stream().filter(t -> t.getName().equals("edi_transaction")).findFirst().orElseThrow())).isNull();
      assertThat(com.kingsrook.qqq.esb.model.EsbTableMetaData.of(tables.stream().filter(t -> t.getName().equals("edi_partner")).findFirst().orElseThrow()).getPublications()).hasSize(1);
   }
}
