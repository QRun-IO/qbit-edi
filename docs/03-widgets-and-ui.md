# Widgets and UI

The application producer registers a `QAppMetaData` containing a `QAppSection`. The section references metadata names already registered in the instance; it is not registered directly on `QInstance`.

```java
QAppSection section = new QAppSection()
   .withName("exampleSection")
   .withLabel("Example Application")
   .withTables(List.of(ExampleEntity.TABLE_NAME, ExampleChildEntity.TABLE_NAME))
   .withProcesses(List.of(ExampleProcessMetaDataProducer.NAME))
   .withWidgets(List.of(ExampleDashboardWidgetMetaDataProducer.NAME));

qInstance.addApp(new QAppMetaData()
   .withName("exampleApp")
   .withLabel("Example Application")
   .withSection(section));
```

Use [ExampleAppQBitProducer](../src/main/java/com/kingsrook/qbits/example/ExampleAppQBitProducer.java) as the starting point. Keep these references consistent when enabling/disabling tables or introducing prefixes.

The widget has two parts: [its metadata producer](../src/main/java/com/kingsrook/qbits/example/widgets/ExampleDashboardWidgetMetaDataProducer.java) declares a statistics widget and its renderer, and [its renderer](../src/main/java/com/kingsrook/qbits/example/widgets/ExampleDashboardWidgetRenderer.java) returns a `RenderWidgetOutput` containing `StatisticsData`. Replace the example count with a query or calculation. Choose a widget type that matches the returned data model.

For QQQ 4.0, `AbstractWidgetRenderer` is in `com.kingsrook.qqq.backend.core.actions.dashboard.widgets`; `RenderWidgetInput` and `RenderWidgetOutput` are in `com.kingsrook.qqq.backend.core.model.actions.widgets`.
