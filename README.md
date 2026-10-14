# QBit EDI

A full EDI application for QQQ: trading partners and agreements, pluggable transports (AS2, SFTP
and FTPS, VAN mailboxes, HTTPS, email), X12 parsing and validation against data-driven
implementation guides, automatic 997/999/TA1 acknowledgements, versioned maps between EDI segments
and QQQ records, inbound and outbound flows with resend and reprocess, and HIPAA healthcare
documents with PHI classification. X12 004010 and 005010 first; EDIFACT is designed in and comes
later. The design is [docs/DESIGN.md](docs/DESIGN.md).

## Status

Phase a1 implements the 21-table model, X12 envelopes, baseline guide validation, TA1/997/999,
control allocation, duplicate checks, and encrypted database payload storage. Transports, inbox
workers, maps, and the app/home arrive in later phases; see `docs/DESIGN.md`.

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

The producer scans only `com.kingsrook.qbits.edi.metadata` for component producers. App placement
settings are retained for phase a2; a1 installs no app or home.

Apply [the PostgreSQL schema](docs/ddl/edi-postgres.sql), establish the host's service session in
QQQ, and call `EdiRuntime.current().initialize()` to install the five baseline guides. All other
services are available from that runtime. Database services use the current QQQ service context;
the host owns authentication, permissions and the authorized system scope for internal writes.

## Tables and permissions

Tables are lowercase snake_case prefixed `edi_`, and the SQL table name equals the QQQ table name.
Permissions use the full name (`edi.*`). Runtime tables disable people-facing insert, edit and
delete capabilities. Exceptions permit status, assignment and resolution edits. Optional partner
locks apply to direct and inherited partner relationships. See `docs/DESIGN.md` for the table list.

## Configuration

`EdiQBitConfig` holds backend/app placement, optional `schedulerName`,
`maxSegmentsPerTransaction` (100000), `payloadStore` (`DATABASE`), `secretsResolver` (environment
variables), and `partnerLocks` (false). Partners, agreements, transports and guides are table data.

Set `QQQ_EDI_PAYLOAD_KEY` to a base64-encoded 32-byte master key. During rotation, retain the old
key in `QQQ_EDI_PAYLOAD_KEY_PREVIOUS`; new writes use the current key. Payloads use random data
keys wrapped with AES-256-GCM, with authenticated ciphertext and plaintext SHA-256. Nothing is
written to local storage. The database fallback buffers a payload in memory; production object
storage is deferred to a2. The host is responsible for database encryption at rest for PHI columns.

RC.5 does not expose the upcoming core `DataClassification` API. `EdiClassifications` carries
explicit PHI/PII/confidential metadata until RC.6; PHI fields and internal payload/certificate/inbox
tables deny AI exposure. This marker does not provide field masking: a PHI host still needs the
secured-data integration and access controls described in the design.

Guide JSON and rule syntax are documented in [GUIDE-FORMAT.md](docs/GUIDE-FORMAT.md).

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
