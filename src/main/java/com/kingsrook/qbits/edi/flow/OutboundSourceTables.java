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

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.kingsrook.qbits.edi.storage.EdiRecords;
import com.kingsrook.qbits.edi.support.SystemWrites;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;

/*******************************************************************************
 ** Per-runtime cache of the source tables that have an active RECORD_CHANGE
 ** outbound definition on an active agreement.  The record-change listener
 ** consults it so writes to every other table (audits, jobs, alerts...) cost
 ** nothing.  It is loaded from committed data only (never in a writer's
 ** transaction).  A change to an agreement or document invalidates it, and
 ** again once that change commits (discarding any reload that raced it); until
 ** then the changing transaction bypasses the cache, so it sees its own
 ** definitions.  A short TTL covers changes made by another process.
 *******************************************************************************/
public final class OutboundSourceTables
{
   static final String TRIGGER = "RECORD_CHANGE";
   static final Set<String> DEFINITION_TABLES = Set.of("edi_agreement", "edi_agreement_document");

   private static final Duration TTL = Duration.ofSeconds(60);

   private final AtomicLong generation = new AtomicLong();
   private final AtomicLong loads = new AtomicLong();
   private final Set<QBackendTransaction> changingTransactions = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
   private volatile Snapshot snapshot;

   /*******************************************************************************
    ** Loaded table names, with the generation and time they were loaded at.
    *******************************************************************************/
   private record Snapshot(Set<String> tables, long generation, long loadedAtNanos)
   {
   }

   /*******************************************************************************
    ** Whether record changes to this table, written in this transaction (may be
    ** null), can enqueue outbound work.
    *******************************************************************************/
   public boolean contains(String table, QBackendTransaction transaction) throws QException
   {
      if(transaction != null && changingTransactions.contains(transaction))
      {
         return (true);
      }
      Snapshot current = snapshot;
      if(current == null || current.generation() != generation.get() || System.nanoTime() - current.loadedAtNanos() > TTL.toNanos())
      {
         current = load();
      }
      return (current.tables().contains(table));
   }

   /*******************************************************************************
    ** Drop the cache now, and again once the changing transaction commits.
    *******************************************************************************/
   public void invalidate(QBackendTransaction transaction)
   {
      generation.incrementAndGet();
      if(transaction != null)
      {
         changingTransactions.add(transaction);
         transaction.addAfterCommitCallback(() ->
         {
            changingTransactions.remove(transaction);
            generation.incrementAndGet();
         });
      }
   }

   /*******************************************************************************
    ** How many times the definitions have been loaded (for tests and diagnostics).
    *******************************************************************************/
   public long loadCount()
   {
      return (loads.get());
   }

   /*******************************************************************************
    ** Read committed active definitions once.  A load that raced an invalidation
    ** is not kept as current, because it carries the older generation.
    *******************************************************************************/
   private Snapshot load() throws QException
   {
      long loadGeneration = generation.get();
      Set<String> tables = SystemWrites.call(() ->
      {
         List<QRecord> documents = committed("edi_agreement_document", new QQueryFilter()
            .withCriteria("direction", QCriteriaOperator.EQUALS, "OUTBOUND").withCriteria("isActive", QCriteriaOperator.EQUALS, true)
            .withCriteria("outboundTrigger", QCriteriaOperator.EQUALS, TRIGGER));
         Set<Integer> agreementIds = agreementIds(documents);
         Set<Integer> activeAgreements = new HashSet<>();
         if(!agreementIds.isEmpty())
         {
            committed("edi_agreement", new QQueryFilter().withCriteria("isActive", QCriteriaOperator.EQUALS, true)
               .withCriteria("id", QCriteriaOperator.IN, agreementIds)).forEach(agreement -> activeAgreements.add(agreement.getValueInteger("id")));
         }
         Set<String> result = new HashSet<>();
         for(QRecord document : documents)
         {
            if(document.getValueString("sourceTableName") != null && activeAgreements.contains(document.getValueInteger("agreementId")))
            {
               result.add(document.getValueString("sourceTableName"));
            }
         }
         return (Set.copyOf(result));
      });
      loads.incrementAndGet();
      Snapshot loaded = new Snapshot(tables, loadGeneration, System.nanoTime());
      snapshot = loaded;
      return (loaded);
   }

   /*******************************************************************************
    ** Query committed rows - deliberately without the caller's transaction.
    *******************************************************************************/
   private static List<QRecord> committed(String table, QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput(table).withFilter(filter)).getRecords());
   }

   /*******************************************************************************
    ** The distinct non-null agreement ids the documents reference.
    *******************************************************************************/
   private static Set<Integer> agreementIds(List<QRecord> documents)
   {
      Set<Integer> ids = new HashSet<>();
      documents.forEach(document -> ids.add(document.getValueInteger("agreementId")));
      ids.remove(null);
      return (ids);
   }

   /*******************************************************************************
    ** Active agreement ids among those the documents reference, in one query in
    ** the current transaction.
    *******************************************************************************/
   static Set<Integer> activeAgreementIds(List<QRecord> documents) throws QException
   {
      Set<Integer> ids = agreementIds(documents);
      Set<Integer> active = new HashSet<>();
      if(!ids.isEmpty())
      {
         for(QRecord agreement : EdiRecords.query("edi_agreement", EdiRecords.equal("isActive", true).withCriteria("id", QCriteriaOperator.IN, ids)))
         {
            active.add(agreement.getValueInteger("id"));
         }
      }
      return (active);
   }
}
