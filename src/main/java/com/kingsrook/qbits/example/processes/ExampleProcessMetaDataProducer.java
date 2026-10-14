/*******************************************************************************
 ** MetaData producer for the example process.
 *******************************************************************************/
package com.kingsrook.qbits.example.processes;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qbits.example.model.ExampleEntity;


public class ExampleProcessMetaDataProducer implements MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "exampleProcess";



   /*******************************************************************************
    ** Produce the process metadata.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(NAME)
         .withLabel("Example Process")
         .withTableName(ExampleEntity.TABLE_NAME)
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("process")
               .withCode(new QCodeReference(ExampleProcessStep.class))
               .withInputData(new QFunctionInputMetaData()
                  .withField(new QFieldMetaData("confirmAction", QFieldType.BOOLEAN)
                     .withLabel("Confirm Action")
                     .withDefaultValue(false)))
         ));
   }
}
