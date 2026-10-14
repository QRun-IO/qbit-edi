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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 ** Operator vocabulary and scan order shared by lists, records and child widgets.
 *******************************************************************************/
public final class EdiTablePresentation
{
   private static final Set<String> ACRONYMS = Set.of("ID", "PHI", "PII", "URL", "ISA", "GS", "ST", "AS2", "SFTP", "HTTPS", "HTTP", "MDN", "TLS", "TA1", "EDI", "UPC", "NPI", "QQQ", "JSON", "PEM", "SHA256", "SNIP", "VAN", "AK3", "AK4", "IK3", "IK4");



   /*******************************************************************************
    ** Explicit singular names; unknown tables must declare their vocabulary.
    *******************************************************************************/
   public static String label(String table)
   {
      return (switch(table)
      {
         case "edi_partner" -> "Trading Partner";
         case "edi_identity" -> "EDI Identity";
         case "edi_agreement" -> "Agreement";
         case "edi_agreement_document" -> "Agreement Document";
         case "edi_control_number" -> "Control Number";
         case "edi_transport" -> "Transport";
         case "edi_certificate" -> "Certificate";
         case "edi_guide" -> "Implementation Guide";
         case "edi_guide_segment" -> "Guide Segment";
         case "edi_guide_element" -> "Guide Element";
         case "edi_guide_rule" -> "Guide Rule";
         case "edi_code_list" -> "Code List";
         case "edi_code_list_value" -> "Code";
         case "edi_map" -> "Map";
         case "edi_map_rule" -> "Map Rule";
         case "edi_map_sample" -> "Map Sample";
         case "edi_interchange" -> "Interchange";
         case "edi_functional_group" -> "Functional Group";
         case "edi_transaction" -> "Document";
         case "edi_transaction_error" -> "Validation Error";
         case "edi_event" -> "Event";
         case "edi_exception" -> "Exception";
         case "edi_inbox" -> "Inbound Message";
         case "edi_outbound_request" -> "Outbound Request";
         case "edi_outbound_message" -> "Outbound Message";
         case "edi_payload" -> "Payload";
         default -> throw new IllegalArgumentException("Missing EDI table label: " + table);
      });
   }



   /*******************************************************************************
    ** Child sections use the plain plural, without database relationship syntax.
    *******************************************************************************/
   public static String pluralLabel(String table)
   {
      return (switch(table)
      {
         case "edi_identity" -> "Identities";
         case "edi_guide" -> "Guides";
         case "edi_guide_segment" -> "Segments";
         case "edi_guide_element" -> "Elements";
         case "edi_guide_rule" -> "Rules";
         case "edi_map_sample" -> "Samples";
         default -> label(table) + "s";
      });
   }



   /*******************************************************************************
    ** Keep field insertion order and record sections aligned, with dates last.
    *******************************************************************************/
   public static void apply(QTableMetaData table)
   {
      table.setLabel(label(table.getName()));
      List<String> summary = leadingFields(table.getName());
      Map<String, QFieldMetaData> ordered = new LinkedHashMap<>();
      summary.forEach(name -> ordered.put(name, table.getField(name)));
      table.getFields().forEach(ordered::putIfAbsent);
      List<String> details = new ArrayList<>();
      List<String> dates = new ArrayList<>();
      for(QFieldMetaData field : ordered.values())
      {
         field.setLabel(fieldLabel(field));
         if(summary.contains(field.getName()) || Boolean.TRUE.equals(field.getIsHidden()))
         {
            continue;
         }
         (field.getType() == QFieldType.DATE_TIME || field.getType() == QFieldType.DATE ? dates : details).add(field.getName());
      }
      Map<String, QFieldMetaData> fields = new LinkedHashMap<>();
      for(List<String> section : List.of(summary, details, dates))
      {
         section.forEach(name -> fields.put(name, ordered.get(name)));
      }
      ordered.forEach(fields::putIfAbsent);
      table.setFields(fields);
      table.setSections(new ArrayList<>(List.of(
         new QFieldSection("identity", new QIcon("badge"), Tier.T1, new ArrayList<>(summary)).withLabel("Overview"),
         new QFieldSection("details", new QIcon("description"), Tier.T2, details).withLabel("Details"),
         new QFieldSection("dates", new QIcon("schedule"), Tier.T3, dates).withLabel("Dates"))));
   }



   /*******************************************************************************
    ** References name the linked record; external identifiers retain the ID suffix.
    *******************************************************************************/
   private static String fieldLabel(QFieldMetaData field)
   {
      String name = switch(field.getName())
      {
         case "transactionId" -> "document";
         case "ackTransactionId" -> "ackDocument";
         case "inboxId" -> "inboundMessage";
         case "assignedToUserId" -> "assignedTo";
         default -> field.getName();
      };
      if(name.endsWith("Id") && field.getPossibleValueSourceName() != null)
      {
         name = name.substring(0, name.length() - 2);
      }
      List<String> words = new ArrayList<>();
      for(String word : name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").split(" "))
      {
         String upper = word.toUpperCase(Locale.ROOT);
         words.add(ACRONYMS.contains(upper) ? upper : Character.toUpperCase(word.charAt(0)) + word.substring(1));
      }
      return (String.join(" ", words));
   }



   /*******************************************************************************
    ** The first columns answer what happened, to whom, and what needs attention.
    *******************************************************************************/
   private static List<String> leadingFields(String table)
   {
      return (switch(table)
      {
         case "edi_partner" -> List.of("name", "code", "status", "contactName", "contactEmail", "contactPhone", "containsPhi");
         case "edi_identity" -> List.of("interchangeId", "interchangeQualifier", "applicationCode", "partnerId", "standard", "isActive");
         case "edi_agreement" -> List.of("partnerId", "localIdentityId", "partnerIdentityId", "standard", "usageIndicator", "isActive", "inboundTransportId", "outboundTransportId");
         case "edi_agreement_document" -> List.of("transactionSet", "direction", "version", "agreementId", "isActive", "guideId", "mapId", "ackExpectation");
         case "edi_control_number" -> List.of("level", "lastValue", "agreementId", "localIdentityId", "minValue", "maxValue", "wrapAllowed");
         case "edi_transport" -> List.of("name", "type", "direction", "partnerId", "isActive");
         case "edi_certificate" -> List.of("alias", "status", "usage", "partnerId", "notAfter", "subject", "issuer");
         case "edi_guide" -> List.of("name", "revision", "status", "transactionSet", "version", "standard", "partnerId", "source");
         case "edi_guide_segment" -> List.of("segmentId", "name", "position", "sequence", "usage", "loopId", "guideId");
         case "edi_guide_element" -> List.of("name", "elementId", "position", "componentPosition", "usage", "dataType", "guideSegmentId");
         case "edi_guide_rule" -> List.of("type", "message", "severity", "positions", "guideId", "guideSegmentId");
         case "edi_code_list" -> List.of("name", "elementId", "source", "externalCodeSet");
         case "edi_code_list_value" -> List.of("code", "description", "isActive", "codeListId");
         case "edi_map" -> List.of("name", "revision", "status", "direction", "transactionSet", "version", "partnerId", "guideId", "tableName");
         case "edi_map_rule" -> List.of("sequence", "ediPath", "qqqPath", "transform", "required", "mapId");
         case "edi_map_sample" -> List.of("name", "mapId", "lastPreviewStatus", "lastPreviewAt");
         case "edi_interchange" -> List.of("direction", "partnerId", "controlNumber", "status", "interchangeDate", "transportId", "groupCount");
         case "edi_functional_group" -> List.of("controlNumber", "functionalIdCode", "version", "transactionCount", "ackStatus", "interchangeId");
         case "edi_transaction" -> List.of("transactionSet", "businessKeyType", "businessKey", "partnerId", "direction", "status", "ackStatus", "version", "interchangeId");
         case "edi_transaction_error" -> List.of("severity", "message", "segmentId", "segmentPosition", "elementPosition", "transactionId");
         case "edi_event" -> List.of("type", "at", "detail", "transactionId", "interchangeId", "inboxId");
         case "edi_exception" -> List.of("type", "status", "summary", "partnerId", "transactionId", "interchangeId", "assignedToUserId", "occurrences");
         case "edi_inbox" -> List.of("transportType", "externalId", "status", "partnerId", "transportId", "receivedAt", "attempts");
         case "edi_outbound_request" -> List.of("status", "agreementDocumentId", "sourceTableName", "sourceRecordId", "attempts", "nextAttemptAt");
         case "edi_outbound_message" -> List.of("transportId", "status", "partnerId", "agreementId", "attempts", "sentAt", "deliveredAt");
         case "edi_payload" -> List.of("kind", "sizeBytes", "sha256");
         default -> throw new IllegalArgumentException("Missing EDI field order: " + table);
      });
   }
}
