# Software Specification — Focus Lock (Android)

**Version:** 1.0 (draft for implementation)
**Status:** Approved as basis for build; source of truth alongside `prompt.md`.
**Scope:** Native Android application for named, timed focus sessions with hybrid app blocking, hard lock, focus music, and screensaver.

---

## 1. Overview

### 1.1 Vision
A user declares a focus session ("Deep Work — Q3 report", 50 minutes). For that interval the phone becomes a focus tool: distracting apps and short-form feeds are inaccessible, calm music plays, a screensaver fills the display, and the user cannot leave until the timer expires. When time is up, everything is restored automatically and the session is recorded.

### 1.2 Goals
- Let users **name and schedule** focus sessions.
- **Block** distracting apps and short-form feed features during a session.
- Enforce a **hard lock** with no user bypass while the session is active.
- Provide **focus music** and a **screensaver** to support concentration.
- **Automatically restore** device state at session end and keep history/stats.
- Work in two capability tiers: full (device-owner) and reduced (personal).

### 1.3 Non-goals
- Parental controls / remote monitoring by another person.
- Rooting the device or bypassing Android security model.
- Guaranteed detection of every future Reels/Shorts UI change.
- Public Play Store distribution as a primary target (policy risk — see §13).

### 1.4 Definitions
- **Session** — a named interval with configuration and a lifecycle.
- **Blocklist** — set of packages denied during a session.
- **Feature target** — short-form feed surface (Reels/Shorts) detected heuristically.
- **Owner mode** — app is Device Owner; can disable apps and pin kiosk UI.
- **Personal mode** — ordinary install; relies on Accessibility + overlay permissions.
- **Hard lock** — no in-app exit before deadline.

---

## 2. Users

- **Primary user:** an individual who wants to stop doom-scrolling and work/study for bounded periods, on a personal Android phone.
- **Advanced user:** wipes/provisions a dedicated device (or secondary profile) as a focus device and uses owner mode for true enforcement.

---

## 3. Functional requirements

Priority: **P0** = MVP must-have, **P1** = important, **P2** = optional/advanced.

### FR-1 — Session creation and naming (P0)
- FR-1.1 User can create a session with a required name (1–50 chars), duration (minutes/hours) or explicit start/end time.
- FR-1.2 User can select: blocklist apps, feature targets, music profile, screensaver style, optional emergency PIN.
- FR-1.3 User can start immediately or schedule for later.
- FR-1.4 User can duplicate a previous session as a template.
- FR-1.5 Sessions persist in Room and appear in a list/history screen.

### FR-2 — Timer and scheduling (P0)
- FR-2.1 A foreground service maintains an accurate countdown using elapsed realtime.
- FR-2.2 Countdown visible in app UI, lock/overlay UI, and ongoing notification (1s granularity).
- FR-2.3 An exact alarm (`setExactAndAllowWhileIdle`) fires at deadline even under doze.
- FR-2.4 `BOOT_COMPLETED` restores active/scheduled sessions after reboot.
- FR-2.5 Scheduled sessions auto-start at their start time without user interaction.
- FR-2.6 Wall-clock changes do not affect countdown correctness.

### FR-3 — Full-app blocking (P0)
- FR-3.1 Owner mode: blocklisted third-party apps are disabled at session start and restored to their original enabled state at end.
- FR-3.2 A safety denylist prevents disabling critical system packages (launcher, settings, phone, system UI, the focus app itself).
- FR-3.3 Personal mode: AccessibilityService detects a blocklisted foreground app and immediately dismisses it (HOME/BACK) and shows the block overlay.
- FR-3.4 Block attempts are counted and attributed to the active session.
- FR-3.5 If blocking fails (permission revoked, OEM kill), the app surfaces a clear status and attempts recovery.

### FR-4 — Feature-level blocking (P0 best-effort, P2 VPN)
- FR-4.1 During a session, AccessibilityService inspects the active window's UI hierarchy for configurable markers (Reels, Shorts, resource IDs, locale variants).
- FR-4.2 On a match, inject BACK/HOME or show the block overlay; log the event.
- FR-4.3 Marker list is user-configurable in settings.
- FR-4.4 (P2) Optional local VPN blocks short-form web domains in browsers.

### FR-5 — Hard lock (P0)
- FR-5.1 Owner mode: app registers as device owner; at session start it adds itself to lock-task packages and calls `startLockTask()`, with status bar/notifications/recents restricted where supported.
- FR-5.2 No "End session" control is presented to the focused user while active.
- FR-5.3 Optional emergency PIN (set before starting) is the only deliberate abort path; abort records `ABORTED` and restores all state.
- FR-5.4 At deadline: `stopLockTask()`, overlay removed, apps re-enabled, interception stopped.
- FR-5.5 Personal mode: full-screen overlay intercepts input, reappears if dismissed, and stays above other apps; ongoing notification is non-dismissible.
- FR-5.6 Mode detection via `DevicePolicyManager.isDeviceOwnerApp()` gates all privileged behavior; privileged calls are guarded so personal mode never crashes.

### FR-6 — Music (P0)
- FR-6.1 Three source types: bundled assets, user-selected local files, streaming URL.
- FR-6.2 A music profile (source + identifier + volume) is attachable to a session.
- FR-6.3 Playback via Media3/ExoPlayer + MediaSession with lock-screen controls.
- FR-6.4 Fade-in at start and fade-out at end; audio focus and headset changes handled.
- FR-6.5 Local file access permissions are persisted; streaming URLs validated (HTTPS/HTTP).

### FR-7 — Screensaver (P0)
- FR-7.1 Full-screen, immersive, stay-awake visual shown during the active session.
- FR-7.2 At least two styles (e.g., ambient gradient, breathing circle); style selectable per session.
- FR-7.3 In owner mode it is the pinned lock-task activity; in personal mode an overlay-hosted activity/window.
- FR-7.4 Sensitive notification content is hidden while active.

### FR-8 — Session end and statistics (P0)
- FR-8.1 A single idempotent `finishSession()` use-case performs all shutdown steps in order: stop audio → restore apps → release lock/overlay → home → persist summary → show result.
- FR-8.2 Summary shows session name, planned duration, actual duration, status, block counts.
- FR-8.3 History list and aggregate stats (total focus time, completed sessions, streaks, per-app blocks).
- FR-8.4 Replay of alarm/boot events must not double-apply or double-restore blocking state.

### FR-9 — Setup, permissions, settings (P0)
- FR-9.1 Onboarding explains and requests: Accessibility, overlay, notifications (API 33+), usage access, battery-optimization exemption, exact alarms where needed.
- FR-9.2 Deep links to the relevant system settings pages; in-app status indicators reflect what is granted.
- FR-9.3 Settings: emergency PIN, whitelists, feature markers, music profiles, screensaver preference.
- FR-9.4 Owner-mode provisioning and de-provisioning instructions are included in the app/docs.

---

## 4. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Reliability | Timer accurate to within 1s displayed; deadline completion guaranteed under doze via exact alarm + boot receiver. |
| NFR-2 | Resilience | All blocking/lock/restore operations are idempotent and safe to repeat after reboot or service restart. |
| NFR-3 | Performance | UI remains responsive; countdown ticks at 1s; no busy loops; overlay lightweight. |
| NFR-4 | Battery | Use foreground service appropriately; request battery-optimization exemption; avoid wake locks except where required. |
| NFR-5 | Compatibility | minSdk 26; target current stable; version-guarded permission/service-type handling. |
| NFR-6 | Privacy | No analytics, no personal data transmission; audio URLs user-provided; no credentials in APK. |
| NFR-7 | Security | PIN stored safely (hashed, e.g., via Android Keystore-backed storage); no hardcoded secrets; validate package names/URLs. |
| NFR-8 | Usability | Clear explanations for every permission; honest copy about personal-mode limits. |
| NFR-9 | Testability | OS interactions behind interfaces; core state machine/unit-testable; manual test checklist provided. |
| NFR-10 | Safety | Never disable critical system packages; always provide a restore path; safe de-provisioning documented. |

---

## 5. Architecture

### 5.1 Modules
`app`, `feature/session`, `feature/focus`, `core/timer`, `core/blocking`, `core/audio`, `core/data`, `core/domain`, `core/notifications`, `core/common` (see `prompt.md` §5).

### 5.2 Components
- **SessionRepository / Room DB + DataStore** — persisted sessions, settings, active-session state.
- **ActiveSessionManager** — single source of truth for current session; exposes `StateFlow<SessionState>`.
- **TimerService** — foreground service; countdown, notification, alarm, boot restore.
- **BlockingCoordinator** — chooses owner/personal strategy; dispatches to `AppDisabler`, `AccessibilityBlocker`, `OverlayController`, `LockTaskController`.
- **FocusAccessibilityService** — foreground-app + feature-marker detection; injects dismiss actions.
- **MusicController** — Media3 source resolver, MediaSession, fades.
- **ScreensaverActivity** — full-screen visual; activated as lock-task or overlay host.
- **FinishSessionUseCase** — idempotent end orchestration.
- **Setup/Onboarding** — permission status and deep links.

### 5.3 Key flows
**Start:** create/validate session → persist ACTIVE + deadline → start TimerService → start MusicController → apply blocking (disable apps / enable interception) → show Screensaver → (owner) startLockTask → notification.

**End (deadline/alarm/PIN abort):** FinishSessionUseCase idempotent shutdown → restore apps → stop audio → release lock → persist COMPLETED/ABORTED → navigate home → summary.

**Reboot:** BootReceiver reads persisted active/scheduled session → compares deadline → if past, finish; else restart TimerService + blocking.

---

## 6. Data model (Room entities)

- **FocusSession**: `id: Long`, `name: String`, `scheduledStartAt: Long?`, `startedAt: Long?`, `deadlineElapsedRealtime: Long?`, `plannedDurationMs: Long`, `actualDurationMs: Long`, `status: String` (SCHEDULED/ACTIVE/COMPLETED/ABORTED), `screensaverStyle: String`, `musicProfileId: Long?`, `createdAt: Long`, `completedAt: Long?`.
- **BlockedApp**: `id: Long`, `sessionId: Long`, `packageName: String`, `mode: String` (FULL/FEATURE), `originalEnabledState: Int?` (for exact restore).
- **MusicProfile**: `id: Long`, `name: String`, `source: String` (BUNDLED/LOCAL/STREAMING), `location: String` (asset path, content URI, or URL), `volume: Float`.
- **BlockEvent**: `id: Long`, `sessionId: Long`, `packageName: String`, `kind: String` (APP/FEATURE), `timestamp: Long`.
- **Settings (DataStore)**: `emergencyPinHash: String?`, `featureMarkers: List<String>`, `whitelistedPackages: List<String>`, `onboardingComplete: Boolean`, prefs.

Relationships: one session has many BlockedApp rows and many BlockEvent rows; music profile referenced by id (profiles can be reused).

---

## 7. State machine

```
        schedule/start
[NONE] ──────────────▶ SCHEDULED ──(start time)──▶ ACTIVE
                                                    │
                                       ┌────────────┴────────────┐
                                    (deadline)                 (PIN abort)
                                       ▼                         ▼
                                  COMPLETED                   ABORTED
```

Transitions are idempotent and guarded by current state. Only `ACTIVE` has blocking/lock/music active. Entering `ACTIVE` applies blocking exactly once; leaving `ACTIVE` restores exactly once (tracked with a persisted flag to survive restarts).

---

## 8. Android APIs and permissions (implementation reference)

| Capability | API / permission | Notes |
|---|---|---|
| App detection | AccessibilityService + `BIND_ACCESSIBILITY_SERVICE`; UsageStatsManager + `PACKAGE_USAGE_STATS` | User must grant manually. |
| App disable/restore | DevicePolicyManager `setApplicationEnabledSetting` | Device/profile owner only. |
| Kiosk pinning | `startLockTask()` + `setLockTaskPackages` | Owner mode for true no-exit. |
| Overlay | `SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY` | Approval required on API 29+. |
| Notifications | `POST_NOTIFICATIONS` (API 33+), ongoing notification | Foreground service requirement. |
| Foreground service | `FOREGROUND_SERVICE` (+ media/special-use type on API 34+) | Declare types correctly. |
| Exact alarm | `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, `setExactAndAllowWhileIdle` | Version & policy dependent. |
| Boot restore | `RECEIVE_BOOT_COMPLETED` | Receiver declares permission. |
| Battery | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | OEM caveats. |
| Local audio | `ACTION_OPEN_DOCUMENT` + persisted URI permission | `takePersistableUriPermission`. |
| Provisioning | `DeviceAdminReceiver` + `device_admin.xml` + `adb shell dpm set-device-owner` or QR | See prerequisite.md. |
| Web filtering (P2) | `VpnService` + `Builder` | Optional/advanced. |

---

## 9. UX flows (summary)

1. **First run:** welcome → explanation of two modes → onboarding checklist with status + deep links → done.
2. **Create session:** name → duration/schedule → pick apps → pick feature targets → pick music → pick screensaver → optional PIN → Save / Start.
3. **Active session:** screensaver + countdown; notification shows name/time; no cancel control; blocked attempts show brief overlay message.
4. **End:** automatic summary screen → History/Stats.
5. **Settings:** permissions status, PIN, markers, whitelist, profiles, provisioning help.

---

## 10. Edge cases and error handling

- Permission revoked mid-session → stop interception safely, restore state, prompt user (in personal mode; owner mode should prevent revocation).
- App update changes Reels/Shorts UI → markers no longer match → block silently fails; markers configurable; document limitation.
- Battery/OEM kills service → boot/alarm/receiver recovery; if impossible in personal mode, surface honest warning.
- Device reboot mid-session → state restored from DataStore/Room.
- Two alarms/duplicate finish calls → idempotence prevents double restore.
- User tries to disable/uninstall in personal mode → possible; documented as a limitation.
- Streaming URL offline → playback fails gracefully; session timer continues.
- Attempt to disable critical package → rejected by safety denylist; user warned.
- Provisioning fails (accounts on device) → clear message with instructions.

---

## 11. Acceptance criteria (testable)

- **AC-1:** A session named "Test Focus" with 1-minute duration can be created, appears in list, and shows that name in notification and overlay.
- **AC-2:** Countdown decrements accurately and reaches zero; `finishSession()` runs exactly once.
- **AC-3:** Owner mode: a blocklisted app is disabled at start (icon gone, cannot launch) and restored at end to its prior state.
- **AC-4:** Personal mode: opening a blocklisted app triggers HOME and the block overlay within ~1s.
- **AC-5:** Detecting a Reels/Shorts surface triggers dismissal/block and increments a BlockEvent.
- **AC-6:** No UI control can end an active session before deadline; PIN abort works when configured.
- **AC-7:** At deadline: audio stopped, lock/overlay released, apps re-enabled, summary with COMPLETED shown.
- **AC-8:** Reboot during active session: state restored and correct deadline honored.
- **AC-9:** All three music sources play; fades occur; local URI permission survives app restart.
- **AC-10:** Screensaver covers the screen, stays awake, and is the pinned activity in owner mode.
- **AC-11:** Missing permissions produce explanatory UI, not crashes.
- **AC-12:** History/stats reflect completed sessions and block counts.

---

## 12. Out of scope / future

- Cross-device sync / accounts.
- AI-generated focus music.
- Wear OS companion.
- Advanced VPN-based web filtering is P2; feature detection via Accessibility is the MVP approach.
- Enterprise MDM-style remote policy management.

---

## 13. Risks

- Android platform restrictions: true hard lock requires device ownership; personal mode cannot fully enforce.
- Accessibility-based feature detection is fragile across app updates/locales.
- OEM battery managers may kill services; mitigation is best-effort.
- Play Store policy on accessibility/device-admin apps may block distribution; plan sideload/enterprise.
- Device-owner provisioning is destructive to normal phone setup; must be clearly explained and reversible.
