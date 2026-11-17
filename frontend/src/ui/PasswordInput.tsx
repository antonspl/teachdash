import { useState, type Ref } from 'react'
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
  inputRef,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  autoComplete?: string
  error?: string
  required?: boolean
  disabled?: boolean
  inputRef?: Ref<HTMLInputElement>
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
      inputRef={inputRef}
      type={shown ? 'text' : 'password'}
      adornment={
        <button
          type="button"
          className={styles.toggle}
          disabled={disabled}
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
