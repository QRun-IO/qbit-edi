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


import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;


/*******************************************************************************
 ** Explicitly enforce table and field permissions inside the widget's action scope.
 ** RC.5 treats nested DEFAULT queries as trusted, so the generic renderer alone
 ** does not remove fields denied by a FieldSecurityLock.
 *******************************************************************************/
public class EdiChildRecordListRenderer extends ChildRecordListRenderer
{
   /*******************************************************************************
    ** Use the standard typed child list with an enforced query at its public boundary.
    *******************************************************************************/
   @Override
   protected QueryOutput executeQuery(QueryInput input) throws QException
   {
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (super.executeQuery(input));
   }
}
