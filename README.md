# QBit EDI

EDI for QQQ: trading partners, agreements, X12 and EDIFACT validation, encrypted payloads, inbound mapping
and application, and TA1/997/999/CONTRL acknowledgements. See [the design](docs/DESIGN.md).

## Status

Phases a1, a2, b, d and g implement inbound/outbound retail and logistics EDI. Original baseline guides cover
850, 855, 856, 810, 846 and 860 in 004010/005010, plus 997/999. Source records and associations
produce validated, encrypted outbound interchanges, with control-number tracking, retries,
acknowledgement monitoring and operator resend/cancel actions. AS2 and email sending remain disabled.
A runnable synthetic [demo](docs/DEMO.md) includes Next UI and needs no external services.

EDIFACT syntax versions 3 and 4 support UNA/UNB, optional UNG groups, UNH messages and their
trailers, release-character escaping, mixed-standard payloads, and CONTRL syntax responses.
Original D96A/D01B baselines cover ORDERS, ORDRSP, DESADV, INVOIC and CONTRL. ORDERS maps to
sample orders and lines; the other three retail documents map outbound. APERAK remains future work.

Configure `standard=EDIFACT` on identities and agreements. `interchangeId` and
`interchangeQualifier` carry the UNB party identifiers and qualifiers. Set `interchangeVersion`
to `3` or `4`, `syntaxIdentifier` to `UNOC`, and outbound separators explicitly: element `+`,
component `:`, segment `'`, release `?`, decimal `.` (or `,`); syntax 4 also uses the configured
repetition separator. Document versions are `D96A` or `D01B`, with `ackExpectation=CONTRL`.
UNB11=`1` uses a test agreement and respects `applyTestDocuments`.

Existing schemas need these additional nullable agreement columns before using this version:

```sql
ALTER TABLE edi_agreement ADD COLUMN release_character VARCHAR(255);
ALTER TABLE edi_agreement ADD COLUMN decimal_mark VARCHAR(255);
ALTER TABLE edi_agreement ADD COLUMN syntax_identifier VARCHAR(255);
```

## Requirements and installation

Java 21, QQQ **4.1.0-RC.5**, an RDBMS backend, and `com.kingsrook.qbits:qbit-edi:0.1.0-SNAPSHOT`.
Apply [the PostgreSQL schema](docs/ddl/edi-postgres.sql). Targets and their associations must
share the EDI backend so each transaction set can commit or roll back atomically.

```java
new EdiQBitProducer()
   .withQBitConfig(new EdiQBitConfig()
      .withBackendName("rdbms")
      .withSchedulerName("scheduler") // optional
      .withSendAsEdiTable("sample_order") // host-defined table, optional manual action
      .withServiceSessionSupplier(() -> makeEdiServiceSession()))
   .produce(qInstance);
```

Call `EdiRuntime.current().initialize()` in the host's authorized startup context to install
baseline guides and data-driven schedules. After defining batch/poll settings, call
`OutboundProcesses.installSchedules()` before starting the host scheduler; rebuild/restart scheduling
after changing those settings. Define partners, identities, active agreements, inbound agreement documents
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
failure becomes DEAD_LETTER. Each mapped accepted ST or UNH applies in its own transaction through QQQ
actions with explicit permission enforcement. ISA15=T or UNB11=1 applies only with `applyTestDocuments`.

Routing checks ISA or UNB identities, qualifiers, standard and usage. Linked gateway usernames must match
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

Resources under `/edi/maps` include inbound 850/860 and outbound 855/856/810/846 maps.
The order, shipment/carton/item, invoice/line and inventory schemas exist **only in test sources**.
Outbound maps support nested association paths; inbound target paths retain the one-association limit.

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
| Send As EDI | `edi.send` |
| Resend / Resend As New | `edi.resend` |
| Cancel | `edi.cancel` |
| Test Connection | `edi.transport.test` |
| Scheduled workers / ESB enqueue | `edi.admin` |

Partner locks use `edi_partner` with `edi.admin` all-access. Home queries explicitly enforce
the viewing user's permissions. Processes are table/record actions, never navigation entries.

## Configuration

| Setting | Default / behavior |
| --- | --- |
| `backendName` | Required host RDBMS backend |
| `appName`, `appLabel`, `parentAppName` | `edi`, `EDI`, no parent |
| `schedulerName` | Null; configure for 15-second polling |
| `serviceSessionSupplier` | Null; required for inbound application and outbound source reads/mapping |
| `withSendAsEdiTable(name)` | Opt a host table into Send As EDI |
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
`edi.inbox.dead-letter`, `edi.ack.overdue`, `edi.ack.rejected`, `edi.send.failed`,
`edi.certificate.expiring` and `edi.certificate.expired` (RC.5 requires dotted names).

RC.5 lacks the planned core `DataClassification` API. `EdiClassifications` marks PHI/PII/confidential
fields and PHI fields deny AI exposure. Hosts must supply secured-data protection, database
encryption at rest and access policies for PHI columns; markers alone do not mask fields.
The home reports installed integration and storage state.

## Outbound and transports

Active OUTBOUND agreement documents select the source table, map, guide and trigger. RECORD_CHANGE
uses core's instance listener and the writer's transaction. Source and EDI tables must share the
backend. `triggerFilter` is a serialized QQQ `QQueryFilter`; MANUAL also honors that filter.
RC.5 catches listener errors without failing the source write: successful enqueues roll back with
the source transaction, but hosts must monitor failed listener execution.

`edi_outbound_enqueue` is the ESB boundary. Configure an RC.5 `EsbTrigger` to invoke it with
`sourceTableName`, `sourceRecordId` and `eventId`; broker setup belongs to the host. Event IDs are
unique per agreement document. RC.5's consumer runner uses JMS, so broker-free acceptance calls
this process directly.

`edi_outbound_worker` and `edi_send_worker` run every 15 seconds when a scheduler is configured.
IMMEDIATE produces one interchange per request. BATCH cuts up to 100 requests for one agreement
document at its UTC `batchSchedule` into one group/interchange. Mapping/enveloping and sending
use database row locks to fence replicas; crashes release the lock. Remote delivery is at least
once: a lost connection after remote acceptance can resend the same bytes and control numbers.
Retry waits are 1m, 5m, 15m, 1h, 3h, 6h, 12h, 24h, capped at the last delay; `maxAttempts` defaults
to 8. Permanent transport rejection is FAILED; retry exhaustion is DEAD_LETTER. Both open a
TRANSPORT exception and raise `edi.send.failed`.

| Transport | Behavior and settings |
| --- | --- |
| GATEWAY | `gatewayEndpointName`; marked `FileGatewayOutbox` stand-in with a provider registration hook. Put is SENT; `EdiFileGatewayConsumer.onDownloaded` is DELIVERED. |
| SFTP | MINA client from core's filesystem module; required `sftpHostKeyFingerprint` in MINA SHA256 format, username and password/private-key env name. Streams upload to `.tmp`, then rename. `sftpPollCron` schedules remote pickup; `sftpFilePattern` is a Java regex (default `.*\.x12`). DELETE/MOVE occurs only after durable inbox commit. |
| HTTPS | POST with NONE/BASIC/BEARER/HMAC; TLS required except loopback. BASIC's secret is `user:password`; BEARER's is the token; HMAC uses core Standard Webhooks `whsec_` format. `httpsHeadersJson` holds nonsecret headers only. Redirects are refused; 2xx is DELIVERED, 429/5xx retry. |
| AS2 / EMAIL | Settings validation only. Sending returns a permanent unavailable result until phase c / the email API. |

Optional Javalin registers `POST /qqq/hooks/edi/https/{transport}`. `httpsInboundAuth` must be
BASIC, BEARER or HMAC with `httpsSecretEnvVar`. It authenticates exact bytes, caps intake at
50 MiB and returns 202 only after durable storage. HMAC verifies timestamps and deduplicates by
transport plus webhook ID. TLS terminates at the host ingress.

The transport registry is `EdiTransportPluginsMetaData`, attached to `QInstance`. Hosts can
register/replace implementations with `withPlugin`. All transports open encrypted PayloadStore
streams; production code never writes local files.

Ack deadlines start on the first successful send. `edi_ack_monitor` runs every five minutes,
raises overdue alerts and exceptions, and checks certificate expiry at 30/14/7 days and expiry.
A functional ack resolves its alert and exception; TA1 updates interchange acknowledgement only.
Resend retains archived bytes/control numbers. Resend As New re-envelopes the archived document
without re-running its map and links `originalInterchangeId`. Cancel accepts QUEUED/RETRY_WAIT only.

Existing a2 databases need `edi_interchange.original_interchange_id INTEGER` and a unique index
on `edi_outbound_request(agreement_document_id, event_id)` in addition to their prior schema.

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
