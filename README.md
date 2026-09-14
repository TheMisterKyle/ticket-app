# Ticket App

Ticket App contains the Android Ticket Toss controller and its Windows projector companion.

## Style releases

Style owns update discovery and installation for the desktop Ticket App. Ticket App must publish compatible approved releases, and contributors must read and follow the mandatory [Style Release Policy](STYLE_RELEASE_POLICY.md) before producing or changing a release.

For normal releases, the final matching version bump in the desktop project and `desktop-companion/style-app.json` is pushed to `master`. That manifest change triggers CI to validate, package, and publish the approved Style release automatically. Manual workflow dispatch is reserved for recovery.
