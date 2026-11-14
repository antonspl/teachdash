import { useId } from 'react'
import styles from './Field.module.css'

export function Select({
  label,
  value,
  onChange,
  options,
  required,
  disabled,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: string[]
  required?: boolean
  disabled?: boolean
}) {
  const id = useId()
  return (
    <div className={styles.field}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>
      <div className={styles.selectWrap}>
        <select
          id={id}
          className={`${styles.input} ${styles.select}`}
          value={value}
          required={required}
          disabled={disabled}
          onChange={(event) => onChange(event.target.value)}
        >
          {options.map((option) => (
            <option key={option} value={option}>
              {option}
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
    </div>
  )
}
