import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { Button } from './Button'

afterEach(cleanup)

test('icon is rendered before the label and hidden from assistive tech', () => {
  render(<Button icon="plus">Add teacher</Button>)
  const button = screen.getByRole('button', { name: 'Add teacher' })
  const svg = button.querySelector('svg')
  expect(svg).not.toBeNull()
  expect(svg?.getAttribute('aria-hidden')).toBe('true')
  expect(svg?.getAttribute('focusable')).toBe('false')
  expect(button.firstElementChild).toBe(svg)
})

test('button without icon renders no svg', () => {
  render(<Button>Create teacher</Button>)
  expect(
    screen.getByRole('button', { name: 'Create teacher' }).querySelector('svg'),
  ).toBeNull()
})
