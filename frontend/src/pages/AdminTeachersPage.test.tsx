import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { MemoryRouter } from 'react-router'
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

async function fillForm(name: string, email: string) {
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
  expect(screen.getByLabelText(en.adminTeachers.create.name)).toBeTruthy()
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
  expect(await screen.findByText(en.errors.INTERNAL_ERROR)).toBeTruthy()
  expect(screen.getByRole('button', { name: en.ui.tryAgain })).toBeTruthy()
  // the form was cleared and the success path reloaded the list
  expect(
    screen.getByLabelText<HTMLInputElement>(en.adminTeachers.create.name).value,
  ).toBe('')
  expect(calls('GET')).toHaveLength(2)
})
