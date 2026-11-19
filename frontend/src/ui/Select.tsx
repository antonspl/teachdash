import { useId, type Ref } from 'react'
import styles from './Field.module.css'

export function Select({
  label,
  value,
  onChange,
  options,
  labels,
  placeholder,
  error,
  required,
  disabled,
  inputRef,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: string[]
  // Shown instead of the option value, where given.
  labels?: Record<string, string>
  // An empty first option, so the select can start with nothing chosen.
  placeholder?: string
  error?: string
  required?: boolean
  disabled?: boolean
  // Lets a form move the focus to the select.
  inputRef?: Ref<HTMLSelectElement>
}) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className={styles.field}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>
      <div className={styles.selectWrap}>
        <select
          ref={inputRef}
          id={id}
          className={`${styles.input} ${styles.select} ${error ? styles.invalid : ''}`}
          value={value}
          required={required}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          onChange={(event) => onChange(event.target.value)}
        >
          {placeholder === undefined ? null : (
            <option value="">{placeholder}</option>
          )}
          {options.map((option) => (
            <option key={option} value={option}>
              {labels?.[option] ?? option}
            </option>
          ))}
        </select>
        <span className={styles.arrow} aria-hidden="true" data-select-arrow>
          <svg
            className={styles.arrowIcon}
            viewBox="0 0 12 12"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M1 3.5 6 8.5 11 3.5" />
          </svg>
        </span>
      </div>
      {error ? (
        <p id={errorId} className={styles.error}>
          {error}
        </p>
      ) : null}
    </div>
  )
}
