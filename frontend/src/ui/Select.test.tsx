import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { Select } from './Select'

afterEach(cleanup)

function renderSelect(error?: string) {
  render(
    <Select
      label="Time zone"
      value="UTC"
      onChange={() => {}}
      options={['UTC', 'Europe/Paris']}
      error={error}
    />,
  )
  return screen.getByLabelText('Time zone')
}

test('error is shown under the select and described by it', () => {
  const select = renderSelect('Please pick one.')
  const message = screen.getByText('Please pick one.')
  expect(select.getAttribute('aria-invalid')).toBe('true')
  expect(select.getAttribute('aria-describedby')).toBe(message.id)
  const wrap = select.parentElement as HTMLElement
  expect(wrap.contains(message)).toBe(false)
})

test('without an error the select is not marked invalid', () => {
  const select = renderSelect()
  expect(select.getAttribute('aria-invalid')).toBeNull()
  expect(select.getAttribute('aria-describedby')).toBeNull()
})
