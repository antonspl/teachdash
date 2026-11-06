import { useEffect } from 'react'
import { act, cleanup, render, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '../i18n'
import { apiRequest } from '../api/client'
import { AuthProvider, useAuth, type AuthContextValue } from './AuthProvider'

const fetchMock = vi.fn()
const user = {
  id: '1',
  name: 'Ann',
  email: 'ann@example.com',
  role: 'TEACHER',
  timeZone: 'UTC',
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function problem(code: string, status: number) {
  return new Response(JSON.stringify({ status, code }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

const probe: { current?: AuthContextValue } = {}
const auth = new Proxy({} as AuthContextValue, {
  get: (_, key) => probe.current?.[key as keyof AuthContextValue],
})
function Probe() {
  const value = useAuth()
  useEffect(() => {
    probe.current = value
  })
  return null
}

function mount() {
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  )
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

test('start: 200 from me gives the user', async () => {
  fetchMock.mockResolvedValue(json(user))
  mount()
  expect(auth.state.status).toBe('loading')
  await waitFor(() => expect(auth.state).toEqual({ status: 'user', user }))
})

test('start: 401 from me gives anonymous without a reason', async () => {
  fetchMock.mockResolvedValue(problem('UNAUTHENTICATED', 401))
  mount()
  await waitFor(() => expect(auth.state).toEqual({ status: 'anonymous' }))
})

test('start: a 500 or a network failure gives error, and retry recovers', async () => {
  fetchMock.mockResolvedValueOnce(problem('INTERNAL_ERROR', 500))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('error'))

  fetchMock.mockRejectedValueOnce(new TypeError('network'))
  act(() => auth.retry())
  await waitFor(() => expect(auth.state.status).toBe('error'))

  fetchMock.mockResolvedValueOnce(json(user))
  act(() => auth.retry())
  expect(auth.state.status).toBe('loading')
  await waitFor(() => expect(auth.state.status).toBe('user'))
})

test('login sends credentials and sets the user', async () => {
  fetchMock.mockResolvedValueOnce(problem('UNAUTHENTICATED', 401))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('anonymous'))

  fetchMock.mockResolvedValueOnce(json(user))
  let result
  await act(async () => {
    result = await auth.login('ann@example.com', 'secret')
  })
  const [url, init] = fetchMock.mock.calls[1]
  expect(url).toBe('/api/auth/login')
  expect(JSON.parse(init.body)).toEqual({
    email: 'ann@example.com',
    password: 'secret',
  })
  expect(result).toEqual(user)
  expect(auth.state).toEqual({ status: 'user', user })
})

test('failed login rejects and stays anonymous without a reason', async () => {
  fetchMock.mockResolvedValueOnce(problem('UNAUTHENTICATED', 401))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('anonymous'))

  fetchMock.mockResolvedValueOnce(problem('INVALID_CREDENTIALS', 401))
  await act(async () => {
    await expect(auth.login('a@b.c', 'x')).rejects.toMatchObject({
      code: 'INVALID_CREDENTIALS',
    })
  })
  expect(auth.state).toEqual({ status: 'anonymous' })
})

test('setPasswordWithLink posts token and password and sets the user', async () => {
  fetchMock.mockResolvedValueOnce(problem('UNAUTHENTICATED', 401))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('anonymous'))

  fetchMock.mockResolvedValueOnce(json(user))
  await act(async () => {
    await auth.setPasswordWithLink('tok', 'longenough')
  })
  const [url, init] = fetchMock.mock.calls[1]
  expect(url).toBe('/api/password-links')
  expect(JSON.parse(init.body)).toEqual({
    token: 'tok',
    password: 'longenough',
  })
  expect(auth.state).toEqual({ status: 'user', user })
})

test('logout success gives anonymous without a reason', async () => {
  fetchMock.mockResolvedValueOnce(json(user))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('user'))

  fetchMock.mockResolvedValueOnce(new Response(null, { status: 200 }))
  await act(async () => {
    await auth.logout()
  })
  expect(auth.state).toEqual({ status: 'anonymous' })
})

test('logout answered 401 also gives anonymous without a reason', async () => {
  fetchMock.mockResolvedValueOnce(json(user))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('user'))

  fetchMock.mockResolvedValueOnce(problem('UNAUTHENTICATED', 401))
  await act(async () => {
    await auth.logout()
  })
  expect(auth.state).toEqual({ status: 'anonymous' })
})

test('logout failing otherwise keeps the user and returns the error', async () => {
  fetchMock.mockResolvedValueOnce(json(user))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('user'))

  fetchMock.mockResolvedValueOnce(problem('INTERNAL_ERROR', 500))
  await act(async () => {
    await expect(auth.logout()).rejects.toMatchObject({ status: 500 })
  })
  expect(auth.state).toEqual({ status: 'user', user })
})

test('a 401 from another request ends the session with the current path', async () => {
  window.history.replaceState(null, '', '/teacher?tab=1')
  fetchMock.mockResolvedValueOnce(json(user))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('user'))

  fetchMock.mockResolvedValueOnce(problem('UNAUTHENTICATED', 401))
  await act(async () => {
    await apiRequest('courses').catch(() => undefined)
  })
  expect(auth.state).toEqual({
    status: 'anonymous',
    sessionEnded: { path: '/teacher?tab=1' },
  })
  window.history.replaceState(null, '', '/')
})

test('a 401 from login does not end a session', async () => {
  fetchMock.mockResolvedValueOnce(json(user))
  mount()
  await waitFor(() => expect(auth.state.status).toBe('user'))

  fetchMock.mockResolvedValueOnce(problem('INVALID_CREDENTIALS', 401))
  await act(async () => {
    await auth.login('a@b.c', 'x').catch(() => undefined)
  })
  expect(auth.state).toEqual({ status: 'user', user })
})
