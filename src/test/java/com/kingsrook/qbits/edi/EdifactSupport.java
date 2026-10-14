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
import com.kingsrook.qbits.edi.edifact.EdifactEnvelopeReader;
import com.kingsrook.qbits.edi.edifact.EdifactInterchange;
import com.kingsrook.qbits.edi.flow.InboundPayload;
import com.kingsrook.qbits.edi.flow.InboundSink;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Synthetic Lindqvist retail partner shared by tests and the standalone demo.
 *******************************************************************************/
public class EdifactSupport
{
   /*******************************************************************************
    ** Configure both CONTRL directions and the four retail documents.
    *******************************************************************************/
   public static QRecord install(String version) throws Exception
   {
      QRecord partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", "Lindqvist Retail AB").withValue("code", "LINDQVIST")
         .withValue("status", "ACTIVE").withValue("contactEmail", "lindqvist@example.invalid").withValue("defaultTimezone", "Europe/Stockholm"));
      QRecord local = SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "EDIFACT").withValue("interchangeQualifier", "14").withValue("interchangeId", "QQQTEST"));
      QRecord remote = SystemWrites.insert("edi_identity", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("standard", "EDIFACT")
         .withValue("interchangeQualifier", "14").withValue("interchangeId", "LINDQVIST"));
      QRecord transport = SystemWrites.insert("edi_transport", new QRecord().withValue("name", "lindqvist-outbox").withValue("type", "GATEWAY")
         .withValue("direction", "BOTH").withValue("gatewayEndpointName", "lindqvist-outbox").withValue("isActive", true));
      QRecord agreement = SystemWrites.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", local.getValue("id"))
         .withValue("partnerIdentityId", remote.getValue("id")).withValue("standard", "EDIFACT").withValue("usageIndicator", "T").withValue("interchangeVersion", "3")
         .withValue("elementSeparator", "+").withValue("componentSeparator", ":").withValue("segmentTerminator", "'").withValue("releaseCharacter", "?")
         .withValue("applyTestDocuments", true).withValue("outboundTransportId", transport.getValue("id")));
      for(String doc : List.of("ORDERS", "ORDRSP", "DESADV", "INVOIC", "CONTRL"))
      {
         QRecord map = doc.equals("CONTRL") ? null : RetailSupport.installMap(doc, version);
         QRecord guide = EdiRecords.query("edi_guide", EdiRecords.equal("transactionSet", doc).withCriteria("version", QCriteriaOperator.EQUALS, version)).getFirst();
         for(String direction : doc.equals("CONTRL") ? List.of("INBOUND", "OUTBOUND") : List.of(doc.equals("ORDERS") ? "INBOUND" : "OUTBOUND"))
         {
            SystemWrites.insert("edi_agreement_document", new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", direction)
               .withValue("transactionSet", doc).withValue("version", version).withValue("guideId", guide.getValue("id")).withValue("mapId", map == null ? null : map.getValue("id"))
               .withValue("sourceTableName", map == null ? null : map.getValue("tableName")).withValue("targetTableName", map == null ? null : map.getValue("tableName"))
               .withValue("ackExpectation", doc.equals("CONTRL") ? "NONE" : "CONTRL").withValue("ackLevel", "ALL_TRANSACTIONS").withValue("ackDueMinutes", 1)
               .withValue("outboundTrigger", "MANUAL").withValue("batchMode", "IMMEDIATE").withValue("isActive", true));
         }
      }
      return (agreement);
   }

   /*******************************************************************************
    ** Two synthetic lines, released delimiters and an EANCOM association identifier.
    *******************************************************************************/
   public static String orders(String reference, String version)
   {
      return ("UNA:+.? 'UNB+UNOC:3+LINDQVIST:14+QQQTEST:14+261014:1200+" + reference + "++++++1'"
         + "UNH+M1+ORDERS:D:" + version.substring(1) + ":UN:EAN008'BGM+220+TEST-" + reference + "+9'DTM+137:20261014:102'RFF+ON:PO-1'NAD+BY+BUY?+ER::9'"
         + "LIN+1++SKU?:ONE:SA'QTY+21:2:PCE'PRI+AAA:7.5'LIN+2++SKU?':SA'QTY+21:3:PCE'PRI+AAA:2'UNS+S'CNT+2:2'UNT+14+M1'UNZ+1+" + reference + "'");
   }

   /*******************************************************************************
    ** Parse one synthetic interchange in memory.
    *******************************************************************************/
   public static EdifactInterchange read(String text) throws Exception
   {
      return (new EdifactEnvelopeReader(100000).read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))).getFirst());
   }

   /*******************************************************************************
    ** Exercise the durable receive boundary without a real network transport.
    *******************************************************************************/
   public static void receive(String text, String external) throws Exception
   {
      new InboundSink(EdiRuntime.current().payloads()).receive(new InboundPayload("GATEWAY", external, "synthetic.edi", null, new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
   }
}
