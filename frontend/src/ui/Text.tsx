import type { ReactNode } from 'react'
import styles from './Text.module.css'

export function Text({ children }: { children: ReactNode }) {
  return <p className={styles.text}>{children}</p>
}
