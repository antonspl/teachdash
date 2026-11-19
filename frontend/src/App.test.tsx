import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import './i18n'
import { App } from './App'
import en from './i18n/en.json'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

function userResponse(role: string, name = 'Anna Smith') {
  return new Response(
    JSON.stringify({
      id: '1',
      name,
      email: 'a@example.com',
      role,
      timeZone: 'UTC',
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

function open(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

test('[web-auth/R2-S1] Anonymous user opening a cabinet goes to login', async () => {
  // given
  fetchMock.mockResolvedValue(problem(401, 'UNAUTHENTICATED'))

  // when
  open('/teacher')

  // then
  expect(
    await screen.findByRole('heading', { name: en.login.title }),
  ).toBeTruthy()
  expect(screen.queryByText(en.cabinet.teacher)).toBeNull()
})

test('[web-auth/R2-S2] Root path sends each role to its cabinet', async () => {
  const cases = [
    ['ADMIN', en.cabinet.admin],
    ['TEACHER', en.cabinet.teacher],
    ['STUDENT', en.cabinet.student],
  ]
  for (const [role, title] of cases) {
    // given
    fetchMock.mockResolvedValueOnce(userResponse(role))

    // when
    const view = open('/')

    // then
    expect(await screen.findByText(title), role).toBeTruthy()
    view.unmount()
  }
})

test('[web-auth/R2-S3] Root path sends an anonymous user to login', async () => {
  // given
  fetchMock.mockResolvedValue(problem(401, 'UNAUTHENTICATED'))

  // when
  open('/')

  // then
  expect(
    await screen.findByRole('heading', { name: en.login.title }),
  ).toBeTruthy()
})

test("[web-auth/R2-S4] Another role's cabinet sends the user to their own", async () => {
  // given
  fetchMock.mockResolvedValue(userResponse('TEACHER'))

  // when
  open('/admin')

  // then
  expect(await screen.findByText(en.cabinet.teacher)).toBeTruthy()
  expect(screen.queryByText(en.cabinet.admin)).toBeNull()
})

test('[web-auth/R2-S5] Logged-in user opening login goes to the cabinet', async () => {
  // given
  fetchMock.mockResolvedValue(userResponse('STUDENT'))

  // when
  open('/login')

  // then
  expect(await screen.findByText(en.cabinet.student)).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
})

test('[web-auth/R2-S6] Nothing is shown before the user is known', async () => {
  // given
  fetchMock.mockReturnValue(new Promise(() => undefined))

  // when
  open('/teacher')

  // then
  expect(screen.getByText(en.ui.loading)).toBeTruthy()
  expect(screen.queryByText(en.cabinet.teacher)).toBeNull()
  expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
})

test('[web-auth/R2-S7] Failure to load the user is not a logout', async () => {
  const failures: Array<[string, () => Promise<Response>]> = [
    ['network error', () => Promise.reject(new TypeError('Failed to fetch'))],
    ['500', async () => problem(500, 'INTERNAL_ERROR')],
  ]
  for (const [name, fail] of failures) {
    // given
    fetchMock.mockImplementationOnce(fail)
    fetchMock.mockResolvedValueOnce(userResponse('TEACHER'))

    // when
    const view = open('/teacher')

    // then
    expect(await screen.findByText(en.startup.error), name).toBeTruthy()
    expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()

    // when the backend has recovered
    fireEvent.click(screen.getByRole('button', { name: en.ui.tryAgain }))

    // then
    expect(await screen.findByText(en.cabinet.teacher), name).toBeTruthy()
    view.unmount()
  }
})

test("[web-auth/R3-S1] Cabinet shows the role and the user's name", async () => {
  // given
  fetchMock.mockResolvedValue(userResponse('TEACHER', 'Anna Smith'))

  // when
  open('/teacher')

  // then
  expect(await screen.findByText(en.cabinet.teacher)).toBeTruthy()
  expect(screen.getByText(/Anna Smith/)).toBeTruthy()
  expect(screen.getByRole('button', { name: en.header.profile })).toBeTruthy()
  expect(screen.getByRole('button', { name: en.header.logout })).toBeTruthy()
})

test('[web-auth/R3-S2] Profile button opens the profile', async () => {
  // given
  fetchMock.mockResolvedValue(userResponse('ADMIN'))
  open('/admin')

  // when
  fireEvent.click(
    await screen.findByRole('button', { name: en.header.profile }),
  )

  // then
  expect(
    await screen.findByRole('heading', { name: en.profile.title }),
  ).toBeTruthy()
})

// Answers each API path with the given response factory; anything else is a 401 problem.
function backend(routes: Record<string, () => Promise<Response> | Response>) {
  fetchMock.mockImplementation((url: string) => {
    const handler = routes[url.replace('/api/', '')]
    return Promise.resolve(
      handler ? handler() : problem(401, 'UNAUTHENTICATED'),
    )
  })
}

function loginCalls() {
  return fetchMock.mock.calls.filter(([url]) => url === '/api/auth/login')
}

async function submitLogin(email = 'a@example.com', password = 'secret-pass') {
  fireEvent.change(await screen.findByLabelText(en.login.email), {
    target: { value: email },
  })
  fireEvent.change(screen.getByLabelText(en.login.password), {
    target: { value: password },
  })
  fireEvent.click(screen.getByRole('button', { name: en.login.submit }))
}

function expectFormReset(email = 'a@example.com') {
  expect(
    (screen.getByLabelText(en.login.email) as HTMLInputElement).value,
  ).toBe(email)
  expect(
    (screen.getByLabelText(en.login.password) as HTMLInputElement).value,
  ).toBe('')
}

test('[web-auth/R1-S2] Wrong credentials show the translated message', async () => {
  // given
  backend({ 'auth/login': () => problem(401, 'INVALID_CREDENTIALS') })
  open('/login')

  // when
  await submitLogin()

  // then
  expect(await screen.findByText(en.errors.INVALID_CREDENTIALS)).toBeTruthy()
  expectFormReset()
})

test('[web-auth/R1-S3] Deactivated account shows its own message', async () => {
  // given
  backend({ 'auth/login': () => problem(401, 'ACCOUNT_DEACTIVATED') })
  open('/login')

  // when
  await submitLogin()

  // then
  expect(await screen.findByText(en.errors.ACCOUNT_DEACTIVATED)).toBeTruthy()
})

test('[web-auth/R1-S4] Form is not sent twice', async () => {
  // given
  backend({ 'auth/login': () => new Promise<Response>(() => undefined) })
  open('/login')
  await submitLogin()
  await waitFor(() => expect(loginCalls()).toHaveLength(1))

  // when
  fireEvent.click(screen.getByRole('button', { name: en.login.submit }))
  fireEvent.submit(screen.getByRole('button', { name: en.login.submit }))

  // then
  expect(
    (screen.getByRole('button', { name: en.login.submit }) as HTMLButtonElement)
      .disabled,
  ).toBe(true)
  expect(loginCalls()).toHaveLength(1)
})

test('[web-auth/R1-S5] Page explains how to get a link', async () => {
  // given
  backend({})

  // when
  open('/login')

  // then
  const hint = await screen.findByText(en.login.linkHint)
  expect(hint.textContent).toMatch(/ask your teacher or administrator/i)
  expect(hint.textContent).toMatch(/link/i)
})

test('[web-auth/R1-S6] Network error or rejected CSRF check', async () => {
  const failures: Array<[string, () => Promise<Response> | Response, string]> =
    [
      [
        'network error',
        () => Promise.reject(new TypeError('Failed to fetch')),
        en.errors.generic,
      ],
      [
        'CSRF_INVALID',
        () => problem(403, 'CSRF_INVALID'),
        en.errors.CSRF_INVALID,
      ],
    ]
  for (const [name, fail, message] of failures) {
    // given
    backend({ 'auth/login': fail })
    const view = open('/login')

    // when
    await submitLogin()

    // then
    expect(await screen.findByText(message), name).toBeTruthy()
    expect(
      (
        screen.getByRole('button', {
          name: en.login.submit,
        }) as HTMLButtonElement
      ).disabled,
      name,
    ).toBe(false)
    view.unmount()
  }
})

test('[web-auth/R4-S2] Logout with an already ended session', async () => {
  // given
  backend({
    me: () => userResponse('STUDENT'),
    'auth/logout': () => problem(401, 'UNAUTHENTICATED'),
  })
  open('/student')
  fireEvent.click(await screen.findByRole('button', { name: en.header.logout }))

  // then
  expect(
    await screen.findByRole('heading', { name: en.login.title }),
  ).toBeTruthy()
  expect(screen.queryByText(en.login.sessionEnded)).toBeNull()
  expect(screen.queryByText(en.header.logoutError)).toBeNull()
})

test('[web-auth/R4-S3] Logout that fails for another reason', async () => {
  const failures: Array<[string, () => Promise<Response> | Response]> = [
    ['network error', () => Promise.reject(new TypeError('Failed to fetch'))],
    ['500', () => problem(500, 'INTERNAL_ERROR')],
  ]
  for (const [name, fail] of failures) {
    // given
    backend({ me: () => userResponse('STUDENT'), 'auth/logout': fail })
    const view = open('/student')

    // when
    fireEvent.click(
      await screen.findByRole('button', { name: en.header.logout }),
    )

    // then
    expect(await screen.findByText(en.header.logoutError), name).toBeTruthy()
    expect(screen.getByText(en.cabinet.student), name).toBeTruthy()
    view.unmount()
  }
})
