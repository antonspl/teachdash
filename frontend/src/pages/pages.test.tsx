import { StrictMode } from 'react'
import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '../i18n'
import en from '../i18n/en.json'
import { LoginPage } from './LoginPage'

vi.mock('../auth/AuthProvider', () => ({
  useAuth: () => ({ state: { status: 'anonymous' }, login: vi.fn() }),
}))

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

function problemResponse() {
  return new Response(
    JSON.stringify({ status: 401, code: 'UNAUTHENTICATED' }),
    {
      status: 401,
      headers: { 'Content-Type': 'application/problem+json' },
    },
  )
}

test('[web-client/R12-S2] Backend cannot be reached', async () => {
  const failures: Array<[string, () => Promise<Response>]> = [
    ['network error', () => Promise.reject(new TypeError('Failed to fetch'))],
    [
      'non-problem 502',
      async () =>
        new Response('<html>Bad Gateway</html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html' },
        }),
    ],
    [
      'HTML 200 such as a captive portal',
      async () =>
        new Response('<html>Sign in</html>', {
          status: 200,
          headers: { 'Content-Type': 'text/html; charset=utf-8' },
        }),
    ],
    [
      'JSON 200 whose body drops',
      async () => {
        const response = new Response('{}', {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
        vi.spyOn(response, 'text').mockRejectedValue(new TypeError('dropped'))
        return response
      },
    ],
  ]

  for (const [name, respond] of failures) {
    // given
    fetchMock.mockImplementationOnce(respond)

    // when
    const view = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    // then
    const status = await screen.findByText(en.login.unreachable)
    expect(status.getAttribute('role'), name).toBe('status')
    view.unmount()
  }
})

test('A problem response whose body read fails counts as unreachable', async () => {
  const response = problemResponse()
  vi.spyOn(response, 'text').mockRejectedValue(new TypeError('network error'))
  fetchMock.mockResolvedValueOnce(response)

  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  const status = await screen.findByText(en.login.unreachable)
  expect(status.getAttribute('role')).toBe('status')
})

test('Any response with a problem body or a 2xx status means the backend is reachable', async () => {
  const responses: Array<[string, () => Response]> = [
    ['problem 401', problemResponse],
    [
      '2xx',
      () =>
        new Response('{}', {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
    ],
  ]

  for (const [, respond] of responses) {
    fetchMock.mockResolvedValueOnce(respond())

    const view = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    await expectReachable()
    view.unmount()
  }
})

test('Status is "checking" until the request finishes', async () => {
  fetchMock.mockReturnValue(new Promise(() => undefined))

  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  expect(screen.queryByRole('status')).toBeNull()
})

test('A request that never answers counts as unreachable after 10 seconds', async () => {
  vi.useFakeTimers()
  try {
    fetchMock.mockImplementation(
      (_url: string, init?: RequestInit) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener('abort', () =>
            reject(init.signal?.reason),
          )
        }),
    )

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    await act(() => vi.advanceTimersByTimeAsync(9_999))
    expect(screen.queryByRole('status')).toBeNull()

    await act(() => vi.advanceTimersByTimeAsync(1))
    expect(screen.getByRole('status').textContent).toBe(en.login.unreachable)
  } finally {
    vi.useRealTimers()
  }
})

test('A JSON content type with parameters or a +json suffix is reachable', async () => {
  for (const contentType of [
    'Application/JSON; charset=utf-8',
    'application/vnd.api+json',
  ]) {
    fetchMock.mockResolvedValueOnce(
      new Response('{}', {
        status: 200,
        headers: { 'Content-Type': contentType },
      }),
    )
    const view = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    await expectReachable()
    view.unmount()
  }
})

test('Unmounting aborts the running request', async () => {
  let signal: AbortSignal | undefined | null
  fetchMock.mockImplementation((_url: string, init?: RequestInit) => {
    signal = init?.signal
    return new Promise(() => undefined)
  })
  const view = render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
  expect(signal?.aborted).toBe(false)

  view.unmount()

  expect(signal?.aborted).toBe(true)
})

test('A JSON 200 whose body hangs is unreachable after 10 seconds', async () => {
  vi.useFakeTimers()
  try {
    fetchMock.mockImplementation(async (_url: string, init?: RequestInit) => {
      const response = new Response('{}', {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
      vi.spyOn(response, 'text').mockImplementation(
        () =>
          new Promise<string>((_resolve, reject) => {
            init?.signal?.addEventListener('abort', () =>
              reject(init.signal?.reason),
            )
          }),
      )
      return response
    })
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    await act(() => vi.advanceTimersByTimeAsync(9_999))
    expect(screen.queryByRole('status')).toBeNull()

    await act(() => vi.advanceTimersByTimeAsync(1))
    expect(screen.getByRole('status').textContent).toBe(en.login.unreachable)
  } finally {
    vi.useRealTimers()
  }
})

test('The status check does not send cookies', async () => {
  fetchMock.mockResolvedValue(problemResponse())
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  await expectReachable()

  expect(fetchMock.mock.calls[0][1].credentials).toBe('omit')
})

function stubHidden(hidden: boolean) {
  Object.defineProperty(document, 'hidden', {
    configurable: true,
    get: () => hidden,
  })
  document.dispatchEvent(new Event('visibilitychange'))
}

afterEach(() => {
  Reflect.deleteProperty(document, 'hidden')
})

test('[web-client/R12-S4] Page checks again when the tab is shown, and not on a timer', async () => {
  // given
  vi.useFakeTimers()
  try {
    fetchMock
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(problemResponse())
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )
    await act(() => vi.advanceTimersByTimeAsync(0))
    expect(screen.getByRole('status').textContent).toBe(en.login.unreachable)
    stubHidden(true)

    // when
    await act(() => vi.advanceTimersByTimeAsync(10 * 60_000))
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await act(async () => stubHidden(false))
    await act(() => vi.advanceTimersByTimeAsync(0))

    // then
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(screen.queryByRole('status')).toBeNull()
  } finally {
    vi.useRealTimers()
  }
})

test('Becoming hidden does not start a check', async () => {
  fetchMock.mockResolvedValue(problemResponse())
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
  await expectReachable()

  await act(async () => stubHidden(true))

  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('The browser coming online starts a check, without showing "checking"', async () => {
  fetchMock
    .mockRejectedValueOnce(new TypeError('Failed to fetch'))
    .mockResolvedValueOnce(problemResponse())
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
  await screen.findByText(en.login.unreachable)

  await act(async () => {
    window.dispatchEvent(new Event('online'))
  })

  await expectReachable(2)
})

test('A check does not start while another is running', async () => {
  fetchMock.mockReturnValue(new Promise(() => undefined))
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  await act(async () => {
    window.dispatchEvent(new Event('online'))
    document.dispatchEvent(new Event('visibilitychange'))
  })

  expect(fetchMock).toHaveBeenCalledTimes(1)
})

function deferredFetch() {
  let reject: (reason: unknown) => void = () => undefined
  const promise = new Promise<Response>((_resolve, rej) => {
    reject = rej
  })
  return { promise, reject }
}

for (const [name, fire] of [
  ['online', () => window.dispatchEvent(new Event('online'))],
  [
    'visibilitychange',
    () => document.dispatchEvent(new Event('visibilitychange')),
  ],
] as const) {
  test(`A ${name} event during a running check causes one more check when it ends`, async () => {
    const first = deferredFetch()
    fetchMock
      .mockReturnValueOnce(first.promise)
      .mockResolvedValueOnce(problemResponse())
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    await act(async () => fire())
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await act(async () => first.reject(new TypeError('Failed to fetch')))

    await expectReachable()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
}

test('Several events during one check cause only one more check', async () => {
  const first = deferredFetch()
  fetchMock
    .mockReturnValueOnce(first.promise)
    .mockResolvedValue(problemResponse())
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  await act(async () => {
    window.dispatchEvent(new Event('online'))
    document.dispatchEvent(new Event('visibilitychange'))
    window.dispatchEvent(new Event('online'))
  })
  await act(async () => first.reject(new TypeError('Failed to fetch')))

  await expectReachable()
  await act(() => new Promise((resolve) => setTimeout(resolve, 20)))
  expect(fetchMock).toHaveBeenCalledTimes(2)
})

test('A visibilitychange to hidden during a running check causes no more check', async () => {
  const first = deferredFetch()
  fetchMock.mockReturnValueOnce(first.promise)
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  await act(async () => stubHidden(true))
  await act(async () => first.reject(new TypeError('Failed to fetch')))

  expect(await screen.findByText(en.login.unreachable)).toBeTruthy()
  await act(() => new Promise((resolve) => setTimeout(resolve, 20)))
  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('The request timer is cleared when the check ends', async () => {
  vi.useFakeTimers()
  try {
    fetchMock.mockResolvedValue(problemResponse())
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    await act(() => vi.advanceTimersByTimeAsync(0))

    expect(screen.queryByRole('status')).toBeNull()
    expect(vi.getTimerCount()).toBe(0)
  } finally {
    vi.useRealTimers()
  }
})

test('A 2xx response that is not JSON has its body discarded', async () => {
  const cancel = vi.fn()
  const body = new ReadableStream({ cancel })
  fetchMock.mockResolvedValue(
    new Response(body, {
      status: 200,
      headers: { 'Content-Type': 'text/html' },
    }),
  )
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  expect(await screen.findByText(en.login.unreachable)).toBeTruthy()
  expect(cancel).toHaveBeenCalledTimes(1)
})

test('A tab that opens hidden is checked once', async () => {
  Object.defineProperty(document, 'hidden', {
    configurable: true,
    get: () => true,
  })
  fetchMock.mockResolvedValue(problemResponse())
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )

  await expectReachable()
  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('Nothing is sent on a timer', async () => {
  vi.useFakeTimers()
  try {
    fetchMock.mockImplementation(async () => problemResponse())
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )
    await act(() => vi.advanceTimersByTimeAsync(0))

    await act(() => vi.advanceTimersByTimeAsync(10 * 60_000))

    expect(fetchMock).toHaveBeenCalledTimes(1)
  } finally {
    vi.useRealTimers()
  }
})

test('StrictMode leaves one active setup', async () => {
  fetchMock.mockImplementation(async () => problemResponse())
  render(
    <StrictMode>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </StrictMode>,
  )
  await expectReachable()
  const afterStart = fetchMock.mock.calls.length

  await act(async () => {
    window.dispatchEvent(new Event('online'))
  })

  await waitForCalls(afterStart + 1)
  expect(fetchMock.mock.calls.length - afterStart).toBe(1)
})

test('Unmounting removes the listeners', async () => {
  fetchMock.mockImplementation(async () => problemResponse())
  const view = render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
  await expectReachable()
  view.unmount()

  window.dispatchEvent(new Event('online'))
  document.dispatchEvent(new Event('visibilitychange'))

  expect(fetchMock).toHaveBeenCalledTimes(1)
})

async function expectReachable(count?: number) {
  await waitFor(() =>
    count === undefined
      ? expect(fetchMock).toHaveBeenCalled()
      : expect(fetchMock).toHaveBeenCalledTimes(count),
  )
  await act(() => new Promise((resolve) => setTimeout(resolve, 10)))
  expect(screen.queryByText(en.login.unreachable)).toBeNull()
}

async function waitForCalls(count: number) {
  await vi.waitFor(() =>
    expect(fetchMock.mock.calls.length).toBeGreaterThanOrEqual(count),
  )
}
