import { cleanup, render, screen } from '@testing-library/react'
import i18next from 'i18next'
import { createElement } from 'react'
import { I18nextProvider } from 'react-i18next'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { App } from '../App'
import { detectLanguage } from './detectLanguage'
import { initI18n, LANGUAGE_STORAGE_KEY } from './initI18n'
import en from './en.json'
import { setUiLanguage } from '.'

const supported = ['en', 'ru']
const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockImplementation(() =>
    Promise.resolve(
      new Response(JSON.stringify({ status: 401, code: 'UNAUTHENTICATED' }), {
        status: 401,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    ),
  )
})

function renderLogin(instance: ReturnType<typeof i18next.createInstance>) {
  return render(
    createElement(
      I18nextProvider,
      { i18n: instance },
      createElement(
        MemoryRouter,
        { initialEntries: ['/login'] },
        createElement(App),
      ),
    ),
  )
}

afterEach(async () => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  await i18next.changeLanguage('en')
  localStorage.clear()
  document.documentElement.lang = 'en'
})

function fakeStorage(initial: Record<string, string> = {}) {
  const data = { ...initial }
  return {
    getItem: vi.fn((key: string) => data[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      data[key] = value
    }),
  }
}

function env(overrides: Partial<Parameters<typeof initI18n>[1]> = {}) {
  return {
    storage: { localStorage: fakeStorage() },
    languages: ['en-US'],
    language: 'en-US',
    root: { lang: '' },
    ...overrides,
  }
}

test('[web-client/R19-S1] The stored choice wins over the browser languages', () => {
  expect(detectLanguage(supported, 'ru', ['en-US'], 'en-US')).toBe('ru')
})

test('[web-client/R19-S2] The first supported browser language is used, without its region', () => {
  expect(
    detectLanguage(supported, null, ['de-DE', 'RU-ru', 'en-US'], 'de-DE'),
  ).toBe('ru')
})

test('[web-client/R19-S3] Unsupported browser languages give English', () => {
  expect(detectLanguage(supported, null, ['de-DE', 'fr'], 'de-DE')).toBe('en')
})

test('[web-client/R19-S4] An unsupported stored value is ignored and kept', async () => {
  const storage = fakeStorage({ [LANGUAGE_STORAGE_KEY]: 'xx' })
  const instance = i18next.createInstance()
  await initI18n(
    instance,
    env({ storage: { localStorage: storage }, languages: ['ru-RU'] }),
  )
  expect(instance.language).toBe('ru')
  expect(storage.getItem(LANGUAGE_STORAGE_KEY)).toBe('xx')
  expect(storage.setItem).not.toHaveBeenCalled()
})

test('[web-client/R19-S5] Unavailable storage does not stop the app', async () => {
  const throwingProperty = {
    get localStorage(): never {
      throw new DOMException('denied', 'SecurityError')
    },
  }
  const throwingMethods = {
    localStorage: {
      getItem: () => {
        throw new Error('get')
      },
      setItem: () => {
        throw new Error('set')
      },
    },
  }
  for (const storage of [throwingProperty, throwingMethods]) {
    const instance = i18next.createInstance()
    const { setUiLanguage: set } = await initI18n(instance, env({ storage }))
    expect(instance.language).toBe('en')
    await set('ru')
    expect(instance.language).toBe('ru')
  }
})

test('[web-client/R19-S6] The page language equals the UI language at start', async () => {
  for (const lng of ['en', 'ru']) {
    const root = { lang: '' }
    const instance = i18next.createInstance()
    await initI18n(instance, env({ languages: [lng], language: lng, root }))
    expect(root.lang).toBe(lng)
    expect(instance.language).toBe(lng)
  }
})

test('[web-client/R19-S7] Missing browser languages and a stored value in another case', () => {
  expect(detectLanguage(supported, null, [], 'ru-RU')).toBe('ru')
  expect(detectLanguage(supported, null, undefined, 'ru-RU')).toBe('ru')
  expect(detectLanguage(supported, 'RU', ['en-US'], 'en-US')).toBe('ru')
  expect(detectLanguage(supported, null, undefined, undefined)).toBe('en')
})

test('[web-client/R2-S2] A Russian string is shown when the UI language is Russian', async () => {
  const instance = i18next.createInstance()
  await initI18n(
    instance,
    env({
      languages: ['ru'],
      language: 'ru',
      resources: {
        en: { translation: en },
        ru: { translation: { login: { title: 'Вход' } } },
      },
    }),
  )
  renderLogin(instance)
  expect(await screen.findByRole('heading', { name: 'Вход' })).toBeTruthy()
  expect(screen.queryByRole('heading', { name: en.login.title })).toBeNull()
})

test('[web-client/R2-S3] A key missing in Russian shows the English text', async () => {
  const instance = i18next.createInstance()
  await initI18n(
    instance,
    env({
      languages: ['ru'],
      language: 'ru',
      resources: {
        en: { translation: en },
        ru: { translation: { uiLanguage: { label: 'Язык' } } },
      },
    }),
  )
  expect(instance.language).toBe('ru')
  renderLogin(instance)
  expect(
    await screen.findByRole('heading', { name: en.login.title }),
  ).toBeTruthy()
})

test('setUiLanguage changes the language, persists it and sets lang', async () => {
  await setUiLanguage('ru')
  expect(i18next.language).toBe('ru')
  expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('ru')
  expect(document.documentElement.lang).toBe('ru')
})
