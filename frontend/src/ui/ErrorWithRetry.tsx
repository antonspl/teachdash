import { useTranslation } from 'react-i18next'
import { Button } from './Button'
import styles from './ErrorWithRetry.module.css'
import { StatusMessage } from './StatusMessage'

export function ErrorWithRetry({
  message,
  onRetry,
}: {
  message: string
  onRetry: () => void
}) {
  const { t } = useTranslation()
  return (
    <div className={styles.box}>
      <StatusMessage tone="danger">{message}</StatusMessage>
      <Button onClick={onRetry}>{t('ui.tryAgain')}</Button>
    </div>
  )
}
