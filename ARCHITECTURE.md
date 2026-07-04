# Infinity APK Architecture

## Layers

### Screens (`lib/screens/`)

Each screen is a top-level route widget.

- **AuthScreen** – Firebase Authentication entry point.
- **DashboardScreen** – main hub after sign-in. Shows the connection status,
  runtime selector, control buttons (`Pause`, `Resume`, `Start`), and hosts
  bottom-navigation tabs for Dashboard, Agents, and Logs.
- **AgentsScreen** – lists agent/runtime statuses with pull-to-refresh and a
  small linear progress indicator per agent.
- **LogsScreen** – scrollable, reverse-ordered log viewer with level chips,
  color-coded levels, pull-to-refresh, and a clear button.

### Services (`lib/services/`)

- **AuthService** – wraps Firebase Authentication and exposes auth state changes.
- **ApiService** – handles HTTP requests to the Infinity backend using the `http` package.
- **WebSocketService** – maintains a WebSocket connection for real-time server events via `web_socket_channel`.

### Models (`lib/models/`)

Plain Dart objects representing domain entities such as agents, tasks, and messages. These are serialized from API and WebSocket payloads.

### Widgets (`lib/widgets/`)

Small, reusable UI components shared across screens.

- **ConnectionStatusIndicator** – colored dot + text reflecting online/offline state.
- **AgentListTile** – tile rendering agent id, status badge, and progress bar.
- **LogLevelChip** – compact, color-coded chip for log levels (`info`, `warning`, `error`, etc.).
- **ProgressSummaryCard** – card showing an overall progress value and bar.

### State Management

The app uses `provider` for dependency injection and state propagation.
`ApiProvider` is created in `main.dart` and exposed via `ChangeNotifierProvider`.
Screens can consume it with `context.read<ApiProvider>()` and `context.watch<ApiProvider>()`.
`DashboardScreen` also accepts `InfinityApi` and `InfinityWebSocket` instances directly
for testing and local development.

## Data Flow

1. The user launches the app; Firebase Auth determines signed-in status.
2. `main.dart` creates an `ApiProvider` with an `HttpInfinityApi` client and
   exposes it near the root of the widget tree.
3. After authentication, the user is routed to `DashboardScreen`.
4. `DashboardScreen` displays the WebSocket connection state, lists available
   runtimes, and lets the user select the active runtime.
5. Control buttons (`Pause`, `Resume`, `Start`) enqueue commands for the selected
   runtime through `ApiProvider.sendCommand`.
6. The **Agents** tab fetches the latest `AgentStatus` for the active runtime (or
   all runtimes when none is selected) and renders progress indicators.
7. The **Logs** tab queries `LogEntry` records, color-codes them by level, and
   displays them in a scrollable, reverse-ordered list.
8. `WebSocketService` streams live events; `DashboardScreen` listens and updates
   the online/offline indicator accordingly.

## Deployment

### Prerequisites

- [Flutter SDK](https://docs.flutter.dev/get-started/install) stable channel.
- Android SDK with API 34 (`compileSdk 34` and `targetSdk 34` are set in
  `android/app/build.gradle`).
- JDK 17 (the Gradle build configures `sourceCompatibility` /
  `targetCompatibility` to Java 17).
- A running Infinity-api instance (local or hosted) for the app to connect to.

### Firebase project setup

1. Create a Firebase project.
2. Enable **Authentication** and enable at least **Anonymous** and/or
   **Email/Password** sign-in.
3. Register an Android app with the package name `dev.infinity.apk`
   (matching `applicationId` in `android/app/build.gradle`).
4. Download `google-services.json` and place it in
   `android/app/google-services.json`. The file is **not included in this
   repository**.
5. If you use the google-services Gradle plugin, apply it in the Android
   build files. The current code also initializes Firebase manually in
   `lib/main.dart` with placeholder values (`apiKey`, `appId`, etc.); replace
   those with your real Firebase options or remove them in favor of
   `google-services.json`.

### API base URL configuration

`lib/main.dart` reads the API base URL from the `INFINITY_API_URL` compile-time
environment variable and defaults to `http://10.0.2.2:8000` for the Android
emulator:

```dart
const apiBaseUrl = String.fromEnvironment(
  'INFINITY_API_URL',
  defaultValue: 'http://10.0.2.2:8000',
);
```

For a hosted Infinity-api instance build with:

```bash
flutter build apk --release --dart-define=INFINITY_API_URL=https://my-infinity-api.onrender.com
```

### Build the APK

Install dependencies and build a debug or release APK:

```bash
flutter pub get
flutter analyze
flutter test
flutter build apk --release
```

The release build uses the debug signing config by default (see
`android/app/build.gradle`). Configure your own keystore for Play Store
uploads.

### GitHub Actions CI build

`.github/workflows/flutter.yml` runs on every push and pull request to
`main`/`master`:

1. Checks out the repository.
2. Sets up Flutter stable.
3. Runs `flutter pub get`.
4. Runs `flutter analyze`.
5. Runs `flutter test`.
6. Builds a debug APK with `flutter build apk --debug`.

### Run the app

1. Install the built APK on an Android device or emulator running API 21+.
2. Launch the app and sign in via Firebase Auth.
3. Ensure the configured `INFINITY_API_URL` is reachable from the device.
4. Select a runtime and use the dashboard controls to send commands back to
   Infinity-Cli.

## Known Limitations

- **Local-first reliance on a running Infinity-api** — Infinity-apk is a
  remote dashboard, not an embedded agent runtime. It can only monitor and
  control runs when it has network reachability to an Infinity-api server.
- **Firebase Auth is required** — Sign-in happens through Firebase Auth. The
  app cannot be used without a real Firebase project configured with valid
  credentials.
- **No offline queue** — Commands are sent immediately over the network. If
  the device is offline when a button is pressed the command is lost; there
  is no local persistence or retry queue yet.
- **WebSocket battery and background constraints** — The live WebSocket
  connection is maintained while the app is active. Android may suspend or
  terminate the connection when the app moves to the background, so live
  updates stop until the app returns to the foreground.
- **Free Render tier cold starts** — When Infinity-api is hosted on Render's
  free plan, the service sleeps after inactivity. The first launch of the app
  or reconnect after a period of inactivity may experience several seconds of
  cold-start latency.
- **Manual `google-services.json` not included** — The repository does not
  contain a production Firebase configuration file. You must generate and add
  it (or update the manual options in `lib/main.dart`) before the app can
  authenticate.
- **API base URL is compile-time only** — The backend URL is baked in at build
  time via `--dart-define`. There is no in-app settings screen to change the
  URL at runtime.
- **Android API 34 target** — The app targets API 34 (`targetSdk 34`). Older
  devices may require compatibility testing, and newer platform behavior
  changes may need future updates.

## Deployment

1. Install the Flutter SDK and Android SDK (API 34).
2. Run `flutter pub get` to fetch dependencies.
3. Add your Firebase project's `google-services.json` to `android/app/` and enable Firebase Authentication.
4. Configure the Infinity-api base URL in `lib/services/api_service.dart` (or via build-time environment).
5. Build the release APK: `flutter build apk --release`.
6. Install the APK on an Android device or emulator: `flutter install`.
7. The GitHub Actions workflow in `.github/workflows/ci.yml` runs `flutter test` and `flutter build apk` on every push.

## Known Limitations

- Requires a reachable Infinity-api backend; the app does not run agents locally.
- Firebase Auth requires a manual `google-services.json`, which is not included in this repository.
- There is no offline command queue; commands require an active network connection.
- The WebSocket connection may drop when the app is backgrounded or under battery saver restrictions.
- The default free Render backend tier sleeps after inactivity, causing cold-start latency.
- The current UI targets Android phones; tablet and iOS support are not implemented.
