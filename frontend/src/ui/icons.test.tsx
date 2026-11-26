import { render } from '@testing-library/react'
import type { ComponentType } from 'react'
import { expect, test } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import * as icons from './icons'

// Vitest does not hand out the text of a CSS file through `?raw`, so read it from disk
// (the tests run from `frontend/`).
const tokensCss = readFileSync(resolve('src/styles/tokens.css'), 'utf8')
const iconsCss = readFileSync(resolve('src/ui/icons.module.css'), 'utf8')

type Shape = Record<string, string>
type Icon = { name: string; component: string; shapes: Shape[] }

const p = (d: string): Shape => ({ tag: 'path', d })
const c = (cx: string, cy: string, r: string, extra: Shape = {}): Shape => ({
  tag: 'circle',
  cx,
  cy,
  r,
  ...extra,
})
const r = (x: string, y: string, width: string, height: string): Shape => ({
  tag: 'rect',
  x,
  y,
  width,
  height,
  rx: '1.5',
})
const filled = { fill: 'currentColor', stroke: 'none' }

// Copied by hand from .agents/design/design-code.md, "Icons", in the table's order.
const table: Icon[] = [
  { name: 'plus', component: 'PlusIcon', shapes: [p('M10 4v12M4 10h12')] },
  {
    name: 'close',
    component: 'CloseIcon',
    shapes: [p('M5 5l10 10M15 5L5 15')],
  },
  { name: 'chev-down', component: 'ChevDownIcon', shapes: [p('M5 8l5 5 5-5')] },
  {
    name: 'chev-right',
    component: 'ChevRightIcon',
    shapes: [p('M8 5l5 5-5 5')],
  },
  {
    name: 'chev-left',
    component: 'ChevLeftIcon',
    shapes: [p('M12 5l-5 5 5 5')],
  },
  {
    name: 'arrow-up',
    component: 'ArrowUpIcon',
    shapes: [p('M10 16V4M5 9l5-5 5 5')],
  },
  {
    name: 'arrow-down',
    component: 'ArrowDownIcon',
    shapes: [p('M10 4v12M5 11l5 5 5-5')],
  },
  {
    name: 'trash',
    component: 'TrashIcon',
    shapes: [p('M4 6h12M8 6V4h4v2M6 6l1 10h6l1-10M9 9v4.5M11 9v4.5')],
  },
  {
    name: 'pencil',
    component: 'PencilIcon',
    shapes: [p('M4 16l1-4 8-8 3 3-8 8zM11 6l3 3')],
  },
  {
    name: 'search',
    component: 'SearchIcon',
    shapes: [c('9', '9', '5.5'), p('M13 13l4 4')],
  },
  {
    name: 'link',
    component: 'LinkIcon',
    shapes: [
      p(
        'M8.5 11.5l3-3M9 6.5l1.5-1.5a3 3 0 0 1 4.2 4.2l-1.5 1.5M11 13.5l-1.5 1.5a3 3 0 0 1-4.2-4.2l1.5-1.5',
      ),
    ],
  },
  {
    name: 'user-off',
    component: 'UserOffIcon',
    shapes: [
      c('9', '7', '3'),
      p('M3.5 16c.6-2.8 2.8-4.5 5.5-4.5M13 13l4 4M17 13l-4 4'),
    ],
  },
  {
    name: 'user-check',
    component: 'UserCheckIcon',
    shapes: [
      c('9', '7', '3'),
      p('M3.5 16c.6-2.8 2.8-4.5 5.5-4.5M12.5 15l2 2 3.5-4'),
    ],
  },
  {
    name: 'ban',
    component: 'BanIcon',
    shapes: [c('10', '10', '7'), p('M5 15L15 5')],
  },
  {
    name: 'home',
    component: 'HomeIcon',
    shapes: [p('M3.5 9.5L10 4l6.5 5.5V16h-4.5v-4h-4v4H3.5z')],
  },
  {
    name: 'book',
    component: 'BookIcon',
    shapes: [
      p(
        'M10 6v10M10 6C8.5 4.8 6.5 4.5 4 4.5v10c2.5 0 4.5.3 6 1.5 1.5-1.2 3.5-1.5 6-1.5v-10c-2.5 0-4.5.3-6 1.5z',
      ),
    ],
  },
  {
    name: 'people',
    component: 'PeopleIcon',
    shapes: [
      c('7.5', '7', '2.5'),
      p('M3 15.5c.4-2.3 2.2-3.8 4.5-3.8s4.1 1.5 4.5 3.8'),
      c('13.5', '7.5', '2'),
      p('M13.5 11.7c1.8 0 3.2 1.2 3.5 3.3'),
    ],
  },
  {
    name: 'profile',
    component: 'ProfileIcon',
    shapes: [
      c('10', '10', '7'),
      c('10', '8.5', '2.5'),
      p('M5.5 15.2c.9-1.6 2.6-2.6 4.5-2.6s3.6 1 4.5 2.6'),
    ],
  },
  {
    name: 'log-out',
    component: 'LogOutIcon',
    shapes: [p('M8 4H5v12h3M12 6.5l3.5 3.5-3.5 3.5M15.5 10H8')],
  },
  {
    name: 'globe',
    component: 'GlobeIcon',
    shapes: [
      c('10', '10', '7'),
      p(
        'M3 10h14M10 3c2 2 2.8 4.4 2.8 7s-.8 5-2.8 7c-2-2-2.8-4.4-2.8-7S8 5 10 3z',
      ),
    ],
  },
  {
    name: 'menu',
    component: 'MenuIcon',
    shapes: [p('M3.5 6h13M3.5 10h13M3.5 14h13')],
  },
  {
    name: 'sidebar',
    component: 'SidebarIcon',
    shapes: [r('3.5', '4', '13', '12'), p('M8 4v12')],
  },
  {
    name: 't-theory',
    component: 'TTheoryIcon',
    shapes: [p('M5 3.5h7l3 3v10H5zM12 3.5v3h3M7.5 10h5M7.5 13h5')],
  },
  {
    name: 't-image',
    component: 'TImageIcon',
    shapes: [
      r('3.5', '4.5', '13', '11'),
      c('7.5', '8.5', '1.25'),
      p('M4 14l4-4 3 3 2-2 3.5 3.5'),
    ],
  },
  {
    name: 't-pick',
    component: 'TPickIcon',
    shapes: [r('3.5', '6', '13', '8'), p('M11 9.25l1.5 1.5 1.5-1.5M6 10h3')],
  },
  {
    name: 't-type',
    component: 'TTypeIcon',
    shapes: [
      p(
        'M10 5v10M7 3.5c1.6 0 3 .6 3 1.5 0-.9 1.4-1.5 3-1.5M7 16.5c1.6 0 3-.6 3-1.5 0 .9 1.4 1.5 3 1.5',
      ),
    ],
  },
  {
    name: 't-audio',
    component: 'TAudioIcon',
    shapes: [
      p(
        'M4 8h2.5L10 5v10l-3.5-3H4zM13 7.5c.8.7 1.2 1.6 1.2 2.5s-.4 1.8-1.2 2.5M15 5.5c1.3 1.2 2 2.8 2 4.5s-.7 3.3-2 4.5',
      ),
    ],
  },
  {
    name: 'play',
    component: 'PlayIcon',
    shapes: [{ ...p('M7 5v10l8-5z'), ...filled }],
  },
  { name: 'pause', component: 'PauseIcon', shapes: [p('M7 5v10M13 5v10')] },
  {
    name: 'more',
    component: 'MoreIcon',
    shapes: [
      c('5', '10', '1.4', filled),
      c('10', '10', '1.4', filled),
      c('15', '10', '1.4', filled),
    ],
  },
  {
    name: 'check',
    component: 'CheckIcon',
    shapes: [p('M4.5 10.5l3.5 3.5 7.5-8')],
  },
  {
    name: 'right',
    component: 'RightIcon',
    shapes: [c('10', '10', '7'), p('M7 10.2l2 2 4-4.4')],
  },
  {
    name: 'wrong',
    component: 'WrongIcon',
    shapes: [c('10', '10', '7'), p('M7.5 7.5l5 5M12.5 7.5l-5 5')],
  },
  {
    name: 'clock',
    component: 'ClockIcon',
    shapes: [c('10', '10', '7'), p('M10 6v4l2.5 2')],
  },
  {
    name: 'lock',
    component: 'LockIcon',
    shapes: [r('5', '9', '10', '7.5'), p('M7 9V7a3 3 0 0 1 6 0v2')],
  },
  {
    name: 'todo',
    component: 'TodoIcon',
    shapes: [c('10', '10', '7', { 'stroke-dasharray': '2.5 2.5' })],
  },
  {
    name: 'send',
    component: 'SendIcon',
    shapes: [p('M3.5 10L16.5 4l-4 12.5-2.5-5.5zM10 11l6.5-7')],
  },
  {
    name: 'eye',
    component: 'EyeIcon',
    shapes: [
      p('M2.5 10s2.8-5 7.5-5 7.5 5 7.5 5-2.8 5-7.5 5-7.5-5-7.5-5z'),
      c('10', '10', '2.25'),
    ],
  },
  {
    name: 'info',
    component: 'InfoIcon',
    shapes: [c('10', '10', '7'), p('M10 9v4.5M10 6.5v.01')],
  },
  {
    name: 'alert',
    component: 'AlertIcon',
    shapes: [p('M10 3.5l7 12.5H3zM10 8.5v3.5M10 14v.01')],
  },
]

const components = icons as unknown as Record<
  string,
  ComponentType<{ size?: string }>
>

function rootOf(name: string, size?: string) {
  const Icon = components[name]
  const { container } = render(<Icon size={size} />)
  return container.querySelector('svg')!
}

test('[web-client/R21-S1] Every icon of the table exists and none is extra', () => {
  // given the table, when the icon components are listed
  const exported = Object.keys(icons)
    .filter((key) => /Icon$/.test(key))
    .sort()

  // then there is one for each icon of the table and no other
  expect(exported).toEqual(table.map((icon) => icon.component).sort())
  expect(new Set(table.map((icon) => icon.name)).size).toBe(table.length)
  for (const icon of table) {
    const pascal = icon.name
      .split('-')
      .map((word) => word[0].toUpperCase() + word.slice(1))
      .join('')
    expect(icon.component).toBe(`${pascal}Icon`)
  }
})

test.each(table)(
  '[web-client/R21-S2] $name has the shared attributes on its root svg',
  ({ component }) => {
    // given the icon is rendered, when its root is read
    const root = rootOf(component)

    // then
    expect(root.tagName.toLowerCase()).toBe('svg')
    expect(root.getAttribute('viewBox')).toBe('0 0 20 20')
    expect(root.getAttribute('fill')).toBe('none')
    expect(root.getAttribute('stroke')).toBe('currentColor')
    expect(root.getAttribute('stroke-width')).toBe('1.75')
    expect(root.getAttribute('stroke-linecap')).toBe('round')
    expect(root.getAttribute('stroke-linejoin')).toBe('round')
    expect(root.getAttribute('aria-hidden')).toBe('true')
    expect(root.getAttribute('focusable')).toBe('false')

    // and no title and no aria-label
    expect(root.querySelector('title')).toBeNull()
    expect(root.hasAttribute('aria-label')).toBe(false)

    // and the shared attributes are not repeated on the shapes
    for (const shape of Array.from(root.children)) {
      for (const attribute of [
        'viewBox',
        'stroke-width',
        'stroke-linecap',
        'stroke-linejoin',
        'aria-hidden',
        'focusable',
      ]) {
        expect(shape.hasAttribute(attribute)).toBe(false)
      }
    }
  },
)

test.each(table)(
  "[web-client/R21-S3] $name's shapes equal the table",
  ({ component, shapes }) => {
    // given the icon is rendered, when its elements are read
    const actual = Array.from(rootOf(component).children).map((element) => ({
      tag: element.tagName.toLowerCase(),
      ...Object.fromEntries(
        Array.from(element.attributes).map((a) => [a.name, a.value]),
      ),
    }))

    // then they equal the hand-copied list, with no attributes beyond it
    expect(actual).toEqual(shapes)
  },
)

function declarations(css: string, className: string) {
  const match = new RegExp(`\\.${className}\\s*\\{([^}]*)\\}`).exec(css)
  expect(match, `.${className} exists`).not.toBeNull()
  return match![1]
}

test('[web-client/R22-S1] The three tokens have the specified values', () => {
  // given tokens.css read as text, when the icon size tokens are read
  const value = (name: string) =>
    new RegExp(`${name}:\\s*([^;]+);`).exec(tokensCss)?.[1].trim()

  // then
  expect(value('--icon-size')).toBe('1.25rem')
  expect(value('--icon-size-small')).toBe('1rem')
  expect(value('--icon-size-rail')).toBe('1.5rem')
})

test('[web-client/R22-S2] The size prop selects the size class', () => {
  // given an icon with no size, default, small and rail
  const none = rootOf('PlusIcon').getAttribute('class')
  const byDefault = rootOf('PlusIcon', 'default').getAttribute('class')
  const small = rootOf('PlusIcon', 'small').getAttribute('class')
  const rail = rootOf('PlusIcon', 'rail').getAttribute('class')

  // then no size and default give the same class, small and rail differ
  expect(none).toBeTruthy()
  expect(none).toBe(byDefault)
  expect(new Set([none, small, rail]).size).toBe(3)

  // and each size class sets width and height from its own token, with no em
  const tokens = {
    default: '--icon-size',
    small: '--icon-size-small',
    rail: '--icon-size-rail',
  }
  for (const [className, token] of Object.entries(tokens)) {
    const body = declarations(iconsCss, className)
    expect(body).toMatch(new RegExp(`width:\\s*var\\(${token}\\)\\s*;`))
    expect(body).toMatch(new RegExp(`height:\\s*var\\(${token}\\)\\s*;`))
    expect(body).toMatch(/flex:\s*none\s*;/)
  }
  expect(iconsCss).not.toMatch(/\d\s*em\b/)
})
