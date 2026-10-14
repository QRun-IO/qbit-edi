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


/*******************************************************************************
 ** Default healthcare classification and validation depth when configuring a document.
 *******************************************************************************/
public class AgreementDocumentDefaults implements TableCustomizerInterface
{
   /*******************************************************************************
    ** Preserve explicit values and existing edits; default new records by document family.
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
         boolean healthcare = List.of("837", "835", "270", "271", "276", "277", "834").contains(Objects.toString(merged.getValue("transactionSet"), ""));
         if(merged.getValue("containsPhi") == null)
         {
            record.setValue("containsPhi", healthcare);
         }
         if(merged.getValue("snipLevel") == null)
         {
            record.setValue("snipLevel", healthcare ? 4 : 2);
         }
      }
      return (records);
   }
}
