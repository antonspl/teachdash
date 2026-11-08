import styles from './PlusIcon.module.css'

export function PlusIcon() {
  return (
    <svg
      className={styles.icon}
      viewBox="0 0 16 16"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M8 2.5v11M2.5 8h11"
        fill="none"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
  )
}
