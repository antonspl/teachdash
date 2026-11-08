import type { FormEventHandler, ReactNode } from 'react'
import styles from './Form.module.css'

export function Form({
  onSubmit,
  children,
}: {
  onSubmit: FormEventHandler<HTMLFormElement>
  children: ReactNode
}) {
  return (
    <form className={styles.form} onSubmit={onSubmit}>
      {children}
    </form>
  )
}
