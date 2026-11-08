import { useEffect, useId, useRef, type ReactNode } from 'react'
import styles from './Dialog.module.css'

// A modal dialog on the native <dialog>. Mount it to open it, unmount it to close it.
// Escape (the native "cancel") calls onCancel. Focus moves to the first button or field inside
// (put the safe choice first) and goes back to `returnFocusTo` (the element that opened it;
// browsers like Safari do not focus a clicked button, so activeElement cannot be trusted).
export function Dialog({
  title,
  children,
  onCancel,
  returnFocusTo,
}: {
  title: string
  children: ReactNode
  onCancel: () => void
  returnFocusTo?: HTMLElement | null
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const cancelRef = useRef(onCancel)
  const returnRef = useRef(returnFocusTo)

  useEffect(() => {
    cancelRef.current = onCancel
    returnRef.current = returnFocusTo
  })

  useEffect(() => {
    const dialog = ref.current
    dialog?.showModal()
    // Browsers focus the first focusable element themselves; make it explicit so the
    // behaviour does not depend on that.
    if (dialog && !dialog.contains(document.activeElement)) {
      dialog.querySelector<HTMLElement>('button, input, select')?.focus()
    }
    return () => {
      if (dialog?.open) {
        dialog.close()
      }
      const opener = returnRef.current
      if (opener?.isConnected) {
        opener.focus()
      }
    }
  }, [])

  return (
    <dialog
      ref={ref}
      className={styles.dialog}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault()
        cancelRef.current()
      }}
    >
      <h2 id={titleId} className={styles.title}>
        {title}
      </h2>
      {children}
    </dialog>
  )
}
