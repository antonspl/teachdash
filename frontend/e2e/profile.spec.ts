import { expect, test } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { createTeacherAndLoginInUi } from './accounts'

test('[web-profile/R2-S1] Saved profile survives a reload', async ({
  page,
  playwright,
  baseURL,
}) => {
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)

  await page.getByLabel(en.profile.name).fill('Anna S.')
  await page.getByLabel(en.profile.timeZone).selectOption('Asia/Dubai')
  await page.getByRole('button', { name: en.profile.save }).click()
  await expect(page.getByText(en.profile.saved)).toBeVisible()
  await page.reload()

  await expect(page.getByText(/^Anna S\./)).toBeVisible()
  await expect(page.getByLabel(en.profile.name)).toHaveValue('Anna S.')
  await expect(page.getByLabel(en.profile.timeZone)).toHaveValue('Asia/Dubai')
})

test('[web-profile/R3-S1] Password change keeps the user logged in', async ({
  page,
  playwright,
  baseURL,
}) => {
  const { email } = await createTeacherAndLoginInUi(
    page,
    playwright,
    baseURL,
    'Anna',
  )
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)

  await page
    .getByLabel(en.profile.currentPassword, { exact: true })
    .fill('teacher-pass-1')
  await page
    .getByLabel(en.profile.newPassword, { exact: true })
    .fill('teacher-pass-2')
  await page
    .getByLabel(en.profile.repeatPassword, { exact: true })
    .fill('teacher-pass-2')
  await page.getByRole('button', { name: en.profile.changePassword }).click()
  await expect(page.getByText(en.profile.passwordChanged)).toBeVisible()
  await expect(page).toHaveURL(/\/profile$/)

  // A save right after uses the new session and CSRF token.
  await page.getByLabel(en.profile.name).fill('Anna S.')
  await page.getByRole('button', { name: en.profile.save }).click()
  await expect(page.getByText(en.profile.saved)).toBeVisible()

  await page.getByRole('button', { name: en.header.logout }).click()
  await expect(page).toHaveURL(/\/login$/)

  await page.getByLabel(en.login.email).fill(email)
  await page
    .getByLabel(en.login.password, { exact: true })
    .fill('teacher-pass-1')
  await page.getByRole('button', { name: en.login.submit }).click()
  await expect(page.getByText(en.errors.INVALID_CREDENTIALS)).toBeVisible()
  await expect(page).toHaveURL(/\/login$/)

  await page
    .getByLabel(en.login.password, { exact: true })
    .fill('teacher-pass-2')
  await page.getByRole('button', { name: en.login.submit }).click()
  await expect(page).toHaveURL(/\/teacher$/)
})

test('[web-profile/R1-S5] Page fits a phone', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a teacher with a 200-character name without spaces, on a 360-pixel screen
  await page.setViewportSize({ width: 360, height: 800 })
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'W'.repeat(200))

  // when /profile is opened, and then /teacher
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)
  await expect(
    page.getByRole('heading', { name: en.profile.title }),
  ).toBeVisible()

  // then neither page scrolls sideways
  const fits = () =>
    page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    )
  expect(await fits(), '/profile').toBe(true)
  await page.getByRole('button', { name: en.header.backToCabinet }).click()
  await expect(page).toHaveURL(/\/teacher$/)
  expect(await fits(), '/teacher').toBe(true)
})
