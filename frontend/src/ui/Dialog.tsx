import { useEffect, useId, useRef, type ReactNode } from 'react'
import styles from './Dialog.module.css'

// A modal dialog on the native <dialog>. Mount it to open it, unmount it to close it.
// Escape (the native "cancel") calls onCancel. Focus goes back to the element that was
// focused when it opened.
export function Dialog({
  title,
  children,
  onCancel,
}: {
  title: string
  children: ReactNode
  onCancel: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const cancelRef = useRef(onCancel)

  useEffect(() => {
    cancelRef.current = onCancel
  })

  useEffect(() => {
    const dialog = ref.current
    const opener = document.activeElement
    dialog?.showModal()
    return () => {
      if (dialog?.open) {
        dialog.close()
      }
      if (opener instanceof HTMLElement && opener.isConnected) {
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
