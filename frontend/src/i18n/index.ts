import i18next from 'i18next'
import { initI18n } from './initI18n'

const { setUiLanguage } = await initI18n(i18next, {
  storage: window,
  languages: navigator.languages,
  language: navigator.language,
  root: document.documentElement,
})

export { setUiLanguage }
export default i18next
