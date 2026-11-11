import { useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthProvider'
import { cabinetPath } from '../auth/guards'
import { passwordProblem } from '../passwordRules'
import { Button } from '../ui/Button'
import { Form } from '../ui/Form'
import { Page } from '../ui/Page'
import { PasswordInput } from '../ui/PasswordInput'
import { StatusMessage } from '../ui/StatusMessage'
import { Title } from '../ui/Title'

// Read without side effects in a useState initialiser, so a repeated render or effect
// under React strict mode cannot lose it; the fragment is removed in a layout effect.
function readToken(): string | undefined {
  const token = new URLSearchParams(window.location.hash.slice(1)).get('token')
  return token ? token : undefined
}

function removeFragment() {
  if (window.location.hash) {
    window.history.replaceState(
      window.history.state,
      '',
      window.location.pathname + window.location.search,
    )
  }
}

export function SetPasswordPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { setPasswordWithLink } = useAuth()
  const [token] = useState(readToken)
  useLayoutEffect(removeFragment, [])
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (token === undefined || inFlight.current) {
      return
    }
    const problem = passwordProblem(password, confirm)
    if (problem) {
      setError(t(problem))
      return
    }
    inFlight.current = true
    setSubmitting(true)
    setError(undefined)
    try {
      const user = await setPasswordWithLink(token, password)
      void navigate(cabinetPath[user.role], { replace: true })
    } catch (failure) {
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
      <Title>{t('setPassword.title')}</Title>
      {token === undefined ? (
        <StatusMessage tone="danger">
          {t('setPassword.incompleteLink')}
        </StatusMessage>
      ) : (
        <Form onSubmit={(event) => void onSubmit(event)}>
          <PasswordInput
            label={t('setPassword.password')}
            autoComplete="new-password"
            required
            value={password}
            onChange={setPassword}
          />
          <PasswordInput
            label={t('setPassword.confirm')}
            autoComplete="new-password"
            required
            value={confirm}
            onChange={setConfirm}
          />
          {error ? <StatusMessage tone="danger">{error}</StatusMessage> : null}
          <Button type="submit" disabled={submitting}>
            {t('setPassword.submit')}
          </Button>
        </Form>
      )}
    </Page>
  )
}
