# Upgrade notes

## Unreleased

- No schema change and no Liquibase include.
- Behavior change: an inactive identity (`isActive` false) now stops X12 routing and sending, as it already did for EDIFACT. Inbound documents from or to it are rejected as an unknown partner, and outbound requests for its agreements wait until it is active. Identities default to active, so only identities someone switched off are affected.
- Hosts that already call `EdiRuntime.initialize()` at startup keep getting the 41 built-in guides and code lists. A host that never called it will now see "Needs setup" for Guides and code lists on the home.
- New home section "Setup status" (widget `edi_home_setup`).
- New table action "Install sample maps" on Maps, permission `edi.admin`. It is never run automatically and only adds draft maps whose name does not exist yet.
