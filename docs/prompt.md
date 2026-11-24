# Build Prompt — Focus Lock (Android)

> Copy/paste this file into a fresh implementation agent (or tell it: "Build the app described in `/root/.opencode/plan/prompt.md`").
> Supporting documents: `spec.md` (requirements/acceptance criteria) and `prerequisite.md` (toolchain, permissions, provisioning, scaffolding).

---

## 1. Your role and objective

You are an expert Android engineer. Build **Focus Lock**, a native Android application that helps a user stay focused by running *named focus sessions* during which distracting applications and distracting *features* are blocked, focus-enhancing music plays, a screensaver is shown, and the user cannot bypass the lock until the timer ends. At the end of the session, normal state is restored automatically.

Implement in the order given in §6. Produce working, compilable code with clear structure — not pseudocode. Where a genuinely impossible Android behavior is requested, implement the closest supported mechanism and clearly document the limitation (see §8).

---

## 2. Locked-in product decisions (do not re-litigate)

- **Platform:** Android (mobile), native. Minimum SDK 26, target current stable (API 35+ recommended).
- **Language/UI:** Kotlin + Jetpack Compose (Material 3). No cross-platform framework.
- **Blocking:** *Hybrid* — full-app lockdown for major distractors plus best-effort feature-level blocking of short-form feeds (Instagram Reels, YouTube Shorts).
- **Hard lock:** support **both** modes:
  1. **Device-owner / kiosk mode** — true hard lock (apps can be disabled, lock-task pins the UI, no bypass).
  2. **Personal mode** — works on an ordinary phone with Accessibility + overlay permissions; keeps the user out via dismissal and an undismissable overlay, but cannot truly force-stop apps or prevent uninstall. This mode is honestly "soft-hard."
  - The app must detect which mode it is in (`DevicePolicyManager.isDeviceOwnerApp()`) and enable the stronger capabilities when available.
- **Bypass protection:** No in-app "end session" for the focused user. Only the timer, an optional pre-set emergency PIN (admin use), or device-owner de-provisioning can end a session. No confirm-and-exit shortcuts.
- **Music:** support all three sources, selectable per session: bundled royalty-free audio, user's local audio files, and streaming URLs.
- **Screensaver:** full-screen calm visual, stay-awake, immersive. In device-owner mode it is the pinned lock-task activity.

---

## 3. What "done" looks like (MVP acceptance)

The user can:

1. Create a session: give it a **name**, pick a **duration** (or start/end time), select **apps to block**, select **feature targets** (Reels/Shorts), choose a **music profile** and **screensaver style**, then press **Start**.
2. See a live countdown in the UI and in the ongoing notification.
3. On start: selected apps are disabled (device-owner mode) or interception begins (personal mode); music fades in; screensaver appears; the app pins itself into lock task (device-owner mode).
4. Try to open a blocked app → it cannot launch (owner mode) or is immediately dismissed and the block overlay reappears (personal mode).
5. Try to open Reels/Shorts → detected and dismissed/best-effort blocked, with a clear "Focus session active" message.
6. Be unable to cancel or leave the session until the deadline.
7. At the deadline: music fades out, apps are re-enabled, lock task/overlay is removed, user is returned home, and a **session summary** (name, planned vs. actual time, status = Completed) is stored and displayed.
8. Review session history and basic stats.
9. Survive app backgrounding, screen-off/doze, and device reboot (active session resumes from a stored absolute deadline).
10. Configure permissions from an in-app onboarding/setup screen with deep links to the relevant Android settings.

All of the above must be covered by the automated/unit tests where feasible and by a manual test checklist on a physical device.

---

## 4. Required behavior in detail

### 4.1 Session management
- CRUD for named sessions; name is required, max ~50 chars; shown everywhere (notification, overlay, summary).
- Sessions may start immediately or be scheduled for a future time.
- A template concept is useful: reuse a previous session's blocklist/music/screensaver.
- Persist everything in Room. Use DataStore for settings and for the *active* session deadline so it can be restored.

### 4.2 Timer
- Store deadline as `SystemClock.elapsedRealtime()`-based absolute value plus wall-clock start for display.
- Foreground service owns the countdown; publishes state via `StateFlow`; updates the notification at ~1s.
- Guarantee completion under doze with `AlarmManager.setExactAndAllowWhileIdle` at the deadline.
- `BOOT_COMPLETED` receiver restores any `ACTIVE`/`SCHEDULED` session by comparing stored deadline.
- Handle clock changes by relying on elapsed realtime, not wall clock.

### 4.3 Full-app blocking
- **Owner mode:** `DevicePolicyManager.setApplicationEnabledSetting(pkg, COMPONENT_ENABLED_STATE_DISABLED, 0)` at start; restore to `COMPONENT_ENABLED_STATE_DEFAULT` at end. Maintain the original states so restoration is exact. Never attempt to disable critical system packages; validate against a denylist of critical packages.
- **Personal mode:** AccessibilityService observes `TYPE_WINDOW_STATE_CHANGED` events; on a foreground package in the blocklist, immediately `performGlobalAction(GLOBAL_ACTION_HOME)` (or BACK), show the block overlay, and keep re-showing it if the user returns.
- Use `UsageStatsManager` (`PACKAGE_USAGE_STATS`) as a secondary foreground-app check where available.

### 4.4 Feature-level blocking (Reels / Shorts)
- AccessibilityService scans the UI hierarchy node text / content-description / resource-id for markers such as `Reels`, `Shorts`, `Reel`, `ShortsPlayer`, `reel`, locale-aware variants.
- On a match during an active session, inject BACK/HOME or cover with overlay; log the event for the session stats.
- Treat as **best-effort**: app updates break selectors. Make marker lists configurable in settings so they can be updated without a full rebuild.
- Optional Phase-2: local `VpnService` to block short-form web domains for browsers (mark clearly as optional/advanced).

### 4.5 Hard lock
- **Owner mode provisioning** is documented in `prerequisite.md`; the app must provide a `DeviceAdminReceiver` with the proper manifest declaration and `device_admin.xml`.
- At start: `setLockTaskPackages` (admin) includes own package, then `startLockTask()` from the focus activity. Use `DevicePolicyManager` lock-task feature flags to disable status bar / notifications / home / recents as available.
- Optional emergency PIN: set during setup, required to abort a session; if absent, there is no abort. Abort still records the session as `ABORTED` and fully restores state.
- At end: `stopLockTask()`, re-enable apps, stop overlay, stop accessibility interception.
- **Personal mode:** full-screen overlay window (`TYPE_APPLICATION_OVERLAY`) with `FLAG_KEEP_SCREEN_ON`, immersive sticky, intercepts all touches; AccessibilityService re-shows it if dismissed; ongoing non-dismissible notification.

### 4.6 Music
- Media3/ExoPlayer in a `MediaSessionService`/foreground service. Implement a source resolver:
  - `BUNDLED`: play audio files from `assets/` or `res/raw`.
  - `LOCAL`: user picks files via `ACTION_OPEN_DOCUMENT`; persist URI permissions (`takePersistableUriPermission`).
  - `STREAMING`: HTTP(S) URL field; validate scheme.
- Per-session profile with volume; fade-in on start, fade-out on end (~2–4s); handle audio focus, ducking, and headset/unplug events gracefully; expose lock-screen transport controls.

### 4.7 Screensaver
- Compose full-screen visual styles (e.g., ambient gradient, breathing circle, rain). Immersive sticky + keep screen on. In owner mode this is the lock-task activity; in personal mode an overlay host activity.
- Must not show sensitive/notification content; hide behind the overlay.

### 4.8 Session end & stats
- Orchestrate shutdown in a single idempotent `finishSession()` use-case: stop audio → restore apps → release lock/overlay → navigate home → persist summary → show result.
- Stats: total focused time, completed sessions, streaks, per-app blocks count.

### 4.9 Setup/onboarding
- First-run screen explains and requests, with deep links:
  - Accessibility access (with explicit explanation of why), overlay permission, notification permission (Android 13+), usage access, ignore battery optimizations, exact alarms (Android 12+ where needed).
- Clear explanation that device-owner mode requires provisioning and is optional.

---

## 5. Architecture / module layout

```
app/
 ├── app/                     Application, navigation, DI graph, onboarding
 ├── feature/session/         Create/name/schedule/list/history UI
 ├── feature/focus/           Active session overlay, screensaver, countdown
 ├── core/timer/              Timer foreground service, alarms, boot receiver
 ├── core/blocking/           AccessibilityService, app-disable, lock-task, overlay, VPN (opt.)
 ├── core/audio/              Source resolver, MediaSession service, fades
 ├── core/data/               Room DB, DataStore, repositories, entities, DAOs
 ├── core/domain/             Models, session state machine, orchestration use-cases
 ├── core/notifications/      Ongoing notification builders
 └── core/common/             DI modules, utils, constants
```

Key state machine: `SCHEDULED → ACTIVE → COMPLETED` (or `ABORTED`). Use a single source of truth (`SessionRepository` + `ActiveSessionManager`), expose `StateFlow`, and make all transitions idempotent so reboot/alarm replay cannot double-apply blocking.

---

## 6. Build order (follow this sequence)

0. **Scaffolding / prerequisites** — create project, modules, Gradle/Kotlin/Compose config, Room + Hilt, manifest declarations (AccessibilityService, DeviceAdminReceiver + `device_admin.xml`, foreground services, receivers), permissions, baseline navigation + theme. See `prerequisite.md` §10.
1. **Data + session UX** — entities/DAOs/repositories, create/name/schedule/list/history screens, state machine.
2. **Timer** — foreground service, notification, exact alarm, boot restore.
3. **Blocking engine** — AccessibilityService detection + personal-mode overlay/dismiss; device-owner app disable + lock task; mode detection.
4. **Audio** — three sources, MediaSession, fades.
5. **Screensaver** — styles + overlay reliability.
6. **End orchestration + stats + summary UI** — idempotent finish flow, history/stats.
7. **Settings + hardening + tests** — PIN, whitelists, marker config, manual test checklist, docs.

Do not skip ahead; each phase builds on the previous one. Keep the app runnable at the end of every phase.

---

## 7. Code quality expectations

- Idiomatic Kotlin, coroutines/Flow, Compose with unidirectional state (UiState + events).
- Hilt DI; repositories abstract data sources; use-cases own orchestration.
- Keep OS-interaction code behind interfaces so it can be faked in tests (e.g., `Blocker`, `AppDisabler`, `Locker`, `MusicPlayer`).
- Unit tests for state machine, timer deadline math, and use-cases; instrumentation tests for Room and key flows if time permits.
- Meaningful error handling and user-facing messages when a permission is missing or provisioning fails.
- No secrets, API keys, or credentials hardcoded. No root usage.

---

## 8. Constraints and honest limitations (respect these)

- A normal app **cannot** truly force-stop other apps or prevent its own uninstall. Only device-owner mode can; personal mode is best-effort. Never claim otherwise in UI copy.
- Accessibility-based Reels/Shorts detection is heuristic and breaks with app updates; make it configurable and clearly labeled best-effort.
- Device-owner provisioning requires the device to have no accounts and is typically done before first setup / after factory reset via adb or QR; the app must ship a documented, safe de-provision path.
- Accessibility and overlay permissions are granted manually by the user; some OEMs kill background services — request battery-optimization exemption and document OEM caveats.
- Foreground service types, notification permission, and exact-alarm permissions differ by API level — guard with version checks.
- Play Store policy restricts accessibility/kiosk/device-admin apps; distribution may need to be sideload/enterprise. Note this in docs.

---

## 9. Deliverables

- Complete Android project source in the working directory, organized by the module layout above.
- `README.md` with build instructions, provisioning steps, and the manual test checklist.
- Keep `/root/.opencode/plan/prompt.md`, `spec.md`, and `prerequisite.md` as the source of truth; update them only if requirements change.
- Final response: short summary of what was built, how to run/provision, known limitations, and test results.

---

## 10. Start here

Read `prerequisite.md` first and verify the toolchain and permissions/provisioning approach. Then read `spec.md` for acceptance criteria. Then execute §6 Phase 0. If anything in the environment is missing (SDK, JDK, device), report it before writing code rather than guessing.
