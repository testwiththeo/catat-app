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

## Features

Catat is built around the workflow QA teams repeat every day: capture evidence, add context, write a clear report, and send it to the right tracker.

| Area | Capability |
|------|------------|
| Capture | Floating overlay button, foreground service, MediaProjection screenshot flow, app-private PNG storage. |
| Annotation | Arrow, rectangle, text, blur, and pen tools with undo/redo, pinch zoom, pan, color, and stroke width controls. |
| Reports | Title, steps to reproduce, actual result, expected result, editable device context, and screenshot attachment. |
| Export | Jira, GitHub Issues, Linear, and Markdown templates with copy, share, and save actions. |
| History | Paginated report list, full-text search, status filters, report detail navigation, and swipe delete with undo. |
| Settings | Default export format, screenshot quality, floating button position, auto-attach preferences, templates, and data actions. |
| Privacy | Local-first storage. No account, no backend, no cloud sync, and no third-party analytics. |

## Product Flow

1. Enable the floating capture button.
2. Tap the button from any app.
3. Approve Android screen capture permission.
4. Annotate the screenshot.
5. Fill or edit the bug report details.
6. Export to Jira, GitHub Issues, Linear, or Markdown.
7. Reopen previous reports from searchable history.

## Screens

| Screen | What it does |
|--------|--------------|
| Splash | Lightweight entry screen with first-launch routing. |
| Onboarding | Explains capture, annotate, and export in three steps. |
| History | Main report list with search, filters, empty state, and quick capture entry. |
| Capture | Screenshot preview and retake flow. |
| Annotation | Full-screen editing surface for marking up screenshots. |
| Report Details | Structured bug report form with device context and screenshot preview. |
| Export | Template selector, rendered preview, clipboard, share, and file export. |
| Settings | Preferences, template management, data controls, and app info. |

## Tech Stack

| Layer | Tools |
|-------|-------|
| Language | Kotlin 1.9.23 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | Clean Architecture, MVVM, StateFlow, Use Cases |
| Dependency Injection | Hilt |
| Persistence | Room, FTS4, DataStore Preferences |
| Lists | Paging 3 |
| Capture | Foreground Service, WindowManager overlay, MediaProjection, VirtualDisplay, ImageReader |
| Testing | JUnit 5, Robolectric, MockK, Kotest, Room Testing |
| Build | Gradle, Android Gradle Plugin 8.3.2, KSP |
| CI | GitHub Actions |

## Architecture

Catat keeps the app split into presentation, domain, and data layers. The UI depends on ViewModels, ViewModels depend on use cases, and use cases depend on repository interfaces. Data implementations stay behind those interfaces.

```text
presentation
  -> ViewModels
  -> Compose screens
  -> Navigation

domain
  -> Models
  -> Repository interfaces
  -> Use cases
  -> Export and annotation logic

data
  -> Room database
  -> Repository implementations
  -> DataStore preferences
  -> File storage
  -> Capture implementation
```

This keeps screenshot capture, annotation rendering, export formatting, and persistence testable without coupling the UI directly to Android framework details.

## Project Structure

```text
app/src/main/java/com/catat/app/
|-- CatatApplication.kt
|-- data/
|   |-- db/
|   |-- mapper/
|   |-- repository/
|   |-- storage/
|   `-- preferences/
|-- di/
|-- domain/
|   |-- model/
|   |-- repository/
|   |-- usecase/
|   `-- export/
|-- presentation/
|   |-- navigation/
|   |-- screen/
|   `-- theme/
`-- service/
```

## Requirements

- Android Studio with Android Gradle Plugin 8.3 support
- JDK 17
- Android SDK 34
- Android device or emulator running API 26+
- Android SDK Platform Tools for `adb`

If `adb` is not found in your terminal, add Android SDK Platform Tools to your `PATH`. On Linux, it is commonly located at:

```bash
export PATH="$PATH:$HOME/Android/Sdk/platform-tools"
```

## Development

Open the project in Android Studio, let Gradle sync finish, then run the `app` configuration on a device or emulator. The app needs a real Android runtime for overlay and screen capture behavior.

Common commands:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew lintDebug testDebugUnitTest
```

Install the debug APK manually:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing

The test suite covers the core logic that should stay stable as the UI evolves:

- Export templates for Jira, GitHub Issues, Linear, and Markdown
- FTS query building
- Annotation engine undo, redo, and blur behavior
- Report use cases
- Room DAO insert, search, filter, and delete flows

Run tests locally before opening a pull request:

```bash
./gradlew lintDebug testDebugUnitTest
```

## Permissions

Catat uses Android permissions only for the bug report workflow:

| Permission | Why it is needed |
|------------|------------------|
| Draw over other apps | Shows the floating capture button above the app being tested. |
| Media projection | Lets Android ask for explicit consent before taking a screenshot. |
| Notifications | Keeps the capture overlay service alive on Android 13+. |

Screens protected by `FLAG_SECURE` cannot be captured. Catat handles that case as an Android platform restriction instead of bypassing it.

## Privacy

Catat is local-first by design.

- Reports are stored in the app database on the device.
- Screenshots are saved in app-private storage.
- Exports happen only when you copy, share, or save a report.
- There is no sign-in, backend service, telemetry, or analytics SDK.
- Uninstalling the app removes app-private data unless Android backup behavior is enabled by the device.

## Roadmap

- [x] Capture overlay and screenshot engine
- [x] Annotation tools with undo/redo
- [x] Report detail form and export templates
- [x] Searchable report history
- [x] Settings, onboarding, polish, tests, and CI
- [ ] Accessibility pass for large font and TalkBack
- [ ] More export templates
- [ ] Screenshot diff attachments
- [ ] Release build signing documentation

## Contributing

Contributions are welcome. Keep changes focused and open a pull request against `develop`.

```bash
git checkout develop
git pull origin develop
git checkout -b feat/your-feature
```

Before committing:

```bash
./gradlew lintDebug testDebugUnitTest
```

Use Conventional Commits:

```bash
git commit -m "feat: add export template preview"
git push origin feat/your-feature
```

Good pull requests usually include:

- A short explanation of the problem and solution
- Screenshots or screen recordings for UI changes
- Tests for domain, data, or formatting behavior
- Notes for Android permissions, migrations, or storage changes

## Issue Reports

When reporting a bug, include:

- Android version and device model
- App version or commit hash
- Steps to reproduce
- Expected result
- Actual result
- Screenshot or screen recording when possible

## License

Catat is released under the MIT License. You are free to use, modify, and distribute this project under the terms described in [LICENSE](LICENSE).

---

<div align="center">
  <strong>Catat</strong>
  <br />
  Built for QA engineers who care about clear, reproducible bug reports.
</div>
