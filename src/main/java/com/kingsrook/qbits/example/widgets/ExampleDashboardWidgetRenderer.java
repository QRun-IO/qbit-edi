/*******************************************************************************
 ** Renderer for the example dashboard widget.
 **
 ** This class produces the data displayed in the widget.
 *******************************************************************************/
package com.kingsrook.qbits.example.widgets;


import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;


public class ExampleDashboardWidgetRenderer extends AbstractWidgetRenderer
{
   /*******************************************************************************
    ** Render the widget data.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // Add chart data, counts, or other dashboard information here         //
      /////////////////////////////////////////////////////////////////////////
      StatisticsData data = new StatisticsData()
         .withCount(0)
         .withCountContext("Total items");

      return (new RenderWidgetOutput(data));
   }
}
