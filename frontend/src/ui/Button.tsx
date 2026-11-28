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
  // A request is in flight: native disabled, aria-busy and the busy label. Idle, only
  // the idle label is in the DOM; busy, both share one grid cell (the idle one hidden),
  // so the button never gets narrower and grows only when the busy label is longer.
  busy?: boolean
  busyLabel?: string
  icon?: IconName
  fullWidth?: boolean
  ariaLabel?: string
  ariaPressed?: boolean
  ref?: Ref<HTMLButtonElement>
}) {
  const Icon = icon ? icons[icon] : null
  const showBusyLabel = busy && busyLabel !== undefined
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
      aria-label={showBusyLabel ? undefined : ariaLabel}
      aria-pressed={ariaPressed}
      onClick={onClick}
    >
      {Icon && <Icon />}
      {showBusyLabel ? (
        <span className={styles.labels}>
          <span className={styles.inactive} aria-hidden="true">
            {children}
          </span>
          <span className={styles.label}>{busyLabel}</span>
        </span>
      ) : (
        children
      )}
    </button>
  )
}
