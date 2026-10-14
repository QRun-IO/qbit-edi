# QBit EDI

EDI for QQQ: trading partners, agreements, X12 validation, encrypted payloads, inbound mapping
and application, and TA1/997/999 acknowledgements. See [the design](docs/DESIGN.md).

## Status

Phases a1/a2 implement the inbound vertical slice. A gateway 850 (004010 or 005010) becomes
a durable inbox record, validated document, permission-enforced target record with children,
and an enveloped 997 queued for delivery. HIPAA 005010X uses 999. Outbound sending, outbound
business-document building and real transport servers remain phase b and later.

## Requirements and installation

Java 21, QQQ **4.1.0-RC.5**, an RDBMS backend, and `com.kingsrook.qbits:qbit-edi:0.1.0-SNAPSHOT`.
Apply [the PostgreSQL schema](docs/ddl/edi-postgres.sql). Targets and their associations must
share the EDI backend so each transaction set can commit or roll back atomically.

```java
new EdiQBitProducer()
   .withQBitConfig(new EdiQBitConfig()
      .withBackendName("rdbms")
      .withSchedulerName("scheduler") // optional
      .withServiceSessionSupplier(() -> makeEdiServiceSession()))
   .produce(qInstance);
```

Call `EdiRuntime.current().initialize()` in the host's authorized startup context to install
baseline guides. Define partners, identities, active agreements, inbound agreement documents
and active maps as normal QQQ records. `makeEdiServiceSession()` is a host function returning
a least-privilege `QSession` with target and association permissions. Applying without a supplier
produces an APPLY exception: `no service session configured`.

The `edi` app has Activity/Setup sections, typed home widgets, record screens and standard
Review/Result actions. Main tables have RC.5 search-field metadata; `EdiIntegrations.mainTables()`
also exposes the list for host search registration.

## Tables

There are **26 persisted tables**, whose SQL names equal their QQQ names.

| Area | Tables |
| --- | --- |
| Setup | `edi_partner`, `edi_identity`, `edi_agreement`, `edi_agreement_document`, `edi_control_number`, `edi_transport`, `edi_certificate` |
| Guides | `edi_guide`, `edi_guide_segment`, `edi_guide_element`, `edi_guide_rule`, `edi_code_list`, `edi_code_list_value` |
| Maps | `edi_map`, `edi_map_rule`, `edi_map_sample` |
| Processing | `edi_inbox`, `edi_interchange`, `edi_functional_group`, `edi_transaction`, `edi_transaction_error`, `edi_exception`, `edi_event` |
| Storage/queue | `edi_payload`, `edi_outbound_request`, `edi_outbound_message` |

`edi_preview_download` is a **storage-only metadata alias**, not a SQL table. Supporting tables
appear as child lists. Partner-scoped form choices use RC.5 contingent filters, including shared
transports/guides and local certificates/identities.

## Inbound operation

`InboundSink.receive(InboundPayload)` stores encrypted bytes and SHA-256 before returning a durable
inbox record. Transport/external ID retries are idempotent; matching bytes within the 365-day intake
window create DUPLICATE rows. `EdiFileGatewayConsumer` uses the marked internal stand-in contract,
with upload ID plus digest as external ID, and returns `edi_inbox:<id>` without parsing.
Storage failure requests retry in one minute.

`InboxWorker.runOnce()` processes up to 20 messages with compare-and-set claims, fenced database
checkpoints and five-minute stale-claim recovery. Retry delays are 1m, 5m, 15m, 1h and 6h; the next
failure becomes DEAD_LETTER. Each mapped accepted ST applies in its own transaction through QQQ
actions with explicit permission enforcement. ISA15=T applies only with `applyTestDocuments`.

Routing checks ISA identities, qualifiers, standard and usage. Linked gateway usernames must match
the routed partner. Findings and failures become exceptions with occurrence counts and value-free
timeline events. Replies allocate controls and remain QUEUED, recording `ACK_QUEUED`. Incoming
acknowledgements reconcile outbound groups and resolve overdue alerts.

Reprocess restarts the containing inbox from encrypted storage, using original or current guide/map
revisions. Source-position checkpoints and target upsert keys preserve idempotency, stale findings
are replaced, and the same reply is not queued twice. A live PROCESSING claim cannot be reset.

## Maps and preview

EDI paths reuse the guide rule language. Targets support `field` and `association.field`;
wildcards such as `PO1[*]/PO107` produce child records. `matchKeyFields` controls upsert, with
`onMatch` UPDATE/SKIP/EXCEPTION. [MAPS.md](docs/MAPS.md) documents transform arguments.

The two `/edi/maps/850-*-sample.json` resources map onto `sample_order` and `sample_order_line`;
those tables exist **only in test sources**, never in host installations.

Preview shows parse counts, SNIP findings, the would-be ack, mapped JSON, expected-result and
previous-revision comparisons. It writes no target records. PHI previews suppress document values.
A session-authorized encrypted JSON report is downloadable. Select a stored sample or upload
synthetic EDI on the standard Review step. Browser uploads require the host's RC.5 Javalin
`uploadedFileArchiveTableName` to be `edi_preview_download` and multipart handling without disk
staging. The alias encrypts streams and persists samples; this process accepts no filesystem archive.
Samples must contain synthetic data only.

## Permissions

Full table permissions include `edi.partner.read|insert|edit|delete`, `edi.map.*`,
`edi.transaction.read` and `edi.exception.read|edit`. Multiword names use dots
(e.g. `edi.map.rule.read`). Runtime tables disable people-facing mutations.

| Action | Permission |
| --- | --- |
| Reprocess | `edi.reprocess` |
| Preview Map | `edi.map.preview` |
| Assign / Ignore / Mark Resolved | `edi.exception.assign` / `edi.exception.ignore` / `edi.exception.resolve` |
| Create Partner Override / Activate Guide | `edi.guide.override` / `edi.guide.activate` |
| Activate Map | `edi.map.activate` |
| Scheduled worker | `edi.admin` |

Partner locks use `edi_partner` with `edi.admin` all-access. Home queries explicitly enforce
the viewing user's permissions. Processes are table/record actions, never navigation entries.

## Configuration

| Setting | Default / behavior |
| --- | --- |
| `backendName` | Required host RDBMS backend |
| `appName`, `appLabel`, `parentAppName` | `edi`, `EDI`, no parent |
| `schedulerName` | Null; configure for 15-second polling |
| `serviceSessionSupplier` | Null; required when a map actually applies |
| `partnerLocks` | `false` |
| `maxSegmentsPerTransaction` | 100000 |
| `payloadStore` | `DATABASE` or `OBJECT_STORAGE` |
| `payloadBucketAlias` | `edi` |
| `secretsResolver` | Environment-variable lookup |
| `clock` | UTC system clock |
| `esbDestinationName` | Null; optional core ESB destination |

Set `QQQ_EDI_PAYLOAD_KEY` to a base64-encoded 32-byte master key; retain the old key in
`QQQ_EDI_PAYLOAD_KEY_PREVIOUS` during rotation. AES-256-GCM envelope encryption uses random data
keys and SHA-256 verification. Both stores use the same envelope and currently buffer a payload
in memory. Database storage remains the default for demos/tests.

For object storage, add/install optional `qbit-object-storage:0.1.0-RC.2`, configure its bucket
alias and select `OBJECT_STORAGE`. EDI uses only its public API and system access, with keys
`edi/yyyy/MM/dd/kind/uuid`. Its transitive QQQ dependencies are excluded to retain RC.5.

Optional `qqq-esb` publishes setup/activity events after commit. Document events contain only ID
and status because RC.5's standard factory otherwise copies PHI fields. Alert names are
`edi.inbox.dead-letter`, `edi.ack.overdue` and `edi.ack.rejected` (RC.5 requires dotted names).

RC.5 lacks the planned core `DataClassification` API. `EdiClassifications` marks PHI/PII/confidential
fields and PHI fields deny AI exposure. Hosts must supply secured-data protection, database
encryption at rest and access policies for PHI columns; markers alone do not mask fields.
The home reports installed integration and storage state.

## Build and verification

```bash
./mvnw -B verify
```

Use the wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM. Verification runs Checkstyle,
synthetic H2 tests and JaCoCo (70% instructions, 90% classes). Object-storage key/envelope metadata
are unit-tested. Live S3, a deployed gateway/scheduler and browser layout are not exercised.
The public object-storage facade is final with restricted constructors; tests avoid provider internals.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
