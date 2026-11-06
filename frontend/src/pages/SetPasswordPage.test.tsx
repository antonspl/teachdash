import { StrictMode } from 'react'
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
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

function teacher() {
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

// Answers /api/me as anonymous and the password-link request with `linkAnswer`.
function backend(linkAnswer: () => Response) {
  fetchMock.mockImplementation((url: string) =>
    Promise.resolve(
      url.endsWith('/password-links')
        ? linkAnswer()
        : problem(401, 'UNAUTHENTICATED'),
    ),
  )
}

function linkCalls() {
  return fetchMock.mock.calls.filter(([url]) =>
    String(url).endsWith('/password-links'),
  )
}

function open(hash: string, strict = false) {
  window.history.replaceState(null, '', `/set-password${hash}`)
  const app = (
    <MemoryRouter initialEntries={['/set-password']}>
      <App />
    </MemoryRouter>
  )
  return render(strict ? <StrictMode>{app}</StrictMode> : app)
}

async function submit(first: string, second = first) {
  fireEvent.change(await screen.findByLabelText(en.setPassword.password), {
    target: { value: first },
  })
  fireEvent.change(screen.getByLabelText(en.setPassword.confirm), {
    target: { value: second },
  })
  fireEvent.click(screen.getByRole('button', { name: en.setPassword.submit }))
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  window.history.replaceState(null, '', '/')
})

test('[web-auth/R5-S2] Token is removed from the address bar', async () => {
  // given
  backend(teacher)

  // when
  open('#token=abc', true)

  // then
  expect(window.location.pathname).toBe('/set-password')
  expect(window.location.hash).toBe('')
  await submit('long-enough-1')
  await waitFor(() => expect(linkCalls()).toHaveLength(1))
  const body = JSON.parse(String(linkCalls()[0][1].body)) as { token: string }
  expect(body.token).toBe('abc')
})

test('[web-auth/R5-S3] Passwords that differ are not sent', async () => {
  // given
  backend(teacher)
  open('#token=abc')

  // when
  await submit('long-enough-1', 'long-enough-2')

  // then
  expect(await screen.findByText(en.setPassword.mismatch)).toBeTruthy()
  expect(linkCalls()).toHaveLength(0)
})

test('[web-auth/R5-S4] A short password is not sent', async () => {
  // given
  backend(teacher)
  open('#token=abc')

  // when
  await submit('1234567')

  // then
  expect(await screen.findByText(en.setPassword.tooShort)).toBeTruthy()
  expect(linkCalls()).toHaveLength(0)
})

test('[web-auth/R5-S5] A used, expired or replaced link shows a clear message', async () => {
  // given
  backend(() => problem(410, 'LINK_INVALID'))
  open('#token=abc')

  // when
  await submit('long-enough-1')

  // then
  expect(await screen.findByText(en.errors.LINK_INVALID)).toBeTruthy()
})

test('[web-auth/R5-S6] A link without a token', async () => {
  // given
  backend(teacher)

  // when
  open('')

  // then
  expect(await screen.findByText(en.setPassword.incompleteLink)).toBeTruthy()
  expect(screen.queryByLabelText(en.setPassword.password)).toBeNull()
  expect(
    screen.queryByRole('button', { name: en.setPassword.submit }),
  ).toBeNull()
})

test('[web-auth/R5-S7] Link for a deactivated account', async () => {
  // given
  backend(() => problem(401, 'ACCOUNT_DEACTIVATED'))
  open('#token=abc')

  // when
  await submit('long-enough-1')

  // then
  expect(await screen.findByText(en.errors.ACCOUNT_DEACTIVATED)).toBeTruthy()
})

test('[web-auth/R5-S8] A password over 72 bytes is not sent', async () => {
  // given
  backend(teacher)
  open('#token=abc')

  // when
  await submit('я'.repeat(40))

  // then
  expect(await screen.findByText(en.setPassword.tooLong)).toBeTruthy()
  expect(linkCalls()).toHaveLength(0)
})

test('[web-auth/R5-S9] A link used at the same moment elsewhere', async () => {
  // given
  backend(() => problem(409, 'LINK_CONFLICT'))
  open('#token=abc')

  // when
  await submit('long-enough-1')

  // then
  expect(await screen.findByText(en.errors.LINK_CONFLICT)).toBeTruthy()
  const button = screen.getByRole('button', { name: en.setPassword.submit })
  await waitFor(() => expect(button).toHaveProperty('disabled', false))
  fireEvent.click(button)
  await waitFor(() => expect(linkCalls()).toHaveLength(2))
})
