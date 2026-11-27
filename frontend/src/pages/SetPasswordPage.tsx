import { useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthProvider'
import { cabinetPath } from '../auth/guards'
import { passwordHints, passwordProblem } from '../passwordRules'
import { Button } from '../ui/Button'
import { Form } from '../ui/Form'
import { LanguageRow } from '../ui/LanguageRow'
import { Page } from '../ui/Page'
import { PasswordInput } from '../ui/PasswordInput'
import { StatusMessage } from '../ui/StatusMessage'
import { Title } from '../ui/Title'
import { useFieldErrors } from '../ui/useFieldErrors'

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
  const passwordInput = useRef<HTMLInputElement>(null)
  const confirmInput = useRef<HTMLInputElement>(null)
  // `token` comes from the link, not from a field: an error on it stays form-level.
  const fieldErrors = useFieldErrors([
    { key: 'password', backend: 'password', ref: passwordInput },
  ])
  const { errors } = fieldErrors

  const hints = passwordHints(password, confirm)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (token === undefined || inFlight.current) {
      return
    }
    setError(undefined)
    fieldErrors.clearAll()
    if (passwordProblem(password, confirm)) {
      // The hint is the message: focusing its field makes assistive technology read it.
      ;(hints.password ? passwordInput : confirmInput).current?.focus()
      return
    }
    inFlight.current = true
    setSubmitting(true)
    try {
      const user = await setPasswordWithLink(token, password)
      void navigate(cabinetPath[user.role], { replace: true })
    } catch (failure) {
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
      <LanguageRow />
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
            inputRef={passwordInput}
            error={
              (hints.password ? t(hints.password) : undefined) ??
              errors.password
            }
            value={password}
            onChange={(value) => {
              setPassword(value)
              setError(undefined)
              fieldErrors.clear('password')
            }}
          />
          <PasswordInput
            label={t('setPassword.confirm')}
            autoComplete="new-password"
            required
            inputRef={confirmInput}
            error={hints.repeat ? t(hints.repeat) : undefined}
            value={confirm}
            onChange={(value) => {
              setConfirm(value)
              setError(undefined)
            }}
          />
          {error ? <StatusMessage tone="danger">{error}</StatusMessage> : null}
          <Button type="submit" disabled={submitting} fullWidth>
            {t('setPassword.submit')}
          </Button>
        </Form>
      )}
    </Page>
  )
}
