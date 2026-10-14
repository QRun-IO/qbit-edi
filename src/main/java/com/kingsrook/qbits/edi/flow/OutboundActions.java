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

package com.kingsrook.qbits.edi.flow;

import java.util.List;
import com.kingsrook.qbits.edi.EdiRuntime;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.storage.PayloadRef;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Guarded resend/cancel operations used after the standard Review step.
 *******************************************************************************/
public final class OutboundActions
{
   /*******************************************************************************
    ** Call within SystemWrites.transaction so validation and mutation share a lock.
    *******************************************************************************/
   public static void execute(String table, QRecord selected, String operation) throws Exception
   {
      QRecord message = table.equals("edi_interchange") ? OutboundWorker.row("edi_outbound_message", selected.getValue("outboundMessageId")) : selected;
      if(!SystemWrites.guard("UPDATE edi_outbound_message SET id=id WHERE id=? AND status=?", message.getValue("id"), message.getValue("status")))
      {
         throw new IllegalStateException("Outbound message changed; refresh the record");
      }
      message = OutboundWorker.row("edi_outbound_message", message.getValue("id"));
      if(operation.equals("cancel"))
      {
         if(!List.of("QUEUED", "RETRY_WAIT").contains(message.getValueString("status")))
         {
            throw new IllegalStateException("Only queued or retrying messages can be cancelled");
         }
         SystemWrites.update("edi_outbound_message", new QRecord().withValue("id", message.getValue("id")).withValue("status", "CANCELLED").withValue("nextAttemptAt", null));
      }
      else if(operation.equals("resend"))
      {
         SystemWrites.update("edi_outbound_message", new QRecord().withValue("id", message.getValue("id")).withValue("status", "QUEUED")
            .withValue("attempts", 0).withValue("nextAttemptAt", null).withValue("lastError", null));
      }
      else if(operation.equals("new"))
      {
         reEnvelope(message);
      }
      else
      {
         throw new IllegalArgumentException("Unknown outbound operation");
      }
      EdiJournal.event(null, message.getValueInteger("interchangeId"), null, operation.toUpperCase(java.util.Locale.ROOT));
   }

   /*******************************************************************************
    ** Re-envelope the archived payload and tracking snapshots, including TA1/997/999.
    ** Maps are not re-run: source edits require a new outbound request instead.
    *******************************************************************************/
   private static void reEnvelope(QRecord message) throws Exception
   {
      if("EDIFACT".equals(OutboundWorker.row("edi_agreement", message.getValue("agreementId")).getValueString("standard")))
      {
         com.kingsrook.qbits.edi.edifact.EdifactOutbound.resendAsNew(message);
         return;
      }
      var runtime = EdiRuntime.current();
      QRecord originalRow = OutboundWorker.row("edi_interchange", message.getValue("interchangeId"));
      QRecord agreement = OutboundWorker.row("edi_agreement", message.getValue("agreementId"));
      boolean local = "LOCAL_IDENTITY".equals(agreement.getValueString("controlNumberScope"));
      String control = String.format("%09d", runtime.controls().allocate(local ? null : agreement.getValueInteger("id"), local ? agreement.getValueInteger("localIdentityId") : null, "ISA", 1, 999999999, true));
      com.kingsrook.qbits.edi.x12.X12Interchange original;
      try(var stream = runtime.payloads().open(new PayloadRef(message.getValueString("payloadRef"), message.getValueString("payloadSha256"), message.getValueLong("sizeBytes"))))
      {
         original = runtime.reader().read(stream).getFirst();
      }
      var groups = new java.util.ArrayList<com.kingsrook.qbits.edi.x12.X12FunctionalGroup>();
      for(var group : original.groups())
      {
         String groupControl = String.valueOf(runtime.controls().allocate(local ? null : agreement.getValueInteger("id"), local ? agreement.getValueInteger("localIdentityId") : null, "GS", 1, 999999999, true));
         groups.add(new com.kingsrook.qbits.edi.x12.X12FunctionalGroup(replace(group.gs(), 6, groupControl), replace(group.ge(), 2, groupControl), group.transactions(), List.of()));
      }
      var rebuilt = new com.kingsrook.qbits.edi.x12.X12Interchange(original.separators(), replace(original.isa(), 13, control), replace(original.iea(), 2, control), groups, original.serviceSegments(), List.of());
      var payload = runtime.payloads().put(com.kingsrook.qbits.edi.storage.PayloadKind.OUTBOUND,
         new java.io.ByteArrayInputStream(runtime.writer().write(rebuilt).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      QRecord newInterchange = copy(originalRow).withValue("originalInterchangeId", originalRow.getValue("id")).withValue("controlNumber", control)
         .withValue("status", "ENVELOPED").withValue("ta1Status", null).withValue("outboundMessageId", null).withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256()).withValue("sizeBytes", payload.sizeBytes());
      newInterchange = SystemWrites.insert("edi_interchange", newInterchange);
      var groupIds = new java.util.HashMap<Integer, Integer>();
      for(QRecord oldGroup : EdiRecords.query("edi_functional_group", EdiRecords.equal("interchangeId", originalRow.getValue("id"))))
      {
         int index = -1;
         for(int i = 0; i < original.groups().size(); i++)
         {
            if(original.groups().get(i).gs().value(6).equals(oldGroup.getValueString("controlNumber")))
            {
               index = i;
               break;
            }
         }
         if(index < 0)
         {
            throw new IllegalStateException("Stored group does not match archived envelope");
         }
         QRecord newGroup = SystemWrites.insert("edi_functional_group", copy(oldGroup).withValue("interchangeId", newInterchange.getValue("id"))
            .withValue("controlNumber", groups.get(index).gs().value(6)).withValue("ackDueAt", null).withValue("ackReceivedAt", null).withValue("ackTransactionId", null)
            .withValue("ackStatus", "NONE".equals(oldGroup.getValueString("ackStatus")) ? "NONE" : "PENDING"));
         groupIds.put(oldGroup.getValueInteger("id"), newGroup.getValueInteger("id"));
      }
      for(QRecord transaction : EdiRecords.query("edi_transaction", EdiRecords.equal("interchangeId", originalRow.getValue("id"))))
      {
         SystemWrites.insert("edi_transaction", copy(transaction).withValue("interchangeId", newInterchange.getValue("id"))
            .withValue("functionalGroupId", groupIds.get(transaction.getValueInteger("functionalGroupId"))).withValue("status", "QUEUED").withValue("ackStatus", null));
      }
      QRecord newMessage = SystemWrites.insert("edi_outbound_message", copy(message).withValue("interchangeId", newInterchange.getValue("id"))
         .withValue("payloadRef", payload.value()).withValue("payloadSha256", payload.sha256()).withValue("sizeBytes", payload.sizeBytes())
         .withValue("status", "QUEUED").withValue("attempts", 0).withValue("nextAttemptAt", null).withValue("sentAt", null).withValue("deliveredAt", null).withValue("claimedAt", null)
         .withValue("transportMessageId", null).withValue("lastError", null).withValue("ackForGroupId", null).withValue("ackForInterchangeId", null));
      SystemWrites.update("edi_interchange", new QRecord().withValue("id", newInterchange.getValue("id")).withValue("outboundMessageId", newMessage.getValue("id")));
   }

   /*******************************************************************************
    ** Preserve components and repetitions while replacing one envelope element.
    *******************************************************************************/
   private static com.kingsrook.qbits.edi.x12.X12Segment replace(com.kingsrook.qbits.edi.x12.X12Segment segment, int position, String value)
   {
      var elements = new java.util.ArrayList<>(segment.elements());
      elements.set(position - 1, List.of(List.of(value)));
      return (new com.kingsrook.qbits.edi.x12.X12Segment(segment.id(), segment.position(), elements));
   }

   /*******************************************************************************
    ** Clone business metadata without identity and audit timestamps.
    *******************************************************************************/
   private static QRecord copy(QRecord source)
   {
      QRecord copy = new QRecord();
      copy.getValues().putAll(source.getValues());
      for(String field : List.of("id", "createDate", "modifyDate"))
      {
         copy.getValues().remove(field);
      }
      return (copy);
   }
}
