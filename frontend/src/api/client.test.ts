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

test.only('[web-client/R4-S1] State-changing request carries the CSRF token', async () => {
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

test('[web-client/R5-S4] Failed login message includes the set-password hint', async () => {
  // given
  fetchMock.mockResolvedValue(problem('INVALID_CREDENTIALS'))

  // when
  const error = await failureOf(apiRequest('auth/login', { method: 'POST' }))

  // then
  expect(error.message).toMatch(/email or password is incorrect/i)
  expect(error.message).toMatch(/link/i)
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

test('A cancel during the body read is rethrown as the original AbortError', async () => {
  const cancel = new DOMException('The operation was aborted', 'AbortError')
  const controller = new AbortController()
  controller.abort(cancel)
  const response = problem('WRONG_PASSWORD')
  vi.spyOn(response, 'text').mockRejectedValue(cancel)
  fetchMock.mockResolvedValue(response)

  await expect(apiRequest('me', { signal: controller.signal })).rejects.toBe(
    cancel,
  )
})

test('A failed body read becomes an ApiError that is not a problem and keeps the cause', async () => {
  const timeout = new DOMException('signal timed out', 'TimeoutError')
  const response = problem('WRONG_PASSWORD')
  vi.spyOn(response, 'text').mockRejectedValue(timeout)
  fetchMock.mockResolvedValue(response)

  const error = await failureOf(apiRequest('me'))

  expect(error.status).toBe(401)
  expect(error.code).toBeUndefined()
  expect(error.isProblem).toBe(false)
  expect(error.cause).toBe(timeout)
})

test('A complete body that is not valid JSON stays a problem with the generic message', async () => {
  fetchMock.mockResolvedValue(
    new Response('not json', {
      status: 401,
      headers: { 'Content-Type': 'application/problem+json' },
    }),
  )

  const error = await failureOf(apiRequest('me'))

  expect(error.isProblem).toBe(true)
  expect(error.code).toBeUndefined()
  expect(error.message).toBe(en.errors.generic)
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

function pendingUntilAborted() {
  fetchMock.mockImplementation(
    (_url: string, init: RequestInit) =>
      new Promise((_resolve, reject) => {
        init.signal?.addEventListener('abort', () =>
          reject(init.signal?.reason),
        )
      }),
  )
}

test('A cancel with a custom reason is rethrown as it is', async () => {
  const controller = new AbortController()
  const reason = new Error('route changed')
  pendingUntilAborted()

  const request = apiRequest('me', { signal: controller.signal })
  controller.abort(reason)

  await expect(request).rejects.toBe(reason)
})

test('A timeout on the fetch path becomes an ApiError that is not a problem', async () => {
  const controller = new AbortController()
  const reason = new DOMException('', 'TimeoutError')
  pendingUntilAborted()

  const request = apiRequest('me', { signal: controller.signal })
  controller.abort(reason)

  const error = await failureOf(request)
  expect(error.isProblem).toBe(false)
  expect(error.cause).toBe(reason)
})

test('A cancel with a custom reason during the body read is rethrown as it is', async () => {
  const controller = new AbortController()
  const reason = new Error('route changed')
  fetchMock.mockImplementation(async () => {
    controller.abort(reason)
    const response = problem('WRONG_PASSWORD')
    vi.spyOn(response, 'text').mockRejectedValue(reason)
    return response
  })

  await expect(apiRequest('me', { signal: controller.signal })).rejects.toBe(
    reason,
  )
})

test('A timeout during the body read becomes an ApiError that is not a problem', async () => {
  const controller = new AbortController()
  const reason = new DOMException('', 'TimeoutError')
  fetchMock.mockImplementation(async () => {
    controller.abort(reason)
    const response = problem('WRONG_PASSWORD')
    vi.spyOn(response, 'text').mockRejectedValue(reason)
    return response
  })

  const error = await failureOf(apiRequest('me', { signal: controller.signal }))

  expect(error.isProblem).toBe(false)
  expect(error.cause).toBe(reason)
})

test('A network failure with a signal that is not aborted becomes an ApiError', async () => {
  const controller = new AbortController()
  fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

  const error = await failureOf(apiRequest('me', { signal: controller.signal }))

  expect(error.isProblem).toBe(false)
})

test('A non-problem error response has its body cancelled', async () => {
  const response = new Response('<html>Bad Gateway</html>', {
    status: 502,
    headers: { 'Content-Type': 'text/html' },
  })
  const cancel = vi.spyOn(response.body!, 'cancel')
  fetchMock.mockResolvedValue(response)

  const error = await failureOf(apiRequest('me'))

  expect(cancel).toHaveBeenCalledTimes(1)
  expect(error.status).toBe(502)
  expect(error.isProblem).toBe(false)
})

test('A body cancel that fails or hangs does not replace or delay the error', async () => {
  const cancels: Array<() => Promise<void>> = [
    () => Promise.reject(new TypeError('cancel failed')),
    () => new Promise<void>(() => undefined),
    () => {
      throw new TypeError('cancel threw')
    },
  ]
  for (const cancelBody of cancels) {
    const response = new Response('<html>Bad Gateway</html>', {
      status: 502,
      headers: { 'Content-Type': 'text/html' },
    })
    vi.spyOn(response.body!, 'cancel').mockImplementation(cancelBody)
    fetchMock.mockResolvedValueOnce(response)

    const error = await failureOf(apiRequest('me'))

    expect(error.status).toBe(502)
    expect(error.isProblem).toBe(false)
  }
})

test('Credentials are passed to fetch only when the caller sets them', async () => {
  fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

  await apiRequest('me', { credentials: 'omit' })
  await apiRequest('me')

  expect(fetchMock.mock.calls[0][1].credentials).toBe('omit')
  expect(fetchMock.mock.calls[1][1].credentials).toBeUndefined()
})
