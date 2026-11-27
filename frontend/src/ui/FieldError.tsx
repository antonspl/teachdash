import type { ReactNode } from 'react'
import styles from './Field.module.css'
import { AlertIcon } from './icons'

// The message under a field: the alert icon on the first line, the text beside it.
// The text sits directly in the paragraph, so the description is the message alone.
export function FieldError({
  id,
  children,
}: {
  id: string
  children: ReactNode
}) {
  return (
    <p id={id} className={styles.error}>
      <AlertIcon size="small" />
      {children}
    </p>
  )
}
