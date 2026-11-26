import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import i18next from 'i18next'
import { afterEach, expect, test } from 'vitest'
import '../i18n'
import { UiLanguageSelect } from './UiLanguageSelect'

afterEach(async () => {
  cleanup()
  await i18next.changeLanguage('en')
  localStorage.clear()
  document.documentElement.lang = 'en'
})

test('[web-client/R20-S4] the select shows endonyms with their own language', async () => {
  const { rerender } = render(<UiLanguageSelect />)
  const select = screen.getByRole('combobox', { name: 'Language' })
  const options = Array.from(select.querySelectorAll('option'))
  expect(options.map((o) => o.textContent)).toEqual(['English', 'Русский'])
  expect(options.map((o) => o.lang)).toEqual(['en', 'ru'])

  await i18next.changeLanguage('ru')
  rerender(<UiLanguageSelect />)
  const ruSelect = screen.getByRole('combobox', { name: 'Язык' })
  expect(
    Array.from(ruSelect.querySelectorAll('option')).map((o) => o.textContent),
  ).toEqual(['English', 'Русский'])
})

test('[web-client/R20-S6] choosing English changes the page back', async () => {
  await i18next.changeLanguage('ru')
  render(<UiLanguageSelect />)
  const select = screen.getByRole('combobox', { name: 'Язык' })
  fireEvent.change(select, { target: { value: 'en' } })
  const back = await screen.findByRole('combobox', { name: 'Language' })
  expect(localStorage.getItem('teachdash.lang')).toBe('en')
  expect(document.documentElement.lang).toBe('en')
  expect(back).toBe(select)
})
