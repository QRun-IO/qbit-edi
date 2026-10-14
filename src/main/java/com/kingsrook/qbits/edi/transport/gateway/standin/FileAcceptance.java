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

package com.kingsrook.qbits.edi.transport.gateway.standin;

import java.time.Duration;

/*******************************************************************************
 ** STAND-IN(core=QRun-IO/qbit-file-gateway, removeBy=qbit-file-gateway 0.1.0-RC.1)
 *******************************************************************************/
public final class FileAcceptance
{
   private final String status;
   private final String consumerReference;
   private final String reason;
   private final Duration retryAfter;

   /*******************************************************************************
    ** Immutable acceptance result.
    *******************************************************************************/
   private FileAcceptance(String status, String consumerReference, String reason, Duration retryAfter)
   {
      this.status = status;
      this.consumerReference = consumerReference;
      this.reason = reason;
      this.retryAfter = retryAfter;
   }

   /*******************************************************************************
    ** Get status.
    *******************************************************************************/
   public String getStatus()
   {
      return (status);
   }

   /*******************************************************************************
    ** Get consumerReference.
    *******************************************************************************/
   public String getConsumerReference()
   {
      return (consumerReference);
   }

   /*******************************************************************************
    ** Get reason.
    *******************************************************************************/
   public String getReason()
   {
      return (reason);
   }

   /*******************************************************************************
    ** Get retryAfter.
    *******************************************************************************/
   public Duration getRetryAfter()
   {
      return (retryAfter);
   }

   /*******************************************************************************
    ** Intake committed.
    *******************************************************************************/
   public static FileAcceptance accepted(String consumerReference)
   {
      return (new FileAcceptance("ACCEPTED", consumerReference, null, null));
   }

   /*******************************************************************************
    ** Permanently reject a file.
    *******************************************************************************/
   public static FileAcceptance rejected(String reason)
   {
      return (new FileAcceptance("REJECTED", null, reason, null));
   }

   /*******************************************************************************
    ** Storage is temporarily unavailable.
    *******************************************************************************/
   public static FileAcceptance retryLater(String reason, Duration retryAfter)
   {
      return (new FileAcceptance("RETRY_LATER", null, reason, retryAfter));
   }
}
