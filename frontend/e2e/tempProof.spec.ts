import { expect, test } from '@playwright/test'

test('TEMP CI proof failing e2e', async ({ page }) => {
  await page.goto('/')
  expect(1).toBe(2)
})
