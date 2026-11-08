import { useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthProvider'
import { cabinetPath } from '../auth/guards'
import { Button } from '../ui/Button'
import { Form } from '../ui/Form'
import { Page } from '../ui/Page'
import { PasswordInput } from '../ui/PasswordInput'
import { StatusMessage } from '../ui/StatusMessage'
import { Title } from '../ui/Title'

const MIN_LENGTH = 8
const MAX_BYTES = 72

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

  function check(): string | undefined {
    if (password !== confirm) {
      return t('setPassword.mismatch')
    }
    if (password.length < MIN_LENGTH) {
      return t('setPassword.tooShort')
    }
    if (new TextEncoder().encode(password).length > MAX_BYTES) {
      return t('setPassword.tooLong')
    }
    return undefined
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (token === undefined || inFlight.current) {
      return
    }
    const problem = check()
    if (problem) {
      setError(problem)
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
