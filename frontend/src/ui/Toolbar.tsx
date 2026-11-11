import type { ReactNode } from 'react'
import styles from './Toolbar.module.css'

// A bar right under the header; its content sits on the header's right edge.
export function Toolbar({ children }: { children: ReactNode }) {
  return <div className={styles.toolbar}>{children}</div>
}
