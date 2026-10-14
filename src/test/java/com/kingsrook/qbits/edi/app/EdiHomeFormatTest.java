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

package com.kingsrook.qbits.edi.app;


import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Formatting used by the home tables.
 *******************************************************************************/
class EdiHomeFormatTest
{

   /*******************************************************************************
    ** Durations, enum labels and document names read plainly.
    *******************************************************************************/
   @Test
   void formatsReadPlainly()
   {
      assertThat(EdiHome.humanize(Duration.ofSeconds(-5))).isEqualTo("0 s");
      assertThat(EdiHome.humanize(Duration.ofSeconds(45))).isEqualTo("45 s");
      assertThat(EdiHome.humanize(Duration.ofMinutes(12))).isEqualTo("12 m");
      assertThat(EdiHome.humanize(Duration.ofMinutes(185))).isEqualTo("3 h 5 m");
      assertThat(EdiHome.humanize(Duration.ofHours(3))).isEqualTo("3 h");
      assertThat(EdiHome.humanize(Duration.ofHours(52))).isEqualTo("2 d 4 h");
      assertThat(EdiHome.humanize(Duration.ofDays(2))).isEqualTo("2 d");
      assertThat(EdiHome.plain("UNKNOWN_PARTNER")).isEqualTo("Unknown partner");
      assertThat(EdiHome.plain(null)).isEmpty();
      assertThat(EdiHome.documentName("850")).isEqualTo("850 Purchase Order");
      assertThat(EdiHome.documentName("123")).isEqualTo("123");
   }
}
