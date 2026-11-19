import { useId, type ReactNode } from 'react'
import styles from './Field.module.css'

export type TextInputProps = {
  label: string
  value: string
  onChange: (value: string) => void
  type?: 'text' | 'email' | 'password'
  autoComplete?: string
  error?: string
  required?: boolean
  disabled?: boolean
  // Extra control shown beside the input (used by PasswordInput).
  adornment?: ReactNode
}

export function TextInput({
  label,
  value,
  onChange,
  type = 'text',
  autoComplete,
  error,
  required,
  disabled,
  adornment,
}: TextInputProps) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className={styles.field}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>
      <div className={styles.row}>
        <input
          id={id}
          className={`${styles.input} ${error ? styles.invalid : ''}`}
          type={type}
          value={value}
          autoComplete={autoComplete}
          required={required}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          onChange={(event) => onChange(event.target.value)}
        />
        {adornment}
      </div>
      {error ? (
        <p id={errorId} className={styles.error}>
          {error}
        </p>
      ) : null}
    </div>
  )
}
