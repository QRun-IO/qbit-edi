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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import com.kingsrook.qbits.edi.guide.BuiltInGuideLoader;
import com.kingsrook.qbits.edi.guide.Guide;
import com.kingsrook.qbits.edi.guide.GuideRepository;
import com.kingsrook.qbits.edi.guide.GuideValidator;
import com.kingsrook.qbits.edi.storage.DatabasePayloadStore;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadCrypto;
import com.kingsrook.qbits.edi.storage.PayloadKind;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.x12.ControlNumberAllocator;
import com.kingsrook.qbits.edi.x12.X12FunctionalGroup;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*******************************************************************************
 ** Real SQL concurrency, payload crypto, guide revisions and metadata integration.
 *******************************************************************************/
class DatabaseTest
{
   /*******************************************************************************
    ** Transport routing validation runs on insert and on merged partial updates.
    *******************************************************************************/
   @Test
   void transportValidation() throws Exception
   {
      H2Support.create();
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         assertThatThrownBy(() -> EdiRecords.insert("edi_transport", new QRecord().withValue("name", "Bad Slug").withValue("type", "HTTPS"))).hasMessageContaining("insert failed");
         assertThatThrownBy(() -> EdiRecords.insert("edi_transport", new QRecord().withValue("name", "good-slug"))).hasMessageContaining("insert failed");
         QRecord row = EdiRecords.insert("edi_transport", new QRecord().withValue("name", "test-https").withValue("type", "HTTPS"));
         var result = new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput("edi_transport")
            .withRecord(new QRecord().withValue("id", row.getValueInteger("id")).withValue("notes", "Synthetic update"))).getRecords().getFirst();
         assertThat(result.getErrors()).isNullOrEmpty();
         QRecord healthcare = EdiRecords.insert("edi_agreement_document", new QRecord().withValue("transactionSet", "837").withValue("version", "005010X222A1"));
         assertThat(healthcare.getValueBoolean("containsPhi")).isTrue();
         assertThat(healthcare.getValueInteger("snipLevel")).isEqualTo(4);
         QRecord retail = EdiRecords.insert("edi_agreement_document", new QRecord().withValue("transactionSet", "850").withValue("version", "004010"));
         assertThat(retail.getValueBoolean("containsPhi")).isFalse();
         assertThat(retail.getValueInteger("snipLevel")).isEqualTo(2);
      }
   }

   /*******************************************************************************
    ** Release thread-local host state after every case.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Metadata validates on RC.5 and committed DDL derives from that metadata.
    *******************************************************************************/
   @Test
   void metadata() throws Exception
   {
      QInstance instance = H2Support.create();
      assertThat(instance.getTables()).hasSize(27);
      for(QTableMetaData table : instance.getTables().values())
      {
         if(table.getName().equals("edi_preview_download"))
         {
            continue;
         }
         assertThat(table.getName()).startsWith("edi_");
         assertThat(table.getPermissionRules().getPermissionBaseName()).startsWith("edi.");
         assertThat(table.getSections()).isNotEmpty();
         assertThat(table.getSourceQBitName()).isNotBlank();
      }
      String ddl = instance.getTables().values().stream().filter(t -> !t.getName().equals("edi_preview_download")).sorted(java.util.Comparator.comparing(QTableMetaData::getName)).map(t -> H2Support.ddl(t, true) + ";").collect(Collectors.joining("\n\n")) + "\n";
      Path path = Path.of("docs/ddl/edi-postgres.sql");
      if(Boolean.getBoolean("edi.generateDdl"))
      {
         Files.createDirectories(path.getParent());
         Files.writeString(path, ddl);
      }
      else
      {
         assertThat(Files.readString(path).replace("\r\n", "\n")).isEqualTo(ddl);
      }
      assertThat(EdiRuntime.current().reader()).isNotNull();
      assertThat(EdiRuntime.current().writer()).isNotNull();
      assertThat(EdiRuntime.current().validator()).isNotNull();
      assertThat(EdiRuntime.current().guides()).isNotNull();
      assertThat(EdiRuntime.current().controls()).isNotNull();
      assertThat(EdiRuntime.current().ack997()).isNotNull();
      assertThat(EdiRuntime.current().ack999()).isNotNull();
      assertThat(EdiRuntime.current().ta1()).isNotNull();
      assertThat(EdiRuntime.current().acknowledgements()).isNotNull();
   }

   /*******************************************************************************
    ** Eight independent allocators race on first creation and every subsequent CAS.
    *******************************************************************************/
   @Test
   void controlConcurrency() throws Exception
   {
      QInstance instance = H2Support.create();
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(EdiRuntime.current().config().getBackendName());
      Set<Long> numbers = ConcurrentHashMap.newKeySet();
      CountDownLatch start = new CountDownLatch(1);
      try(ExecutorService executor = Executors.newFixedThreadPool(8))
      {
         List<Future<?>> futures = new ArrayList<>();
         for(int thread = 0; thread < 8; thread++)
         {
            futures.add(executor.submit(() ->
            {
               start.await();
               ControlNumberAllocator allocator = new ControlNumberAllocator(backend);
               for(int i = 0; i < 50; i++)
               {
                  assertThat(numbers.add(allocator.allocate(1, null, "ISA", 1, 999999999, true))).isTrue();
               }
               return (null);
            }));
         }
         start.countDown();
         for(Future<?> future : futures)
         {
            future.get(30, TimeUnit.SECONDS);
         }
      }
      assertThat(numbers).containsExactlyInAnyOrderElementsOf(LongStream.rangeClosed(1, 400).boxed().toList());
      ControlNumberAllocator allocator = new ControlNumberAllocator(backend);
      assertThat(allocator.allocate(null, 3, "GS", 7, 8, true)).isEqualTo(7);
      assertThat(allocator.allocate(null, 3, "GS", 7, 8, true)).isEqualTo(8);
      assertThat(allocator.allocate(null, 3, "GS", 7, 8, true)).isEqualTo(7);
      assertThat(allocator.allocate(2, null, "GS", 1, 1, false)).isEqualTo(1);
      assertThatThrownBy(() -> allocator.allocate(2, null, "GS", 1, 1, false)).isInstanceOf(IllegalStateException.class);
      assertThatThrownBy(() -> allocator.allocate(null, null, "ST", 0, 0, false)).isInstanceOf(IllegalArgumentException.class);
   }

   /*******************************************************************************
    ** The payload path performs no temp-directory writes, including during key rotation.
    *******************************************************************************/
   @Test
   void payloadStore(@TempDir Path tempDirectory) throws Exception
   {
      H2Support.create();
      String originalTemp = System.getProperty("java.io.tmpdir");
      System.setProperty("java.io.tmpdir", tempDirectory.toString());
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         com.kingsrook.qbits.edi.storage.PayloadStore store = EdiRuntime.current().payloads();
         byte[] content = "Synthetic payload".getBytes(StandardCharsets.UTF_8);
         // Warm QQQ action/audit infrastructure before measuring payload IO.
         PayloadRef warm = store.put(PayloadKind.SAMPLE, new ByteArrayInputStream(content));
         store.open(warm).close();
         store.delete(warm);
         Set<Path> before;
         try(var paths = Files.walk(Path.of(System.getProperty("java.io.tmpdir"))))
         {
            before = paths.collect(Collectors.toSet());
         }
         PayloadRef ref = store.put(PayloadKind.INBOUND, new ByteArrayInputStream(content));
         assertThat(store.open(ref).readAllBytes()).isEqualTo(content);
         QRecord row = EdiRecords.query("edi_payload", EdiRecords.equal("id", Integer.valueOf(ref.value().substring(9)))).getFirst();
         assertThat(new String((byte[]) row.getValue("ciphertext"), StandardCharsets.ISO_8859_1)).doesNotContain("Synthetic payload");
         String current = Base64.getEncoder().encodeToString(new byte[32]);
         byte[] nextKey = new byte[32];
         Arrays.fill(nextKey, (byte) 7);
         DatabasePayloadStore rotated = new DatabasePayloadStore(new PayloadCrypto(name -> name.endsWith("PREVIOUS") ? current : Base64.getEncoder().encodeToString(nextKey)));
         assertThat(rotated.open(ref).readAllBytes()).isEqualTo(content);
         store.delete(ref);
         assertThatThrownBy(() -> store.open(ref)).isInstanceOf(IllegalArgumentException.class);
         assertThatThrownBy(() -> store.open(new PayloadRef("s3:invalid", "", 0))).isInstanceOf(IllegalArgumentException.class);
         try(var paths = Files.walk(Path.of(System.getProperty("java.io.tmpdir"))))
         {
            assertThat(paths.filter(p -> !before.contains(p)).toList()).isEmpty();
         }
      }
      finally
      {
         System.setProperty("java.io.tmpdir", originalTemp);
      }
   }

   /*******************************************************************************
    ** Ciphertext, wrapped keys and metadata are authenticated; key errors disclose no values.
    *******************************************************************************/
   @Test
   void cryptoTampering() throws Exception
   {
      PayloadCrypto crypto = new PayloadCrypto(name -> Base64.getEncoder().encodeToString(new byte[32]));
      PayloadCrypto.Encrypted encrypted = crypto.encrypt(new byte[] {1, 2, 3});
      assertThat(crypto.decrypt(encrypted)).containsExactly(1, 2, 3);
      for(int position : new int[] {0, 1, 20, 70, encrypted.ciphertext().length - 1})
      {
         byte[] tampered = encrypted.ciphertext().clone();
         tampered[position] ^= 1;
         assertThatThrownBy(() -> crypto.decrypt(new PayloadCrypto.Encrypted(tampered, encrypted.keyId(), encrypted.sha256(), encrypted.sizeBytes())))
            .isInstanceOf(GeneralSecurityException.class);
      }
      assertThatThrownBy(() -> crypto.decrypt(new PayloadCrypto.Encrypted(encrypted.ciphertext(), encrypted.keyId(), "bad", 3))).isInstanceOf(GeneralSecurityException.class);
      assertThatThrownBy(() -> new PayloadCrypto(name -> null).encrypt(new byte[0])).hasMessageContaining("QQQ_EDI_PAYLOAD_KEY");
      assertThatThrownBy(() -> new PayloadCrypto(name -> "secret-canary").encrypt(new byte[0])).hasMessageNotContaining("secret-canary");
      assertThatThrownBy(() -> new PayloadCrypto(name -> "AA==").encrypt(new byte[0])).isInstanceOf(GeneralSecurityException.class);
   }

   /*******************************************************************************
    ** Startup is idempotent, changes create revisions, and normalized table guides validate.
    *******************************************************************************/
   @Test
   void guides() throws Exception
   {
      H2Support.create();
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         List<Integer> ids = EdiRuntime.current().initialize();
         assertThat(ids).hasSize(5);
         assertThat(EdiRuntime.current().initialize()).isEqualTo(ids);
         Guide guide = new GuideRepository().load(ids.getFirst());
         var transaction = X12Test.read(Synthetic850.generate(1, "004010", new X12Separators('*', '~', ':', null))).groups().getFirst().transactions().getFirst();
         assertThat(new GuideValidator().validate(transaction, guide, false)).isEmpty();
         byte[] changed;
         try(var input = getClass().getResourceAsStream("/edi/guides/850-004010.json"))
         {
            changed = (new String(input.readAllBytes(), StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8);
         }
         Integer newId = new BuiltInGuideLoader().upsert(changed);
         assertThat(newId).isNotEqualTo(ids.getFirst());
         assertThat(new GuideRepository().load(ids.getFirst()).name()).isEqualTo(guide.name());
         assertThat(EdiRecords.query("edi_guide", EdiRecords.equal("name", guide.name()))).hasSize(2);
      }
   }

   /*******************************************************************************
    ** Persisted identities and windows constrain interchange and group duplicate detection.
    *******************************************************************************/
   @Test
   void duplicates() throws Exception
   {
      H2Support.create();
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         var interchange = X12Test.read(Synthetic850.generate(1, "005010", X12Separators.DEFAULT));
         var detector = EdiRuntime.current().duplicates();
         assertThat(detector.interchange(1, interchange.isa(), 365, Instant.now())).isFalse();
         EdiRecords.insert("edi_interchange", new QRecord().withValue("agreementId", 1).withValue("senderQualifier", "ZZ").withValue("senderId", "SYNTHETIC")
            .withValue("receiverQualifier", "ZZ").withValue("receiverId", "QQQTEST").withValue("controlNumber", "000000001").withValue("usageIndicator", "T"));
         assertThat(detector.interchange(1, interchange.isa(), 365, Instant.now())).isTrue();
         assertThat(detector.interchange(2, interchange.isa(), 365, Instant.now())).isFalse();
         EdiRecords.insert("edi_functional_group", new QRecord().withValue("senderId", "SYNTHETIC").withValue("controlNumber", "1"));
         assertThat(detector.group("SYNTHETIC", "1", 365, Instant.now())).isTrue();
         assertThat(detector.group("OTHER", "1", 365, Instant.now())).isFalse();
         var group = interchange.groups().getFirst();
         assertThat(detector.transactions(new X12FunctionalGroup(group.gs(), group.ge(), List.of(group.transactions().getFirst(), group.transactions().getFirst()), List.of()))).containsExactly("0001");
      }
   }
}
