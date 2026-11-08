import { expect, test } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { loginAsAdminInUi } from './accounts'

test('[web-admin-teachers/R1-S4] The page fits a phone screen', async ({
  page,
}) => {
  // given a teacher with a long email, created in the UI so a link is shown
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const email = `e2e-long-${unique}-${'x'.repeat(30)}@a-rather-long-domain-name.example.com`
  await page.setViewportSize({ width: 360, height: 800 })
  await loginAsAdminInUi(page)
  await page.getByRole('button', { name: en.adminTeachers.add }).click()
  await page.getByLabel(en.adminTeachers.create.name).fill('Long Email Teacher')
  await page.getByLabel(en.adminTeachers.create.email).fill(email)

  // when the link is shown
  await page
    .getByRole('button', { name: en.adminTeachers.create.submit })
    .click()
  await expect(
    page.getByLabel(
      en.issuedLink.label.replace('{{name}}', 'Long Email Teacher'),
    ),
  ).toBeVisible()
  const row = page.getByRole('listitem').filter({ hasText: email })
  await expect(row).toBeVisible()

  // then nothing scrolls sideways and the row's buttons are visible
  const widths = await page.evaluate(() => ({
    scroll: document.documentElement.scrollWidth,
    viewport: document.documentElement.clientWidth,
  }))
  expect(widths.scroll).toBeLessThanOrEqual(widths.viewport)
  const buttons = row.getByRole('button')
  expect(await buttons.count()).toBeGreaterThan(0)
  for (const button of await buttons.all()) {
    await expect(button).toBeVisible()
    const box = await button.boundingBox()
    expect(box!.x).toBeGreaterThanOrEqual(0)
    expect(box!.x + box!.width).toBeLessThanOrEqual(widths.viewport)
  }

  // and the "Add teacher" dialog fits too
  await page.getByRole('button', { name: en.adminTeachers.add }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  const after = await page.evaluate(() => ({
    scroll: document.documentElement.scrollWidth,
    viewport: document.documentElement.clientWidth,
  }))
  expect(after.scroll).toBeLessThanOrEqual(after.viewport)
  const controls = dialog.locator('input, select, button')
  expect(await controls.count()).toBeGreaterThan(3)
  for (const control of await controls.all()) {
    await expect(control).toBeVisible()
    const box = await control.boundingBox()
    expect(box!.x).toBeGreaterThanOrEqual(0)
    expect(box!.x + box!.width).toBeLessThanOrEqual(after.viewport)
  }
})

test('[web-admin-teachers/R2-S1] Admin creates a teacher', async ({ page }) => {
  // given a logged-in admin on /admin
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const name = `Created Teacher ${unique}`
  const email = `e2e-create-${unique}@example.com`
  await loginAsAdminInUi(page)

  // when the admin enters a name, a new email and a time zone, and submits
  await page.getByRole('button', { name: en.adminTeachers.add }).click()
  const nameField = page.getByLabel(en.adminTeachers.create.name)
  const emailField = page.getByLabel(en.adminTeachers.create.email)
  await nameField.fill(name)
  await emailField.fill(email)
  await page
    .getByLabel(en.adminTeachers.create.timeZone)
    .selectOption('Europe/Berlin')
  await page
    .getByRole('button', { name: en.adminTeachers.create.submit })
    .click()

  // then the dialog closes and the set-password link is shown with the new teacher's name
  await expect(page.getByRole('dialog')).toBeHidden()
  const link = page.getByLabel(en.issuedLink.label.replace('{{name}}', name))
  await expect(link).toBeVisible()
  await expect(link).toHaveValue(
    /^https?:\/\/[^/]+(\/[^/#?]+)*\/set-password#token=.+/,
  )

  // and the new teacher is the first row of the list
  const row = page.getByRole('listitem').first()
  await expect(row).toContainText(email)
  await expect(row).toContainText(name)
  await expect(row).toContainText(en.adminTeachers.justAdded)
})
