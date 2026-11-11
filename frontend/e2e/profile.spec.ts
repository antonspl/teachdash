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
