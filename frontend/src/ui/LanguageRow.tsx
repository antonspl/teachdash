import { UiLanguageSelect } from './UiLanguageSelect'
import styles from './LanguageRow.module.css'

// The first row of the login and set-password pages, outside the form.
export function LanguageRow() {
  return (
    <div className={styles.row}>
      <UiLanguageSelect />
    </div>
  )
}
