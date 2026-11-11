import { useEffect, useId, useRef, type ReactNode } from 'react'
import styles from './Dialog.module.css'

// A modal dialog on the native <dialog>. Mount it to open it, unmount it to close it.
// Escape (the native "cancel") calls onCancel. If the browser closes the <dialog> by itself
// anyway (a second Escape without user activation cannot be prevented in Chrome), the native
// "close" calls onClose so the owner can unmount it, or, while `busy`, opens it again. A "close"
// from the component's own unmount or remount (StrictMode) is ignored: that dialog is detached
// or open again. Focus moves to the first button or field inside
// (put the safe choice first) and goes back to `returnFocusTo` (the element that opened it;
// browsers like Safari do not focus a clicked button, so activeElement cannot be trusted).
export function Dialog({
  title,
  children,
  onCancel,
  onClose,
  busy = false,
  returnFocusTo,
}: {
  title: string
  children: ReactNode
  onCancel: () => void
  onClose: () => void
  // A request is running: the dialog must stay visible, even if the browser closes it.
  busy?: boolean
  returnFocusTo?: HTMLElement | null
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const cancelRef = useRef(onCancel)
  const closeRef = useRef(onClose)
  const busyRef = useRef(busy)
  const returnRef = useRef(returnFocusTo)

  useEffect(() => {
    cancelRef.current = onCancel
    closeRef.current = onClose
    busyRef.current = busy
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
      onClose={(event) => {
        const dialog = event.currentTarget
        if (dialog.open || !dialog.isConnected) {
          return
        }
        if (busyRef.current) {
          dialog.showModal()
        } else {
          closeRef.current()
        }
      }}
    >
      <h2 id={titleId} className={styles.title}>
        {title}
      </h2>
      {children}
    </dialog>
  )
}
