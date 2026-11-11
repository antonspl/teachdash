import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
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
  vi.restoreAllMocks()
})

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function problem(status: number, code: string) {
  return new Response(JSON.stringify({ status, code }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

const admin = {
  id: '1',
  name: 'Ada Admin',
  email: 'admin@example.com',
  role: 'ADMIN',
  timeZone: 'UTC',
}

function teacher(over: Record<string, unknown> = {}) {
  return {
    id: '10',
    name: 'Anna Smith',
    email: 'anna@example.com',
    role: 'TEACHER',
    timeZone: 'Europe/Berlin',
    active: true,
    hasPassword: true,
    ...over,
  }
}

type Handler = () => Response | Promise<Response>

// Answers /api/me as the admin, the teachers list from `list` and create from `create`.
function backend(handlers: { list: Handler; create?: Handler }) {
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    const method = init?.method ?? 'GET'
    if (url.endsWith('/api/me')) {
      return Promise.resolve(json(admin))
    }
    if (url.endsWith('/api/admin/teachers')) {
      const handler = method === 'POST' ? handlers.create : handlers.list
      return Promise.resolve(handler?.() ?? problem(500, 'INTERNAL_ERROR'))
    }
    return Promise.resolve(problem(404, 'NOT_FOUND'))
  })
}

function calls(method: string) {
  return fetchMock.mock.calls.filter((call: unknown[]) => {
    const init = call[1] as RequestInit | undefined
    return (
      String(call[0]).endsWith('/api/admin/teachers') &&
      (init?.method ?? 'GET') === method
    )
  })
}

function open() {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <App />
    </MemoryRouter>,
  )
}

function field(label: string) {
  return screen.findByLabelText<HTMLInputElement>(label)
}

function addButton() {
  return screen.findByRole<HTMLButtonElement>('button', {
    name: en.adminTeachers.add,
  })
}

async function openDialog() {
  fireEvent.click(await addButton())
  return screen.findByRole('dialog')
}

async function fillForm(name: string, email: string) {
  await openDialog()
  fireEvent.change(await field(en.adminTeachers.create.name), {
    target: { value: name },
  })
  fireEvent.change(screen.getByLabelText(en.adminTeachers.create.email), {
    target: { value: email },
  })
}

function submitButton() {
  return screen.getByRole<HTMLButtonElement>('button', {
    name: en.adminTeachers.create.submit,
  })
}

test('[web-admin-teachers/R1-S1] Each teacher is shown with their status', async () => {
  // given
  backend({
    list: () =>
      json([
        teacher({ id: '1', name: 'Anna Smith' }),
        teacher({
          id: '2',
          name: 'Boris Brown',
          email: 'boris@example.com',
          timeZone: 'UTC',
          hasPassword: false,
        }),
        teacher({
          id: '3',
          name: 'Clara Cole',
          email: 'clara@example.com',
          timeZone: 'Asia/Tokyo',
          active: false,
        }),
      ]),
  })

  // when
  open()

  // then
  const rows = await screen.findAllByRole('listitem')
  expect(rows).toHaveLength(3)
  const expected = [
    ['Anna Smith', 'anna@example.com', 'Europe/Berlin', 'Active'],
    ['Boris Brown', 'boris@example.com', 'UTC', 'Waiting for password'],
    ['Clara Cole', 'clara@example.com', 'Asia/Tokyo', 'Deactivated'],
  ]
  expected.forEach((parts, index) => {
    parts.forEach((part) => {
      expect(within(rows[index]).getByText(part)).toBeTruthy()
    })
  })
})

test('[web-admin-teachers/R1-S2] No teachers yet', async () => {
  // given
  backend({ list: () => json([]) })

  // when
  open()

  // then
  expect(await screen.findByText(en.adminTeachers.empty)).toBeTruthy()
  expect(await addButton()).toBeTruthy()
  expect(screen.queryByRole('listitem')).toBeNull()
})

test('[web-admin-teachers/R1-S3] List fails to load', async () => {
  // given
  let attempt = 0
  backend({
    list: () =>
      attempt++ === 0 ? problem(500, 'INTERNAL_ERROR') : json([teacher()]),
  })

  // when
  open()

  // then
  expect(await screen.findByText(en.errors.INTERNAL_ERROR)).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: en.ui.tryAgain }))
  expect(await screen.findByText('Anna Smith')).toBeTruthy()
  expect(screen.queryByText(en.errors.INTERNAL_ERROR)).toBeNull()
})

test('[web-admin-teachers/R2-S2] The time zone of the browser is preselected', async () => {
  // given
  const real = Intl.DateTimeFormat
  vi.spyOn(Intl, 'DateTimeFormat').mockImplementation(((
    ...args: ConstructorParameters<typeof Intl.DateTimeFormat>
  ) => {
    const format = new real(...args)
    return {
      resolvedOptions: () => ({
        ...format.resolvedOptions(),
        timeZone: 'Europe/Moscow',
      }),
    }
  }) as unknown as typeof Intl.DateTimeFormat)
  backend({ list: () => json([]) })

  // when
  open()

  // then
  await openDialog()
  const select = await screen.findByLabelText<HTMLSelectElement>(
    en.adminTeachers.create.timeZone,
  )
  expect(select.value).toBe('Europe/Moscow')
  const options = within(select)
    .getAllByRole('option')
    .map((option) => option.textContent)
  expect(options).toContain('UTC')
  expect(options).toContain('Europe/Moscow')
})

test('[web-admin-teachers/R2-S3] Email already in use', async () => {
  // given
  backend({
    list: () => json([]),
    create: () => problem(409, 'EMAIL_TAKEN'),
  })
  open()
  await fillForm('Anna Smith', 'anna@example.com')
  fireEvent.change(screen.getByLabelText(en.adminTeachers.create.timeZone), {
    target: { value: 'UTC' },
  })

  // when
  fireEvent.click(submitButton())

  // then
  expect(await screen.findByText(en.errors.EMAIL_TAKEN)).toBeTruthy()
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.name).value,
  ).toBe('Anna Smith')
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.email)
      .value,
  ).toBe('anna@example.com')
  expect(
    screen.getByLabelText<HTMLSelectElement>(en.adminTeachers.create.timeZone)
      .value,
  ).toBe('UTC')
})

test('[web-admin-teachers/R2-S4] Backend rejects the fields', async () => {
  // given
  backend({
    list: () => json([]),
    create: () => problem(400, 'VALIDATION_FAILED'),
  })
  open()
  await fillForm('Anna Smith', 'anna@example.com')

  // when
  fireEvent.click(submitButton())

  // then
  expect(
    await screen.findByText(en.adminTeachers.create.validationFailed),
  ).toBeTruthy()
  expect(en.adminTeachers.create.validationFailed).toMatch(/time zone/i)
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.name).value,
  ).toBe('Anna Smith')
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.email)
      .value,
  ).toBe('anna@example.com')
})

test('[web-admin-teachers/R2-S5] The form is not sent twice', async () => {
  // given
  backend({
    list: () => json([]),
    create: () => new Promise<Response>(() => {}),
  })
  open()
  await fillForm('Anna Smith', 'anna@example.com')

  // when
  const form = submitButton().closest('form')!
  fireEvent.submit(form)
  fireEvent.submit(form)
  fireEvent.click(submitButton())

  // then
  await waitFor(() => expect(submitButton().disabled).toBe(true))
  expect(calls('POST')).toHaveLength(1)
})

test('[web-admin-teachers/R2-S6] Network failure on create', async () => {
  // given
  backend({
    list: () => json([]),
    create: () => {
      throw new TypeError('Failed to fetch')
    },
  })
  open()
  await fillForm('Anna Smith', 'anna@example.com')

  // when
  fireEvent.click(submitButton())

  // then
  expect(await screen.findByText(en.errors.generic)).toBeTruthy()
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.name).value,
  ).toBe('Anna Smith')
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.email)
      .value,
  ).toBe('anna@example.com')
  expect(submitButton().disabled).toBe(false)
})

test('[web-admin-teachers/R3-S5] Link stays when the list fails to reload', async () => {
  // given
  let listCalls = 0
  backend({
    list: () => (listCalls++ === 0 ? json([]) : problem(500, 'INTERNAL_ERROR')),
    create: () =>
      json(
        { id: '11', setPasswordLink: 'https://x.test/set-password#t=1' },
        201,
      ),
  })
  open()
  await fillForm('Anna Smith', 'anna@example.com')

  // when
  fireEvent.click(submitButton())

  // then
  const linkField = await screen.findByLabelText<HTMLInputElement>(
    en.issuedLink.label.replace('{{name}}', 'Anna Smith'),
  )
  expect(linkField.value).toBe('https://x.test/set-password#t=1')
  expect(linkField.closest('li')).toBeNull()
  expect(await screen.findByText(en.errors.INTERNAL_ERROR)).toBeTruthy()
  expect(screen.getByRole('button', { name: en.ui.tryAgain })).toBeTruthy()
  // the dialog closed and the success path reloaded the list
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(calls('GET')).toHaveLength(2)
})

test('[web-admin-teachers/R2-S7] Cancelling the dialog', async () => {
  // given
  backend({ list: () => json([]) })
  open()
  const opener = await addButton()
  for (const cancel of [
    (dialog: HTMLElement) =>
      fireEvent.click(
        within(dialog).getByRole('button', {
          name: en.adminTeachers.create.cancel,
        }),
      ),
    (dialog: HTMLElement) =>
      fireEvent(dialog, new Event('cancel', { cancelable: true })),
  ]) {
    const dialog = await openDialog()
    fireEvent.change(screen.getByLabelText(en.adminTeachers.create.name), {
      target: { value: 'Typed Name' },
    })

    // when
    cancel(dialog)

    // then
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(calls('POST')).toHaveLength(0)
    expect(document.activeElement).toBe(opener)
    await openDialog()
    expect(
      screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.name)
        .value,
    ).toBe('')
    fireEvent(
      screen.getByRole('dialog'),
      new Event('cancel', { cancelable: true }),
    )
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  }
})

test('[web-admin-teachers/R2-S7] Cancel and Escape are ignored while creating', async () => {
  // given
  backend({
    list: () => json([]),
    create: () => new Promise<Response>(() => {}),
  })
  open()
  await fillForm('Anna Smith', 'anna@example.com')
  fireEvent.click(submitButton())
  await waitFor(() => expect(submitButton().disabled).toBe(true))

  // when
  fireEvent(
    screen.getByRole('dialog'),
    new Event('cancel', { cancelable: true }),
  )

  // then
  expect(screen.getByRole('dialog')).toBeTruthy()
  expect(
    screen.getByRole<HTMLButtonElement>('button', {
      name: en.adminTeachers.create.cancel,
    }).disabled,
  ).toBe(true)
})

test('[web-admin-teachers/R2-S8] The new teacher stays first until a reload', async () => {
  // given the backend sorts by name, so the new teacher comes last
  const anna = teacher({ id: '1', name: 'Anna Smith' })
  const zoe = teacher({
    id: '2',
    name: 'Zoe Young',
    email: 'zoe@example.com',
  })
  let created = false
  backend({
    list: () => json(created ? [anna, zoe] : [anna]),
    create: () => {
      created = true
      return json({ id: '2', setPasswordLink: 'https://x.test/s#t=1' }, 201)
    },
  })
  const first = open()
  await fillForm('Zoe Young', 'zoe@example.com')

  // when
  fireEvent.click(submitButton())

  // then
  await screen.findByDisplayValue('https://x.test/s#t=1')
  await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(2))
  expect(screen.queryByRole('dialog')).toBeNull()
  let rows = screen.getAllByRole('listitem')
  expect(within(rows[0]).getByText('Zoe Young')).toBeTruthy()
  expect(within(rows[0]).getByText(en.adminTeachers.justAdded)).toBeTruthy()
  expect(within(rows[1]).getByText('Anna Smith')).toBeTruthy()

  // and after a reload the backend's order returns without the label
  first.unmount()
  open()
  await screen.findAllByRole('listitem')
  rows = screen.getAllByRole('listitem')
  expect(within(rows[0]).getByText('Anna Smith')).toBeTruthy()
  expect(within(rows[1]).getByText('Zoe Young')).toBeTruthy()
  expect(screen.queryByText(en.adminTeachers.justAdded)).toBeNull()
})

// A backend whose teacher list is `state.teachers` and whose action requests are answered
// by `actions`, keyed like "POST admin/teachers/10/deactivate".
type Actions = Record<string, () => Response | Promise<Response>>

function world(initial: unknown[], actions: Actions) {
  const state = { teachers: initial }
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    const method = init?.method ?? 'GET'
    const path = url.replace('/api/', '')
    if (path === 'me') {
      return Promise.resolve(json(admin))
    }
    if (path === 'admin/teachers' && method === 'GET') {
      return Promise.resolve(json(state.teachers))
    }
    const handler = actions[`${method} ${path}`]
    return Promise.resolve(handler ? handler() : problem(404, 'NOT_FOUND'))
  })
  return state
}

function actionCalls(key: string) {
  return fetchMock.mock.calls.filter(
    (call: unknown[]) =>
      `${(call[1] as RequestInit | undefined)?.method ?? 'GET'} ${String(call[0]).replace('/api/', '')}` ===
      key,
  )
}

function listCalls() {
  return calls('GET').length
}

async function rowOf(name: string) {
  const row = (await screen.findByText(name)).closest('li')
  return within(row as HTMLElement)
}

const DEACTIVATE = 'POST admin/teachers/10/deactivate'
const REACTIVATE = 'POST admin/teachers/10/reactivate'
const RESET = 'POST admin/users/10/reset-link'

function deactivateButton(row: ReturnType<typeof within>) {
  return row.getByRole('button', { name: en.adminTeachers.actions.deactivate })
}

test('[web-admin-teachers/R4-S1] Deactivate asks first', async () => {
  // given
  world([teacher()], {})
  open()
  const row = await rowOf('Anna Smith')

  // when
  fireEvent.click(deactivateButton(row))

  // then
  const dialog = await screen.findByRole('dialog')
  expect(
    within(dialog).getByText(/Anna Smith/, { selector: 'h2' }),
  ).toBeTruthy()
  expect(within(dialog).getByText(/students will be released/)).toBeTruthy()
  expect(actionCalls(DEACTIVATE)).toHaveLength(0)
})

test('[web-admin-teachers/R4-S2] Confirming deactivates the teacher once', async () => {
  // given
  const state = world([teacher()], {
    [DEACTIVATE]: () => {
      state.teachers = [teacher({ active: false })]
      return new Response(null, { status: 204 })
    },
  })
  open()
  fireEvent.click(deactivateButton(await rowOf('Anna Smith')))
  const confirm = within(await screen.findByRole('dialog')).getByRole(
    'button',
    {
      name: en.adminTeachers.deactivate.confirm,
    },
  )

  // when
  fireEvent.click(confirm)
  fireEvent.click(confirm)

  // then
  await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  expect(actionCalls(DEACTIVATE)).toHaveLength(1)
  expect(await screen.findByText('Deactivated')).toBeTruthy()
})

test.each(['button', 'escape'])(
  '[web-admin-teachers/R4-S3] Cancelling changes nothing (%s)',
  async (how) => {
    // given
    world([teacher()], {})
    open()
    fireEvent.click(deactivateButton(await rowOf('Anna Smith')))
    const dialog = await screen.findByRole('dialog')

    // when
    if (how === 'button') {
      fireEvent.click(
        within(dialog).getByRole('button', {
          name: en.adminTeachers.deactivate.cancel,
        }),
      )
    } else {
      fireEvent(dialog, new Event('cancel', { cancelable: true }))
    }

    // then
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(actionCalls(DEACTIVATE)).toHaveLength(0)
    expect(screen.getByText('Active')).toBeTruthy()
  },
)

test('[web-admin-teachers/R4-S4] Reactivating a teacher who has a password', async () => {
  // given
  const state = world([teacher({ active: false })], {
    [REACTIVATE]: () => {
      state.teachers = [teacher()]
      return new Response(null, { status: 204 })
    },
  })
  open()
  const row = await rowOf('Anna Smith')

  // when
  fireEvent.click(
    row.getByRole('button', { name: en.adminTeachers.actions.reactivate }),
  )

  // then
  expect(await screen.findByText('Active')).toBeTruthy()
  expect(
    screen.queryByLabelText(
      en.issuedLink.label.replace('{{name}}', 'Anna Smith'),
    ),
  ).toBeNull()
})

test('[web-admin-teachers/R4-S5] Reactivating a teacher who never set a password', async () => {
  // given
  const state = world([teacher({ active: false, hasPassword: false })], {
    [REACTIVATE]: () => {
      state.teachers = [teacher({ hasPassword: false })]
      return json({ setPasswordLink: 'https://x.test/set#token=r' })
    },
  })
  open()
  const row = await rowOf('Anna Smith')

  // when
  fireEvent.click(
    row.getByRole('button', { name: en.adminTeachers.actions.reactivate }),
  )

  // then
  const field = await screen.findByLabelText<HTMLInputElement>(
    en.issuedLink.label.replace('{{name}}', 'Anna Smith'),
  )
  expect(field.value).toBe('https://x.test/set#token=r')
  expect(await screen.findByText('Waiting for password')).toBeTruthy()
})

test('[web-admin-teachers/R4-S6] A failed deactivation is reported', async () => {
  // given
  world([teacher()], { [DEACTIVATE]: () => problem(404, 'NOT_FOUND') })
  open()
  fireEvent.click(deactivateButton(await rowOf('Anna Smith')))
  const before = listCalls()

  // when
  fireEvent.click(
    within(await screen.findByRole('dialog')).getByRole('button', {
      name: en.adminTeachers.deactivate.confirm,
    }),
  )

  // then
  expect(await screen.findByText(en.errors.NOT_FOUND)).toBeTruthy()
  expect(screen.queryByRole('dialog')).toBeNull()
  await waitFor(() => expect(listCalls()).toBe(before + 1))
})

test('[web-admin-teachers/R4-S7] A failed reactivation is reported', async () => {
  // given
  world([teacher({ active: false })], {
    [REACTIVATE]: () => problem(500, 'INTERNAL_ERROR'),
  })
  open()
  const row = await rowOf('Anna Smith')
  const before = listCalls()

  // when
  fireEvent.click(
    row.getByRole('button', { name: en.adminTeachers.actions.reactivate }),
  )

  // then
  expect(await screen.findByText(en.errors.INTERNAL_ERROR)).toBeTruthy()
  await waitFor(() => expect(listCalls()).toBe(before + 1))
  const button = row.getByRole<HTMLButtonElement>('button', {
    name: en.adminTeachers.actions.reactivate,
  })
  expect(button.disabled).toBe(false)
})

test('[web-admin-teachers/R4-S8] Focus moves into the dialog and back', async () => {
  // given
  world([teacher()], {})
  open()
  const opener = deactivateButton(await rowOf('Anna Smith'))
  // Safari and macOS Firefox do not focus a button on click: focus is on <body>.
  opener.blur()
  expect(document.activeElement).toBe(document.body)

  // when
  fireEvent.click(opener)
  const dialog = await screen.findByRole('dialog')

  // then
  expect(dialog.contains(document.activeElement)).toBe(true)
  fireEvent.click(
    within(dialog).getByRole('button', {
      name: en.adminTeachers.deactivate.cancel,
    }),
  )
  await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  expect(document.activeElement).toBe(opener)
})

test('[web-admin-teachers/R1-S5] Session ends during an action', async () => {
  // given
  let ended = false
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    const path = url.replace('/api/', '')
    const method = init?.method ?? 'GET'
    if (path === 'auth/login') {
      ended = false
      return Promise.resolve(json(admin))
    }
    if (ended) {
      return Promise.resolve(problem(401, 'UNAUTHENTICATED'))
    }
    if (path === 'me') {
      return Promise.resolve(json(admin))
    }
    if (path === 'admin/teachers' && method === 'GET') {
      return Promise.resolve(json([teacher()]))
    }
    if (path === 'admin/teachers' && method === 'POST') {
      return Promise.resolve(
        json({ id: '11', setPasswordLink: 'https://x.test/old#token=1' }, 201),
      )
    }
    ended = true
    return Promise.resolve(problem(401, 'UNAUTHENTICATED'))
  })
  window.history.pushState({}, '', '/admin')
  render(
    <BrowserRouter>
      <App />
    </BrowserRouter>,
  )
  await fillForm('Zed Zane', 'zed@example.com')
  fireEvent.click(submitButton())
  await screen.findByDisplayValue('https://x.test/old#token=1')
  fireEvent.click(deactivateButton(await rowOf('Anna Smith')))

  // when
  fireEvent.click(
    within(await screen.findByRole('dialog')).getByRole('button', {
      name: en.adminTeachers.deactivate.confirm,
    }),
  )

  // then
  expect(await screen.findByText(en.login.sessionEnded)).toBeTruthy()
  fireEvent.change(screen.getByLabelText(en.login.email), {
    target: { value: 'admin@example.com' },
  })
  fireEvent.change(screen.getByLabelText(en.login.password), {
    target: { value: 'secret-pass' },
  })
  fireEvent.click(screen.getByRole('button', { name: en.login.submit }))
  expect(await screen.findByText('Anna Smith')).toBeTruthy()
  expect(window.location.pathname).toBe('/admin')
  expect(screen.queryByDisplayValue('https://x.test/old#token=1')).toBeNull()
})

test('[web-admin-teachers/R5-S1] New invite link for a teacher without a password', async () => {
  // given
  world([teacher({ hasPassword: false })], {
    [RESET]: () => json({ link: 'https://x.test/set#token=n' }, 201),
  })
  open()
  const row = await rowOf('Anna Smith')

  // when
  fireEvent.click(
    row.getByRole('button', { name: en.adminTeachers.actions.inviteLink }),
  )

  // then
  const field = await screen.findByLabelText<HTMLInputElement>(
    en.issuedLink.label.replace('{{name}}', 'Anna Smith'),
  )
  expect(field.value).toBe('https://x.test/set#token=n')
  expect(actionCalls(RESET)).toHaveLength(1)
  expect(
    row.getByLabelText(en.issuedLink.label.replace('{{name}}', 'Anna Smith')),
  ).toBe(field)
})

test("[web-admin-teachers/R3-S6] A new link moves to its teacher's row", async () => {
  // given a link for Anna is shown in her row
  world(
    [
      teacher({ id: '10', name: 'Anna Smith' }),
      teacher({ id: '11', name: 'Bob Brown', email: 'bob@example.com' }),
    ],
    {
      'POST admin/users/10/reset-link': () =>
        json({ link: 'https://x.test/set#token=anna' }, 201),
      'POST admin/users/11/reset-link': () =>
        json({ link: 'https://x.test/set#token=bob' }, 201),
    },
  )
  open()
  const anna = await rowOf('Anna Smith')
  const bob = await rowOf('Bob Brown')
  fireEvent.click(
    anna.getByRole('button', { name: en.adminTeachers.actions.resetLink }),
  )
  await screen.findByDisplayValue('https://x.test/set#token=anna')
  const annaLabel = en.issuedLink.label.replace('{{name}}', 'Anna Smith')
  const bobLabel = en.issuedLink.label.replace('{{name}}', 'Bob Brown')
  expect(anna.getByLabelText(annaLabel)).toBeTruthy()

  // when the admin gets a link for Bob
  fireEvent.click(
    bob.getByRole('button', { name: en.adminTeachers.actions.resetLink }),
  )

  // then it is shown in Bob's row and Anna's row has no box
  await screen.findByDisplayValue('https://x.test/set#token=bob')
  expect(bob.getByLabelText(bobLabel)).toBeTruthy()
  expect(anna.queryByLabelText(annaLabel)).toBeNull()
  expect(screen.queryByDisplayValue('https://x.test/set#token=anna')).toBeNull()
  expect(screen.getAllByRole('textbox')).toHaveLength(1)
})

test('[web-admin-teachers/R3-S2] A new link clears the Copied message', async () => {
  // given
  let issued = 0
  world([teacher()], {
    [RESET]: () => json({ link: `https://x.test/set#token=${++issued}` }, 201),
  })
  const writeText = vi.fn().mockResolvedValue(undefined)
  vi.stubGlobal('navigator', { clipboard: { writeText } })
  open()
  const row = await rowOf('Anna Smith')
  const resetLink = () =>
    row.getByRole<HTMLButtonElement>('button', {
      name: en.adminTeachers.actions.resetLink,
    })
  fireEvent.click(resetLink())
  await screen.findByDisplayValue('https://x.test/set#token=1')
  fireEvent.click(screen.getByRole('button', { name: en.issuedLink.copy }))
  await screen.findByText(en.issuedLink.copied)
  await waitFor(() => expect(resetLink().disabled).toBe(false))

  // when
  fireEvent.click(resetLink())

  // then
  await screen.findByDisplayValue('https://x.test/set#token=2')
  expect(screen.queryByText(en.issuedLink.copied)).toBeNull()
  expect(screen.queryByText(en.issuedLink.copyByHand)).toBeNull()
})

test('[web-admin-teachers/R5-S2] Password reset link for a teacher with a password', async () => {
  // given
  world([teacher()], {})

  // when
  open()
  const row = await rowOf('Anna Smith')

  // then
  expect(
    row.getByRole('button', { name: en.adminTeachers.actions.resetLink }),
  ).toBeTruthy()
  expect(
    row.queryByRole('button', { name: en.adminTeachers.actions.inviteLink }),
  ).toBeNull()
})

test('[web-admin-teachers/R5-S3] No link for a deactivated teacher', async () => {
  // given
  world([teacher({ active: false })], {})

  // when
  open()
  const row = await rowOf('Anna Smith')

  // then
  expect(
    row.getByRole('button', { name: en.adminTeachers.actions.reactivate }),
  ).toBeTruthy()
  expect(
    row.queryByRole('button', { name: en.adminTeachers.actions.resetLink }),
  ).toBeNull()
  expect(
    row.queryByRole('button', { name: en.adminTeachers.actions.inviteLink }),
  ).toBeNull()
})

test("[web-admin-teachers/R5-S4] A row's buttons wait for its request", async () => {
  // given
  world([teacher()], { [RESET]: () => new Promise<Response>(() => {}) })
  open()
  const row = await rowOf('Anna Smith')
  const link = row.getByRole<HTMLButtonElement>('button', {
    name: en.adminTeachers.actions.resetLink,
  })
  fireEvent.click(link)
  await waitFor(() => expect(link.disabled).toBe(true))

  // when
  fireEvent.click(link)
  fireEvent.click(deactivateButton(row))

  // then
  expect(actionCalls(RESET)).toHaveLength(1)
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(deactivateButton(row).disabled).toBe(true)
})

test('[web-admin-teachers/R5-S5] A failed link request is reported', async () => {
  // given
  world([teacher()], { [RESET]: () => problem(403, 'CSRF_INVALID') })
  open()
  const row = await rowOf('Anna Smith')

  // when
  fireEvent.click(
    row.getByRole('button', { name: en.adminTeachers.actions.resetLink }),
  )

  // then
  expect(await screen.findByText(en.errors.CSRF_INVALID)).toBeTruthy()
  expect(screen.queryByLabelText(/^Link for/)).toBeNull()
  const button = row.getByRole<HTMLButtonElement>('button', {
    name: en.adminTeachers.actions.resetLink,
  })
  await waitFor(() => expect(button.disabled).toBe(false))
})

// Regression tests for review findings, not spec scenarios. jsdom has no real modal dialog:
// the "close" a browser sends after an Escape it could not cancel is simulated by hand.
function closeNatively(dialog: HTMLElement) {
  dialog.removeAttribute('open')
  fireEvent(dialog, new Event('close'))
}

test('A native close while creating keeps the dialog open and a failure shows in it', async () => {
  // given a create request that is still running
  let fail: () => void = () => {}
  backend({
    list: () => json([]),
    create: () =>
      new Promise<Response>((resolve) => {
        fail = () => resolve(problem(400, 'VALIDATION_FAILED'))
      }) as unknown as Response,
  })
  open()
  await fillForm('Anna Smith', 'anna@example.com')
  fireEvent.click(submitButton())
  await waitFor(() => expect(submitButton().disabled).toBe(true))

  // when the browser closes the dialog itself
  const dialog = screen.getByRole('dialog')
  closeNatively(dialog)

  // then it is open again, still busy, and the failure shows in it with the values kept
  expect(dialog.hasAttribute('open')).toBe(true)
  expect(submitButton().disabled).toBe(true)
  fail()
  expect(
    await screen.findByText(en.adminTeachers.create.validationFailed),
  ).toBeTruthy()
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.name).value,
  ).toBe('Anna Smith')
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.email)
      .value,
  ).toBe('anna@example.com')
})

test('A native close while not creating closes the dialog and Add opens an empty one', async () => {
  // given
  backend({ list: () => json([]) })
  open()
  await fillForm('Anna Smith', 'anna@example.com')

  // when the browser closes the dialog itself
  closeNatively(screen.getByRole('dialog'))

  // then it is gone and the Add button opens a fresh, empty one
  await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  await openDialog()
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.name).value,
  ).toBe('')
})

test('A native close while deactivating keeps the confirm dialog open', async () => {
  // given a deactivate request that is still running
  world([teacher()], { [DEACTIVATE]: () => new Promise<Response>(() => {}) })
  open()
  fireEvent.click(deactivateButton(await rowOf('Anna Smith')))
  const dialog = await screen.findByRole('dialog')
  const confirm = () =>
    within(dialog).getByRole<HTMLButtonElement>('button', {
      name: en.adminTeachers.deactivate.confirm,
    })
  fireEvent.click(confirm())
  await waitFor(() => expect(confirm().disabled).toBe(true))

  // when the browser closes the dialog itself
  closeNatively(dialog)

  // then it is open again and still busy
  expect(dialog.hasAttribute('open')).toBe(true)
  expect(confirm().disabled).toBe(true)
})

test('A native close while not deactivating closes the confirm dialog', async () => {
  // given
  world([teacher()], {})
  open()
  const opener = deactivateButton(await rowOf('Anna Smith'))
  fireEvent.click(opener)

  // when
  closeNatively(await screen.findByRole('dialog'))

  // then
  await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  expect(document.activeElement).toBe(opener)
})

test('Focus and the Copied message stay when the new teacher link moves into the row', async () => {
  // given a created teacher whose list reload is still pending, and a copied link
  const anna = teacher({ id: '1', name: 'Anna Smith' })
  const zoe = teacher({ id: '2', name: 'Zoe Young', email: 'zoe@example.com' })
  let created = false
  let finishReload: () => void = () => {}
  backend({
    list: () =>
      created
        ? (new Promise<Response>((resolve) => {
            finishReload = () => resolve(json([anna, zoe]))
          }) as unknown as Response)
        : json([anna]),
    create: () => {
      created = true
      return json({ id: '2', setPasswordLink: 'https://x.test/s#t=1' }, 201)
    },
  })
  vi.stubGlobal('navigator', {
    clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
  })
  open()
  await fillForm('Zoe Young', 'zoe@example.com')
  fireEvent.click(submitButton())
  await screen.findByDisplayValue('https://x.test/s#t=1')
  const copy = screen.getByRole<HTMLButtonElement>('button', {
    name: en.issuedLink.copy,
  })
  fireEvent.click(copy)
  await screen.findByText(en.issuedLink.copied)
  copy.focus()

  // when the reload returns and the box moves into Zoe's row
  finishReload()
  await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(2))

  // then the Copied message is still there and focus is on the Copy button
  const row = screen.getAllByRole('listitem')[0]
  expect(within(row).getByDisplayValue('https://x.test/s#t=1')).toBeTruthy()
  expect(screen.getByText(en.issuedLink.copied)).toBeTruthy()
  expect(document.activeElement).toBe(
    within(row).getByRole('button', { name: en.issuedLink.copy }),
  )
})

test('Focus moves to Reactivate after a successful deactivation', async () => {
  // given
  const state = world([teacher()], {
    [DEACTIVATE]: () => {
      state.teachers = [teacher({ active: false })]
      return new Response(null, { status: 204 })
    },
  })
  open()
  fireEvent.click(deactivateButton(await rowOf('Anna Smith')))
  const dialog = await screen.findByRole('dialog')

  // when
  fireEvent.click(
    within(dialog).getByRole('button', {
      name: en.adminTeachers.deactivate.confirm,
    }),
  )

  // then
  await screen.findByText('Deactivated')
  await waitFor(() =>
    expect(document.activeElement).toBe(
      screen.getByRole('button', {
        name: en.adminTeachers.actions.reactivate,
      }),
    ),
  )
})

test('Focus is not stolen by a late reload after a deactivation', async () => {
  // given a deactivation whose list reload is still pending
  let deactivated = false
  let finishReload: () => void = () => {}
  fetchMock.mockImplementation((url: string, init?: RequestInit) => {
    const method = init?.method ?? 'GET'
    const path = url.replace('/api/', '')
    if (path === 'me') {
      return Promise.resolve(json(admin))
    }
    if (path === 'admin/teachers' && method === 'GET') {
      if (!deactivated) {
        return Promise.resolve(json([teacher()]))
      }
      return new Promise<Response>((resolve) => {
        finishReload = () => resolve(json([teacher({ active: false })]))
      })
    }
    if (`${method} ${path}` === DEACTIVATE) {
      deactivated = true
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    return Promise.resolve(problem(404, 'NOT_FOUND'))
  })
  open()
  fireEvent.click(deactivateButton(await rowOf('Anna Smith')))
  fireEvent.click(
    within(await screen.findByRole('dialog')).getByRole('button', {
      name: en.adminTeachers.deactivate.confirm,
    }),
  )
  await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  await waitFor(() => expect(deactivated).toBe(true))
  await waitFor(() => expect(typeof finishReload).toBe('function'))

  // when the admin moves on before the reload lands
  const add = await addButton()
  add.focus()
  finishReload()

  // then the row shows Reactivate but focus stays where the admin put it
  await screen.findByText('Deactivated')
  expect(document.activeElement).toBe(add)
})
