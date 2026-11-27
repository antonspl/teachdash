import i18next from 'i18next'
import { initReactI18next } from 'react-i18next'
import { initI18n } from './initI18n'

// The app instance is wired to React once, before it is initialised.
i18next.use(initReactI18next)

const { setUiLanguage } = await initI18n(i18next, {
  storage: window,
  languages: navigator.languages,
  language: navigator.language,
  root: document.documentElement,
})

export { setUiLanguage }
export default i18next
