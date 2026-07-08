# Infinity-apk

Infinity-apk is the Kotlin/Jetpack Compose Android companion app for Infinity.
It pairs with the desktop agent over the Infinity-api relay and lets you monitor
remote execution, view terminal output and unified diffs, receive notifications,
and approve or control sensitive commands from your phone.

All payloads to and from the desktop are end-to-end encrypted. The Android app
stores private keys in the Android Keystore.

## Prerequisites

- Android Studio Ladybug or newer
- Android SDK 35 (compileSdk) with SDK 26+ device or emulator
- JDK 17 or newer
- Infinity-api running and reachable from the device/emulator
- A Google account for OAuth sign-in

## Configure the Android SDK

Create or edit `local.properties` in the project root with your Windows SDK path
(using forward slashes):

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
```

## Configure the API endpoints

Open `app/src/main/res/values/strings.xml` (or the equivalent build-config
source set) and set the Infinity-api base URL and Supabase client ID that the
app should use:

```xml
<resources>
    <string name="app_name">Infinity Companion</string>
    <!-- Add or update these for your environment -->
    <string name="api_base_url">http://10.0.2.2:3000</string>
    <string name="supabase_client_id">your-supabase-client-id</string>
</resources>
```

Use `10.0.2.2` when running against Infinity-api on the emulator host.

## Build a debug APK

From PowerShell in the project root:

```powershell
./gradlew.bat --no-daemon assembleDebug
```

The APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Run unit tests

```powershell
./gradlew.bat testDebugUnitTest
```

## Run connected tests

Connect a device or start an emulator, then run:

```powershell
./gradlew.bat --no-daemon connectedDebugAndroidTest
```

## Device pairing

1. Sign in with Google on both the desktop agent and the Android app.
2. On the desktop, open the pairing screen to display a QR code.
3. In the Android app, scan the QR code to request pairing.
4. Approve the pairing request on the desktop.

Once paired, both devices exchange public keys so all relay traffic can be
encrypted end-to-end.

## Remote control features

- View live execution status and terminal output
- Receive notifications when human approval is needed
- Inspect unified diffs produced by the desktop agent
- Pause, resume, or stop an active execution
- Approve or deny sensitive commands from the approval dialog

## Security notes

- Messages are encrypted with the desktop's public key before leaving the device.
- The Android private key is generated and stored in Android Keystore.
- The Infinity-api relay cannot read message contents; it forwards encrypted
  envelopes only.
