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
  const cases: Array<
    [string, () => Response | Promise<never>, boolean, string]
  > = [
    [
      'unknown code',
      () => problem('NO_SUCH_CODE', 400),
      true,
      en.errors.generic,
    ],
    ['missing code', () => problem(undefined, 400), true, en.errors.generic],
    ['non-string code', () => problem(42, 400), true, en.errors.generic],
    [
      'prototype key as code',
      () => problem('constructor', 400),
      true,
      en.errors.generic,
    ],
    [
      'problem with charset and different case',
      () =>
        problem(
          'WRONG_PASSWORD',
          401,
          'Application/Problem+JSON; charset=UTF-8',
        ),
      true,
      en.errors.WRONG_PASSWORD,
    ],
    [
      'plain 502 page',
      () =>
        new Response('<html>Bad Gateway</html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html' },
        }),
      false,
      en.errors.generic,
    ],
    [
      'plain JSON with a known code',
      () =>
        new Response(JSON.stringify({ code: 'WRONG_PASSWORD' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        }),
      false,
      en.errors.generic,
    ],
    [
      'malformed problem body',
      () =>
        new Response('{not json', {
          status: 500,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
      true,
      en.errors.generic,
    ],
    [
      'network failure',
      () => Promise.reject(new TypeError('Failed to fetch')),
      false,
      en.errors.generic,
    ],
  ]

  for (const [
    name,
    makeResponse,
    expectedIsProblem,
    expectedMessage,
  ] of cases) {
    // given
    fetchMock.mockImplementationOnce(async () => makeResponse())

    // when
    const error = await failureOf(apiRequest('me'))

    // then
    expect(error.message, name).toBe(expectedMessage)
    expect(error.isProblem, name).toBe(expectedIsProblem)
  }
})

test('CSRF cookie with a similar name is ignored', async () => {
  document.cookie = 'NOT_XSRF-TOKEN=x; path=/'
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  await apiRequest('auth/login', { method: 'POST' })

  expect(fetchMock.mock.calls[0][1].headers).not.toHaveProperty('X-XSRF-TOKEN')
  document.cookie =
    'NOT_XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
})

test('CSRF token containing = is sent whole', async () => {
  document.cookie = 'XSRF-TOKEN=a=b==; path=/'
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  await apiRequest('auth/login', { method: 'POST' })

  expect(fetchMock.mock.calls[0][1].headers['X-XSRF-TOKEN']).toBe('a=b==')
})

test('CSRF token with a stray percent sign is sent as is', async () => {
  document.cookie = 'XSRF-TOKEN=50%; path=/'
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  await apiRequest('auth/login', { method: 'POST' })

  expect(fetchMock.mock.calls[0][1].headers['X-XSRF-TOKEN']).toBe('50%')
})

test('GET request has no CSRF header', async () => {
  document.cookie = 'XSRF-TOKEN=abc; path=/'
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  await apiRequest('me')

  expect(fetchMock.mock.calls[0][1].headers).not.toHaveProperty('X-XSRF-TOKEN')
})

test('State-changing request without the cookie has no CSRF header', async () => {
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  await apiRequest('auth/login', { method: 'POST' })

  expect(fetchMock.mock.calls[0][1].headers).not.toHaveProperty('X-XSRF-TOKEN')
})

test('Request asks for problem and JSON responses', async () => {
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  await apiRequest('me')

  const [, init] = fetchMock.mock.calls[0]
  expect(init.headers.Accept).toBe('application/problem+json, application/json')
})

test('A deliberate cancel is rethrown as the original AbortError', async () => {
  const controller = new AbortController()
  fetchMock.mockImplementation(
    (_url: string, init: RequestInit) =>
      new Promise((_resolve, reject) => {
        init.signal?.addEventListener('abort', () =>
          reject(init.signal?.reason),
        )
      }),
  )

  const request = apiRequest('me', { signal: controller.signal })
  controller.abort()

  await expect(request).rejects.toMatchObject({ name: 'AbortError' })
  await expect(request).rejects.not.toBeInstanceOf(ApiError)
})

test('A timeout becomes an ApiError that keeps the original error as cause', async () => {
  const timeout = new DOMException('signal timed out', 'TimeoutError')
  fetchMock.mockRejectedValue(timeout)

  const error = await failureOf(apiRequest('me'))

  expect(error.message).toBe(en.errors.generic)
  expect(error.status).toBeUndefined()
  expect(error.cause).toBe(timeout)
})

test('A network failure keeps the original error as cause', async () => {
  const failure = new TypeError('Failed to fetch')
  fetchMock.mockRejectedValue(failure)

  const error = await failureOf(apiRequest('me'))

  expect(error.cause).toBe(failure)
})
