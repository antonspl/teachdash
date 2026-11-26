import type { i18n as I18nInstance } from 'i18next'
import { initReactI18next } from 'react-i18next'
import { detectLanguage, FALLBACK_LANGUAGE } from './detectLanguage'
import en from './en.json'
import ru from './ru.json'

export const LANGUAGE_STORAGE_KEY = 'teachdash.lang'

export const resources = {
  en: { translation: en },
  ru: { translation: ru },
}

export type StorageLike = {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
}

export type I18nEnv = {
  // The object that owns the `localStorage` property (`window` in the app).
  storage: { readonly localStorage: StorageLike }
  languages: readonly string[] | undefined
  language: string | undefined
  root: { lang: string }
  // Tests pass their own bundles; the app uses the bundled ones.
  resources?: typeof resources
}

// Reads and writes the stored choice. Every access, including reading the
// `localStorage` property, is guarded; on failure the last value lives in memory.
function createStorage(host: I18nEnv['storage']) {
  let memory: string | null = null
  return {
    get(): string | null {
      try {
        return host.localStorage.getItem(LANGUAGE_STORAGE_KEY)
      } catch {
        return memory
      }
    },
    set(value: string) {
      memory = value
      try {
        host.localStorage.setItem(LANGUAGE_STORAGE_KEY, value)
      } catch {
        // kept in memory for the session
      }
    },
  }
}

// Startup detection never writes storage; the returned setUiLanguage does.
export async function initI18n(instance: I18nInstance, env: I18nEnv) {
  const bundles = env.resources ?? resources
  const supported = Object.keys(bundles)
  const store = createStorage(env.storage)
  const lng = detectLanguage(
    supported,
    store.get(),
    env.languages,
    env.language,
  )
  await instance.use(initReactI18next).init({
    lng,
    fallbackLng: FALLBACK_LANGUAGE,
    resources: bundles,
    interpolation: { escapeValue: false },
  })
  env.root.lang = lng
  instance.on('languageChanged', (changed) => {
    env.root.lang = changed
  })
  async function setUiLanguage(next: string) {
    const value = next.toLowerCase()
    if (!supported.includes(value)) {
      return
    }
    await instance.changeLanguage(value)
    store.set(value)
  }
  return { setUiLanguage }
}
