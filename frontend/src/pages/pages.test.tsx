import { StrictMode } from 'react'
import { act, cleanup, render, screen } from '@testing-library/react'
import i18next from 'i18next'
import { I18nextProvider } from 'react-i18next'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '../i18n'
import { App } from '../App'
import en from '../i18n/en.json'
import { PlaceholderPage } from './PlaceholderPage'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

function leaves(node: unknown, prefix = ''): Array<[string, string]> {
  if (typeof node === 'string') {
    return [[prefix, node]]
  }
  return Object.entries(node as Record<string, unknown>).flatMap(
    ([key, child]) => leaves(child, prefix ? `${prefix}.${key}` : key),
  )
}

function setPath(target: Record<string, unknown>, path: string, value: string) {
  const keys = path.split('.')
  let node = target
  for (const key of keys.slice(0, -1)) {
    node[key] = (node[key] ?? {}) as Record<string, unknown>
    node = node[key] as Record<string, unknown>
  }
  node[keys[keys.length - 1]] = value
}

async function testInstance() {
  const resources: Record<string, unknown> = {}
  for (const [path] of leaves(en)) {
    setPath(resources, path, `TEST<${path}>`)
  }
  const instance = i18next.createInstance()
  await instance.init({
    lng: 'en',
    resources: { en: { translation: resources } },
    interpolation: { escapeValue: false },
  })
  return instance
}

function problemResponse() {
  return new Response(
    JSON.stringify({ status: 401, code: 'UNAUTHENTICATED' }),
    {
      status: 401,
      headers: { 'Content-Type': 'application/problem+json' },
    },
  )
}

test('[web-client/R2-S1] Page text comes from the translation resources', async () => {
  // given
  const instance = await testInstance()
  fetchMock.mockResolvedValue(problemResponse())

  for (const path of ['/', '/no-such-page']) {
    // when
    const view = render(
      <I18nextProvider i18n={instance}>
        <MemoryRouter initialEntries={[path]}>
          <App />
        </MemoryRouter>
      </I18nextProvider>,
    )
    if (path === '/') {
      await screen.findByText('TEST<status.reachable>')
    }

    // then
    expect(document.title).toBe('TEST<app.name>')
    const shown = view.container.textContent ?? ''
    expect(shown).toMatch(/TEST<[\w.]+>/)
    expect(shown.replace(/TEST<[\w.]+>/g, '').trim()).toBe('')
    view.unmount()
  }
})

test('[web-client/R3-S2] Backend cannot be reached', async () => {
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
        <PlaceholderPage />
      </MemoryRouter>,
    )

    // then
    const status = await screen.findByText(en.status.unreachable)
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
      <PlaceholderPage />
    </MemoryRouter>,
  )

  const status = await screen.findByText(en.status.unreachable)
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

  for (const [name, respond] of responses) {
    fetchMock.mockResolvedValueOnce(respond())

    const view = render(
      <MemoryRouter>
        <PlaceholderPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(en.status.reachable), name).toBeTruthy()
    view.unmount()
  }
})

test('Status is "checking" until the request finishes', async () => {
  fetchMock.mockReturnValue(new Promise(() => undefined))

  render(
    <MemoryRouter>
      <PlaceholderPage />
    </MemoryRouter>,
  )

  expect(screen.getByRole('status').textContent).toBe(en.status.checking)
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
        <PlaceholderPage />
      </MemoryRouter>,
    )

    await act(() => vi.advanceTimersByTimeAsync(9_999))
    expect(screen.getByRole('status').textContent).toBe(en.status.checking)

    await act(() => vi.advanceTimersByTimeAsync(1))
    expect(screen.getByRole('status').textContent).toBe(en.status.unreachable)
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
        <PlaceholderPage />
      </MemoryRouter>,
    )

    expect(
      await screen.findByText(en.status.reachable),
      contentType,
    ).toBeTruthy()
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
      <PlaceholderPage />
    </MemoryRouter>,
  )
  expect(signal?.aborted).toBe(false)

  view.unmount()

  expect(signal?.aborted).toBe(true)
})

test('[web-client/R3-S2] A JSON 200 whose body hangs is unreachable after 10 seconds', async () => {
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
        <PlaceholderPage />
      </MemoryRouter>,
    )

    await act(() => vi.advanceTimersByTimeAsync(9_999))
    expect(screen.getByRole('status').textContent).toBe(en.status.checking)

    await act(() => vi.advanceTimersByTimeAsync(1))
    expect(screen.getByRole('status').textContent).toBe(en.status.unreachable)
  } finally {
    vi.useRealTimers()
  }
})

test('The status check does not send cookies', async () => {
  fetchMock.mockResolvedValue(problemResponse())
  render(
    <MemoryRouter>
      <PlaceholderPage />
    </MemoryRouter>,
  )

  await screen.findByText(en.status.reachable)

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

test('[web-client/R3-S4] Page checks again when the tab is shown, and not on a timer', async () => {
  // given
  vi.useFakeTimers()
  try {
    fetchMock
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(problemResponse())
    render(
      <MemoryRouter>
        <PlaceholderPage />
      </MemoryRouter>,
    )
    await act(() => vi.advanceTimersByTimeAsync(0))
    expect(screen.getByRole('status').textContent).toBe(en.status.unreachable)
    stubHidden(true)

    // when
    await act(() => vi.advanceTimersByTimeAsync(10 * 60_000))
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await act(async () => stubHidden(false))
    await act(() => vi.advanceTimersByTimeAsync(0))

    // then
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('status').textContent).toBe(en.status.reachable)
  } finally {
    vi.useRealTimers()
  }
})

test('Becoming hidden does not start a check', async () => {
  fetchMock.mockResolvedValue(problemResponse())
  render(
    <MemoryRouter>
      <PlaceholderPage />
    </MemoryRouter>,
  )
  await screen.findByText(en.status.reachable)

  await act(async () => stubHidden(true))

  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('The browser coming online starts a check, without showing "checking"', async () => {
  fetchMock
    .mockRejectedValueOnce(new TypeError('Failed to fetch'))
    .mockResolvedValueOnce(problemResponse())
  render(
    <MemoryRouter>
      <PlaceholderPage />
    </MemoryRouter>,
  )
  await screen.findByText(en.status.unreachable)

  await act(async () => {
    window.dispatchEvent(new Event('online'))
  })

  expect(screen.getByRole('status').textContent).not.toBe(en.status.checking)
  expect(await screen.findByText(en.status.reachable)).toBeTruthy()
  expect(fetchMock).toHaveBeenCalledTimes(2)
})

test('A check does not start while another is running', async () => {
  fetchMock.mockReturnValue(new Promise(() => undefined))
  render(
    <MemoryRouter>
      <PlaceholderPage />
    </MemoryRouter>,
  )

  await act(async () => {
    window.dispatchEvent(new Event('online'))
    document.dispatchEvent(new Event('visibilitychange'))
  })

  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('A tab that opens hidden is checked once', async () => {
  Object.defineProperty(document, 'hidden', {
    configurable: true,
    get: () => true,
  })
  fetchMock.mockResolvedValue(problemResponse())
  render(
    <MemoryRouter>
      <PlaceholderPage />
    </MemoryRouter>,
  )

  expect(await screen.findByText(en.status.reachable)).toBeTruthy()
  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('Nothing is sent on a timer', async () => {
  vi.useFakeTimers()
  try {
    fetchMock.mockImplementation(async () => problemResponse())
    render(
      <MemoryRouter>
        <PlaceholderPage />
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
        <PlaceholderPage />
      </MemoryRouter>
    </StrictMode>,
  )
  await screen.findByText(en.status.reachable)
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
      <PlaceholderPage />
    </MemoryRouter>,
  )
  await screen.findByText(en.status.reachable)
  view.unmount()

  window.dispatchEvent(new Event('online'))
  document.dispatchEvent(new Event('visibilitychange'))

  expect(fetchMock).toHaveBeenCalledTimes(1)
})

async function waitForCalls(count: number) {
  await vi.waitFor(() =>
    expect(fetchMock.mock.calls.length).toBeGreaterThanOrEqual(count),
  )
}
