import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { BrowserRouter, MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '../i18n'
import { App } from '../App'
import en from '../i18n/en.json'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

function userResponse(role: string, name = 'Anna Smith', timeZone = 'UTC') {
  return new Response(
    JSON.stringify({
      id: '1',
      name,
      email: 'anna@example.com',
      role,
      timeZone,
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

function problem(status: number, code: string) {
  return new Response(JSON.stringify({ status, code }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function open(path = '/profile') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

test('[web-profile/R1-S1] Each role opens the profile', async () => {
  for (const [role, cabinet] of [
    ['ADMIN', en.cabinet.admin],
    ['TEACHER', en.cabinet.teacher],
    ['STUDENT', en.cabinet.student],
  ]) {
    // given
    fetchMock.mockResolvedValue(userResponse(role, `Name of ${role}`))

    // when
    const view = open()

    // then
    expect(
      await screen.findByRole('heading', { name: en.profile.title }),
      role,
    ).toBeTruthy()
    expect(screen.getByText(new RegExp(`Name of ${role}`)), role).toBeTruthy()
    expect(screen.queryByText(cabinet), role).toBeNull()
    view.unmount()
  }
})

test('[web-profile/R1-S2] Anonymous visitor gets the login page', async () => {
  // given
  fetchMock.mockResolvedValue(problem(401, 'UNAUTHENTICATED'))

  // when
  open()

  // then
  expect(
    await screen.findByRole('heading', { name: en.login.title }),
  ).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.profile.title })).toBeNull()
})

test("[web-profile/R1-S4] Back to cabinet opens the user's own cabinet", async () => {
  // given
  fetchMock.mockResolvedValue(userResponse('STUDENT'))
  open()

  // when
  fireEvent.click(
    await screen.findByRole('button', { name: en.header.backToCabinet }),
  )

  // then
  expect(await screen.findByText(en.cabinet.student)).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.profile.title })).toBeNull()
})

test('[web-profile/R1-S6] Profile waits for the startup check', () => {
  // given
  fetchMock.mockReturnValue(new Promise(() => undefined))

  // when
  open()

  // then
  expect(screen.getByText(en.ui.loading)).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
  expect(screen.queryByRole('heading', { name: en.profile.title })).toBeNull()
})

test('[web-profile/R1-S7] Startup error on the profile page', async () => {
  // given
  fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
  fetchMock.mockResolvedValueOnce(userResponse('TEACHER'))

  // when
  open()

  // then
  expect(await screen.findByText(en.startup.error)).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: en.ui.tryAgain }))
  expect(
    await screen.findByRole('heading', { name: en.profile.title }),
  ).toBeTruthy()
})

// Logged in as a teacher; every PATCH /api/me goes to `onSave`.
function teacherWithSave(
  onSave: () => Promise<Response>,
  timeZone = 'UTC',
  name = 'Anna Smith',
) {
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url === '/api/me' && init?.method === 'PATCH') {
      return onSave()
    }
    return Promise.resolve(userResponse('TEACHER', name, timeZone))
  })
}

function saves() {
  return fetchMock.mock.calls.filter(
    ([url, init]) =>
      url === '/api/me' &&
      (init as RequestInit | undefined)?.method === 'PATCH',
  )
}

function sentBody(call: unknown[]): unknown {
  return JSON.parse((call[1] as RequestInit).body as string)
}

function headerHas(text: string) {
  return screen.getAllByText(new RegExp(text)).length > 0
}

async function openForm() {
  open()
  return screen.findByLabelText(en.profile.name)
}

function pressSave() {
  fireEvent.click(screen.getByRole('button', { name: en.profile.save }))
}

function typeName(value: string) {
  fireEvent.change(screen.getByLabelText(en.profile.name), {
    target: { value },
  })
}

test('[web-profile/R2-S2] Form and header show what the backend returned', async () => {
  // given
  teacherWithSave(() =>
    Promise.resolve(userResponse('TEACHER', 'Anna S.', 'Asia/Dubai')),
  )
  await openForm()

  // when
  typeName('  Anna S.  ')
  pressSave()

  // then
  expect(await screen.findByText(en.profile.saved)).toBeTruthy()
  expect(
    (screen.getByLabelText(en.profile.name) as HTMLInputElement).value,
  ).toBe('Anna S.')
  expect(headerHas('Anna S\\. \\(')).toBe(true)
  expect(screen.queryByText(/Anna Smith/)).toBeNull()
  expect(saves()).toHaveLength(1)
  expect(sentBody(saves()[0])).toEqual({
    name: '  Anna S.  ',
    timeZone: 'UTC',
  })
})

test('[web-profile/R2-S3] Success message goes away on the next edit', async () => {
  // given
  teacherWithSave(() => Promise.resolve(userResponse('TEACHER')))
  await openForm()

  for (const edit of [
    () => typeName('Anna S.'),
    () =>
      fireEvent.change(screen.getByLabelText(en.profile.timeZone), {
        target: { value: 'Europe/Berlin' },
      }),
  ]) {
    pressSave()
    expect(await screen.findByText(en.profile.saved)).toBeTruthy()

    // when
    edit()

    // then
    expect(screen.queryByText(en.profile.saved)).toBeNull()
  }
})

test('[web-profile/R2-S4] An empty name is not sent', async () => {
  // given
  teacherWithSave(() => Promise.resolve(userResponse('TEACHER')))
  await openForm()

  // when
  typeName('')
  pressSave()

  // then
  await new Promise((resolve) => setTimeout(resolve, 20))
  expect(saves()).toHaveLength(0)
})

test('[web-profile/R2-S5] Values the backend rejects', async () => {
  // given
  teacherWithSave(() => Promise.resolve(problem(400, 'VALIDATION_FAILED')))
  await openForm()
  typeName('Anna Typed')

  // when
  pressSave()

  // then
  expect(await screen.findByText(en.profile.validationFailed)).toBeTruthy()
  expect(
    (screen.getByLabelText(en.profile.name) as HTMLInputElement).value,
  ).toBe('Anna Typed')
  expect(headerHas('Anna Smith')).toBe(true)
  expect(
    (screen.getByRole('button', { name: en.profile.save }) as HTMLButtonElement)
      .disabled,
  ).toBe(false)
  pressSave()
  await waitFor(() => expect(saves()).toHaveLength(2))
})

test('[web-profile/R2-S6] Save that fails for another reason', async () => {
  for (const failure of [
    () => Promise.reject(new TypeError('Failed to fetch')),
    () => Promise.resolve(problem(500, 'INTERNAL_ERROR')),
    () => Promise.resolve(problem(403, 'CSRF_INVALID')),
  ]) {
    // given
    fetchMock.mockReset()
    teacherWithSave(failure)
    const view = open()
    await screen.findByLabelText(en.profile.name)
    typeName('Anna Typed')

    // when
    pressSave()

    // then
    const status = await screen.findByRole('status')
    expect(status.textContent).toBeTruthy()
    expect(status.textContent).not.toBe(en.profile.saved)
    expect(status.textContent).not.toBe(en.profile.validationFailed)
    expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
    expect(headerHas('Anna Smith')).toBe(true)
    expect(
      (
        screen.getByRole('button', {
          name: en.profile.save,
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(false)
    view.unmount()
  }
})

test('[web-profile/R2-S7] Stored time zone the browser does not list', async () => {
  // given
  // Only the zone list is replaced; the rest of Intl is inherited.
  vi.stubGlobal(
    'Intl',
    Object.create(Intl, {
      supportedValuesOf: { value: () => ['Europe/Berlin'] },
    }),
  )
  teacherWithSave(
    () => Promise.resolve(userResponse('TEACHER', 'Anna B.', 'Europe/Kiev')),
    'Europe/Kiev',
  )

  // when
  await openForm()
  typeName('Anna B.')
  pressSave()

  // then
  expect(
    (screen.getByLabelText(en.profile.timeZone) as HTMLSelectElement).value,
  ).toBe('Europe/Kiev')
  await waitFor(() => expect(saves()).toHaveLength(1))
  expect(sentBody(saves()[0])).toEqual({
    name: 'Anna B.',
    timeZone: 'Europe/Kiev',
  })
})

test('[web-profile/R2-S8] A second press while saving sends nothing', async () => {
  // given
  teacherWithSave(() => new Promise(() => undefined))
  await openForm()
  pressSave()
  await waitFor(() => expect(saves()).toHaveLength(1))

  // when
  pressSave()
  fireEvent.submit(screen.getByRole('button', { name: en.profile.save }))

  // then
  await new Promise((resolve) => setTimeout(resolve, 20))
  expect(saves()).toHaveLength(1)
})

// The session-ended tests read the path from the real location, like web-auth/R6.
function openInBrowser(path: string) {
  window.history.pushState({}, '', path)
  return render(
    <BrowserRouter>
      <App />
    </BrowserRouter>,
  )
}

test('[web-profile/R4-S1] A profile save with an ended session', async () => {
  // given
  teacherWithSave(() => Promise.resolve(problem(401, 'UNAUTHENTICATED')))
  openInBrowser('/profile')
  await screen.findByLabelText(en.profile.name)

  // when
  pressSave()

  // then
  expect(await screen.findByText(en.login.sessionEnded)).toBeTruthy()
  expect(screen.getByRole('heading', { name: en.login.title })).toBeTruthy()
})

test('[web-profile/R4-S3] After logging in again the profile is shown', async () => {
  // given
  let ended = false
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url === '/api/me' && init?.method === 'PATCH') {
      ended = true
      return Promise.resolve(problem(401, 'UNAUTHENTICATED'))
    }
    if (url === '/api/me') {
      return Promise.resolve(
        ended
          ? problem(401, 'UNAUTHENTICATED')
          : userResponse('TEACHER', 'Anna Smith'),
      )
    }
    return Promise.resolve(userResponse('STUDENT', 'Sam Student', 'Asia/Dubai'))
  })
  openInBrowser('/profile')
  await screen.findByLabelText(en.profile.name)
  pressSave()
  await screen.findByText(en.login.sessionEnded)

  // when
  fireEvent.change(screen.getByLabelText(en.login.email), {
    target: { value: 'sam@example.com' },
  })
  fireEvent.change(screen.getByLabelText(en.login.password), {
    target: { value: 'secret-pass' },
  })
  fireEvent.click(screen.getByRole('button', { name: en.login.submit }))

  // then
  expect(await screen.findByLabelText(en.profile.name)).toBeTruthy()
  expect(window.location.pathname).toBe('/profile')
  expect(
    (screen.getByLabelText(en.profile.name) as HTMLInputElement).value,
  ).toBe('Sam Student')
  expect(
    (screen.getByLabelText(en.profile.timeZone) as HTMLSelectElement).value,
  ).toBe('Asia/Dubai')
})
