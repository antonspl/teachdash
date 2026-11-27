import { useId, type Ref } from 'react'
import styles from './Field.module.css'
import { FieldError } from './FieldError'
import { ChevDownIcon, icons, type IconName } from './icons'

export function Select({
  label,
  value,
  onChange,
  options,
  labels,
  placeholder,
  error,
  required,
  ariaRequired,
  disabled,
  inputRef,
  hideLabel,
  optionLang,
  flush,
  icon,
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
  // Announces the field as required without the native validation bubble.
  ariaRequired?: boolean
  disabled?: boolean
  // Lets a form move the focus to the select.
  inputRef?: Ref<HTMLSelectElement>
  // Keeps the label for assistive technology but hides it visually.
  hideLabel?: boolean
  // The `lang` of each option, by option value.
  optionLang?: Record<string, string>
  // Drops the bottom margin, for a select that sits among buttons.
  flush?: boolean
  // A decorative icon at the start of the select (the language select's globe).
  icon?: IconName
}) {
  const id = useId()
  const Icon = icon ? icons[icon] : null
  const errorId = `${id}-error`
  return (
    <div className={flush ? `${styles.field} ${styles.flush}` : styles.field}>
      <label
        htmlFor={id}
        className={`${styles.label} ${hideLabel ? styles.visuallyHidden : ''}`}
      >
        {label}
      </label>
      <div className={styles.selectWrap}>
        <select
          ref={inputRef}
          id={id}
          className={`${styles.input} ${styles.select} ${Icon ? styles.selectWithIcon : ''} ${error ? styles.invalid : ''}`}
          value={value}
          required={required}
          aria-required={ariaRequired ? true : undefined}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          onChange={(event) => onChange(event.target.value)}
        >
          {placeholder === undefined ? null : (
            <option value="" disabled hidden>
              {placeholder}
            </option>
          )}
          {options.map((option) => (
            <option key={option} value={option} lang={optionLang?.[option]}>
              {labels?.[option] ?? option}
            </option>
          ))}
        </select>
        <span className={styles.arrow} aria-hidden="true" data-select-arrow>
          <ChevDownIcon />
        </span>
        {Icon ? (
          <span
            className={styles.selectIcon}
            aria-hidden="true"
            data-select-icon
          >
            <Icon />
          </span>
        ) : null}
      </div>
      {error ? <FieldError id={errorId}>{error}</FieldError> : null}
    </div>
  )
}
