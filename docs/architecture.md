# Technical Architecture — Focus Lock

Companion to `prompt.md` (build brief), `spec.md` (requirements), `prerequisite.md` (setup). This document explains **how the system is structured and why**.

---

## 1. Architectural principles

1. **Single source of truth** for session state (`ActiveSessionManager` + repository).
2. **Idempotent side effects** — every OS mutation (disable app, lock task, start audio) can be safely repeated or replayed after reboot.
3. **Capability tiering** — privileged paths compile and run in both modes; a mode check gates them, never a runtime assumption.
4. **OS interactions behind interfaces** — blockers, lockers, audio, and app-state controllers are abstract so core logic is unit-testable.
5. **Persistence as recovery** — deadline and "blocking applied" flags are persisted before side effects, so any crash recovers deterministically.
6. **No root, no hidden APIs** — only documented Android APIs; honest about what cannot be done.

---

## 2. Module dependency graph

```
             feature/session      feature/focus
                    \                /
                     \              /
                      core/domain  (use-cases, state machine, models)
                           |
        +------------------+------------------+
        |          |            |             |
  core/data  core/timer  core/blocking  core/audio
        \          |            |             /
         \         |            |            /
          core/notifications   core/common (DI, utils)
                    \            /
                        app (wiring, nav)
```

Rules:
- `core/domain` depends on interfaces only, never on Android framework classes (except model primitives).
- Features depend on `core/domain` and Compose; they never talk directly to services.
- `core/common` holds DI modules and shared constants; no feature imports.
- `app` is the only module that knows all others (Hilt graph + navigation).

---

## 3. Component responsibilities

| Component | Responsibility | Key interface |
|---|---|---|
| `SessionRepository` | CRUD, history, settings persistence | `getSession`, `saveSession`, `observeActive` |
| `ActiveSessionManager` | Current state, transitions, exposes `StateFlow<SessionState>` | `start`, `finish`, `abort`, `state` |
| `TimerService` | Foreground countdown, notification, alarm scheduling | bound via `StateFlow`/broadcast |
| `AlarmScheduler` | Exact alarm at deadline; cancel/reschedule | `schedule(deadline)`, `cancel()` |
| `BootReceiver` | Restore sessions after reboot | manifest receiver |
| `BlockingCoordinator` | Selects strategy by mode; applies/removes blocking | `apply(session)`, `restore(session)` |
| `AppDisabler` | DevicePolicyManager enable/disable + exact state restore | `disable(apps)`, `restore(map)` |
| `FocusAccessibilityService` | Foreground app + feature-marker detection; dismiss | Android service + `performGlobalAction` |
| `OverlayController` | Full-screen overlay lifecycle (personal mode) | `show`, `hide` |
| `LockTaskController` | startLockTask/stopLockTask (owner mode) | `lock`, `unlock` |
| `MusicController` | Source resolution, Media3 player, fades | `play(profile)`, `stopWithFade()` |
| `ScreensaverActivity` | Immersive visual host (lock-task or overlay) | Activity |
| `FinishSessionUseCase` | Orchestrates shutdown exactly once | `invoke(reason)` |
| `PermissionManager` | Permission status + deep links | `status()`, `request()` |

---

## 4. State management and data flow

```
UI event -> ViewModel -> UseCase -> ActiveSessionManager -> StateFlow -> UI
                                      |
                          +-----------+-----------+
                          |           |           |
                     Room/DataStore  TimerService  BlockingCoordinator
```

- **SessionState** contains: `sessionId`, `name`, `status`, `remainingMs`, `deadlineElapsedRealtime`, `mode`, `permissionStatus`, `activeBlocking`, `blockCounts`.
- UI is a pure function of `SessionState` + events (unidirectional).
- TimerService writes remaining time into the shared flow; it does not own session truth.

---

## 5. Threading model

- Main thread: UI, Compose recomposition, Activity/service lifecycle calls.
- `Dispatchers.IO`: Room, DataStore, file/URI access, network stream preparation.
- `Dispatchers.Default`: UI hierarchy marker matching (cheap), state computations.
- Timer tick: coroutine in foreground service using `delay()` aligned to second boundaries; no busy loop.
- Media playback: ExoPlayer's internal threads; volume fades via `setVolume` on a coroutine with small steps.
- All cross-component communication via `StateFlow`/`SharedFlow` or explicit use-case calls, never direct cross-thread mutation.

---

## 6. Key sequences

### 6.1 Start session
1. Validate config + permissions (or warn in personal mode).
2. Persist session `ACTIVE` with `deadlineElapsedRealtime` and `blockingApplied = false`.
3. Start `TimerService` (foreground notification with name + countdown).
4. Schedule exact alarm at deadline.
5. Start `MusicController` (fade-in).
6. `BlockingCoordinator.apply()`: persist each app's original state → disable (owner) or enable interception (personal) → set `blockingApplied = true`.
7. Show `ScreensaverActivity`; owner mode calls `startLockTask()`.

### 6.2 Detection & dismissal (personal mode)
1. AccessibilityService receives `TYPE_WINDOW_STATE_CHANGED`.
2. Resolve foreground package; if in blocklist → `GLOBAL_ACTION_HOME`, show overlay, log BlockEvent.
3. If feature markers match UI nodes → `GLOBAL_ACTION_BACK`/HOME, show overlay, log BlockEvent.

### 6.3 End session (deadline / PIN abort)
1. `FinishSessionUseCase` acquires idempotency guard (persisted `finishStarted` flag).
2. Stop music with fade-out.
3. `BlockingCoordinator.restore()` using saved original states; disable interception.
4. `unlock()` / hide overlay; return to home.
5. Persist status `COMPLETED` or `ABORTED`, actual duration, block counts.
6. Cancel alarm; stop TimerService; show summary UI.

### 6.4 Reboot recovery
1. BootReceiver reads active/scheduled sessions from Room/DataStore.
2. If deadline passed → run `FinishSessionUseCase` (restores state).
3. Else → restart TimerService + re-apply blocking using persisted flags (no double-apply).

---

## 7. Persistence strategy

| Data | Store | Why |
|---|---|---|
| Sessions, blocklists, events, profiles | Room (SQLite) | Relational, queryable history/stats |
| Active session id, deadline, flags, settings | DataStore Preferences | Small, atomic, fast restore at boot |
| PIN | Hashed in DataStore + Android Keystore key | No plaintext secret |
| Original enabled states | Room rows on `BlockedApp` | Exact restoration |

Write order rule: **persist intent before applying side effect** so a crash mid-start can be reconciled by recovery.

---

## 8. Error and recovery model

-Every side-effect wrapper returns `Result`; failures are logged, surfaced in UI status, and retried with backoff where safe.
- Stale blocking after crash → boot/alarm recovery re-runs restore.
- Permission revoked → personal mode stops interception, restores apps it can, prompts user; owner mode should make revocation impossible, but code must not crash.
- Service killed by OEM → alarm + boot receiver provide best-effort restoration; documented limitation.

---

## 9. Architecture decision records (ADRs)

**ADR-001 — Native Kotlin + Compose.** Deep OS integration (accessibility, device admin, lock task, media) is required; cross-platform frameworks add fragile bridges. Accepted.

**ADR-002 — Two capability tiers.** True hard lock requires device ownership; a normal install cannot force-close apps. Supporting both modes maximizes usefulness while being honest. Accepted.

**ADR-003 — Accessibility heuristics for Reels/Shorts.** No public API exposes "shorts surface"; node-text/resource-id matching is the only feasible mechanism and is explicitly best-effort and configurable. Accepted with risk.

**ADR-004 — Media3/ExoPlayer + MediaSession.** Unified local/bundled/streaming playback and system controls with one API. Accepted.

**ADR-005 — Room + DataStore.** Relational history plus lightweight atomic active-state restore. Accepted.

**ADR-006 — Idempotent finish with persisted flags.** Prevents double-restore and inconsistent state after reboot/alarm replay. Accepted.

**ADR-007 — No root.** Play Store/OEM compatibility and user trust outweigh marginal capabilities. Accepted.

**ADR-008 — Optional VPN as P2.** Browser-based short-form blocking is valuable but complex and battery-sensitive; deferred behind accessibility MVP. Accepted.

---

## 10. Non-functional design choices

- **Reliability:** deadline math from `elapsedRealtime`; exact alarm; boot receiver; idempotency.
- **Battery:** foreground service only while active; no wake locks except brief finish work; battery-exemption request.
- **Privacy:** no analytics/network except user-supplied audio URLs; no PII in logs.
- **Security:** validated package names/URLs; Keystore-backed PIN hash; critical-package denylist.
- **Testability:** interfaces for all OS interactions; fake implementations in unit tests.
