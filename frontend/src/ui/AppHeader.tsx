import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import styles from './AppHeader.module.css'
import { Button } from './Button'

// The button before "Log out" shows `navLabel` and goes to `navTo` on click.
export function AppHeader({
  userName,
  roleLabel,
  navLabel,
  navTo,
  onLogout,
}: {
  userName: string
  roleLabel: string
  navLabel: string
  navTo: string
  onLogout: () => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <header className={styles.header}>
      <p className={styles.name}>{t('app.name')}</p>
      <div className={styles.user}>
        <p className={styles.who}>
          {userName} ({roleLabel})
        </p>
        <Button variant="secondary" onClick={() => void navigate(navTo)}>
          {navLabel}
        </Button>
        <Button variant="secondary" onClick={onLogout}>
          {t('header.logout')}
        </Button>
      </div>
    </header>
  )
}
