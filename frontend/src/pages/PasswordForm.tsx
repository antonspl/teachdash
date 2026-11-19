import { useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../api/client'
import { changePassword } from '../api/profile'
import { passwordHints, passwordProblem } from '../passwordRules'
import { Button } from '../ui/Button'
import { ButtonRow } from '../ui/ButtonRow'
import { Form } from '../ui/Form'
import { PasswordInput } from '../ui/PasswordInput'
import { StatusMessage } from '../ui/StatusMessage'
import { useFieldErrors } from '../ui/useFieldErrors'

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
  // Changes on success: the new inputs start with the password hidden again.
  const [round, setRound] = useState(0)
  const inFlight = useRef(false)
  const currentInput = useRef<HTMLInputElement>(null)
  const nextInput = useRef<HTMLInputElement>(null)
  const repeatInput = useRef<HTMLInputElement>(null)

  const fieldErrors = useFieldErrors(
    [
      { key: 'current', backend: 'currentPassword', ref: currentInput },
      { key: 'next', backend: 'newPassword', ref: nextInput },
    ],
    { WRONG_PASSWORD: t('profile.wrongPassword') },
  )
  const { errors } = fieldErrors

  // Editing a field removes the form's own message and that field's backend error.
  function edit(
    key: 'current' | 'next' | 'repeat',
    set: (value: string) => void,
  ) {
    return (value: string) => {
      setMessage(undefined)
      if (key !== 'repeat') {
        fieldErrors.clear(key)
      }
      set(value)
    }
  }

  const hints = passwordHints(next, repeat)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (inFlight.current) {
      return
    }
    setMessage(undefined)
    fieldErrors.clearAll()
    const problem = passwordProblem(next, repeat)
    if (problem) {
      // The hint is the message: focusing its field makes assistive technology read it.
      ;(hints.password ? nextInput : repeatInput).current?.focus()
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
      setRound((current) => current + 1)
      setMessage({ tone: 'success', text: t('profile.passwordChanged') })
    } catch (failure) {
      if (failure instanceof ApiError && fieldErrors.place(failure)) {
        return
      }
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
        key={`currentPassword-${round}`}
        label={t('profile.currentPassword')}
        autoComplete="current-password"
        required
        disabled={submitting}
        inputRef={currentInput}
        error={errors.current}
        value={current}
        onChange={edit('current', setCurrent)}
      />
      <PasswordInput
        key={`newPassword-${round}`}
        label={t('profile.newPassword')}
        autoComplete="new-password"
        required
        disabled={submitting}
        inputRef={nextInput}
        error={(hints.password ? t(hints.password) : undefined) ?? errors.next}
        value={next}
        onChange={edit('next', setNext)}
      />
      <PasswordInput
        key={`repeatPassword-${round}`}
        label={t('profile.repeatPassword')}
        autoComplete="new-password"
        required
        disabled={submitting}
        inputRef={repeatInput}
        error={hints.repeat ? t(hints.repeat) : undefined}
        value={repeat}
        onChange={edit('repeat', setRepeat)}
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
