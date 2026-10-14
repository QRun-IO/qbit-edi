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

package com.kingsrook.qbits.edi.as2;


/*******************************************************************************
 ** A plain, value-free AS2 failure. The message never carries key material,
 ** secrets or payload content; the optional code is the RFC 4130 disposition
 ** modifier a receiver reports to the sender (for example authentication-failed).
 *******************************************************************************/
public class As2Exception extends Exception
{
   public static final String DECRYPTION_FAILED       = "decryption-failed";
   public static final String AUTHENTICATION_FAILED   = "authentication-failed";
   public static final String INTEGRITY_CHECK_FAILED  = "integrity-check-failed";
   public static final String INSUFFICIENT_SECURITY   = "insufficient-message-security";
   public static final String UNEXPECTED_PROCESSING   = "unexpected-processing-error";

   private final String code;



   /*******************************************************************************
    ** Failure without a disposition code (configuration and local problems).
    *******************************************************************************/
   public As2Exception(String message)
   {
      this(message, null);
   }



   /*******************************************************************************
    ** Failure carrying the RFC 4130 disposition modifier.
    *******************************************************************************/
   public As2Exception(String message, String code)
   {
      super(message);
      this.code = code;
   }



   /*******************************************************************************
    ** The RFC 4130 disposition modifier, or null when the failure is local.
    *******************************************************************************/
   public String getCode()
   {
      return (code);
   }
}
