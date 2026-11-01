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
  // AbortSignal.timeout uses the runtime's own timers, which fake timers do not
  // control, so it is replaced by a signal driven by the faked setTimeout.
  vi.useFakeTimers()
  vi.spyOn(AbortSignal, 'timeout').mockImplementation((ms: number) => {
    const controller = new AbortController()
    setTimeout(
      () => controller.abort(new DOMException('timed out', 'TimeoutError')),
      ms,
    )
    return controller.signal
  })
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
    vi.restoreAllMocks()
    vi.useRealTimers()
  }
})

test('[web-client/R3-S3] Page recovers when the backend comes back', async () => {
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

    // when
    await act(() => vi.advanceTimersByTimeAsync(5_000))

    // then
    expect(screen.getByRole('status').textContent).toBe(en.status.reachable)
    expect(fetchMock).toHaveBeenCalledTimes(2)

    await act(() => vi.advanceTimersByTimeAsync(30_000))
    expect(fetchMock).toHaveBeenCalledTimes(2)
  } finally {
    vi.useRealTimers()
  }
})

test('Status stays "unreachable" between retries and requests do not overlap', async () => {
  vi.useFakeTimers()
  try {
    let resolveSlow: (response: Response) => void = () => undefined
    fetchMock
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockImplementationOnce(
        () => new Promise<Response>((resolve) => (resolveSlow = resolve)),
      )
    render(
      <MemoryRouter>
        <PlaceholderPage />
      </MemoryRouter>,
    )
    await act(() => vi.advanceTimersByTimeAsync(0))

    await act(() => vi.advanceTimersByTimeAsync(5_000))
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('status').textContent).toBe(en.status.unreachable)

    await act(() => vi.advanceTimersByTimeAsync(4_000))
    expect(fetchMock).toHaveBeenCalledTimes(2)

    resolveSlow(problemResponse())
    await act(() => vi.advanceTimersByTimeAsync(0))
    expect(screen.getByRole('status').textContent).toBe(en.status.reachable)
  } finally {
    vi.useRealTimers()
  }
})

test('Retries stop when the page is unmounted', async () => {
  vi.useFakeTimers()
  try {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
    const view = render(
      <MemoryRouter>
        <PlaceholderPage />
      </MemoryRouter>,
    )
    await act(() => vi.advanceTimersByTimeAsync(0))
    expect(fetchMock).toHaveBeenCalledTimes(1)

    view.unmount()
    await act(() => vi.advanceTimersByTimeAsync(60_000))

    expect(fetchMock).toHaveBeenCalledTimes(1)
  } finally {
    vi.useRealTimers()
  }
})
