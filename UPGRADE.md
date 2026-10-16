# Upgrade notes

## Unreleased

- No schema change and no Liquibase include.
- Behavior change: an inactive identity (`isActive` false) now stops X12 routing and sending, as it already did for EDIFACT. Inbound documents from or to it are rejected as an unknown partner, and outbound requests for its agreements wait until it is active. Identities default to active, so only identities someone switched off are affected.
- Hosts that already call `EdiRuntime.initialize()` at startup keep getting the 41 built-in guides and code lists. A host that never called it will now see "Needs setup" for Guides and code lists on the home.
- New widget "Setup status" (`edi_home_setup`).
- Behavior change: the EDI home shows business widgets only (`EdiHome.HOME_WIDGET_NAMES`), each now requiring `edi.hasAccess` (hidden without it). `edi_home_system`, `edi_home_dead`, `edi_home_certificates` and `edi_home_setup` left the EDI home: they are listed in `EdiHome.ADMIN_WIDGET_NAMES`, require `edi.admin.hasAccess` (hidden without it), and the host places them on its Admin home.
- New business widget `edi_home_delayed` ("Inbound documents are delayed"), shown only when the oldest unprocessed inbound document is older than the new config value `inboundDelayedAfterMinutes` (default 60).
- New table action "Install sample maps" on Maps, permission `edi.admin`. It is never run automatically and only adds draft maps whose name does not exist yet.
