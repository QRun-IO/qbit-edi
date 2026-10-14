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

package com.kingsrook.qbits.edi.edifact;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** UNA service characters; the fifth character is reserved in syntax version 3.
 *******************************************************************************/
public record EdifactSeparators(char component, char element, char decimal, char release, char repetition, char segment)
{
   public static final EdifactSeparators DEFAULT = new EdifactSeparators(':', '+', '.', '?', ' ', '\'');

   /*******************************************************************************
    ** Ambiguous service characters cannot be safely parsed or escaped.
    *******************************************************************************/
   public EdifactSeparators
   {
      var values = List.of(component, element, decimal, release, repetition, segment);
      if(!List.of('.', ',').contains(decimal) || new HashSet<>(values).size() != 6 || values.stream().anyMatch(c -> c > 127 || Character.isISOControl(c)))
      {
         throw new IllegalArgumentException("Invalid EDIFACT service characters");
      }
   }

   /*******************************************************************************
    ** Read exactly six UNA service characters.
    *******************************************************************************/
   public static EdifactSeparators fromUna(String una)
   {
      if(una.length() != 9 || !una.startsWith("UNA"))
      {
         throw new IllegalArgumentException("Invalid UNA");
      }
      return (new EdifactSeparators(una.charAt(3), una.charAt(4), una.charAt(5), una.charAt(6), una.charAt(7), una.charAt(8)));
   }

   /*******************************************************************************
    ** Use the agreement's EDIFACT service characters.
    *******************************************************************************/
   public static EdifactSeparators fromAgreement(QRecord agreement)
   {
      return (new EdifactSeparators(character(agreement, "componentSeparator", ":"), character(agreement, "elementSeparator", "+"),
         character(agreement, "decimalMark", "."), character(agreement, "releaseCharacter", "?"),
         "4".equals(agreement.getValueString("interchangeVersion")) ? character(agreement, "repetitionSeparator", "*") : ' ', character(agreement, "segmentTerminator", "'")));
   }

   /*******************************************************************************
    ** A separator setting must contain exactly one character.
    *******************************************************************************/
   private static char character(QRecord agreement, String field, String fallback)
   {
      String value = Objects.requireNonNullElse(agreement.getValueString(field), fallback);
      if(value.length() != 1)
      {
         throw new IllegalArgumentException("Invalid service character setting");
      }
      return (value.charAt(0));
   }

   /*******************************************************************************
    ** Render service string advice, including its segment terminator.
    *******************************************************************************/
   public String una()
   {
      return ("UNA" + component + element + decimal + release + repetition + segment);
   }
}
