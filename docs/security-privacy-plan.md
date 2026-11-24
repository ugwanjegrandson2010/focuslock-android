# Security & Privacy Plan — Focus Lock

The app requests powerful permissions (Accessibility, Device Owner, overlay). This plan keeps that power scoped, transparent, and safe.

---

## 1. Data classification

| Data | Sensitivity | Storage | Transmission |
|---|---|---|---|
| Session names, durations, history | Low (local) | Room | None |
| Blocklist / whitelist packages | Low | Room/DataStore | None |
| Music profiles (streaming URL, local URI) | Low–Medium (URL may encode private link) | Room | Only to user-specified audio host |
| Emergency PIN | High | Hashed (Android Keystore-backed key) in DataStore | Never |
| Permission/provisioning status | Low | In-memory / DataStore | None |
| Block events / stats | Low | Room | None |

No analytics, telemetry, crash-report uploads, accounts, or cloud sync. The only outbound network traffic is streaming audio to a URL the user explicitly supplies.

---

## 2. Threat model (simplified)

| Threat | Mitigation |
|---|---|
| Attacker reads PIN from storage | Hash + Keystore-backed key; never log PIN; no plaintext in DataStore/bundle |
| Malicious app tricks accessibility service | Only act on configured packages/markers; no arbitrary action execution from untrusted input |
| Accidental disable of critical system app | Critical-package denylist; validate package names against installed packages and patterns |
| Streaming URL abuse / data exfiltration | Validate scheme (http/https), no credentials in APK, user-supplied only; document risk of private URLs |
| Eavesdropping on other apps' data via accessibility | Service only inspects window metadata/text for markers; never stores or transmits content; document this in privacy notice |
| Tampering with persisted active state | Write intent before side effects; validate state on restore; guard transitions |
| Reverse engineering / key extraction | No embedded secrets; release signing; R8/ProGuard obfuscation (optional) |
| Device left locked after crash | Watchdog restore on launch/boot; safe de-provision path |

---

## 3. Permission minimization

- Personal mode requests: Accessibility, overlay, notifications, usage access, battery exemption, exact alarms — each with an in-app purpose sentence before the deep link.
- Owner mode is **opt-in** and only provisioned on a dedicated device; app never silently attempts provisioning.
- Privileged APIs called only after `DevicePolicyManager.isDeviceOwnerApp()` returns true.
- Permissions are checked at every use point, not just onboarding, because they can be revoked.

---

## 4. PIN handling

- Set during session creation or settings; 4–6 digits.
- Store `hash(PIN + salt)` using a key in Android Keystore; salt stored alongside hash.
- Compare in constant time; rate-limit repeated wrong attempts (e.g., exponential backoff / temporary lock).
- No PIN = no abort path (hard lock) — state this clearly in UI.
- PIN is never included in logs, notifications, screenshots, or analytics.

---

## 5. Validation rules

- **Package names:** must match Android package pattern and be among installed packages before being added to a blocklist; reject critical/system packages from the denylist.
- **Streaming URLs:** only `http`/`https`; reject `file://` and other schemes; do not auto-fetch arbitrary URLs.
- **Local audio URIs:** require persisted read permission via `ACTION_OPEN_DOCUMENT` + `takePersistableUriPermission`; drop stale URIs gracefully.
- **Feature markers:** user-editable plain strings; cap length/count to avoid abuse; no regex from user input (simple substring matching only).

---

## 6. Logging policy

- Use a dedicated log tag; log state transitions, permission status changes, and side-effect results.
- Never log: PIN, full audio URLs with credentials, notification content, app content text, personal identifiers.
- Debug logs stripped/guarded in release builds.

---

## 7. Dependencies and supply chain

- Use official AndroidX/Jetpack/Media3 libraries from Maven Central/Google Maven.
- Keep dependencies updated; review for known vulnerabilities before release.
- No third-party SDKs that phone home; no ad SDKs.

---

## 8. Distribution integrity

- Sign release APK/AAB with a dedicated keystore stored outside version control.
- Publish checksum (SHA-256) of release APK for sideload users.
- Document that device-owner provisioning must happen only with the official signed APK.
- Provide de-provisioning instructions so users retain control.

---

## 9. Privacy notice outline (for README / in-app About)

- What data is collected: none transmitted; all session data stays on device.
- Why Accessibility is used: detect blocked apps/features and return user to focus session; no message/content reading or transmission.
- Why Device Owner is used (optional): disable distracting apps and pin focus screen; user controls provisioning and de-provisioning.
- Audio: streaming URLs are contacted only when the user selects a streaming profile.
- Contact/licensing: bundled audio attribution; repository link.
