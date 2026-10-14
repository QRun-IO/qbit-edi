# Changelog

Following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Phase a1 data model: 21 annotated entities, QQQ tables, enum/table value sources, joins, child lists, audit, permissions, partner locks, and PostgreSQL DDL with metadata-derived H2 integration tests.
- StAEDI 1.26.4 X12 streaming envelopes, separator detection, bounded transaction buffers, structured envelope findings, and fixed-width ISA serialization.
- Original 850/997/999 baseline guides, versioned database loading, SNIP 1–2 validation, syntax notes, and the documented situational rule language.
- TA1, 997 and 999 builders with acknowledgement levels, positional situational context, reply envelopes, and inbound acknowledgement parsing.
- Atomic RDBMS control-number allocation with first-use creation, wrap/exhaustion handling, and interchange/group/transaction duplicate detection.
- Database payload storage with AES-256-GCM envelope encryption, SHA-256 integrity metadata, key rotation, and replaceable environment-secret resolution.
- Per-instance runtime services and configuration, synthetic generators, exact acknowledgement fixtures, concurrent SQL tests, and payload tests that verify no temporary files are created.
- Repository scaffold to the QRun qbit repository standard: `qbit-build-parent` 2.0.0, QQQ 4.1.0-RC.5, Maven wrapper, Checkstyle, JaCoCo gate, CircleCI qqq-orb 0.6.8, release-candidate workflow, Dependabot, community files.
- `EdiQBitProducer` and `EdiQBitConfig` (table prefix `edi_`, permission base `edi`).
