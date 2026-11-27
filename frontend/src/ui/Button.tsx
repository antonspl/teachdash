import type { MouseEvent, ReactNode, Ref } from 'react'
import styles from './Button.module.css'
import { icons, type IconName } from './icons'

export function Button({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  disabled,
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
      disabled={disabled}
      aria-label={ariaLabel}
      aria-pressed={ariaPressed}
      onClick={onClick}
    >
      {Icon && <Icon />}
      {children}
    </button>
  )
}
