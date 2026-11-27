import { expect, test } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { loginAsAdminInUi } from './accounts'

test('[web-client/R28-S4] The chevron renders at the icon size', async ({
  page,
  isMobile,
}) => {
  // given a logged-in admin with the Add teacher dialog open
  if (isMobile) {
    await page.setViewportSize({ width: 360, height: 800 })
  }
  await loginAsAdminInUi(page)
  await page.goto('/admin')
  await page.getByRole('button', { name: en.adminTeachers.add }).click()
  const select = page.getByLabel(en.adminTeachers.create.timeZone, {
    exact: true,
  })
  await expect(select).toBeVisible()

  // when the chevron in the time zone select is measured by its computed style
  const size = await select
    .locator('xpath=following-sibling::*[@data-select-arrow]')
    .locator('svg')
    .evaluate((element) => {
      const style = getComputedStyle(element)
      return { width: style.width, height: style.height }
    })

  // then its width and its height are 20 pixels
  expect(size).toEqual({ width: '20px', height: '20px' })
})
