import { useTranslation } from 'react-i18next'
import { useReachability, type Reachability } from '../api/useReachability'
import { Page } from '../ui/Page'
import { StatusMessage, type StatusTone } from '../ui/StatusMessage'
import { Title } from '../ui/Title'

const TONES: Record<Reachability, StatusTone> = {
  checking: 'neutral',
  reachable: 'success',
  unreachable: 'danger',
}

export function PlaceholderPage() {
  const { t } = useTranslation()
  const status = useReachability()

  return (
    <Page>
      <Title>{t('app.name')}</Title>
      <StatusMessage tone={TONES[status]}>
        {t(`status.${status}`)}
      </StatusMessage>
    </Page>
  )
}
