import { expect, test } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { loginAsAdminInUi } from './accounts'

test('[web-client/R22-S3] The plus on Add teacher is 20 pixels', async ({
  page,
}, testInfo) => {
  // given a logged-in admin on /admin
  if (testInfo.project.name === 'phone-chrome') {
    await page.setViewportSize({ width: 360, height: 800 })
  }
  await loginAsAdminInUi(page)
  await page.goto('/admin')
  const button = page.getByRole('button', { name: en.adminTeachers.add })
  await expect(button).toBeVisible()

  // when the plus is measured by its computed style
  const size = await button
    .locator('svg')
    .first()
    .evaluate((element) => {
      const style = getComputedStyle(element)
      return { width: style.width, height: style.height }
    })

  // then it is 20 pixels wide and high
  expect(size).toEqual({ width: '20px', height: '20px' })

  // and on the phone project there is no horizontal scroll at 360 pixels
  if (testInfo.project.name === 'phone-chrome') {
    const fits = await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    )
    expect(fits).toBe(true)
  }
})
