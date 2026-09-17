# GitHub Actions Governance

These rules are mandatory for this repository.

1. Ordinary pushes and pull requests may run only targeted validation.
2. Packaging and distributable builds require manual dispatch, a version/manifest change, or a release tag.
3. Every job must define `timeout-minutes`.
4. Push/PR validation must use concurrency with `cancel-in-progress: true`.
5. Windows runners are reserved for work that actually requires Windows.
6. Every Actions artifact must define retention of 1–7 days. The standard is 3 days.
7. Canonical approved binaries belong in GitHub Releases; Actions artifacts are temporary.
8. One-off `apply-*` workflows may not remain active.
9. Workflow edits are checked by `actions-policy.yml`.
10. Account-level spending and usage limits override repository convenience.
