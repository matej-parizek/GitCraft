# Suggestion-popup visual fidelity review

Recommendation: **REQUEST_CHANGES**

## Scope and evidence inspected

- Intent and visual contract: `DESIGN.md` §§2, 5, 6, and 7.
- Fresh actual captures (both written 2026-09-08 23:10, after the reviewed source):
  - `build/visual-qa/suggestion-popup-above.png`
  - `build/visual-qa/suggestion-popup-below.png`
- Failure-reference screenshots (comparison evidence only):
  - `C:/Users/matpa/AppData/Local/Temp/codex-clipboard-79c00614-ca9b-41ee-b278-8d6001404dfb.png`
  - `C:/Users/matpa/AppData/Local/Temp/codex-clipboard-b946746d-8a13-4206-af75-e8337d54a6c8.png`
- Runtime implementation: `src/main/kotlin/cz/parizmat/gitcraft/feature/toolwindow/ui/TerminalSwingHost.kt` and `src/main/kotlin/cz/parizmat/gitcraft/feature/toolwindow/model/SuggestionPopupPlacement.kt`.
- Runtime capture test: `src/test/kotlin/cz/parizmat/gitcraft/feature/toolwindow/ui/TerminalSuggestionPopupWindowTest.kt`.
- Placement unit test: `src/test/kotlin/cz/parizmat/gitcraft/feature/toolwindow/model/SuggestionPopupPositionerTest.kt`.
- Existing design tokens: `src/main/kotlin/cz/parizmat/gitcraft/core/ui/theme/Colors.kt`.

## What the fresh evidence proves

- The popup is a live KetraTerm Swing component moved into a non-focusable `JWindow`, not a screenshot or background-image substitute (`TerminalSwingHost.kt:54-59`, `213-234`).
- Above and below captures show all eight rows completely rendered; the popup flips vertically and is no longer clipped at the owner/SwingPanel boundary. This resolves the failure seen in the supplied screenshots.
- The captures are ARGB composites. Sampled pixels outside the popup have alpha 0 and rounded popup-edge pixels have partial alpha (for example, above capture `(0,0)=0`, `(8,8)=247`; below capture `(8,365)=247`). The corners are transparent rather than an opaque rectangular window.
- The dark navy canvas, muted slate popup, blue selected row, white monospaced primary text, labels, and semantic dots retain the intended terminal style. No CJK content is present.

## Findings

### CRITICAL

None.

### HIGH

1. [product] The terminal palette bypasses the existing design-token layer with duplicated literals at `TerminalSwingHost.kt:285-290` (`#F8FAFC`, `#0F172A`, `#334155`, `#8B5CF6`). The same values are already declared in `core/ui/theme/Colors.kt`, while `DESIGN.md` §2 explicitly requires that source. This makes a theme/token update drift silently from the terminal popup and fails the token-driven styling requirement.

### MEDIUM

1. [product] The live popup is vertically adjacent to the caret but horizontally displaced left in both fresh captures. The actual caret’s purple rectangle is x=38–44; the popup left edge begins at x=8, a 30 px offset. This is visibly unlike the supplied healthy-intent screenshot, where the popup begins at the typed caret. The placement coordinate is derived at `TerminalSwingHost.kt:249-250`; the existing integration test asserts only the vertical relation (`TerminalSuggestionPopupWindowTest.kt:60`, `68`), so this regression has no horizontal guard.

### LOW

None.

## Blockers before approval

1. The palette must be driven by the existing GitCraft color tokens rather than duplicate hexadecimal literals.
2. The popup’s x-origin must align with the active rendered caret; fresh above and below visual evidence must also prove that alignment.

## Review conclusion

The key clipping/transparent-corner behavior is genuinely improved and the popup is a live component. The remaining token bypass and measured horizontal caret offset prevent approval.
