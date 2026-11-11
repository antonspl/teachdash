import type { ReactNode } from 'react'
import styles from './Section.module.css'

// A titled block of a page, such as one form.
export function Section({
  title,
  children,
}: {
  title: string
  children?: ReactNode
}) {
  return (
    <section className={styles.section}>
      <h2 className={styles.heading}>{title}</h2>
      {children}
    </section>
  )
}
