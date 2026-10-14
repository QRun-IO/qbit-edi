# Upgrade notes

## Unreleased

- No schema change and no Liquibase include. Hosts need do nothing.
- Hosts that already call `EdiRuntime.initialize()` at startup keep getting the 41 built-in guides and code lists. A host that never called it will now see "Needs setup" for Guides and code lists on the home.
- New home section "Setup status" (widget `edi_home_setup`). Hosts that list home widgets explicitly must add it to their app.
- New table action "Install sample maps" on Maps, permission `edi.admin`. It is never run automatically and only adds draft maps whose name does not exist yet.
