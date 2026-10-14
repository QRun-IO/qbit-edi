# Changelog

Following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Phase f1 `edi` 1.0.0 spec'd UI API: guide tree, map target/transform metadata, encrypted base64 sample preview with unsaved rule overrides, and per-rule validation under `/qqq/v1/qbit/edi`.
- Versioned `Edi...V1` schemas and generated OpenAPI fragment with the RC.6 contract/compatibility checks; real HTTP strict-mode permission, PHI, upload, isolation and validation tests.
- Shared sandbox/record-action preview service, current/original guide selection, caller-session transforms, structured revision/expected diffs and value-free PHI diagnostics.
- Transform registry argument schemas, including host-supplied schemas. Preview Map record actions now enforce the documented exact `edi.map.preview` permission through core's full-name permission checker.

- Phase g EDIFACT syntax 3/4 envelopes (UNA/UNB, optional UNG/UNE, UNH/UNT, UNZ), release escaping, native envelope findings, UNB identity routing, 14-digit UN control allocation and duplicate detection.
- Original D96A/D01B ORDERS, ORDRSP, DESADV, INVOIC and CONTRL guides; ORDERS inbound and retail response outbound sample maps with composite fields and segment groups.
- Standard-neutral document validation/mapping, mixed X12/EDIFACT inbox framing, CONTRL generation and reconciliation with overdue resolution and rejection alerts, native outbound envelopes and archived resend-as-new.
- Lindqvist Retail AB demo partner with ORDERS application, ORDRSP/DESADV/INVOIC delivery and CONTRL in both directions; EDIFACT map preview and HTTPS media type.
- Phase e original HIPAA baselines for 837P/I, 835, 270/271, 276/277 and 834; exact SNIP 3 balancing, occurrence-scoped SNIP 4 rules, SNIP 5 formats and a host `ExternalCodeSetProvider` SPI.
- Test-only healthcare maps, classified sample tables, seeded synthetic fixtures, Cedar Health Clearinghouse demo traffic and a PHI canary scanner covering both ingress paths, 999s, apply/replay/resend, widgets, query exports, alerts and logs.
- Permission-checked, session-bound Download Raw Document actions for inbox/interchange rows, with a value-free user audit event.

- Phase d warehouse and logistics baseline guides for 940, 945, 214 and 204 in 004010 and 005010 (syntax notes, situational control-total and stop rules, AT7 pairing), inbound/outbound sample maps (940 in, 945 out/in, 214 in, 204 in/out), DATEPART/TIMEPART outbound transforms and `+`-joined inbound paths for date/time pairs, and Harbor Logistics and Ridgeline Freight demo traffic.

- Phase c AS2: `As2Transport` send and receive (RFC 4130) with S/MIME signing (SHA-256/384/512; SHA1 flagged), encryption (AES-128/256 CBC or GCM; 3DES flagged), optional compression, synchronous and asynchronous signed MDNs with MIC verification, Test Connection, optional TLS pinning and VAN transports with no partner. Built on as2-lib 5.1.8 and BouncyCastle, entirely in memory (no keystore or temporary files).
- `POST /qqq/hooks/edi/as2/{transport}` and `/{transport}/mdn` route provider: identity, decryption (ACTIVE or NEXT keys), signature and MIC checks, Message-ID dedupe returning the original MDN, error MDNs with no inbox row, and a queued, retried asynchronous MDN sender.
- Read-only `edi_as2_mdn` table with "AS2 MDNs" child lists on outbound and inbound messages; raw MDNs in the encrypted payload store; `edi_outbound_message.transport_mic`; `edi_transport.weak_crypto`; `EdiQBitConfig.publicBaseUrl`.
- Certificates: parsed on save (private keys refused), keys from environment PEM PKCS#8 or base64 PKCS#12, ACTIVE/NEXT rollover, record actions Promote Certificate and Export Public Certificate, expiry alerts for ACTIVE and NEXT, and the `edi.mdn.overdue` alert.
- Demo: Pacific Wholesale over AS2 against an in-process BouncyCastle peer with in-memory certificates; AS2 acceptance tests in both directions against our own route and an independent peer.
- Phase b retail baseline guides for 855/856/810/846/860 in 004010 and 005010; inbound/outbound sample maps and nested outbound associations with SEQUENCE/HL transforms.
- Transactional RECORD_CHANGE and manual enqueue, host-wired ESB process, batch schedules, validated encrypted envelopes, send workers, retries and dead-letter alerts.
- Instance transport registry, MINA SFTP streaming push/poll with pinned host keys, gateway OUTBOX stand-in/download delivery, and authenticated HTTPS ingress/egress using core Standard Webhooks.
- Ack and certificate monitoring, functional-ack exception resolution, resend/re-envelope/cancel actions, transport tests and outbound queue home statistics.
- Test-scope H2/Next UI demo with synthetic retail traffic, in-memory gateway and public self-signed certificates; local HTTP/SFTP and application acceptance tests.

- Phase a2 inbound vertical slice: gateway intake, fenced inbox claims/retries, dead letters and core alerts; routing, validation, duplicates, safe events and exception occurrence counts.
- Versioned inbound maps, wildcard associations, transforms, permission-enforced transactional upsert and original/current revision replay.
- Queued enveloped TA1/997/999 and inbound acknowledgement reconciliation; outbound request/message tables for phase b.
- Public-API-only encrypted object-storage adapter and marked file-gateway contract stand-in.
- EDI app, typed home with permission-scoped counts, main-table search metadata, optional ESB events with identifier-only document payloads, and standard Review/Result actions.
- Synthetic sample upload, preview comparisons and encrypted JSON downloads; guide overrides and guide/map activation.
- Contingent possible values on existing/new tables, including actual filtered-search tests.
- PostgreSQL schema additions and UTC zone-less timestamps matching RC.5 RDBMS bindings.
- Phase a1 data model: 21 annotated entities, QQQ tables, enum/table value sources, joins, child lists, audit, permissions, partner locks, and PostgreSQL DDL with metadata-derived H2 integration tests.
- StAEDI 1.26.4 X12 streaming envelopes, separator detection, bounded transaction buffers, structured envelope findings, and fixed-width ISA serialization.
- Original 850/997/999 baseline guides, versioned database loading, SNIP 1–2 validation, syntax notes, and the documented situational rule language.
- TA1, 997 and 999 builders with acknowledgement levels, positional situational context, reply envelopes, and inbound acknowledgement parsing.
- Atomic RDBMS control-number allocation with first-use creation, wrap/exhaustion handling, and interchange/group/transaction duplicate detection.
- Database payload storage with AES-256-GCM envelope encryption, SHA-256 integrity metadata, key rotation, and replaceable environment-secret resolution.
- Per-instance runtime services and configuration, synthetic generators, exact acknowledgement fixtures, concurrent SQL tests, and payload tests that verify no temporary files are created.
- Repository scaffold to the QRun qbit repository standard: `qbit-build-parent` 2.0.0, QQQ 4.1.0-RC.5, Maven wrapper, Checkstyle, JaCoCo gate, CircleCI qqq-orb 0.6.8, release-candidate workflow, Dependabot, community files.
- `EdiQBitProducer` and `EdiQBitConfig` (table prefix `edi_`, permission base `edi`).

### Changed

- QQQ baseline is now 4.1.0-RC.6. EDI tables/fields and healthcare test fixtures use core `DataClassification` through `AiTableMetaData`/`AiFieldMetaData`, preserving PHI, PII, CONFIDENTIAL and PHI AI-exposure restrictions.
- Exception Assign and new/reopened partner rejection/dead-letter exceptions send value-free core notifications after commit, with capturing-sink and PHI canary coverage.
- Retain `PhiLogScope` for core #1147 until RC.7: RC.6 still logs RDBMS query filters and has no redaction hook or `FieldAccessHelper`. Contingent dependency-behavior APIs are also absent; shared-choice filters remain tested.

### Removed

- Reflective `EdiClassifications` RC.5 stand-in. File-gateway stand-ins remain until its API is published; the object-storage API integration is unchanged.

### Fixed

- Shared-choice filters group partner-or-shared alternatives beneath an AND root, so core search criteria cannot admit another partner's transports.

- 999 now accepts SNIP 3+ findings with `E` instead of rejecting every ERROR. HIPAA acknowledgement selection follows the implementation version and enabled agreement expectation; outbound HIPAA ST carries its implementation identifier.
- Qualified HL/NM1 anchors distinguish healthcare hierarchy levels; repeated loop names under different parents retain separate occurrences. Situational rules inherit direct ancestor segments without borrowing values from sibling occurrences.
- Review/Result records expose identifiers only. A marked RC.5 PHI logging bridge prevents core upsert query diagnostics from copying claim/member values into logs.

- Inbound apply no longer fails with a null-pointer error on a target table that has no associations (flat tables such as the 214 status sample).
