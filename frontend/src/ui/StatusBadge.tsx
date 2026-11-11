import type { ReactNode } from 'react'
import type { StatusTone } from './StatusMessage'
import styles from './StatusBadge.module.css'

export function StatusBadge({
  tone,
  children,
}: {
  tone: StatusTone
  children: ReactNode
}) {
  return <span className={`${styles.badge} ${styles[tone]}`}>{children}</span>
}
