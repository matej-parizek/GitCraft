# GitCraft design system

## 1. Atmosphere & identity

A quiet, compact desktop Git workspace. Retain the existing purple identity and navy surfaces. The signature is a clear three-step hierarchy: review, stage, commit. This extends the existing Compose components, not a web dashboard. Primary persona: a developer learning Git; secondary: an experienced keyboard user reviewing many files.

## 2. Color

Use core/ui/theme/Colors.kt through MaterialTheme. DarkBackground #0F172A, DarkSurface #111827, DarkSurfaceVariant #1F2937, DarkBorder #334155, primary #8B5CF6, primary text #F8FAFC, secondary text #94A3B8. GitAdded #22C55E, GitModified #F59E0B, GitDeleted #EF4444; diff backgrounds #123524 and #3F1D1D. Selected rows use primary at 14%; hover uses onSurface at 8%. Status always has a letter or text in addition to color. Dividers use outline, not unspecified Material defaults.

## 3. Typography

Existing GitCraftTypography: system sans, 22/28 semibold screen title, 16/22 medium section title, 14/20 body, 12/16 supporting text. Diff and counts use FontFamily.Monospace at bodySmall. No downloaded font dependency.

## 4. Spacing & layout

Base spacing 4dp, steps 4/8/12/16/20/24/32/40/48. Desktop window defaults to 1280x840; minimum 1000x700. Sidebar 200dp; file pane 300dp; diff fills remaining width. Header and commit panel stay visible; files and diff own independent scrolling. Long paths ellipsize in lists and remain available in the diff header. Text content in diff horizontally scrolls. Use 4dp row radius and 8dp panel/input radius. No oversized cards.

## 5. Components

- SideBarItem: icon and sentence-case label, selected tint, keyboard focus and hover; navigation stays in existing StateFlow holder.
- ChangesToolbar: title, small guidance, stage/unstage actions, distinctly red discard. Busy disables mutations; refresh remains discoverable.
- ChangedFilesSection/ChangedFileRow: count, short guidance, status letter, path and named Stage/Unstage action; selected, busy, empty and conflict states.
- DiffViewer: file/type header, old/new line-number gutters, colored addition/removal lines, neutral context; independent scroll; loading, binary, too-large, error and no-selection states.
- CommitPanel: visible input label, multiline message, optional amend with explanation, staged count and local-commit explanation; disabled, busy, success and preserved-input error states.
- Confirmation dialog: operation-specific warning, Cancel first, destructive confirmation colored red. Amend explicitly warns about rewriting the latest commit.
- GuidedInfo: compact secondary copy; errors and success are dismissible and do not replace work areas.

## 6. Motion & interaction

Use standard Compose Material focus/press feedback. No decorative motion. Every action is keyboard reachable and named. File rows use selection semantics. Async refresh never clears a draft or replaces a newer selected diff. Poll repository status while open and provide manual refresh.

## 7. Depth & surface

Borders-only workspace with subtle tonal separation. One-dp outline dividers; no gradients, floating dashboard cards or decorative shadows. Material dialogs may use their standard elevation.

## 8. Accessibility constraints & accepted debt

Target readable desktop contrast, visible focus, keyboard navigation, labeled fields and actions. Destructive actions require explicit confirmation. Lists show status text in addition to color. Native desktop app: browser/mobile/Lighthouse gates do not apply. Screens outside Changes are explicitly marked as not implemented in this phase. Full screen-reader compatibility depends on Compose Desktop accessibility support and must not be claimed from screenshots.
