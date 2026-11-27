import { useTranslation } from 'react-i18next'
import { FALLBACK_LANGUAGE } from '../i18n/detectLanguage'
import { setUiLanguage } from '../i18n'
import { Select } from './Select'
import styles from './UiLanguageSelect.module.css'

// The interface language switch. Not the course or native language select.
export function UiLanguageSelect() {
  const { t, i18n } = useTranslation()
  // The instance's own languages, so the list never offers one it cannot use.
  const languages = Object.keys(i18n.options.resources ?? {})
  const labels: Record<string, string> = {}
  const optionLang: Record<string, string> = {}
  for (const code of languages) {
    labels[code] = t(`uiLanguage.name.${code}`, {
      lng: code,
      fallbackLng: FALLBACK_LANGUAGE,
    })
    optionLang[code] = code
  }
  const current = languages.includes(i18n.language)
    ? i18n.language
    : FALLBACK_LANGUAGE
  return (
    <div className={styles.wrap}>
      <Select
        flush
        label={t('uiLanguage.label')}
        hideLabel
        value={current}
        onChange={(value) => void setUiLanguage(value)}
        options={languages}
        labels={labels}
        optionLang={optionLang}
      />
    </div>
  )
}
