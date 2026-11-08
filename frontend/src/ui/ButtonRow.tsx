import type { ReactNode } from 'react'
import styles from './ButtonRow.module.css'

// A group of buttons with a gap between them; wraps on narrow screens.
export function ButtonRow({ children }: { children: ReactNode }) {
  return <div className={styles.row}>{children}</div>
}
