import { useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../api/client'
import { changePassword } from '../api/profile'
import { passwordProblem } from '../passwordRules'
import { Button } from '../ui/Button'
import { ButtonRow } from '../ui/ButtonRow'
import { Form } from '../ui/Form'
import { PasswordInput } from '../ui/PasswordInput'
import { StatusMessage } from '../ui/StatusMessage'

type Message = { tone: 'success' | 'danger'; text: string }

export function PasswordForm({
  disabled,
  onSendingChange,
}: {
  // The button is disabled from outside while the other form is sending.
  disabled?: boolean
  onSendingChange?: (sending: boolean) => void
}) {
  const { t } = useTranslation()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [repeat, setRepeat] = useState('')
  const [message, setMessage] = useState<Message>()
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)

  // Editing a field removes the form's own message.
  function edit(set: (value: string) => void) {
    return (value: string) => {
      setMessage(undefined)
      set(value)
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (inFlight.current) {
      return
    }
    setMessage(undefined)
    const problem = passwordProblem(next, repeat)
    if (problem) {
      setMessage({ tone: 'danger', text: t(problem) })
      return
    }
    inFlight.current = true
    setSubmitting(true)
    onSendingChange?.(true)
    try {
      await changePassword({ currentPassword: current, newPassword: next })
      setCurrent('')
      setNext('')
      setRepeat('')
      setMessage({ tone: 'success', text: t('profile.passwordChanged') })
    } catch (failure) {
      if (failure instanceof ApiError && failure.code === 'WRONG_PASSWORD') {
        setMessage({ tone: 'danger', text: t('profile.wrongPassword') })
      } else if (
        failure instanceof ApiError &&
        failure.code === 'VALIDATION_FAILED'
      ) {
        setMessage({
          tone: 'danger',
          text: t('profile.passwordValidationFailed'),
        })
      } else {
        setMessage({
          tone: 'danger',
          text:
            failure instanceof ApiError ? failure.message : t('errors.generic'),
        })
      }
    } finally {
      inFlight.current = false
      setSubmitting(false)
      onSendingChange?.(false)
    }
  }

  return (
    <Form onSubmit={(event) => void onSubmit(event)}>
      <PasswordInput
        label={t('profile.currentPassword')}
        autoComplete="current-password"
        required
        disabled={submitting}
        value={current}
        onChange={edit(setCurrent)}
      />
      <PasswordInput
        label={t('profile.newPassword')}
        autoComplete="new-password"
        required
        disabled={submitting}
        value={next}
        onChange={edit(setNext)}
      />
      <PasswordInput
        label={t('profile.repeatPassword')}
        autoComplete="new-password"
        required
        disabled={submitting}
        value={repeat}
        onChange={edit(setRepeat)}
      />
      {message ? (
        <StatusMessage tone={message.tone}>{message.text}</StatusMessage>
      ) : null}
      <ButtonRow>
        <Button type="submit" disabled={submitting || disabled}>
          {t('profile.changePassword')}
        </Button>
      </ButtonRow>
    </Form>
  )
}
