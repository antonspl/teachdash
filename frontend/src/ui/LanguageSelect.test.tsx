import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { useState } from 'react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import '../i18n'
import en from '../i18n/en.json'
import { LanguageSelect } from './LanguageSelect'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

function list(codes: string[]) {
  return new Response(
    JSON.stringify(codes.map((code) => ({ code, content: false }))),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

function Harness({ initial }: { initial: string }) {
  const [value, setValue] = useState(initial)
  return <LanguageSelect label="Language" value={value} onChange={setValue} />
}

function optionTexts() {
  return screen.getAllByRole('option').map((option) => option.textContent)
}

test('shows names and starts empty with a placeholder', async () => {
  fetchMock.mockResolvedValue(list(['de', 'en', 'ru']))
  render(<Harness initial="" />)
  await screen.findByRole('option', { name: 'German' })
  expect(optionTexts()).toEqual(['German', 'English', 'Russian'])
  expect((screen.getByLabelText('Language') as HTMLSelectElement).value).toBe(
    '',
  )
  const placeholder = Array.from(
    (screen.getByLabelText('Language') as HTMLSelectElement).options,
  ).find((option) => option.value === '')
  expect(placeholder?.textContent).toBe(en.languages.choose)
  expect(placeholder?.hidden).toBe(true)
  expect(placeholder?.disabled).toBe(true)
})

test('a code with no name shows the code, and the stored value is kept', async () => {
  fetchMock.mockResolvedValue(list(['de', 'sv']))
  render(<Harness initial="sv" />)
  await screen.findByRole('option', { name: 'German' })
  expect(optionTexts()).toEqual(['German', 'sv'])
  expect((screen.getByLabelText('Language') as HTMLSelectElement).value).toBe(
    'sv',
  )
})

test('a stored value missing from the list is added', async () => {
  fetchMock.mockResolvedValue(list(['de', 'en']))
  render(<Harness initial="ru" />)
  await screen.findByRole('option', { name: 'English' })
  expect(optionTexts()).toEqual(['German', 'English', 'Russian'])
})

test('a failed load shows an error and only the stored value; retry loads', async () => {
  fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
  fetchMock.mockResolvedValueOnce(list(['de', 'en', 'ru']))
  render(<Harness initial="ru" />)
  await screen.findByText(en.languages.loadFailed)
  expect(optionTexts()).toEqual(['Russian'])
  fireEvent.click(screen.getByRole('button', { name: en.ui.tryAgain }))
  await waitFor(() =>
    expect(screen.queryByText(en.languages.loadFailed)).toBeNull(),
  )
  await screen.findByRole('option', { name: 'German' })
  expect(optionTexts()).toEqual(['German', 'English', 'Russian'])
  expect((screen.getByLabelText('Language') as HTMLSelectElement).value).toBe(
    'ru',
  )
})
