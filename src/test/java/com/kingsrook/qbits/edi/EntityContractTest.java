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


import java.lang.reflect.Field;
import java.time.Instant;
import java.util.stream.Stream;
import com.kingsrook.qbits.edi.model.EdiAgreement;
import com.kingsrook.qbits.edi.model.EdiAgreementDocument;
import com.kingsrook.qbits.edi.model.EdiCertificate;
import com.kingsrook.qbits.edi.model.EdiCodeList;
import com.kingsrook.qbits.edi.model.EdiCodeListValue;
import com.kingsrook.qbits.edi.model.EdiControlNumber;
import com.kingsrook.qbits.edi.model.EdiEvent;
import com.kingsrook.qbits.edi.model.EdiException;
import com.kingsrook.qbits.edi.model.EdiFunctionalGroup;
import com.kingsrook.qbits.edi.model.EdiGuide;
import com.kingsrook.qbits.edi.model.EdiGuideElement;
import com.kingsrook.qbits.edi.model.EdiGuideRule;
import com.kingsrook.qbits.edi.model.EdiGuideSegment;
import com.kingsrook.qbits.edi.model.EdiIdentity;
import com.kingsrook.qbits.edi.model.EdiInbox;
import com.kingsrook.qbits.edi.model.EdiInterchange;
import com.kingsrook.qbits.edi.model.EdiPartner;
import com.kingsrook.qbits.edi.model.EdiPayload;
import com.kingsrook.qbits.edi.model.EdiTransaction;
import com.kingsrook.qbits.edi.model.EdiTransactionError;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Entity mappings must preserve every annotated value through the QQQ record boundary.
 *******************************************************************************/
class EntityContractTest
{
   /*******************************************************************************
    ** All a1 persistent entity types.
    *******************************************************************************/
   static Stream<Class<? extends QRecordEntity>> entities()
   {
      return (Stream.of(EdiPartner.class, EdiIdentity.class, EdiAgreement.class, EdiAgreementDocument.class, EdiControlNumber.class, EdiTransport.class, EdiCertificate.class, EdiGuide.class, EdiGuideSegment.class, EdiGuideElement.class, EdiGuideRule.class, EdiCodeList.class, EdiCodeListValue.class, EdiInterchange.class, EdiFunctionalGroup.class, EdiTransaction.class, EdiTransactionError.class, EdiEvent.class, EdiException.class, EdiInbox.class, EdiPayload.class, com.kingsrook.qbits.edi.model.EdiMap.class, com.kingsrook.qbits.edi.model.EdiMapRule.class, com.kingsrook.qbits.edi.model.EdiMapSample.class, com.kingsrook.qbits.edi.model.EdiOutboundRequest.class, com.kingsrook.qbits.edi.model.EdiOutboundMessage.class, com.kingsrook.qbits.edi.model.EdiAs2Mdn.class));
   }

   /*******************************************************************************
    ** Verify fluent construction, metadata types and QQQ serialization/deserialization together.
    *******************************************************************************/
   @ParameterizedTest
   @MethodSource("entities")
   void roundTrip(Class<? extends QRecordEntity> type) throws Exception
   {
      QRecordEntity entity = type.getConstructor().newInstance();
      QTableMetaData table = new QTableMetaData().withName(type.getField("TABLE_NAME").get(null).toString()).withFieldsFromEntity(type);
      QRecordEntity.registerTable(type, table);
      int count = 0;
      for(Field field : type.getDeclaredFields())
      {
         if(field.getAnnotation(QField.class) == null)
         {
            continue;
         }
         count++;
         String name = field.getName();
         String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
         Object value = field.getType() == Integer.class ? 3 : field.getType() == Long.class ? 42L : field.getType() == Boolean.class ? true
            : field.getType() == Instant.class ? Instant.parse("2026-10-13T00:00:00Z") : field.getType() == byte[].class ? new byte[] {1, 2, 3} : "Synthetic " + name;
         assertThat(type.getMethod("with" + suffix, field.getType()).invoke(entity, value)).isSameAs(entity);
         assertThat(table.getField(name).getBackendName()).isEqualTo(field.getAnnotation(QField.class).backendName());
      }
      QRecord record = entity.toQRecord();
      assertThat(record.getValues()).hasSize(count);
      assertThat(QRecordEntity.fromQRecord(type, record).toQRecord().getValues()).usingRecursiveComparison().isEqualTo(record.getValues());
   }
}
