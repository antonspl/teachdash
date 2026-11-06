import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate } from 'react-router'
import { ErrorWithRetry } from '../ui/ErrorWithRetry'
import { Page } from '../ui/Page'
import { StatusMessage } from '../ui/StatusMessage'
import { useAuth, type Role } from './AuthProvider'

export const cabinetPath: Record<Role, string> = {
  ADMIN: '/admin',
  TEACHER: '/teacher',
  STUDENT: '/student',
}

function Loading() {
  const { t } = useTranslation()
  return (
    <Page>
      <StatusMessage tone="neutral">{t('ui.loading')}</StatusMessage>
    </Page>
  )
}

function StartupError() {
  const { t } = useTranslation()
  const { retry } = useAuth()
  return (
    <Page>
      <ErrorWithRetry message={t('startup.error')} onRetry={retry} />
    </Page>
  )
}

export function RequireRole({
  role,
  children,
}: {
  role: Role
  children: ReactNode
}) {
  const { state } = useAuth()
  if (state.status === 'loading') {
    return <Loading />
  }
  if (state.status === 'error') {
    return <StartupError />
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/login" replace />
  }
  if (state.user.role !== role) {
    return <Navigate to={cabinetPath[state.user.role]} replace />
  }
  return <>{children}</>
}

// Redirects a logged-in user away from the page, but only when the page was
// opened by that user. A login submitted on the page is left to the page, which
// decides where to go (design.md, "Who decides where to go after login").
export function AnonymousOnly({ children }: { children: ReactNode }) {
  const { state } = useAuth()
  const [sawAnonymous, setSawAnonymous] = useState(false)

  if (state.status === 'anonymous' && !sawAnonymous) {
    setSawAnonymous(true)
  }

  if (state.status === 'loading') {
    return <Loading />
  }
  if (state.status === 'error') {
    return <StartupError />
  }
  if (state.status === 'user' && !sawAnonymous) {
    return <Navigate to={cabinetPath[state.user.role]} replace />
  }
  return <>{children}</>
}

export function HomeRedirect() {
  const { state } = useAuth()
  if (state.status === 'loading') {
    return <Loading />
  }
  if (state.status === 'error') {
    return <StartupError />
  }
  if (state.status === 'anonymous') {
    return <Navigate to="/login" replace />
  }
  return <Navigate to={cabinetPath[state.user.role]} replace />
}
