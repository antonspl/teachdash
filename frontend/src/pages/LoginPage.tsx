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
import { useFieldErrors } from '../ui/useFieldErrors'

// The remembered page is used only when it belongs to the role's cabinet, or is the
// profile page, which every role has (a trailing slash is ignored there).
function belongsTo(path: string, cabinet: string): boolean {
  const pathname = path.split(/[?#]/)[0]
  return (
    pathname === cabinet ||
    pathname.startsWith(`${cabinet}/`) ||
    pathname.replace(/\/+$/, '') === '/profile'
  )
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
  const emailRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const fieldErrors = useFieldErrors([
    { key: 'email', backend: 'email', ref: emailRef },
    { key: 'password', backend: 'password', ref: passwordRef },
  ])
  const { errors } = fieldErrors

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
    fieldErrors.clearAll()
    try {
      const user = await login(email, password)
      const cabinet = cabinetPath[user.role]
      void navigate(
        remembered && belongsTo(remembered, cabinet) ? remembered : cabinet,
        { replace: true },
      )
    } catch (failure) {
      setPassword('')
      if (failure instanceof ApiError && fieldErrors.place(failure)) {
        return
      }
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
          inputRef={emailRef}
          error={errors.email}
          value={email}
          onChange={(value) => {
            setEmail(value)
            fieldErrors.clear('email')
          }}
        />
        <PasswordInput
          label={t('login.password')}
          autoComplete="current-password"
          required
          inputRef={passwordRef}
          error={errors.password}
          value={password}
          onChange={(value) => {
            setPassword(value)
            fieldErrors.clear('password')
          }}
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
