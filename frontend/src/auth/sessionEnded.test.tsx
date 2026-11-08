import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '../i18n'
import { App } from '../App'
import { apiRequest } from '../api/client'
import en from '../i18n/en.json'

const fetchMock = vi.fn()

function problem(status: number, code: string) {
  return new Response(JSON.stringify({ status, code }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function userResponse(role: string) {
  return new Response(
    JSON.stringify({
      id: '1',
      name: 'Anna Smith',
      email: 'a@example.com',
      role,
      timeZone: 'UTC',
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

// Test-only stand-in for a cabinet page that sends a request through the API client.
function SendRequest() {
  return (
    <button onClick={() => void apiRequest('lessons').catch(() => undefined)}>
      send request
    </button>
  )
}

// Logged in as `startAs` until a request answers 401, then the login accepts `loginAs`.
function backend(startAs: string, loginAs: string) {
  let sessionEnded = false
  fetchMock.mockImplementation((url: string) => {
    if (url === '/api/me') {
      return Promise.resolve(
        sessionEnded ? problem(401, 'UNAUTHENTICATED') : userResponse(startAs),
      )
    }
    if (url === '/api/auth/login') {
      return Promise.resolve(userResponse(loginAs))
    }
    sessionEnded = true
    return Promise.resolve(problem(401, 'UNAUTHENTICATED'))
  })
}

function open(path: string) {
  window.history.pushState({}, '', path)
  return render(
    <BrowserRouter>
      <App />
      <SendRequest />
    </BrowserRouter>,
  )
}

async function endSession() {
  fireEvent.click(await screen.findByRole('button', { name: 'send request' }))
  await screen.findByText(en.login.sessionEnded)
}

async function logIn() {
  fireEvent.change(screen.getByLabelText(en.login.email), {
    target: { value: 'a@example.com' },
  })
  fireEvent.change(screen.getByLabelText(en.login.password), {
    target: { value: 'secret-pass' },
  })
  fireEvent.click(screen.getByRole('button', { name: en.login.submit }))
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

test('[web-auth/R6-S1] Session end sends the user to login', async () => {
  // given
  backend('TEACHER', 'TEACHER')
  open('/teacher')
  await screen.findByText(en.cabinet.teacher)

  // when
  await endSession()

  // then
  expect(screen.getByRole('heading', { name: en.login.title })).toBeTruthy()
  expect(window.location.pathname).toBe('/login')
})

test('[web-auth/R6-S2] After logging in again the user returns to the page', async () => {
  // given
  backend('TEACHER', 'TEACHER')
  open('/teacher?x=1')
  await screen.findByText(en.cabinet.teacher)
  await endSession()

  // when
  await logIn()

  // then
  expect(await screen.findByText(en.cabinet.teacher)).toBeTruthy()
  expect(window.location.pathname + window.location.search).toBe('/teacher?x=1')
})

test('[web-auth/R6-S3] A page of another role is not restored', async () => {
  // given
  backend('TEACHER', 'STUDENT')
  open('/teacher')
  await screen.findByText(en.cabinet.teacher)
  await endSession()

  // when
  await logIn()

  // then
  expect(await screen.findByText(en.cabinet.student)).toBeTruthy()
  expect(window.location.pathname).toBe('/student')
})

test('[web-auth/R6-S4] After a reload the return is not kept', async () => {
  // given
  backend('TEACHER', 'TEACHER')
  const first = open('/teacher?x=1')
  await screen.findByText(en.cabinet.teacher)
  await endSession()

  // when the page is reloaded: a fresh app on the current address
  first.unmount()
  open(window.location.pathname)
  await screen.findByLabelText(en.login.email)
  expect(screen.queryByText(en.login.sessionEnded)).toBeNull()
  await logIn()

  // then
  expect(await screen.findByText(en.cabinet.teacher)).toBeTruthy()
  expect(window.location.pathname + window.location.search).toBe('/teacher')
  expect(screen.queryByText(en.login.sessionEnded)).toBeNull()
})
