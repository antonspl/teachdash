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
  disabled,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  autoComplete?: string
  error?: string
  required?: boolean
  disabled?: boolean
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
      disabled={disabled}
      type={shown ? 'text' : 'password'}
      adornment={
        <button
          type="button"
          className={styles.toggle}
          aria-pressed={shown}
          aria-label={t(shown ? 'ui.hidePassword' : 'ui.showPassword', {
            label,
          })}
          onClick={() => setShown((current) => !current)}
        >
          {shown ? t('ui.hide') : t('ui.show')}
        </button>
      }
    />
  )
}
