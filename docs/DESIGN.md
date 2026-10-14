# QBit EDI: design

Status: **Approved** (owner, 2026-10-13: spec first, then build) · drafted 2026-10-13 · Author:
James Maes (drafted with Claude)

`QRun-IO/qbit-edi` (Apache-2.0) is a full EDI application qbit for QQQ. It handles the transports
(AS2, SFTP, VAN mailboxes, HTTPS, email), carries the EDI documents and their implementation guides
as data, runs inbound and outbound flows with 997, 999 and TA1 acknowledgements, and lets each
trading partner's customizations be expressed as versioned data rather than code.

## Owner request (verbatim)

> For EDI, i guess that would be more of a normal application qbit that would just handle all of
> the transport methods (AS2, etc) and then have all of the edi documents built in, with handling
> for edi flows, 997s etc and handling customizations of them, etc.

## Owner decisions

> **Owner decisions (2026-10-12): all recommendations accepted.** Every open question in section 17
> is decided as this spec recommends ("they are approved. we will adjust as needed once we can view
> them working").

**Owner decisions (2026-10-13):**
- Spec first, then build, in the phases of section 16.
- **First documents (X12):**
  - retail and supply chain: 850, 855, 856, 810, 846, 860;
  - warehouse and logistics: 940, 945, 214, 204;
  - healthcare (HIPAA): 837 (P and I), 835, 270/271, 276/277, 834;
  - acknowledgements: 997, 999, TA1.
- **Versions:** X12 004010 and 005010, with the 005010 HIPAA TR3s for healthcare. EDIFACT is
  designed in now and built last.
- **No files or filesystem: just the database and S3** ("there are no files or filesystem - just
  the database and s3"). Raw EDI documents, AS2 messages and MDNs, and certificates live in object
  storage and the database, never on local disk. Private keys come from secrets (environment
  variables). Every service is stateless, so any replica can process any message.
- **Inbound SFTP** comes through qbit-file-gateway (SFTP only in v1, no FTPS, owner decision),
  using its published `api` package (section 5.3).
- **No kof22 adoption.** kof22 does not need EDI. The demo runs in `qqq-sample-project` (section
  15).

## Mandatory standards that shape this design

- Data lives in standard, visible QQQ tables with full record screens, secured by permissions and
  record locks. Tables are `edi_*`; permissions use the full name `edi.*`.
- Processes are table or record actions with plain labels, never in the nav. Record actions use the
  standard streamed-ETL Review and Result summary lines; no custom dry-run forms.
- One app home that owns the EDI data, built from typed widgets (no `RawHTML`, guardrail G5). The
  nav lists only the main tables; sub-tables are child lists on their parents.
- The API matches the admin. Main tables publish ESB table events and are searchable in Quick
  Search.
- UI endpoints are spec'd `MiddlewareSpecExtension`s under `/qqq/v1/qbit/edi/...` with a committed
  fragment and a contract test (G3). Machine endpoints (AS2, HTTPS receive, MDNs) are non-UI routes
  under `/qqq/hooks/edi/...`.
- No dependency on another qbit's internals (G1). EDI depends only on the published `api` packages
  of qbit-object-storage and qbit-file-gateway, both optional. Alerts and notifications go through
  core `QAlerts`.
- No stand-in without `STAND-IN(core=..., removeBy=...)` (G4). Pinned core baseline, no SNAPSHOT on
  `develop` (G7).
- Readable at all widths, light and dark. Real data only on the home.

## 1. Goals and non-goals

**Goals**
1. Exchange X12 with any trading partner over AS2, SFTP, a VAN mailbox, HTTPS or email, configured
   as data per partner.
2. Never lose a document: store the raw bytes first (encrypted), then process from a durable inbox
   with retries and a dead letter. Every step is idempotent and replayable.
3. Validate against implementation guides that are data (segments, elements, usage, conditional
   rules, code lists), per standard and version, with versioned per-partner overrides.
4. Acknowledge automatically (TA1, 997, 999), and track the acknowledgements we expect from
   partners, alerting when they are overdue.
5. Map documents to and from QQQ records with versioned, data-driven maps, and apply inbound
   documents through normal QQQ actions as a service account, so table permissions, validation,
   customizers, audit and ESB events all apply.
6. Make operations visible: a document timeline, search by business keys (PO, ASN, invoice and
   claim numbers), an exception work queue, resend and reprocess, and a home showing volumes,
   overdue acks and errors.
7. Carry PHI safely: classify healthcare content as PHI so the secured-data tiers and the canary
   egress tests apply, and encrypt raw documents at rest.

**Non-goals (v1)**
- A VAN service of our own. We connect to VANs as mailboxes.
- Business-level responses beyond acknowledgements (824 Application Advice, 864 text). The model
  leaves room for them (section 9.6).
- Shipping licensed X12 or TR3 text (section 7.4).
- CAQH CORE real-time 270/271 (SOAP and MIME envelope). Batch 270/271 is in scope; CORE
  connectivity is a later transport plugin (section 5.6).

## 2. Architecture

```
 partner ──AS2──► /qqq/hooks/edi/as2/{transport}      ─┐
 partner ──SFTP─► qbit-file-gateway INBOX ("edi")     ─┤
 VAN mailbox ◄─poll── SFTP poller (scheduled)          ─┤  1. store raw bytes (object storage, encrypted)
 partner ──HTTPS► /qqq/hooks/edi/https/{transport}    ─┤  2. insert edi_inbox row (PENDING), return
 email (qbit-email inbound, later)                    ─┘     (MDN / 200 / FileAcceptance.accepted)
                                                              │
                         edi_inbox worker (scheduled, claims rows, retries, dead letter)
                                                              ▼
   split interchanges ─► envelope check, TA1 ─► partner + agreement lookup ─► duplicate check
        ─► per ST: guide validation (StAEDI + guide rules) ─► 997/999 ─► map ─► apply as service account
        ─► statuses, timeline, exceptions (edi_exception) ─► ack reconciliation for inbound 997/999/TA1

 host record change ─► ESB table event / record-change listener ─► edi_outbound_request (same tx)
        ─► outbound worker: map ─► guide validate ─► envelope (control numbers) ─► store raw
        ─► edi_outbound_message ─► transport send (AS2 / SFTP push / gateway OUTBOX / HTTPS / email)
        ─► MDN or delivery ─► expected ack due date ─► 997/999 reconcile or overdue alert
```

Package layout (`com.kingsrook.qbits.edi`):

| Package | Holds |
|---|---|
| (root) | `EdiQBitProducer`, `EdiQBitConfig`, `EdiRuntime` (the configured services) |
| `metadata` | the only scanned package: tables, PVS, joins, widgets, processes, app |
| `model` | table entities (`EdiPartner`, `EdiAgreement`, ...) |
| `x12` | StAEDI reading and writing, envelopes, separators, control numbers, splitting |
| `guide` | guide model, structural validator, rule engine, SNIP levels, built-in guide loader |
| `ack` | 997, 999 and TA1 builders, inbound ack reconciliation, overdue monitor |
| `map` | map model, path language, transforms, inbound apply, outbound build, sandbox preview |
| `flow` | inbox and outbound workers, state machines, reprocess and resend |
| `transport` | `EdiTransportPlugin` SPI and the plugins: `as2`, `sftp`, `gateway`, `https`, `email` |
| `storage` | `PayloadStore` (object storage with envelope encryption; database fallback) |
| `security` | secrets by environment-variable name, certificates, classification, service session |
| `hooks` | the `/qqq/hooks/edi/...` route provider |
| `uiapi` | spec'd `/qqq/v1/qbit/edi/...` extension (phase f) |
| `edifact` | EDIFACT envelopes and CONTRL (phase g) |

## 3. Platform and dependencies

| Dependency | Scope | Why |
|---|---|---|
| QQQ `4.1.0-RC.5` (then each new RC, G7) | compile | core, `QAlerts.raise/resolve` (RC.5) |
| `qqq-middleware-javalin` | optional | hooks route provider; UI API extension (RC.6) |
| `qqq-esb` | optional | outbound trigger from ESB table events |
| `qqq-backend-module-filesystem` (SFTP) | compile | SFTP push and VAN polling. This is core's SFTP client, the one qbit-sftp-data-integration uses; EDI uses it directly instead of depending on that qbit (G1). Remote SFTP only; nothing local. |
| `io.xlate:staedi` 1.26.x (Apache-2.0) | compile | streaming X12 and EDIFACT reader and writer with schema validation |
| `com.helger.as2:as2-lib` 5.1.x (Apache-2.0) | compile | AS2 send, receive, MDN, S/MIME via BouncyCastle |
| `qbit-object-storage` (its `api` package only) | optional | raw payload storage (section 10) |
| `qbit-file-gateway` (its `api` package only) | optional | inbound SFTP and partner-pull OUTBOX (section 5.3) |

Core features that arrive in RC.6, and how EDI bridges until then:

| Need | Core | Until RC.6 |
|---|---|---|
| `DataClassification` on fields (PHI) | `actions.ai.DataClassification` (#1142) | `EdiClassifications` stand-in, `STAND-IN(core=QRun-IO/qqq#1142, removeBy=4.1.0-RC.6)`, sets the classification reflectively when the class is present |
| User notifications | `QAlerts.notify` (#1125) | alerts only via `QAlerts.raise` (RC.5); notifications added with the RC.6 bump |
| UI API extension point | `MiddlewareSpecExtension` (#1128) | the sandbox is a record action (section 8.3); spec'd endpoints come in phase f |

The file-gateway `api` ships in that qbit's phase (a). Until it is on a published SNAPSHOT, EDI uses
`GatewayStandIn` with the identical shape, marked `STAND-IN(core=QRun-IO/qbit-file-gateway,
removeBy=qbit-file-gateway 0.1.0-RC.1)`.

## 4. Trading partners and agreements

### 4.1 Tables

All tables have `id`, `createDate`, `modifyDate` and, where people edit them, `modifiedBy`; audit is
on (field-level for configuration tables); record screens have sections. SQL table names equal QQQ
table names; fields are camelCase in QQQ and snake_case in SQL. Labels in brackets.

**`edi_partner`** [Trading Partner] (main, nav, Quick Search, ESB events)
- `name` (unique), `code` (short slug, unique), `status` (TESTING, ACTIVE, SUSPENDED, RETIRED),
  `contactName`, `contactEmail`, `contactPhone`, `notes`.
- `defaultTimezone` (for date transforms), `containsPhi` (boolean; set for healthcare partners,
  drives classification defaults and the home's PHI badge).
- Children: identities, agreements, transports, certificates, documents, exceptions.

**`edi_identity`** [EDI Identity] (child of partner; "ours" when `partnerId` is null)
- `partnerId` (null = one of our own identities), `standard` (X12, EDIFACT),
  `interchangeQualifier` (ISA05/07, e.g. `ZZ`, `01`, `12`; EDIFACT UNB 0007),
  `interchangeId` (ISA06/08, padded to 15 on write; UNB 0004/0010), `applicationCode` (GS02/03),
  `description`, `isActive`.
- Unique (standard, interchangeQualifier, interchangeId, applicationCode). Inbound routing matches
  on these.

**`edi_agreement`** [Agreement] (main, nav; child list on partner)
One agreement is one (our identity, partner identity, standard, usage) combination.
- `partnerId`, `localIdentityId`, `partnerIdentityId`, `standard` (X12, EDIFACT), `usageIndicator`
  (T test, P production; ISA15). Test and production are separate agreements, so test traffic never
  touches production records unless the agreement says so.
- `interchangeVersion` (ISA12: `00401`, `00501`), `elementSeparator` (default `*`),
  `segmentTerminator` (default `~`), `componentSeparator` (ISA16, default `:`),
  `repetitionSeparator` (ISA11 for 00501, default `^`), `lineEnding` (NONE, LF, CRLF; outbound).
  Inbound separators are always read from the ISA itself; these settings are for outbound.
- `requestTa1` (ISA14), `sendTa1` (NEVER, ON_REQUEST, ON_ERROR, ALWAYS; default: on request and on
  error).
- `inboundTransportId`, `outboundTransportId` (section 5).
- `controlNumberScope` (AGREEMENT, LOCAL_IDENTITY; default AGREEMENT; section 4.3).
- `duplicateWindowDays` (default 365), `onDuplicate` (REJECT_TA1, IGNORE, PROCESS; default
  REJECT_TA1 with an exception row).
- `maxInterchangeBytes` (default 50 MB), `applyTestDocuments` (boolean, default false: T documents
  are validated and acknowledged but not applied).
- `isActive`, `notes`.

**`edi_agreement_document`** [Agreement Document] (child of agreement)
One row per document, version and direction that the agreement carries.
- `agreementId`, `direction` (INBOUND, OUTBOUND), `transactionSet` (`850`, `837`, ...),
  `functionalIdCode` (GS01: PO, PR, SH, IN, IB, PC, OW, SW, QM, SM, HC, HP, HS, HB, HR, HN, BE, FA),
  `version` (GS08: `004010`, `005010`, `005010X222A1`, ...), `guideId` (a base or override guide),
  `mapId`, `ackExpectation` (NONE, 997, 999), `ackDueMinutes` (default 1440), `ackLevel`
  (GROUP_ONLY, ALL_TRANSACTIONS, ERRORS_ONLY; for the acks we send), `snipLevel` (1-7; section 11;
  default 2 for non-HIPAA, 4 for HIPAA), `containsPhi` (defaulted from the version),
  `targetTableName` (inbound) or `sourceTableName` (outbound), `outboundTrigger` and
  `triggerFilter` (section 9.3), `batchMode` (IMMEDIATE, BATCH; outbound), `batchSchedule` (cron,
  when BATCH), `isActive`.
- Unique (agreementId, direction, transactionSet, version).

**`edi_control_number`** [Control Number] (child of agreement)
- `agreementId` (or `localIdentityId` when the scope is LOCAL_IDENTITY), `level` (ISA, GS),
  `lastValue`, `minValue` (1), `maxValue` (999999999), `wrapAllowed` (default true).
- ST02 is assigned per group (0001 upward), so it needs no row.

### 4.2 Inbound routing
1. Read the ISA: 106 fixed characters; the element separator is character 4, the repetition
   separator ISA11 (00501), the component separator ISA16 and the segment terminator character 106.
2. Find the partner identity by (ISA05, ISA06) and our identity by (ISA07, ISA08), then the agreement
   by both plus ISA15. No match: an `UNKNOWN_PARTNER` exception, and TA1 code `006` or `007` when
   we can address a reply. The raw document is always kept.
3. Per GS, match GS02/GS03 when application codes are configured; per ST, find the agreement
   document by direction INBOUND, ST01 and GS08 (exact, then by the base version `005010`). No
   match: the group is rejected in the 997/999 (AK5/IK5 `R`, "transaction set not supported") and a
   `NOT_CONFIGURED` exception is opened.

### 4.3 Control numbers
- Allocation is an atomic compare-and-set on `edi_control_number` (`UPDATE ... SET last_value = n+1
  WHERE id = ? AND last_value = n`, retried) in its own short transaction, so concurrent replicas
  never hand out the same number. Past `maxValue` it restarts at `minValue` when wrapping is
  allowed; otherwise the outbound fails with a plain error.
- Numbers are allocated when an interchange is enveloped, not when it is sent, so a resend of the
  same interchange keeps its numbers and the partner can detect the duplicate. "Resend As New"
  re-envelopes with new numbers (section 9.5).
- Inbound duplicate detection per agreement, within `duplicateWindowDays`:
  - interchange: the same (sender, receiver, ISA13, ISA15). TA1 note code `025`.
  - group: the same GS06 from the same sender. AK9/IK9 `R`.
  - transaction: the same ST02 within the group. AK5/IK5 `R`.
  - Byte-identical payloads (the same sha256 within the window) are caught earlier, at the inbox,
    and marked `DUPLICATE` without processing.

### 4.4 Acknowledgement expectations and overdue alerts
- Each outbound functional group whose agreement document expects a 997 or 999 gets `ackDueAt`
  (sent time + `ackDueMinutes`).
- The overdue monitor (a scheduled process, every 5 minutes) calls `QAlerts.raise(AlertSignal)`
  with alert type `edi.ack.overdue`, severity WARNING, dedup key `edi_functional_group:<id>`, a
  record reference to the group and a link to the interchange. qbit-alerts delivers it (inbox,
  channels, quiet hours); without qbit-alerts, core's logging sink records it.
- When the ack arrives, `QAlerts.resolve("edi.ack.overdue", dedupKey, note)`.
- A rejecting ack (AK9/IK9 `R`, or partial `P`) raises `edi.ack.rejected` (ERROR) and opens a
  `REJECTED_ACK` exception.
- From RC.6, assignees of exceptions also get a `QAlerts.notify` notification.

## 5. Transports

### 5.1 Model
Transports are data. **`edi_transport`** [Transport] (main, nav):
- `name` (unique slug, used in hook URLs), `type`, `direction` (INBOUND, OUTBOUND, BOTH),
  `partnerId` (null for a shared transport such as a VAN), `isActive`, `notes`.
- Typed settings in sections. Only the section for `type` is shown and validated (a pre-insert and
  pre-update customizer calls the plugin's `validate`):
  - **AS2:** `as2LocalId`, `as2PartnerId`, `as2Url`, `as2Subject`, `signAlgorithm` (SHA256, SHA384,
    SHA512; SHA1 only when the partner requires it, flagged), `encryptAlgorithm` (AES128_CBC,
    AES256_CBC, AES128_GCM, AES256_GCM; 3DES only when required, flagged), `compress`, `mdnMode`
    (SYNC, ASYNC, NONE), `mdnSigned`, `localCertificateId`, `partnerCertificateId`,
    `partnerTlsCertificateId` (optional pinning), `httpAuthUserEnvVar`, `httpAuthPasswordEnvVar`.
  - **SFTP (push and poll):** `sftpHost`, `sftpPort`, `sftpUsername`, `sftpPasswordEnvVar` or
    `sftpPrivateKeyEnvVar`, `sftpHostKeyFingerprint` (required; unknown host keys are refused),
    `sftpOutboundPath`, `sftpInboundPath`, `sftpPollCron`, `sftpAfterPickup` (DELETE, MOVE),
    `sftpArchivePath`, `sftpFilePattern`, `sftpTempSuffix` (upload as `.tmp`, then rename).
  - **File gateway:** `gatewayEndpointName` (the `file_gateway_alias.name`: an INBOX endpoint for
    inbound, an OUTBOX endpoint for partner-pull outbound).
  - **HTTPS:** `httpsUrl`, `httpsAuthType` (NONE, BASIC, BEARER, HMAC, MTLS), `httpsSecretEnvVar`,
    `httpsHeadersJson` (non-secret headers), `httpsInboundAuth` (for `/qqq/hooks/edi/https/{name}`:
    BASIC, BEARER or HMAC, the secret by env var).
  - **Email:** `emailProviderName` (qbit-email), `emailTo`, `emailFromAlias`,
    `emailAttachmentName`.
  - **VAN:** an AS2 or SFTP transport with `isVan` true and `vanName`. One VAN transport serves many
    partners (`partnerId` null); routing is by ISA identities, never by transport.
- Retry policy: `maxAttempts` (default 8), backoff 1m, 5m, 15m, 1h, 3h, 6h, 12h, 24h.

**`edi_certificate`** [Certificate] (main, nav)
- `alias`, `partnerId` (null = ours), `usage` (SIGNING, ENCRYPTION, SIGNING_AND_ENCRYPTION, TLS),
  `certificatePem` (the public certificate only), `subject`, `issuer`, `serialNumber`,
  `fingerprintSha256`, `notBefore`, `notAfter` (all parsed from the PEM on save),
  `privateKeyEnvVar` (ours only: the env var holding a PEM or base64 PKCS#12),
  `privateKeyPasswordEnvVar`, `status` (ACTIVE, NEXT, RETIRED). `NEXT` supports rollover: inbound
  accepts either key, outbound switches when "Promote Certificate" runs.
- Expiry: the monitor raises `edi.certificate.expiring` at 30, 14 and 7 days (dedup per
  certificate and threshold), and `edi.certificate.expired` at expiry.
- Record actions: "Promote Certificate", "Export Public Certificate" (download the PEM to send to a
  partner). Private keys never enter a table, a log or a response.

### 5.2 Plugin SPI

```java
public interface EdiTransportPlugin
{
   String getType();                                            // "AS2", "SFTP", "GATEWAY", "HTTPS", "EMAIL"
   void validate(EdiTransport transport, List<String> errors);   // settings check on save
   EdiSendResult send(EdiOutboundMessage message, PayloadRef payload, EdiTransport transport) throws EdiTransportException;
   default void poll(EdiTransport transport, InboundSink sink) throws EdiTransportException { }   // pull transports
   default EdiTestResult test(EdiTransport transport) { ... }    // the "Test Connection" record action
}
```

- Plugins register in `EdiTransportPluginsMetaData` (a `QSupplementalInstanceMetaData`). The
  producer registers the built-in ones; a host can add its own (e.g. a VAN's proprietary API).
- `EdiSendResult`: SENT (with the transport message id), DELIVERED (sync MDN, or HTTP 2xx where the
  transport confirms delivery), RETRY (with a reason), FAILED (permanent, with a reason).
- `InboundSink.receive(InboundPayload)` stores the bytes and inserts the inbox row (section 9.1).
  Every inbound path, push or pull, ends in this one call.
- **Secrets:** settings name environment variables (`...EnvVar`). `EdiSecrets.resolve(name)` reads
  them at use time; values are never stored, logged, cached beyond the call, or returned by any
  API. Tests replace the resolver. A missing variable fails "Test Connection" and the send with
  "environment variable X is not set" (the name only).

### 5.3 SFTP through qbit-file-gateway (inbound) and partner pull (outbound)
Settled with the qbit-file-gateway agent (its DESIGN.md section "Consumer and outbox API"):
- EDI registers `EdiFileGatewayConsumer` (name `edi`) with
  `FileGatewayConsumersMetaData.ofOrWithNew(qInstance).withConsumer(new QCodeReference(...))`. An
  INBOX endpoint (a `file_gateway_alias` row with `consumer_name = edi`) delivers each uploaded
  file, at least once, after upload close and scan.
- `accept(InboundFile)` runs on the gateway worker as the SFTP account's user (channel `sftp`).
  `InboundFile` carries `uploadJobId`, `endpointName`, `username`, `userId`, `remoteAddress`,
  `path`, `fileName`, `sizeBytes`, `sha256`, `receivedAt`, `storageBucketAlias`, `storageKey` and
  `openStream()` (always from object storage). EDI streams the bytes into its own encrypted payload
  store, inserts an `edi_inbox` row keyed by (`GATEWAY`, `uploadJobId`) with the sha256, and returns
  `FileAcceptance.accepted("edi_inbox:<id>")`; a storage failure returns `retryLater(reason,
  duration)`. It never parses inside `accept()`.
- The inbox row records the endpoint, username, user id, file name and remote address. The partner
  is still determined from the ISA. When the SFTP account is linked to a partner and the ISA sender
  is a different partner, processing stops with a `PARTNER_MISMATCH` exception, so one partner
  cannot send as another.
- Outbound to partners that pull from us: `FileGatewayOutbox.put(endpointName, fileName, stream,
  sizeBytes)` writes to the partner's OUTBOX endpoint and returns an `OutboxFileRef`;
  `onDownloaded(OutboxFileRef)` on our consumer marks the outbound message DELIVERED.
  `FileGatewayOutbox.isAvailable()` false makes a GATEWAY transport fail validation with
  "qbit-file-gateway is not installed".

### 5.4 SFTP push and VAN polling (remote servers)
For partners and VANs that host their own SFTP, EDI connects out with core's SFTP client
(`qqq-backend-module-filesystem`):
- **Push:** write `sftpOutboundPath/<fileName>.tmp`, then rename. File names follow a configurable
  pattern, default `<partnerCode>_<transactionSet>_<isa13>_<yyyyMMddHHmmss>.edi`.
- **Poll:** a scheduled process per transport lists `sftpInboundPath` matching `sftpFilePattern`,
  streams each file straight into `InboundSink` (no local file), and deletes or moves it only after
  the inbox row commits. A crash between commit and delete re-reads the file on the next poll; the
  inbox's sha256 dedupe absorbs it.
- qbit-sftp-data-integration stays the tool for generic file imports and report exports. EDI does
  not depend on it (G1) but uses the same core client, so behaviour and host-key handling match.

### 5.5 AS2 (phase c)
Built on as2-lib (Apache-2.0):
- **Send:** `AS2Client` with partnership settings built from `edi_transport` and `edi_certificate`
  (a custom partnership factory and certificate factory backed by our tables, keys from env vars, no
  keystore file). Sign, then encrypt, optional compression; the `Message-ID` is recorded. Sync MDN:
  verify the signature and the MIC against the sent content; a mismatch or a failed disposition
  fails the send. Async MDN: `Receipt-Delivery-Option` is
  `https://<host>/qqq/hooks/edi/as2/{transport}/mdn`; the message stays SENT until the MDN arrives,
  then becomes DELIVERED or FAILED. An MDN not received within `ackDueMinutes` raises
  `edi_mdn_overdue`.
- **Receive:** `POST /qqq/hooks/edi/as2/{transport}` (a route provider outside `/qqq/v1`). The
  handler reads the body up to the agreement's `maxInterchangeBytes` (in memory up to 8 MB, then
  streamed to object storage, never to a temp file), uses as2-lib's receive helpers to decrypt,
  verify the signature against the partner's certificate, decompress and compute the MIC, then calls
  `InboundSink.receive` and returns the signed sync MDN, or sends the async MDN after commit. A
  message that cannot be stored gets a failure MDN (`processed/error:
  unexpected-processing-error`) so the partner retries.
- The AS2 `Message-ID` is the inbox dedupe key: a retransmitted message gets the original MDN again
  and is not reprocessed.
- MDNs, sent and received, are stored in **`edi_as2_mdn`** [AS2 MDN] (child of the outbound message
  or the inbox row): `direction`, `messageId`, `originalMessageId`, `disposition`, `micAlgorithm`,
  `micValue`, `signed`, `signatureValid`, `payloadRef` (the raw MDN in object storage),
  `receivedAt`.
- TLS: outbound uses the JVM trust store plus optional pinning (`partnerTlsCertificateId`). Inbound
  TLS terminates at the host's ingress.

### 5.6 HTTPS and email
- **HTTPS out:** POST the interchange (`application/edi-x12` or `application/EDIFACT`) with the
  configured auth. 2xx is DELIVERED. Responses are not parsed as acks (acks arrive as documents).
- **HTTPS in:** `POST /qqq/hooks/edi/https/{transport}` with BASIC, BEARER or HMAC (Standard
  Webhooks signatures, verified with core's webhook signing utilities), a body size limit, then
  `InboundSink.receive`.
- **CAQH CORE** (real-time 270/271 and 276/277 over SOAP or MIME) is a later plugin that reuses the
  same parser and map engine with a synchronous reply. Not in v1.
- **Email out:** qbit-email's `EmailService.send` with the interchange as an attachment, category
  TRANSACTIONAL, linked record `edi_outbound_message`. Healthcare documents may only go through a
  provider marked `phi_allowed` and are refused otherwise. EDI calls qbit-email through its
  published API only; until that API ships, this transport is disabled.
- **Email in:** through qbit-email's phase 2 inbound (an `InboundEmailHandler` registered by EDI).
  Designed here, built when qbit-email inbound ships.

## 6. Parsing (X12 and EDIFACT)

- StAEDI (`EDIInputFactory`, `EDIStreamReader`) tokenizes X12 and EDIFACT. Inbox framing buffers
  one interchange at a time, capped at 50 MiB, and each transaction retains a bounded segment list.
- One payload may hold several interchanges; each ISA/IEA becomes an `edi_interchange` row. Each
  ST/SE is buffered as its own segment list (bounded by the guide's maximum segment count, default
  100,000) for validation, ack building and mapping.
- Envelope checks: ISA fixed width, IEA01 = group count, IEA02 = ISA13, GE01 = transaction count,
  GE02 = GS06, SE01 = segment count, SE02 = ST02. Envelope errors produce TA1 or AK9/IK9 codes.
- Character set: the X12 basic and extended character sets, and UTF-8 where the agreement allows it
  (005010 extended). Unrecognised bytes are an envelope error, never silently replaced.
- Writing uses an in-memory serializer with the agreement's separators, fixed-width ISA
  padding, component/repetition handling, and optional line endings.
- EDIFACT is built: StAEDI reads syntax 3/4 UNA/UNB/UNG/UNH envelopes. UNA advice and release
  characters survive round trips; UNT/UNE/UNZ counts and references produce findings. A payload
  may contain both X12 and EDIFACT interchanges. In-house D96A/D01B ORDERS/ORDRSP/DESADV/INVOIC
  and CONTRL guides use the shared composite and loop grammar.
- EDIFACT routing uses the identity's `interchangeId`/`interchangeQualifier` as UNB identifiers;
  UNB11=1 selects test usage. The agreement's syntax identifier, version and service characters
  control output. UNB/UNH control references use the allocator's EDIFACT levels (up to 14 digits).
  CONTRL reconciles outbound status and resolves overdue alerts; rejected responses open
  REJECTED_ACK and raise `edi.ack.rejected`. APERAK remains a later document type.

## 7. Implementation guides as data

### 7.1 Tables
**`edi_guide`** [Implementation Guide] (main, nav)
- `name`, `standard`, `version` (`004010`, `005010`, `005010X222A1`, ...), `transactionSet`,
  `functionalIdCode`, `partnerId` (null = a base guide), `parentGuideId` (an override's base),
  `revision` (increments on activation), `status` (DRAFT, ACTIVE, RETIRED), `source` (BUILT_IN,
  IMPORTED, CUSTOM), `description`, `compiledChecksum`.
- BUILT_IN rows are read-only (a record lock keyed on `source`; only the loader writes them).

**`edi_guide_segment`** [Guide Segment] (child of guide): `guideId`, `sequence`, `loopId` (e.g.
`2000A`, `N1`, `HL`), `parentLoopId`, `loopMaxRepeat`, `segmentId`, `position` (the X12 position,
e.g. `0100`), `usage` (REQUIRED, SITUATIONAL, NOT_USED), `minUse`, `maxUse`, `name`,
`situationalRuleId`, `notes`.

**`edi_guide_element`** [Guide Element] (child of guide segment): `guideSegmentId`, `position` (3
for BEG03), `componentPosition` (null or 1..n), `elementId` (the data element number), `name`,
`dataType` (AN, ID, N0-N9, R, DT, TM, B), `minLength`, `maxLength`, `usage`, `codeListId`,
`fixedValue`, `repeatMax`, `situationalRuleId`.

**`edi_guide_rule`** [Guide Rule] (child of guide)
- X12 syntax notes per segment: `type` PAIRED (P), REQUIRED (R), EXCLUSION (E), CONDITIONAL (C) or
  LIST_CONDITIONAL (L), with the element positions.
- Situational and inter-segment rules: `type` SITUATIONAL with an `expression` in the rule language
  (section 7.3), `snipLevel` (2, 4, 6 or 7), `severity` (ERROR, WARNING), `message`, and the
  997/999 error codes to report.

**`edi_code_list`** [Code List] (main, nav) and **`edi_code_list_value`** [Code] (child): `name`,
`elementId`, `source` (BUILT_IN, CUSTOM, EXTERNAL), `externalCodeSet` (for licensed code sets,
section 11.4); values have `code`, `description`, `isActive`.

### 7.2 Guide validation
StAEDI tokenizes the stream and checks envelope syntax. The guide engine applies the guide data
to each buffered transaction: segment order, nested loops, usage and repetition limits, element
types and lengths, code lists, syntax notes, and situational and inter-segment expressions. Both
JSON resources and database guide revisions use this same in-memory engine; guide validation does
not generate a second StAEDI schema or write files.

- Every finding becomes an `edi_transaction_error` row: segment position, segment id, element
  position, the X12 code for the ack (AK3/AK4 for 997, IK3/IK4/CTX for 999), the SNIP level and a
  plain message. Element values go in `valueExcerpt` only for non-PHI documents (section 12).

### 7.3 Rule language
A deliberately small expression language over the buffered transaction set, evaluated without a
scripting engine:
- paths as in maps (section 8.2): `CLM02`, `2300/CLM05-1`, `HL[HL03=22]/NM1[NM101=IL]/NM109`;
- functions: `present(p)`, `absent(p)`, `count(p)`, `sum(p)`, `eq(a, b)`, `in(p, 'A', 'B')`,
  `matches(p, regex)`, `date(p)`, `and(...)`, `or(...)`, `not(x)`, `when(condition, then)`.
- Example: "if the patient is not the subscriber, the 2000C loop is required" is
  `when(eq(2000B/SBR02, '18'), absent(2000C))` together with its converse.

### 7.4 Built-in guides and licensing
X12 standards and the HIPAA TR3s are copyrighted and licensed by ASC X12. A public Apache-2.0 repo
must not ship their text. So:
- **Built-in guides are authored in-house** for the documents in scope: segment and element
  structure, usage, lengths and the code values the maps use, with our own short names and no copied
  notes, descriptions or complete code lists. They are enough to parse, validate structurally,
  acknowledge and map typical traffic, and are labelled "QQQ baseline guide".
- **Imported guides:** the table action "Import Guide" on `edi_guide` loads a guide that a licensee
  exports from their licensed tools (a documented JSON format, plus an adapter for a common X12
  schema JSON). Imported guides live in the host's database, never in this repo.
- Partner companion-guide differences are **override guides** (section 7.5).
- Built-in guides ship as JSON resources in the jar and are upserted by the loader at startup when
  their checksum changes (as a new revision; the previous revision stays for documents processed
  under it).

### 7.5 Partner overrides
- "Create Partner Override" (record action on a guide) copies the guide into a CUSTOM guide with
  `partnerId` and `parentGuideId`, in DRAFT.
- Edits are ordinary record edits of its segments, elements, rules and code lists.
- "Activate Guide" requires that the guide compiles, increments `revision`, and retires the previous
  ACTIVE revision for that partner. Documents record the guide id and revision they were validated
  with, so reprocessing an old document can use the original or the current revision (a choice on
  the reprocess action).
- "Compare With Base" lists the differences as standard Result lines (segments, elements and rules
  added, removed or changed).

## 8. Maps

### 8.1 Tables
**`edi_map`** [Map] (main, nav)
- `name`, `direction` (INBOUND, OUTBOUND), `standard`, `version`, `transactionSet`, `guideId`,
  `partnerId` (null = shared), `tableName` (the target or source QQQ table), `matchKeyFields`
  (comma list; the inbound upsert key, e.g. `partnerId,poNumber`), `onMatch` (UPDATE, SKIP,
  EXCEPTION), `businessKeyPath`, `businessKeyType`, `secondaryKeyPath`, `revision`, `status`
  (DRAFT, ACTIVE, RETIRED), `parentMapId`, `description`.

**`edi_map_rule`** [Map Rule] (child of map): `sequence`, `ediPath`, `qqqPath` (`field`,
`association.field`), `transform`, `transformArgs`, `required`, `condition` (rule language),
`defaultValue`, `notes`.

**`edi_map_sample`** [Map Sample] (child of map): `name`, `payloadRef` (stored like any payload,
encrypted), `expectedResultJson`, `lastPreviewAt`, `lastPreviewStatus`. Synthetic data only; the
record screen says so, and samples on PHI maps are classified PHI.

### 8.2 Paths and transforms
- **EDI paths:** segment plus element position (`BEG03`), components with `-` (`CLM05-1`), loops by
  id (`2300/CLM01`) or by qualifier predicate (`N1[N101=ST]/N3/N301`), repeats with an index or `*`
  (`PO1[*]/PO102`). Inbound, `*` loops map to associations; outbound, associations drive loops.
- **QQQ paths:** fields and associations of the map's table, checked against the table's metadata
  on activation.
- **Transforms:** `DATE` (CCYYMMDD; YYMMDD with a century window), `TIME` (HHMM, HHMMSS and
  fractions), `DATETIME` (date + time + the partner's timezone), `DECIMAL` (implied decimals for N
  types), `QUALIFIER` (pick by a sibling qualifier), `CODE` (translate through a code list or a
  map-local table), `UNIT` (unit-of-measure conversion, EA, CA, PK, LB, KG, ..., with pack sizes
  from a host field), `CONSTANT`, `CONCAT`, `SPLIT`, `SUBSTRING`, `TRIM`, `UPPER`, `LOOKUP` (find a
  QQQ record id by a key, such as an item by UPC or a ship-to by N104, read as the service session),
  `SEQUENCE` (outbound line numbers), `HL` (outbound HL numbering for 856 hierarchies). Hosts can
  register their own transforms by name.
- A rule failure is a mapping error naming the rule, the EDI path and the QQQ path (with no value
  for PHI documents).

### 8.3 Sandbox (phase a as an action, phase f as UI)
"Preview Map" (record action on `edi_map` and `edi_map_sample`): pick a sample or upload one (the
upload is stored as a sample), and the standard Review/Result lines show:
1. the parse: interchanges, groups and transaction sets found;
2. validation findings by SNIP level;
3. the 997/999 that would be sent;
4. the map result (records and associations that would be written), downloadable as JSON;
5. the diff against the previous map revision's result and against `expectedResultJson`.

Nothing is written to target tables: the preview runs the whole pipeline with a no-op applier.
Phase f adds spec'd endpoints (`POST /qqq/v1/qbit/edi/sandbox/preview` and related) for the mapper
UI.

### 8.4 No-code mapper UI (phase f; specified now, built later)
A custom Next UI page (custom UIs may be nav entries):
- left, the guide tree (loops, segments, elements with usage and code lists); right, the target
  table's fields and associations; drag to create a rule, then choose a transform;
- a live preview pane on a chosen sample (the sandbox endpoint), with errors inline;
- revisions: edit a DRAFT, activate, compare revisions;
- reads and writes only through `/qqq/v1` (CRUD on `edi_map_rule` plus the extension endpoints).

## 9. Flows

### 9.1 Inbox and inbound processing
**`edi_inbox`** [Inbound Message] (child list on the transport and the interchange; linked from the
home):
- `transportId`, `transportType`, `externalId` (AS2 Message-ID, gateway `uploadJobId`, poll path and
  modification time, HTTPS request id, email message id), `payloadRef`, `payloadSha256`,
  `sizeBytes`, `fileName`, `remoteAddress`, `receivedAt`, `status` (PENDING, PROCESSING, PROCESSED,
  RETRY_WAIT, DEAD_LETTER, DUPLICATE, IGNORED), `attempts`, `nextAttemptAt`, `claimedAt`,
  `processedAt`, `lastError` (no payload content), `partnerId` (once known).
- Unique (`transportType`, `externalId`); sha256 duplicates within the window become DUPLICATE.
- The worker (a scheduled process, every 15 seconds, batches of 20) claims rows with a
  compare-and-set on `status` and `claimedAt`. Claims older than 5 minutes are reclaimed, so any
  replica can process and a crashed one loses nothing. Backoff 1m, 5m, 15m, 1h, 6h; then
  DEAD_LETTER, an `edi_inbox_dead_letter` alert and an exception.

Per interchange, group and transaction set (each transaction set in its own database transaction,
so one bad document does not block the others):
1. envelope checks and TA1 (section 6);
2. partner and agreement routing (4.2), duplicate checks (4.3);
3. validation (7.2) at the agreement document's SNIP level;
4. ack building: one 997 or 999 per received group, sent per the agreement (9.4);
5. if the transaction set is accepted (`A` or `E`): map and apply (9.2); a rejected set is not
   applied;
6. statuses, timeline events and exceptions.

### 9.2 Apply as a service account
- The applier runs `InsertAction` and `UpdateAction` (with associations) on the map's table under a
  **service session** from `EdiQBitConfig.withServiceSessionSupplier(Supplier<QSession>)`, which the
  host provides (a user with exactly the permissions EDI should have). Permission enforcement stays
  on, so a map cannot write to a table the service account cannot. When partner locks are on,
  that partner's security key is added to the session for the call.
- Upsert by `matchKeyFields`: query for an existing record as the service session; `onMatch`
  decides. The 860 (PO change) is a map onto the same order table with UPDATE.
- The target table's customizers, validation, audit and ESB events all run, because this is the
  normal action path.
- The target record id is stored on `edi_transaction` (`targetTableName`, `targetRecordId`). A host
  can show a record's EDI documents with a child-list widget on `edi_transaction` filtered by
  target.
- A failure rolls back that transaction set only, sets status EXCEPTION, and opens an `APPLY`
  exception with the error.

### 9.3 Outbound
- **Trigger** (per outbound agreement document, `outboundTrigger`):
  - `ESB_EVENT` (preferred when qqq-esb is configured): the source table publishes ESB table
    events; an `EsbTrigger` subscribed to them runs the process `edi_outbound_enqueue`, which
    applies `triggerFilter` (a QQueryFilter as JSON, e.g. `status = SHIPPED`) and inserts an
    **`edi_outbound_request`** row (`agreementDocumentId`, `sourceTableName`, `sourceRecordId`,
    `eventId`, `status`, `attempts`, `nextAttemptAt`, `claimedAt`, `lastError`). It is idempotent on
    (event id, agreement document).
  - `RECORD_CHANGE` (no broker): a core `RecordChangeListenerInterface` registered by EDI inserts the
    same row inside the writer's transaction (a transactional outbox: the request exists exactly
    when the source change commits).
  - `MANUAL`: the table action "Send As EDI" on the source table, added when the host opts in, with
    the standard Review/Result summary.
- **Outbound worker** (claims requests like the inbox): loads the source record as the service
  session, maps it (section 8), and validates it against the guide (an invalid outbound document is
  never sent; it opens a `VALIDATION` exception). It then batches per agreement (IMMEDIATE or on
  `batchSchedule`), envelopes (control numbers, 4.3), stores the raw interchange and creates an
  **`edi_outbound_message`**.
- **`edi_outbound_message`** [Outbound Message]: `interchangeId`, `transportId`, `payloadRef`,
  `status` (QUEUED, SENDING, SENT, DELIVERED, RETRY_WAIT, FAILED, DEAD_LETTER, CANCELLED),
  `attempts`, `nextAttemptAt`, `transportMessageId`, `sentAt`, `deliveredAt`, `lastError`. The
  sender claims rows the same way and retries per the transport's policy; DEAD_LETTER raises
  `edi.send.failed` and opens a `TRANSPORT` exception.
- **Reconcile:** an MDN or delivery confirmation sets DELIVERED; the expected 997/999 sets the
  functional group's `ackStatus` (4.4); the home and the timeline show both.

### 9.4 Acknowledgements we send
- **TA1:** per the agreement's `sendTa1`, with codes from the envelope checks (e.g. `001` control
  number mismatch, `006`/`007` invalid sender or receiver, `022` invalid control structure, `025`
  duplicate).
- **997** (non-HIPAA, and 004010) and **999** (005010 HIPAA, `005010X231A1`): AK1, AK2, AK3, AK4,
  AK5 and AK9, or IK3, IK4, IK5 and CTX, at the agreement document's `ackLevel`, built from the
  validation findings.
- Acks are outbound interchanges on the same agreement and transport, with their own control
  numbers, and appear as `edi_transaction` rows (997, 999, TA1) linked to what they acknowledge.
- Acks are never themselves acknowledged.

### 9.5 Resend, reprocess, search and timeline
Record actions, all with the standard Review/Result summary:
- **Reprocess** (on `edi_inbox`, `edi_interchange`, `edi_transaction`, `edi_exception`): run again
  from the stored raw payload, from the parse or from the map step, with the original or the current
  guide and map revisions. Idempotent: an applied document is matched by its target record and
  updated or skipped per the map's `onMatch`. Acks are re-sent only when chosen.
- **Resend** (on `edi_outbound_message`, `edi_interchange`): send the same bytes again (the same
  control numbers). **Resend As New**: re-envelope with new control numbers (for partners that
  rejected a duplicate).
- **Cancel** (queued outbound), **Assign**, **Ignore** and **Mark Resolved** (exceptions).
- **Search by business keys:** `edi_transaction.businessKey` (PO, ASN, invoice, load and shipment
  numbers, from the map's `businessKeyPath`, e.g. BEG03, BSN02, BIG02, B204, W0506),
  `businessKeyType`, and `secondaryKey` (e.g. the PO on an 856 or 810). Healthcare keys (claim and
  patient control numbers, member ids) go to `phiBusinessKey` instead (section 12). All are in Quick
  Search; the PHI field matches only for users with FULL access to it (secured-data).
- **Timeline:** `edi_event` [Event] (child of interchange and transaction): `at`, `type` (RECEIVED,
  STORED, PARSED, VALIDATED, ACK_SENT, MAPPED, APPLIED, ENVELOPED, SENT, MDN, DELIVERED,
  ACK_RECEIVED, EXCEPTION, REPROCESSED, RESENT), `detail` (no payload values), `userName`. The
  interchange and transaction screens show it in time order next to the record audit.

### 9.6 Runtime tables
**`edi_interchange`** [Interchange] (main, nav, Quick Search on ISA13, ESB events): `direction`,
`partnerId`, `agreementId`, `transportId`, `inboxId` or `outboundMessageId`, `standard`,
`senderQualifier`, `senderId`, `receiverQualifier`, `receiverId`, `controlNumber` (ISA13),
`interchangeVersion`, `usageIndicator`, `interchangeDate`, `groupCount`, `payloadRef`,
`payloadSha256`, `sizeBytes`, `status` (RECEIVED, ACCEPTED, ACCEPTED_WITH_ERRORS, REJECTED,
DUPLICATE, ENVELOPED, SENT, DELIVERED, FAILED), `ta1Status`, `containsPhi`.

**`edi_functional_group`** [Functional Group] (child of interchange): `functionalIdCode`,
`controlNumber` (GS06), `version` (GS08), `transactionCount`, `ackStatus` (A, E, P, R, NONE,
PENDING), `ackDueAt`, `ackReceivedAt`, `ackTransactionId`.

**`edi_transaction`** [Document] (main, nav, Quick Search, ESB events): `interchangeId`,
`functionalGroupId`, `direction`, `partnerId`, `agreementDocumentId`, `transactionSet`, `version`,
`controlNumber` (ST02), `businessKeyType`, `businessKey`, `secondaryKey`, `phiBusinessKey`,
`status` (RECEIVED, VALID, INVALID, MAPPED, APPLIED, EXCEPTION, QUEUED, SENT, ACKNOWLEDGED,
REJECTED, IGNORED), `ackStatus`, `errorCount`, `warningCount`, `guideId`, `guideRevision`, `mapId`,
`mapRevision`, `targetTableName`, `targetRecordId`, `segmentCount`, `containsPhi`.

**`edi_transaction_error`** [Validation Error] (child of transaction): `segmentPosition`,
`segmentId`, `loopId`, `elementPosition`, `componentPosition`, `snipLevel`, `ackCode`, `severity`,
`message`, `valueExcerpt` (non-PHI documents only).

**`edi_exception`** [Exception] (main, nav; the work queue): `type` (UNKNOWN_PARTNER,
PARTNER_MISMATCH, NOT_CONFIGURED, DUPLICATE, ENVELOPE, VALIDATION, MAPPING, APPLY, TRANSPORT,
REJECTED_ACK, OVERDUE_ACK, DEAD_LETTER), `status` (OPEN, IN_PROGRESS, RESOLVED, IGNORED),
`partnerId`, `inboxId`, `interchangeId`, `transactionId`, `outboundMessageId`, `summary`, `detail`
(no payload values), `assignedToUserId`, `resolvedBy`, `resolvedAt`, `resolution`, `occurrences`
(repeated failures of the same item increment it instead of adding rows).

The 824 Application Advice and the 864 are later document types on the same model (outbound
documents generated from an exception), not new tables.

## 10. Payload storage (database and S3 only)

- **`PayloadStore`** writes raw payloads (inbound files, outbound interchanges, AS2 messages and
  MDNs, samples) to qbit-object-storage: the bucket alias from
  `EdiQBitConfig.withPayloadBucketAlias` (default `edi`), key
  `edi/<yyyy>/<MM>/<dd>/<kind>/<uuid>`, through `ObjectStorage.asSystem("edi", runId)`.
- **Envelope encryption, always:** each payload is encrypted with AES-256-GCM under a random data
  key; the data key is wrapped with the master key from env var `QQQ_EDI_PAYLOAD_KEY` (base64, 32
  bytes) and stored with its key id in the object's metadata. `QQQ_EDI_PAYLOAD_KEY_PREVIOUS` allows
  rotation (read with either, write with the current one); the table action "Rewrap Payloads"
  re-wraps old payloads. Bucket-side SSE is recommended as well, not relied on.
- `payloadRef` columns hold `objectstorage:<bucket>:<key>`; the plaintext sha256 is stored next to
  it.
- **Database fallback:** without qbit-object-storage, payloads go to **`edi_payload`** (`kind`,
  `ciphertext` BLOB, `keyId`, `sha256`, `sizeBytes`, `createDate`), encrypted the same way. It suits
  the demo and tests; production should use object storage (startup logs a WARN, and the home
  shows it).
- Nothing touches local disk: parsing streams from the store, AS2 uses memory and object storage,
  and certificates come from table rows and env vars. A test asserts that a full round trip creates
  no file under the JVM temp directory.
- **Retention** (`EdiQBitConfig.withRetention...`; the nightly table action "Purge Payloads"): raw
  payloads are kept 7 years for healthcare (HIPAA documentation retention is 6 years) and 2 years
  otherwise, configurable. Rows and their sha256 stay after the payload is purged. The home shows the
  policy.

## 11. Validation levels (SNIP) and code sets

### 11.1 What each level checks and what we support
| SNIP | Meaning | Support |
|---|---|---|
| 1 | EDI syntax integrity: envelopes, segment and element syntax, separators | **Full** (StAEDI and the envelope checks) |
| 2 | HIPAA syntax requirements: TR3 usage (R/S/N), repeats, lengths, TR3 code values | **Full** for the rules in the active guide (the built-in baseline or an imported TR3) |
| 3 | Balancing: totals across segments (837 CLM02 equals the sum of the SV1/SV2 line charges; 835 BPR02 equals the sum of CLP04 less PLB adjustments; claim and line adjustments balance) | **Full** for 837P, 837I and 835 (coded checks) |
| 4 | Situational inter-segment rules ("if X then Y") | **Full** through guide rules (7.3); baseline guides carry the common ones, imported TR3s the rest |
| 5 | External code sets: ICD-10, CPT, HCPCS, NDC, CARC/RARC, taxonomy, ZIP | **Partial:** a pluggable `ExternalCodeSetProvider` (11.4); no licensed code set ships (CPT is AMA-licensed). Format checks (ICD-10 shape, NPI check digit, ZIP format) are built in. |
| 6 | Product type or line of business (ambulance, chiropractic, ...) | **Through override guides and rules**; no built-in content |
| 7 | Trading-partner-specific requirements (companion guides) | **Through partner override guides** (7.5) |

### 11.2 Findings and acks
SNIP 1-2 errors reject the transaction set (AK5/IK5 `R`) unless the agreement document downgrades a
rule to WARNING. SNIP 3-7 findings default to `E` (accepted with errors) plus an exception, because
business rejection belongs in an 824 or a 277CA, not in a 999. Each rule can set its severity.

### 11.3 Defaults
Non-HIPAA documents validate at level 2 by default and HIPAA documents at level 4. An agreement
document can raise or lower its level.

### 11.4 External code sets
`ExternalCodeSetProvider` (SPI: `boolean isValid(String codeSet, String code, LocalDate asOf)`),
registered by the host, for example backed by a licensed table the host loads. Code lists with
`source = EXTERNAL` delegate to it. Without a provider they check the format only, and the home says
so.

## 12. HIPAA and PHI

- **Classification** (`DataClassification`, core #1142; the stand-in until RC.6):
  - PHI: `edi_transaction.phiBusinessKey`, `edi_transaction_error.valueExcerpt` (never written for
    PHI documents, but classified anyway), `edi_map_sample.expectedResultJson`, `edi_inbox.fileName`
    (file names often carry member or claim ids), and the payloads themselves (encrypted, never in a
    table column).
  - PII: partner contact fields. CONFIDENTIAL: everything else in the `edi_` tables.
  - Host target tables classify their own fields. The map writes through normal actions, so their
    classification applies to what it writes.
- With qbit-secured-data installed, PHI fields follow its tiers: a customer-service user sees
  `phiBusinessKey` masked and can find a claim by exact match only (REVEALABLE), which is how claim
  lookup is meant to work for non-FULL users. Without it, the fields are FULL to anyone with the
  table permission, so a host carrying PHI should install it (the home warns).
- **No PHI outside the encrypted payloads and PHI-classified fields:** for documents with
  `containsPhi`, errors, events, exceptions, alert texts, log lines and Result lines carry segment
  and element positions and codes, never values. The 999 we send carries positions and element
  references only.
- **Encryption at rest:** payloads always (section 10). PHI columns rely on the database's
  encryption at rest (a host responsibility, stated in the README) plus secured-data masking.
- **Minimum necessary:** PHI maps write only the mapped fields. The raw document stays encrypted and
  can be downloaded only with the record action "Download Raw Document" (permission
  `edi.raw.download`), which is audited and, with secured-data, written to its access log.
- **Canary egress tests** (phase e, the core SECURED-DATA procedure): synthetic 837P and 835
  documents carry `CANARY-PHI-<random>` in the patient name, member id, claim number and a
  diagnosis. They run through receive, validate (with forced errors), 999, map, apply, exception,
  reprocess, resend, the home widgets, exports of every `edi_` table, Quick Search, alerts and
  captured logs, as a FULL, a MASKED and a HIDDEN user. The test fails on any substring of the canary
  longer than 4 characters outside the encrypted payloads and the PHI-classified columns, and for the
  MASKED and HIDDEN users anywhere at all.
- No real PHI anywhere in the repo: samples are synthetic, produced by a seeded generator.

## 13. App home and navigation

App `edi` [EDI], optionally under the host's parent app. Nav (main tables only):
- **Activity:** Documents (`edi_transaction`), Interchanges, Exceptions.
- **Setup:** Trading Partners, Agreements, Transports, Certificates, Implementation Guides, Code
  Lists, Maps.

Sub-tables (identities, agreement documents, control numbers, guide segments, elements and rules,
code list values, map rules and samples, functional groups, validation errors, events, inbox,
outbound requests and messages, MDNs) are child lists on their parents.

**Home** (typed widgets only, no HTML; every figure links to the filtered table):
1. Statistics: documents in and out today and over 7 days, open exceptions, overdue acks, dead
   letters, certificates expiring within 30 days.
2. Table: volume by partner and document type over 7 days (in, out, rejected), each cell linking to
   `edi_transaction` filtered.
3. Table: overdue acknowledgements (partner, group, sent, due, age).
4. Table: recent errors (validation and transport, last 24 hours).
5. Child-record list: the exception queue (OPEN and IN_PROGRESS, oldest first), with Assign and
   Reprocess actions.
6. Statistics: system state (payload store: object storage or the database fallback; secured-data
   present; external code-set provider present; inbox backlog and the oldest pending age).

All counts run as the viewing user, so permissions and partner record locks apply. Real data only.

## 14. Security and permissions

- Table permissions use the full name: `edi.partner.read|insert|edit|delete`, and likewise for each
  table (`edi.agreement.*`, `edi.transport.*`, `edi.certificate.*`, `edi.guide.*`, `edi.map.*`,
  `edi.interchange.read`, `edi.transaction.read`, `edi.exception.read|edit`, ...). Runtime tables
  are read-only to people (no insert or delete permission is offered; the qbit writes them as the
  system).
- Process permissions: `edi.reprocess`, `edi.resend`, `edi.exception.assign`, `edi.guide.activate`,
  `edi.guide.import`, `edi.map.activate`, `edi.map.preview`, `edi.certificate.promote`,
  `edi.transport.test`, `edi.raw.download`, `edi.payload.purge`, `edi.admin`.
- **Partner record locks** (optional, `EdiQBitConfig.withPartnerLocks(true)`): every partner-scoped
  table carries a READ_AND_WRITE lock on `partnerId` with the security key type `edi_partner` and the
  all-access key `edi.admin`, so a host can limit operators to some partners. The service session
  gets the needed partner key per call.
- Hooks authenticate per transport (AS2 signature and certificate, HTTPS auth), not with user
  sessions; they run as the system only to store and enqueue.
- Client channels: the producer registers its own denies (architecture review F17): the assistant
  channel is denied `edi_payload`, `edi_certificate` and `edi_inbox`, and the PHI fields follow
  secured-data.

## 15. Demo

Phase b includes a test-scope `EdiDemoServer` with the Next UI and synthetic H2 seed;
see [DEMO.md](DEMO.md) for launch and acceptance details. The broader host demo below remains
a deployment example.

No kof22 adoption. The demo is a module in `qqq-sample-project` (in QRun-IO/qqq): a small orders,
shipments and invoices schema; a retailer partner over the file gateway or SFTP and a 3PL over AS2
against a local as2-lib peer; seeded synthetic documents; and a healthcare clearinghouse partner with
synthetic 837/835 for the PHI walkthrough. It runs on H2 with the database payload fallback, or with
MinIO for object storage.

## 16. Phases and acceptance

| Phase | Scope | Acceptance |
|---|---|---|
| a | Partners, identities, agreements, agreement documents, control numbers, transports (model), payload store, inbox and worker, X12 parse and envelopes, guide model, compiler and rule engine, built-in guides for 850 (004010, 005010) and the acks, 997/999/TA1 generation, inbound SFTP through the file gateway (stand-in until its api ships), exceptions, events, home v1, Preview Map | An 850 from a gateway upload becomes an applied record and a 997; TA1 and 997 for envelope and syntax errors; duplicates caught at every level; control numbers unique under 8 concurrent allocators; every table has a record screen; home counts are real; `./mvnw verify` green |
| b | Retail 850, 855, 856, 810, 846, 860 (004010, 005010) guides and maps; outbound (ESB and record-change triggers, batching, envelope, send); SFTP push and VAN polling; gateway OUTBOX; HTTPS in and out; inbound ack reconciliation; overdue-ack alerts; resend, reprocess, cancel | 855/856/810 built from host records, validated, enveloped and sent; a 997 reconciles and resolves the overdue alert, and a missing one raises it; resend keeps control numbers, resend-as-new does not |
| c | AS2 send and receive, sync and async MDN, certificates and rollover, expiry alerts, VAN over AS2 | Round trip against a local as2-lib peer: signed and encrypted, sync and async MDN, MIC verified, a bad signature refused, a retransmit gets the original MDN |
| d | Logistics 940, 945, 214, 204 guides and maps | Round trips per document |
| e | Healthcare 837P, 837I, 835, 270/271, 276/277, 834 (005010 TR3 baselines), 999, SNIP 3-4, PHI classification, canary egress tests; the email transport when qbit-email's API ships | 837 batches validated to SNIP 4 with balancing errors reported; correct 999s; canary test green |
| f | Sandbox endpoints and the mapper UI (UI API extension and a Next page) | Fragment committed and contract-tested; the mapper edits a map and previews a sample |
| g | EDIFACT (UNA/UNB/UNH, ORDERS, ORDRSP, DESADV, INVOIC, CONTRL) | EDIFACT round trips and CONTRL |

Each phase is one or more PRs into `develop`, with tests and, for UI-visible changes, screenshots of
the home and key records in light and dark at 390 and 1440 px.

### 16.1 Tests (all phases)
- Synthetic sample documents per type and version from a seeded generator; no real data.
- Round trips: parse, map and apply, then build the outbound document from the applied record and
  compare.
- 997, 999 and TA1 generation against expected acks for crafted errors at every level.
- AS2 against a local as2-lib peer, in-process, in both directions.
- Control-number concurrency, wrap and duplicate detection.
- Partner overrides change validation outcomes as intended; map revisions and the sandbox diff.
- No local files during a full round trip; no secret values in logs or responses.
- Canary PHI egress (phase e).

## 17. Open questions (decided as recommended, 2026-10-12)

| # | Question | Recommendation (accepted) |
|---|---|---|
| Q1 | Ship full X12 or TR3 guide content? | No. In-house baseline guides plus an "Import Guide" action for licensed content (7.4). |
| Q2 | Use qbit-sftp-data-integration for SFTP push and polling? | No code dependency (G1); use core's SFTP client directly, the same one it uses (5.4). |
| Q3 | Where do raw documents live without object storage? | Encrypted in `edi_payload` in the database, for the demo and tests; production uses object storage (10). |
| Q4 | Outbound trigger: ESB or a record-change listener? | Both: the ESB trigger when a broker is configured, otherwise the in-transaction record-change listener; MANUAL for one-offs (9.3). |
| Q5 | Which user applies inbound documents? | A host-supplied service session with enforcement on (9.2). |
| Q6 | Test (ISA15 = T) documents? | Separate agreements; validated and acknowledged, applied only with `applyTestDocuments` (4.1). |
| Q7 | Business rejections (824, 277CA)? | Later document types on the same model; v1 uses exceptions plus `E` acks (11.2). |
| Q8 | Are PHI business keys searchable? | Yes, in a PHI-classified field, by exact match for REVEALABLE users through secured-data (12). |
| Q9 | Payload retention? | 7 years for healthcare, 2 years otherwise, configurable (10). |
| Q10 | SHA-1 and 3DES for AS2? | Only when a partner requires them, flagged on the transport and the home (5.1). |
| Q11 | Partner record locks? | Optional (`withPartnerLocks`), off by default (14). |
| Q12 | Demo host? | `qqq-sample-project`, not kof22 (15). |

## 18. Owner steps (outside code)
- Protect `develop` and make `test_only` required (the coordinator).
- For production use: set `QQQ_EDI_PAYLOAD_KEY`, an object-storage bucket alias `edi`, the service
  user for the applier, AS2 keys and passwords as environment variables, and load licensed TR3
  guides or code sets if healthcare validation beyond the baseline is needed.


## Phase a2 implementation notes (2026-10-13)

The inbound pipeline, map/apply service boundary, typed app/home, actions, gateway stand-in and
queued acknowledgements are implemented. These RC.5 adaptations refine the executable contract:

- Core alert metadata rejects underscore names: use `edi.inbox.dead-letter`, `edi.ack.overdue`
  and `edi.ack.rejected`.
- Queued replies emit `ACK_QUEUED`; actual sending remains phase b.
- `inboundPosition` on interchange/group/transaction distinguishes repeated controls within
  a payload and makes retries resume the same occurrence.
- Reprocessing a descendant restarts its containing inbox using original/current definitions,
  stable checkpoints and idempotent reply queuing; live claims cannot be reset.
- Targets and associations must share the EDI RDBMS backend for atomic ST application.
  Cross-backend application fails as APPLY.
- RC.5 binds UTC LocalDateTime. DATE_TIME DDL uses zone-less UTC TIMESTAMP to prevent
  host-timezone shifts in leases and due times. Existing timestamp-with-time-zone columns
  require migration preserving UTC instant values.
- Typed TABLE supports a table-level link rather than individual count-cell URLs. Volume/queue
  tables use filtered-table links; STATISTICS links each figure. No HTML widgets are used.
- `edi_preview_download` is a storage-only metadata alias bridging standard streams to encrypted
  PayloadStore. It has no SQL table. Hosts configure it as the Javalin upload archive and avoid
  multipart disk staging. Samples are synthetic only; downloads require session-registered refs.
- Preview compares structured before/after JSON against expected output and the previous revision.
  PHI map results and acknowledgement values are suppressed. Selection/upload use the standard Review.
- [MAPS.md](MAPS.md) defines transform arguments. QUALIFIER selects via path predicates; CODE uses
  map-local translations; UNIT uses configured factors/pack sizes; DATETIME uses an explicit timezone.
  Host code-list/pack-field conversions use registered transforms. SEQUENCE/HL remain phase b.
- Both payload adapters buffer complete envelopes in memory and use no runtime filesystem storage.
  Object storage uses only its published API.
- Standard optional ESB publication follows RC.5. Document events use the core envelope/publisher
  with ID/status only because the standard factory copies every field, including PHI keys.
- The a1 classification stand-in and host responsibility for PHI field protection remain unchanged.

Synthetic H2 verification covers permissions, association rollback, retry/stale claims, gateway
storage failures, original/current replay, 997/999, ack reconciliation, previews and home renderers.
The object-storage facade is final with restricted constructors: key/encryption metadata are tested
without provider internals. Live S3, deployed gateway/scheduler and light/dark browser layout remain
deployment validation. Outbound messages intentionally stay QUEUED.


## Phase b implementation notes (2026-10-13)

RC.5 adaptations implemented in this worktree:

- Record-change listeners are registered in instance metadata and receive the source writer's transaction. Successful enqueues participate in that transaction (rollback is tested). RC.5 catches listener exceptions, so a failed enqueue cannot veto a source write; hosts requiring strict delivery guarantees must use the explicit enqueue boundary in their transaction.
- RC.5 ESB consumption requires JMS. `edi_outbound_enqueue` accepts `sourceTableName`, `sourceRecordId`, and `eventId`; a host EsbTrigger can invoke that process. Tests exercise that boundary without a broker.
- The filesystem module supplies Apache MINA SSHD. SFTP uses its streaming client directly with mandatory pinned host-key verification, remote temporary upload/rename, and durable inbox receipt before remote cleanup. No runtime local files are created.
- HTTPS uses core `WebhookSigner`/`WebhookVerifier` for Standard Webhooks HMAC. The optional Javalin provider exposes the non-UI `/qqq/hooks/edi/https/{transport}` endpoint with a 50 MiB limit.
- Worker claims use database row locks within the processing transaction. A crashed worker rolls back its claim; remote-send ambiguity remains at-least-once. Batch schedules are installed from active agreement documents during runtime initialization, with up to 100 transactions from one agreement document per envelope. Schedule changes require host scheduler refresh/restart. SFTP poll schedules are installed the same way.
- Acknowledgement deadlines start on the first successful send. Resend reuses the archived bytes; Resend As New re-envelopes the archive with new ISA/GS controls and linked tracking snapshots, without remapping changed source records.
- The test-scope demo uses `QApplicationJavalinServer`, Next UI 1.0.0-RC.14, fresh in-memory H2, anonymous demo authentication, synthetic certificates generated in memory, and an in-memory gateway OUTBOX provider. See `DEMO.md`. A host scheduler and production transport credentials remain host responsibilities.


## Phase d implementation notes (2026-10-14)

- Guides for 940, 945, 214 and 204 are in-house baselines in both releases, with the same structure in 004010 and 005010 (as in phase b). Code lists carry only the values the sample maps and tests use; ID elements without a list are unconstrained. Control totals (940 W7601 against the W0101 lines, 945 W0301 against the W1203 lines) and the 204 stop rules (first stop LD, at least one UL) are guide-level `SITUATIONAL` rules, because the rule evaluator reads whole-document values; per-segment relationships use syntax notes (AT7 status/reason and date/time pairing, W12 difference needs ordered and shipped) and the MS1 segment `condition`, which is evaluated per LX occurrence.
- The 204 stop-level N1 loop has its own loop id (`S5N1`, parent `S5`) so it does not merge with the header N1 loop; map paths name it (`S5[*]/S5N1/N102`).
- Mapping needed two small engine additions, documented in `MAPS.md`: inbound paths may join elements with `+` so `DATETIME` receives a date and a time from separate elements, and outbound `DATEPART`/`TIMEPART` split a QQQ timestamp. Optional inbound DECIMAL/DATETIME rules use a `condition` because a transform with no input still fails the rule (unchanged phase a behavior).
- `InboundApplier` assumed every target table has associations; a flat table (the 214 status sample) hit a null pointer. It is now null-safe.
- The 214 sample map records the first LX event per transaction into a flat status table and always inserts (no match key), so repeated statuses form a history; a host that wants every LX event maps an association.
- Demo: Harbor Logistics gets a gateway OUTBOX transport for 945s (the partner pulls over SFTP, per 5.3) after the retail seed has run, so its earlier acknowledgements keep their visible retries; the fourth partner Ridgeline Freight is added, so `EdiDemoServerTest` now expects four partners.


## Phase g implementation notes (2026-10-14)

- Outbound EDIFACT is ungrouped. Readers/writers preserve optional UNG/UNE groups. Ungrouped
  interchanges have a logical `edi_functional_group` tracking row, keyed by UNB reference, so
  the existing send-time deadline/overdue monitor also covers CONTRL. `groupCount` remains zero
  for those wire envelopes. GROUP_ONLY means an interchange-level CONTRL in this case.
- Payload framing buffers one interchange (hard cap 50 MiB) before StAEDI; retained message
  segments respect `maxSegmentsPerTransaction`. It uses no filesystem. This follows the existing
  buffered storage adapters rather than the original bounded-memory streaming aspiration.
- Retail SG labels are local guide identifiers shared by the two baseline versions, as documented
  in GUIDE-FORMAT.md; the baselines are deliberately small, not complete directory content.
- Existing databases require the three agreement columns listed in README.md. Existing X12
  separator defaults remain unchanged; EDIFACT agreements explicitly select their outbound UNA
  characters. CONTRL action 8 is receipt-only and does not clear a functional acknowledgement deadline.
