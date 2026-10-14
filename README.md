# QBit EDI

EDI for QQQ: trading partners, agreements, X12 and EDIFACT validation, encrypted payloads, inbound mapping
and application, and TA1/997/999/CONTRL acknowledgements. See [the design](docs/DESIGN.md).

## Status

Phases a1 to e and g implement inbound/outbound retail, logistics and healthcare EDI (X12 and EDIFACT) over gateway, SFTP, HTTPS and AS2. Original baseline guides cover
850, 855, 856, 810, 846 and 860 in 004010/005010, plus 997/999. Source records and associations
produce validated, encrypted outbound interchanges, with control-number tracking, retries,
acknowledgement monitoring and operator resend/cancel actions. AS2 (signed, encrypted, compressed, synchronous and
asynchronous MDN, certificate rollover) is built in; email sending remains disabled until the qbit-email API ships.
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
| Promote Certificate | `edi.certificate.promote` |
| Export Public Certificate | `edi.certificate.read` |
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
| `publicBaseUrl` | Null; the deployment base URL (for example `https://edi.example.com`), required for ASYNC AS2 receipts |
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
`edi.certificate.expiring`, `edi.certificate.expired` and `edi.mdn.overdue` (RC.5 requires dotted names).

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
| AS2 | See [AS2](#as2). |
| EMAIL | Settings validation only. Sending returns a permanent unavailable result until the qbit-email API ships. |

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

## AS2

`As2Transport` (type `AS2`) sends and receives AS2 (RFC 4130) with S/MIME signing and encryption
(SHA-256/384/512, AES-128/256 CBC or GCM; SHA1 and 3DES only when set explicitly and flagged by
the read-only `weakCrypto` field), optional zlib compression, and synchronous or asynchronous
signed MDNs. Everything is in memory: no keystore, key or temporary file is ever written.

**Transport settings** (`edi_transport`, type AS2): `as2LocalId`, `as2PartnerId`, `as2Url` (HTTPS;
plain HTTP only on loopback), `as2Subject`, `signAlgorithm` (blank = unsigned), `encryptAlgorithm`
(blank = unencrypted), `compress`, `mdnMode` (`SYNC` default, `ASYNC`, `NONE`), `mdnSigned`,
`localCertificateId` (ours), `partnerCertificateId`, optional `partnerTlsCertificateId` (pins the
partner TLS certificate), and optional `httpAuthUserEnvVar` / `httpAuthPasswordEnvVar` for HTTP
Basic. For inbound, a set `signAlgorithm` / `encryptAlgorithm` means signed / encrypted messages are
required (otherwise `insufficient-message-security`). A VAN is an AS2 transport with `isVan` true
and no `partnerId`; its partner certificate belongs to the VAN operator's partner record, and
routing to trading partners stays by ISA identity.

**Certificates** (`edi_certificate`): paste the public PEM only; subject, issuer, serial,
SHA-256 fingerprint and validity are parsed on save, and a pasted private key is refused. Our private
key is never stored: `privateKeyEnvVar` names an environment variable holding a PEM PKCS#8 key or a
base64 PKCS#12 store (with `privateKeyPasswordEnvVar` for its password). A missing or mismatched
variable fails plainly, naming only the variable. Status `ACTIVE` signs and encrypts outbound;
inbound accepts `ACTIVE` and `NEXT` (ours for decryption, the partner's for verification), so a
rollover is: add the new certificate as `NEXT`, tell the partner, then run **Promote Certificate**
(`NEXT` becomes `ACTIVE`, the previous `ACTIVE` of the same partner and usage becomes `RETIRED`).
**Export Public Certificate** downloads the PEM to send to a partner. The monitor alerts at 30, 14
and 7 days before expiry and at expiry for `ACTIVE` and `NEXT` certificates.

**Hook URLs** (outside `/qqq/v1`, authenticated per transport by AS2 identity, signature and
certificate; give your partner the first, and the second is sent in `Receipt-Delivery-Option`):

| URL | Purpose |
| --- | --- |
| `POST {base}/qqq/hooks/edi/as2/{transport}` | Receive messages; answers with the MDN, or 200 and a queued asynchronous MDN |
| `POST {base}/qqq/hooks/edi/as2/{transport}/mdn` | Receive asynchronous MDNs for messages we sent |

`{transport}` is the transport `name`. Set `EdiQBitConfig.withPublicBaseUrl(...)` so `ASYNC`
transports can announce the second URL. Bodies are limited by the largest `maxInterchangeBytes` of
the agreements on the transport (50 MB default). A retransmitted Message-ID is answered with the
original MDN and is not reprocessed; a bad signature, decryption or MIC returns an MDN with
`processed/error: authentication-failed`, `decryption-failed` or `integrity-check-failed` and creates
no inbox row; a storage failure returns `unexpected-processing-error` so the partner retries. Every MDN
sent or received is kept in the read-only `edi_as2_mdn` table (AS2 MDNs child lists on the
outbound message and the inbound message) with the raw MDN in the encrypted payload store.
`ASYNC` messages stay SENT until the MDN arrives; the `edi_as2_mdn_sender` step delivers the MDNs we
owe, retrying with the send backoff, and `edi.mdn.overdue` warns when an `ASYNC` MDN is later than
the agreement document `ackDueMinutes` (60 when unset).

Existing databases need the new `edi_as2_mdn` table, `edi_outbound_message.transport_mic` and
`edi_transport.weak_crypto` (see `docs/ddl/edi-postgres.sql`).

## HIPAA and PHI

Original **QQQ baseline guides** cover 837P (005010X222A1), 837I (005010X223A2),
835 (005010X221A1), 270/271 (005010X279A1), 276/277 (005010X212), 834 (005010X220A1)
and 999 (005010X231A1). These are in-house structure and usage baselines, not licensed TR3s.
No TR3 text or full licensed code lists ship. Hosts may import their licensed guides.

SNIP 1–2 covers envelopes and the active guide. SNIP 3 uses exact decimals for claim charges,
claim/line CAS adjustments and payments less signed PLB adjustments (zero tolerance). SNIP 4
rules run per loop occurrence, including dependent patients, claim/line dates, diagnoses,
subscriber demographics and benefits-or-rejection responses. SNIP 3+ findings produce `E` and
an exception; SNIP 1–2 errors produce `R`. HIPAA replies use 999 when acknowledgement is enabled.
SNIP 5 includes ICD-10 diagnosis shape, NPI Luhn with the 80840 prefix, and US ZIP shape.
`ExternalCodeSetProvider.isValid(codeSet, code, asOf)` supplies licensed membership checks for
EXTERNAL code lists; without it, only formats are checked. The System widget reports that state.

Healthcare agreement documents default to PHI and SNIP 4. Clinical keys go only to
`edi_transaction.phiBusinessKey`; validation excerpts, acknowledgements, events, exceptions,
alerts and process summaries contain no submitted clinical values. Healthcare sample maps and
host tables live **only in test sources/resources** and use seeded synthetic patients and claims.
Their NPI `1234567893` is a checksum test fixture, not an asserted provider identity.

**The host must encrypt the database at rest and should install qbit-secured-data.** Payloads
are always AES-GCM encrypted; PHI columns rely on host database encryption. RC.5's
`EdiClassifications` stand-in marks PHI/PII/CONFIDENTIAL and denies PHI AI exposure, but does not
mask values. Without secured-data policies, a user with table access can read PHI columns.
The home explicitly reports this limitation. Use record-level audits on host PHI target tables
until core classified-value audit protection is available.

RC.5 also logs query criteria verbatim. The marked `PhiLogScope` bridge suppresses synchronous
logger diagnostics during PHI mapping/application and preview; ordinary value-free EDI events
remain persisted. It changes no logger levels and does not suppress other threads. Replace both
bridges with core secured-data support at 4.1.0-RC.6. Map samples use record-level audits to avoid
copying expected-result PHI into audit details. Host code outside these scopes must protect its
own logging, audits and asynchronous work.

Download Raw Document uses a session-bound process grant, rechecks `edi.raw.download` and record
read/partner access at download time, streams decrypted bytes without local files, and records
`RAW_DOWNLOADED` with the user and row references, never content. Download filenames are generic.

`PhiCanaryTest` scans every five-character marker window through gateway/HTTPS intake, forced
SNIP 2/3/4 errors, mapping/apply, 999, replay/resend, Review/Result, alerts, DEBUG logs, all EDI
query exports and all home/child widgets. FULL exports exempt only PHI-classified columns;
RC.5 HIDDEN field-lock exports have no exemptions. MASKED tiers and the optional Quick Search
qbit require the newer secured-data integration and are not claimed by these RC.5 tests.

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
