import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import i18next from 'i18next'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import './i18n'
import { App } from './App'
import { leaves } from './i18n/bundleTools'
import en from './i18n/en.json'
import ru from './i18n/ru.json'

// Pages, dialogs and message states with the UI language Russian: no text on them
// is an `en` leaf value (R15-S3), and the names and roles keep their case (R15-S4).

const fetchMock = vi.fn()

// Same text in both languages: `app.name`, `uiLanguage.name.en`, and the `en` value of
// `uiLanguage.name.ru` (which is `languages.ru` in `ru`).
const ALLOW = new Set(['teachdash', 'English', 'Русский'])
const enValues = new Set(
  leaves(en)
    .map(([, text]) => text.trim())
    .filter((text) => !ALLOW.has(text)),
)

beforeEach(async () => {
  vi.stubGlobal('fetch', fetchMock)
  await i18next.changeLanguage('ru')
})

afterEach(async () => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  await i18next.changeLanguage('en')
  localStorage.clear()
  document.documentElement.lang = 'en'
})

// The trimmed text content of every element and the text attributes, as the user sees
// or hears them.
function shownTexts(): string[] {
  const found: string[] = []
  for (const element of Array.from(document.body.querySelectorAll('*'))) {
    found.push(element.textContent ?? '')
    for (const name of ['aria-label', 'title', 'placeholder', 'alt']) {
      found.push(element.getAttribute(name) ?? '')
    }
  }
  return found.map((text) => text.trim()).filter((text) => text !== '')
}

function expectNoEnglish(state: string) {
  const left = shownTexts().filter((text) => enValues.has(text))
  expect(left, `English text left on: ${state}`).toEqual([])
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function problem(status: number, code: string, errors?: unknown[]) {
  return new Response(JSON.stringify({ status, code, errors }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function user(role: string) {
  return {
    id: '1',
    name: 'Анна',
    email: 'anna@example.com',
    role,
    timeZone: 'UTC',
    nativeLanguage: role === 'TEACHER' ? 'de' : undefined,
  }
}

const teachers = [
  {
    id: '2',
    name: 'Борис',
    email: 'boris@example.com',
    role: 'TEACHER',
    timeZone: 'UTC',
    active: true,
    hasPassword: false,
  },
  {
    id: '3',
    name: 'Вера',
    email: 'vera@example.com',
    role: 'TEACHER',
    timeZone: 'UTC',
    active: true,
    hasPassword: true,
  },
  {
    id: '4',
    name: 'Глеб',
    email: 'gleb@example.com',
    role: 'TEACHER',
    timeZone: 'UTC',
    active: false,
    hasPassword: true,
  },
]

type Route = (init: RequestInit) => Response | Promise<Response>

// Answers by "METHOD path"; `routes` override the defaults. `role` undefined is anonymous.
function backend(role?: string, routes: Record<string, Route> = {}) {
  fetchMock.mockImplementation((url: string, init: RequestInit = {}) => {
    const path = String(url).replace('/api/', '')
    const key = `${init.method ?? 'GET'} ${path}`
    if (routes[key]) {
      return Promise.resolve(routes[key](init))
    }
    if (key === 'GET me') {
      return Promise.resolve(
        role ? json(user(role)) : problem(401, 'UNAUTHENTICATED'),
      )
    }
    if (key === 'GET admin/teachers') {
      return Promise.resolve(json(teachers))
    }
    if (key === 'GET languages') {
      return Promise.resolve(
        json(
          ['de', 'en', 'es', 'fr', 'it', 'pl', 'pt', 'ru', 'sr', 'uk'].map(
            (code) => ({ code }),
          ),
        ),
      )
    }
    return Promise.resolve(problem(500, 'INTERNAL_ERROR'))
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

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

// The default matcher folds U+00A0 into a plain space; the copy rules need it kept.
function findExact(text: string) {
  return screen.findByText(text, { normalizer: (value) => value.trim() })
}

async function heading(name: string) {
  return screen.findByRole('heading', { name })
}

test('[web-client/R15-S3] Russian pages show Russian text: login, set-password, not found', async () => {
  const pages: Array<[string, string, string]> = [
    ['/login', ru.login.title, 'login'],
    ['/set-password#token=abc', ru.setPassword.title, 'set-password'],
    ['/set-password', ru.setPassword.title, 'set-password, incomplete link'],
    ['/nowhere', ru.notFound.title, 'not found'],
  ]
  for (const [path, title, state] of pages) {
    backend()
    const view = open(path)
    await heading(title)
    expectNoEnglish(state)
    view.unmount()
  }
  expect(ru.login.title).toBe('Вход')
  expect(ru.setPassword.title).toBe('Задайте пароль')
  expect(ru.notFound.title).toBe('Страница не найдена')
  // the incomplete link shows its message
  backend()
  open('/set-password')
  expect(await screen.findByText(ru.setPassword.incompleteLink)).toBeTruthy()
  expect(screen.queryByLabelText(ru.setPassword.password)).toBeNull()
})

test('[web-client/R15-S3] Russian pages show Russian text: the cabinets and Profile', async () => {
  const pages: Array<[string, string, string, string]> = [
    ['TEACHER', '/teacher', ru.cabinet.teacher, 'учитель'],
    ['STUDENT', '/student', ru.cabinet.student, 'ученик'],
    ['TEACHER', '/profile', ru.profile.title, 'учитель'],
    ['ADMIN', '/profile', ru.profile.title, 'администратор'],
  ]
  for (const [role, path, title] of pages) {
    backend(role)
    const view = open(path)
    await heading(title)
    expectNoEnglish(`${role} ${path}`)
    view.unmount()
  }
  expect(ru.cabinet.teacher).toBe('Кабинет учителя')
  expect(ru.cabinet.student).toBe('Кабинет ученика')
  expect(ru.profile.title).toBe('Ваш профиль')
  // the back link of Profile and the language options
  backend('TEACHER')
  open('/profile')
  await heading(ru.profile.title)
  expect(
    screen.getByRole('button', { name: ru.header.backToCabinet }),
  ).toBeTruthy()
  await screen.findByRole('option', { name: ru.languages.uk })
  for (const code of Object.keys(en.languages).filter(
    (key) => key.length === 2,
  )) {
    expect(
      screen.getAllByRole('option', {
        name: ru.languages[code as keyof typeof ru.languages],
      }).length,
      code,
    ).toBeGreaterThan(0)
  }
  expectNoEnglish('profile with languages')
})

test('[web-client/R15-S3] Russian pages show Russian text: the admin list and its states', async () => {
  backend('ADMIN')
  const view = open('/admin')
  await heading(ru.cabinet.admin)
  expect(ru.cabinet.admin).toBe('Кабинет администратора')
  await screen.findByText('Борис')
  for (const text of [
    ru.adminTeachers.status.ACTIVE,
    ru.adminTeachers.status.WAITING,
    ru.adminTeachers.status.DEACTIVATED,
  ]) {
    expect(screen.getAllByText(text).length, text).toBeGreaterThan(0)
  }
  expectNoEnglish('admin list')
  view.unmount()

  // empty list
  backend('ADMIN', { 'GET admin/teachers': () => json([]) })
  const empty = open('/admin')
  expect(await screen.findByText(ru.adminTeachers.empty)).toBeTruthy()
  expectNoEnglish('admin empty')
  empty.unmount()

  // loading
  fetchMock.mockImplementation((url: string) =>
    String(url).endsWith('/me')
      ? Promise.resolve(json(user('ADMIN')))
      : new Promise(() => undefined),
  )
  const loading = open('/admin')
  expect(await screen.findByText(ru.ui.loading)).toBeTruthy()
  expectNoEnglish('admin loading')
  loading.unmount()

  // load error, with the retry button
  backend('ADMIN', {
    'GET admin/teachers': () => problem(500, 'INTERNAL_ERROR'),
  })
  open('/admin')
  expect(await screen.findByText(ru.errors.INTERNAL_ERROR)).toBeTruthy()
  expect(screen.getByRole('button', { name: ru.ui.tryAgain })).toBeTruthy()
  expectNoEnglish('admin load error')
})

test('[web-client/R15-S3] Russian pages show Russian text: the create and deactivate dialogs and the issued-link box', async () => {
  const listed = [...teachers]
  backend('ADMIN', {
    'GET admin/teachers': () => json(listed),
    'POST admin/teachers': () => {
      listed.push({
        id: '9',
        name: 'Дмитрий',
        email: 'dmitry@example.com',
        role: 'TEACHER',
        timeZone: 'UTC',
        active: true,
        hasPassword: false,
      })
      return json(
        { id: '9', setPasswordLink: 'http://localhost/x#token=t' },
        201,
      )
    },
  })
  open('/admin')
  await screen.findByText('Борис')

  // create dialog, with the client-side field errors
  fireEvent.click(screen.getByRole('button', { name: ru.adminTeachers.add }))
  const dialog = await screen.findByRole('dialog', {
    name: ru.adminTeachers.create.title,
  })
  await within(dialog).findByRole('option', { name: ru.languages.de })
  expectNoEnglish('create dialog')
  fireEvent.click(
    within(dialog).getByRole('button', {
      name: ru.adminTeachers.create.submit,
    }),
  )
  await within(dialog).findAllByText(ru.fieldErrors.REQUIRED)
  expect(within(dialog).getByText(ru.languages.required)).toBeTruthy()
  expectNoEnglish('create dialog, required errors')

  // the server-side form error and every field error code
  type(ru.adminTeachers.create.name, 'Дмитрий')
  type(ru.adminTeachers.create.email, 'dmitry@example.com')
  type(ru.adminTeachers.create.nativeLanguage, 'de')
  const submit = () =>
    fireEvent.click(
      within(dialog).getByRole('button', {
        name: ru.adminTeachers.create.submit,
      }),
    )
  fetchMock.mockImplementationOnce(() =>
    Promise.resolve(problem(400, 'VALIDATION_FAILED')),
  )
  submit()
  expect(
    await within(dialog).findByText(ru.adminTeachers.create.validationFailed),
  ).toBeTruthy()
  expectNoEnglish('create dialog, validation banner')
  for (const code of Object.keys(en.fieldErrors)) {
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        problem(400, 'VALIDATION_FAILED', [{ field: 'name', code }]),
      ),
    )
    submit()
    await within(dialog).findByText(
      ru.fieldErrors[code as keyof typeof ru.fieldErrors],
    )
    expectNoEnglish(`create dialog, fieldErrors.${code}`)
  }
  fetchMock.mockImplementationOnce(() =>
    Promise.resolve(problem(409, 'EMAIL_TAKEN', undefined)),
  )
  submit()
  await within(dialog).findByText(ru.errors.EMAIL_TAKEN)
  expectNoEnglish('create dialog, errors.EMAIL_TAKEN')

  // created: the issued-link box, the badge, copy by hand and copied
  submit()
  expect(await screen.findByText(ru.adminTeachers.justAdded)).toBeTruthy()
  const label = i18next.t('issuedLink.label', { name: 'Дмитрий' })
  expect(label).toBe('Ссылка для входа: Дмитрий')
  expect(await screen.findByText(label)).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: ru.issuedLink.copy }))
  expect(await screen.findByText(ru.issuedLink.copyByHand)).toBeTruthy()
  expectNoEnglish('issued link, copy by hand')
  vi.stubGlobal('navigator', {
    ...navigator,
    clipboard: { writeText: () => Promise.resolve() },
  })
  fireEvent.click(screen.getByRole('button', { name: ru.issuedLink.copy }))
  expect(await screen.findByText(ru.issuedLink.copied)).toBeTruthy()
  expectNoEnglish('issued link, copied')
  fireEvent.click(screen.getByRole('button', { name: ru.issuedLink.close }))

  // deactivate dialog
  const row = screen.getByText('Вера').closest('li') as HTMLElement
  fireEvent.click(
    within(row).getByRole('button', {
      name: ru.adminTeachers.actions.deactivate,
    }),
  )
  const confirm = await screen.findByRole('dialog', {
    name: i18next.t('adminTeachers.deactivate.title', { name: 'Вера' }),
  })
  expect(within(confirm).getByText(/Вера больше не сможет войти/)).toBeTruthy()
  expectNoEnglish('deactivate dialog')
  fireEvent.click(
    within(confirm).getByRole('button', {
      name: ru.adminTeachers.deactivate.cancel,
    }),
  )
  // the reactivate button of the deactivated teacher
  expect(
    screen.getByRole('button', { name: ru.adminTeachers.actions.reactivate }),
  ).toBeTruthy()
})

test('[web-client/R15-S3] Russian pages show Russian text: login messages', async () => {
  // every errors.* key as the message of a failed login
  backend()
  open('/login')
  await heading(ru.login.title)
  for (const code of Object.keys(en.errors)) {
    fetchMock.mockImplementationOnce(() =>
      Promise.resolve(
        problem(
          code === 'generic' ? 500 : 400,
          code === 'generic' ? 'UNKNOWN' : code,
        ),
      ),
    )
    type(ru.login.email, 'a@example.com')
    type(ru.login.password, 'secret-pass')
    fireEvent.click(screen.getByRole('button', { name: ru.login.submit }))
    await waitFor(() =>
      expect(
        screen.getAllByText(ru.errors[code as keyof typeof ru.errors]).length,
        code,
      ).toBeGreaterThan(0),
    )
    expectNoEnglish(`login error ${code}`)
  }
  cleanup()

  // the server cannot be reached: the first request (the session check) is answered,
  // the reachability check after it fails
  let calls = 0
  fetchMock.mockImplementation(() =>
    ++calls === 1
      ? Promise.resolve(problem(401, 'UNAUTHENTICATED'))
      : Promise.reject(new TypeError('Failed to fetch')),
  )
  open('/login')
  expect(await screen.findByText(ru.login.unreachable)).toBeTruthy()
  expectNoEnglish('login unreachable')
  cleanup()

  // the session has ended
  let ended = false
  fetchMock.mockImplementation((url: string) => {
    if (String(url).endsWith('/me')) {
      return Promise.resolve(
        ended ? problem(401, 'UNAUTHENTICATED') : json(user('ADMIN')),
      )
    }
    ended = true
    return Promise.resolve(problem(401, 'UNAUTHENTICATED'))
  })
  open('/admin')
  expect(await screen.findByText(ru.login.sessionEnded)).toBeTruthy()
  expectNoEnglish('login session ended')
})

test('[web-client/R15-S3] Russian pages show Russian text: startup, logout and language list failures', async () => {
  // startup error with the retry button
  fetchMock.mockImplementation(() => Promise.reject(new TypeError('offline')))
  open('/login')
  expect(await screen.findByText(ru.startup.error)).toBeTruthy()
  expect(screen.getByRole('button', { name: ru.ui.tryAgain })).toBeTruthy()
  expectNoEnglish('startup error')
  cleanup()

  // logout fails
  backend('TEACHER', {
    'POST auth/logout': () => problem(500, 'INTERNAL_ERROR'),
  })
  open('/teacher')
  await heading(ru.cabinet.teacher)
  fireEvent.click(screen.getByRole('button', { name: ru.header.logout }))
  expect(await screen.findByText(ru.header.logoutError)).toBeTruthy()
  expectNoEnglish('logout error')
  cleanup()

  // the language list cannot be loaded, and the required message
  backend('TEACHER', {
    'GET languages': () => problem(500, 'INTERNAL_ERROR'),
  })
  open('/profile')
  expect(await screen.findByText(ru.languages.loadFailed)).toBeTruthy()
  expectNoEnglish('languages load failed')
  expect(ru.languages.required).toBe('Выберите родной язык.')
})

test('[web-client/R15-S3] Russian pages show Russian text: the Profile messages', async () => {
  backend('TEACHER', {
    'PATCH me': () => json(user('TEACHER')),
    'POST me/password': () => new Response(null, { status: 204 }),
  })
  open('/profile')
  await heading(ru.profile.title)
  await screen.findByRole('option', { name: ru.languages.de })

  // saved
  fireEvent.click(screen.getByRole('button', { name: ru.profile.save }))
  expect(await screen.findByText(ru.profile.saved)).toBeTruthy()
  expectNoEnglish('profile saved')

  // validation banners of the teacher
  fetchMock.mockImplementationOnce(() =>
    Promise.resolve(problem(400, 'VALIDATION_FAILED')),
  )
  fireEvent.click(screen.getByRole('button', { name: ru.profile.save }))
  expect(
    await screen.findByText(ru.profile.validationFailedTeacher),
  ).toBeTruthy()
  expectNoEnglish('profile validation (teacher)')

  // password: client hints, then every banner
  type(ru.profile.currentPassword, 'old-password')
  type(ru.profile.newPassword, 'short')
  expect(await findExact(ru.password.tooShort)).toBeTruthy()
  expectNoEnglish('password too short')
  type(ru.profile.newPassword, 'x'.repeat(80))
  expect(await findExact(ru.password.tooLong)).toBeTruthy()
  expectNoEnglish('password too long')
  type(ru.profile.newPassword, 'new-password-1')
  type(ru.profile.repeatPassword, 'different-1')
  expect(await screen.findByText(ru.password.mismatch)).toBeTruthy()
  expectNoEnglish('password mismatch')
  type(ru.profile.repeatPassword, 'new-password-1')

  const change = () =>
    fireEvent.click(
      screen.getByRole('button', { name: ru.profile.changePassword }),
    )
  fetchMock.mockImplementationOnce(() =>
    Promise.resolve(problem(403, 'WRONG_PASSWORD')),
  )
  change()
  expect(await screen.findByText(ru.profile.wrongPassword)).toBeTruthy()
  expectNoEnglish('profile wrong password')
  fetchMock.mockImplementationOnce(() =>
    Promise.resolve(problem(400, 'VALIDATION_FAILED')),
  )
  change()
  expect(
    await screen.findByText(ru.profile.passwordValidationFailed),
  ).toBeTruthy()
  expectNoEnglish('profile password validation')
  change()
  expect(await screen.findByText(ru.profile.passwordChanged)).toBeTruthy()
  expectNoEnglish('password changed')

  // the show and hide toggles, with the label in the accessible name
  const toggle = screen.getAllByRole('button', {
    name: i18next.t('ui.showPassword', { label: ru.profile.currentPassword }),
  })[0]
  expect(toggle.textContent).toBe(ru.ui.show)
  fireEvent.click(toggle)
  expect(
    screen.getByRole('button', {
      name: i18next.t('ui.hidePassword', { label: ru.profile.currentPassword }),
    }).textContent,
  ).toBe(ru.ui.hide)
  expectNoEnglish('password shown')
  cleanup()

  // the validation banner of a student-like user has no native language
  backend('ADMIN', {
    'PATCH me': () => problem(400, 'VALIDATION_FAILED'),
  })
  open('/profile')
  await heading(ru.profile.title)
  fireEvent.click(screen.getByRole('button', { name: ru.profile.save }))
  expect(await screen.findByText(ru.profile.validationFailed)).toBeTruthy()
  expectNoEnglish('profile validation (no native language)')
})

test('[web-client/R15-S3] Russian pages show Russian text: the set-password messages', async () => {
  backend(undefined, {
    'POST password-links': () => problem(400, 'LINK_INVALID'),
  })
  open('/set-password#token=abc')
  await heading(ru.setPassword.title)
  type(ru.setPassword.password, 'short')
  expect(await findExact(ru.password.tooShort)).toBeTruthy()
  type(ru.setPassword.password, 'long-enough-1')
  type(ru.setPassword.confirm, 'other-enough-1')
  expect(await screen.findByText(ru.password.mismatch)).toBeTruthy()
  expectNoEnglish('set-password hints')
  type(ru.setPassword.confirm, 'long-enough-1')
  fireEvent.click(screen.getByRole('button', { name: ru.setPassword.submit }))
  expect(await screen.findByText(ru.errors.LINK_INVALID)).toBeTruthy()
  expectNoEnglish('set-password link invalid')
})

test('[web-client/R15-S4] Names and roles keep their case', async () => {
  backend('TEACHER')
  const teacher = open('/teacher')
  await heading(ru.cabinet.teacher)
  expect(await screen.findByText('Анна (учитель)')).toBeTruthy()
  teacher.unmount()

  const withoutPassword = { ...teachers[0], name: 'Анна' }
  const withPassword = { ...teachers[1], name: 'Вера' }
  backend('ADMIN', {
    'GET admin/teachers': () => json([withoutPassword, withPassword]),
    'POST admin/users/2/reset-link': () => json({ link: 'http://x/#token=t' }),
  })
  open('/admin')
  const [invited, reset] = await within(
    await screen.findByRole('list'),
  ).findAllByRole('listitem')
  const invite = within(invited).getByRole('button', {
    name: 'Новое приглашение',
  })
  expect(
    within(reset).getByRole('button', { name: 'Ссылка для сброса пароля' }),
  ).toBeTruthy()

  fireEvent.click(invite)
  expect(await screen.findByText('Ссылка для входа: Анна')).toBeTruthy()

  fireEvent.click(
    within(invited).getByRole('button', { name: 'Деактивировать' }),
  )
  expect(
    await screen.findByRole('dialog', { name: 'Деактивировать: Анна?' }),
  ).toBeTruthy()
})
