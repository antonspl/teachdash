import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthProvider'
import { cabinetPath } from '../auth/guards'
import { PasswordForm } from './PasswordForm'
import { ProfileForm } from './ProfileForm'
import { AppHeader } from '../ui/AppHeader'
import { Page } from '../ui/Page'
import { Section } from '../ui/Section'
import { StatusMessage } from '../ui/StatusMessage'
import { Text } from '../ui/Text'
import { Title } from '../ui/Title'

export function ProfilePage() {
  const { t } = useTranslation()
  const { state, logout } = useAuth()
  const [logoutFailed, setLogoutFailed] = useState(false)
  // Which form is sending a request; the other form's button waits (R3-S11).
  const [sending, setSending] = useState<'profile' | 'password'>()
  if (state.status !== 'user') {
    return null
  }
  const { user } = state
  return (
    <>
      <AppHeader
        userName={user.name}
        roleLabel={t(`roles.${user.role}`)}
        navLabel={t('header.backToCabinet')}
        navTo={cabinetPath[user.role]}
        onLogout={() => {
          setLogoutFailed(false)
          logout().catch(() => setLogoutFailed(true))
        }}
      />
      <Page>
        <Title>{t('profile.title')}</Title>
        {logoutFailed ? (
          <StatusMessage tone="danger">{t('header.logoutError')}</StatusMessage>
        ) : null}
        <Text wrap>{t('profile.email', { email: user.email })}</Text>
        <Section title={t('profile.profileHeading')}>
          <ProfileForm
            user={user}
            disabled={sending === 'password'}
            onSendingChange={(value) =>
              setSending(value ? 'profile' : undefined)
            }
          />
        </Section>
        <Section title={t('profile.passwordHeading')}>
          <PasswordForm
            disabled={sending === 'profile'}
            onSendingChange={(value) =>
              setSending(value ? 'password' : undefined)
            }
          />
        </Section>
      </Page>
    </>
  )
}
