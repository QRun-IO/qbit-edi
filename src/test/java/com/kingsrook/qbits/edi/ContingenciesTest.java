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
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

/*******************************************************************************
 ** Contingent metadata and real RC.6 possible-value searches with form values.
 *******************************************************************************/
class ContingenciesTest
{
   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "edi_interchange,originalInterchangeId,partnerId,EQUALS,${input.partnerId}",
      "edi_interchange,originalInterchangeId,agreementId,EQUALS,${input.agreementId}",
      "edi_agreement,partnerIdentityId,partnerId,EQUALS,${input.partnerId}",
      "edi_agreement,localIdentityId,partnerId,IS_BLANK,",
      "edi_agreement,inboundTransportId,partnerId,EQUALS,${input.partnerId}",
      "edi_agreement,outboundTransportId,partnerId,EQUALS,${input.partnerId}",
      "edi_transport,localCertificateId,partnerId,IS_BLANK,",
      "edi_transport,partnerCertificateId,partnerId,EQUALS,${input.partnerId}",
      "edi_transport,partnerTlsCertificateId,partnerId,EQUALS,${input.partnerId}",
      "edi_map,guideId,partnerId,EQUALS,${input.partnerId}",
      "edi_map,guideId,transactionSet,EQUALS,${input.transactionSet}",
      "edi_agreement_document,guideId,transactionSet,EQUALS,${input.transactionSet}",
      "edi_agreement_document,guideId,version,EQUALS,${input.version}",
      "edi_agreement_document,mapId,transactionSet,EQUALS,${input.transactionSet}",
      "edi_agreement_document,mapId,version,EQUALS,${input.version}"
   })
   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   void metadata(String table, String field, String criteriaField, QCriteriaOperator operator, String value) throws Exception
   {
      var instance = H2Support.create();
      var filter = instance.getTable(table).getField(field).getPossibleValueSourceFilter();
      assertThat(filter).isNotNull();
      assertThat(criteria(filter)).anySatisfy(c ->
      {
         assertThat(c.getFieldName()).isEqualTo(criteriaField);
         assertThat(c.getOperator()).isEqualTo(operator);
         if(value != null)
         {
            assertThat(c.getValues()).contains(value);
         }
      });
      if(field.contains("Transport") || table.equals("edi_map"))
      {
         assertThat(orFilters(filter)).isNotEmpty();
         assertThat(criteria(filter)).anySatisfy(c ->
         {
            assertThat(c.getFieldName()).isEqualTo("partnerId");
            assertThat(c.getOperator()).isEqualTo(QCriteriaOperator.IS_BLANK);
         });
      }
   }

   /*******************************************************************************
    ** Verify the synthetic scenario.
    *******************************************************************************/
   @Test
   void searchPartnerIdentitiesAndCertificates() throws Exception
   {
      var instance = H2Support.create();
      SystemWrites.call(() ->
      {
         for(int partner = 1; partner <= 2; partner++)
         {
            EdiRecords.insert("edi_identity", new QRecord().withValue("partnerId", partner).withValue("standard", "X12").withValue("interchangeQualifier", "ZZ")
               .withValue("interchangeId", "SYNTH" + partner).withValue("applicationCode", "APP" + partner));
            EdiRecords.insert("edi_certificate", new QRecord().withValue("partnerId", partner).withValue("alias", "Synthetic " + partner)
               .withValue("certificatePem", As2Support.party("contingency-" + partner, 365).certificatePem()));
         }
         return (null);
      });
      for(String[] pair : List.of(new String[] {"edi_agreement", "partnerIdentityId"}, new String[] {"edi_transport", "partnerCertificateId"}))
      {
         var field = instance.getTable(pair[0]).getField(pair[1]);
         QQueryFilter interpreted = field.getPossibleValueSourceFilter().clone();
         // RC.6 middleware interprets the field filter before calling the search action.
         interpreted.interpretValues(Map.of("partnerId", 1));
         var result = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput().withPossibleValueSourceName(field.getPossibleValueSourceName())
            .withDefaultQueryFilter(interpreted).withOtherValues(Map.of("partnerId", 1)).withSearchTerm("").withLimit(100));
         assertThat(result.getResults()).hasSize(1);
         assertThat(result.getResults().getFirst().getId()).isEqualTo(1);
      }
      assertThat(instance.getTable("edi_exception").getField("assignedToUserId").getPossibleValueSourceFilter()).isNull();
   }

   /*******************************************************************************
    ** RC.6 lacks dependency behavior metadata; shared transports still work with no partner.
    *******************************************************************************/
   @Test
   void sharedTransportBeforeAndAfterPartnerSelection() throws Exception
   {
      var instance = H2Support.create();
      SystemWrites.call(() ->
      {
         for(Integer partner : java.util.Arrays.asList(null, 1, 2))
         {
            SystemWrites.insert("edi_transport", new QRecord().withValue("name", "van-" + partner).withValue("partnerId", partner)
               .withValue("type", "GATEWAY").withValue("direction", "BOTH").withValue("gatewayEndpointName", "synthetic-outbox"));
         }
         return (null);
      });
      for(var values : List.of(Map.<String, java.io.Serializable>of(), Map.<String, java.io.Serializable>of("partnerId", 1)))
      {
         var field = instance.getTable("edi_agreement").getField("outboundTransportId");
         var filter = field.getPossibleValueSourceFilter().clone();
         filter.interpretValues(Map.of("input", values), PossibleValueSearchFilterUseCase.FORM);
         var result = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput()
            .withPossibleValueSourceName(field.getPossibleValueSourceName()).withDefaultQueryFilter(filter)
            .withOtherValues(values).withSearchTerm("").withLimit(100));
         assertThat(result.getResults()).extracting(value -> value.getId().toString()).contains("1").doesNotContain("3");
         assertThat(result.getResults()).hasSize(values.isEmpty() ? 1 : 2);
         filter = field.getPossibleValueSourceFilter().clone();
         filter.interpretValues(Map.of("input", values), PossibleValueSearchFilterUseCase.FORM);
         var unrelated = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput()
            .withPossibleValueSourceName(field.getPossibleValueSourceName()).withDefaultQueryFilter(filter)
            .withOtherValues(values).withSearchTerm("van-2").withLimit(100));
         assertThat(unrelated.getResults()).isEmpty();
      }
   }

   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   static List<QFilterCriteria> criteria(QQueryFilter filter)
   {
      List<QFilterCriteria> result = new ArrayList<>();
      if(filter.getCriteria() != null)
      {
         result.addAll(filter.getCriteria());
      }
      if(filter.getSubFilters() != null)
      {
         filter.getSubFilters().forEach(f -> result.addAll(criteria(f)));
      }
      return (result);
   }

   /*******************************************************************************
    ** Test fixture operation.
    *******************************************************************************/
   static List<QQueryFilter> orFilters(QQueryFilter filter)
   {
      List<QQueryFilter> result = new ArrayList<>();
      if(filter.getBooleanOperator() == QQueryFilter.BooleanOperator.OR)
      {
         result.add(filter);
      }
      if(filter.getSubFilters() != null)
      {
         filter.getSubFilters().forEach(f -> result.addAll(orFilters(f)));
      }
      return (result);
   }
}
