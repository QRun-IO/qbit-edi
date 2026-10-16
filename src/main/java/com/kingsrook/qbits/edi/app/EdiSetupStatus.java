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

import java.util.ArrayList;
import java.util.List;

/*******************************************************************************
 ** The Setup status on the EDI home: each setup step in the order it has to be
 ** done (partner, identity, agreement, document, transport), whether it is
 ** done, and the next step. The counts use the same conditions the runtime
 ** uses to route documents (active rows), so the card never reports "set up"
 ** for rows the runtime ignores.
 *******************************************************************************/
public final class EdiSetupStatus
{
   /*******************************************************************************
    ** DONE: working. TODO: the exchange needs it. BUILT_IN: provided by the
    ** qbit. OPTIONAL: only some setups need it. UNKNOWN: its count failed (for
    ** example the viewer cannot read the table), shown as "Unavailable".
    *******************************************************************************/
   public enum State
   {
      DONE("Set up"), TODO("Needs setup"), BUILT_IN("Built in"), OPTIONAL("Optional"), UNKNOWN("Unavailable");

      private final String label;

      /*******************************************************************************
       **
       *******************************************************************************/
      State(String label)
      {
         this.label = label;
      }

      /*******************************************************************************
       ** The label shown on the card.
       *******************************************************************************/
      public String label()
      {
         return (label);
      }
   }

   /*******************************************************************************
    ** One line of the card.
    *******************************************************************************/
   public record Item(String step, String tableName, State state, String detail, String nextStep)
   {
   }

   /*******************************************************************************
    ** The row counts the card is built from; null means the count failed and the
    ** step shows as unavailable. Active counts are rows with isActive true (what
    ** the runtime uses).
    *******************************************************************************/
   public record Counts(Integer partners, Integer ownIdentities, Integer partnerIdentities, Integer agreements, Integer activeAgreements, Integer agreementDocuments,
                        Integer activeAgreementDocuments, Integer transports, Integer activeTransports, Integer certificates, Integer guides, Integer codeLists,
                        Integer maps, Integer activeMaps)
   {
   }

   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private EdiSetupStatus()
   {
   }

   /*******************************************************************************
    ** The items, in setup order.
    *******************************************************************************/
   public static List<Item> items(Counts c)
   {
      List<Item> items = new ArrayList<>();
      items.add(item("1. Partner", "edi_partner", c.partners(), c.partners(), "partner", "Add the trading partner with New on Partners."));
      items.add(identities(c));
      items.add(item("3. Agreement", "edi_agreement", c.agreements(), c.activeAgreements(), "agreement",
         "Add an agreement on Agreements: your identity, the partner's identity, the standard and test or production. Make sure Active is on."));
      items.add(item("4. Agreement documents", "edi_agreement_document", c.agreementDocuments(), c.activeAgreementDocuments(), "agreement document",
         "Add one per transaction set and direction on Agreement Documents: pick the guide, and a map if it should become records. A document type with no row is not processed."));
      items.add(item("5. Transport", "edi_transport", c.transports(), c.activeTransports(), "transport",
         "Add the AS2, SFTP or HTTPS connection on Transports (secrets as variable names), link it on the agreement, then run Test Connection."));
      items.add(c.certificates() == null ? new Item("Certificates", "edi_certificate", State.UNKNOWN, "", "")
         : c.certificates() == 0
            ? new Item("Certificates", "edi_certificate", State.OPTIONAL, "None.", "Needed only for AS2 signing and encryption: add yours and the partner's on Certificates.")
            : new Item("Certificates", "edi_certificate", State.DONE, c.certificates() + (c.certificates() == 1 ? " certificate." : " certificates."), ""));
      items.add(c.guides() == null || c.codeLists() == null ? new Item("Guides and code lists", "edi_guide", State.UNKNOWN, "", "")
         : c.guides() == 0 || c.codeLists() == 0
            ? new Item("Guides and code lists", "edi_guide", State.TODO, "The built-in guides are not loaded.", "The host must call EdiRuntime.initialize() at startup, after the schema exists.")
            : new Item("Guides and code lists", "edi_guide", State.BUILT_IN, c.guides() + " guides and " + c.codeLists() + " code lists.", ""));
      items.add(c.maps() == null || c.activeMaps() == null ? new Item("Maps", "edi_map", State.UNKNOWN, "", "")
         : c.maps() == 0
            ? new Item("Maps", "edi_map", State.OPTIONAL, "None.", "Needed to turn documents into records. Run Install sample maps on Maps to start from an example.")
            : c.activeMaps() == 0
               ? new Item("Maps", "edi_map", State.TODO, c.maps() + " maps, none active.", "Point a map at your table, preview it, then run Activate Map.")
               : new Item("Maps", "edi_map", State.DONE, c.activeMaps() + " active of " + c.maps() + ".", ""));
      return (items);
   }

   /*******************************************************************************
    ** Step 2: your own identity (no partner) and the partner's.
    *******************************************************************************/
   private static Item identities(Counts c)
   {
      String step = "2. Identities";
      if(c.ownIdentities() == null || c.partnerIdentities() == null)
      {
         return (new Item(step, "edi_identity", State.UNKNOWN, "", ""));
      }
      if(c.ownIdentities() == 0)
      {
         return (new Item(step, "edi_identity", State.TODO, "None of yours.", "Add your own ISA or UNB qualifier and ID on Identities, with no partner."));
      }
      if(c.partnerIdentities() == 0)
      {
         return (new Item(step, "edi_identity", State.TODO, "Yours is set; no partner has one.", "Add the partner's qualifier and ID on Identities, with the partner selected."));
      }
      return (new Item(step, "edi_identity", State.DONE, c.ownIdentities() + " of yours, " + c.partnerIdentities() + " for partners.", ""));
   }

   /*******************************************************************************
    ** A step that needs at least one active row.
    *******************************************************************************/
   private static Item item(String step, String table, Integer total, Integer active, String noun, String next)
   {
      if(total == null || active == null)
      {
         return (new Item(step, table, State.UNKNOWN, "", ""));
      }
      if(total == 0)
      {
         return (new Item(step, table, State.TODO, "None.", next));
      }
      if(active == 0)
      {
         return (new Item(step, table, State.TODO, total + " " + noun + (total == 1 ? "" : "s") + ", none active.", "Turn Active on for the one to use."));
      }
      return (new Item(step, table, State.DONE, active + " active.", ""));
   }
}
