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


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.kingsrook.qbits.edi.x12.X12Segment;
import com.kingsrook.qbits.edi.x12.X12Separators;
import com.kingsrook.qbits.edi.x12.X12Writer;


/*******************************************************************************
 ** Deterministic synthetic purchase orders; no customer or patient data.
 *******************************************************************************/
public final class Synthetic850
{
   /*******************************************************************************
    ** Generate one complete interchange for either supported base version.
    *******************************************************************************/
   public static String generate(long seed, String version, X12Separators separators)
   {
      Random random = new Random(seed);
      List<X12Segment> segments = new ArrayList<>();
      segments.add(X12Segment.of("ISA", 1, "00", "", "00", "", "ZZ", "SYNTHETIC", "ZZ", "QQQTEST", "261013", "1200", version.equals("004010") ? "U" : "^", version.substring(0, 5), "000000001", "0", "T", ":"));
      segments.add(X12Segment.of("GS", 2, "PO", "SYNTHETIC", "QQQTEST", "20261013", "1200", "1", "X", version));
      segments.add(X12Segment.of("ST", 1, "850", "0001"));
      segments.add(X12Segment.of("BEG", 2, "00", "NE", "TEST-" + random.nextInt(100000), "", "20261013"));
      segments.add(X12Segment.of("N1", 3, "ST", "Synthetic Dock", "92", "TEST1"));
      segments.add(X12Segment.of("N3", 4, "100 Test Street"));
      segments.add(X12Segment.of("N4", 5, "Test City", "IL", "00000"));
      segments.add(X12Segment.of("PO1", 6, "1", "2", "EA", "10.50", "", "VN", "SYNTH-1"));
      segments.add(X12Segment.of("PID", 7, "F", "", "", "", "Synthetic item"));
      segments.add(X12Segment.of("CTT", 8, "1"));
      segments.add(X12Segment.of("SE", 9, "9", "0001"));
      segments.add(X12Segment.of("GE", 12, "1", "1"));
      segments.add(X12Segment.of("IEA", 13, "1", "000000001"));
      return (new X12Writer().write(segments, separators, ""));
   }
}
