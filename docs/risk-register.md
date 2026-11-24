# Risk Register — Focus Lock

Living document. Review at each milestone in `implementation-roadmap.md`. Severity = Likelihood × Impact (H/M/L).

---

## 1. Top risks

| ID | Risk | Likelihood | Impact | Severity | Mitigation | Contingency / trigger |
|---|---|---|---|---|---|---|
| R-01 | Android blocks true app force-stop / no-exit in personal mode | Certain | High | **H** | Two-tier design; owner mode for true enforcement; honest UI copy | If owner mode not provisioned, document personal mode as best-effort; no false claims |
| R-02 | Accessibility feature detection breaks after Instagram/YouTube updates | High | High | **H** | Configurable markers; locale variants; clear "best-effort" labeling | Ship marker updates via settings; P2 VPN fallback for browsers |
| R-03 | OEM battery managers kill timer/blocking services | Medium | High | **H** | Foreground service + battery-exemption request + exact alarm + boot receiver | Per-OEM setup guidance in onboarding/README; accept reduced reliability where OS blocks us |
| R-04 | Device-owner provisioning is destructive / hard for users | Medium | High | **H** | Optional mode; dedicated spare-device guidance; tested de-provision path | Provide QR provisioning instructions and factory-reset fallback |
| R-05 | Play Store policy rejects accessibility/device-admin app | Medium | High | **H** | Plan sideload/enterprise distribution from the start | Private distribution; document policy risk |
| R-06 | Crash mid-start leaves apps disabled or device locked | Low | High | **H** | Persist intent before side effects; idempotent finish; boot/alarm watchdog recovery | Recovery routine on app launch; emergency de-provision documented |
| R-07 | User revokes Accessibility/overlay permission mid-session | Medium | Medium | **M** | Permission status observer; safe restore; explanatory UI | Fall back to owner-mode enforcement if available; otherwise end session with warning |
| R-08 | Local audio URI permission lost after app update/reinstall | Medium | Low | **M** | Persist URI permissions; re-pick flow; bundled tracks as fallback | Show "re-select file" prompt |
| R-09 | Streaming audio fails offline | High | Low | **M** | Graceful error; timer continues; bundled/local alternatives | Silent fallback to bundled track if configured |
| R-10 | Name/field validation and time-zone/clock bugs | Low | Medium | **M** | Use elapsedRealtime; unit tests; validation rules | Fix in next release; no data loss since persisted |
| R-11 | Disabling a critical system package breaks device | Low | High | **H** | Critical-package denylist; validation of package names | Abort disable; warn user; de-provision/factory reset fallback |
| R-12 | Scope creep (VPN, extra themes, AI music) delays MVP | Medium | Medium | **M** | Strict phase order; P2 items explicitly deferred | Cut P2 items; ship MVP |
| R-13 | No Android SDK/JDK in current dev environment | Certain (current container) | High | **H** | Verify toolchain first (`prerequisite.md` §3/§11) before coding | Install SDK/JDK or move development to a capable machine |

---

## 2. Mitigation ownership

| Area | Owner role | Key action |
|---|---|---|
| Blocking/lock correctness | Blocking engineer | Mode checks, denylist, idempotent restore, R-01/R-06/R-11 |
| Detection heuristics | Blocking engineer | Configurable markers, version tests, R-02 |
| Timer robustness | Core engineer | Alarms/boot/foreground service, R-03 |
| Onboarding/permissions | UI engineer | Deep links, explanations, R-07 |
| Provisioning/docs | Release engineer | Runbook, de-provision, R-04/R-05 |
| Environment | Implementer | Toolchain verification, R-13 |

---

## 3. Go / no-go criteria for release

**Go only if:**
- All P0 acceptance criteria pass on a physical device.
- Owner-mode set/verify/de-provision tested end-to-end (if owner mode shipped).
- No double-restore after reboot/alarm replay.
- Critical-package denylist verified.
- README documents limitations, provisioning, de-provisioning, and OEM caveats.

**No-go if:**
- Blocking leaves apps disabled or device locked after a normal session end.
- Any crash on permission revocation.
- Owner mode cannot be safely de-provisioned.

---

## 4. Review cadence

- At the end of each milestone (M0–M4): re-score risks, close resolved ones, add newly discovered ones.
- Before release: full go/no-go review using §3.
