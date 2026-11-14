import { expect, test, type Locator, type Page } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { createTeacherAndLoginInUi, loginAsAdminInUi } from './accounts'

const LONG_ZONE = 'America/Argentina/ComodRivadavia'

async function box(locator: Locator) {
  const result = await locator.boundingBox()
  expect(result, 'the element has a box').not.toBeNull()
  return result!
}

/** The arrow element beside the select with the given label. */
function arrowOf(page: Page, label: string) {
  return page
    .getByLabel(label)
    .locator('xpath=following-sibling::*[@data-select-arrow]')
}

function styleOf(select: Locator) {
  return select.evaluate((element) => {
    const style = getComputedStyle(element)
    return {
      appearance: style.appearance,
      textOverflow: style.textOverflow,
      paddingLeft: parseFloat(style.paddingLeft),
      paddingRight: parseFloat(style.paddingRight),
      borderLeft: parseFloat(style.borderLeftWidth),
      borderRight: parseFloat(style.borderRightWidth),
      scrollWidth: element.scrollWidth,
      clientWidth: element.clientWidth,
    }
  })
}

async function checkInset(page: Page, label: string) {
  const select = page.getByLabel(label)
  const arrow = arrowOf(page, label)
  await expect(arrow).toBeVisible()
  const style = await styleOf(select)
  expect(style.appearance, "the browser's own arrow is off").toBe('none')
  const s = await box(select)
  const a = await box(arrow)
  expect(a.x).toBeGreaterThanOrEqual(s.x)
  expect(a.x + a.width).toBeLessThanOrEqual(s.x + s.width)
  const innerRight = s.x + s.width - style.borderRight
  expect(
    Math.abs(innerRight - (a.x + a.width) - style.paddingLeft),
  ).toBeLessThanOrEqual(1)
}

test('[web-client/R13-S1] Arrow is inset like the text', async ({ page }) => {
  // given a logged-in admin, on /profile
  await loginAsAdminInUi(page)
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)

  // when the time zone select is shown, then the arrow is inset
  await checkInset(page, en.profile.timeZone)

  // and in the "Add teacher" dialog
  await page.goto('/admin')
  await page.getByRole('button', { name: en.adminTeachers.add }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await checkInset(page, en.adminTeachers.create.timeZone)
})

test('[web-client/R13-S2] Long option does not run under the arrow', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a teacher with a long time zone on a 360-pixel screen
  await page.setViewportSize({ width: 360, height: 800 })
  await createTeacherAndLoginInUi(
    page,
    playwright,
    baseURL,
    'Anna',
    'teacher-pass-1',
    LONG_ZONE,
  )

  // when /profile is opened
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)
  const select = page.getByLabel(en.profile.timeZone)
  await expect(select).toHaveValue(LONG_ZONE)

  // then the select is as wide as the name field, its text area ends before the arrow
  const style = await styleOf(select)
  const s = await box(select)
  const n = await box(page.getByLabel(en.profile.name))
  expect(Math.abs(s.width - n.width)).toBeLessThanOrEqual(1)
  const a = await box(arrowOf(page, en.profile.timeZone))
  const textRight = s.x + s.width - style.borderRight - style.paddingRight
  expect(textRight).toBeLessThanOrEqual(a.x)
  expect(style.textOverflow).toBe('ellipsis')
  expect(style.scrollWidth).toBeLessThanOrEqual(style.clientWidth)

  // and the page has no horizontal scroll
  const fits = await page.evaluate(
    () =>
      document.documentElement.scrollWidth <=
      document.documentElement.clientWidth,
  )
  expect(fits).toBe(true)
})

test('[web-client/R13-S3] Click on the arrow reaches the select', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a logged-in teacher on /profile
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)
  const arrow = arrowOf(page, en.profile.timeZone)
  await expect(arrow).toBeVisible()
  const a = await box(arrow)
  const x = a.x + a.width / 2
  const y = a.y + a.height / 2

  // when the teacher clicks in the middle of the arrow
  await page.mouse.click(x, y)

  // then the click lands on the select, and the select has the focus
  const hit = await page.evaluate(
    ([px, py]) => document.elementFromPoint(px, py)?.id ?? '',
    [x, y],
  )
  const selectId = await page
    .getByLabel(en.profile.timeZone)
    .evaluate((element) => element.id)
  expect(hit).toBe(selectId)
  await expect(page.getByLabel(en.profile.timeZone)).toBeFocused()
})

test('[web-client/R13-S4] Disabled select keeps the arrow', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a logged-in teacher on /profile, and the arrow's position
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await page.getByRole('button', { name: en.header.profile }).click()
  await expect(page).toHaveURL(/\/profile$/)
  const arrow = arrowOf(page, en.profile.timeZone)
  await expect(arrow).toBeVisible()
  const before = await box(arrow)

  // when a profile save is still running
  let release: () => void = () => {}
  const held = new Promise<void>((resolve) => {
    release = resolve
  })
  await page.route('**/api/me', async (route) => {
    if (route.request().method() === 'GET') {
      await route.continue()
      return
    }
    await held
    await route.continue()
  })
  await page.getByRole('button', { name: en.profile.save }).click()
  await expect(page.getByLabel(en.profile.timeZone)).toBeDisabled()

  // then the arrow is visible at the same position
  await expect(arrow).toBeVisible()
  const after = await box(arrow)
  expect(Math.abs(after.x - before.x)).toBeLessThanOrEqual(1)
  expect(Math.abs(after.y - before.y)).toBeLessThanOrEqual(1)
  release()
})
