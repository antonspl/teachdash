import { cleanup, render, screen } from '@testing-library/react'
import { createRef } from 'react'
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

test('inputRef points at the select element', () => {
  const ref = createRef<HTMLSelectElement>()
  render(
    <Select
      label="Time zone"
      value="UTC"
      onChange={() => {}}
      options={['UTC']}
      inputRef={ref}
    />,
  )
  expect(ref.current).toBe(screen.getByLabelText('Time zone'))
})

test('labels are shown instead of values, with a fallback to the value', () => {
  render(
    <Select
      label="Language"
      value="de"
      onChange={() => {}}
      options={['de', 'xx']}
      labels={{ de: 'German' }}
    />,
  )
  expect(screen.getByRole('option', { name: 'German' })).toBeTruthy()
  expect(screen.queryByRole('option', { name: 'de' })).toBeNull()
  expect(screen.getByRole('option', { name: 'xx' })).toBeTruthy()
})

test('placeholder is not a selectable option and nothing is preselected', () => {
  render(
    <Select
      label="Language"
      value=""
      onChange={() => {}}
      options={['de']}
      placeholder="Choose"
    />,
  )
  expect(screen.queryByRole('option', { name: 'Choose' })).toBeNull()
  expect((screen.getByLabelText('Language') as HTMLSelectElement).value).toBe(
    '',
  )
})

test('flush adds its own class to the root, without it the default stays', () => {
  const { rerender } = render(
    <Select label="Zone" value="a" onChange={() => {}} options={['a']} />,
  )
  const rootOf = () =>
    screen.getByLabelText('Zone').parentElement?.parentElement as HTMLElement
  const plain = rootOf().className
  expect(plain).not.toBe('')
  rerender(
    <Select label="Zone" value="a" onChange={() => {}} options={['a']} flush />,
  )
  const flushed = rootOf().className
  expect(flushed.startsWith(plain)).toBe(true)
  expect(flushed).not.toBe(plain)
})
