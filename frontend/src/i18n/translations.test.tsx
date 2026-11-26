import { cleanup, render, screen } from '@testing-library/react'
import i18next from 'i18next'
import { I18nextProvider } from 'react-i18next'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '.'
import { App } from '../App'
import { leaves, setPath } from './bundleTools'
import en from './en.json'
import { timeZoneOptions } from '../timeZones'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

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

function userResponse() {
  return new Response(
    JSON.stringify({
      id: '1',
      name: 'Anna Smith',
      email: 'anna@example.com',
      role: 'TEACHER',
      timeZone: 'UTC',
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

test('[web-client/R2-S1] Page text comes from the translation resources', async () => {
  // given
  const instance = await testInstance()
  // A fixed zone list for the profile page, so the check does not depend on the
  // runtime's own list.
  vi.stubGlobal(
    'Intl',
    Object.create(Intl, {
      supportedValuesOf: { value: () => ['Test/Zone-One', 'Test/Zone-Two'] },
    }),
  )

  for (const path of ['/', '/no-such-page', '/profile']) {
    fetchMock.mockImplementation(() =>
      Promise.resolve(path === '/profile' ? userResponse() : problemResponse()),
    )

    // when
    const view = render(
      <I18nextProvider i18n={instance}>
        <MemoryRouter initialEntries={[path]}>
          <App />
        </MemoryRouter>
      </I18nextProvider>,
    )
    if (path === '/') {
      await screen.findByText('TEST<login.title>')
    }
    if (path === '/profile') {
      await screen.findByText('TEST<profile.title>')
    }

    // then
    expect(document.title).toBe('TEST<app.name>')
    // The user's name and the brackets around the role are data, not text.
    // The time zone ids in the select are data too; whole ids are removed, longest first.
    let shown = (view.container.textContent ?? '').replace(
      /Anna Smith|[()]/g,
      '',
    )
    for (const zone of timeZoneOptions('UTC').sort(
      (a, b) => b.length - a.length,
    )) {
      shown = shown.replaceAll(zone, '')
    }
    expect(shown).toMatch(/TEST<[\w.]+>/)
    expect(shown.replace(/TEST<[\w.]+>/g, '').trim(), path).toBe('')
    view.unmount()
  }
})
