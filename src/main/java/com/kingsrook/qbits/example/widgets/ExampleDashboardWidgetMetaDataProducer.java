/*******************************************************************************
 ** MetaData producer for an example dashboard widget.
 **
 ** Widgets provide dashboard components for Application QBits.
 *******************************************************************************/
package com.kingsrook.qbits.example.widgets;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;


public class ExampleDashboardWidgetMetaDataProducer implements MetaDataProducerInterface<QWidgetMetaData>
{
   public static final String NAME = "exampleDashboardWidget";



   /*******************************************************************************
    ** Produce the widget metadata.
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withLabel("Example Dashboard")
         .withType(WidgetType.STATISTICS.getType())
         .withCodeReference(new QCodeReference(ExampleDashboardWidgetRenderer.class));
   }
}
