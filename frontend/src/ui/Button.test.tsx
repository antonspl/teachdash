import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { Button } from './Button'
import { icons, type IconName } from './icons'

afterEach(cleanup)

test('[web-client/R27-S8] icon is rendered before the label, hidden from assistive tech, and the button is icon-led', () => {
  render(<Button icon="plus">Add teacher</Button>)
  const button = screen.getByRole('button', { name: 'Add teacher' })
  const svg = button.querySelector('svg')
  expect(svg).not.toBeNull()
  expect(svg?.getAttribute('aria-hidden')).toBe('true')
  expect(svg?.getAttribute('focusable')).toBe('false')
  expect(button.firstElementChild).toBe(svg)
  expect(button.className).toContain('withIcon')
})

test('[web-client/R27-S8] every icon of the shared set can lead a button, each with its own path', () => {
  const paths = new Set<string>()
  for (const name of Object.keys(icons) as IconName[]) {
    const { unmount } = render(<Button icon={name}>Label</Button>)
    const button = screen.getByRole('button', { name: 'Label' })
    const svg = button.querySelector('svg')
    expect(svg, name).not.toBeNull()
    expect(button.firstElementChild).toBe(svg)
    paths.add(svg!.innerHTML)
    unmount()
  }
  expect(paths.size).toBe(Object.keys(icons).length)
})

test('[web-client/R27-S8] button without icon renders no svg and no icon-led class', () => {
  render(<Button>Create teacher</Button>)
  const button = screen.getByRole('button', { name: 'Create teacher' })
  expect(button.querySelector('svg')).toBeNull()
  expect(button.className).not.toContain('withIcon')
})

test('[web-client/R27-S5] quiet, dangerText and muted each carry their own variant class and the label', () => {
  for (const variant of ['quiet', 'dangerText', 'muted'] as const) {
    const { unmount } = render(
      <Button variant={variant}>{variant} label</Button>,
    )
    const button = screen.getByRole('button', { name: `${variant} label` })
    expect(button.className).toContain(variant)
    for (const other of [
      'primary',
      'secondary',
      'danger',
      'quiet',
      'dangerText',
      'muted',
    ]) {
      if (other === variant) continue
      expect(button.className.split(' ')).not.toContain(other)
    }
    unmount()
  }
})

test('[web-client/R27-S9] fullWidth adds the full-width class, and none by default', () => {
  render(
    <>
      <Button fullWidth>Wide</Button>
      <Button>Narrow</Button>
    </>,
  )
  expect(screen.getByRole('button', { name: 'Wide' }).className).toContain(
    'fullWidth',
  )
  expect(
    screen.getByRole('button', { name: 'Narrow' }).className,
  ).not.toContain('fullWidth')
})

test('[web-client/R27-S5] ariaLabel and ariaPressed pass through to the button', () => {
  render(
    <Button variant="secondary" ariaLabel="Show password" ariaPressed={false}>
      Show
    </Button>,
  )
  const button = screen.getByRole('button', { name: 'Show password' })
  expect(button.getAttribute('aria-pressed')).toBe('false')
})
