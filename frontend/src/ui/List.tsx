import type { ReactNode } from 'react'
import styles from './List.module.css'

export function List({ children }: { children: ReactNode }) {
  return <ul className={styles.list}>{children}</ul>
}

// A card: a title line with an optional badge, detail lines and actions.
export function ListItem({
  title,
  badge,
  details,
  actions,
  extra,
}: {
  title: ReactNode
  badge?: ReactNode
  details?: ReactNode
  actions?: ReactNode
  extra?: ReactNode
}) {
  return (
    <li className={styles.item}>
      <div className={styles.head}>
        <strong className={styles.title}>{title}</strong>
        {badge}
      </div>
      {details ? <div className={styles.details}>{details}</div> : null}
      {actions}
      {extra ? <div className={styles.extra}>{extra}</div> : null}
    </li>
  )
}
