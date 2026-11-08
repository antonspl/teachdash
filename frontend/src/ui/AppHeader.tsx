import { useTranslation } from 'react-i18next'
import styles from './AppHeader.module.css'
import { Button } from './Button'

export function AppHeader({
  userName,
  roleLabel,
  onLogout,
}: {
  userName: string
  roleLabel: string
  onLogout: () => void
}) {
  const { t } = useTranslation()
  return (
    <header className={styles.header}>
      <p className={styles.name}>{t('app.name')}</p>
      <div className={styles.user}>
        <p className={styles.who}>
          {userName} ({roleLabel})
        </p>
        <Button variant="secondary" onClick={onLogout}>
          {t('header.logout')}
        </Button>
      </div>
    </header>
  )
}
