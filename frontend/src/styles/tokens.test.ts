import { expect, test } from 'vitest'
import { readFileSync } from 'node:fs'

const css = readFileSync(`${import.meta.dirname}/tokens.css`, 'utf8').replace(
  /\/\*[\s\S]*?\*\//g,
  '',
)

// Copied by hand from .agents/design/design-code.md, "Tokens" tables (names and values),
// in the tables' order. It proves tokens.css equals this list, not design-code.md itself.
const tokens: Record<string, string> = {
  '--color-background': '#f3f8fa',
  '--color-surface': '#ffffff',
  '--color-surface-sunken': '#e9f0f4',
  '--color-rail': '#e1eaef',
  '--color-text': '#152127',
  '--color-text-muted': '#4b5c65',
  '--color-accent': '#155770',
  '--color-accent-hover': '#04465d',
  '--color-accent-active': '#00394d',
  '--color-on-accent': '#ffffff',
  '--color-accent-subtle': '#d9ebf4',
  '--color-control-border': '#6b808b',
  '--color-divider': '#d3dce1',
  '--color-tree-guide': '#9db0ba',
  '--color-focus-ring': 'var(--color-accent)',
  '--color-neutral-background': '#e3e9ec',
  '--color-neutral-text': '#283840',
  '--color-success-background': '#dcf1e3',
  '--color-success-text': '#135a31',
  '--color-danger-background': '#fbe4e1',
  '--color-danger-text': '#981d15',
  '--color-danger-action': '#a8261b',
  '--color-danger-action-hover': '#8c1c13',
  '--color-backdrop': 'rgb(21 33 39 / 50%)',
  '--font-body': "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif",
  '--text-small': '0.8125rem',
  '--text-base': '1rem',
  '--text-reading': '1.0625rem',
  '--text-large': '1.25rem',
  '--text-title': '1.5rem',
  '--weight-regular': '400',
  '--weight-strong': '600',
  '--line-height': '1.45',
  '--line-height-small': '1.4',
  '--line-height-tight': '1.2',
  '--line-height-large': '1.3',
  '--line-height-reading': '1.6',
  '--space-adjacent': '0.25rem',
  '--space-inline': '0.5rem',
  '--space-group': '0.75rem',
  '--space-block': '1.25rem',
  '--space-section': '2rem',
  '--space-page': '2rem',
  '--space-button-gap': '0.75rem',
  '--radius': '0.25rem',
  '--border-width': '1px',
  '--border-width-strong': '2px',
  '--selection-bar-width': '3px',
  '--focus-offset': '2px',
  '--opacity-disabled': '0.6',
  '--shadow-overlay': '0 6px 20px rgb(21 33 39 / 20%)',
  '--duration-fast': '120ms',
  '--tooltip-delay': '400ms',
  '--control-height': '2.75rem',
  '--row-action-size': 'var(--control-height)',
  '--icon-size': '1.25rem',
  '--icon-size-small': '1rem',
  '--icon-size-rail': '1.5rem',
  '--icon-size-placeholder': '3rem',
  '--rail-width': '5rem',
  '--rail-item-height': '4.5rem',
  '--tree-width': '14rem',
  '--outline-width': '24rem',
  '--content-width': '40rem',
  '--reading-width': '40rem',
  '--list-width': '72rem',
  '--home-width': '44rem',
  '--form-width': '22rem',
  '--dialog-width': '28rem',
  '--picker-width': '20rem',
  '--menu-width': '15rem',
  '--tooltip-max-width': '16rem',
  '--lang-select-width': '9rem',
  '--answer-width': '10ch',
  '--type-label-width': '3rem',
  '--topbar-height': '3.5rem',
  '--tabbar-height': '4rem',
  '--action-bar-height': 'calc(var(--control-height) + 2 * var(--space-group))',
  '--side-panel-min': '15rem',
  '--side-panel-max': '24rem',
  '--side-panel-now':
    'clamp(var(--side-panel-min), calc(100cqi - var(--reading-width) - 2 * var(--space-page)), var(--side-panel-max))',
  '--list-gutter':
    'max(var(--space-page), calc(100cqi - var(--space-page) - var(--list-width)))',
  '--editor-column-min': '24rem',
  '--progress-height': '0.375rem',
  '--tree-indent': 'calc(var(--space-adjacent) + var(--row-action-size) / 2)',
  '--login-top-offset': '12vh',
  '--name-min-width': '8rem',
  '--underline-offset': '0.15em',
  '--size-visually-hidden': '1px',
}
const pointerFine = {
  '--control-height': '2.5rem',
  '--row-action-size': '2rem',
}
const russian = { '--type-label-width': '5.5rem' }

function normalize(value: string) {
  return value
    .replace(/\s+/g, ' ')
    .replace(/\( /g, '(')
    .replace(/ \)/g, ')')
    .replace(/, \)/g, ')')
    .trim()
}

function definitions(block: string) {
  const result: Record<string, string> = {}
  for (const match of block.matchAll(/(--[\w-]+)\s*:\s*([^;]+);/g)) {
    result[match[1]] = normalize(match[2])
  }
  return result
}

const fineMatch =
  /@media \(pointer: fine\)\s*\{\s*:root\s*\{([^}]*)\}\s*\}/.exec(css)
const russianMatch = /:root:lang\(ru\)\s*\{([^}]*)\}/.exec(css)
const base = css
  .replace(fineMatch?.[0] ?? '', '')
  .replace(russianMatch?.[0] ?? '', '')
const baseNames = definitions(base)
const fine = definitions(fineMatch?.[1] ?? '')
const russianNames = definitions(russianMatch?.[1] ?? '')

test('[web-client/R24-S1] tokens.css declares exactly the token names', () => {
  // given the literal list of 89 names, and tokens.css read as text
  expect(Object.keys(tokens)).toHaveLength(89)

  // when its custom property definitions are listed
  const defined = Object.keys({ ...baseNames, ...fine, ...russianNames })

  // then every name of the list is defined, and no other name is
  expect(defined.filter((name) => !(name in tokens))).toEqual([])
  expect(Object.keys(tokens).filter((name) => !defined.includes(name))).toEqual(
    [],
  )
})

test('[web-client/R24-S2] tokens.css has the design-code values', () => {
  // given the literal values, including the two media overrides

  // when each token's value is read, then it equals the list's value
  for (const [name, value] of Object.entries(tokens)) {
    expect(baseNames[name], name).toBe(normalize(value))
  }
  expect(fine).toEqual(pointerFine)
  expect(russianNames).toEqual(russian)

  // and the headline values are the specified ones
  expect(baseNames['--radius']).toBe('0.25rem')
  expect(baseNames['--text-large']).toBe('1.25rem')
  expect(baseNames['--text-title']).toBe('1.5rem')
  expect(baseNames['--line-height']).toBe('1.45')
  expect(baseNames['--line-height-tight']).toBe('1.2')
  expect(baseNames['--control-height']).toBe('2.75rem')
  expect(fine['--control-height']).toBe('2.5rem')
})

// WCAG 2.x relative luminance and contrast ratio, from a hex colour of the tokens.
function channel(value: number) {
  const s = value / 255
  return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
}

function luminance(hex: string) {
  const digits = hex.slice(1)
  const [r, g, b] = [0, 2, 4].map((at) =>
    channel(parseInt(digits.slice(at, at + 2), 16)),
  )
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function contrast(foreground: string, background: string) {
  const [a, b] = [luminance(foreground), luminance(background)].sort(
    (x, y) => y - x,
  )
  return (a + 0.05) / (b + 0.05)
}

function color(name: string) {
  const value = baseNames[`--color-${name}`]
  expect(value, name).toMatch(/^#[0-9a-f]{6}$/)
  return value
}

test('[web-client/R26-S1] The control border passes 3:1 on every background', () => {
  // given the colour values of tokens.css
  const border = color('control-border')

  // when the ratio against each background it can sit on is computed
  const backgrounds = [
    'surface',
    'background',
    'surface-sunken',
    'rail',
    'accent-subtle',
    'neutral-background',
    'success-background',
    'danger-background',
  ]

  // then every ratio is at least 3
  for (const background of backgrounds) {
    expect(
      contrast(border, color(background)),
      background,
    ).toBeGreaterThanOrEqual(3)
  }
})

test('[web-client/R26-S3] Text pairs meet their ratios', () => {
  // given the colour values and the contrast helper
  const pairs: [string, string][] = []
  for (const text of ['text', 'text-muted']) {
    for (const background of [
      'surface',
      'background',
      'surface-sunken',
      'rail',
      'accent-subtle',
    ]) {
      pairs.push([text, background])
    }
  }
  pairs.push(
    ['accent', 'surface'],
    ['accent', 'rail'],
    ['on-accent', 'accent'],
    ['on-accent', 'danger-action'],
    ['neutral-text', 'neutral-background'],
    ['success-text', 'success-background'],
    ['danger-text', 'danger-background'],
  )

  // when the ratio of each pair is computed, then it is at least 4.5
  for (const [text, background] of pairs) {
    expect(
      contrast(color(text), color(background)),
      `${text} on ${background}`,
    ).toBeGreaterThanOrEqual(4.5)
  }
})
