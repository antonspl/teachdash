Source: wondelai/skills, `refactoring-ui/SKILL.md` @c526558, MIT, licence in `.agents/design/licenses/wondelai-skills.LICENSE`; modified by teachdash.

# Refactoring UI

A practical, opinionated approach to UI design. Apply these principles when building frontend code, reviewing a slice or advising on visual improvements. `project-rules.md` wins over this file: every value below is a token from `frontend/src/styles/tokens.css`, and a new value becomes a new role-named token (C3).

## Core principle

**Check hierarchy in grayscale first. Colour comes last.** This forces proper hierarchy through spacing, contrast and typography before relying on colour as a crutch.

**The foundation:** great UI isn't about talent, it's about systems. Constrained scales for spacing, type, colour and shadows produce consistently professional results. In teachdash those scales are the tokens. Start with too much white space and remove; leave details (icons, shadows, micro-interactions) until layout and hierarchy work.

## The framework

Seven principles for building professional interfaces without a designer.

### 1. Visual hierarchy

**Core concept:** not everything can be important. Create hierarchy through three levers: size, weight and colour.

**Why it works:** when every element competes for attention, nothing stands out; deliberately de-emphasising secondary content makes primary content powerful by contrast.

**Key insights:**
- Combine levers, don't multiply: primary text is large OR bold OR dark, not all three; save "all three" for the single most important element
- Labels are secondary: form labels, table headers and metadata support the data, not compete with it; make them smaller or lighter (`--color-text-muted`), never all-caps (C2)
- Semantic colour is not visual weight: a muted secondary button often beats loud red for routine destructive actions

| Context | Hierarchy technique | Example |
|---------|---------------------|---------|
| **Form fields** | De-emphasise labels, emphasise values | Label in `--color-text-muted` above the value |
| **Dashboards** | Key figure large, context small | "12 lessons" in `--text-large`, "this week" in `--text-base` |
| **Tables** | De-emphasise headers, emphasise data | Headers lighter than the data, same case as the data |

**Patterns:**
- Three-level hierarchy: size (`--text-title`, `--text-large`, `--text-base`), weight (bold, medium, normal), colour (`--color-text`, `--color-text-muted`)
- Button hierarchy: the `src/ui/` Button variants: `primary` (filled), `secondary` (muted) and `danger` for their roles; one primary action per view

**Ethical boundary:** don't use hierarchy tricks to hide important information such as terms, deletions or cancellation options.

See `refactoring-ui-patterns.md` for components beyond static layout: interaction and focus states, form design, empty states, truncation, dialogs, menus and navigation.

### 2. Spacing and sizing

**Core concept:** use a constrained spacing scale, not arbitrary values. Spacing defines relationships: closer elements read as more related.

**Why it works:** arbitrary spacing creates inconsistency; a fixed scale forces deliberate decisions and harmonious layouts. Generous spacing feels calm; dense feels overwhelming.

**Key insights:**
- Use the role tokens: `--space-adjacent` (0.25rem), `--space-inline` (0.5rem), `--space-group` (0.75rem), `--space-block` (1.25rem), `--space-section` and `--space-page` (2rem); `--space-button-gap` (0.75rem) between buttons
- Start with too much white space, then remove; you'll almost never remove enough
- Spacing between groups must exceed spacing within groups
- Constrain widths: text and forms sit inside `--content-width` (short lines, under about 80 characters); full width is almost never right
- Buttons never touch fields, cards or each other (project rules)

| Context | Spacing strategy | Example |
|---------|-----------------|---------|
| **Icon + label** | Tight coupling | `--space-adjacent` keeps them connected |
| **Field to field** | Between blocks of a group | `--space-block` |
| **Box sections** | Inside a group | `--space-group` between title, content and actions |
| **Page sections** | Major sections | `--space-section` |

### 3. Typography

**Core concept:** use the type scale, constrain line heights by context, and keep one font family.

**Why it works:** a scale creates natural visual rhythm; tight line heights on headings and relaxed ones on body text improve readability in each context.

**Key insights:**
- Scale: `--text-base`, `--text-large`, `--text-title`; a new step is a new role-named token
- Headings: `--line-height-tight`; body: `--line-height`
- Avoid weights below 400 for body text; use bold (600-700) for emphasis, not everything
- One family: `--font-body`, the system stack (C4); hierarchy comes from size and weight, not from a second typeface

| Context | Typography rule | Example |
|---------|----------------|---------|
| **Page title** | `--text-title`, `--line-height-tight`, bold | The `src/ui/` Title |
| **Body text** | `--text-base`, `--line-height`, normal | Comfortable reading |
| **Hints and metadata** | `--text-base`, `--color-text-muted` | Secondary information |

### 4. Colour

**Core concept:** colour comes from the role tokens (`--color-text`, `--color-text-muted`, `--color-accent`, `--color-surface`, `--color-background`, `--color-control-border`, `--color-divider` and the `neutral`, `success`, `danger` pairs). Check the design in grayscale first.

**Why it works:** random colours clash; a fixed set of roles keeps the interface consistent, and each role has one meaning everywhere.

**Key insights:**
- Pick the token by role, not by look: an error uses `--color-danger-text`, not "some red"
- A missing role becomes a new role-named token in `tokens.css` (C3). When choosing its value: pure greys look lifeless, so tint them slightly; for a lighter variant raise lightness and lower saturation, for a darker one the reverse
- Contrast minimums (WCAG 2.2 AA): 4.5:1 for body text, 3:1 for large text (at least 18 pt, or 14 pt bold) and for UI component boundaries and focus indicators
- Never convey meaning by colour alone: add text or an icon

| Context | Colour strategy | Example |
|---------|---------------|---------|
| **Primary action** | Accent role | `--color-accent` with `--color-on-accent` |
| **Status** | Semantic pairs | `--color-success-background` with `--color-success-text` |
| **Text** | Two levels | `--color-text`, `--color-text-muted` |

Contrast, focus rings, keyboard use and screen readers are in `accessibility.md`.

### 5. Depth and shadows

**Core concept:** shadow conveys elevation. teachdash has one shadow, `--shadow-overlay`, used on overlays only (dialog, tooltip, menu, picker), and one `--radius`; they are ours, and changing them is an engineer decision (C5).

**Why it works:** the eye reads shadow as height above the page; a consistent shadow makes elevation legible, so people see what is raised, floating or background.

**Key insights:**
- Shadows only on overlays, through `--shadow-overlay`; a box is a surface fill plus a divider, not a shadow. The native `<dialog>` (`src/ui/Dialog`) also floats above the page through its backdrop
- Depth without shadows: `--color-surface` on `--color-background`, a `--color-divider` line, overlapping elements
- Don't overuse: if everything floats, nothing has depth

Motion is in the checklist (Animation) and C6.

### 6. Icons

**Core concept:** icons support text; size them deliberately.

**Key insights:**
- Size icons relative to the text next to them; use one set with a consistent stroke width and style (see `src/ui/PlusIcon`)
- Decorative icons get `aria-hidden="true"`; an icon-only button needs an accessible name
- Empty states are text plus a `src/ui/` action, with no image assets (C14): say what goes here and offer the action that fills it

### 7. Layout and composition

**Core concept:** don't centre everything. Use alignment and emphasis to create clear compositions.

**Why it works:** a consistent left edge gives the eye a fixed return point per line, so it costs less to scan; centred multi-line text moves that edge every line and slows reading.

**Key insights:**
- Left-align by default; centre only short headings, single-action prompts and empty states
- Vary visual treatment in lists: feature what matters, minimise the rest
- Use alignment to create relationships between unrelated elements
- Pages compose `src/ui/` components (Page, Section, List, Form, ButtonRow); they set no classes themselves (project rules)

| Context | Layout strategy | Example |
|---------|----------------|---------|
| **Forms** | One column, constrained width | Fields stacked inside `--content-width` |
| **Lists** | Cards with a title line, details, actions | `src/ui/` List and ListItem |
| **Content pages** | Constrained width, left-aligned | Text inside `--content-width` |

## Common mistakes

| Mistake | Why it fails | Fix |
|---------|-------------|------|
| **"Looks amateur"** | Too little white space, unconstrained widths | More white space, constrain content widths |
| **"Feels flat"** | No depth differentiation | `--color-surface` on `--color-background` with a `--color-divider` edge, no shadow on cards |
| **"Text is hard to read"** | Poor line height, too wide, low contrast | `--line-height`, `--content-width`, a darker text token |
| **"Everything looks the same"** | No visual hierarchy | Vary size, weight or colour between primary and secondary |
| **"Feels cluttered"** | Equal spacing everywhere | Group related items, larger gaps between groups |
| **"Colours clash"** | Colours picked by look | Use the role tokens |
| **"Buttons don't stand out"** | Low contrast with surroundings | One `primary` Button per view, `secondary` for the rest |
| **Raw values** | Values outside `tokens.css` breed inconsistency and fail `npm run lint` | Use the tokens; add a role-named token if one is missing |

## Quick diagnostic

| Question | If no | Action |
|----------|-------|--------|
| Does hierarchy read when squinting (blur test)? | Elements competing | Increase primary and secondary contrast |
| Does it work in grayscale? | Colour is a crutch | Strengthen size, weight and spacing hierarchy |
| Is there enough white space? | Probably not: most designs are too dense | Increase spacing, especially between groups |
| Are labels de-emphasised against values? | Labels competing with data | Smaller or lighter labels, never all-caps |
| Does spacing use only the `--space-*` tokens? | Arbitrary spacing is visual noise | Replace raw values with tokens |
| Is text width constrained? | Long lines tire readers | Keep text inside `--content-width` |
| Do colours have enough contrast? | Accessibility failure | Check against WCAG 2.2 AA; use a darker text token |
| Is elevation used sparingly? | Elements float at the wrong level | `--shadow-overlay` on overlays only |

## Further reading

- *Refactoring UI* by Adam Wathan and Steve Schoger (refactoringui.com): the full book with visual before and after examples. It replaces artistic talent with constrained systems: fixed scales for spacing, typography, colour and shadows.
