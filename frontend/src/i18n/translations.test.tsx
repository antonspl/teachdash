import { cleanup, render, screen } from '@testing-library/react'
import i18next from 'i18next'
import { I18nextProvider } from 'react-i18next'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '.'
import { App } from '../App'
import en from './en.json'

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
      await screen.findByText('TEST<login.title>')
    }

    // then
    expect(document.title).toBe('TEST<app.name>')
    const shown = view.container.textContent ?? ''
    expect(shown).toMatch(/TEST<[\w.]+>/)
    expect(shown.replace(/TEST<[\w.]+>/g, '').trim()).toBe('')
    view.unmount()
  }
})

test('[web-client/R2-S1] The profile page text comes from the translation resources', async () => {
  // given
  const instance = await testInstance()
  fetchMock.mockResolvedValue(
    new Response(
      JSON.stringify({
        id: '1',
        name: 'Anna Smith',
        email: 'anna@example.com',
        role: 'TEACHER',
        timeZone: 'UTC',
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ),
  )

  // when
  const view = render(
    <I18nextProvider i18n={instance}>
      <MemoryRouter initialEntries={['/profile']}>
        <App />
      </MemoryRouter>
    </I18nextProvider>,
  )
  await screen.findByText('TEST<profile.title>')

  // then
  // The user's name and the brackets around the role are data, not text.
  const shown = (view.container.textContent ?? '').replace(
    /Anna Smith|[()]/g,
    '',
  )
  expect(shown.replace(/TEST<[\w.]+>/g, '').trim()).toBe('')
})
