import { cleanup, render, screen } from '@testing-library/react'
import { createRef } from 'react'
import { afterEach, expect, test } from 'vitest'
import styles from './Field.module.css'
import iconStyles from './icons.module.css'
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
  expect(rootOf().className).toContain(styles.field)
  expect(rootOf().className).not.toContain(styles.flush)
  rerender(
    <Select label="Zone" value="a" onChange={() => {}} options={['a']} flush />,
  )
  expect(rootOf().className).toContain(styles.field)
  expect(rootOf().className).toContain(styles.flush)
})

test('[web-client/R28-S3] The arrow of a Select holds only the shared chevron', () => {
  // given a rendered Select
  const select = renderSelect()

  // when the [data-select-arrow] element is read
  const arrow = select.parentElement!.querySelector('[data-select-arrow]')!

  // then it is hidden and holds the one chevron svg with the default size class
  expect(arrow.getAttribute('aria-hidden')).toBe('true')
  expect(arrow.querySelectorAll('svg')).toHaveLength(1)
  expect(arrow.querySelector('svg path')?.getAttribute('d')).toBe(
    'M5 8l5 5 5-5',
  )
  expect(arrow.querySelector('svg')?.getAttribute('class')).toBe(
    iconStyles.default,
  )
})

test('[web-client/R13-S6] A select with an icon renders it outside the arrow, hidden, and only then', () => {
  // given a select with the globe icon and one without
  const { unmount } = render(
    <Select
      label="Language"
      value="en"
      onChange={() => {}}
      options={['en']}
      icon="globe"
    />,
  )
  const select = screen.getByLabelText('Language')
  const wrap = select.parentElement as HTMLElement

  // when the markup is read
  const icon = wrap.querySelector('[data-select-icon]')!
  const arrow = wrap.querySelector('[data-select-arrow]')!

  // then the icon is hidden, is not inside the arrow, and holds the globe
  expect(icon.getAttribute('aria-hidden')).toBe('true')
  expect(arrow.contains(icon)).toBe(false)
  expect(icon.querySelectorAll('svg')).toHaveLength(1)
  expect(icon.querySelector('circle')).not.toBeNull()

  // and the arrow still holds only the chevron and is right after the select
  expect(arrow.querySelectorAll('svg')).toHaveLength(1)
  expect(select.nextElementSibling).toBe(arrow)

  // and the select has the icon-led class
  expect(select.className).toContain(styles.selectWithIcon)
  unmount()

  // and a select without the prop has no icon and no icon-led class
  const plain = renderSelect()
  expect(plain.parentElement?.querySelector('[data-select-icon]')).toBeNull()
  expect(plain.className).not.toContain(styles.selectWithIcon)
})
