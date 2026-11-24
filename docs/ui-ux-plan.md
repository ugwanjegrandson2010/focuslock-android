# UI/UX Plan — Focus Lock

Defines screens, navigation, interaction patterns, and copy guidelines. Complements `spec.md` §9 and `architecture.md`.

---

## 1. Design principles

1. **Calm and minimal** — the app should feel like a focus tool, not a dashboard.
2. **Honest** — never imply personal mode can force-quit apps or prevent uninstall.
3. **Friction for exit, clarity for entry** — starting is easy; leaving during a session is not possible.
4. **Permission transparency** — every special access has a plain-language explanation before the settings deep link.
5. **Name-forward** — the session name is prominent everywhere (create, notification, lock screen, summary).

---

## 2. Navigation graph

```
Onboarding (first run)
   |
   v
Sessions (home / list)
   |-- Create/Edit Session --> App picker --> Feature targets --> Music --> Screensaver --> PIN (optional)
   |-- Active Focus (full-screen) --> Summary
   |-- History & Stats
   |-- Settings --> Permissions | Music profiles | Markers | Whitelist | Provisioning help | About
```

- Active Focus is a full-screen destination that takes over the task in owner mode.
- Back navigation is disabled while a session is active (no exit control).

---

## 3. Screen inventory

### 3.1 Onboarding
- Welcome + two-mode explanation ("Full mode needs one-time device setup; Standard mode works on any phone with a few permissions").
- Checklist with live status: Accessibility, Overlay, Notifications, Usage access, Battery, Alarms.
- Each row: purpose sentence + "Open settings" deep link + granted/denied badge.
- "Continue" enabled when minimum permissions granted or user acknowledges reduced behavior.

### 3.2 Sessions (home)
- Active session card (if any): name, big countdown, "Session in progress — no exit until time is up."
- "New focus session" primary action.
- Upcoming scheduled sessions list.
- Recent sessions with status chips.

### 3.3 Create / Edit session
- **Name** (required, 1–50 chars, inline validation).
- **Time**: duration presets (25/50/90 min) + custom; or start time + end time.
- **Apps to block**: searchable installed-app list with toggles; show warning for unsupported/critical packages.
- **Features**: Reels / Shorts toggles + note "best-effort detection."
- **Music**: profile selector (bundled / local file / streaming URL) with volume.
- **Screensaver**: style selector with preview.
- **Emergency PIN** (optional, 4–6 digits) with confirmation field.
- Buttons: Save (schedule), Start now.

### 3.4 Active Focus (full-screen)
- Session name, large countdown, music title (if any), screensaver visual behind.
- Minimal status line: "Focus session active · Apps blocked".
- No cancel/back/exit affordance.
- Blocked-attempt toast/overlay: "Blocked — stay focused. <name> ends at <time>."
- Owner mode: pinned via lock task; personal mode: overlay host.

### 3.5 Summary
- Status badge (Completed / Aborted), name, planned vs actual duration, block counts.
- Actions: Done, Repeat session, View stats.

### 3.6 History & Stats
- List of sessions with name/date/duration/status.
- Aggregate cards: total focus time, completed sessions, streak, most-blocked app.

### 3.7 Settings
- Permissions status (re-enter onboarding).
- Music profiles management.
- Feature markers editor (text list) with reset to defaults.
- Whitelisted packages.
- PIN change/remove.
- Provisioning help (owner mode steps + de-provision warning) with link to `prerequisite.md` content in-app.
- About / licensing (including bundled audio attribution).

---

## 4. Design system (Material 3)

- **Color:** dark, low-stimulation palette for focus screens (deep indigo/charcoal with a single accent for countdown). Light theme for management screens.
- **Typography:** large bold countdown; readable body; name in title medium/large.
- **Components:** M3 cards, switches, chips, time pickers, FAB for "New session".
- **Motion:** gentle fades for screensaver transitions; no flashing or high-stimulation animation.
- **Icons:** custom vector/adaptive icon; consistent icon set.
- All text from `strings.xml`; support rtl and long names gracefully (ellipsis).

---

## 5. Copy guidelines

- Use "Focus session active" language, not punishment framing.
- Permission explanations example: "Accessibility access lets Focus Lock notice when a blocked app opens and return you to your session. It is never used to read your messages."
- Personal-mode honesty: "On a standard install, Focus Lock can dismiss blocked apps and cover the screen, but it cannot uninstall-proof itself or forcibly stop every app. Full enforcement requires one-time device-owner setup."
- Feature note: "Reels/Shorts detection is best-effort and may change when apps update."
- Error states: state what happened and the next action ("Overlay permission is off. Open settings to re-enable it.").

---

## 6. Accessibility (a11y) of the app UI

- Content descriptions on all interactive controls; semantic labels for countdown.
- Sufficient contrast in both themes; support large text and TalkBack.
- Haptic/audio feedback optional for block events.
- Note: the app *uses* the Accessibility API for blocking, but its own UI must also be accessible.

---

## 7. States to design

- Empty states: no sessions yet, no history, no music profiles.
- Loading: app list scanning, permission check.
- Error: permission missing, provisioning failed, stream offline, disable failed.
- Disabled states: Start disabled until name valid; PIN abort disabled when no PIN set.

---

## 8. Wireframe notes (text)

- **Active Focus:** full-bleed visual; centered name at top, oversized countdown in middle, thin status line at bottom. No nav bar, no exit control.
- **Create:** scrollable form with section headers; sticky bottom action bar (Save / Start now).
- **Onboarding:** single-column checklist with progress indicator at top and one deep-link button per permission.
- **Summary:** status badge + name at top, three stat tiles, primary "Done" with secondary "Repeat".
