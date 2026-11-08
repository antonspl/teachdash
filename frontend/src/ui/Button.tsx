import type { MouseEvent, ReactNode } from 'react'
import styles from './Button.module.css'
import { PlusIcon } from './PlusIcon'

export function Button({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  disabled,
  icon,
}: {
  children: ReactNode
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void
  type?: 'button' | 'submit'
  variant?: 'primary' | 'secondary' | 'danger' | 'success'
  disabled?: boolean
  icon?: 'plus'
}) {
  return (
    <button
      type={type}
      className={`${styles.button} ${styles[variant]}`}
      disabled={disabled}
      onClick={onClick}
    >
      {icon === 'plus' && <PlusIcon />}
      {children}
    </button>
  )
}
