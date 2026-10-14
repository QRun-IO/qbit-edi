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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;


/*******************************************************************************
 ** Code configuration for the EDI qbit. Runtime settings (partners, agreements,
 ** transports, guides and maps) are data in the edi_ tables, not here.
 *******************************************************************************/
public class EdiQBitConfig implements QBitConfig
{
   public static final String TABLE_NAME_PREFIX    = "edi_";
   public static final String PERMISSION_BASE_NAME = "edi";

   private String schedulerName;
   private Integer maxSegmentsPerTransaction = 100000;
   private String payloadStore = "DATABASE";
   private com.kingsrook.qbits.edi.security.EdiSecrets secretsResolver = System::getenv;
   private Boolean partnerLocks = false;
   private String backendName;
   private String appName  = "edi";
   private String appLabel = "EDI";
   private String parentAppName;



   /*******************************************************************************
    ** Validate the configuration, collecting all errors.
    *******************************************************************************/
   @Override
   public void validate(QInstance instance, List<String> errors)
   {
      if(backendName == null || instance.getBackend(backendName) == null)
      {
         errors.add("backendName must name an existing backend");
      }
      if(maxSegmentsPerTransaction == null || maxSegmentsPerTransaction < 2)
      {
         errors.add("maxSegmentsPerTransaction must be at least 2");
      }
      if(!"DATABASE".equals(payloadStore))
      {
         errors.add("payloadStore must be DATABASE in phase a1");
      }
      if(secretsResolver == null || partnerLocks == null)
      {
         errors.add("secretsResolver and partnerLocks are required");
      }
      if(appName == null || appName.isBlank())
      {
         errors.add("appName is required");
      }
   }



   /*******************************************************************************
    ** The backend for the qbit's tables.
    *******************************************************************************/
   @Override
   public String getDefaultBackendNameForTables()
   {
      return (backendName);
   }



   /*******************************************************************************
    ** Getter for backendName
    *******************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }



   /*******************************************************************************
    ** Fluent setter for backendName
    *******************************************************************************/
   public EdiQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for appName
    *******************************************************************************/
   public String getAppName()
   {
      return (this.appName);
   }



   /*******************************************************************************
    ** Fluent setter for appName
    *******************************************************************************/
   public EdiQBitConfig withAppName(String appName)
   {
      this.appName = appName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for appLabel
    *******************************************************************************/
   public String getAppLabel()
   {
      return (this.appLabel);
   }



   /*******************************************************************************
    ** Fluent setter for appLabel
    *******************************************************************************/
   public EdiQBitConfig withAppLabel(String appLabel)
   {
      this.appLabel = appLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentAppName
    *******************************************************************************/
   public String getParentAppName()
   {
      return (this.parentAppName);
   }



   /*******************************************************************************
    ** Fluent setter for parentAppName
    *******************************************************************************/
   public EdiQBitConfig withParentAppName(String parentAppName)
   {
      this.parentAppName = parentAppName;
      return (this);
   }

   /*******************************************************************************
    ** Get schedulerName.
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (schedulerName);
   }

   /*******************************************************************************
    ** Configure schedulerName.
    *******************************************************************************/
   public EdiQBitConfig withSchedulerName(String value)
   {
      schedulerName = value;
      return (this);
   }

   /*******************************************************************************
    ** Get maxSegmentsPerTransaction.
    *******************************************************************************/
   public Integer getMaxSegmentsPerTransaction()
   {
      return (maxSegmentsPerTransaction);
   }

   /*******************************************************************************
    ** Configure maxSegmentsPerTransaction.
    *******************************************************************************/
   public EdiQBitConfig withMaxSegmentsPerTransaction(Integer value)
   {
      maxSegmentsPerTransaction = value;
      return (this);
   }

   /*******************************************************************************
    ** Get payloadStore.
    *******************************************************************************/
   public String getPayloadStore()
   {
      return (payloadStore);
   }

   /*******************************************************************************
    ** Configure payloadStore.
    *******************************************************************************/
   public EdiQBitConfig withPayloadStore(String value)
   {
      payloadStore = value;
      return (this);
   }

   /*******************************************************************************
    ** Get secretsResolver.
    *******************************************************************************/
   public com.kingsrook.qbits.edi.security.EdiSecrets getSecretsResolver()
   {
      return (secretsResolver);
   }

   /*******************************************************************************
    ** Configure secretsResolver.
    *******************************************************************************/
   public EdiQBitConfig withSecretsResolver(com.kingsrook.qbits.edi.security.EdiSecrets value)
   {
      secretsResolver = value;
      return (this);
   }

   /*******************************************************************************
    ** Get partnerLocks.
    *******************************************************************************/
   public Boolean getPartnerLocks()
   {
      return (partnerLocks);
   }

   /*******************************************************************************
    ** Configure partnerLocks.
    *******************************************************************************/
   public EdiQBitConfig withPartnerLocks(Boolean value)
   {
      partnerLocks = value;
      return (this);
   }
}
