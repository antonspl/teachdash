import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import '../i18n'
import { App } from '../App'
import en from '../i18n/en.json'

const fetchMock = vi.fn()

function problem(status: number, code: string) {
  return new Response(JSON.stringify({ status, code }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function teacherResponse() {
  return new Response(
    JSON.stringify({
      id: '1',
      name: 'Tina Teacher',
      email: 't@example.com',
      role: 'TEACHER',
      timeZone: 'UTC',
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

// What the two forms have in common: where their fields are, which request they send.
type Form = {
  name: string
  // Opens the form; `answer` is the reply to the password request.
  open: (answer?: () => Response) => Promise<void>
  password: string
  repeat: string
  button: string
  // Fills the required fields that are not the two new-password fields.
  fillOthers: () => void
  // The requests that send the new password.
  sent: () => unknown[]
}

const profile: Form = {
  name: 'profile password form',
  async open(answer = () => new Response(null, { status: 204 })) {
    fetchMock.mockImplementation((url: string, init?: RequestInit) =>
      Promise.resolve(
        url === '/api/me/password' && init?.method === 'POST'
          ? answer()
          : teacherResponse(),
      ),
    )
    render(
      <MemoryRouter initialEntries={['/profile']}>
        <App />
      </MemoryRouter>,
    )
    await screen.findByLabelText(en.profile.currentPassword)
  },
  password: en.profile.newPassword,
  repeat: en.profile.repeatPassword,
  button: en.profile.changePassword,
  fillOthers() {
    fireEvent.change(screen.getByLabelText(en.profile.currentPassword), {
      target: { value: 'old-password' },
    })
  },
  sent: () =>
    fetchMock.mock.calls.filter(([url]) => url === '/api/me/password'),
}

const setPassword: Form = {
  name: 'set-password page',
  async open(answer = () => new Response(null, { status: 204 })) {
    window.history.replaceState(null, '', '/set-password#token=abc')
    fetchMock.mockImplementation((url: string) =>
      Promise.resolve(
        String(url).endsWith('/password-links')
          ? answer()
          : problem(401, 'UNAUTHENTICATED'),
      ),
    )
    render(
      <MemoryRouter initialEntries={['/set-password']}>
        <App />
      </MemoryRouter>,
    )
    await screen.findByLabelText(en.setPassword.password)
  },
  password: en.setPassword.password,
  repeat: en.setPassword.confirm,
  button: en.setPassword.submit,
  fillOthers() {},
  sent: () =>
    fetchMock.mock.calls.filter(([url]) =>
      String(url).endsWith('/password-links'),
    ),
}

const forms = [profile, setPassword]

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  window.history.replaceState(null, '', '/')
})

function field(label: string) {
  return screen.getByLabelText(label) as HTMLInputElement
}

function type(label: string, value: string) {
  fireEvent.change(field(label), { target: { value } })
}

function press(form: Form) {
  fireEvent.click(screen.getByRole('button', { name: form.button }))
}

// The text that describes the field for assistive technology, if any.
function hintOf(label: string) {
  const id = field(label).getAttribute('aria-describedby')
  return id ? (document.getElementById(id)?.textContent ?? null) : null
}

const hints = [en.password.tooShort, en.password.tooLong, en.password.mismatch]

function expectNoHint() {
  for (const text of hints) {
    expect(screen.queryByText(text), text).toBeNull()
  }
}

describe('new-password hints', () => {
  it.each(forms)(
    '[web-client/R14-S1] Short new password ($name)',
    async (form) => {
      // given
      await form.open()

      // when
      type(form.password, 'abc')

      // then
      expect(hintOf(form.password)).toBe(en.password.tooShort)
      type(form.password, 'abcdefgh')
      expectNoHint()
      expect(hintOf(form.password)).toBeNull()
    },
  )

  it.each(forms)(
    '[web-client/R14-S2] New password over 72 bytes ($name)',
    async (form) => {
      // given
      await form.open()

      // when
      type(form.password, 'я'.repeat(40))

      // then
      expect(hintOf(form.password)).toBe(en.password.tooLong)
      type(form.password, 'я'.repeat(36))
      expectNoHint()
    },
  )

  it.each(forms)(
    '[web-client/R14-S3] Repeat that differs ($name)',
    async (form) => {
      // given
      await form.open()
      type(form.password, 'teacher-pass-2')

      // when
      type(form.repeat, 'teacher')

      // then
      expect(hintOf(form.repeat)).toBe(en.password.mismatch)
      type(form.repeat, 'teacher-pass-2')
      expectNoHint()
    },
  )

  it.each(forms)(
    '[web-client/R14-S4] New password edited after the repeat matched ($name)',
    async (form) => {
      // given
      await form.open()
      type(form.password, 'teacher-pass-2')
      type(form.repeat, 'teacher-pass-2')
      expectNoHint()

      // when
      type(form.password, 'teacher-pass-23')

      // then
      expect(hintOf(form.repeat)).toBe(en.password.mismatch)
    },
  )

  it.each(forms)(
    '[web-client/R14-S5] Empty fields show no hint ($name)',
    async (form) => {
      // given
      await form.open()
      expectNoHint()

      // when
      type(form.password, 'teacher-pass-2')
      expectNoHint()
      type(form.password, '')

      // then
      expectNoHint()
    },
  )

  it.each(forms)(
    '[web-client/R14-S6] Two hints at once ($name)',
    async (form) => {
      // given
      await form.open()

      // when
      type(form.password, 'abc')
      type(form.repeat, 'abd')

      // then
      expect(hintOf(form.password)).toBe(en.password.tooShort)
      expect(hintOf(form.repeat)).toBe(en.password.mismatch)
    },
  )

  it.each(forms)(
    '[web-client/R14-S7] Submitting with a hint sends nothing ($name)',
    async (form) => {
      // given
      await form.open()
      form.fillOthers()
      type(form.password, 'teacher-pass-2')
      type(form.repeat, 'teacher-pass-3')
      expect(hintOf(form.repeat)).toBe(en.password.mismatch)

      // when
      const button = screen.getByRole('button', {
        name: form.button,
      }) as HTMLButtonElement
      expect(button.disabled).toBe(false)
      press(form)

      // then
      expect(button.disabled).toBe(false)
      expect(form.sent()).toHaveLength(0)
      expect(hintOf(form.repeat)).toBe(en.password.mismatch)
    },
  )

  it.each(forms)(
    '[web-client/R14-S12] Submitting with a hint moves the focus to it ($name)',
    async (form) => {
      // given
      await form.open()
      form.fillOthers()
      type(form.password, 'abc')
      type(form.repeat, 'abd')
      const button = screen.getByRole('button', { name: form.button })
      button.focus()
      expect(document.activeElement).toBe(button)

      // when
      press(form)

      // then
      expect(form.sent()).toHaveLength(0)
      expect(document.activeElement).toBe(field(form.password))

      // when
      type(form.password, 'teacher-pass-2')
      press(form)

      // then
      expect(form.sent()).toHaveLength(0)
      expect(document.activeElement).toBe(field(form.repeat))
    },
  )

  it.each(forms)(
    '[web-client/R14-S8] Hint belongs to its field ($name)',
    async (form) => {
      // given
      await form.open()

      // when
      type(form.password, 'abc')

      // then
      expect(field(form.password).getAttribute('aria-invalid')).toBe('true')
      expect(hintOf(form.password)).toBe(en.password.tooShort)
      expect(field(form.repeat).getAttribute('aria-invalid')).toBeNull()
    },
  )
})

it('[web-client/R14-S9] Current password is not checked while typing', async () => {
  // given
  await profile.open()
  const callsBefore = fetchMock.mock.calls.length

  // when
  type(en.profile.currentPassword, 'x')

  // then
  expectNoHint()
  for (const label of [
    en.profile.currentPassword,
    en.profile.newPassword,
    en.profile.repeatPassword,
  ]) {
    expect(hintOf(label), label).toBeNull()
  }
  expect(fetchMock.mock.calls).toHaveLength(callsBefore)
})

it('[web-client/R14-S10] No hints after a successful change', async () => {
  // given
  await profile.open()
  profile.fillOthers()
  type(en.profile.newPassword, 'teacher-pass-2')
  type(en.profile.repeatPassword, 'teacher-pass-2')

  // when
  press(profile)

  // then
  expect(await screen.findByText(en.profile.passwordChanged)).toBeTruthy()
  for (const label of [
    en.profile.currentPassword,
    en.profile.newPassword,
    en.profile.repeatPassword,
  ]) {
    expect(field(label).value, label).toBe('')
    expect(hintOf(label), label).toBeNull()
  }
  expectNoHint()
})

it('[web-client/R14-S11] A hint replaces an old backend error', async () => {
  // given
  await setPassword.open(() => problem(409, 'LINK_CONFLICT'))
  type(en.setPassword.password, 'teacher-pass-2')
  type(en.setPassword.confirm, 'teacher-pass-2')
  press(setPassword)
  expect(await screen.findByText(en.errors.LINK_CONFLICT)).toBeTruthy()
  await waitFor(() =>
    expect(
      (
        screen.getByRole('button', {
          name: en.setPassword.submit,
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(false),
  )
  expect(setPassword.sent()).toHaveLength(1)

  // when
  type(en.setPassword.confirm, 'teacher-pass-3')
  press(setPassword)

  // then
  expect(hintOf(en.setPassword.confirm)).toBe(en.password.mismatch)
  expect(screen.queryByText(en.errors.LINK_CONFLICT)).toBeNull()
  expect(setPassword.sent()).toHaveLength(1)
})
