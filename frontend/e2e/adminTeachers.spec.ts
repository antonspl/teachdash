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
})
