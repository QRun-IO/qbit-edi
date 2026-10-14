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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qbits.edi.x12.X12EnvelopeReader;
import com.kingsrook.qbits.edi.x12.X12Interchange;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12TransactionSet;
import com.kingsrook.qbits.edi.x12.X12Writer;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** Seeded synthetic healthcare fixtures and host tables; never used in main code.
 ** NPI 1234567893 is a documented checksum test value, not a provider identity.
 *******************************************************************************/
public final class HealthcareSupport
{
   /*******************************************************************************
    ** Test-only schema with classified target fields and actual QQQ associations.
    *******************************************************************************/
   public static void tables() throws Exception
   {
      table("sample_claim", "claimNumber", "memberId", "patientName", "diagnosis", "totalCharge:DECIMAL");
      table("sample_claim_line", "parentId:INTEGER", "lineNumber:INTEGER", "procedureCode", "charge:DECIMAL");
      table("sample_remittance", "trace", "payment:DECIMAL");
      table("sample_remittance_claim", "parentId:INTEGER", "claimNumber", "patientName", "memberId", "charge:DECIMAL", "paid:DECIMAL");
      table("sample_eligibility_request", "trace", "memberId", "patientName");
      table("sample_eligibility_response", "trace", "memberId", "patientName", "benefit", "serviceType");
      table("sample_claim_status", "trace", "memberId", "patientName", "claimNumber", "category", "statusCode");
      table("sample_enrollment", "reference");
      table("sample_enrollment_member", "parentId:INTEGER", "memberId", "patientName", "birthDate:DATE", "coverage");
      association("sample_claim", "sample_claim_line", "lines");
      association("sample_remittance", "sample_remittance_claim", "claims");
      association("sample_enrollment", "sample_enrollment_member", "members");
      var instance = QContext.getQInstance();
      EdiRuntime.current().config().withServiceSessionSupplier(() -> new QSystemUserSession(instance));
   }

   /*******************************************************************************
    ** Define one table and its H2 schema; fields are STRING unless suffixed with a type.
    *******************************************************************************/
   private static void table(String name, String... fields) throws Exception
   {
      var instance = QContext.getQInstance();
      String backend = EdiRuntime.current().config().getBackendName();
      var table = new QTableMetaData().withName(name).withLabel(name).withBackendName(backend).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id"));
      for(String field : fields)
      {
         String[] parts = field.split(":");
         table.withField(new QFieldMetaData(parts[0], parts.length > 1 ? QFieldType.valueOf(parts[1]) : QFieldType.STRING).withBackendName(QInstanceEnricher.inferBackendName(parts[0]))
            .withSupplementalMetaData(new AiFieldMetaData().withClassification(parts[0].equals("parentId") ? DataClassification.CONFIDENTIAL : DataClassification.PHI)));
      }
      instance.addTable(table);
      try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) instance.getBackend(backend)); var statement = connection.createStatement())
      {
         statement.execute(H2Support.ddl(table, false));
      }
   }

   /*******************************************************************************
    ** Join-backed child records, as the retail samples do.
    *******************************************************************************/
   private static void association(String parent, String child, String name)
   {
      var instance = QContext.getQInstance();
      String join = parent + "_" + name;
      instance.getTable(parent).withAssociation(new Association().withName(name).withAssociatedTableName(child).withJoinName(join));
      instance.addJoin(new QJoinMetaData().withName(join).withType(JoinType.ONE_TO_MANY).withLeftTable(parent).withRightTable(child).withJoinOn(new JoinOn("id", "parentId")));
   }

   /*******************************************************************************
    ** Version and group identifiers for the eight original baselines.
    *******************************************************************************/
   public static String version(String key)
   {
      return (switch(key)
      {
         case "837P" -> "005010X222A1";
         case "837I" -> "005010X223A2";
         case "835" -> "005010X221A1";
         case "270", "271" -> "005010X279A1";
         case "276", "277" -> "005010X212";
         default -> "005010X220A1";
      });
   }

   /*******************************************************************************
    ** Functional identifiers are routing metadata, never patient information.
    *******************************************************************************/
   public static String group(String key)
   {
      return (switch(key)
      {
         case "837P", "837I" -> "HC";
         case "835" -> "HP";
         case "270" -> "HS";
         case "271" -> "HB";
         case "276" -> "HR";
         case "277" -> "HN";
         default -> "BE";
      });
   }

   /*******************************************************************************
    ** Reproducible member and claim references; no production names or identifiers.
    *******************************************************************************/
   public static String body(String key, long seed)
   {
      var random = new java.util.Random(seed);
      String member = String.format("SYN%06d", random.nextInt(1000000));
      String claim = String.format("SYNCLM%06d", random.nextInt(1000000));
      if(key.startsWith("837"))
      {
         boolean inpatient = key.equals("837I");
         return ("BHT*0019*00*SYNBATCH*20261015*1200*CH~"
            + "NM1*41*2*Synthetic Submitter*****46*SYN~NM1*40*2*Synthetic Receiver*****46*SYN~"
            + "HL*1**20*1~NM1*85*2*Synthetic Practice*****XX*1234567893~N3*1 Synthetic Way~N4*Testville*IL*60000~"
            + "HL*2*1*22*0~SBR*P*18*******CI~NM1*IL*1*Testpatient Alpha*****MI*" + member + "~DMG*D8*19800101*U~NM1*PR*2*Synthetic Plan*****PI*SYNPLAN~"
            + "CLM*" + claim + "*150***" + (inpatient ? "11:A:1" : "11:B:1") + "~"
            + (inpatient ? "DTP*434*RD8*20261014-20261015~" : "") + "HI*ABK:A123~"
            + "LX*1~" + (inpatient ? "SV2*0300*HC:S0001*100*UN*1~" : "SV1*HC:S0001*100*UN*1~") + "DTP*472*D8*20261015~"
            + "LX*2~" + (inpatient ? "SV2*0300*HC:S0002*50*UN*1~" : "SV1*HC:S0002*50*UN*1~") + "DTP*472*D8*20261015~");
      }
      if(key.equals("835"))
      {
         return ("BPR*I*115*C*CHK************20261015~TRN*1*SYNTRACE" + seed + "~N1*PR*Synthetic Plan~N1*PE*Synthetic Practice~"
            + "LX*1~CLP*" + claim + "*1*150*120*30~CAS*PR*1*10~NM1*QC*1*Testpatient Alpha~NM1*IL*1*Testpatient Alpha*****MI*" + member + "~"
            + "SVC*HC:S0001*100*80~DTM*472*20261015~CAS*CO*45*20~SVC*HC:S0002*50*40~DTM*472*20261015~"
            + "PLB*1234567893*20261231*WO:SYNADJ*5~");
      }
      if(key.equals("834"))
      {
         return ("BGN*00*SYNENROLL" + seed + "*20261015*1200~N1*P5*Synthetic Sponsor~N1*IN*Synthetic Plan~"
            + "INS*Y*18*021~REF*0F*" + member + "~NM1*IL*1*Testpatient Alpha~DMG*D8*19800101*U~HD*021**HLT~DTP*348*D8*20261015~");
      }
      String head = "BHT*0022*13*SYNBATCH*20261015*1200~HL*1**20*1~NM1*PR*2*Synthetic Plan*****PI*SYNPLAN~"
         + "HL*2*1*21*1~NM1*1P*2*Synthetic Practice*****XX*1234567893~HL*3*2*22*0~";
      return (head + (key.equals("270") || key.equals("271") ? "TRN*1*SYNTRACE" + seed + "~" : "")
         + "NM1*IL*1*Testpatient Alpha*****MI*" + member + "~"
         + (key.equals("270") ? "EQ*30~" : key.equals("271") ? "EB*1**30~" : "TRN*1*SYNTRACE" + seed + "~"
         + (key.equals("277") ? "STC*A1:19*20261015~" : "") + "REF*EJ*" + claim + "~"));
   }

   /*******************************************************************************
    ** Wrap raw synthetic segments using real ISA widths and counted ST/SE boundaries.
    *******************************************************************************/
   public static String interchange(String key, int control, String body)
   {
      String version = version(key);
      String set = key.startsWith("837") ? "837" : key;
      String isa = new X12Writer().write(List.of(X12Segment.of("ISA", 0, "00", "", "00", "", "ZZ", "CEDAR", "ZZ", "QQQTEST", "261015", "1200", "^", "00501", String.format("%09d", control), "0", "T", ":")), X12Separators.DEFAULT, "");
      return (isa + "GS*" + group(key) + "*CEDAR*QQQTEST*20261015*1200*" + control + "*X*" + version + "~ST*" + set + "*0001*" + version + "~"
         + body + "SE*" + (body.split("~").length + 2) + "*0001~GE*1*" + control + "~IEA*1*" + String.format("%09d", control) + "~");
   }

   /*******************************************************************************
    ** Seed the healthcare partner and sample agreement documents as normal records.
    *******************************************************************************/
   public static QRecord configure() throws Exception
   {
      return (SystemWrites.call(() ->
      {
         QRecord partner = SystemWrites.insert("edi_partner", new QRecord().withValue("name", "Cedar Health Clearinghouse").withValue("code", "CEDAR")
            .withValue("status", "ACTIVE").withValue("containsPhi", true).withValue("contactEmail", "cedar@example.invalid"));
         var locals = EdiRecords.query("edi_identity", EdiRecords.equal("interchangeId", "QQQTEST"));
         QRecord local = locals.isEmpty() ? SystemWrites.insert("edi_identity", new QRecord().withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "QQQTEST").withValue("applicationCode", "QQQTEST")) : locals.getFirst();
         QRecord remote = SystemWrites.insert("edi_identity", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("standard", "X12").withValue("interchangeQualifier", "ZZ").withValue("interchangeId", "CEDAR").withValue("applicationCode", "CEDAR"));
         QRecord transport = SystemWrites.insert("edi_transport", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("name", "cedar-gateway").withValue("type", "GATEWAY")
            .withValue("direction", "BOTH").withValue("gatewayEndpointName", "cedar-outbox").withValue("isActive", true));
         QRecord agreement = SystemWrites.insert("edi_agreement", new QRecord().withValue("partnerId", partner.getValue("id")).withValue("localIdentityId", local.getValue("id")).withValue("partnerIdentityId", remote.getValue("id"))
            .withValue("standard", "X12").withValue("usageIndicator", "T").withValue("applyTestDocuments", true).withValue("isActive", true).withValue("interchangeVersion", "00501")
            .withValue("inboundTransportId", transport.getValue("id")).withValue("outboundTransportId", transport.getValue("id")));
         for(String key : List.of("837P", "837I", "835", "270", "271", "276", "277", "834"))
         {
            QRecord map = LogisticsSupport.installMap(key, version(key));
            SystemWrites.insert("edi_agreement_document", new QRecord().withValue("agreementId", agreement.getValue("id")).withValue("direction", map.getValue("direction"))
               .withValue("transactionSet", map.getValue("transactionSet")).withValue("version", version(key)).withValue("functionalIdCode", group(key))
               .withValue("guideId", map.getValue("guideId")).withValue("mapId", map.getValue("id")).withValue("ackExpectation", "999").withValue("ackLevel", "ALL_TRANSACTIONS")
               .withValue("sourceTableName", map.getValueString("direction").equals("OUTBOUND") ? map.getValue("tableName") : null).withValue("isActive", true));
         }
         return (agreement);
      }));
   }

   /*******************************************************************************
    ** Normal durable intake and parsing, shared with the demo.
    *******************************************************************************/
   public static void receive(String key, int control, String body) throws Exception
   {
      new com.kingsrook.qbits.edi.flow.InboundSink(EdiRuntime.current().payloads()).receive(new com.kingsrook.qbits.edi.flow.InboundPayload("GATEWAY", "health-" + control,
         "synthetic-healthcare.edi", "127.0.0.1", new ByteArrayInputStream(interchange(key, control, body).getBytes(StandardCharsets.US_ASCII))));
      new com.kingsrook.qbits.edi.flow.InboxWorker().runOnce();
   }
}
