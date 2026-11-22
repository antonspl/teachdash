Source: pbakaus/impeccable, `skill/reference/polish.md`, `quieter.md`, `bolder.md` and `distill.md` @b49177d, Apache-2.0, licence in `.agents/design/licenses/pbakaus-impeccable.LICENSE`; modified by teachdash.

# Visual passes: polish, quieter, bolder, distill

Four refinement passes over a surface that already exists. Each keeps the incumbent visual world, content and behaviour, and touches only the named target. `project-rules.md` wins over this file: colours, sizes, radius and shadow are tokens from `frontend/src/styles/tokens.css` (C3), a missing value becomes a new role-named token, and the numbers below (saturation, weights, sizes) are directions, not values to hard-code. A fact that cannot be inferred from the code, the spec or the tokens goes into the slice report as an open question; it is not guessed.

## Polish

Polish is refinement, never concealed redesign. Preserve the incumbent visual world, content, behaviour, and everything outside scope. If the concept itself is wrong, say so in the report and recommend a redesign or a bolder pass instead of smuggling in a replacement. An automated check result is defect evidence, not proof of quality: inspect the rendered experience and the real interaction path.

### 1. Establish the system

Read `tokens.css`, representative shared components, patterns and neighbouring flows. Classify each drift before fixing it:

- **missing token:** the system needs a reusable value;
- **one-off implementation:** an existing shared component or pattern should replace it;
- **conceptual mismatch:** the flow, information architecture or hierarchy differs from comparable product areas;
- **local defect:** the implementation is simply incomplete or inconsistent.

Fix the cause at the narrowest correct level. A binding system principle that cannot be inferred is reported, not invented.

### 2. Gather the evidence

Use the feature yourself at the representative sizes, desktop and mobile. Determine:

- whether the path is functionally complete;
- the intended quality bar and the known constraints or deliberately unfinished work (from the spec);
- the states, content lengths, roles and input methods users will actually meet.

If a prior review of the screen exists in the change's documents, use it as one input, and still make an independent pass.

### 3. Triage

Separate functional defects from cosmetic ones and fix in this order:

1. broken or blocked tasks, data loss, misleading state, inaccessible paths;
2. missing loading, empty, error, success, disabled and permission states;
3. flow, hierarchy, responsive and design-system drift;
4. visual and motion inconsistencies;
5. code and asset cleanup.

Do not perfect one corner while leaving the rest below the same quality bar.

### 4. Polish the whole path

**Flow and hierarchy**
- Match neighbouring mental models, terminology, disclosure, routing, save behaviour, and optimistic or pessimistic patterns.
- Make the primary task and current state obvious without flattening every element to equal weight.
- Make arrival, transition, empty and recovery paths connect instead of behaving as isolated screens.

**Layout and type**
- Align to the spacing scale; fix optical as well as mathematical alignment.
- Group related content tightly and separate distinct groups generously.
- Keep same-role typography consistent; test measure, wrapping, localisation expansion and zoom.
- Verify every supported viewport, not only the one currently on screen.

**Colour, imagery and icons**
- Use semantic tokens and stable colour meanings.
- Verify text, control and focus contrast in every state.
- Keep icon family, stroke weight, size and optical alignment coherent.
- Prevent image layout shift; use correct aspect ratios, responsive sources and useful alt text.

**Interaction and state**
- Every control needs fitting default, hover, focus, active, disabled, loading, error and success behaviour.
- Preserve visible keyboard focus, logical tab order, labels and adequate touch targets.
- Keep motion coherent, interruptible and performant. Do not add animation merely to make polish visible.
- Validate long, missing, localised, offline, slow and permission-limited content where the product can meet it.

**Content and code**
- Keep terminology, capitalisation, punctuation and factual copy consistent. Do not change claims; report doubtful ones.
- Remove debug output, dead code, unused imports, obsolete styles and polish-created duplication.
- Replace custom implementations with shared components where the system owns the pattern.
- Promote genuinely reusable values to tokens; do not create an abstraction for one local exception.

### 5. Verify and finish

Walk the complete path again with mouse, keyboard and touch where applicable. Check:

- mobile, intermediate and wide layouts;
- loading, empty, error, success, disabled, long-content and missing-content states;
- zoom, contrast, focus, semantics and screen-reader names;
- console errors, layout shift, interaction latency and image loading;
- agreement with the tokens, neighbouring features and the slice's scope.

Fix real defects and document only narrow intentional exceptions. A clean automated scan does not replace visual judgement. Finish with a look at the source diff: remove accidental churn, orphaned code, redundant values and temporary artifacts. Ship only when the feature is functionally complete and consistently finished across the path.

## Quieter

Quiet design is harder than bold design. Subtlety needs precision. Reduce visual intensity in designs that are too loud, aggressive or overstimulating without losing personality or making the result generic. For a tool or reading surface, "quieter" means reducing visual noise: fewer background accents, flatter cards, less colour, less motion. The tool should disappear more completely into the task.

### Assess the current state

Find what makes the design feel too intense:

- **Colour saturation:** overly bright or saturated colours.
- **Contrast extremes:** too much high-contrast juxtaposition.
- **Visual weight:** too many bold, heavy elements competing.
- **Animation excess:** too much motion or too dramatic effects.
- **Complexity:** too many visual elements, patterns or decorations.
- **Scale:** everything large and loud, with no hierarchy.

Then understand the context: the purpose (tool or reading), the audience, what is already working (do not throw away good ideas) and the core message. If any of these is unclear from the code and the spec, do not guess: put it in the report as an open question.

"Quieter" means refined and easier on the eyes, not generic. Generic quiet looks like grey text on white, one sans at a uniform weight and a grid of identical cards; restraint still needs a point of view.

### Plan the refinement

Decide the colour approach (desaturate or shift to more restrained tones), the hierarchy approach (which very few elements stay bold, which recede), the simplification approach (what can go entirely) and the sophistication approach (how restraint signals quality). Quiet without intent collapses to generic.

### Refine

**Colour**
- Shift from fully saturated to somewhat less saturated; soften bright colours to muted tones.
- Use fewer colours more thoughtfully; let neutrals do more of the work and use colour as an accent (the 10% rule).
- Keep high contrast only where it matters most.
- Use tinted greys, warm or cool, instead of pure grey.
- Never grey text on a coloured background: use a darker shade of that colour, or transparency, so contrast follows whatever sits behind the text.

**Visual weight**
- Reduce font weights (heavy to medium) and decrease sizes where appropriate.
- Build hierarchy through weight, size and space rather than colour and boldness.
- Increase breathing room and reduce density.
- Thin, fade or remove borders and lines.

**Simplification**
- Remove gradients, shadows, patterns and textures that serve no purpose.
- Reduce extremes of border radius and simplify custom shapes.
- Flatten layering where possible; reduce or remove blur, glows and stacked shadows.

**Motion**
- Shorter distances (10-20px instead of 40px) and gentler easing.
- Keep functional motion, remove flourishes; replace dramatic effects with gentle feedback.
- Use ease-out-quart for smooth, understated motion. Never bounce or elastic.
- Remove an animation entirely if it serves no clear purpose.

**Composition**
- Reduce scale jumps; smaller contrast between sizes feels calmer.
- Bring rogue elements back into alignment and replace extreme spacing variations with a consistent rhythm.

**Avoid**
- Making everything the same size or weight (hierarchy still matters).
- Removing all colour (quiet is not grayscale).
- Eliminating all personality (keep character through refinement).
- Sacrificing usability for aesthetics (functional elements still need clear affordances).
- Making everything small and light (some anchors are needed).

### Verify

- Still functional: can users still accomplish tasks easily?
- Still distinctive: does it have character, or is it generic now?
- Better reading: is text easier to read for extended periods?
- Restrained, not absent: does the point of view survive the cuts?

## Bolder

"Bolder" is an amplification request, and almost always it is scoped to something that already exists. The surrounding page, its system and its conventions are the given. The job is to raise one part to the conviction the rest already implies, without rebuilding anything the brief did not name. The reflex answer, reaching for more effects, is the opposite of bold; reject it first.

### Scope is sovereign

"Everything else stays" is a literal instruction. Touch only the named target. Do not restyle its neighbours, do not migrate the page to a new idea, do not add colours, fonts, radii, shadows or system primitives the surface does not already own. If the existing system genuinely cannot express the direction, do not expand it on your own: name the exact addition and the job it would do in the report, as a new-token proposal (C3).

### Why it reads flat

A section usually reads flat for reasons its neighbours have already solved. Look at what the rest of the page does that this section does not: the display type at full strength, the structural devices that carry meaning, the signature motif, the density and pacing. A flat section is typically one that quietly opts out of the system's own strongest moves. The most reliable bolder pass brings the target up to the expressive level its neighbours already reach, in the system's own vocabulary rather than a new one.

### The amplification

- **Amplify what the system already owns.** Reuse its motif and its type scale at full strength, turned up for this section rather than invented for it. The bolder version should look more like the same product, not less.
- **Keep content true.** Existing claims are part of the scope; preserve them. If real evidence is essential to the direction but absent, report it; do not invent it.
- **Commit, then clarify.** Half-measures read as noise. Make the one decisive move completely, then quiet everything around it so the move is legible. If every element got louder, the section got flatter.
- **Give it its own rhythm.** The target should read as a peak in the scroll, a shift in density or pace from what surrounds it, not simply more of the same.

### The skeleton test

Strip the copy out of the planned section and study the bare structure. Does the skeleton still say what this section is and why it matters, through hierarchy and the system's devices alone? If it only works once the words return, the boldness is in the text size, not the design.

### Before you finish

- Everything outside the named target is unchanged.
- No new colour, font or system primitive appeared without being named in the report.
- The conventions the section carried, including anything that drives an action, still work the same way.
- The section is unmistakably the same product, only more sure of itself.

## Distill

Strip a design to its essence. Remove anything that does not earn its place: redundant elements, repeated information, decorative noise, cosmetic complexity.

### Assess the current state

Find what makes the design feel complex or cluttered:

- **Too many elements:** competing buttons, redundant information, visual clutter.
- **Excessive variation:** too many colours, fonts, sizes and styles without purpose.
- **Information overload:** everything visible at once, no progressive disclosure.
- **Visual noise:** unnecessary borders, shadows, backgrounds, decorations.
- **Confusing hierarchy:** unclear what matters most.
- **Feature creep:** too many options, actions or paths forward.

Then find the essence: the primary user goal (there should be one), what is necessary and what is nice to have, what can be removed, hidden or combined, and the 20% that delivers 80% of the value. If any of these is unclear from the code and the spec, do not guess: report it as an open question.

Simplicity is not about removing features. It is about removing obstacles between users and their goals. Every element should justify its existence.

### Plan the simplification

State the core purpose (the one thing this should accomplish), the essential elements, what can be hidden until needed and what can be combined. Expect to say no to good ideas to make room for great execution.

### Simplify

**Information architecture**
- Remove secondary actions and redundant information.
- Hide complexity behind clear entry points (accordions, dialogs, step-through flows).
- Merge similar buttons, consolidate forms, group related content.
- One primary action, few secondary actions, everything else tertiary or hidden.
- If it is said elsewhere, do not repeat it here.

**Visual**
- Keep one or two decorative or brand colours plus neutrals, not five to seven; keep semantic colours (success, warning, error) and pair each with a non-colour cue.
- One font family, three or four sizes at most, two or three weights.
- Remove borders, shadows and backgrounds that do not serve hierarchy or function.
- Flatten structure: reduce nesting, remove unnecessary containers; never nest cards inside cards.
- Cards are not needed for basic layout; use spacing and alignment instead.
- One spacing scale; remove arbitrary gaps.

**Layout**
- Replace complex grids with a simple vertical flow where possible.
- Move secondary content inline or hide it instead of a sidebar.
- Use available space generously instead of complex multi-column layouts.
- Pick left or centre alignment and stick with it.
- Let content breathe; do not pack everything tight.

**Interaction**
- Fewer buttons, fewer options, a clearer path forward (the paradox of choice is real).
- Make common choices automatic by smart defaults; request input in the interface only when necessary.
- Replace dialog flows with inline editing where possible.
- Remove steps: can the flow lose one?
- One obvious next action, not five competing ones.

**Content**
- Shorter copy: cut filler, not meaning.
- Active voice: "Save changes", not "Changes will be saved".
- Plain language over jargon.
- Scannable structure: short paragraphs, bullet points, clear headings.
- Essential information only; cut marketing fluff and hedging.
- No headers restating intros, no repeated explanations: say it once.

**Code**
- Remove unused code: dead CSS, unused components, orphaned files.
- Flatten component trees and consolidate similar styles.
- Reduce variants: does a component need twelve variations, or do three cover 90% of cases?

**Avoid**
- Removing necessary functionality (simplicity is not feature-less).
- Sacrificing accessibility for simplicity (clear labels and ARIA are still required).
- Making things so simple they are unclear (mystery is not minimalism).
- Removing information users need to make decisions.
- Eliminating hierarchy completely (some things should stand out).
- Oversimplifying complex domains (match complexity to the actual task).

### Verify

- Faster task completion: can users accomplish goals more quickly?
- Reduced cognitive load: is it easier to understand what to do?
- Still complete: are all necessary features still accessible?
- Clearer hierarchy: is it obvious what matters most?
- Better performance: does the simpler design load faster?

If features or options were removed, say why in the report, consider whether they need another access point, and note anything to monitor.

As Antoine de Saint-Exupery put it: "Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away."
