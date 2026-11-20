Source: pbakaus/impeccable, `skill/reference/critique-rubric.md` @b49177d, Apache-2.0, licence in `.agents/design/licenses/pbakaus-impeccable.LICENSE`; modified by teachdash.

# Critique rubric

A rubric for critiquing a screen or flow: cognitive load, Nielsen's heuristics and persona walk-throughs. The `frontend-reviewer` and the `frontend-researcher` read it on demand. `project-rules.md` wins over this file. There are no grades, ratings or severity levels: each problem found is a finding, triaged as C13 says.

## Cognitive load

Cognitive load is the total mental effort needed to use an interface. Overloaded users make mistakes, get frustrated and leave.

### Three types

- **Intrinsic load (the task itself):** complexity inherent to what the user is trying to do. You can't remove it, but you can structure it: break complex tasks into steps, give scaffolding (defaults, examples), show what's needed now and hide the rest, group related decisions.
- **Extraneous load (bad design):** effort caused by poor design choices. Remove it ruthlessly. Common sources: navigation that needs a mental map, unclear labels that make users guess, visual clutter, inconsistent patterns, needless steps between intent and result.
- **Germane load (learning):** effort spent building understanding. This is good load; it leads to mastery. Support it with gradual disclosure, consistent patterns, feedback that confirms understanding, and onboarding that teaches through action, not walls of text.

### Checklist

Check the interface against these eight items. Each failed item is a finding.

- [ ] **Single focus:** can the user finish their main task without distraction from competing elements?
- [ ] **Chunking:** is information shown in small groups (up to 4 items per group)?
- [ ] **Grouping:** are related items visually grouped (proximity, borders, shared background)?
- [ ] **Visual hierarchy:** is it clear at once what matters most on the screen?
- [ ] **One thing at a time:** can the user focus on one decision before the next?
- [ ] **Minimal choices:** are decisions simplified (up to 4 visible options at any decision point)?
- [ ] **Working memory:** does the user need to remember something from a previous screen to act on this one?
- [ ] **Progressive disclosure:** is complexity revealed only when the user needs it?

### The working memory rule

People hold about 4 items in working memory at once (Miller's law as revised by Cowan, 2001). At any decision point, count the distinct options, actions or pieces of information the user must weigh at the same time:

- up to 4: within limits;
- 5 to 7: at the edge; consider grouping or progressive disclosure;
- 8 or more: overloaded; users skip, misclick or give up.

In practice: one primary action and one or two secondary ones, the rest grouped; up to 5 top-level navigation items; one decision per screen where possible.

### Common violations

1. **The wall of options:** 10 or more choices with no hierarchy. Fix: group them, highlight the usual one, disclose the rest gradually.
2. **The memory bridge:** the user must remember step 1 to finish step 3. Fix: keep the context visible, or repeat it where it's needed.
3. **The hidden navigation:** the user must build a mental map. Fix: always show the current location (active states, progress).
4. **The jargon barrier:** technical or domain words force translation. Fix: plain language; define a domain term inline when it can't be avoided.
5. **The visual noise floor:** every element has the same weight. Fix: one primary element, two or three secondary, the rest muted.
6. **The inconsistent pattern:** similar actions work differently in different places. Fix: the same kind of action gets the same kind of UI (`src/ui/` components).
7. **The multi-task demand:** reading, deciding and navigating at once. Fix: sequence the steps.
8. **The context switch:** the user jumps between screens or dialogs to gather what one decision needs. Fix: put that information together.

## Nielsen's 10 heuristics

For each heuristic, check the screen for the points listed. Each gap is a finding.

### 1. Visibility of system status

Keep users informed about what's happening through timely feedback. Check for:
- loading indicators during requests;
- confirmation of user actions (save, submit, delete);
- progress indicators for multi-step processes;
- the current location in navigation (active states);
- form validation feedback next to the field, not only a summary.

### 2. Match between system and real world

Speak the user's language and follow real-world conventions. Check for:
- familiar words (no unexplained jargon);
- information in the order users expect;
- recognisable icons and metaphors;
- language that fits the audience (teachers and their students);
- a natural reading flow, most important first.

### 3. User control and freedom

Users need a clear way out of unwanted states. Check for:
- undo where an action can be undone;
- cancel on forms and dialogs;
- a clear way back (home, previous page);
- an easy way to clear filters, search and selections;
- a way out of long or multi-step processes.

### 4. Consistency and standards

Users shouldn't wonder whether different words or actions mean the same thing. Check for:
- the same words for the same things throughout;
- the same actions giving the same results everywhere;
- platform conventions (native elements, standard patterns);
- visual consistency (tokens and `src/ui/` components);
- the same interaction for the same kind of control.

### 5. Error prevention

Better than a good error message is a design that prevents the problem. Check for:
- confirmation or undo before destructive actions;
- constraints that prevent invalid input (native date inputs, selects);
- sensible defaults;
- clear labels that prevent misunderstanding;
- work kept when something fails (no wiped form).

### 6. Recognition rather than recall

Minimise memory load: make objects, actions and options visible. Check for:
- visible options, not buried in hidden menus;
- help in context (inline hints);
- recent items where they help;
- autocomplete and suggestions;
- text labels on icons.

### 7. Flexibility and efficiency of use

Accelerators, invisible to novices, speed up experienced users. Check for:
- keyboard paths for common actions (native controls, Enter to submit);
- bulk actions where people handle many items;
- recent items;
- shortcuts that don't complicate the basics.

### 8. Aesthetic and minimalist design

An interface should not show irrelevant or rarely needed information. Check for:
- only the information needed at each step;
- a clear visual hierarchy directing attention;
- purposeful colour and emphasis;
- no decoration competing for attention;
- focused, uncluttered layouts.

### 9. Help users recognise, diagnose and recover from errors

Error messages use plain language, name the problem precisely and suggest a fix (C10). Check for:
- plain-language messages (no error codes for users);
- the specific problem ("The email needs an @", not "Invalid input");
- a suggestion for what to do next;
- the error shown next to its source, with focus as web-client R14-S12 and R17-S3 say (C12);
- the user's input kept.

### 10. Help and documentation

Even when the system is usable without help, help is easy to find, task-focused and short. Check for:
- hints in context, at the point of decision;
- help organised by task, not by feature;
- short, scannable text;
- help reachable without leaving the current screen.

## Persona walk-throughs

Walk through the main user action as two or three of these personas. Each exposes failures a single "design director" view would miss. Report specific problems, not general concerns.

### 1. Impatient power user: "Alex"

**Profile:** experienced with similar products, expects efficiency, hates hand-holding. Skips instructions, looks for keyboard paths and bulk actions, leaves if anything feels slow or patronising.

**Test questions:** Can Alex finish the core task in under a minute? Can every step be done by keyboard? Do dialogs close with Escape? Can onboarding be skipped?

**Report:** forced tutorials; no keyboard path for main actions; slow animations that can't be skipped; one-at-a-time work where a batch would be natural; extra confirmations for low-risk actions.

### 2. Confused first-timer: "Jordan"

**Profile:** has never used this kind of product. Reads every instruction, hesitates before anything unfamiliar, takes every label literally, gives up rather than work it out.

**Test questions:** Is the first action clear within 5 seconds? Do all icons have text? Is there help at decision points? Do the words assume prior knowledge? Is there a clear way back at every step?

**Report:** icon-only navigation; unexplained jargon; no visible help; unclear next steps after an action; no confirmation that an action worked.

### 3. Accessibility-dependent user: "Sam"

**Profile:** uses a screen reader (VoiceOver, NVDA) and the keyboard only; may have low vision, a motor impairment or cognitive differences. Tabs through the page in order, relies on labels and headings, can't see hover-only cues, zooms to 200%.

**Test questions:** Can the whole main flow be done by keyboard? Is focus always visible? Do images that carry meaning have alt text? Does text meet WCAG 2.2 AA contrast (4.5:1)? Does the screen reader announce state changes (loading, success, errors)?

**Report:** click-only interactions; missing or invisible focus; meaning by colour alone; unlabelled fields or buttons; time limits with no way to extend; custom components that break screen-reader flow. Details are in `accessibility.md`.

### 4. Deliberate stress tester: "Riley"

**Profile:** pushes past the happy path on purpose: empty states, very long strings, special characters, emoji, going back, refreshing mid-flow, two tabs at once. Looks for gaps between what the UI promises and what happens.

**Test questions:** What happens with 0 items, 1,000 items, very long text? Do errors recover, or leave the UI broken? Is state kept on refresh mid-flow? Does anything look like it worked but didn't?

**Report:** silent failures or wrong results; errors that show technical details or break the UI; empty states with no guidance (C14); data lost on refresh or navigation; similar interactions behaving differently.

### 5. Distracted mobile user: "Casey"

**Profile:** on a phone, one-handed, often interrupted, maybe on a slow connection. Types as little as possible, comes back to half-finished tasks.

**Test questions:** Are main actions easy to reach with a thumb? Is state kept after leaving and coming back? Does it work on a slow connection? Do fields use autocomplete tokens and sensible defaults (C8)? Are touch targets at least 24 CSS px, with controls at `--control-height` (C9)?

**Report:** key actions out of thumb reach; progress lost on interruption; long typing where a choice would do; heavy assets on every page; small or crowded targets.

### Choosing personas

| Interface type | Personas | Why |
|---------------|----------|-----|
| Dashboard or admin | Alex, Sam | Efficiency, accessibility |
| Onboarding or first login | Jordan, Casey | Confusion, interruption |
| Data-heavy lists | Alex, Sam | Efficiency, keyboard use |
| Forms and multi-step flows | Jordan, Sam, Casey | Clarity, accessibility, mobile |

### Personas from the change

When the change's proposal or the specs in `openspec/specs/` describe real users (for example a teacher or a student) that none of the five covers, add one persona from them:

```text
### [Role]: "[Name]"

**Profile:** two or three traits taken from the proposal or specs.

**Test questions:** three or four questions about the main action.

**Report:** three or four things that would put this user off.
```

Don't invent audience details; with no such description, use the five personas above.
