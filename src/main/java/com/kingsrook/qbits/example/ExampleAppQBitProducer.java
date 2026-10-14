/*******************************************************************************
 ** Producer for the Example Application QBit.
 **
 ** Application QBits produce:
 ** - QBitMetaData for identification
 ** - QAppSection for UI navigation (REQUIRED)
 ** - Tables, processes, widgets, and other metadata
 ** - Security configuration
 *******************************************************************************/
package com.kingsrook.qbits.example;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProducer;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qbits.example.model.ExampleEntity;
import com.kingsrook.qbits.example.model.ExampleChildEntity;


public class ExampleAppQBitProducer implements QBitProducer
{
   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "qbit-example-app";
   public static final String VERSION     = "0.1.0";

   private ExampleAppQBitConfig config;



   /*******************************************************************************
    ** Produce the QBit metadata into the QInstance.
    *******************************************************************************/
   @Override
   public void produce(QInstance qInstance, String namespace) throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // Validate configuration                                              //
      /////////////////////////////////////////////////////////////////////////
      List<String> errors = new ArrayList<>();
      config.validate(qInstance, errors);
      if(!errors.isEmpty())
      {
         throw new QException("Configuration errors: " + String.join(", ", errors));
      }

      /////////////////////////////////////////////////////////////////////////
      // Create and register QBit identity                                   //
      /////////////////////////////////////////////////////////////////////////
      QBitMetaData qBitMetaData = new QBitMetaData()
         .withGroupId(GROUP_ID)
         .withArtifactId(ARTIFACT_ID)
         .withVersion(VERSION)
         .withNamespace(namespace)
         .withConfig(config);
      qInstance.addQBit(qBitMetaData);

      /////////////////////////////////////////////////////////////////////////
      // Discover and produce all component metadata                         //
      /////////////////////////////////////////////////////////////////////////
      List<MetaDataProducerInterface<?>> producers =
         MetaDataProducerHelper.findProducers(getClass().getPackageName());

      for(MetaDataProducerInterface<?> producer : producers)
      {
         ///////////////////////////////////////////////////////////////////////
         // Skip disabled components                                         //
         ///////////////////////////////////////////////////////////////////////
         if(!isProducerEnabled(producer))
         {
            continue;
         }

         MetaDataProducerOutput output = producer.produce(qInstance);

         ///////////////////////////////////////////////////////////////////////
         // Apply table prefix if configured                                  //
         ///////////////////////////////////////////////////////////////////////
         if(output instanceof QTableMetaData table)
         {
            String prefixedName = config.applyPrefix(table.getName());
            table.setName(prefixedName);
            table.setBackendName(config.getBackendName());
         }

         ///////////////////////////////////////////////////////////////////////
         // Mark scope for all SourceQBitAware outputs                        //
         ///////////////////////////////////////////////////////////////////////
         if(output instanceof SourceQBitAware sqa)
         {
            sqa.setSourceQBitName(qBitMetaData.getName());
         }

         output.addSelfToInstance(qInstance);
      }

      /////////////////////////////////////////////////////////////////////////
      // Register QAppMetaData for UI navigation (REQUIRED for app QBits)    //
      /////////////////////////////////////////////////////////////////////////
      qInstance.addApp(produceApp(config, qBitMetaData.getName()));
   }



   /*******************************************************************************
    ** Produce the QAppMetaData for UI navigation.
    **
    ** This is REQUIRED for Application QBits - it defines how users navigate
    ** to the tables, processes, and widgets in the UI.
    *******************************************************************************/
   private QAppMetaData produceApp(ExampleAppQBitConfig config, String qbitName)
   {
      List<String> tables = new ArrayList<>();
      tables.add(config.applyPrefix(ExampleEntity.TABLE_NAME));

      if(Boolean.TRUE.equals(config.getEnableChildModule()))
      {
         tables.add(config.applyPrefix(ExampleChildEntity.TABLE_NAME));
      }

      QAppSection section = new QAppSection()
         .withName(config.applyPrefix("exampleSection"))
         .withLabel("Example Application")
         .withIcon(new QIcon().withName("dashboard"))
         .withTables(tables);

      return new QAppMetaData()
         .withName(config.applyPrefix("exampleApp"))
         .withLabel("Example Application")
         .withIcon(new QIcon().withName("dashboard"))
         .withSection(section);
   }



   /*******************************************************************************
    ** Check if a producer should be enabled based on configuration.
    *******************************************************************************/
   private boolean isProducerEnabled(MetaDataProducerInterface<?> producer)
   {
      /////////////////////////////////////////////////////////////////////////
      // Skip child entity if module disabled                                //
      /////////////////////////////////////////////////////////////////////////
      if(!Boolean.TRUE.equals(config.getEnableChildModule()))
      {
         if(producer.getClass().getSimpleName().contains("ExampleChildEntity"))
         {
            return false;
         }
      }
      return true;
   }



   //////////////////////////////////////////////////////////////////////////////
   // Fluent setters                                                           //
   //////////////////////////////////////////////////////////////////////////////

   public ExampleAppQBitProducer withConfig(ExampleAppQBitConfig config)
   {
      this.config = config;
      return this;
   }
}
