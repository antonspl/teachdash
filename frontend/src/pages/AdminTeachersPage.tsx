import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  createTeacher,
  deactivateTeacher,
  issueResetLink,
  listTeachers,
  reactivateTeacher,
  teacherStatus,
  type Teacher,
  type TeacherStatus,
} from '../api/adminTeachers'
import { ApiError } from '../api/client'
import { timeZoneOptions, defaultTimeZone } from '../timeZones'
import { Button } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
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

function TeacherRow({
  teacher,
  busy,
  onDeactivate,
  onReactivate,
  onLink,
}: {
  teacher: Teacher
  busy: boolean
  onDeactivate: () => void
  onReactivate: () => void
  onLink: () => void
}) {
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
      actions={
        teacher.active ? (
          <>
            <Button variant="secondary" disabled={busy} onClick={onLink}>
              {teacher.hasPassword
                ? t('adminTeachers.actions.resetLink')
                : t('adminTeachers.actions.inviteLink')}
            </Button>
            <Button variant="danger" disabled={busy} onClick={onDeactivate}>
              {t('adminTeachers.actions.deactivate')}
            </Button>
          </>
        ) : (
          <Button variant="secondary" disabled={busy} onClick={onReactivate}>
            {t('adminTeachers.actions.reactivate')}
          </Button>
        )
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
  const [busyIds, setBusyIds] = useState<string[]>([])
  const [confirming, setConfirming] = useState<{ id: string; name: string }>()
  const [actionError, setActionError] = useState<string>()
  const busyRef = useRef(new Set<string>())
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

  // Runs one request for a row: one busy flag per row, reload afterwards either way.
  async function runRowAction(
    teacher: { id: string; name: string },
    action: () => Promise<string | null | void>,
  ) {
    if (busyRef.current.has(teacher.id)) {
      return
    }
    busyRef.current.add(teacher.id)
    setBusyIds([...busyRef.current])
    setActionError(undefined)
    try {
      const link = await action()
      if (mounted.current && link) {
        setShownLink({ name: teacher.name, link })
      }
    } catch (failure) {
      if (mounted.current) {
        setActionError(
          failure instanceof ApiError ? failure.message : t('errors.generic'),
        )
      }
    } finally {
      busyRef.current.delete(teacher.id)
      if (mounted.current) {
        setBusyIds([...busyRef.current])
        void load()
      }
    }
  }

  async function onConfirmDeactivate() {
    if (!confirming) {
      return
    }
    const target = confirming
    await runRowAction(target, () => deactivateTeacher(target.id))
    if (mounted.current) {
      setConfirming(undefined)
    }
  }

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
      {actionError ? (
        <StatusMessage tone="danger">{actionError}</StatusMessage>
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
            <TeacherRow
              key={teacher.id}
              teacher={teacher}
              busy={busyIds.includes(teacher.id)}
              onDeactivate={() =>
                setConfirming({ id: teacher.id, name: teacher.name })
              }
              onReactivate={() =>
                void runRowAction(teacher, () => reactivateTeacher(teacher.id))
              }
              onLink={() =>
                void runRowAction(teacher, () => issueResetLink(teacher.id))
              }
            />
          ))}
        </List>
      )}
      {confirming ? (
        <Dialog
          title={t('adminTeachers.deactivate.title', { name: confirming.name })}
          onCancel={() => {
            if (!busyIds.includes(confirming.id)) {
              setConfirming(undefined)
            }
          }}
        >
          <Text>
            {t('adminTeachers.deactivate.body', { name: confirming.name })}
          </Text>
          <Button
            variant="secondary"
            disabled={busyIds.includes(confirming.id)}
            onClick={() => setConfirming(undefined)}
          >
            {t('adminTeachers.deactivate.cancel')}
          </Button>
          <Button
            variant="danger"
            disabled={busyIds.includes(confirming.id)}
            onClick={() => void onConfirmDeactivate()}
          >
            {t('adminTeachers.deactivate.confirm')}
          </Button>
        </Dialog>
      ) : null}
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
