<div align="center">
  <img src="docs/images/catat-logo.png" alt="Catat Logo" width="120" />
  <h1 align="center">Catat</h1>
  <p align="center">
    <strong>Bug Report Companion for QA Engineers</strong>
    <br />
    Capture. Annotate. Export. In 30 seconds.
    <br />
    All data on device. Zero cloud dependencies.
  </p>

  <p align="center">
    <img src="https://img.shields.io/badge/Kotlin-2.0-blue?logo=kotlin" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Compose-BOM-4285F4?logo=jetpackcompose" alt="Compose" />
    <img src="https://img.shields.io/badge/API-26%2B-brightgreen" alt="API 26+" />
    <img src="https://img.shields.io/badge/License-MIT-yellow" alt="MIT" />
    <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build" />
    <img src="https://img.shields.io/badge/coverage-80%25-brightgreen" alt="Coverage" />
  </p>
</div>

Catat is an Android app that helps QA engineers file better bug reports faster. A floating button lets you capture screenshots from any app, annotate with arrows and blur, and export formatted reports to Jira, GitHub Issues, Linear, or Markdown. All in under 30 seconds.

## The Problem

Every day you lose 30 to 60 minutes on busywork. Taking screenshots, copying device info, formatting steps to reproduce, pasting everything into a ticket system. That is not testing. That is paperwork.

Worse, bug reports end up messy. Missing context, inconsistent format, no screenshots with annotations. Developers waste time asking for clarification. Bugs take longer to fix.

## What Catat Does

Catat turns bug reporting into a single flow.

Tap the floating button from any app. Capture a screenshot. Annotate it with arrows, text, or blur to hide sensitive data. Catat automatically attaches device model, OS version, network type, and battery level. Export to Jira, GitHub Issues, Linear, or Markdown with one tap. Done.

**30 seconds per bug report instead of 5 minutes.**

| Pain Point | How Catat Helps |
|------------|----------------|
| Switching between apps to capture, annotate, and file | Floating button works over any app. No context switching. |
| Bug reports missing critical context | Device info, OS, network, battery attached automatically. |
| Inconsistent report formats | One tap export to Jira, GitHub, Linear, or Markdown. Consistent every time. |
| Sensitive data in screenshots | Built-in blur tool to redact before export. |
| Reports lost or scattered | All reports saved locally, searchable, offline. |

## Quick Start

```bash
git clone https://github.com/theobuilds/catat-app.git
cd catat-app
./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk
```

Catat requests three permissions on first run: draw over other apps (for the floating button), media projection (to capture screenshots), and notifications (to keep the service alive). All data stays on your device.

## Built With

Kotlin, Jetpack Compose, Clean Architecture, Room, Hilt. Full docs on [architecture](docs/03_ARCHITECTURE.md), [data model](docs/04_DATA_MODEL.md), and [testing strategy](docs/06_TEST_PLAN.md).

## Contributing

Bug reports and pull requests are welcome. See [CONTRIBUTING.md](COMMIT_GUIDE.md) for conventions.

## License

MIT. See [LICENSE](LICENSE).

---

<div align="center">
  Built for every Software Tester, QA Engineer, and Test Engineer.
</div>
