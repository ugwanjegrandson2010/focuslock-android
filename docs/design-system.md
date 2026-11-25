# Focus Lock visual system (v1.2.0)

Guidance-only mode: the UX/UI Agent Skills kit is not installed here, so no gate ran and nothing below is measured. Contrast, WCAG status and responsive overflow are not verified.

## Taste preflight

- Domain: consumer focus tool
- Audience and tone: self-directed, calm but premium
- Mood: cinematic precision
- Motion depth: subtle feedback, ambient only in the lock screen
- Layout family: asymmetric editorial hero, hairline-separated configuration, stat band
- Reference anchor: Cyera AI Guardian (deep violet cosmos, #441363 and #6D2D93) and Obys "Grids" (precision measurement grid), plus Awwwards typography-led composition

## Inspiration, not copies

- Awwwards Site of the Day "Cyera AI Guardian" by Active Theory: deep violet palette, exploration framing, hero introduction and interaction challenges. We borrowed the palette and the idea of a navigable field, not its artwork.
- Awwwards "Grids" by Obys: fine grid and typographic hierarchy. We implemented a subtle 48dp measurement grid behind content.
- Awwwards collections "Typography in Web Design" and "Black and White Websites": display type at least 2.5x body, one leading element per screen.

## Tokens

`ui/design/DesignTokens.kt` is the single source of truth:

- Palette: Void, Surface, SurfaceRaised, Hairline, TextPrimary, TextSecondary, Accent, AccentDeep, AccentSoft, Success, Danger
- Type: Display 68sp, Hero 44sp, Headline 30sp, Title 20sp, Body 16sp, Label 13sp, Caption 12sp; tabular numerals on all countdowns
- Space: 4, 8, 16, 24, 32, 48dp
- Shape: panel 20dp, pill, field 12dp
- Motion: fast 150ms, normal 300ms, ambient 6000ms

## Components

`ui/design/Components.kt`: `CosmicBackground`, `GlassPanel`, `EyebrowText`, `DisplayHeading`, `NumericDisplay`, `PrimaryAction`, `StatusPill`, `HairlineRule`, `StatBlock`, `RowsLabel`.

House rules applied: one leading element per screen, no equal-weight card grids, hairline borders on all sides rather than one-sided accents, no emoji, no em dashes, status by text plus pill, numbers use tabular figures.

## Verification performed

- `:app:compileDebugKotlin` exit 0
- `:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` exit 0, 16 tests, zero failures
- Both APKs verified with `apksigner verify`
- No rendering or screenshot check was possible in this environment. Visual confirmation on a physical device is still required.

Skipped (kit not installed): `gate.mjs verify_states.mjs`, `accuracy_report.mjs`, `gate.mjs verify_responsive.mjs`, `verify_mobile_first.mjs`, `lint_native_select.py`, `slop_tells.mjs`, `check_no_emoji.py`, `lint_intent.mjs`, `contrast.py`, `design_systems.py`, `library.mjs`. Install with `npx ux-ui-agent-skills@3.1.1 init`.
