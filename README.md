# Focus Lock (Android)

A native Android focus app. Run **named focus sessions** that block distracting apps and short-form feeds, play focus music, show a screensaver, and keep the user locked out until the timer ends. At the end of the session everything is restored automatically.

## Features

- Named sessions with duration or scheduled start ("start in N minutes")
- Full-app blocklist plus best-effort Reels/Shorts feature blocking
- Two enforcement tiers:
  - **Full mode** (Device Owner): apps truly disabled, lock-task kiosk, no bypass
  - **Standard mode** (any phone): accessibility dismissal + full-screen blocking overlay
- Focus music from bundled assets, local files, or streaming URLs, with audio fades
- Three screensaver styles (ambient gradient, breathing circle, rain)
- Optional emergency PIN to abort a session
- Live countdown in a foreground notification; exact-alarm and reboot recovery
- Session history and statistics

## Building

Requirements: JDK 17, Android SDK Platform 35, Build-Tools 35.0.0, Gradle 8.9 (wrapper included).

On a normal x86_64/arm64 development machine with Android Studio installed, open the project and sync. `wrap-x86.sh` and `build-app.sh` are only needed for the container environment used during development (arm64 host running x86_64 Android build tools via qemu).

```bash
export ANDROID_HOME=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
cd /root/focuslock
./gradlew assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Installing

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch, grant the permissions listed in onboarding (Accessibility, overlay, notifications, usage access, battery exemption). Exact alarms may also need to be enabled from Android settings.

## Full mode (optional)

Use a dedicated device with **no accounts** (factory reset recommended):

```bash
adb shell dpm set-device-owner com.focuslock.app/.blocking.DeviceAdminReceiver
```

Verify with `adb shell dumpsys device_policy | grep -i owner`.

Remove Full mode:

```bash
adb shell dpm remove-active-admin com.focuslock.app/.blocking.DeviceAdminReceiver
```

If anything goes wrong, a factory reset always removes the device owner.

## Known limitations

- A normal Android app cannot truly force-stop other apps or prevent its own uninstall. Full enforcement requires device-owner provisioning.
- Reels/Shorts detection is heuristic (Accessibility UI text/resource IDs) and can break when apps update. Markers are editable in Settings.
- Aggressive OEM battery managers may kill background services. Grant battery-optimization exemption and OEM auto-start permissions where available.
- Distribution is intended for sideload/enterprise use; Play Store policies restrict device-admin/accessibility apps.

## Tests

```bash
./gradlew test
```

Unit tests cover timing math and state transitions. Manual validation scenarios are listed in `docs/test-plan.md`.
