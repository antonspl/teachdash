import { cleanup, fireEvent, render, screen } from '@testing-library/react'
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
})

function userResponse(role: string, name = 'Anna Smith') {
  return new Response(
    JSON.stringify({
      id: '1',
      name,
      email: 'anna@example.com',
      role,
      timeZone: 'UTC',
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

function problem(status: number, code: string) {
  return new Response(JSON.stringify({ status, code }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function open(path = '/profile') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

test('[web-profile/R1-S1] Each role opens the profile', async () => {
  for (const [role, cabinet] of [
    ['ADMIN', en.cabinet.admin],
    ['TEACHER', en.cabinet.teacher],
    ['STUDENT', en.cabinet.student],
  ]) {
    // given
    fetchMock.mockResolvedValue(userResponse(role, `Name of ${role}`))

    // when
    const view = open()

    // then
    expect(
      await screen.findByRole('heading', { name: en.profile.title }),
      role,
    ).toBeTruthy()
    expect(screen.getByText(new RegExp(`Name of ${role}`)), role).toBeTruthy()
    expect(screen.queryByText(cabinet), role).toBeNull()
    view.unmount()
  }
})

test('[web-profile/R1-S2] Anonymous visitor gets the login page', async () => {
  // given
  fetchMock.mockResolvedValue(problem(401, 'UNAUTHENTICATED'))

  // when
  open()

  // then
  expect(
    await screen.findByRole('heading', { name: en.login.title }),
  ).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.profile.title })).toBeNull()
})

test("[web-profile/R1-S4] Back to cabinet opens the user's own cabinet", async () => {
  // given
  fetchMock.mockResolvedValue(userResponse('STUDENT'))
  open()

  // when
  fireEvent.click(
    await screen.findByRole('button', { name: en.header.backToCabinet }),
  )

  // then
  expect(await screen.findByText(en.cabinet.student)).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.profile.title })).toBeNull()
})

test('[web-profile/R1-S6] Profile waits for the startup check', () => {
  // given
  fetchMock.mockReturnValue(new Promise(() => undefined))

  // when
  open()

  // then
  expect(screen.getByText(en.ui.loading)).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
  expect(screen.queryByRole('heading', { name: en.profile.title })).toBeNull()
})

test('[web-profile/R1-S7] Startup error on the profile page', async () => {
  // given
  fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
  fetchMock.mockResolvedValueOnce(userResponse('TEACHER'))

  // when
  open()

  // then
  expect(await screen.findByText(en.startup.error)).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: en.ui.tryAgain }))
  expect(
    await screen.findByRole('heading', { name: en.profile.title }),
  ).toBeTruthy()
})
