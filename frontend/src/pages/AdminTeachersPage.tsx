import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  createTeacher,
  listTeachers,
  teacherStatus,
  type Teacher,
  type TeacherStatus,
} from '../api/adminTeachers'
import { ApiError } from '../api/client'
import { timeZoneOptions, defaultTimeZone } from '../timeZones'
import { Button } from '../ui/Button'
import { ErrorWithRetry } from '../ui/ErrorWithRetry'
import { Form } from '../ui/Form'
import { IssuedLink } from '../ui/IssuedLink'
import { List, ListItem } from '../ui/List'
import { Select } from '../ui/Select'
import { StatusBadge } from '../ui/StatusBadge'
import { StatusMessage, type StatusTone } from '../ui/StatusMessage'
import { Text } from '../ui/Text'
import { TextInput } from '../ui/TextInput'

const statusTone: Record<TeacherStatus, StatusTone> = {
  ACTIVE: 'success',
  WAITING: 'neutral',
  DEACTIVATED: 'danger',
}

type ShownLink = { name: string; link: string }

function TeacherRow({ teacher }: { teacher: Teacher }) {
  const { t } = useTranslation()
  const status = teacherStatus(teacher)
  return (
    <ListItem
      title={teacher.name}
      badge={
        <StatusBadge tone={statusTone[status]}>
          {t(`adminTeachers.status.${status}`)}
        </StatusBadge>
      }
      details={
        <>
          <Text>{teacher.email}</Text>
          <Text>{teacher.timeZone}</Text>
        </>
      }
    />
  )
}

export function AdminTeachersPage() {
  const { t } = useTranslation()
  const [teachers, setTeachers] = useState<Teacher[]>()
  const [loadError, setLoadError] = useState<string>()
  const [shownLink, setShownLink] = useState<ShownLink>()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [timeZone, setTimeZone] = useState(defaultTimeZone)
  const [createError, setCreateError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)
  const mounted = useRef(false)
  const loadSeq = useRef(0)
  const [zones] = useState(timeZoneOptions)

  // Only the newest load may change the state, and none after unmount.
  const load = useCallback(async () => {
    const seq = ++loadSeq.current
    setLoadError(undefined)
    try {
      const list = await listTeachers()
      if (mounted.current && seq === loadSeq.current) {
        setTeachers(list)
      }
    } catch (failure) {
      if (mounted.current && seq === loadSeq.current) {
        setLoadError(
          failure instanceof ApiError ? failure.message : t('errors.generic'),
        )
      }
    }
  }, [t])

  useEffect(() => {
    mounted.current = true
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch
    void load()
    return () => {
      mounted.current = false
    }
  }, [load])

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    if (inFlight.current) {
      return
    }
    inFlight.current = true
    setSubmitting(true)
    setCreateError(undefined)
    const submitted = { name, email, timeZone }
    try {
      const link = await createTeacher(submitted)
      if (!mounted.current) {
        return
      }
      setShownLink({ name: submitted.name, link })
      setName('')
      setEmail('')
      setTimeZone(defaultTimeZone())
      void load()
    } catch (failure) {
      if (!mounted.current) {
        return
      }
      if (failure instanceof ApiError && failure.code === 'VALIDATION_FAILED') {
        setCreateError(t('adminTeachers.create.validationFailed'))
      } else {
        setCreateError(
          failure instanceof ApiError ? failure.message : t('errors.generic'),
        )
      }
    } finally {
      inFlight.current = false
      if (mounted.current) {
        setSubmitting(false)
      }
    }
  }

  return (
    <>
      {shownLink ? (
        <IssuedLink
          name={shownLink.name}
          link={shownLink.link}
          onClose={() => setShownLink(undefined)}
        />
      ) : null}
      {loadError ? (
        <ErrorWithRetry message={loadError} onRetry={() => void load()} />
      ) : null}
      {teachers === undefined ? (
        loadError ? null : (
          <Text>{t('ui.loading')}</Text>
        )
      ) : teachers.length === 0 ? (
        <Text>{t('adminTeachers.empty')}</Text>
      ) : (
        <List>
          {teachers.map((teacher) => (
            <TeacherRow key={teacher.id} teacher={teacher} />
          ))}
        </List>
      )}
      <Form onSubmit={(event) => void onCreate(event)}>
        <TextInput
          label={t('adminTeachers.create.name')}
          required
          value={name}
          onChange={setName}
        />
        <TextInput
          label={t('adminTeachers.create.email')}
          type="email"
          required
          value={email}
          onChange={setEmail}
        />
        <Select
          label={t('adminTeachers.create.timeZone')}
          required
          value={timeZone}
          onChange={setTimeZone}
          options={zones}
        />
        {createError ? (
          <StatusMessage tone="danger">{createError}</StatusMessage>
        ) : null}
        <Button type="submit" disabled={submitting}>
          {t('adminTeachers.create.submit')}
        </Button>
      </Form>
    </>
  )
}
