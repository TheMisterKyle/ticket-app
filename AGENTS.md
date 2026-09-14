# Repository Instructions

These instructions apply to the entire repository.

## Mandatory Style release policy

Before modifying packaging, release automation, update behaviour, manifests, version metadata, or distributable artifacts, read and follow [STYLE_RELEASE_POLICY.md](STYLE_RELEASE_POLICY.md).

Do not create or maintain an application-specific updater. Style owns update discovery and installation. All approved Ticket App releases must remain discoverable and installable through Style.

## Build Room release responsibility

Future Build Rooms must:

- preserve Ticket App's Style identity: `ca.kyle.ticket-toss`, display name `Ticket App`, and catalogue channel `stable`;
- bump the desktop application version and `desktop-companion/style-app.json` version together;
- make the `style-app.json` version bump the final release-ready change;
- preserve the separation between the packaged runtime and persistent data under `%LOCALAPPDATA%/TicketToss`;
- let GitHub CI create the immutable consumer artifact and approved Style release;
- treat manual GitHub workflow execution as emergency/recovery fallback only;
- use an unapproved validation candidate and explicit promotion when a release changes data migration, installation architecture, lifecycle/shutdown behaviour, package layout, security boundaries, destructive state, or the runtime platform.

A push to `master` that changes `desktop-companion/style-app.json` is a production release signal. Do not change that file until the build is verified and genuinely ready to ship.
