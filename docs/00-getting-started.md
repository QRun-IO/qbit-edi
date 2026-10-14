# Getting Started

Targets Java 21, Maven 3.8 or later, and QQQ 4.0.0. Maven resolves the framework dependencies from Maven Central.

1. Choose **Use this template** on GitHub, then clone the repository you created.
2. In `pom.xml`, set your own `groupId`, `artifactId`, name, description, and version.
3. Use your IDE's package refactoring to rename `com.kingsrook.qbits.example` throughout `src/`. Rename the `Example*` classes and update their imports and references with the IDE's rename refactoring.
4. Update the QBit producer's `GROUP_ID`, `ARTIFACT_ID`, and `VERSION` constants to match your project. Give the metadata names their own stable names before combining the QBit with other examples.
5. Build the generated project:

```bash
mvn clean verify
```

The repository provides example Java sources; customization uses normal package/class refactoring. Keep example coordinates unpublished. After customizing, add tests for your QBit's behavior before setting up its publishing workflow.

## Register with a host application

The following uses the original example names; substitute the names chosen above:

```java
new ExampleAppQBitProducer()
   .withConfig(new ExampleAppQBitConfig()
      .withBackendName("rdbms"))
   .produce(qInstance, "my-application");
```

Register the configured backend in the host first. If you add table prefixes or multiple QBit instances, update related table references, possible-value-source names, and process/widget names consistently. A table prefix alone does not namespace all metadata.

[Tables and Relationships](01-tables-and-relationships.md) describes the model; the `processes/` and `widgets/` directories contain the remaining examples.
