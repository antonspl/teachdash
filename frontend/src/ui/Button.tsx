import type { MouseEvent, ReactNode, Ref } from 'react'
import styles from './Button.module.css'
import { icons, type IconName } from './icons'

export function Button({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  disabled,
  busy,
  busyLabel,
  icon,
  fullWidth,
  ariaLabel,
  ariaPressed,
  ref,
}: {
  children: ReactNode
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void
  type?: 'button' | 'submit'
  variant?:
    'primary' | 'secondary' | 'quiet' | 'dangerText' | 'muted' | 'danger'
  disabled?: boolean
  // A request is in flight: native disabled, aria-busy and the busy label in place
  // of the idle one. Both labels stay in one grid cell, so the width never changes.
  busy?: boolean
  busyLabel?: string
  icon?: IconName
  fullWidth?: boolean
  ariaLabel?: string
  ariaPressed?: boolean
  ref?: Ref<HTMLButtonElement>
}) {
  const Icon = icon ? icons[icon] : null
  const classes = [styles.button, styles[variant]]
  if (Icon) classes.push(styles.withIcon)
  if (fullWidth) classes.push(styles.fullWidth)
  return (
    <button
      ref={ref}
      type={type}
      className={classes.join(' ')}
      disabled={disabled || busy}
      aria-busy={busy ? true : undefined}
      aria-label={ariaLabel}
      aria-pressed={ariaPressed}
      onClick={onClick}
    >
      {Icon && <Icon />}
      {busyLabel === undefined ? (
        children
      ) : (
        <span className={styles.labels}>
          <span
            className={busy ? styles.inactive : styles.label}
            aria-hidden={busy ? true : undefined}
          >
            {children}
          </span>
          <span
            className={busy ? styles.label : styles.inactive}
            aria-hidden={busy ? undefined : true}
          >
            {busyLabel}
          </span>
        </span>
      )}
    </button>
  )
}
