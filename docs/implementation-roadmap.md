# Implementation Roadmap — Focus Lock

Work breakdown of `prompt.md` §6 into executable phases with dependencies and exit criteria. Build order is mandatory; each phase leaves the app compiling and runnable.

---

## Milestones

| Milestone | Phases | Outcome |
|---|---|---|
| M0 — Foundation | P0 | Project builds, installs, permissions/onboarding skeleton works |
| M1 — Core loop | P1–P2 | Named session can be created, timed, and finished |
| M2 — Enforcement | P3 | Blocking + lock work in both modes |
| M3 — Experience | P4–P5 | Music, screensaver, full focus experience |
| M4 — Finish & ship | P6–P7 | Stats, settings, hardening, docs, tested release |

---

## P0 — Scaffolding / prerequisites (M0)

**Goal:** compilable project with all declarations and baseline navigation.
**Depends on:** toolchain verified (`prerequisite.md` §3/§11).
**Tasks:**
1. Create Kotlin + Compose project, version catalog, modules (`prompt.md` §5).
2. Hilt application + DI modules.
3. Room database, entities, DAOs skeleton; DataStore module.
4. Manifest: AccessibilityService (+ config XML), DeviceAdminReceiver (+ `device_admin.xml`), TimerService, MusicService, BootReceiver, all permissions.
5. Theme, strings, icons, baseline screens: Onboarding, Sessions, Active Focus, History, Settings.
6. Build and install check.

**Exit criteria / DoD:** `./gradlew assembleDebug` passes; APK installs; app opens; onboarding shows real permission status; no runtime crashes.

**Effort:** M.

---

## P1 — Data + session UX (M1)

**Goal:** users can create, name, schedule, and list sessions.
**Depends on:** P0.
**Tasks:**
1. Room entities: FocusSession, BlockedApp, MusicProfile, BlockEvent + migrations strategy.
2. Repositories + `SessionRepository` with Flow observation.
3. Create/Edit session screen: name validation, duration/schedule picker, app picker (installed packages), feature targets, music profile, screensaver, PIN.
4. Session list + history screens; duplicate-as-template.
5. `ActiveSessionManager` state machine skeleton (SCHEDULED/ACTIVE/COMPLETED/ABORTED) with transitions.

**DoD:** Can create named session, see it in list, edit/duplicate; state machine unit tests pass; Room instrumentation test for CRUD passes.

**Effort:** L.

---

## P2 — Timer service (M1)

**Goal:** reliable countdown surviving background/doze/reboot.
**Depends on:** P1.
**Tasks:**
1. `TimerService` foreground service + ongoing notification with name/countdown.
2. Deadline math with `elapsedRealtime`; ticking StateFlow.
3. `AlarmScheduler` with `setExactAndAllowWhileIdle`.
4. `BootReceiver` restore logic from persisted state.
5. Notification permission + foreground service type handling by API level.

**DoD:** Countdown accurate; alarm fires under doze; reboot restores session; AC-1/AC-2/AC-8 from spec pass.

**Effort:** L.

---

## P3 — Blocking engine (M2)

**Goal:** hybrid blocking + hard lock in both modes.
**Depends on:** P2.
**Tasks:**
1. `FocusAccessibilityService`: foreground detection, feature-marker scan, dismiss actions, BlockEvent logging.
2. `OverlayController`: full-screen, keep-awake, input-intercepting overlay for personal mode.
3. `AppDisabler`: owner-mode disable/restore with critical-package denylist and original-state storage.
4. `LockTaskController`: owner-mode lock task + feature flags.
5. `BlockingCoordinator`: mode detection and strategy selection.
6. Permission onboarding deep links fully wired.

**DoD:** AC-3/AC-4/AC-5/AC-6 pass on a physical device in both applicable modes; de-provisioning tested.

**Effort:** XL (highest risk).

---

## P4 — Audio (M3)

**Goal:** focus music from all three sources.
**Depends on:** P3 (needs active-session lifecycle).
**Tasks:**
1. `MusicController` with Media3/ExoPlayer + MediaSessionService.
2. Source resolver: bundled assets, persisted local URIs, streaming URLs.
3. Fade-in/out, audio focus, headset handling, lock-screen controls.
4. Music profile management UI.

**DoD:** AC-9 passes; local URI survives restart; fades observed; no crash offline.

**Effort:** M.

---

## P5 — Screensaver (M3)

**Goal:** immersive focus display.
**Depends on:** P3, P4.
**Tasks:**
1. `ScreensaverActivity` Compose visuals (gradient, breathing circle, rain) with animation.
2. Immersive sticky + keep screen on; hide notification content.
3. Wire as lock-task activity (owner) / overlay host (personal).

**DoD:** AC-10 passes; pinned in owner mode; overlay shown in personal mode.

**Effort:** M.

---

## P6 — End orchestration, stats, summary (M4)

**Goal:** clean shutdown and reporting.
**Depends on:** P2–P5.
**Tasks:**
1. Idempotent `FinishSessionUseCase` with persisted guard.
2. Summary screen (name, planned/actual, status, block counts).
3. History aggregation, total focus time, streaks, per-app stats.
4. Alarm/boot replay safety tests.

**DoD:** AC-7/AC-12 pass; no double-restore under replay.

**Effort:** M.

---

## P7 — Settings, hardening, tests, docs (M4)

**Goal:** production-quality release candidate.
**Depends on:** P6.
**Tasks:**
1. Settings: PIN, whitelist, configurable feature markers, profiles.
2. Permission revocation handling and OEM guidance.
3. Unit tests (state machine, deadline math, use-cases), instrumentation tests (Room), Compose UI tests.
4. `README.md` with build, provisioning, de-provisioning, manual test checklist.
5. Final manual test pass per `test-plan.md`; risk register review.

**DoD:** All acceptance criteria pass; zero known P0/P1 bugs; docs complete.

**Effort:** L.

---

## Cross-cutting concerns (every phase)

- Keep OS calls behind interfaces; write fakes for tests.
- Version-guard permissions/service types/API behavior.
- Persist intent before side effects; maintain idempotency.
- All strings in `strings.xml`; honest copy about personal-mode limits.
- No secrets, no analytics, no root.
- Update `spec.md`/`architecture.md` if design changes.

---

## Overall Definition of Done

- All P0–P7 phases complete with their DoD satisfied.
- All AC-1..AC-12 from `spec.md` verified on a physical device.
- `README.md`, provisioning/de-provisioning docs, and test results delivered.
- Known limitations documented (accessibility heuristic, personal mode, OEM battery managers, Play policy).
