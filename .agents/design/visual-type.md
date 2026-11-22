Source: pbakaus/impeccable, `skill/reference/typeset.md` @b49177d, Apache-2.0, licence in `.agents/design/licenses/pbakaus-impeccable.LICENSE`; modified by teachdash.

# Typography: assessment, reading and line breaks

Typography carries information, hierarchy and voice. Improve it inside the established visual world; do not replace the identity unless the engineer asked to. `project-rules.md` wins over this file: one family, `--font-body`, the system stack (C4), and sizes from the `--text-*` tokens (C3). The scale, the line-height tokens, weights and "one family" are in `refactoring-ui.md` section 3 and are not repeated here.

## Operate and read surfaces

Stability, scanability and measure come first. A single well-tuned family and a fixed role scale are often right.

## Typographic assessment

Inspect representative pages and styles. Answer every question with a file, selector or computed value:

- **Authority and fit:** Which faces, weights and roles are established? Do they fit the product, or are they unexamined defaults? Is every family necessary?
- **Hierarchy:** Can heading, body, label, metadata and data roles be distinguished at a glance? Are adjacent sizes or weights too close to carry different jobs?
- **Scale and consistency:** Is there a deliberate role scale, or a collection of arbitrary values? Do repeated roles stay identical across screens and states?
- **Reading:** Does body copy stay within a comfortable 45-75 character measure? Are line height, paragraph rhythm, contrast and tracking tuned to the actual face, width, language and surface?
- **Stress:** What happens with long headings, localization expansion, zoom, narrow containers, missing weights and font fallback?
- **Delivery:** Are only used assets loaded? Do fallback metrics and the loading strategy avoid invisible text and disruptive reflow?

Also inspect dynamic or arbitrary font values by reading the source. A clean read is a floor, not proof of good typography.

## Set the system

Before editing, state:

- the roles the interface needs;
- the intended contrast between those roles;
- the reading measure and density;
- which existing faces and weights are authoritative;
- any performance, localization or accessibility constraints.

Combine size, weight, space and tone deliberately instead of asking size alone to do all the work. Role names and tokens describe purpose rather than values.

## Apply

- Keep body copy comfortably readable and zoomable. Use 1rem / 16px as the ordinary web body floor unless a dense role or user setting justifies otherwise.
- Keep prose in the 45-75ch range. Tune line height inversely with measure: wider lines generally need more leading.
- Compensate light text on dark surfaces on all three perceptual axes: slightly more line height, a touch more tracking, and one step more weight when the face needs it.
- Tune line height to the face, width, language and contrast, not a universal ratio.
- Use numeric, tabular, code and label features when their content benefits.
- Preserve browser zoom and user font settings.
- Use paragraph spacing or first-line indentation as the primary paragraph rhythm; combining both usually double-marks the boundary.

Do not make type decorative at the expense of comprehension.

## Line breaks

After Matthew Butterick's *Practical Typography*, paraphrased; where his numbers differ from these, these stand.

- Set `text-wrap: balance` on headings and short centered blocks, and `text-wrap: pretty` on body copy. Both are progressive: a browser that ignores them keeps its default wrap.
- `pretty` pulls a word down beside a lone last word. It does not fill the last line, and a two-word tail is fine.
- In a column too narrow for either to help, edit the copy or widen the column.
- Break a heading by hand only between phrases, and check the break at every width the heading renders at. A hard break that lands mid-line on a phone is worse than the default wrap.
- An uneven break can carry meaning: a headline that lands on one word for weight, a poster line, a pull quote. Make it deliberate with a hard break or a wrap set on that element, and keep it rare. Say which breaks are intended.

## Optical alignment

- Put `text-box: trim-both cap alphabetic` on the label element, not the flex container. On an `inline-flex` button the text is an anonymous flex item, and trim on the button never reaches it. Wrap the label in a `<span>` with `display: block` and trim that.
- For a trimmed label's fallback face, set `size-adjust` in the fallback `@font-face` to the web font's cap height over the fallback's, both per em. `ascent-override`, `descent-override` and `line-gap-override` change only what trim removes.
- Judge the rendered component: centered caps are not a centered word. Most of a mixed-case label sits in the x-height, whose center is below the cap center, so a trimmed label can still look low. Check real labels at every size the control ships.

## Verify

- Primary, secondary, body and metadata roles are recognizable without reading the copy.
- Long text remains comfortable across relevant widths and languages.
- The typography belongs to the product and its established world.
- Loading does not create disruptive reflow or invisible text.
- Zoom, text scaling, focus, contrast and reduced viewport paths remain usable.

Answer each item with rendered or source evidence. Do not substitute a bare "yes" for verification.
