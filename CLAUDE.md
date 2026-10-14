# qbit-template-application

GitHub template repository for scaffolding Application QBits (complete mini-applications
with tables, processes, widgets, and UI navigation) on the QQQ platform.

## QQQ 4.0 baseline — 2026-09-24

The historical review below predates the 4.0 migration. This template targets Java 21 and the public QQQ 4.0.0 release from Maven Central. The corrected APIs, license references and actual template commands are in README.md and docs/00-getting-started.md. Preserve the documented multi-instance and scaffold limitations.

## Knowledge base

Reviewed dossier and platform knowledge live in the second-brain vault:

- Hub: `R:/Git.Local/KofTwentyTwo/second-brain/knowledge/qqq/qqq-hub.md`
- This repo's dossier: `R:/Git.Local/KofTwentyTwo/second-brain/knowledge/qqq/repos/qbit-template-application.md`
- QBit mechanics refresher: `R:/Git.Local/KofTwentyTwo/second-brain/knowledge/qqq/architecture/metadata-model.md`

Reviewed at commit `fb9395ac0d07` (branch `main`, 2026-07-04).

Key caveat from that review: the scaffold in `src/main` references six qqq APIs that do not
exist in any qqq version (wrong packages for `MetaDataProducerInterface` /
`AbstractWidgetRenderer` / `RenderWidgetInput`, plus nonexistent `QInstance.addAppSection`,
`QProcessMetaData.withInputFields`, `QAppSection.withSourceQBitName`) — it has never compiled,
and CI on `main` is red. Verify against the qqq checkout (`R:/Git.Local/QRunIO/qqq`) before
trusting any API usage shown here.
