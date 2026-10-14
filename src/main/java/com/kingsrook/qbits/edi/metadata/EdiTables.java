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

package com.kingsrook.qbits.edi.metadata;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.edi.EdiQBitConfig;
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
import com.kingsrook.qbits.edi.model.EdiMap;
import com.kingsrook.qbits.edi.model.EdiMapRule;
import com.kingsrook.qbits.edi.model.EdiMapSample;
import com.kingsrook.qbits.edi.model.EdiOutboundMessage;
import com.kingsrook.qbits.edi.model.EdiOutboundRequest;
import com.kingsrook.qbits.edi.model.EdiPartner;
import com.kingsrook.qbits.edi.model.EdiPayload;
import com.kingsrook.qbits.edi.model.EdiTransaction;
import com.kingsrook.qbits.edi.model.EdiTransactionError;
import com.kingsrook.qbits.edi.model.EdiTransport;
import com.kingsrook.qbits.edi.security.EdiClassifications;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Tables, relations and value sources for the EDI data model.
 *******************************************************************************/
public final class EdiTables
{
   /*******************************************************************************
    ** Build annotated tables and complete record screens.
    *******************************************************************************/
   public static List<QTableMetaData> tables(EdiQBitConfig config) throws QException
   {
      List<QTableMetaData> tables = new ArrayList<>();
      QTableMetaData partner = base(config, EdiPartner.TABLE_NAME, EdiPartner.class, "name", false);
      partner.withUniqueKey(new UniqueKey("name"));
      partner.withUniqueKey(new UniqueKey("code"));
      tables.add(partner);
      QTableMetaData identity = base(config, EdiIdentity.TABLE_NAME, EdiIdentity.class, "id", false);
      identity.withUniqueKey(new UniqueKey("standard", "interchangeQualifier", "interchangeId", "applicationCode"));
      tables.add(identity);
      QTableMetaData agreement = base(config, EdiAgreement.TABLE_NAME, EdiAgreement.class, "id", false);
      agreement.withUniqueKey(new UniqueKey("localIdentityId", "partnerIdentityId", "standard", "usageIndicator"));
      tables.add(agreement);
      QTableMetaData agreementDocument = base(config, EdiAgreementDocument.TABLE_NAME, EdiAgreementDocument.class, "id", false);
      agreementDocument.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(AgreementDocumentDefaults.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(AgreementDocumentDefaults.class));
      agreementDocument.withUniqueKey(new UniqueKey("agreementId", "direction", "transactionSet", "version"));
      tables.add(agreementDocument);
      QTableMetaData controlNumber = base(config, EdiControlNumber.TABLE_NAME, EdiControlNumber.class, "id", false);
      controlNumber.getField("scopeKey").withIsHidden(true).withIsEditable(false);
      controlNumber.getSections().forEach(section -> section.getFieldNames().remove("scopeKey"));
      controlNumber.withUniqueKey(new UniqueKey("scopeKey", "level"));
      tables.add(controlNumber);
      QTableMetaData transport = base(config, EdiTransport.TABLE_NAME, EdiTransport.class, "name", false);
      transport.withUniqueKey(new UniqueKey("name"));
      transport.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(TransportValidation.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(TransportValidation.class));
      tables.add(transport);
      QTableMetaData certificate = base(config, EdiCertificate.TABLE_NAME, EdiCertificate.class, "alias", false);
      tables.add(certificate);
      QTableMetaData guide = base(config, EdiGuide.TABLE_NAME, EdiGuide.class, "name", false);
      guide.withUniqueKey(new UniqueKey("name", "revision"));
      guide.withRecordSecurityLock(new RecordSecurityLock().withFieldName("source").withSecurityKeyType("edi_guide_source").withLockScope(RecordSecurityLock.LockScope.WRITE));
      tables.add(guide);
      QTableMetaData guideSegment = base(config, EdiGuideSegment.TABLE_NAME, EdiGuideSegment.class, "name", false);
      tables.add(guideSegment);
      QTableMetaData guideElement = base(config, EdiGuideElement.TABLE_NAME, EdiGuideElement.class, "name", false);
      tables.add(guideElement);
      QTableMetaData guideRule = base(config, EdiGuideRule.TABLE_NAME, EdiGuideRule.class, "id", false);
      tables.add(guideRule);
      QTableMetaData codeList = base(config, EdiCodeList.TABLE_NAME, EdiCodeList.class, "name", false);
      tables.add(codeList);
      QTableMetaData codeListValue = base(config, EdiCodeListValue.TABLE_NAME, EdiCodeListValue.class, "code", false);
      codeListValue.withUniqueKey(new UniqueKey("codeListId", "code"));
      tables.add(codeListValue);
      QTableMetaData interchange = base(config, EdiInterchange.TABLE_NAME, EdiInterchange.class, "controlNumber", true);
      tables.add(interchange);
      QTableMetaData functionalGroup = base(config, EdiFunctionalGroup.TABLE_NAME, EdiFunctionalGroup.class, "controlNumber", true);
      tables.add(functionalGroup);
      QTableMetaData transaction = base(config, EdiTransaction.TABLE_NAME, EdiTransaction.class, "controlNumber", true);
      tables.add(transaction);
      QTableMetaData transactionError = base(config, EdiTransactionError.TABLE_NAME, EdiTransactionError.class, "id", true);
      tables.add(transactionError);
      QTableMetaData event = base(config, EdiEvent.TABLE_NAME, EdiEvent.class, "id", true);
      tables.add(event);
      QTableMetaData exception = base(config, EdiException.TABLE_NAME, EdiException.class, "summary", false);
      tables.add(exception);
      QTableMetaData inbox = base(config, EdiInbox.TABLE_NAME, EdiInbox.class, "id", true);
      inbox.withUniqueKey(new UniqueKey("transportType", "externalId"));
      tables.add(inbox);
      QTableMetaData payload = base(config, EdiPayload.TABLE_NAME, EdiPayload.class, "kind", true);
      tables.add(payload);
      tables.add(base(config, "edi_map", EdiMap.class, "name", false));
      tables.add(base(config, "edi_map_rule", EdiMapRule.class, "id", false));
      tables.add(base(config, "edi_map_sample", EdiMapSample.class, "name", false));
      tables.add(base(config, "edi_outbound_request", EdiOutboundRequest.class, "id", true));
      tables.add(base(config, "edi_outbound_message", EdiOutboundMessage.class, "id", true));
      for(QTableMetaData table : tables)
      {
         List<String> guidePath = switch(table.getName())
         {
            case "edi_guide_segment" -> List.of("edi_guide_guide_id_edi_guide_segment");
            case "edi_guide_rule" -> List.of("edi_guide_guide_id_edi_guide_rule");
            case "edi_guide_element" -> List.of("edi_guide_guide_id_edi_guide_segment", "edi_guide_segment_guide_segment_id_edi_guide_element");
            default -> List.of();
         };
         if(!guidePath.isEmpty())
         {
            table.withRecordSecurityLock(new RecordSecurityLock().withFieldName("edi_guide.source").withSecurityKeyType("edi_guide_source")
               .withLockScope(RecordSecurityLock.LockScope.WRITE).withJoinNameChain(guidePath));
         }
         if(config.getPartnerLocks() && !table.getFields().containsKey("partnerId"))
         {
            List<String> path = switch(table.getName())
            {
               case "edi_agreement_document" -> List.of("edi_agreement_agreement_id_edi_agreement_document");
               case "edi_control_number" -> List.of("edi_agreement_agreement_id_edi_control_number");
               case "edi_functional_group" -> List.of("edi_interchange_interchange_id_edi_functional_group");
               case "edi_transaction_error" -> List.of("edi_transaction_transaction_id_edi_transaction_error");
               case "edi_event" -> List.of("edi_interchange_interchange_id_edi_event");
               case "edi_map_rule" -> List.of("edi_map_map_id_edi_map_rule");
               case "edi_map_sample" -> List.of("edi_map_map_id_edi_map_sample");
               case "edi_guide_segment" -> List.of("edi_guide_guide_id_edi_guide_segment");
               case "edi_guide_rule" -> List.of("edi_guide_guide_id_edi_guide_rule");
               case "edi_guide_element" -> List.of("edi_guide_guide_id_edi_guide_segment", "edi_guide_segment_guide_segment_id_edi_guide_element");
               default -> List.of();
            };
            if(!path.isEmpty())
            {
               String parent = switch(table.getName())
               {
                  case "edi_agreement_document", "edi_control_number" -> "edi_agreement";
                  case "edi_functional_group", "edi_event" -> "edi_interchange";
                  case "edi_transaction_error" -> "edi_transaction";
                  case "edi_map_rule", "edi_map_sample" -> "edi_map";
                  default -> "edi_guide";
               };
               table.withRecordSecurityLock(new RecordSecurityLock().withFieldName(parent + ".partnerId").withSecurityKeyType("edi_partner").withJoinNameChain(path));
            }
         }
         for(QJoinMetaData join : joins())
         {
            if(join.getLeftTable().equals(table.getName()))
            {
               table.withExposedJoin(new ExposedJoin().withLabel(joinLabel(join)).withJoinTable(join.getRightTable()).withJoinPath(List.of(join.getName())));
               table.withSection(new QFieldSection().withName(join.getName()).withLabel(joinLabel(join)).withTier(Tier.T2).withWidgetName(join.getName()));
            }
         }
      }
      for(QTableMetaData table : tables)
      {
         if(com.kingsrook.qbits.edi.app.EdiHome.ACTIVITY.contains(table.getName()) || com.kingsrook.qbits.edi.app.EdiHome.SETUP.contains(table.getName()))
         {
            table.withSearchFields(table.getFields().values().stream().filter(f -> List.of("id", "name", "code", "controlNumber", "businessKey", "secondaryKey", "summary", "alias").contains(f.getName())).map(QFieldMetaData::getName).toList());
         }
      }
      tables.forEach(t -> com.kingsrook.qbits.edi.support.EdiIntegrations.configure(t, config.getEsbDestinationName()));
      EdiContingencies.apply(tables);
      return (tables);
   }

   /*******************************************************************************
    ** Common permissions, audit, field names and optional partner isolation.
    *******************************************************************************/
   private static QTableMetaData base(EdiQBitConfig config, String name, Class<? extends QRecordEntity> entity, String label, boolean readOnly) throws QException
   {
      QTableMetaData table = new QTableMetaData().withName(name).withBackendName(config.getBackendName())
         .withPrimaryKeyField("id").withFieldsFromEntity(entity).withRecordLabelFormatAndFields("%s", label)
         .withPermissionRules(new QPermissionRules().withPermissionBaseName("edi." + name.substring(4)).withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS))
         .withAuditRules(new QAuditRules().withAuditLevel(readOnly ? AuditLevel.RECORD : AuditLevel.FIELD));
      QRecordEntity.registerTable(entity, table);
      table.setLabel(switch(name)
      {
         case "edi_partner" -> "Trading Partner";
         case "edi_transaction" -> "Document";
         case "edi_guide" -> "Implementation Guide";
         case "edi_transaction_error" -> "Validation Error";
         case "edi_inbox" -> "Inbound Message";
         default -> name.substring(4).replace('_', ' ');
      });
      if(List.of("edi_payload", "edi_certificate", "edi_inbox").contains(name))
      {
         table.withSupplementalMetaData(new AiTableMetaData().withExposure(AiExposure.NONE));
      }
      if(name.equals("edi_exception"))
      {
         table.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_DELETE);
         table.getFields().values().forEach(field -> field.setIsEditable(List.of("status", "assignedToUserId", "resolution").contains(field.getName())));
      }
      if(readOnly)
      {
         table.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      }
      if(config.getPartnerLocks() && table.getFields().containsKey("partnerId"))
      {
         table.withRecordSecurityLock(new RecordSecurityLock().withFieldName("partnerId").withSecurityKeyType("edi_partner"));
      }
      List<String> identity = new ArrayList<>(List.of("id"));
      List<String> details = new ArrayList<>();
      List<String> dates = new ArrayList<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         String classification = List.of("phiBusinessKey", "fileName", "ciphertext", "expectedResultJson", "valueExcerpt").contains(field.getName()) ? "PHI"
            : List.of("contactName", "contactEmail", "contactPhone").contains(field.getName()) ? "PII" : "CONFIDENTIAL";
         field.withSupplementalMetaData(new EdiClassifications(classification));
         if(classification.equals("PHI"))
         {
            field.withSupplementalMetaData(new AiFieldMetaData().withExposure(AiExposure.NONE));
         }
         if(field.getType() == QFieldType.DATE_TIME)
         {
            dates.add(field.getName());
         }
         else if(!field.getName().equals("id"))
         {
            (field.getName().equals(label) || field.getName().endsWith("Id") ? identity : details).add(field.getName());
         }
      }
      table.withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, identity))
         .withSection(new QFieldSection("details", new QIcon("description"), Tier.T2, details))
         .withSection(new QFieldSection("dates", new QIcon("schedule"), Tier.T3, dates));
      return (table);
   }

   /*******************************************************************************
    ** One source for each enumeration and referenced table.
    *******************************************************************************/
   public static List<QPossibleValueSource> possibleValueSources()
   {
      List<QPossibleValueSource> sources = new ArrayList<>();
      sources.add(enumSource("edi_standard", "X12", "EDIFACT"));
      sources.add(enumSource("edi_partner_status", "TESTING", "ACTIVE", "SUSPENDED", "RETIRED"));
      sources.add(enumSource("edi_usage_indicator", "T", "P"));
      sources.add(enumSource("edi_line_ending", "NONE", "LF", "CRLF"));
      sources.add(enumSource("edi_send_ta1", "NEVER", "ON_REQUEST", "ON_ERROR", "ALWAYS"));
      sources.add(enumSource("edi_control_number_scope", "AGREEMENT", "LOCAL_IDENTITY"));
      sources.add(enumSource("edi_on_duplicate", "REJECT_TA1", "IGNORE", "PROCESS"));
      sources.add(enumSource("edi_direction", "INBOUND", "OUTBOUND"));
      sources.add(enumSource("edi_transport_direction", "INBOUND", "OUTBOUND", "BOTH"));
      sources.add(enumSource("edi_ack_expectation", "NONE", "997", "999"));
      sources.add(enumSource("edi_ack_level", "GROUP_ONLY", "ALL_TRANSACTIONS", "ERRORS_ONLY"));
      sources.add(enumSource("edi_batch_mode", "IMMEDIATE", "BATCH"));
      sources.add(enumSource("edi_outbound_trigger", "ESB_EVENT", "RECORD_CHANGE", "MANUAL"));
      sources.add(enumSource("edi_level", "ISA", "GS"));
      sources.add(enumSource("edi_transport_type", "AS2", "SFTP", "GATEWAY", "HTTPS", "EMAIL"));
      sources.add(enumSource("edi_sign_algorithm", "SHA256", "SHA384", "SHA512", "SHA1"));
      sources.add(enumSource("edi_encrypt_algorithm", "AES128_CBC", "AES256_CBC", "AES128_GCM", "AES256_GCM", "3DES"));
      sources.add(enumSource("edi_mdn_mode", "SYNC", "ASYNC", "NONE"));
      sources.add(enumSource("edi_sftp_after_pickup", "DELETE", "MOVE"));
      sources.add(enumSource("edi_https_auth_type", "NONE", "BASIC", "BEARER", "HMAC", "MTLS"));
      sources.add(enumSource("edi_https_inbound_auth", "BASIC", "BEARER", "HMAC"));
      sources.add(enumSource("edi_certificate_status", "ACTIVE", "NEXT", "RETIRED"));
      sources.add(enumSource("edi_certificate_usage", "SIGNING", "ENCRYPTION", "SIGNING_AND_ENCRYPTION", "TLS"));
      sources.add(enumSource("edi_guide_status", "DRAFT", "ACTIVE", "RETIRED"));
      sources.add(enumSource("edi_guide_source", "BUILT_IN", "IMPORTED", "CUSTOM"));
      sources.add(enumSource("edi_usage", "REQUIRED", "SITUATIONAL", "NOT_USED"));
      sources.add(enumSource("edi_data_type", "AN", "ID", "N0", "N1", "N2", "N3", "N4", "N5", "N6", "N7", "N8", "N9", "R", "DT", "TM", "B"));
      sources.add(enumSource("edi_rule_type", "PAIRED", "REQUIRED", "EXCLUSION", "CONDITIONAL", "LIST_CONDITIONAL", "SITUATIONAL"));
      sources.add(enumSource("edi_severity", "ERROR", "WARNING"));
      sources.add(enumSource("edi_code_list_source", "BUILT_IN", "CUSTOM", "EXTERNAL"));
      sources.add(enumSource("edi_interchange_status", "RECEIVED", "ACCEPTED", "ACCEPTED_WITH_ERRORS", "REJECTED", "DUPLICATE", "ENVELOPED", "SENT", "DELIVERED", "FAILED"));
      sources.add(enumSource("edi_ack_status", "A", "E", "P", "R", "NONE", "PENDING"));
      sources.add(enumSource("edi_transaction_status", "RECEIVED", "VALID", "INVALID", "MAPPED", "APPLIED", "EXCEPTION", "QUEUED", "SENT", "ACKNOWLEDGED", "REJECTED", "IGNORED"));
      sources.add(enumSource("edi_event_type", "RECEIVED", "STORED", "PARSED", "VALIDATED", "ACK_SENT", "ACK_QUEUED", "MAPPED", "APPLIED", "ENVELOPED", "SENT", "MDN", "DELIVERED", "ACK_RECEIVED", "EXCEPTION", "REPROCESSED", "RESENT"));
      sources.add(enumSource("edi_exception_type", "UNKNOWN_PARTNER", "PARTNER_MISMATCH", "NOT_CONFIGURED", "DUPLICATE", "ENVELOPE", "VALIDATION", "MAPPING", "APPLY", "TRANSPORT", "REJECTED_ACK", "OVERDUE_ACK", "DEAD_LETTER"));
      sources.add(enumSource("edi_exception_status", "OPEN", "IN_PROGRESS", "RESOLVED", "IGNORED"));
      sources.add(enumSource("edi_inbox_status", "PENDING", "PROCESSING", "PROCESSED", "RETRY_WAIT", "DEAD_LETTER", "DUPLICATE", "IGNORED"));
      sources.add(enumSource("edi_payload_kind", "INBOUND", "OUTBOUND", "AS2", "MDN", "SAMPLE"));
      sources.add(QPossibleValueSource.newForTable("edi_partner"));
      sources.add(QPossibleValueSource.newForTable("edi_identity"));
      sources.add(QPossibleValueSource.newForTable("edi_agreement"));
      sources.add(QPossibleValueSource.newForTable("edi_agreement_document"));
      sources.add(QPossibleValueSource.newForTable("edi_control_number"));
      sources.add(QPossibleValueSource.newForTable("edi_transport"));
      sources.add(QPossibleValueSource.newForTable("edi_certificate"));
      sources.add(QPossibleValueSource.newForTable("edi_guide"));
      sources.add(QPossibleValueSource.newForTable("edi_guide_segment"));
      sources.add(QPossibleValueSource.newForTable("edi_guide_element"));
      sources.add(QPossibleValueSource.newForTable("edi_guide_rule"));
      sources.add(QPossibleValueSource.newForTable("edi_code_list"));
      sources.add(QPossibleValueSource.newForTable("edi_code_list_value"));
      sources.add(QPossibleValueSource.newForTable("edi_interchange"));
      sources.add(QPossibleValueSource.newForTable("edi_functional_group"));
      sources.add(QPossibleValueSource.newForTable("edi_transaction"));
      sources.add(QPossibleValueSource.newForTable("edi_transaction_error"));
      sources.add(QPossibleValueSource.newForTable("edi_event"));
      sources.add(QPossibleValueSource.newForTable("edi_exception"));
      sources.add(QPossibleValueSource.newForTable("edi_inbox"));
      sources.add(QPossibleValueSource.newForTable("edi_payload"));
      sources.add(enumSource("edi_on_match", "UPDATE", "SKIP", "EXCEPTION"));
      sources.add(QPossibleValueSource.newForTable("edi_map"));
      sources.add(QPossibleValueSource.newForTable("edi_map_rule"));
      sources.add(QPossibleValueSource.newForTable("edi_map_sample"));
      sources.add(QPossibleValueSource.newForTable("edi_outbound_request"));
      sources.add(QPossibleValueSource.newForTable("edi_outbound_message"));
      return (sources);
   }

   /*******************************************************************************
    ** Stable string ids for enum values.
    *******************************************************************************/
   private static QPossibleValueSource enumSource(String name, String... values)
   {
      List<QPossibleValue<?>> entries = new ArrayList<>();
      for(String value : values)
      {
         entries.add(new QPossibleValue<>(value, value));
      }
      return (new QPossibleValueSource().withName(name).withType(QPossibleValueSourceType.ENUM).withEnumValues(entries));
   }

   /*******************************************************************************
    ** Foreign-key relationships used by parent record screens.
    *******************************************************************************/
   public static List<QJoinMetaData> joins()
   {
      List<QJoinMetaData> joins = new ArrayList<>();
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_identity").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_identity").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_agreement").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_agreement").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_identity_local_identity_id_edi_agreement").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_identity").withRightTable("edi_agreement").withJoinOn(new JoinOn("id", "localIdentityId")));
      joins.add(new QJoinMetaData().withName("edi_identity_partner_identity_id_edi_agreement").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_identity").withRightTable("edi_agreement").withJoinOn(new JoinOn("id", "partnerIdentityId")));
      joins.add(new QJoinMetaData().withName("edi_transport_inbound_transport_id_edi_agreement").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_transport").withRightTable("edi_agreement").withJoinOn(new JoinOn("id", "inboundTransportId")));
      joins.add(new QJoinMetaData().withName("edi_transport_outbound_transport_id_edi_agreement").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_transport").withRightTable("edi_agreement").withJoinOn(new JoinOn("id", "outboundTransportId")));
      joins.add(new QJoinMetaData().withName("edi_agreement_agreement_id_edi_agreement_document").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_agreement").withRightTable("edi_agreement_document").withJoinOn(new JoinOn("id", "agreementId")));
      joins.add(new QJoinMetaData().withName("edi_guide_guide_id_edi_agreement_document").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_guide").withRightTable("edi_agreement_document").withJoinOn(new JoinOn("id", "guideId")));
      joins.add(new QJoinMetaData().withName("edi_agreement_agreement_id_edi_control_number").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_agreement").withRightTable("edi_control_number").withJoinOn(new JoinOn("id", "agreementId")));
      joins.add(new QJoinMetaData().withName("edi_identity_local_identity_id_edi_control_number").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_identity").withRightTable("edi_control_number").withJoinOn(new JoinOn("id", "localIdentityId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_transport").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_transport").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_certificate_local_certificate_id_edi_transport").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_certificate").withRightTable("edi_transport").withJoinOn(new JoinOn("id", "localCertificateId")));
      joins.add(new QJoinMetaData().withName("edi_certificate_partner_certificate_id_edi_transport").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_certificate").withRightTable("edi_transport").withJoinOn(new JoinOn("id", "partnerCertificateId")));
      joins.add(new QJoinMetaData().withName("edi_certificate_partner_tls_certificate_id_edi_transport").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_certificate").withRightTable("edi_transport").withJoinOn(new JoinOn("id", "partnerTlsCertificateId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_certificate").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_certificate").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_guide").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_guide").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_guide_guide_id_edi_guide_segment").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_guide").withRightTable("edi_guide_segment").withJoinOn(new JoinOn("id", "guideId")));
      joins.add(new QJoinMetaData().withName("edi_guide_segment_guide_segment_id_edi_guide_element").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_guide_segment").withRightTable("edi_guide_element").withJoinOn(new JoinOn("id", "guideSegmentId")));
      joins.add(new QJoinMetaData().withName("edi_code_list_code_list_id_edi_guide_element").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_code_list").withRightTable("edi_guide_element").withJoinOn(new JoinOn("id", "codeListId")));
      joins.add(new QJoinMetaData().withName("edi_guide_guide_id_edi_guide_rule").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_guide").withRightTable("edi_guide_rule").withJoinOn(new JoinOn("id", "guideId")));
      joins.add(new QJoinMetaData().withName("edi_guide_segment_guide_segment_id_edi_guide_rule").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_guide_segment").withRightTable("edi_guide_rule").withJoinOn(new JoinOn("id", "guideSegmentId")));
      joins.add(new QJoinMetaData().withName("edi_code_list_code_list_id_edi_code_list_value").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_code_list").withRightTable("edi_code_list_value").withJoinOn(new JoinOn("id", "codeListId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_interchange").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_interchange").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_agreement_agreement_id_edi_interchange").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_agreement").withRightTable("edi_interchange").withJoinOn(new JoinOn("id", "agreementId")));
      joins.add(new QJoinMetaData().withName("edi_transport_transport_id_edi_interchange").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_transport").withRightTable("edi_interchange").withJoinOn(new JoinOn("id", "transportId")));
      joins.add(new QJoinMetaData().withName("edi_inbox_inbox_id_edi_interchange").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_inbox").withRightTable("edi_interchange").withJoinOn(new JoinOn("id", "inboxId")));
      joins.add(new QJoinMetaData().withName("edi_interchange_interchange_id_edi_functional_group").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_interchange").withRightTable("edi_functional_group").withJoinOn(new JoinOn("id", "interchangeId")));
      joins.add(new QJoinMetaData().withName("edi_interchange_interchange_id_edi_transaction").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_interchange").withRightTable("edi_transaction").withJoinOn(new JoinOn("id", "interchangeId")));
      joins.add(new QJoinMetaData().withName("edi_functional_group_functional_group_id_edi_transaction").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_functional_group").withRightTable("edi_transaction").withJoinOn(new JoinOn("id", "functionalGroupId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_transaction").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_transaction").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_agreement_document_agreement_document_id_edi_transaction").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_agreement_document").withRightTable("edi_transaction").withJoinOn(new JoinOn("id", "agreementDocumentId")));
      joins.add(new QJoinMetaData().withName("edi_guide_guide_id_edi_transaction").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_guide").withRightTable("edi_transaction").withJoinOn(new JoinOn("id", "guideId")));
      joins.add(new QJoinMetaData().withName("edi_transaction_transaction_id_edi_transaction_error").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_transaction").withRightTable("edi_transaction_error").withJoinOn(new JoinOn("id", "transactionId")));
      joins.add(new QJoinMetaData().withName("edi_interchange_interchange_id_edi_event").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_interchange").withRightTable("edi_event").withJoinOn(new JoinOn("id", "interchangeId")));
      joins.add(new QJoinMetaData().withName("edi_transaction_transaction_id_edi_event").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_transaction").withRightTable("edi_event").withJoinOn(new JoinOn("id", "transactionId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_exception").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_exception").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_inbox_inbox_id_edi_exception").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_inbox").withRightTable("edi_exception").withJoinOn(new JoinOn("id", "inboxId")));
      joins.add(new QJoinMetaData().withName("edi_interchange_interchange_id_edi_exception").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_interchange").withRightTable("edi_exception").withJoinOn(new JoinOn("id", "interchangeId")));
      joins.add(new QJoinMetaData().withName("edi_transaction_transaction_id_edi_exception").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_transaction").withRightTable("edi_exception").withJoinOn(new JoinOn("id", "transactionId")));
      joins.add(new QJoinMetaData().withName("edi_transport_transport_id_edi_inbox").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_transport").withRightTable("edi_inbox").withJoinOn(new JoinOn("id", "transportId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_inbox").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_inbox").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_map_map_id_edi_map_rule").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_map").withRightTable("edi_map_rule").withJoinOn(new JoinOn("id", "mapId")));
      joins.add(new QJoinMetaData().withName("edi_map_map_id_edi_map_sample").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_map").withRightTable("edi_map_sample").withJoinOn(new JoinOn("id", "mapId")));
      joins.add(new QJoinMetaData().withName("edi_partner_partner_id_edi_map").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_partner").withRightTable("edi_map").withJoinOn(new JoinOn("id", "partnerId")));
      joins.add(new QJoinMetaData().withName("edi_map_map_id_edi_agreement_document").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_map").withRightTable("edi_agreement_document").withJoinOn(new JoinOn("id", "mapId")));
      joins.add(new QJoinMetaData().withName("edi_agreement_document_agreement_document_id_edi_outbound_request").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_agreement_document").withRightTable("edi_outbound_request").withJoinOn(new JoinOn("id", "agreementDocumentId")));
      joins.add(new QJoinMetaData().withName("edi_agreement_agreement_id_edi_outbound_message").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_agreement").withRightTable("edi_outbound_message").withJoinOn(new JoinOn("id", "agreementId")));
      joins.add(new QJoinMetaData().withName("edi_outbound_message_outbound_message_id_edi_interchange").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_outbound_message").withRightTable("edi_interchange").withJoinOn(new JoinOn("id", "outboundMessageId")));
      joins.add(new QJoinMetaData().withName("edi_inbox_inbox_id_edi_event").withType(JoinType.ONE_TO_MANY).withLeftTable("edi_inbox").withRightTable("edi_event").withJoinOn(new JoinOn("id", "inboxId")));
      return (joins);
   }

   /*******************************************************************************
    ** Typed child lists.
    *******************************************************************************/
   public static List<QWidgetMetaData> childWidgets()
   {
      return (joins().stream().map(join -> ChildRecordListRenderer.widgetMetaDataBuilder(join).withName(join.getName())
         .withLabel(joinLabel(join)).withCanAddChildRecord(false).getWidgetMetaData().withGridColumns(12)).toList());
   }

   /*******************************************************************************
    ** Distinguish multiple relationships using readable field labels.
    *******************************************************************************/
   private static String joinLabel(QJoinMetaData join)
   {
      String role = join.getJoinOns().getFirst().getRightField().replaceAll("([a-z])([A-Z])", "$1 $2").replace(" Id", "");
      return (join.getRightTable().substring(4).replace('_', ' ') + " (" + role.toLowerCase(java.util.Locale.ROOT) + ")");
   }
}
