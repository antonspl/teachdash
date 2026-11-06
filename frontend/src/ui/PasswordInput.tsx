import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './Field.module.css'
import { TextInput } from './TextInput'

export function PasswordInput({
  label,
  value,
  onChange,
  autoComplete,
  error,
  required,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  autoComplete?: string
  error?: string
  required?: boolean
}) {
  const { t } = useTranslation()
  const [shown, setShown] = useState(false)
  return (
    <TextInput
      label={label}
      value={value}
      onChange={onChange}
      autoComplete={autoComplete}
      error={error}
      required={required}
      inputType={shown ? 'text' : 'password'}
      adornment={
        <button
          type="button"
          className={styles.toggle}
          aria-pressed={shown}
          onClick={() => setShown((current) => !current)}
        >
          {shown ? t('ui.hidePassword') : t('ui.showPassword')}
        </button>
      }
    />
  )
}
