import { expect, test } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { createTeacherAndLoginInUi, loginAsAdminInUi } from './accounts'

test('[web-client/R17-S5] Taken email against the real backend', async ({
  page,
}) => {
  // given a logged-in admin and a teacher with a taken email (unique per run)
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const email = `taken-${unique}@example.com`
  await loginAsAdminInUi(page)
  await page.getByRole('button', { name: en.adminTeachers.add }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel(en.adminTeachers.create.name).fill('Taken Owner')
  await dialog.getByLabel(en.adminTeachers.create.email).fill(email)
  await dialog
    .getByRole('button', { name: en.adminTeachers.create.submit })
    .click()
  await expect(dialog).toBeHidden()

  // when the admin adds a teacher named "Анна" with that email
  await page.getByRole('button', { name: en.adminTeachers.add }).click()
  await expect(dialog).toBeVisible()
  await dialog.getByLabel(en.adminTeachers.create.name).fill('Анна')
  const emailField = dialog.getByLabel(en.adminTeachers.create.email)
  await emailField.fill(email)
  await dialog
    .getByRole('button', { name: en.adminTeachers.create.submit })
    .click()

  // then the message is under the email field, focused, no form-level message
  await expect(emailField).toHaveAccessibleDescription(en.errors.EMAIL_TAKEN)
  await expect(dialog.getByText(en.errors.EMAIL_TAKEN)).toBeVisible()
  await expect(emailField).toBeFocused()
  await expect(dialog.getByRole('status')).toHaveCount(0)
  await expect(dialog).toBeVisible()
})

test('[web-client/R17-S6] Wrong current password against the real backend', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a logged-in teacher with a known password on /profile
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)

  // when a wrong current password and a valid new one are submitted
  const current = page.getByLabel(en.profile.currentPassword, { exact: true })
  const next = page.getByLabel(en.profile.newPassword, { exact: true })
  const repeat = page.getByLabel(en.profile.repeatPassword, { exact: true })
  await current.fill('wrong-pass-1')
  await next.fill('teacher-pass-2')
  await repeat.fill('teacher-pass-2')
  await page.getByRole('button', { name: en.profile.changePassword }).click()

  // then the message is under the current password, focused, no form-level one
  await expect(current).toHaveAccessibleDescription(en.profile.wrongPassword)
  await expect(page.getByText(en.profile.wrongPassword)).toBeVisible()
  await expect(current).toBeFocused()
  await expect(page.getByRole('status')).toHaveCount(0)
  await expect(next).toHaveValue('teacher-pass-2')
  await expect(repeat).toHaveValue('teacher-pass-2')
})

test('[web-client/R17-S7] Too-long name against the real backend', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a logged-in teacher on /profile
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)

  // when a name of 201 "Я" is saved
  const longName = 'Я'.repeat(201)
  const name = page.getByLabel(en.profile.name)
  await name.fill(longName)
  await page.getByRole('button', { name: en.profile.save }).click()

  // then the message is under the name field and nothing else changes
  await expect(name).toHaveAccessibleDescription(en.fieldErrors.TOO_LONG)
  await expect(page.getByText(en.fieldErrors.TOO_LONG)).toBeVisible()
  await expect(page.getByRole('status')).toHaveCount(0)
  await expect(name).toHaveValue(longName)
  await expect(page.getByText(/^Anna/).first()).toBeVisible()
  await expect(page.getByText(longName)).toHaveCount(0)
})
