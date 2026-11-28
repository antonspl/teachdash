import { readFileSync } from 'node:fs'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
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

test('[web-client/R29-S1] idle, a button is found by its idle label, enabled and not busy', () => {
  render(<Button busyLabel="Saving…">Save</Button>)
  const button = screen.getByRole('button', { name: 'Save' })
  expect((button as HTMLButtonElement).disabled).toBe(false)
  expect(button.getAttribute('aria-busy')).toBeNull()
  expect(screen.queryByRole('button', { name: 'Saving…' })).toBeNull()
  expect(button.textContent).toBe('Save')
  expect(button.querySelector('span')).toBeNull()
})

test('[web-client/R29-S1] busy, it is found by its busy label, disabled and aria-busy, and the idle label is hidden from its name', () => {
  const onClick = vi.fn()
  render(
    <Button busy busyLabel="Saving…" onClick={onClick}>
      Save
    </Button>,
  )
  const button = screen.getByRole<HTMLButtonElement>('button', {
    name: 'Saving…',
  })
  expect(button.disabled).toBe(true)
  expect(button.getAttribute('aria-busy')).toBe('true')
  expect(screen.queryByRole('button', { name: 'Save' })).toBeNull()
  const idle = [...button.querySelectorAll('span')].find(
    (span) => span.textContent === 'Save',
  )
  expect(idle?.getAttribute('aria-hidden')).toBe('true')
  expect(button.querySelector('svg')).toBeNull()
  expect(button.hasAttribute('style')).toBe(false)
  fireEvent.click(button)
  expect(onClick).not.toHaveBeenCalled()
})

test('[web-client/R29-S1] the same button element goes idle to busy and back, and a button without a busy label renders as before', () => {
  const ariaLabel = 'Save profile'
  const { rerender } = render(
    <Button busyLabel="Saving…" ariaLabel={ariaLabel}>
      Save
    </Button>,
  )
  const button = screen.getByRole('button', { name: ariaLabel })
  rerender(
    <Button busy busyLabel="Saving…" ariaLabel={ariaLabel}>
      Save
    </Button>,
  )
  expect(screen.getByRole('button', { name: 'Saving…' })).toBe(button)
  expect(button.textContent).toBe('SaveSaving…')
  rerender(
    <Button busyLabel="Saving…" ariaLabel={ariaLabel}>
      Save
    </Button>,
  )
  expect(screen.getByRole('button', { name: ariaLabel })).toBe(button)
  expect(button.textContent).toBe('Save')
  expect(button.getAttribute('aria-busy')).toBeNull()
  cleanup()
  render(<Button>Plain</Button>)
  expect(screen.getByRole('button', { name: 'Plain' }).children).toHaveLength(0)
})

test('[web-client/R29-S2] the CSS puts both labels in one grid cell, rendered only while busy, and hides the idle one', () => {
  const css = readFileSync(`${import.meta.dirname}/Button.module.css`, 'utf8')
  expect(css).toMatch(/\.labels\s*\{[^}]*display:\s*grid/)
  expect(css).toMatch(/\.label,\s*\.inactive\s*\{[^}]*grid-area:\s*1\s*\/\s*1/)
  expect(css).toMatch(/\.inactive\s*\{[^}]*visibility:\s*hidden/)
})
