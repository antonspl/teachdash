import { expect, test, type Page } from '@playwright/test'
import ru from '../src/i18n/ru.json' with { type: 'json' }
import { createTeacherAndLoginInUi } from './accounts'

const STORAGE_KEY = 'teachdash.lang'

async function expectSelectFits(page: Page) {
  const select = page.getByLabel(ru.uiLanguage.label, { exact: true })
  await expect(select).toBeVisible()
  const box = await select.boundingBox()
  const viewport = page.viewportSize()
  expect(box, 'the select has a box').not.toBeNull()
  expect(viewport, 'the page has a viewport').not.toBeNull()
  expect(box!.x).toBeGreaterThanOrEqual(0)
  expect(box!.x + box!.width).toBeLessThanOrEqual(viewport!.width)
  const overflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth -
      document.documentElement.clientWidth,
  )
  expect(overflow, 'no horizontal scrolling').toBeLessThanOrEqual(0)
}

test('[web-client/R20-S8] The stored choice is used after a reload', async ({
  page,
}) => {
  await page.goto('/login')
  await page.evaluate(([key]) => localStorage.setItem(key, 'ru'), [STORAGE_KEY])
  await page.reload()

  await expect(
    page.getByLabel(ru.uiLanguage.label, { exact: true }),
  ).toHaveValue('ru')
  await expect(page.locator('html')).toHaveAttribute('lang', 'ru')
})

test('[web-client/R20-S9] The switch fits a phone screen', async ({
  page,
  playwright,
  baseURL,
}) => {
  await page.setViewportSize({ width: 360, height: 800 })
  await page.goto('/login')
  await page.evaluate(([key]) => localStorage.setItem(key, 'ru'), [STORAGE_KEY])
  await page.reload()
  await expectSelectFits(page)

  // the login helper uses the English labels
  await page.evaluate(([key]) => localStorage.removeItem(key), [STORAGE_KEY])
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'E2E Lang Teacher')
  await page.evaluate(([key]) => localStorage.setItem(key, 'ru'), [STORAGE_KEY])
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('lang', 'ru')
  await expectSelectFits(page)
})
