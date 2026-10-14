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

package com.kingsrook.qbits.edi.x12;




/*******************************************************************************
 ** Separators declared by the fixed-width interchange header; repetition is absent before 00501.
 *******************************************************************************/
public record X12Separators(char element, char segment, char component, Character repetition)
{
   public static final X12Separators DEFAULT = new X12Separators('*', '~', ':', '^');

   /*******************************************************************************
    ** Read and validate all fixed ISA element boundaries.
    *******************************************************************************/
   public static X12Separators fromIsa(String isa)
   {
      if(isa.length() != 106 || !isa.startsWith("ISA"))
      {
         throw new IllegalArgumentException("ISA must contain 106 characters");
      }
      char element = isa.charAt(3);
      for(int position : new int[] {6, 17, 20, 31, 34, 50, 53, 69, 76, 81, 83, 89, 99, 101, 103})
      {
         if(isa.charAt(position) != element)
         {
            throw new IllegalArgumentException("Invalid ISA width");
         }
      }
      Character repetition = isa.substring(84, 89).compareTo("00501") >= 0 ? isa.charAt(82) : null;
      char component = isa.charAt(104);
      char segment = isa.charAt(105);
      if(element == component || element == segment || component == segment
         || repetition != null && (repetition == element || repetition == component || repetition == segment))
      {
         throw new IllegalArgumentException("ISA separators must be distinct");
      }
      return (new X12Separators(element, segment, component, repetition));
   }
}
