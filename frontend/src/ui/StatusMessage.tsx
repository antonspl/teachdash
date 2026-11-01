import type { ReactNode } from 'react'
import styles from './StatusMessage.module.css'

export type StatusTone = 'neutral' | 'success' | 'danger'

export function StatusMessage({
  tone,
  children,
}: {
  tone: StatusTone
  children: ReactNode
}) {
  return (
    <p role="status" className={`${styles.status} ${styles[tone]}`}>
      {children}
    </p>
  )
}
