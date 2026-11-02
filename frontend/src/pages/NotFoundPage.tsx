import { useTranslation } from 'react-i18next'
import { Page } from '../ui/Page'
import { Text } from '../ui/Text'
import { TextLink } from '../ui/TextLink'
import { Title } from '../ui/Title'

export function NotFoundPage() {
  const { t } = useTranslation()

  return (
    <Page>
      <Title>{t('notFound.title')}</Title>
      <Text>{t('notFound.message')}</Text>
      <TextLink to="/">{t('notFound.home')}</TextLink>
    </Page>
  )
}
