import { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from './Button'
import styles from './IssuedLink.module.css'
import { StatusMessage } from './StatusMessage'

// Shows a link once, as plain text in a read-only field (never as an <a>).
export function IssuedLink({
  name,
  link,
  onClose,
}: {
  name: string
  link: string
  onClose: () => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const [copy, setCopy] = useState<'idle' | 'copied' | 'manual'>('idle')

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
    <section className={styles.box}>
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
      <div className={styles.actions}>
        <Button onClick={() => void onCopy()}>{t('issuedLink.copy')}</Button>
        <Button variant="secondary" onClick={onClose}>
          {t('issuedLink.close')}
        </Button>
      </div>
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
