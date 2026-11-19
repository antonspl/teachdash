import type { Ref } from 'react'
import { useTranslation } from 'react-i18next'
import { useLanguages } from '../api/useLanguages'
import { ErrorWithRetry } from './ErrorWithRetry'
import { Select } from './Select'

export function LanguageSelect({
  label,
  value,
  onChange,
  error,
  required,
  disabled,
  inputRef,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  required?: boolean
  disabled?: boolean
  inputRef?: Ref<HTMLSelectElement>
}) {
  const { t } = useTranslation()
  const { codes, failed, retry } = useLanguages()

  // The stored value is always offered, even when the list lacks it or failed.
  const options =
    value === '' || codes.includes(value) ? codes : [...codes, value]
  const labels: Record<string, string> = {}
  for (const code of options) {
    labels[code] = t(`languages.${code}`, { defaultValue: code })
  }

  return (
    <>
      <Select
        label={label}
        value={value}
        onChange={onChange}
        options={options}
        labels={labels}
        placeholder={value === '' ? t('languages.choose') : undefined}
        error={error}
        required={required}
        disabled={disabled}
        inputRef={inputRef}
      />
      {failed ? (
        <ErrorWithRetry message={t('languages.loadFailed')} onRetry={retry} />
      ) : null}
    </>
  )
}
