# Style Release Policy — Ticket App

This file is a mandatory build and release contract for every future Ticket App build room.

## Authoritative rule

Style discovers application updates automatically. To publish an update, publish both of the following together on a GitHub Release in the application's canonical repository:

1. The complete immutable consumer artifact.
2. A valid, approved Style release manifest named `style-release.json`.

The release manifest must:

- set `approved: true`;
- use an `app_id` matching Style's catalogue entry exactly;
- use a `channel` matching Style's catalogue entry exactly;
- contain the exact immutable artifact URL;
- contain the artifact's exact byte size;
- contain the artifact's SHA-256 digest;
- identify the canonical source repository;
- identify the exact full source commit used to build the artifact.

Once the newer approved release is published correctly, Style detects it and displays **Update** automatically.

## Normal automated release path

Ticket App's canonical Style application manifest is `desktop-companion/style-app.json`. Its version is the production release signal.

For a normal feature or fix release, the Build Room must:

1. Finish and verify the feature.
2. Bump the desktop project version and the Style application manifest to the same new version.
3. Make the Style manifest/version bump the final release-ready change.
4. Push that change to `master`.

A push to `master` that changes `desktop-companion/style-app.json` automatically runs the release workflow. CI validates the identity and matching versions, builds the complete Windows runtime, checks the package boundary, calculates the artifact digest and byte size, generates an approved release manifest, validates it, and publishes both files to one GitHub Release.

Do not release on ordinary source commits. Manual workflow dispatch is an emergency/recovery mechanism, not the normal procedure.

## Riskier releases

Do not use the normal approved path without additional validation when a release changes persistent-data migration, installation architecture, lifecycle or shutdown behaviour, package layout, security or trust boundaries, destructive state, or the runtime platform. Produce an unapproved validation candidate and obtain explicit promotion approval where those risks warrant it.

## Application identity and boundaries

- Canonical repository: `https://github.com/TheMisterKyle/ticket-app`
- App ID: `ca.kyle.ticket-toss`
- Display name: `Ticket App`
- Channel: `stable`
- Runtime: Windows x64
- Package: portable managed directory
- Entrypoint: `TicketToss.Companion.exe`
- Persistent data: `%LOCALAPPDATA%/TicketToss`
- Migration owner: application

Persistent user data must remain outside the replaceable consumer package.

## Prohibition

Do **not** add or maintain an application-specific self-updater. Ticket App participates in Style's shared installation and update lifecycle.

## Build-room checklist

Before declaring a Ticket App release complete:

- [ ] Consumer artifact is complete and immutable.
- [ ] Consumer artifact contains everything required on the destination machine.
- [ ] Runtime and Style application versions match.
- [ ] `style-release.json` validates against the current Style contract.
- [ ] `approved` is deliberately set to `true` only for an approved release.
- [ ] `app_id` and `channel` match Style's catalogue.
- [ ] URL, byte size and SHA-256 match the published artifact exactly.
- [ ] Source repository and full source commit are exact.
- [ ] Artifact and manifest are attached to the same GitHub Release.
- [ ] Update discovery is verified through Style.
- [ ] Persistent data is excluded from the consumer package.
- [ ] No Ticket-App-specific updater was introduced.

## Repository placement

Keep this file at the repository root as `STYLE_RELEASE_POLICY.md`. Reference it prominently from both `AGENTS.md` and the main `README` so future agents and human maintainers encounter it before changing packaging or release automation.
