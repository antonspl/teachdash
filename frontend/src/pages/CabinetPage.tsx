import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth, type Role } from '../auth/AuthProvider'
import { AppHeader } from '../ui/AppHeader'
import { Page } from '../ui/Page'
import { StatusMessage } from '../ui/StatusMessage'
import { Title } from '../ui/Title'

const titleKey = {
  ADMIN: 'cabinet.admin',
  TEACHER: 'cabinet.teacher',
  STUDENT: 'cabinet.student',
} as const

export function CabinetPage({ role }: { role: Role }) {
  const { t } = useTranslation()
  const { state, logout } = useAuth()
  const [logoutFailed, setLogoutFailed] = useState(false)
  if (state.status !== 'user') {
    return null
  }
  return (
    <>
      <AppHeader
        userName={state.user.name}
        roleLabel={t(`roles.${state.user.role}`)}
        onLogout={() => {
          setLogoutFailed(false)
          logout().catch(() => setLogoutFailed(true))
        }}
      />
      <Page>
        <Title>{t(titleKey[role])}</Title>
        {logoutFailed ? (
          <StatusMessage tone="danger">{t('header.logoutError')}</StatusMessage>
        ) : null}
      </Page>
    </>
  )
}
