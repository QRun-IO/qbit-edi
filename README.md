# QBit Template - Application

Template repository for creating **Application QBits** - complete, self-contained applications or major functional modules for the QQQ framework.

## When to Use This Template

Use this template when building:
- Complete mini-applications (WMS, OMS, CRM)
- Major functional modules with multiple tables
- QBits that require UI navigation (QAppSection)
- Self-contained features with business logic

## Quick Start

Requires **Java 21**, **Maven 3.8+**, and QQQ **4.0.0**. Create a repository with **Use this template**, customize its Maven coordinates and Java package/classes, then run `mvn clean verify`. Follow the [Getting Started guide](docs/00-getting-started.md) for the full sequence.

## Structure

```
src/main/java/com/kingsrook/qbits/example/
├── ExampleAppQBitConfig.java
├── ExampleAppQBitProducer.java
├── model/
│   ├── ExampleEntity.java
│   └── ExampleChildEntity.java
├── processes/
│   ├── ExampleProcessStep.java
│   └── ExampleProcessMetaDataProducer.java
└── widgets/
    ├── ExampleDashboardWidgetRenderer.java
    └── ExampleDashboardWidgetMetaDataProducer.java
```

## Key Characteristics

| Feature | Description |
|---------|-------------|
| QAppSection | Required - provides UI navigation |
| Tables | Multiple interconnected business tables |
| Processes | Full CRUD with business logic |
| Widgets | Dashboard components |
| Security | Permission key support |
| Integration | Hooks for other QBits |

## Documentation

- [Getting Started](docs/00-getting-started.md)
- [Tables and Relationships](docs/01-tables-and-relationships.md)
- [Processes](docs/02-processes.md)
- [Widgets and UI](docs/03-widgets-and-ui.md)

## See Also

- [qbit-template-extension](https://github.com/QRun-IO/qbit-template-extension) - For infrastructure extensions
- [qbit-template-data](https://github.com/QRun-IO/qbit-template-data) - For reference data providers
