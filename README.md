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

Every time you file a bug, you lose 5 minutes switching between apps, typing context, and formatting screenshots. Do that 10 times a day and you have wasted an hour. That hour belongs to testing, not paperwork.

Catat is an Android app that fixes this. A floating button lets you capture, annotate, and export a complete bug report in under 30 seconds without ever leaving the app you are testing.

## The Problem

Bug reporting is broken. You switch between your app and a ticket system. You type the same device info over and over. You open a separate editor to annotate screenshots. You paste everything together and hope the format sticks.

The result is slow, inconsistent, and frustrating. Developers get reports with missing context, no annotations, and different formats every time. They ask for clarifications. Bugs sit longer. Quality suffers.

**Testing should be the bottleneck. Not bug reporting.**

## The Flow

Tap the floating button from any screen. Catat captures the screenshot and overlays your annotation tools. Draw arrows, add text, or blur sensitive data. Device model, OS version, network type, and battery level are attached automatically. Choose Jira, GitHub Issues, Linear, or Markdown and export with one tap.

30 seconds. Done.

| What Used to Happen | What Happens Now |
|---|---|
| Switch between 4 apps to file one bug | Stay in your flow. Catat never leaves your screen. |
| Type device info manually every time | Device context attached automatically. |
| Reports arrive in random formats | Every export follows the same template. |
| Accidentally share sensitive data | One tap blur, gone. |
| Lose track of past reports | All reports saved locally, searchable offline. |

## Quick Start

```bash
git clone https://github.com/theobuilds/catat-app.git
cd catat-app
./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk
```

Catat needs draw-overlay permission for the floating button, media projection to capture screens, and notifications to stay alive. Nothing leaves your device.

## Built With

Kotlin, Jetpack Compose, Clean Architecture, Room, Hilt.

## License

MIT. See [LICENSE](LICENSE).

---

<div align="center">
  Built for every Software Tester, QA Engineer, and Test Engineer.
</div>
