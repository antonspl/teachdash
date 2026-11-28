import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from 'react'
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
import { ButtonRow } from '../ui/ButtonRow'
import { CabinetPage } from './CabinetPage'
import { Dialog } from '../ui/Dialog'
import { ErrorWithRetry } from '../ui/ErrorWithRetry'
import { Form } from '../ui/Form'
import { LanguageSelect } from '../ui/LanguageSelect'
import { IssuedLink, type CopyStatus } from '../ui/IssuedLink'
import { List, ListItem } from '../ui/List'
import { Select } from '../ui/Select'
import { StatusBadge } from '../ui/StatusBadge'
import { StatusMessage, type StatusTone } from '../ui/StatusMessage'
import { Text } from '../ui/Text'
import { TextInput } from '../ui/TextInput'
import { Toolbar } from '../ui/Toolbar'
import { useFieldErrors } from '../ui/useFieldErrors'

const statusTone: Record<TeacherStatus, StatusTone> = {
  ACTIVE: 'success',
  WAITING: 'neutral',
  DEACTIVATED: 'danger',
}

type ShownLink = { id: string; name: string; link: string }

// The action a row's request runs, so only the pressed button shows its busy label.
type RowAction = 'invite' | 'reset' | 'reactivate' | 'deactivate'

function TeacherRow({
  teacher,
  busyAction,
  justAdded,
  linkBox,
  refocus,
  onRefocused,
  onDeactivate,
  onReactivate,
  onLink,
}: {
  teacher: Teacher
  busyAction: RowAction | undefined
  justAdded: boolean
  linkBox?: ReactNode
  // Put focus on the Reactivate button as soon as the row shows it.
  refocus: boolean
  onRefocused: () => void
  onDeactivate: (opener: HTMLElement) => void
  onReactivate: () => void
  onLink: () => void
}) {
  const { t } = useTranslation()
  const status = teacherStatus(teacher)
  const busy = busyAction !== undefined
  const linkAction = teacher.hasPassword ? 'reset' : 'invite'
  const reactivate = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (refocus && !teacher.active && reactivate.current) {
      // Only when focus was lost with the old button, not when the admin has moved on.
      if (document.activeElement === document.body) {
        reactivate.current.focus()
      }
      onRefocused()
    }
  }, [refocus, teacher.active, onRefocused])

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
      extra={linkBox}
      actions={
        teacher.active ? (
          <ButtonRow>
            <Button
              variant="secondary"
              disabled={busy}
              busy={busyAction === linkAction}
              busyLabel={t(`adminTeachers.busy.${linkAction}Link`)}
              onClick={onLink}
            >
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
          </ButtonRow>
        ) : (
          <ButtonRow>
            <Button
              ref={reactivate}
              variant="secondary"
              disabled={busy}
              busy={busyAction === 'reactivate'}
              busyLabel={t('adminTeachers.busy.reactivate')}
              onClick={onReactivate}
            >
              {t('adminTeachers.actions.reactivate')}
            </Button>
          </ButtonRow>
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
  const [nativeLanguage, setNativeLanguage] = useState('')
  const [error, setError] = useState<string>()
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)
  const mounted = useRef(true)
  const [zones] = useState(timeZoneOptions)
  const nameRef = useRef<HTMLInputElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  const timeZoneRef = useRef<HTMLSelectElement>(null)
  const nativeLanguageRef = useRef<HTMLSelectElement>(null)
  const fieldErrors = useFieldErrors([
    { key: 'name', backend: 'name', ref: nameRef },
    { key: 'email', backend: 'email', ref: emailRef },
    { key: 'timeZone', backend: 'timeZone', ref: timeZoneRef },
    {
      key: 'nativeLanguage',
      backend: 'nativeLanguage',
      ref: nativeLanguageRef,
    },
  ])
  const { errors } = fieldErrors

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
    setError(undefined)
    fieldErrors.clearAll()
    const missing: Partial<
      Record<'name' | 'email' | 'timeZone' | 'nativeLanguage', string>
    > = {}
    if (name.trim() === '') {
      missing.name = t('fieldErrors.REQUIRED')
    }
    if (email.trim() === '') {
      missing.email = t('fieldErrors.REQUIRED')
    }
    if (timeZone === '') {
      missing.timeZone = t('fieldErrors.REQUIRED')
    }
    if (nativeLanguage === '') {
      missing.nativeLanguage = t('languages.required')
    }
    if (Object.keys(missing).length > 0) {
      fieldErrors.show(missing)
      return
    }
    inFlight.current = true
    setSubmitting(true)
    const submitted = { name, email, timeZone, nativeLanguage }
    try {
      const { id, link } = await createTeacher(submitted)
      onCreated({ id, name: submitted.name, link })
    } catch (failure) {
      if (!mounted.current) {
        return
      }
      if (failure instanceof ApiError && fieldErrors.place(failure)) {
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
      // The browser closed it anyway: unmount it, or keep it open while the request runs.
      onClose={onCancel}
      busy={submitting}
    >
      <Form noValidate onSubmit={(event) => void onSubmit(event)}>
        <TextInput
          label={t('adminTeachers.create.name')}
          required
          value={name}
          onChange={(value) => {
            setName(value)
            fieldErrors.clear('name')
          }}
          error={errors.name}
          inputRef={nameRef}
        />
        <TextInput
          label={t('adminTeachers.create.email')}
          type="email"
          required
          value={email}
          onChange={(value) => {
            setEmail(value)
            fieldErrors.clear('email')
          }}
          error={errors.email}
          inputRef={emailRef}
        />
        <Select
          label={t('adminTeachers.create.timeZone')}
          required
          value={timeZone}
          onChange={(value) => {
            setTimeZone(value)
            fieldErrors.clear('timeZone')
          }}
          error={errors.timeZone}
          inputRef={timeZoneRef}
          options={zones}
        />
        <LanguageSelect
          label={t('adminTeachers.create.nativeLanguage')}
          ariaRequired
          value={nativeLanguage}
          onChange={(value) => {
            setNativeLanguage(value)
            fieldErrors.clear('nativeLanguage')
          }}
          error={errors.nativeLanguage}
          inputRef={nativeLanguageRef}
        />
        {error ? <StatusMessage tone="danger">{error}</StatusMessage> : null}
        <ButtonRow>
          <Button variant="secondary" disabled={submitting} onClick={onCancel}>
            {t('adminTeachers.create.cancel')}
          </Button>
          <Button
            type="submit"
            busy={submitting}
            busyLabel={t('adminTeachers.create.submitting')}
          >
            {t('adminTeachers.create.submit')}
          </Button>
        </ButtonRow>
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
  const [busyActions, setBusyActions] = useState<Record<string, RowAction>>({})
  const [confirming, setConfirming] = useState<{
    id: string
    name: string
    opener: HTMLElement
  }>()
  const [actionError, setActionError] = useState<string>()
  const [refocusId, setRefocusId] = useState<string>()
  const [linkCopy, setLinkCopy] = useState<CopyStatus>('idle')
  const linkFocus = useRef<number | null>(null)
  const busyRef = useRef(new Map<string, RowAction>())
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
        setRefocusId(undefined)
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

  // A new link starts with a fresh copy status and no remembered focus.
  function showLink(next: ShownLink) {
    setLinkCopy('idle')
    linkFocus.current = null
    setShownLink(next)
  }

  const clearRefocus = useCallback(() => setRefocusId(undefined), [])

  // Runs one request for a row: one busy action per row (the one pressed), reload
  // afterwards either way.
  async function runRowAction(
    teacher: { id: string; name: string },
    pressed: RowAction,
    action: () => Promise<string | null | void>,
  ): Promise<boolean> {
    if (busyRef.current.has(teacher.id)) {
      return false
    }
    busyRef.current.set(teacher.id, pressed)
    setBusyActions(Object.fromEntries(busyRef.current))
    setActionError(undefined)
    setRefocusId(undefined)
    let succeeded = false
    try {
      const link = await action()
      succeeded = true
      if (mounted.current && link) {
        showLink({ id: teacher.id, name: teacher.name, link })
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
        setBusyActions(Object.fromEntries(busyRef.current))
        void load()
      }
    }
    return succeeded
  }

  async function onConfirmDeactivate() {
    if (!confirming) {
      return
    }
    const target = confirming
    const done = await runRowAction(target, 'deactivate', () =>
      deactivateTeacher(target.id),
    )
    if (mounted.current) {
      setConfirming(undefined)
      // The reload swaps Deactivate for Reactivate; focus follows it.
      if (done) {
        setRefocusId(target.id)
      }
    }
  }

  function onCreated(created: { id: string; name: string; link: string }) {
    if (!mounted.current) {
      return
    }
    showLink({
      id: created.id,
      name: created.name,
      link: created.link,
    })
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

  const linkRowShown =
    shownLink !== undefined && shown?.some((item) => item.id === shownLink.id)
  const linkBox = (standalone: boolean) =>
    shownLink ? (
      <IssuedLink
        key={`${shownLink.id}:${shownLink.link}`}
        name={shownLink.name}
        link={shownLink.link}
        standalone={standalone}
        copy={linkCopy}
        onCopyChange={setLinkCopy}
        focusMemoryRef={linkFocus}
        onClose={() => setShownLink(undefined)}
      />
    ) : null

  return (
    <CabinetPage
      role="ADMIN"
      toolbar={
        <Toolbar>
          <Button
            icon="plus"
            onClick={(event) => setCreating({ opener: event.currentTarget })}
          >
            {t('adminTeachers.add')}
          </Button>
        </Toolbar>
      }
    >
      {shownLink && !linkRowShown ? linkBox(true) : null}
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
              busyAction={busyActions[teacher.id]}
              justAdded={teacher.id === pinnedId}
              linkBox={
                shownLink?.id === teacher.id ? linkBox(false) : undefined
              }
              refocus={teacher.id === refocusId && !confirming}
              onRefocused={clearRefocus}
              onDeactivate={(opener) =>
                setConfirming({ id: teacher.id, name: teacher.name, opener })
              }
              onReactivate={() =>
                void runRowAction(teacher, 'reactivate', () =>
                  reactivateTeacher(teacher.id),
                )
              }
              onLink={() =>
                void runRowAction(
                  teacher,
                  teacher.hasPassword ? 'reset' : 'invite',
                  () => issueResetLink(teacher.id),
                )
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
            if (!(confirming.id in busyActions)) {
              setConfirming(undefined)
            }
          }}
          // The browser closed it anyway: unmount it, or keep it open while the request runs.
          onClose={() => setConfirming(undefined)}
          busy={confirming.id in busyActions}
        >
          <Text>
            {t('adminTeachers.deactivate.body', { name: confirming.name })}
          </Text>
          <ButtonRow>
            <Button
              variant="secondary"
              disabled={confirming.id in busyActions}
              onClick={() => setConfirming(undefined)}
            >
              {t('adminTeachers.deactivate.cancel')}
            </Button>
            <Button
              variant="danger"
              disabled={confirming.id in busyActions}
              busy={busyActions[confirming.id] === 'deactivate'}
              busyLabel={t('adminTeachers.deactivate.confirming')}
              onClick={() => void onConfirmDeactivate()}
            >
              {t('adminTeachers.deactivate.confirm')}
            </Button>
          </ButtonRow>
        </Dialog>
      ) : null}
      {creating ? (
        <CreateDialog
          opener={creating.opener}
          onCancel={() => setCreating(undefined)}
          onCreated={onCreated}
        />
      ) : null}
    </CabinetPage>
  )
}
