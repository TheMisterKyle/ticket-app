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

## Prohibition

Do **not** add or maintain an application-specific self-updater. Ticket App participates in Style's shared installation and update lifecycle.

## Build-room checklist

Before declaring a Ticket App release complete:

- [ ] Consumer artifact is complete and immutable.
- [ ] Consumer artifact contains everything required on the destination machine.
- [ ] `style-release.json` validates against the current Style contract.
- [ ] `approved` is deliberately set to `true` only for an approved release.
- [ ] `app_id` and `channel` match Style's catalogue.
- [ ] URL, byte size and SHA-256 match the published artifact exactly.
- [ ] Source repository and full source commit are exact.
- [ ] Artifact and manifest are attached to the same GitHub Release.
- [ ] Update discovery is verified through Style.
- [ ] No Ticket-App-specific updater was introduced.

## Repository placement

Keep this file at the repository root as `STYLE_RELEASE_POLICY.md`. Reference it prominently from both `AGENTS.md` and the main `README` so future agents and human maintainers encounter it before changing packaging or release automation.
