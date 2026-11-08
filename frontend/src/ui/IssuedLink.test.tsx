import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import '../i18n'
import en from '../i18n/en.json'
import { IssuedLink } from './IssuedLink'

const LINK = 'https://app.example.com/set-password#token=abc123'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function stubClipboard(writeText: (text: string) => Promise<void>) {
  vi.stubGlobal('navigator', { clipboard: { writeText } })
}

test('[web-admin-teachers/R3-S1] link is shown as text with a note', () => {
  render(<IssuedLink name="Anna Smith" link={LINK} onClose={() => undefined} />)
  const field = screen.getByLabelText<HTMLInputElement>(
    en.issuedLink.label.replace('{{name}}', 'Anna Smith'),
  )
  expect(field.value).toBe(LINK)
  expect(screen.queryByRole('link')).toBeNull()
  expect(screen.getByText(en.issuedLink.note)).toBeTruthy()
})

test('[web-admin-teachers/R3-S2] Copy puts the link on the clipboard', async () => {
  const writeText = vi.fn().mockResolvedValue(undefined)
  stubClipboard(writeText)
  render(<IssuedLink name="Anna Smith" link={LINK} onClose={() => undefined} />)
  fireEvent.click(screen.getByRole('button', { name: en.issuedLink.copy }))
  const status = await screen.findByRole('status')
  expect(writeText).toHaveBeenCalledWith(LINK)
  expect(status.textContent).toBe(en.issuedLink.copied)
})

test('[web-admin-teachers/R3-S3] Copy fails and the link text is selected', async () => {
  stubClipboard(() => Promise.reject(new Error('denied')))
  render(<IssuedLink name="Anna Smith" link={LINK} onClose={() => undefined} />)
  fireEvent.click(screen.getByRole('button', { name: en.issuedLink.copy }))
  const status = await screen.findByRole('status')
  expect(status.textContent).toBe(en.issuedLink.copyByHand)
  const field = screen.getByLabelText<HTMLInputElement>(
    en.issuedLink.label.replace('{{name}}', 'Anna Smith'),
  )
  expect(field.selectionStart).toBe(0)
  expect(field.selectionEnd).toBe(LINK.length)
})

test('[web-admin-teachers/R3-S4] Close calls onClose', () => {
  const onClose = vi.fn()
  render(<IssuedLink name="Anna Smith" link={LINK} onClose={onClose} />)
  fireEvent.click(screen.getByRole('button', { name: en.issuedLink.close }))
  expect(onClose).toHaveBeenCalledOnce()
})
