import { useTranslation } from 'react-i18next'
import { setUiLanguage } from '../i18n'
import { Select } from './Select'
import styles from './UiLanguageSelect.module.css'

const LANGUAGES = ['en', 'ru']

// The interface language switch. Not the course or native language select.
export function UiLanguageSelect() {
  const { t, i18n } = useTranslation()
  const labels: Record<string, string> = {}
  const optionLang: Record<string, string> = {}
  for (const code of LANGUAGES) {
    labels[code] = t(`uiLanguage.name.${code}`, { lng: code })
    optionLang[code] = code
  }
  const current = LANGUAGES.includes(i18n.language) ? i18n.language : 'en'
  return (
    <div className={styles.wrap}>
      <Select
        label={t('uiLanguage.label')}
        hideLabel
        value={current}
        onChange={(value) => void setUiLanguage(value)}
        options={LANGUAGES}
        labels={labels}
        optionLang={optionLang}
      />
    </div>
  )
}
