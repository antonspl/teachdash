import { useMemo, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../api/client'
import { useAuth, type User } from '../auth/AuthProvider'
import { Button } from '../ui/Button'
import { ButtonRow } from '../ui/ButtonRow'
import { Form } from '../ui/Form'
import { Select } from '../ui/Select'
import { StatusMessage } from '../ui/StatusMessage'
import { TextInput } from '../ui/TextInput'
import { timeZoneOptions } from '../timeZones'

type Message = { tone: 'success' | 'danger'; text: string }

export function ProfileForm({
  user,
  disabled,
  onSendingChange,
}: {
  user: User
  // The button is disabled from outside while the other form is sending.
  disabled?: boolean
  onSendingChange?: (sending: boolean) => void
}) {
  const { t } = useTranslation()
  const { updateProfile } = useAuth()
  const [name, setName] = useState(user.name)
  const [timeZone, setTimeZone] = useState(user.timeZone)
  const [message, setMessage] = useState<Message>()
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)
  const zones = useMemo(() => timeZoneOptions(user.timeZone), [user.timeZone])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (inFlight.current) {
      return
    }
    inFlight.current = true
    setSubmitting(true)
    onSendingChange?.(true)
    setMessage(undefined)
    try {
      const saved = await updateProfile(name, timeZone)
      setName(saved.name)
      setTimeZone(saved.timeZone)
      setMessage({ tone: 'success', text: t('profile.saved') })
    } catch (failure) {
      if (failure instanceof ApiError && failure.code === 'VALIDATION_FAILED') {
        setMessage({ tone: 'danger', text: t('profile.validationFailed') })
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
      <TextInput
        label={t('profile.name')}
        autoComplete="name"
        required
        disabled={submitting}
        value={name}
        onChange={(value) => {
          setMessage(undefined)
          setName(value)
        }}
      />
      <Select
        label={t('profile.timeZone')}
        required
        disabled={submitting}
        value={timeZone}
        options={zones}
        onChange={(value) => {
          setMessage(undefined)
          setTimeZone(value)
        }}
      />
      {message ? (
        <StatusMessage tone={message.tone}>{message.text}</StatusMessage>
      ) : null}
      <ButtonRow>
        <Button type="submit" disabled={submitting || disabled}>
          {t('profile.save')}
        </Button>
      </ButtonRow>
    </Form>
  )
}
