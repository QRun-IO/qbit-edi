/*******************************************************************************
 ** Configuration class for the Example Application QBit.
 **
 ** Application QBits support:
 ** - Backend configuration for data storage
 ** - Optional table prefixing for multi-instance deployment
 ** - Feature toggles for optional modules
 ** - Security key prefix for permission scoping
 ** - Integration hooks for other QBits
 *******************************************************************************/
package com.kingsrook.qbits.example;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


public class ExampleAppQBitConfig implements QBitConfig
{
   private String  backendName;
   private String  tableNamePrefix;
   private Boolean enableChildModule = true;
   private String  securityKeyPrefix;

   /////////////////////////////////////////////////////////////////////////
   // Integration hooks - allow host app to inject custom behavior        //
   /////////////////////////////////////////////////////////////////////////
   private QCodeReference onEntityCreatedHandler;
   private QCodeReference customValidationProvider;



   /*******************************************************************************
    ** Validate the configuration before the QBit is produced.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      if(!StringUtils.hasContent(backendName))
      {
         errors.add("backendName is required for ExampleAppQBit");
      }

      if(qInstance.getBackend(backendName) == null)
      {
         errors.add("Backend not found: " + backendName);
      }
   }



   /*******************************************************************************
    ** Apply prefix to a table name if configured.
    *******************************************************************************/
   public String applyPrefix(String tableName)
   {
      if(StringUtils.hasContent(tableNamePrefix))
      {
         return tableNamePrefix + "_" + tableName;
      }
      return tableName;
   }



   /*******************************************************************************
    ** Get the security key for permission checks.
    *******************************************************************************/
   public String getSecurityKey(String baseName)
   {
      if(StringUtils.hasContent(securityKeyPrefix))
      {
         return securityKeyPrefix + "." + baseName;
      }
      return baseName;
   }



   //////////////////////////////////////////////////////////////////////////////
   // Getters and fluent setters                                               //
   //////////////////////////////////////////////////////////////////////////////

   public String getBackendName()
   {
      return backendName;
   }


   public ExampleAppQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return this;
   }


   public String getTableNamePrefix()
   {
      return tableNamePrefix;
   }


   public ExampleAppQBitConfig withTableNamePrefix(String tableNamePrefix)
   {
      this.tableNamePrefix = tableNamePrefix;
      return this;
   }


   public Boolean getEnableChildModule()
   {
      return enableChildModule;
   }


   public ExampleAppQBitConfig withEnableChildModule(Boolean enableChildModule)
   {
      this.enableChildModule = enableChildModule;
      return this;
   }


   public String getSecurityKeyPrefix()
   {
      return securityKeyPrefix;
   }


   public ExampleAppQBitConfig withSecurityKeyPrefix(String securityKeyPrefix)
   {
      this.securityKeyPrefix = securityKeyPrefix;
      return this;
   }


   public QCodeReference getOnEntityCreatedHandler()
   {
      return onEntityCreatedHandler;
   }


   public ExampleAppQBitConfig withOnEntityCreatedHandler(QCodeReference onEntityCreatedHandler)
   {
      this.onEntityCreatedHandler = onEntityCreatedHandler;
      return this;
   }


   public QCodeReference getCustomValidationProvider()
   {
      return customValidationProvider;
   }


   public ExampleAppQBitConfig withCustomValidationProvider(QCodeReference customValidationProvider)
   {
      this.customValidationProvider = customValidationProvider;
      return this;
   }
}
