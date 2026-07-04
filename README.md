# Infinity APK

The Flutter Android companion app for the Infinity CLI autonomous swarm platform.

## Overview

Infinity APK connects to the Infinity backend to let users authenticate, view and manage autonomous agents, and receive real-time updates over WebSocket. It targets Android API 34 with a minimum SDK of 21.

## Project Structure

```
Infinity-apk/
├── android/                # Android platform files
├── lib/
│   ├── main.dart           # App entry point
│   ├── screens/            # UI screens
│   ├── services/           # API, auth and WebSocket services
│   ├── models/             # Data models
│   └── widgets/            # Reusable widgets
├── test/                   # Widget and unit tests
└── .github/workflows/      # CI/CD workflows
```

## Getting Started

1. Ensure Flutter is installed and configured.
2. Run `flutter pub get` to install dependencies.
3. Run `flutter run` to start the app on a connected device or emulator.

## Build

CI builds are handled by GitHub Actions (`.github/workflows/flutter.yml`). The workflow installs dependencies, analyzes code, runs tests, and builds a debug APK.

## License

See the repository LICENSE file.
