# QBit EDI

A full EDI application for QQQ: trading partners and agreements, pluggable transports (AS2, SFTP
and FTPS, VAN mailboxes, HTTPS, email), X12 parsing and validation against data-driven
implementation guides, automatic 997/999/TA1 acknowledgements, versioned maps between EDI segments
and QQQ records, inbound and outbound flows with resend and reprocess, and HIPAA healthcare
documents with PHI classification. X12 004010 and 005010 first; EDIFACT is designed in and comes
later. The design is [docs/DESIGN.md](docs/DESIGN.md).

## Status

Repository scaffold. The design spec PR and the phased build (partners and X12 core, retail, AS2,
logistics, healthcare, mapper UI, EDIFACT) follow; see the phase table in `docs/DESIGN.md`.

## Requirements

- Java 21; QQQ **4.1.0-RC.5+**.
- A configured QQQ backend for the qbit's `edi_` tables.

## Install

Use `com.kingsrook.qbits:qbit-edi:0.1.0-SNAPSHOT` once published, or build locally. After adding
your backend to the instance, install the producer:

```java
import com.kingsrook.qbits.edi.EdiQBitConfig;
import com.kingsrook.qbits.edi.EdiQBitProducer;

new EdiQBitProducer()
   .withQBitConfig(new EdiQBitConfig()
      .withBackendName("rdbms"))
   .produce(qInstance);
```

The producer scans only `com.kingsrook.qbits.edi.metadata` for component producers. Set
`withParentAppName("admin")` to nest the EDI app under an existing parent app.

## Tables and permissions

Tables are lowercase snake_case prefixed `edi_`, and the SQL table name equals the QQQ table name.
Permissions use the full name (`edi.*`). The table list arrives with phase (a); see
`docs/DESIGN.md`.

## Configuration

Code configuration (`EdiQBitConfig`) holds only the backend and app placement. Partners,
agreements, transports, implementation guides and maps are data in the `edi_` tables. Secrets
(AS2 keys, SFTP credentials) are referenced by environment-variable name, never stored.

## Adopting in another app

Add the dependency, install the producer as above, and grant the `edi.*` permissions to the roles
that operate EDI. Inbound transports register non-UI routes under `/qqq/hooks/edi/...`.

## Build

```bash
./mvnw verify   # Checkstyle, tests, JaCoCo gate (70% instructions, 90% classes)
```

Always use the wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
