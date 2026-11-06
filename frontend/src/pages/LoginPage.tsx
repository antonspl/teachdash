import { useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useReachability } from '../api/useReachability'
import { useAuth } from '../auth/AuthProvider'
import { cabinetPath } from '../auth/guards'
import { Button } from '../ui/Button'
import { Form } from '../ui/Form'
import { Page } from '../ui/Page'
import { PasswordInput } from '../ui/PasswordInput'
import { StatusMessage } from '../ui/StatusMessage'
import { Text } from '../ui/Text'
import { TextInput } from '../ui/TextInput'
import { Title } from '../ui/Title'

// The remembered page is used only when it belongs to the role's cabinet.
function belongsTo(path: string, cabinet: string): boolean {
  const pathname = path.split(/[?#]/)[0]
  return pathname === cabinet || pathname.startsWith(`${cabinet}/`)
}

export function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { state, login } = useAuth()
  const reachability = useReachability()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)

  const remembered =
    state.status === 'anonymous' ? state.sessionEnded?.path : undefined

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (inFlight.current) {
      return
    }
    inFlight.current = true
    setSubmitting(true)
    setError(undefined)
    try {
      const user = await login(email, password)
      const cabinet = cabinetPath[user.role]
      void navigate(
        remembered && belongsTo(remembered, cabinet) ? remembered : cabinet,
        { replace: true },
      )
    } catch (failure) {
      setPassword('')
      setError(
        failure instanceof ApiError ? failure.message : t('errors.generic'),
      )
    } finally {
      inFlight.current = false
      setSubmitting(false)
    }
  }

  return (
    <Page>
      <Title>{t('login.title')}</Title>
      {reachability === 'unreachable' ? (
        <StatusMessage tone="danger">{t('login.unreachable')}</StatusMessage>
      ) : null}
      {remembered !== undefined ? (
        <StatusMessage tone="neutral">{t('login.sessionEnded')}</StatusMessage>
      ) : null}
      <Form onSubmit={(event) => void onSubmit(event)}>
        <TextInput
          label={t('login.email')}
          type="email"
          autoComplete="username"
          required
          value={email}
          onChange={setEmail}
        />
        <PasswordInput
          label={t('login.password')}
          autoComplete="current-password"
          required
          value={password}
          onChange={setPassword}
        />
        {error ? <StatusMessage tone="danger">{error}</StatusMessage> : null}
        <Button type="submit" disabled={submitting}>
          {t('login.submit')}
        </Button>
      </Form>
      <Text>{t('login.linkHint')}</Text>
    </Page>
  )
}
