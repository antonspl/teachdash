import type { FormEventHandler, ReactNode } from 'react'
import styles from './Form.module.css'

export function Form({
  onSubmit,
  noValidate,
  children,
}: {
  onSubmit: FormEventHandler<HTMLFormElement>
  // The form checks its fields itself, so the native bubble does not hide other errors.
  noValidate?: boolean
  children: ReactNode
}) {
  return (
    <form className={styles.form} onSubmit={onSubmit} noValidate={noValidate}>
      {children}
    </form>
  )
}
