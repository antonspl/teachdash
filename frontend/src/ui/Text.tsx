import type { ReactNode } from 'react'
import styles from './Text.module.css'

// `wrap` lets a long word without spaces, such as an email, break across lines.
export function Text({
  children,
  wrap,
}: {
  children: ReactNode
  wrap?: boolean
}) {
  return (
    <p className={wrap ? `${styles.text} ${styles.wrap}` : styles.text}>
      {children}
    </p>
  )
}
