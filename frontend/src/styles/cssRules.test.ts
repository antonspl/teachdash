import { expect, test } from 'vitest'
import { spawnSync } from 'node:child_process'
import {
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const srcDir = join(import.meta.dirname, '..')

function filesUnder(dir: string) {
  return readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => join(entry.parentPath, entry.name))
}

const read = (file: string) => readFileSync(file, 'utf8')
const ui = (name: string) => read(join(srcDir, 'ui', `${name}.module.css`))
const squash = (text: string) =>
  text.replace(/\s+/g, ' ').replace(/\(\s+/g, '(').replace(/\s+\)/g, ')').trim()

// The declarations of the rule `selector { ... }` (not inside a media block), squashed.
function rule(css: string, selector: string) {
  const escaped = selector.replace(/[.]/g, '\\.')
  const match = new RegExp(`(?:^|\\n)${escaped}\\s*\\{([^}]*)\\}`).exec(css)
  expect(match, `rule ${selector}`).not.toBeNull()
  return squash(match![1])
}

const nonTestSources = filesUnder(srcDir).filter(
  (file) => /\.(css|tsx?)$/.test(file) && !/\.test\.tsx?$/.test(file),
)
const cssFiles = filesUnder(srcDir).filter((file) => file.endsWith('.css'))

test('[web-client/R24-S3] Headings use the type tokens', () => {
  // given the CSS of Title, Section and Dialog
  const title = ui('Title')

  // when the heading rules are read
  // then Title uses the title size, tight line height and strong weight, with no size step
  const titleRule = rule(title, '.title')
  expect(titleRule).toContain('font-size: var(--text-title);')
  expect(titleRule).toContain('line-height: var(--line-height-tight);')
  expect(titleRule).toContain('font-weight: var(--weight-strong);')
  expect(title).not.toContain('@media')

  // and the Section heading and the Dialog title use the large tokens
  for (const [file, selector] of [
    ['Section', '.heading'],
    ['Dialog', '.title'],
  ]) {
    const heading = rule(ui(file), selector)
    expect(heading, file).toContain('font-size: var(--text-large);')
    expect(heading, file).toContain('line-height: var(--line-height-large);')
    expect(heading, file).toContain('font-weight: var(--weight-strong);')
  }

  // and no CSS file contains a raw font-weight number
  for (const file of cssFiles) {
    expect(read(file), file).not.toMatch(/(?<![\w-])font-weight\s*:\s*\d/)
  }
})

test('[web-client/R24-S4] Page padding follows the space roles', () => {
  // given the CSS of Page
  const page = ui('Page')

  // when its padding rules are read
  // then the padding is block by group, and from 40rem it is the page padding
  expect(rule(page, '.page')).toContain(
    'padding: var(--space-block) var(--space-group);',
  )
  const wide = /@media \(min-width: 40rem\)\s*\{\s*\.page\s*\{([^}]*)\}/.exec(
    page,
  )
  expect(squash(wide?.[1] ?? '')).toBe('padding: var(--space-page);')
})

test('[web-client/R25-S1] No removed token name is left', () => {
  // given every css, ts and tsx file under src except the test files
  // when they are searched for the removed names
  const removed =
    /--color-border|--color-action-success|--shadow-card|--space-[1-4](?![\w-])|--arrow-width/

  // then none occurs
  for (const file of nonTestSources) {
    expect(read(file), file).not.toMatch(removed)
  }
})

test('[web-client/R25-S2] Every referenced token is defined', () => {
  // given every var(--name) in the css and tsx files, and the names tokens.css defines
  const defined = new Set(
    [
      ...read(join(srcDir, 'styles', 'tokens.css')).matchAll(/(--[\w-]+)\s*:/g),
    ].map((match) => match[1]),
  )

  // when each referenced name is looked up, then it is defined
  for (const file of nonTestSources.filter((name) => !name.endsWith('.ts'))) {
    for (const match of read(file).matchAll(/var\(\s*(--[\w-]+)/g)) {
      expect(defined.has(match[1]), `${file}: ${match[1]}`).toBe(true)
    }
  }
})

test('[web-client/R25-S3] Lint fails on an undefined token', () => {
  // given a temporary folder laid out like src, with a CSS file that reads an
  // undefined token (the name is built here, so no source file holds the literal)
  const folder = mkdtempSync(join(tmpdir(), 'tokens-'))
  const name = ['-', '-', 'not-', 'defined'].join('')
  try {
    mkdirSync(join(folder, 'styles'))
    mkdirSync(join(folder, 'ui'))
    writeFileSync(join(folder, 'styles', 'tokens.css'), ':root {\n}\n')
    writeFileSync(
      join(folder, 'ui', 'x.module.css'),
      `.x {\n  color: var(${name});\n}\n`,
    )
    const script = join(
      import.meta.dirname,
      '../../scripts/check-design-tokens.mjs',
    )

    // when the script runs with that folder as its argument
    const failed = spawnSync(process.execPath, [script, folder], {
      encoding: 'utf8',
    })

    // then it exits non-zero and stderr names the file, the line and the token
    expect(failed.status).not.toBe(0)
    expect(failed.stderr).toContain(`ui/x.module.css:2`)
    expect(failed.stderr).toContain(name)

    // and with the name defined in the folder's tokens.css it exits with 0
    writeFileSync(
      join(folder, 'styles', 'tokens.css'),
      `:root {\n  ${name}: #ffffff;\n}\n`,
    )
    const passed = spawnSync(process.execPath, [script, folder], {
      encoding: 'utf8',
    })
    expect(passed.status).toBe(0)
  } finally {
    rmSync(folder, { recursive: true, force: true })
  }
})

test('[web-client/R26-S2] Control edges and decorative edges use their own tokens', () => {
  // given the CSS of Field, Button, IssuedLink, List, Dialog and AppHeader
  const control =
    'border: var(--border-width) solid var(--color-control-border);'
  const divider = 'border: var(--border-width) solid var(--color-divider);'

  // when their border rules are read
  // then the operated edges use the control border
  expect(rule(ui('Field'), '.input')).toContain(control)
  expect(rule(ui('Field'), '.toggle')).toContain(control)
  expect(rule(ui('Button'), '.secondary')).toContain(control)
  expect(rule(ui('IssuedLink'), '.input')).toContain(control)

  // and the decorative edges use the divider
  expect(rule(ui('List'), '.item')).toContain(divider)
  expect(rule(ui('IssuedLink'), '.box')).toContain(divider)
  expect(rule(ui('Dialog'), '.dialog')).toContain(divider)
  expect(rule(ui('AppHeader'), '.header')).toContain(
    'border-bottom: var(--border-width) solid var(--color-divider);',
  )

  // and the field error border is still the danger text colour
  expect(rule(ui('Field'), '.invalid')).toContain(
    'border-color: var(--color-danger-text);',
  )
})

test('[web-client/R27-S2] There is no success variant', () => {
  // given Button.tsx and Button.module.css
  const tsx = read(join(srcDir, 'ui', 'Button.tsx'))
  const css = ui('Button')

  // when the variants are read
  // then the variant type is the six variants
  expect(tsx).toMatch(
    /variant\?:\s*\|?\s*'primary'\s*\|\s*'secondary'\s*\|\s*'quiet'\s*\|\s*'dangerText'\s*\|\s*'muted'\s*\|\s*'danger'\n/,
  )

  // and the CSS has no success rule and no success token
  expect(css).not.toMatch(/\.success/)
  expect(css).not.toContain('--color-action-success')
})

test('[web-client/R27-S3] The danger rule fills with the danger action colour', () => {
  // given Button.module.css
  // when the .danger rule is read
  const danger = rule(ui('Button'), '.danger')

  // then it fills and edges with the danger action and sets the on-accent text
  expect(danger).toContain('background: var(--color-danger-action);')
  expect(danger).toContain(
    'border: var(--border-width) solid var(--color-danger-action);',
  )
  expect(danger).toContain('color: var(--color-on-accent);')
})

test('[web-client/R28-S1] No card has a shadow', () => {
  // given the CSS files under src/ui
  const files = cssFiles.filter((file) =>
    file.includes(`${join(srcDir, 'ui')}/`),
  )

  // when their box-shadow declarations are read
  // then each one is the overlay shadow
  for (const file of files) {
    for (const match of read(file).matchAll(/box-shadow\s*:\s*([^;]+);/g)) {
      expect(squash(match[1]), file).toBe('var(--shadow-overlay)')
    }
  }

  // and the card-like rules have none
  for (const [name, selector] of [
    ['List', '.item'],
    ['StatusMessage', '.status'],
    ['AppHeader', '.header'],
    ['IssuedLink', '.box'],
  ]) {
    expect(rule(ui(name), selector), name).not.toContain('box-shadow')
  }
})

test('[web-client/R28-S2] The select chevron rules', () => {
  // given Field.module.css
  const css = ui('Field')

  // when the arrow and select rules are read
  const arrow = rule(css, '.arrow')
  const select = rule(css, '.select')

  // then the arrow is inert, muted and inset by the border and the group space
  expect(arrow).toContain('pointer-events: none;')
  expect(arrow).toContain('color: var(--color-text-muted);')
  expect(arrow).toContain(
    'right: calc(var(--border-width) + var(--space-group));',
  )

  // and the select drops the native look and clears the chevron
  expect(select).toContain('appearance: none;')
  expect(select).toContain(
    'padding-right: calc(var(--space-group) + var(--icon-size) + var(--space-inline));',
  )

  // and there is no .arrowIcon rule
  expect(css).not.toContain('.arrowIcon')
})

test('[web-client/R27-S5] The new variants have their own look', () => {
  // given Button.module.css
  const css = ui('Button')

  // when the rules of quiet, dangerText and muted are read
  const quiet = rule(css, '.quiet')
  const dangerText = rule(css, '.dangerText')
  const muted = rule(css, '.muted')

  // then quiet is transparent with text colour and a sunken hover fill
  expect(quiet).toContain('color: var(--color-text);')
  expect(quiet).toContain('background: transparent;')
  expect(squash(css)).toMatch(
    /\.quiet:hover:not\(:disabled\) \{ background: var\(--color-surface-sunken\); \}/,
  )

  // and dangerText is danger text with a danger background hover
  expect(dangerText).toContain('color: var(--color-danger-text);')
  expect(squash(css)).toMatch(
    /\.dangerText:hover:not\(:disabled\) \{ background: var\(--color-danger-background\); \}/,
  )

  // and muted is muted text that goes to the text colour on hover
  expect(muted).toContain('color: var(--color-text-muted);')
  expect(squash(css)).toMatch(
    /\.muted:hover:not\(:disabled\) \{ color: var\(--color-text\); \}/,
  )

  // and the three have the neutral background as their active fill
  for (const name of ['quiet', 'dangerText', 'muted']) {
    expect(squash(css)).toMatch(
      new RegExp(
        `\\.${name}:active:not\\(:disabled\\)[^{]*\\{ background: var\\(--color-neutral-background\\); \\}`,
      ),
    )
  }
})

test('[web-client/R27-S6] The secondary button has text colour', () => {
  // given Button.module.css
  const css = ui('Button')

  // when the .secondary rule is read
  const secondary = rule(css, '.secondary')

  // then its text, fill and border are the specified tokens
  expect(secondary).toContain('color: var(--color-text);')
  expect(secondary).toContain('background: var(--color-surface);')
  expect(secondary).toContain(
    'border: var(--border-width) solid var(--color-control-border);',
  )

  // and no variant other than primary uses the accent as a text colour
  for (const name of ['secondary', 'quiet', 'dangerText', 'muted', 'danger']) {
    expect(rule(css, `.${name}`)).not.toMatch(
      /(^|[ ;])color: var\(--color-accent\)/,
    )
  }
})

test('[web-client/R27-S7] Every button state is a token', () => {
  // given Button.module.css
  const css = squash(ui('Button'))
  const base = rule(ui('Button'), '.button')

  // when the base rule is read, then it is made of the specified tokens
  expect(base).toContain('min-height: var(--control-height);')
  expect(base).toContain('padding: 0 var(--space-group);')
  expect(base).toContain('font-weight: var(--weight-strong);')
  expect(base).toContain('line-height: var(--line-height-tight);')

  // and the transitions list the four properties with the fast duration only
  expect(base).toContain(
    'transition: background-color var(--duration-fast), border-color var(--duration-fast), color var(--duration-fast), opacity var(--duration-fast);',
  )

  // and hover is inside the hover media query, on enabled buttons only
  const media = /@media \(hover: hover\) \{(.*?)\} \.primary:active/.exec(css)
  expect(media).not.toBeNull()
  const hover = media![1]
  expect(hover).toMatch(
    /\.primary:hover:not\(:disabled\) \{ background: var\(--color-accent-hover\); border-color: var\(--color-accent-hover\); \}/,
  )
  expect(hover).toMatch(
    /\.secondary:hover:not\(:disabled\), \.quiet:hover:not\(:disabled\) \{ background: var\(--color-surface-sunken\); \}/,
  )
  expect(hover).toMatch(
    /\.danger:hover:not\(:disabled\) \{ background: var\(--color-danger-action-hover\); border-color: var\(--color-danger-action-hover\); \}/,
  )
  expect(
    css.replace(/@media \(hover: hover\) \{.*?\} \.primary:active/, ''),
  ).not.toContain(':hover')

  // and active is a token step, with none for the danger fill
  expect(css).toMatch(
    /\.primary:active:not\(:disabled\) \{ background: var\(--color-accent-active\); border-color: var\(--color-accent-active\); \}/,
  )
  expect(css).toMatch(
    /\.secondary:active:not\(:disabled\), \.quiet:active:not\(:disabled\), \.dangerText:active:not\(:disabled\), \.muted:active:not\(:disabled\) \{ background: var\(--color-neutral-background\); \}/,
  )
  expect(css).not.toContain('.danger:active')

  // and disabled dims and shows not-allowed
  expect(rule(ui('Button'), '.button:disabled')).toContain(
    'opacity: var(--opacity-disabled); cursor: not-allowed;',
  )
})

test('[web-client/R27-S8] The icon-led rule insets the start padding', () => {
  // given Button.module.css
  const css = ui('Button')

  // when the icon-led rule is read
  const withIcon = rule(css, '.withIcon')

  // then it uses a logical property and the inset token, and no :has
  expect(withIcon).toBe(
    'padding-inline-start: calc(var(--space-group) - var(--button-icon-inset));',
  )
  expect(css).not.toContain(':has')

  // and the gap stays the inline space
  expect(rule(css, '.button')).toContain('gap: var(--space-inline);')
})

test('[web-client/R27-S9] The full-width variant is for the lone submit on phones', () => {
  // given Button.module.css and the TSX files
  const css = squash(ui('Button'))

  // when the full-width rule is read, then it is inside the phone media query
  expect(css).toMatch(
    /@media \(max-width: 63\.999rem\) \{ \.fullWidth \{ width: 100%; \} \}/,
  )

  // and only the two submit buttons use it
  const users = nonTestSources
    .filter((file) => file.endsWith('.tsx'))
    .filter((file) => /<Button[^>]*\bfullWidth\b/s.test(read(file)))
    .map((file) => file.slice(srcDir.length + 1))
    .sort()
  expect(users).toEqual(['pages/LoginPage.tsx', 'pages/SetPasswordPage.tsx'])

  // and the ButtonRow rules are unchanged
  expect(ui('ButtonRow')).not.toContain('width')
})

test('[web-client/R30-S1] One global focus rule, never removed', () => {
  // given base.css and every css file under src
  const base = squash(read(join(srcDir, 'styles', 'base.css')))

  // when the focus rules are read, then base.css has one :focus-visible rule
  expect(base.match(/:focus-visible/g)).toHaveLength(1)
  expect(base).toContain(
    ':focus-visible { outline: var(--border-width-strong) solid var(--color-focus-ring); outline-offset: var(--focus-offset); }',
  )

  // and no css file removes an outline
  for (const file of cssFiles) {
    expect(read(file), file).not.toMatch(/outline:\s*(none|0)\b/)
  }
})
