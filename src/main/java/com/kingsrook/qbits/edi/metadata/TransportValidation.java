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


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;


/*******************************************************************************
 ** Validate transport routing fields, including partial record edits.
 *******************************************************************************/
public class TransportValidation implements TableCustomizerInterface
{
   /*******************************************************************************
    ** Validate merged records without exposing transport settings.
    *******************************************************************************/
   @Override
   public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords)
   {
      for(QRecord record : records)
      {
         QRecord merged = new QRecord();
         oldRecords.orElse(List.of()).stream().filter(old -> Objects.equals(old.getValue("id"), record.getValue("id")))
            .findFirst().ifPresent(old -> merged.getValues().putAll(old.getValues()));
         merged.getValues().putAll(record.getValues());
         String name = merged.getValueString("name");
         if(name == null || !name.matches("[a-z0-9-]+"))
         {
            record.addError(new BadInputStatusMessage("name must be a lowercase slug"));
         }
         String type = merged.getValueString("type");
         if(type == null || type.isBlank())
         {
            record.addError(new BadInputStatusMessage("type is required"));
         }
      }
      for(QRecord record : records)
      {
         QRecord merged = new QRecord();
         oldRecords.orElse(List.of()).stream().filter(old -> Objects.equals(old.getValue("id"), record.getValue("id")))
            .findFirst().ifPresent(old -> merged.getValues().putAll(old.getValues()));
         merged.getValues().putAll(record.getValues());
         try
         {
            List<String> errors = new java.util.ArrayList<>();
            com.kingsrook.qbits.edi.transport.EdiTransportPluginsMetaData.current().get(merged.getValueString("type"))
               .validate(merged.toEntity(com.kingsrook.qbits.edi.model.EdiTransport.class), errors);
            if(merged.getValueInteger("maxAttempts") != null && merged.getValueInteger("maxAttempts") < 1)
            {
               errors.add("maxAttempts must be positive");
            }
            errors.forEach(error -> record.addError(new BadInputStatusMessage(error)));
            record.setValue("weakCrypto", "AS2".equals(merged.getValueString("type")) && com.kingsrook.qbits.edi.as2.As2Crypto.isWeak(merged.getValueString("signAlgorithm"), merged.getValueString("encryptAlgorithm")));
         }
         catch(Exception e)
         {
            record.addError(new BadInputStatusMessage("Invalid transport configuration"));
         }
      }
      return (records);
   }
}
