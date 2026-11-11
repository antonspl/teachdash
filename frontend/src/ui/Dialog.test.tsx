import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { StrictMode } from 'react'
import { afterEach, expect, test, vi } from 'vitest'
import { Dialog } from './Dialog'

afterEach(cleanup)

// Regression tests for review findings, not spec scenarios. jsdom has no real modal
// dialog: the "close" the browser sends after an Escape it could not cancel is simulated by hand.
function dialogWith(onClose: () => void, busy = false) {
  return (
    <Dialog
      title="Title"
      onCancel={() => undefined}
      onClose={onClose}
      busy={busy}
    >
      <button>Ok</button>
    </Dialog>
  )
}

test('The native close event of a closed dialog calls onClose', () => {
  const onClose = vi.fn()
  render(dialogWith(onClose))
  const dialog = screen.getByRole('dialog')

  dialog.removeAttribute('open')
  fireEvent(dialog, new Event('close'))

  expect(onClose).toHaveBeenCalledTimes(1)
})

test('A close event while the dialog is still open is ignored', () => {
  const onClose = vi.fn()
  render(dialogWith(onClose))

  fireEvent(screen.getByRole('dialog'), new Event('close'))

  expect(onClose).not.toHaveBeenCalled()
})

test('A close event after the dialog was removed is ignored', () => {
  const onClose = vi.fn()
  const { unmount } = render(dialogWith(onClose))
  const dialog = screen.getByRole('dialog')

  unmount()
  dialog.removeAttribute('open')
  fireEvent(dialog, new Event('close'))

  expect(onClose).not.toHaveBeenCalled()
})

test('A native close while busy opens the dialog again', () => {
  const onClose = vi.fn()
  render(dialogWith(onClose, true))
  const dialog = screen.getByRole('dialog')

  dialog.removeAttribute('open')
  fireEvent(dialog, new Event('close'))

  expect(dialog.hasAttribute('open')).toBe(true)
  expect(onClose).not.toHaveBeenCalled()
})

test('StrictMode does not close the dialog by itself', async () => {
  // The browser fires the "close" of the first effect cleanup as a queued task, after the
  // second effect run opened the dialog again: simulate that order.
  const onClose = vi.fn()
  const close = HTMLDialogElement.prototype.close
  const queued: HTMLDialogElement[] = []
  HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
    if (this.hasAttribute('open')) {
      this.removeAttribute('open')
      queued.push(this)
    }
  }
  try {
    render(<StrictMode>{dialogWith(onClose)}</StrictMode>)
    for (const dialog of queued) {
      fireEvent(dialog, new Event('close'))
    }
    await Promise.resolve()

    expect(queued.length).toBeGreaterThan(0)
    expect(onClose).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog').hasAttribute('open')).toBe(true)
  } finally {
    HTMLDialogElement.prototype.close = close
  }
})
