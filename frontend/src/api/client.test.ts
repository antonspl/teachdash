import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import en from '../i18n/en.json'
import { ApiError, apiRequest } from './client'

function problem(
  code: unknown,
  status = 401,
  contentType = 'application/problem+json',
) {
  return new Response(JSON.stringify({ status, code }), {
    status,
    headers: { 'Content-Type': contentType },
  })
}

async function failureOf(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise
  } catch (error) {
    expect(error).toBeInstanceOf(ApiError)
    return error as ApiError
  }
  throw new Error('expected the request to fail')
}

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
})

test('[web-client/R4-S1] State-changing request carries the CSRF token', async () => {
  // given
  document.cookie = 'XSRF-TOKEN=abc; path=/'
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  // when
  await apiRequest('auth/login', { method: 'POST', body: { email: 'a@b.c' } })

  // then
  const [url, init] = fetchMock.mock.calls[0]
  expect(url).toBe('/api/auth/login')
  expect(init.method).toBe('POST')
  expect(init.headers['X-XSRF-TOKEN']).toBe('abc')
})

test('[web-client/R5-S1] Known problem code gives its translated message', async () => {
  // given
  fetchMock.mockResolvedValue(problem('WRONG_PASSWORD'))

  // when
  const error = await failureOf(apiRequest('auth/login', { method: 'POST' }))

  // then
  expect(error.message).toBe(en.errors.WRONG_PASSWORD)
  expect(error.message).not.toBe(en.errors.generic)
  expect(error.code).toBe('WRONG_PASSWORD')
  expect(error.status).toBe(401)
  expect(error.isProblem).toBe(true)
})

test('[web-client/R5-S2] Unknown code or non-problem response gives the generic message', async () => {
  const cases: Array<[string, () => Response | Promise<never>, boolean]> = [
    ['unknown code', () => problem('NO_SUCH_CODE', 400), true],
    ['missing code', () => problem(undefined, 400), true],
    ['non-string code', () => problem(42, 400), true],
    ['prototype key as code', () => problem('constructor', 400), true],
    [
      'problem with charset and different case',
      () =>
        problem(
          'WRONG_PASSWORD',
          401,
          'Application/Problem+JSON; charset=UTF-8',
        ),
      true,
    ],
    [
      'plain 502 page',
      () =>
        new Response('<html>Bad Gateway</html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html' },
        }),
      false,
    ],
    [
      'plain JSON with a known code',
      () =>
        new Response(JSON.stringify({ code: 'WRONG_PASSWORD' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        }),
      false,
    ],
    [
      'malformed problem body',
      () =>
        new Response('{not json', {
          status: 500,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
      true,
    ],
    [
      'network failure',
      () => Promise.reject(new TypeError('Failed to fetch')),
      false,
    ],
  ]

  for (const [name, makeResponse, expectedIsProblem] of cases) {
    // given
    fetchMock.mockImplementationOnce(async () => makeResponse())

    // when
    const error = await failureOf(apiRequest('me'))

    // then
    const expectedMessage = name.startsWith('problem with charset')
      ? en.errors.WRONG_PASSWORD
      : en.errors.generic
    expect(error.message, name).toBe(expectedMessage)
    expect(error.isProblem, name).toBe(expectedIsProblem)
  }
})
