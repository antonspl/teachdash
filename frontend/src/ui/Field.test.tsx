import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import '../i18n'
import en from '../i18n/en.json'
import buttonStyles from './Button.module.css'
import iconStyles from './icons.module.css'
import { PasswordInput } from './PasswordInput'
import { Select } from './Select'
import { TextInput } from './TextInput'

afterEach(cleanup)

const MESSAGE = 'Check this value.'

function controls(error?: string) {
  render(
    <>
      <TextInput label="Name" value="" onChange={() => {}} error={error} />
      <PasswordInput
        label="Secret"
        value=""
        onChange={() => {}}
        error={error}
      />
      <Select
        label="Zone"
        value="UTC"
        onChange={() => {}}
        options={['UTC']}
        error={error}
      />
    </>,
  )
  return ['Name', 'Secret', 'Zone'].map((label) =>
    screen.getByLabelText(label, { exact: true }),
  )
}

test('[web-client/R30-S3] A control with an error is described by its message and has an alert icon', async () => {
  // given the three controls with an error text
  const [name, password, zone] = controls(MESSAGE)

  // when each is read by its label, then its accessible description is exact
  expect(
    screen.getByRole('textbox', { name: 'Name', description: MESSAGE }),
  ).toBe(name)
  expect(
    screen.getByRole('combobox', { name: 'Zone', description: MESSAGE }),
  ).toBe(zone)
  // Password inputs have no implicit ARIA role. Use Testing Library's existing
  // description algorithm directly for the input found by its label.
  const { computeAccessibleDescription } = await vi.importActual<{
    computeAccessibleDescription: (element: Element) => string
  }>('dom-accessibility-api')
  expect(computeAccessibleDescription(password)).toBe(MESSAGE)

  for (const field of [name, password, zone]) {
    // Each field is invalid and points to a message with no alert role.
    expect(field.getAttribute('aria-invalid')).toBe('true')
    const message = document.getElementById(
      field.getAttribute('aria-describedby') ?? '',
    )!
    expect(message.getAttribute('role')).toBeNull()

    // and the message starts with the hidden small alert icon
    const icon = message.firstElementChild!
    expect(icon.tagName.toLowerCase()).toBe('svg')
    expect(icon.getAttribute('aria-hidden')).toBe('true')
    expect(icon.getAttribute('focusable')).toBe('false')
    expect(icon.getAttribute('class')).toBe(iconStyles.small)
    expect(icon.querySelector('path')?.getAttribute('d')).toBe(
      'M10 3.5l7 12.5H3zM10 8.5v3.5M10 14v.01',
    )
  }
})

test('[web-client/R30-S3] Without an error there is no message, icon or invalid mark', () => {
  // given the three controls without an error
  const fields = controls()

  // when each is read, then none is invalid or described, and there is no alert icon
  for (const field of fields) {
    expect(field.getAttribute('aria-invalid')).toBeNull()
    expect(field.getAttribute('aria-describedby')).toBeNull()
  }
  expect(document.querySelectorAll('p svg')).toHaveLength(0)
})

test('[web-client/R30-S8] The password toggle is a secondary Button that follows its state', () => {
  // given a password field
  render(<PasswordInput label="Secret" value="" onChange={() => {}} />)
  const showName = en.ui.showPassword.replace('{{label}}', 'Secret')
  const hideName = en.ui.hidePassword.replace('{{label}}', 'Secret')

  // when the toggle is read by role
  const toggle = screen.getByRole('button', { name: showName })

  // then it is a secondary Button, not pressed
  expect(toggle.className).toContain(buttonStyles.button)
  expect(toggle.className).toContain(buttonStyles.secondary)
  expect(toggle.getAttribute('aria-pressed')).toBe('false')
  expect(toggle.textContent).toBe(en.ui.show)

  // and pressing it shows the text, with the pressed state and the hide name
  fireEvent.click(toggle)
  const pressed = screen.getByRole('button', { name: hideName })
  expect(pressed.getAttribute('aria-pressed')).toBe('true')
  expect(pressed.textContent).toBe(en.ui.hide)
  expect(screen.getByLabelText('Secret', { exact: true })).toHaveProperty(
    'type',
    'text',
  )
})

test('[web-client/R30-S8] The toggle is natively disabled with the field', () => {
  // given a disabled password field
  render(<PasswordInput label="Secret" value="" onChange={() => {}} disabled />)

  // when the toggle is read, then it is disabled
  expect(
    (
      screen.getByRole('button', {
        name: en.ui.showPassword.replace('{{label}}', 'Secret'),
      }) as HTMLButtonElement
    ).disabled,
  ).toBe(true)
})
