# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

**QBit EDI**: a full EDI application qbit for QQQ. Trading partners and agreements, pluggable
transports (AS2, SFTP/FTPS, VAN, HTTPS, email), X12 (004010, 005010; EDIFACT later) parsing and
validation against data-driven implementation guides, 997/999/TA1, versioned maps to QQQ records,
inbound and outbound flows, and HIPAA documents with PHI classification. Design: `docs/DESIGN.md`.

## Build Commands

```bash
./mvnw compile
./mvnw test
./mvnw verify  # Checkstyle, tests, JaCoCo: 70% instructions, 90% classes
```

Always use the wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM.

## Architecture

Entry point: `EdiQBitProducer implements QBitMetaDataProducer<EdiQBitConfig>` in
`com.kingsrook.qbits.edi`. Component producers are scanned only in `com.kingsrook.qbits.edi.metadata`.
The package layout per phase is in `docs/DESIGN.md`.

## Invariants

- Data lives in standard visible QQQ tables with record screens, secured by permissions and record locks.
- Processes are table or record actions with plain labels, never in the nav; record actions use the
  standard streamed-ETL Review/Result summary.
- One app home that owns the EDI data, built from typed widgets (no `RawHTML`, guardrail G5).
- Inbound transport endpoints are non-UI routes under `/qqq/hooks/edi/...`, never under `/qqq/v1`.
  UI endpoints are spec'd `MiddlewareSpecExtension`s under `/qqq/v1/qbit/edi/...` with a committed fragment.
- Records are created and updated through normal QQQ actions as a service account.
- Raw documents and PHI fields are classified (`DataClassification`), stored encrypted.
- Secrets are referenced by environment-variable name only. No real PHI in tests or samples.
- No dependency on another qbit's internals (guardrail G1); notifications go through core `QAlerts`.

## Naming

- Tables: `edi_<name>`, lowercase snake_case; SQL names equal QQQ table names.
- Permissions: full name, `edi.*`.

## Code Style

- Copy the Apache header from `checkstyle/license.txt`; 3-space indent, Allman braces, `return (x);`.
- Flower-box Javadoc on every class and method; fluent `withX()` setters.
- Wrapper types in config and entities; JUnit 5 and AssertJ tests.

## Dependencies

Java 21; parent `com.kingsrook:qbit-build-parent:2.0.0`. Default-active `qqq-snapshot` imports QQQ
`4.1.0-RC.6` ahead of the parent's 4.0.0 BOM (ADR-0007); move to each new RC within a week (G7).
Middleware Javalin is optional. Also StAEDI (X12) and as2-lib with BouncyCastle (AS2), all Apache-2.0/MIT-style licensed.

## Release

CircleCI qqq-orb 0.6.8: `develop` publishes SNAPSHOTs, `release/*` publishes RCs (orb version
bumps; cut by the `release-rc.yml` workflow), `vX.Y.Z` tags publish releases. No manual publishing.
Branches are `feature/*` or `fix/*` only; never push `hotfix/*` or `release/*`.
