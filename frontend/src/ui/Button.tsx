import type { MouseEvent, ReactNode, Ref } from 'react'
import styles from './Button.module.css'
import { PlusIcon } from './PlusIcon'

export function Button({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  disabled,
  icon,
  ref,
}: {
  children: ReactNode
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void
  type?: 'button' | 'submit'
  variant?: 'primary' | 'secondary' | 'danger' | 'success'
  disabled?: boolean
  icon?: 'plus'
  ref?: Ref<HTMLButtonElement>
}) {
  return (
    <button
      ref={ref}
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
