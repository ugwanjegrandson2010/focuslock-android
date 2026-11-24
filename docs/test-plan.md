# Test Plan — Focus Lock

Covers verification strategy for both capability tiers. Requirements source: `spec.md` §11. Environment setup: `prerequisite.md` §9.

---

## 1. Objectives

- Prove named sessions block apps/features, enforce the lock, play music, show a screensaver, and restore state exactly at deadline.
- Prove reliability across background, doze, reboot, and service kill.
- Prove no crashes or misleading UI when permissions are missing or revoked.
- Validate owner-mode provisioning and safe de-provisioning.

---

## 2. Test levels

| Level | Scope | Tooling |
|---|---|---|
| Unit | State machine, deadline math, use-cases, marker matching, source resolution | JUnit + Truth/MockK/Turbine |
| Instrumentation | Room CRUD/migrations, DataStore, permission status helpers | AndroidJUnit4 |
| UI | Compose navigation, create-session form validation, onboarding states | Compose UI test |
| Integration | Service + repository + blocking coordinator on device/emulator | Instrumented |
| Manual / field | Real blocking, lock task, overlay, OEM behavior | Physical device + adb |
| Provisioning | Device-owner set/verify/de-provision | adb + spare device |

---

## 3. Environment matrix

| Environment | Purpose | Required? |
|---|---|---|
| Emulator API 35 (Google APIs, arm64) | UI smoke, basic flows | Yes |
| Physical phone API 26+ (stock-like: Pixel) | Blocking, overlay, battery behavior | Yes |
| Physical phone with OEM skin (Samsung/Xiaomi/OnePlus) | OEM battery/overlay quirks | Recommended |
| Spare factory-reset phone (no accounts) | Owner-mode provisioning | Yes for owner mode |

Record for each run: device model, Android version, build number, app version, mode (owner/personal).

---

## 4. Manual test cases

### Core session flow
| ID | Scenario | Steps | Expected |
|---|---|---|---|
| TC-01 | Named session creation | Create "Test Focus", 1 min, pick apps/music/screensaver | Saved; name in list |
| TC-02 | Countdown | Start session; observe UI/notification | Decrements ~1s; name shown |
| TC-03 | Single finish | Watch logcat at deadline | `finishSession()` invoked once |
| TC-04 | Summary | After deadline | Summary with correct name, planned vs actual, COMPLETED |

### Blocking
| ID | Scenario | Steps | Expected |
|---|---|---|---|
| TC-05 | Owner: disable | Owner mode; start with blocked app | App icon gone, cannot launch; restored at end to original state |
| TC-06 | Personal: dismiss | Personal mode; open blocked app | HOME + overlay within ~1s; BlockEvent logged |
| TC-07 | Feature detection | Open Reels/Shorts during session | Dismissed/blocked; BlockEvent logged |
| TC-08 | Critical package safety | Add SystemUI/launcher to blocklist attempt | Rejected; warning shown |
| TC-09 | Overlay persistence | Try to dismiss overlay (back/home/recent) | Reappears immediately |

### Lock / bypass
| ID | Scenario | Steps | Expected |
|---|---|---|---|
| TC-10 | No exit control | Look for End/Exit while active | None present |
| TC-11 | PIN abort | Configure PIN; abort with wrong then right PIN | Wrong rejected; right restores state, ABORTED recorded |
| TC-12 | Lock task pin | Owner mode; try home/recents/status bar | Cannot leave until deadline |

### Audio / screensaver
| ID | Scenario | Steps | Expected |
|---|---|---|---|
| TC-13 | Bundled audio | Session with bundled profile | Plays; fades in/out |
| TC-14 | Local audio | Pick local file; restart app; start session | Plays; URI permission persisted |
| TC-15 | Streaming | Online then offline stream | Plays online; graceful failure offline; timer continues |
| TC-16 | Screensaver | Start session | Full-screen visual; screen stays on; pinned in owner mode |

### Robustness / recovery
| ID | Scenario | Steps | Expected |
|---|---|---|---|
| TC-17 | Reboot mid-session | Reboot while ACTIVE | Session restored; deadline honored; single finish |
| TC-18 | Doze | Screen off > 30 min past deadline | Alarm fires; state restored on wake |
| TC-19 | Service kill | `adb shell am force-stop <pkg>`; relaunch | State recovered, no double-apply (best effort in personal mode) |
| TC-20 | Permission revoke | Revoke overlay/accessibility mid-session | No crash; explanatory UI; safe restore |

### Provisioning
| ID | Scenario | Steps | Expected |
|---|---|---|---|
| TC-21 | Set owner | Fresh no-account device; adb set-device-owner | Succeeds; `dumpsys` shows owner |
| TC-22 | Provisioning failure | Device with accounts | Clear error message, no partial state |
| TC-23 | De-provision | clearDeviceOwnerApp / dpm remove-active-admin | Apps restored, lock released, owner removed |

---

## 5. Automation plan

- Unit tests for: state transitions, deadline math, marker matching, PIN hashing, app-state restore mapping, music source resolution.
- Instrumentation: Room DAO CRUD + migration, DataStore active-state restore.
- Compose UI: create-session validation (empty name, invalid duration), onboarding renders granted/denied states.
- Fake implementations of `AppDisabler`, `LockTaskController`, `MusicController`, `OverlayController` injected via Hilt test module.
- CI (optional): `./gradlew test` + `assembleDebug` on each commit.

---

## 6. Defect reporting template

```
ID / Title:
Mode (owner/personal):
Device / OS / build:
App version:
Steps to reproduce:
Expected:
Actual:
Logcat excerpt:
Severity (P0-P3):
```

---

## 7. Exit criteria

- All TC-01..TC-23 pass in applicable modes (owner-only cases skipped on personal with note).
- All AC-1..AC-12 in `spec.md` satisfied.
- Zero P0/P1 defects; P2 defects documented with workaround.
- Results recorded in `README.md` test section.

---

## 8. Known test limits

- Reels/Shorts detection may fail after an app update; retest with current app versions and update markers.
- OEM battery managers may prevent personal-mode reliability; document per-device findings rather than treating as app defect.
- Emulator cannot fully validate lock task/device owner/OEM behavior.
