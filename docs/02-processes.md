# Processes

Start with [ExampleProcessMetaDataProducer](../src/main/java/com/kingsrook/qbits/example/processes/ExampleProcessMetaDataProducer.java) and [ExampleProcessStep](../src/main/java/com/kingsrook/qbits/example/processes/ExampleProcessStep.java). The producer defines the process, its table, inputs, and backend step; the step transforms `PENDING` records to `PROCESSED` and returns them in the output.

In QQQ 4.0, import `MetaDataProducerInterface` from `com.kingsrook.qqq.backend.core.model.metadata`. Define process inputs through `withInputMetaData(new QFunctionInputMetaData().withField(...))`. An `AbstractTransformStep` must implement both `runOnePage(RunBackendStepInput, RunBackendStepOutput)` and `getProcessSummary(RunBackendStepOutput, boolean)`; keep the example's implementations when renaming it.

To make the process discoverable in navigation, add its registered name to a `QAppSection` through `withProcesses(List.of(ExampleProcessMetaDataProducer.NAME))`, then include the section in a `QAppMetaData` registered with `qInstance.addApp(...)`. Use the actual registered name if you introduce a prefix. Add tests for record changes and failure cases when replacing the example business logic.
