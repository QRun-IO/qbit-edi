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

package com.kingsrook.qbits.edi.security;


import java.util.Map;
import java.util.WeakHashMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;


/*******************************************************************************
 ** STAND-IN(core=QRun-IO/qqq#1147, removeBy=4.1.0-RC.7).
 ** RC.6 logs query criteria verbatim. Suppress synchronous diagnostic logging only
 ** while PHI mapping/application runs; value-free EDI events remain in the database.
 ** This does not replace host masking or protection of host code outside this scope.
 *******************************************************************************/
public final class PhiLogScope implements AutoCloseable
{
   private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
   private static final Map<Configuration, Boolean> INSTALLED = new WeakHashMap<>();
   private final int previous;

   /*******************************************************************************
    ** Protect nested PHI work without changing logger levels or unrelated threads.
    *******************************************************************************/
   private PhiLogScope(boolean enabled)
   {
      previous = DEPTH.get();
      if(enabled)
      {
         install();
         DEPTH.set(previous + 1);
      }
   }

   /*******************************************************************************
    ** Open a scope around code that may pass clinical values to RC.6 core actions.
    *******************************************************************************/
   public static PhiLogScope open(boolean enabled)
   {
      return (new PhiLogScope(enabled));
   }

   /*******************************************************************************
    ** Install once per logger configuration, including after a host reconfiguration.
    *******************************************************************************/
   private static synchronized void install()
   {
      if(!(LogManager.getContext(false) instanceof LoggerContext context))
      {
         throw new IllegalStateException("PHI processing requires the RC.6 Log4j protection bridge");
      }
      Configuration configuration = context.getConfiguration();
      if(!INSTALLED.containsKey(configuration))
      {
         ScopeFilter filter = new ScopeFilter();
         filter.start();
         configuration.addFilter(filter);
         INSTALLED.put(configuration, true);
      }
   }

   /*******************************************************************************
    ** Always restore nesting, including failed mapping or target transactions.
    *******************************************************************************/
   @Override
   public void close()
   {
      if(previous == 0)
      {
         DEPTH.remove();
      }
      else
      {
         DEPTH.set(previous);
      }
   }

   /*******************************************************************************
    ** Pre-event filter prevents sensitive diagnostics from reaching any appender.
    *******************************************************************************/
   private static class ScopeFilter extends AbstractFilter
   {
      /*******************************************************************************
       ** Decide before Log4j creates or formats a data-bearing event.
       *******************************************************************************/
      private Result decision()
      {
         return (DEPTH.get() > 0 ? Result.DENY : Result.NEUTRAL);
      }

      /*******************************************************************************
       ** QLogger uses structured Log4j messages.
       *******************************************************************************/
      @Override
      public Result filter(Logger logger, Level level, Marker marker, Message message, Throwable error)
      {
         return (decision());
      }

      /*******************************************************************************
       ** Object diagnostics from third-party mapping or target customizers.
       *******************************************************************************/
      @Override
      public Result filter(Logger logger, Level level, Marker marker, Object message, Throwable error)
      {
         return (decision());
      }

      /*******************************************************************************
       ** Parameterized diagnostics from registered host extensions.
       *******************************************************************************/
      @Override
      public Result filter(Logger logger, Level level, Marker marker, String message, Object... parameters)
      {
         return (decision());
      }

      /*******************************************************************************
       ** Synchronous event filtering for configurations invoking this filter later.
       *******************************************************************************/
      @Override
      public Result filter(LogEvent event)
      {
         return (decision());
      }
   }
}
