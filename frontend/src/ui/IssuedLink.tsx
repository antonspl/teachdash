import { useEffect, useId, useRef, useState, type RefObject } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from './Button'
import { ButtonRow } from './ButtonRow'
import styles from './IssuedLink.module.css'
import { StatusMessage } from './StatusMessage'

export type CopyStatus = 'idle' | 'copied' | 'manual'

// Shows a link once, as plain text in a read-only field (never as an <a>).
// When the owner moves the box (a new element), it can keep the copy status in its own state
// (`copy`, `onCopyChange`) and a `focusMemoryRef` ref: the box records which of its controls has
// focus there and puts focus back on the same one when it mounts with focus on <body>.
export function IssuedLink({
  name,
  link,
  onClose,
  standalone = false,
  copy: givenCopy,
  onCopyChange,
  focusMemoryRef,
}: {
  name: string
  link: string
  onClose: () => void
  // Shown outside a row (above the list): leaves space below it.
  standalone?: boolean
  copy?: CopyStatus
  onCopyChange?: (copy: CopyStatus) => void
  focusMemoryRef?: RefObject<number | null>
}) {
  const { t } = useTranslation()
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const box = useRef<HTMLElement>(null)
  const [ownCopy, setOwnCopy] = useState<CopyStatus>('idle')
  const copy = givenCopy ?? ownCopy
  const setCopy = (next: CopyStatus) => {
    setOwnCopy(next)
    onCopyChange?.(next)
  }

  const controls = () =>
    Array.from(
      box.current?.querySelectorAll<HTMLElement>('input, button') ?? [],
    )

  useEffect(() => {
    const index = focusMemoryRef?.current
    if (index != null && document.activeElement === document.body) {
      controls()[index]?.focus()
    }
    // Only when the box mounts.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function onCopy() {
    try {
      await navigator.clipboard.writeText(link)
      setCopy('copied')
    } catch {
      setCopy('manual')
      input.current?.focus()
      input.current?.select()
    }
  }

  return (
    <section
      ref={box}
      className={`${styles.box} ${standalone ? styles.standalone : ''}`}
      onFocus={(event) => {
        if (focusMemoryRef) {
          focusMemoryRef.current = controls().indexOf(event.target)
        }
      }}
      onBlur={(event) => {
        // Focus moving elsewhere forgets it; losing it because the box is removed does not.
        if (focusMemoryRef && event.relatedTarget !== null) {
          focusMemoryRef.current = null
        }
      }}
    >
      <label htmlFor={id} className={styles.label}>
        {t('issuedLink.label', { name })}
      </label>
      <input
        id={id}
        ref={input}
        className={styles.input}
        type="text"
        readOnly
        value={link}
      />
      <p className={styles.note}>{t('issuedLink.note')}</p>
      <ButtonRow>
        <Button onClick={() => void onCopy()}>{t('issuedLink.copy')}</Button>
        <Button variant="secondary" onClick={onClose}>
          {t('issuedLink.close')}
        </Button>
      </ButtonRow>
      {copy === 'copied' ? (
        <StatusMessage tone="success">{t('issuedLink.copied')}</StatusMessage>
      ) : null}
      {copy === 'manual' ? (
        <StatusMessage tone="neutral">
          {t('issuedLink.copyByHand')}
        </StatusMessage>
      ) : null}
    </section>
  )
}
