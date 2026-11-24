# Release & Operations Plan — Focus Lock

Covers versioning, builds, signing, distribution, provisioning operations, and support. References: `prerequisite.md` §5/§10, `risk-register.md`.

---

## 1. Versioning

- Semantic-ish versioning: `MAJOR.MINOR.PATCH` (e.g., 1.0.0).
- `versionCode` increments monotonically for every release build.
- `versionName` shown in Settings → About and in the README.
- Change log kept in `CHANGELOG.md` (created at release time).

---

## 2. Build variants

| Variant | Purpose | Signing |
|---|---|---|
| `debug` | Development, logcat, install via adb | Debug keystore |
| `release` | Distribution | Dedicated release keystore (never committed) |

- Enable R8/minify and resource shrinking for release (optional but recommended).
- Strip/guard debug logs in release.
- Verify release builds still pass all acceptance criteria in `spec.md`.

---

## 3. Signing

1. Generate keystore (Android Studio → Generate Signed Bundle/APK, or `keytool`).
2. Store keystore + passwords outside version control (password manager / secure CI secrets).
3. Configure signing in Gradle via `keystore.properties` (gitignored).
4. Publish SHA-256 checksum of the signed APK.

---

## 4. Distribution channels

Primary (due to Play policy risk — see `risk-register.md` R-05):
- **Sideload:** signed APK + README with install instructions ("Install unknown apps" guidance).
- **Enterprise/private:** MDM or private store for owner-mode devices.

Secondary:
- Play Store only after a policy review of Accessibility/device-admin usage. If pursued, prepare a policy declaration explaining the accessibility use is for user-requested self-control, not surveillance.

---

## 5. Provisioning operations runbook (owner mode)

**Before provisioning:** dedicated device, factory reset, no accounts, USB debugging on, signed APK installed.

**Set owner:**
```bash
adb shell dpm set-device-owner <applicationId>/.admin.DeviceAdminReceiver
adb shell dumpsys device_policy | grep -i owner   # verify
```

**In-app setup after provisioning:** enable Accessibility, overlay (if used), notifications, battery exemption, alarms; confirm mode badge shows "Full mode".

**De-provision:**
1. Ensure no active session; app restores any disabled apps and stops lock task.
2. `adb shell dpm remove-active-admin <applicationId>/.admin.DeviceAdminReceiver` or in-app `clearDeviceOwnerApp()`.
3. Verify with `dumpsys`; factory reset as fallback.

**Incident response (device stuck locked/apps disabled):**
- Use adb to remove active admin / clear owner; if unavailable, factory reset.
- Document this prominently in README.

---

## 6. Support & troubleshooting

README section should include:
- Which mode the app is in and how to tell.
- Permission checklist and deep links.
- OEM battery/overlay setup tips (Samsung/Xiaomi/OnePlus: allow background, autostart, display over apps).
- Reels/Shorts detection not working → edit markers in Settings; app updates may require marker changes.
- Music not playing → check source permissions/URL; bundled tracks as fallback.
- Session did not end → check alarms/battery exemption; reboot recovery.
- How to report a bug (template from `test-plan.md` §6).

---

## 7. Rollback

- Sideload distribution: users can install an older signed APK (versionCode lower installs may need uninstall first) or de-provision and uninstall.
- Owner mode rollback always begins with de-provisioning + app state restore.
- Keep previous signed release artifacts and checksums.

---

## 8. Documentation set at release

- `README.md` — overview, build, install, modes, provisioning/de-provisioning, limitations, testing summary, support.
- `CHANGELOG.md` — version history.
- `prerequisite.md`, `prompt.md`, `spec.md`, `architecture.md`, `implementation-roadmap.md`, `test-plan.md`, `ui-ux-plan.md`, `risk-register.md`, `security-privacy-plan.md`, this file — plan set under `/root/.opencode/plan`.
- Bundled audio license/attribution file.

---

## 9. Release checklist

- [ ] All phases complete with DoD met.
- [ ] All acceptance criteria (spec §11) verified on physical device.
- [ ] All test cases (test-plan §4) executed; results recorded.
- [ ] Go/no-go review from risk-register §3 passed.
- [ ] Release build signed; checksum generated.
- [ ] README + CHANGELOG finalized; limitations documented.
- [ ] Provisioning/de-provisioning runbook tested on spare device.
- [ ] Artifacts archived for rollback.
