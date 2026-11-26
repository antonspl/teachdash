import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import i18next from 'i18next'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import './i18n'
import { App } from './App'
import en from './i18n/en.json'
import ru from './i18n/ru.json'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(async () => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  await i18next.changeLanguage('en')
  localStorage.clear()
  document.documentElement.lang = 'en'
})

function json(body: unknown, status = 200, type = 'application/json') {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': type },
  })
}

function problem(status: number, code: string) {
  return json({ status, code }, status, 'application/problem+json')
}

// Answers /api/me with the role's user (and an empty teacher list), or as anonymous
// when `role` is undefined.
function backend(role?: string, routes: Record<string, () => Response> = {}) {
  fetchMock.mockImplementation((url: string) => {
    const path = String(url).replace('/api/', '')
    if (routes[path]) {
      return Promise.resolve(routes[path]())
    }
    if (role && path === 'me') {
      return Promise.resolve(
        json({
          id: '1',
          name: 'Anna Smith',
          email: 'a@example.com',
          role,
          timeZone: 'UTC',
        }),
      )
    }
    // The admin cabinet loads its teacher list. A 401 here would end the session
    // and flip the page to the login form at a random moment.
    if (role && path === 'admin/teachers') {
      return Promise.resolve(json([]))
    }
    return Promise.resolve(problem(401, 'UNAUTHENTICATED'))
  })
}

function open(path: string) {
  window.history.replaceState(null, '', path)
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

const selectName = /^(Language|Язык)$/

test('[web-client/R20-S1] The switch is on the pages that should have it', async () => {
  const cases: Array<[string | undefined, string]> = [
    [undefined, '/login'],
    [undefined, '/set-password'],
    ['ADMIN', '/admin'],
    ['TEACHER', '/teacher'],
    ['STUDENT', '/student'],
    ['ADMIN', '/profile'],
    ['TEACHER', '/profile'],
    ['STUDENT', '/profile'],
  ]
  for (const [role, path] of cases) {
    backend(role)
    const view = open(path)
    expect(
      await screen.findByRole('combobox', { name: selectName }),
      `${role} ${path}`,
    ).toBeTruthy()
    view.unmount()
  }
})

test('[web-client/R20-S2] The switch is absent where it should not be', async () => {
  // not-found route
  backend()
  let view = open('/no-such-page')
  await screen.findByRole('heading', { name: en.notFound.title })
  expect(screen.queryByRole('combobox')).toBeNull()
  view.unmount()

  // loading state of a guard
  fetchMock.mockReturnValue(new Promise(() => undefined))
  view = open('/teacher')
  expect(screen.getByText(en.ui.loading)).toBeTruthy()
  expect(screen.queryByRole('combobox')).toBeNull()
  view.unmount()

  // startup error state of a guard
  fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
  view = open('/teacher')
  await screen.findByText(en.startup.error)
  expect(screen.queryByRole('combobox')).toBeNull()
  view.unmount()

  // HomeRedirect while the user is not known yet
  fetchMock.mockReturnValue(new Promise(() => undefined))
  open('/')
  expect(screen.queryByRole('combobox')).toBeNull()
})

test('[web-client/R20-S3] The switch sits at its place on the page', async () => {
  // header: after "Log out", as the last control
  backend('ADMIN')
  let view = open('/admin')
  const logout = await screen.findByRole('button', { name: en.header.logout })
  const header = screen.getByRole('banner')
  const select = screen.getByRole('combobox', { name: selectName })
  expect(header.contains(select)).toBe(true)
  const controls = Array.from(
    header.querySelectorAll('button, select, a[href], input'),
  )
  expect(controls.indexOf(logout)).toBeGreaterThan(-1)
  expect(controls.indexOf(select)).toBe(controls.length - 1)
  expect(
    logout.compareDocumentPosition(select) & Node.DOCUMENT_POSITION_FOLLOWING,
  ).toBeTruthy()
  view.unmount()

  // login and set-password: before the title, first tab stop, outside the form
  for (const [path, title] of [
    ['/login', en.login.title],
    ['/set-password', en.setPassword.title],
  ]) {
    backend()
    view = open(path)
    const heading = await screen.findByRole('heading', { name: title })
    const lang = screen.getByRole('combobox', { name: selectName })
    expect(
      lang.compareDocumentPosition(heading) & Node.DOCUMENT_POSITION_FOLLOWING,
      path,
    ).toBeTruthy()
    const stops = Array.from(
      document.querySelectorAll<HTMLElement>(
        'a[href], button, input, select, textarea',
      ),
    ).filter((el) => !el.hasAttribute('disabled'))
    expect(stops[0], path).toBe(lang)
    expect(lang.closest('form'), path).toBeNull()
    view.unmount()
  }
})

test('[web-client/R20-S5] Choosing Russian changes the page at once', async () => {
  backend()
  open('/login')
  const select = await screen.findByRole('combobox', { name: 'Language' })
  select.focus()

  fireEvent.change(select, { target: { value: 'ru' } })

  const after = await screen.findByRole('combobox', { name: 'Язык' })
  expect(document.activeElement).toBe(after)
  expect(screen.getByRole('heading', { name: en.login.title })).toBeTruthy()
  expect(localStorage.getItem('teachdash.lang')).toBe('ru')
  expect(document.documentElement.lang).toBe('ru')
})

test('[web-client/R20-S7] A message on screen keeps its language until the next action', async () => {
  backend(undefined, {
    'auth/login': () => problem(401, 'INVALID_CREDENTIALS'),
  })
  open('/login')
  async function submit() {
    fireEvent.change(await screen.findByLabelText(en.login.email), {
      target: { value: 'a@example.com' },
    })
    fireEvent.change(screen.getByLabelText(en.login.password), {
      target: { value: 'secret-pass' },
    })
    fireEvent.click(screen.getByRole('button', { name: en.login.submit }))
  }
  await submit()
  expect(await screen.findByText(en.errors.INVALID_CREDENTIALS)).toBeTruthy()

  fireEvent.change(screen.getByRole('combobox', { name: 'Language' }), {
    target: { value: 'ru' },
  })
  await screen.findByRole('combobox', { name: 'Язык' })
  expect(screen.getByText(en.errors.INVALID_CREDENTIALS)).toBeTruthy()

  // the next failed login resolves its message in the language now active
  backend(undefined, {
    'auth/login': () => problem(401, 'TARGET_ACCOUNT_DEACTIVATED'),
  })
  await submit()
  await waitFor(() =>
    expect(screen.getByText(ru.errors.TARGET_ACCOUNT_DEACTIVATED)).toBeTruthy(),
  )
})
