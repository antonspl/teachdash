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

function userResponse(
  role: string,
  name = 'Anna Smith',
  timeZone = 'UTC',
  nativeLanguage: string | undefined = role === 'ADMIN' ? undefined : 'ru',
) {
  return new Response(
    JSON.stringify({
      id: '1',
      name,
      email: 'anna@example.com',
      role,
      timeZone,
      nativeLanguage,
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

function problem(
  status: number,
  code: string,
  errors?: { field: string; code: string }[],
) {
  return new Response(JSON.stringify({ status, code, errors }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function languagesResponse(codes = ['de', 'en', 'ru']) {
  return Promise.resolve(
    new Response(
      JSON.stringify(codes.map((code) => ({ code, content: false }))),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ),
  )
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
    if (url === '/api/languages') {
      return languagesResponse()
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
  expect(
    (screen.getByLabelText(en.profile.timeZone) as HTMLSelectElement).value,
  ).toBe('Asia/Dubai')
  expect(headerHas('Anna S\\. \\(')).toBe(true)
  expect(screen.queryByText(/Anna Smith/)).toBeNull()
  expect(saves()).toHaveLength(1)
  expect(sentBody(saves()[0])).toEqual({
    name: '  Anna S.  ',
    timeZone: 'UTC',
    nativeLanguage: 'ru',
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
  for (const [role, message] of [
    ['TEACHER', en.profile.validationFailedTeacher],
    ['STUDENT', en.profile.validationFailed],
  ] as const) {
    // given
    fetchMock.mockReset()
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url === '/api/me' && init?.method === 'PATCH') {
        return Promise.resolve(problem(400, 'VALIDATION_FAILED'))
      }
      if (url === '/api/languages') {
        return languagesResponse()
      }
      return Promise.resolve(userResponse(role))
    })
    const view = open()
    try {
      await screen.findByLabelText(en.profile.name)
      typeName('Anna Typed')

      // when
      pressSave()

      // then
      expect(await screen.findByText(message), role).toBeTruthy()
      expect(message).toMatch(/name/)
      expect(message).toMatch(/time zone/)
      expect(/native language/.test(message), role).toBe(role === 'TEACHER')
      expect(
        (screen.getByLabelText(en.profile.name) as HTMLInputElement).value,
      ).toBe('Anna Typed')
      expect(headerHas('Anna Smith')).toBe(true)
      expect(
        (
          screen.getByRole('button', {
            name: en.profile.save,
          }) as HTMLButtonElement
        ).disabled,
      ).toBe(false)
      pressSave()
      await waitFor(() => expect(saves()).toHaveLength(2))
    } finally {
      view.unmount()
    }
  }
})

test('[web-profile/R2-S6] Save that fails for another reason', async () => {
  for (const [failure, message] of [
    [() => Promise.reject(new TypeError('Failed to fetch')), en.errors.generic],
    [
      () => Promise.resolve(problem(500, 'INTERNAL_ERROR')),
      en.errors.INTERNAL_ERROR,
    ],
    [
      () => Promise.resolve(problem(403, 'CSRF_INVALID')),
      en.errors.CSRF_INVALID,
    ],
  ] as const) {
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
    expect(status.textContent).toBe(message)
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
    nativeLanguage: 'ru',
  })
})

test('[web-profile/R2-S8] A second press while saving sends nothing', async () => {
  // given
  teacherWithSave(() => new Promise(() => undefined))
  await openForm()
  pressSave()
  await waitFor(() => expect(saves()).toHaveLength(1))

  // then the fields wait too
  expect(
    (screen.getByLabelText(en.profile.name) as HTMLInputElement).disabled,
  ).toBe(true)
  expect(
    (screen.getByLabelText(en.profile.timeZone) as HTMLSelectElement).disabled,
  ).toBe(true)

  // when the same button, now named by its busy label, is pressed again
  const busy = screen.getByRole<HTMLButtonElement>('button', {
    name: en.profile.saving,
  })
  fireEvent.click(busy)
  fireEvent.submit(busy)

  // then
  await new Promise((resolve) => setTimeout(resolve, 20))
  expect(saves()).toHaveLength(1)
  expect(busy.disabled).toBe(true)
  expect(busy.getAttribute('aria-busy')).toBe('true')
})

function languageSelect() {
  return screen.getByLabelText(en.profile.nativeLanguage) as HTMLSelectElement
}

function languageOptions() {
  return screen
    .getAllByRole('option')
    .filter((option) => option.parentElement === languageSelect())
    .map((option) => option.textContent)
}

function selectedLanguage() {
  const select = languageSelect()
  return select.selectedOptions[0]?.textContent
}

// A teacher whose stored language changes with each save, so a reload shows it.
function teacherWithStoredLanguage(stored: string) {
  let language = stored
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url === '/api/me' && init?.method === 'PATCH') {
      language = (JSON.parse(init.body as string) as { nativeLanguage: string })
        .nativeLanguage
    }
    if (url === '/api/languages') {
      return languagesResponse()
    }
    return Promise.resolve(
      userResponse('TEACHER', 'Anna Smith', 'UTC', language),
    )
  })
}

test('[web-profile/R2-S9] A teacher changes the native language', async () => {
  // given
  teacherWithStoredLanguage('ru')
  const view = open()
  await screen.findByRole('option', { name: 'German' })
  expect(languageOptions()).toEqual(['German', 'English', 'Russian'])
  expect(selectedLanguage()).toBe('Russian')

  // when
  fireEvent.change(languageSelect(), { target: { value: 'de' } })
  pressSave()
  expect(await screen.findByText(en.profile.saved)).toBeTruthy()
  view.unmount()
  open()
  await screen.findByRole('option', { name: 'German' })

  // then
  expect(saves()).toHaveLength(1)
  expect(
    (sentBody(saves()[0]) as { nativeLanguage: string }).nativeLanguage,
  ).toBe('de')
  expect(selectedLanguage()).toBe('German')
})

test('[web-profile/R2-S10] Students and admins have no language select', async () => {
  for (const role of ['STUDENT', 'ADMIN']) {
    // given
    fetchMock.mockReset()
    fetchMock.mockImplementation((url: string, init?: RequestInit) => {
      if (url === '/api/me' && init?.method === 'PATCH') {
        return Promise.resolve(userResponse(role))
      }
      return Promise.resolve(userResponse(role, 'Anna Smith', 'UTC', 'ru'))
    })
    const view = open()
    try {
      await screen.findByLabelText(en.profile.name)

      // when
      typeName('Anna S.')
      pressSave()

      // then
      expect(await screen.findByText(en.profile.saved), role).toBeTruthy()
      expect(
        screen.queryByLabelText(en.profile.nativeLanguage),
        role,
      ).toBeNull()
      expect(sentBody(saves()[0]), role).toEqual({
        name: 'Anna S.',
        timeZone: 'UTC',
      })
      expect(
        fetchMock.mock.calls.some(([url]) => url === '/api/languages'),
        role,
      ).toBe(false)
    } finally {
      view.unmount()
    }
  }
})

test('[web-profile/R2-S11] A language the backend rejects', async () => {
  // given
  teacherWithSave(() =>
    Promise.resolve(
      problem(400, 'VALIDATION_FAILED', [
        { field: 'nativeLanguage', code: 'INVALID_FORMAT' },
      ]),
    ),
  )
  open()
  await screen.findByRole('option', { name: 'German' })
  fireEvent.change(languageSelect(), { target: { value: 'de' } })

  // when
  pressSave()

  // then
  expect(await screen.findByText(en.fieldErrors.INVALID_FORMAT)).toBeTruthy()
  expect(describedBy(en.profile.nativeLanguage)).toBe(
    en.fieldErrors.INVALID_FORMAT,
  )
  expect(languageSelect().value).toBe('de')
  expect(
    (screen.getByRole('button', { name: en.profile.save }) as HTMLButtonElement)
      .disabled,
  ).toBe(false)
  pressSave()
  await waitFor(() => expect(saves()).toHaveLength(2))
})

test('[web-profile/R2-S12] The language list does not load', async () => {
  // given
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url === '/api/languages') {
      return Promise.reject(new TypeError('Failed to fetch'))
    }
    if (url === '/api/me' && init?.method === 'PATCH') {
      return Promise.resolve(userResponse('TEACHER', 'Anna S.'))
    }
    return Promise.resolve(userResponse('TEACHER'))
  })

  // when
  await openForm()
  expect(await screen.findByText(en.languages.loadFailed)).toBeTruthy()
  typeName('Anna S.')
  pressSave()

  // then
  expect(await screen.findByText(en.profile.saved)).toBeTruthy()
  expect(screen.getByRole('button', { name: en.ui.tryAgain })).toBeTruthy()
  expect(languageOptions()).toEqual(['Russian'])
  expect(sentBody(saves()[0])).toEqual({
    name: 'Anna S.',
    timeZone: 'UTC',
    nativeLanguage: 'ru',
  })
})

test('[web-profile/R2-S13] Try again loads the list', async () => {
  // given
  let listCalls = 0
  fetchMock.mockImplementation((url: string) => {
    if (url === '/api/languages') {
      listCalls += 1
      return listCalls === 1
        ? Promise.reject(new TypeError('Failed to fetch'))
        : languagesResponse()
    }
    return Promise.resolve(userResponse('TEACHER'))
  })
  open()
  await screen.findByText(en.languages.loadFailed)

  // when
  fireEvent.click(screen.getByRole('button', { name: en.ui.tryAgain }))

  // then
  await screen.findByRole('option', { name: 'German' })
  expect(screen.queryByText(en.languages.loadFailed)).toBeNull()
  expect(languageOptions()).toEqual(['German', 'English', 'Russian'])
  expect(selectedLanguage()).toBe('Russian')
})

test('[web-profile/R2-S14] A language with no name shows its code', async () => {
  // given
  expect('sv' in en.languages).toBe(false)
  fetchMock.mockImplementation((url: string) => {
    if (url === '/api/languages') {
      return languagesResponse(['de', 'sv'])
    }
    return Promise.resolve(userResponse('TEACHER', 'Anna Smith', 'UTC', 'sv'))
  })

  // when
  open()
  await screen.findByRole('option', { name: 'German' })

  // then
  expect(languageOptions()).toEqual(['German', 'sv'])
  expect(selectedLanguage()).toBe('sv')
})

test('[web-profile/R2-S15] The current-user state gets the new language', async () => {
  // given
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url === '/api/languages') {
      return languagesResponse()
    }
    if (url === '/api/me' && init?.method === 'PATCH') {
      return Promise.resolve(userResponse('TEACHER', 'Anna Smith', 'UTC', 'de'))
    }
    return Promise.resolve(userResponse('TEACHER'))
  })
  open()
  await screen.findByRole('option', { name: 'German' })
  const currentUserRequests = () =>
    fetchMock.mock.calls.filter(
      ([url, init]) =>
        url === '/api/me' &&
        (init as RequestInit | undefined)?.method === undefined,
    ).length
  const before = currentUserRequests()

  // when
  fireEvent.change(languageSelect(), { target: { value: 'de' } })
  pressSave()
  await screen.findByText(en.profile.saved)
  fireEvent.click(screen.getByRole('button', { name: en.header.backToCabinet }))
  await screen.findByText(en.cabinet.teacher)
  fireEvent.click(screen.getByRole('button', { name: en.header.profile }))
  await screen.findByRole('option', { name: 'German' })

  // then
  expect(selectedLanguage()).toBe('German')
  expect(currentUserRequests()).toBe(before)
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

test('After logging in again a profile path with a trailing slash is shown', async () => {
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
    return Promise.resolve(userResponse('STUDENT', 'Sam Student'))
  })
  openInBrowser('/profile/')
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
  expect(
    (screen.getByLabelText(en.profile.name) as HTMLInputElement).value,
  ).toBe('Sam Student')
})

test('[web-profile/R1-S3] Page shows the current values', async () => {
  // given
  teacherWithSave(
    () => Promise.resolve(userResponse('TEACHER')),
    'Europe/Moscow',
  )

  // when
  open()
  await screen.findByLabelText(en.profile.name)

  // then
  const email = screen.getByText(/anna@example\.com/)
  expect(email.tagName).not.toBe('INPUT')
  expect(
    screen
      .getAllByRole('textbox')
      .some((field) => (field as HTMLInputElement).value.includes('anna@')),
  ).toBe(false)
  expect(
    (screen.getByLabelText(en.profile.name) as HTMLInputElement).value,
  ).toBe('Anna Smith')
  expect(
    (screen.getByLabelText(en.profile.timeZone) as HTMLSelectElement).value,
  ).toBe('Europe/Moscow')
  for (const label of [
    en.profile.currentPassword,
    en.profile.newPassword,
    en.profile.repeatPassword,
  ]) {
    expect(
      (screen.getByLabelText(label) as HTMLInputElement).value,
      label,
    ).toBe('')
  }
})

// Logged in as a teacher; every POST /api/me/password goes to `onChange`, and every
// PATCH /api/me answers with the same user.
function teacherWithPassword(onChange: () => Promise<Response>) {
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url === '/api/me/password' && init?.method === 'POST') {
      return onChange()
    }
    if (url === '/api/languages') {
      return languagesResponse()
    }
    return Promise.resolve(userResponse('TEACHER'))
  })
}

function changes() {
  return fetchMock.mock.calls.filter(([url]) => url === '/api/me/password')
}

function noContent() {
  return Promise.resolve(new Response(null, { status: 204 }))
}

function field(label: string) {
  return screen.getByLabelText(label) as HTMLInputElement
}

function typePasswords(current: string, next: string, repeat = next) {
  fireEvent.change(field(en.profile.currentPassword), {
    target: { value: current },
  })
  fireEvent.change(field(en.profile.newPassword), { target: { value: next } })
  fireEvent.change(field(en.profile.repeatPassword), {
    target: { value: repeat },
  })
}

function pressChange() {
  fireEvent.click(
    screen.getByRole('button', { name: en.profile.changePassword }),
  )
}

function button(name: string) {
  return screen.getByRole('button', { name }) as HTMLButtonElement
}

test('[web-profile/R3-S2] Success empties the fields', async () => {
  // given
  teacherWithPassword(noContent)
  await openForm()
  typePasswords('old-password', 'new-password-1')

  fireEvent.click(
    screen.getByRole('button', {
      name: en.ui.showPassword.replace('{{label}}', en.profile.newPassword),
    }),
  )
  expect(field(en.profile.newPassword).type).toBe('text')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.profile.passwordChanged)).toBeTruthy()
  expect(changes()).toHaveLength(1)
  expect(sentBody(changes()[0])).toEqual({
    currentPassword: 'old-password',
    newPassword: 'new-password-1',
  })
  for (const label of [
    en.profile.currentPassword,
    en.profile.newPassword,
    en.profile.repeatPassword,
  ]) {
    expect(field(label).value, label).toBe('')
    expect(field(label).type, label).toBe('password')
  }
  fireEvent.change(field(en.profile.currentPassword), {
    target: { value: 'x' },
  })
  expect(screen.queryByText(en.profile.passwordChanged)).toBeNull()
})

test('[web-profile/R3-S3] New passwords that differ are not sent', async () => {
  // given
  teacherWithPassword(noContent)
  await openForm()
  typePasswords('old-password', 'new-password-1', 'new-password-2')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.password.mismatch)).toBeTruthy()
  expect(changes()).toHaveLength(0)
  const id = (
    screen.getByLabelText(en.profile.repeatPassword) as HTMLElement
  ).getAttribute('aria-describedby')
  expect(document.getElementById(id ?? '')?.textContent).toBe(
    en.password.mismatch,
  )
})

test('[web-profile/R3-S4] A short new password is not sent', async () => {
  // given
  teacherWithPassword(noContent)
  await openForm()
  typePasswords('old-password', '1234567')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.password.tooShort)).toBeTruthy()
  expect(changes()).toHaveLength(0)
  const id = (
    screen.getByLabelText(en.profile.newPassword) as HTMLElement
  ).getAttribute('aria-describedby')
  expect(document.getElementById(id ?? '')?.textContent).toBe(
    en.password.tooShort,
  )
})

test('[web-profile/R3-S5] A new password over 72 bytes is not sent', async () => {
  // given
  teacherWithPassword(noContent)
  await openForm()
  typePasswords('old-password', 'ж'.repeat(40))

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.password.tooLong)).toBeTruthy()
  expect(changes()).toHaveLength(0)
  const id = (
    screen.getByLabelText(en.profile.newPassword) as HTMLElement
  ).getAttribute('aria-describedby')
  expect(document.getElementById(id ?? '')?.textContent).toBe(
    en.password.tooLong,
  )
})

test('[web-profile/R3-S6] Wrong current password', async () => {
  // given
  teacherWithPassword(() => Promise.resolve(problem(400, 'WRONG_PASSWORD')))
  await openForm()
  typePasswords('old-password', 'new-password-1')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.profile.wrongPassword)).toBeTruthy()
  expect(field(en.profile.currentPassword).value).toBe('old-password')
  expect(field(en.profile.newPassword).value).toBe('new-password-1')
  expect(field(en.profile.repeatPassword).value).toBe('new-password-1')
  expect(button(en.profile.changePassword).disabled).toBe(false)
  pressChange()
  await waitFor(() => expect(changes()).toHaveLength(2))
})

test('[web-profile/R3-S7] New password the backend rejects', async () => {
  // given
  teacherWithPassword(() => Promise.resolve(problem(400, 'VALIDATION_FAILED')))
  await openForm()
  typePasswords('old-password', 'new-password-1')

  // when
  pressChange()

  // then
  expect(
    await screen.findByText(en.profile.passwordValidationFailed),
  ).toBeTruthy()
  expect(button(en.profile.changePassword).disabled).toBe(false)
  pressChange()
  await waitFor(() => expect(changes()).toHaveLength(2))
})

test('[web-profile/R3-S8] Password change that fails for another reason', async () => {
  for (const [failure, message] of [
    [() => Promise.reject(new TypeError('Failed to fetch')), en.errors.generic],
    [
      () => Promise.resolve(problem(500, 'INTERNAL_ERROR')),
      en.errors.INTERNAL_ERROR,
    ],
    [
      () => Promise.resolve(problem(403, 'CSRF_INVALID')),
      en.errors.CSRF_INVALID,
    ],
  ] as const) {
    // given
    fetchMock.mockReset()
    teacherWithPassword(failure)
    const view = open()
    await screen.findByLabelText(en.profile.name)
    typePasswords('old-password', 'new-password-1')

    // when
    pressChange()

    // then
    const status = await screen.findByRole('status')
    expect(status.textContent).toBe(message)
    expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
    expect(field(en.profile.currentPassword).value).toBe('old-password')
    expect(field(en.profile.newPassword).value).toBe('new-password-1')
    expect(field(en.profile.repeatPassword).value).toBe('new-password-1')
    expect(button(en.profile.changePassword).disabled).toBe(false)
    view.unmount()
  }
})

test('[web-profile/R3-S9] A second press while changing sends nothing', async () => {
  // given
  teacherWithPassword(() => new Promise(() => undefined))
  await openForm()
  typePasswords('old-password', 'new-password-1')
  pressChange()
  await waitFor(() => expect(changes()).toHaveLength(1))

  // then the fields wait too
  for (const label of [
    en.profile.currentPassword,
    en.profile.newPassword,
    en.profile.repeatPassword,
  ]) {
    expect(field(label).disabled, label).toBe(true)
  }

  // when the same button, now named by its busy label, is pressed again
  const busy = screen.getByRole<HTMLButtonElement>('button', {
    name: en.profile.changingPassword,
  })
  fireEvent.click(busy)
  fireEvent.submit(busy)

  // then
  await new Promise((resolve) => setTimeout(resolve, 20))
  expect(changes()).toHaveLength(1)
  expect(busy.disabled).toBe(true)
  expect(busy.getAttribute('aria-busy')).toBe('true')
})

test('[web-profile/R3-S10] The two forms do not affect each other', async () => {
  // given
  teacherWithPassword(() =>
    Promise.resolve(
      problem(400, 'WRONG_PASSWORD', [
        { field: 'currentPassword', code: 'WRONG_PASSWORD' },
      ]),
    ),
  )
  await openForm()
  typePasswords('old-password', 'new-password-1')
  pressChange()
  await screen.findByText(en.profile.wrongPassword)
  expect(describedBy(en.profile.currentPassword)).toBe(en.profile.wrongPassword)

  // when
  typeName('Anna S.')
  pressSave()

  // then
  expect(await screen.findByText(en.profile.saved)).toBeTruthy()
  expect(saves()).toHaveLength(1)
  expect(sentBody(saves()[0])).toEqual({
    name: 'Anna S.',
    timeZone: 'UTC',
    nativeLanguage: 'ru',
  })
  expect(screen.getByText(en.profile.wrongPassword)).toBeTruthy()
  expect(field(en.profile.currentPassword).value).toBe('old-password')
  expect(field(en.profile.newPassword).value).toBe('new-password-1')
  expect(field(en.profile.repeatPassword).value).toBe('new-password-1')
  fireEvent.change(field(en.profile.newPassword), {
    target: { value: 'another-password' },
  })
  expect(screen.getByText(en.profile.wrongPassword)).toBeTruthy()
  expect(screen.getByText(en.profile.saved)).toBeTruthy()
  fireEvent.change(field(en.profile.currentPassword), {
    target: { value: 'other-old-password' },
  })
  expect(screen.queryByText(en.profile.wrongPassword)).toBeNull()
  expect(screen.getByText(en.profile.saved)).toBeTruthy()
})

test('[web-profile/R3-S11] One form waits while the other is sending', async () => {
  // First run: a password change is running, the profile form's button waits.
  // given
  let finishChange: (response: Response) => void = () => undefined
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    if (url === '/api/me/password' && init?.method === 'POST') {
      return new Promise<Response>((resolve) => {
        finishChange = resolve
      })
    }
    return Promise.resolve(userResponse('TEACHER'))
  })
  const first = open()
  await screen.findByLabelText(en.profile.name)
  typePasswords('old-password', 'new-password-1')

  // when
  pressChange()
  await waitFor(() => expect(changes()).toHaveLength(1))

  // then
  expect(button(en.profile.save).disabled).toBe(true)
  expect(button(en.profile.save).getAttribute('aria-busy')).toBeNull()
  finishChange(new Response(null, { status: 204 }))
  await screen.findByText(en.profile.passwordChanged)
  expect(button(en.profile.save).disabled).toBe(false)
  first.unmount()

  // Second run: a save is running, the password form's button waits.
  // given
  fetchMock.mockReset()
  let finishSave: (response: Response) => void = () => undefined
  teacherWithSave(
    () =>
      new Promise<Response>((resolve) => {
        finishSave = resolve
      }),
  )
  open()
  await screen.findByLabelText(en.profile.name)

  // when
  pressSave()
  await waitFor(() => expect(saves()).toHaveLength(1))

  // then
  expect(button(en.profile.changePassword).disabled).toBe(true)
  expect(button(en.profile.changePassword).getAttribute('aria-busy')).toBeNull()
  finishSave(userResponse('TEACHER'))
  await screen.findByText(en.profile.saved)
  expect(button(en.profile.changePassword).disabled).toBe(false)
})

test('[web-profile/R4-S2] A password change with an ended session', async () => {
  // given
  teacherWithPassword(() => Promise.resolve(problem(401, 'UNAUTHENTICATED')))
  openInBrowser('/profile')
  await screen.findByLabelText(en.profile.name)
  typePasswords('old-password', 'new-password-1')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.login.sessionEnded)).toBeTruthy()
  expect(screen.getByRole('heading', { name: en.login.title })).toBeTruthy()
})

function describedBy(label: string) {
  const id = screen.getByLabelText(label).getAttribute('aria-describedby')
  return document.getElementById(id ?? '')?.textContent
}

test('[web-client/R17-S2] The profile form places a time zone error under the select', async () => {
  // given
  teacherWithSave(() =>
    Promise.resolve(
      problem(400, 'VALIDATION_FAILED', [
        { field: 'timeZone', code: 'INVALID_FORMAT' },
      ]),
    ),
  )
  await openForm()
  typeName('Anna Typed')

  // when
  pressSave()

  // then
  expect(await screen.findByText(en.fieldErrors.INVALID_FORMAT)).toBeTruthy()
  const select = screen.getByLabelText(en.profile.timeZone)
  expect(select.getAttribute('aria-invalid')).toBe('true')
  expect(describedBy(en.profile.timeZone)).toBe(en.fieldErrors.INVALID_FORMAT)
  expect(screen.queryByText(en.profile.validationFailed)).toBeNull()
  expect(select.closest('form')?.querySelector('[role="status"]')).toBeNull()
  expect(field(en.profile.name).value).toBe('Anna Typed')
  expect(headerHas('Anna Smith')).toBe(true)
  await waitFor(() => expect(document.activeElement).toBe(select))
})

test('[web-client/R17-S2] The password form places a current password error', async () => {
  // given
  teacherWithPassword(() =>
    Promise.resolve(
      problem(400, 'VALIDATION_FAILED', [
        { field: 'currentPassword', code: 'REQUIRED' },
      ]),
    ),
  )
  await openForm()
  typePasswords('old-password', 'new-password-1')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.fieldErrors.REQUIRED)).toBeTruthy()
  expect(field(en.profile.currentPassword).getAttribute('aria-invalid')).toBe(
    'true',
  )
  expect(describedBy(en.profile.currentPassword)).toBe(en.fieldErrors.REQUIRED)
  expect(screen.queryByText(en.profile.passwordValidationFailed)).toBeNull()
  expect(
    field(en.profile.currentPassword)
      .closest('form')
      ?.querySelector('[role="status"]'),
  ).toBeNull()
})

test('[web-client/R17-S2] The password form places a new password error', async () => {
  // given
  teacherWithPassword(() =>
    Promise.resolve(
      problem(400, 'VALIDATION_FAILED', [
        { field: 'newPassword', code: 'TOO_LONG' },
      ]),
    ),
  )
  await openForm()
  typePasswords('old-password', 'new-password-1')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.fieldErrors.TOO_LONG)).toBeTruthy()
  expect(field(en.profile.newPassword).getAttribute('aria-invalid')).toBe(
    'true',
  )
  expect(describedBy(en.profile.newPassword)).toBe(en.fieldErrors.TOO_LONG)
  expect(screen.queryByText(en.profile.passwordValidationFailed)).toBeNull()
  expect(
    field(en.profile.newPassword)
      .closest('form')
      ?.querySelector('[role="status"]'),
  ).toBeNull()
})

test('[web-client/R17-S8] Focus goes to the current password field', async () => {
  // given
  teacherWithPassword(() =>
    Promise.resolve(
      problem(400, 'VALIDATION_FAILED', [
        { field: 'newPassword', code: 'TOO_LONG' },
        { field: 'currentPassword', code: 'REQUIRED' },
      ]),
    ),
  )
  await openForm()
  typePasswords('old-password', 'new-password-1')

  // when
  pressChange()

  // then
  expect(await screen.findByText(en.fieldErrors.TOO_LONG)).toBeTruthy()
  expect(describedBy(en.profile.currentPassword)).toBe(en.fieldErrors.REQUIRED)
  expect(describedBy(en.profile.newPassword)).toBe(en.fieldErrors.TOO_LONG)
  await waitFor(() =>
    expect(document.activeElement).toBe(field(en.profile.currentPassword)),
  )
})

test('[web-client/R17-S4] The profile form keeps its message for an unknown code', async () => {
  // given
  teacherWithSave(() =>
    Promise.resolve(
      problem(400, 'VALIDATION_FAILED', [
        { field: 'name', code: 'SOMETHING_NEW' },
      ]),
    ),
  )
  await openForm()
  typeName('Anna Typed')

  // when
  pressSave()

  // then
  expect(
    await screen.findByText(en.profile.validationFailedTeacher),
  ).toBeTruthy()
  expect(field(en.profile.name).getAttribute('aria-invalid')).toBeNull()
  expect(field(en.profile.name).getAttribute('aria-describedby')).toBeNull()
})

test('A teacher without a stored language gets a translated message, not a native bubble', async () => {
  // given a teacher the backend returns with no native language
  fetchMock.mockImplementation((url: string) => {
    if (url === '/api/languages') {
      return languagesResponse()
    }
    return Promise.resolve(
      new Response(
        JSON.stringify({
          id: '1',
          name: 'Anna Smith',
          email: 'anna@example.com',
          role: 'TEACHER',
          timeZone: 'UTC',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )
  })
  open()
  await screen.findByRole('option', { name: 'German' })
  expect(languageSelect().required).toBe(false)
  expect(languageSelect().getAttribute('aria-required')).toBe('true')

  // when
  pressSave()

  // then
  expect(await screen.findByText(en.languages.required)).toBeTruthy()
  expect(describedBy(en.profile.nativeLanguage)).toBe(en.languages.required)
  expect(saves()).toHaveLength(0)

  // and choosing a language clears it and allows the save
  fireEvent.change(languageSelect(), { target: { value: 'de' } })
  expect(screen.queryByText(en.languages.required)).toBeNull()
})

test('A blank name and a missing language are shown together', async () => {
  // given a teacher without a language
  fetchMock.mockImplementation((url: string) => {
    if (url === '/api/languages') {
      return languagesResponse()
    }
    return Promise.resolve(
      new Response(
        JSON.stringify({
          id: '1',
          name: 'Anna Smith',
          email: 'anna@example.com',
          role: 'TEACHER',
          timeZone: 'UTC',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )
  })
  open()
  await screen.findByRole('option', { name: 'German' })
  fireEvent.change(screen.getByLabelText(en.profile.name), {
    target: { value: '  ' },
  })

  // when
  pressSave()

  // then both errors are shown and nothing is saved
  expect(await screen.findByText(en.languages.required)).toBeTruthy()
  expect(screen.getByText(en.fieldErrors.REQUIRED)).toBeTruthy()
  expect(saves()).toHaveLength(0)
})

test('[web-client/R29-S3] Save shows its busy label while the request runs, then its idle label after a failure and after a success', async () => {
  // given a save that is held
  let finish: (response: Response) => void = () => undefined
  teacherWithSave(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve
      }),
  )
  await openForm()
  const idle = button(en.profile.save)
  expect(idle.getAttribute('aria-busy')).toBeNull()

  // when it is pressed
  pressSave()
  await waitFor(() => expect(saves()).toHaveLength(1))

  // then the same button is found by its busy label, disabled and marked busy
  const busy = button(en.profile.saving)
  expect(busy).toBe(idle)
  expect(busy.disabled).toBe(true)
  expect(busy.getAttribute('aria-busy')).toBe('true')
  expect(screen.queryByRole('button', { name: en.profile.save })).toBeNull()

  // and after a failure it is found by its idle label again
  finish(problem(500, 'INTERNAL_ERROR'))
  await waitFor(() => expect(button(en.profile.save).disabled).toBe(false))
  expect(button(en.profile.save).getAttribute('aria-busy')).toBeNull()

  // and after a success too
  pressSave()
  await waitFor(() => expect(saves()).toHaveLength(2))
  expect(button(en.profile.saving).getAttribute('aria-busy')).toBe('true')
  finish(userResponse('TEACHER'))
  await screen.findByText(en.profile.saved)
  expect(button(en.profile.save).disabled).toBe(false)
  expect(button(en.profile.save).getAttribute('aria-busy')).toBeNull()
})

test('[web-client/R29-S3] Change password shows its busy label while the request runs, then its idle label after a failure and after a success', async () => {
  // given a password change that is held
  let finish: (response: Response) => void = () => undefined
  teacherWithPassword(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve
      }),
  )
  await openForm()
  typePasswords('old-password', 'new-password-1')
  const idle = button(en.profile.changePassword)
  expect(idle.getAttribute('aria-busy')).toBeNull()

  // when it is pressed
  pressChange()
  await waitFor(() => expect(changes()).toHaveLength(1))

  // then the same button is found by its busy label, disabled and marked busy
  const busy = button(en.profile.changingPassword)
  expect(busy).toBe(idle)
  expect(busy.disabled).toBe(true)
  expect(busy.getAttribute('aria-busy')).toBe('true')
  expect(
    screen.queryByRole('button', { name: en.profile.changePassword }),
  ).toBeNull()

  // and after a failure it is found by its idle label again
  finish(problem(500, 'INTERNAL_ERROR'))
  await waitFor(() =>
    expect(button(en.profile.changePassword).disabled).toBe(false),
  )
  expect(button(en.profile.changePassword).getAttribute('aria-busy')).toBeNull()

  // and after a success too
  pressChange()
  await waitFor(() => expect(changes()).toHaveLength(2))
  expect(button(en.profile.changingPassword).getAttribute('aria-busy')).toBe(
    'true',
  )
  finish(new Response(null, { status: 204 }))
  await screen.findByText(en.profile.passwordChanged)
  expect(button(en.profile.changePassword).disabled).toBe(false)
  expect(button(en.profile.changePassword).getAttribute('aria-busy')).toBeNull()
})
