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
  justAdded,
  onDeactivate,
  onReactivate,
  onLink,
}: {
  teacher: Teacher
  busy: boolean
  justAdded: boolean
  onDeactivate: (opener: HTMLElement) => void
  onReactivate: () => void
  onLink: () => void
}) {
  const { t } = useTranslation()
  const status = teacherStatus(teacher)
  return (
    <ListItem
      title={teacher.name}
      badge={
        <>
          <StatusBadge tone={statusTone[status]}>
            {t(`adminTeachers.status.${status}`)}
          </StatusBadge>
          {justAdded ? (
            <StatusBadge tone="success">
              {t('adminTeachers.justAdded')}
            </StatusBadge>
          ) : null}
        </>
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
            <Button
              variant="danger"
              disabled={busy}
              onClick={(event) => onDeactivate(event.currentTarget)}
            >
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

function CreateDialog({
  opener,
  onCancel,
  onCreated,
}: {
  opener: HTMLElement
  onCancel: () => void
  onCreated: (created: { id: string; name: string; link: string }) => void
}) {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [timeZone, setTimeZone] = useState(defaultTimeZone)
  const [error, setError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)
  const mounted = useRef(true)
  const [zones] = useState(timeZoneOptions)

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (inFlight.current) {
      return
    }
    inFlight.current = true
    setSubmitting(true)
    setError(undefined)
    const submitted = { name, email, timeZone }
    try {
      const { id, link } = await createTeacher(submitted)
      onCreated({ id, name: submitted.name, link })
    } catch (failure) {
      if (!mounted.current) {
        return
      }
      if (failure instanceof ApiError && failure.code === 'VALIDATION_FAILED') {
        setError(t('adminTeachers.create.validationFailed'))
      } else {
        setError(
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
    <Dialog
      title={t('adminTeachers.create.title')}
      returnFocusTo={opener}
      onCancel={() => {
        if (!inFlight.current) {
          onCancel()
        }
      }}
    >
      <Form onSubmit={(event) => void onSubmit(event)}>
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
        {error ? <StatusMessage tone="danger">{error}</StatusMessage> : null}
        <Button variant="secondary" disabled={submitting} onClick={onCancel}>
          {t('adminTeachers.create.cancel')}
        </Button>
        <Button type="submit" variant="success" disabled={submitting}>
          {t('adminTeachers.create.submit')}
        </Button>
      </Form>
    </Dialog>
  )
}

export function AdminTeachersPage() {
  const { t } = useTranslation()
  const [teachers, setTeachers] = useState<Teacher[]>()
  const [loadError, setLoadError] = useState<string>()
  const [shownLink, setShownLink] = useState<ShownLink>()
  const [creating, setCreating] = useState<{ opener: HTMLElement }>()
  const [pinnedId, setPinnedId] = useState<string>()
  const [busyIds, setBusyIds] = useState<string[]>([])
  const [confirming, setConfirming] = useState<{
    id: string
    name: string
    opener: HTMLElement
  }>()
  const [actionError, setActionError] = useState<string>()
  const busyRef = useRef(new Set<string>())
  const mounted = useRef(false)
  const loadSeq = useRef(0)

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

  function onCreated(created: { id: string; name: string; link: string }) {
    if (!mounted.current) {
      return
    }
    setShownLink({ name: created.name, link: created.link })
    setPinnedId(created.id)
    setCreating(undefined)
    void load()
  }

  // The new teacher's row goes first until the page is reloaded.
  const shown = teachers
    ? [
        ...teachers.filter((item) => item.id === pinnedId),
        ...teachers.filter((item) => item.id !== pinnedId),
      ]
    : undefined

  return (
    <>
      <Button
        variant="success"
        onClick={(event) => setCreating({ opener: event.currentTarget })}
      >
        {t('adminTeachers.add')}
      </Button>
      {shownLink ? (
        <IssuedLink
          key={shownLink.link}
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
      {shown === undefined ? (
        loadError ? null : (
          <Text>{t('ui.loading')}</Text>
        )
      ) : shown.length === 0 ? (
        <Text>{t('adminTeachers.empty')}</Text>
      ) : (
        <List>
          {shown.map((teacher) => (
            <TeacherRow
              key={teacher.id}
              teacher={teacher}
              busy={busyIds.includes(teacher.id)}
              justAdded={teacher.id === pinnedId}
              onDeactivate={(opener) =>
                setConfirming({ id: teacher.id, name: teacher.name, opener })
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
          returnFocusTo={confirming.opener}
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
      {creating ? (
        <CreateDialog
          opener={creating.opener}
          onCancel={() => setCreating(undefined)}
          onCreated={onCreated}
        />
      ) : null}
    </>
  )
}
