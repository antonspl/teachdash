import { expect, test, type Locator, type Page } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import ru from '../src/i18n/ru.json' with { type: 'json' }
import { createTeacherAndLoginInUi, loginAsAdminInUi } from './accounts'

const STORAGE_KEY = 'teachdash.lang'

async function useRussian(page: Page) {
  await page.evaluate(([key]) => localStorage.setItem(key, 'ru'), [STORAGE_KEY])
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('lang', 'ru')
}

/** The computed colour of a token, as the browser writes it (for example rgb(1, 2, 3)). */
function colourOf(page: Page, token: string) {
  return page.evaluate((name) => {
    const probe = document.createElement('span')
    probe.style.color = `var(${name})`
    document.body.append(probe)
    const colour = getComputedStyle(probe).color
    probe.remove()
    return colour
  }, token)
}

function ringAndBorder(field: Locator) {
  return field.evaluate((element) => {
    const style = getComputedStyle(element)
    return {
      borderWidth: style.borderTopWidth,
      borderColour: style.borderTopColor,
      outlineWidth: style.outlineWidth,
      outlineStyle: style.outlineStyle,
      outlineColour: style.outlineColor,
    }
  })
}

async function expectBorderAndRing(page: Page, field: Locator) {
  await expect(field).toBeFocused()
  // the border colour eases in over --duration-fast, so wait for its end
  await expect(field).toHaveCSS(
    'border-top-color',
    await colourOf(page, '--color-danger-text'),
  )
  const look = await ringAndBorder(field)
  expect(look.borderWidth).toBe('2px')
  expect(look.borderColour).toBe(await colourOf(page, '--color-danger-text'))
  expect(look.outlineWidth).toBe('2px')
  expect(look.outlineStyle).toBe('solid')
  expect(look.outlineColour).toBe(await colourOf(page, '--color-focus-ring'))
  expect(look.outlineColour).not.toBe(look.borderColour)
}

test('[web-client/R30-S2] A focused invalid field shows the red border and the ring', async ({
  page,
}) => {
  // given the set-password page with "abc" typed in the focused new password field
  await page.goto('/set-password#token=abc')
  const field = page.getByLabel(en.setPassword.password, { exact: true })
  await field.fill('abc')
  await expect(page.getByText(en.password.tooShort)).toBeVisible()

  // when the computed style is read, then the border is 2px red and the ring is 2px accent
  await expectBorderAndRing(page, field)
})

test('[web-client/R30-S2] The profile form keeps the border and the ring on a wrong current password', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a teacher whose wrong current password was just refused
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await page.getByRole('button', { name: en.header.profile }).click()
  const current = page.getByLabel(en.profile.currentPassword, { exact: true })
  await current.fill('wrong-pass-1')
  await page
    .getByLabel(en.profile.newPassword, { exact: true })
    .fill('teacher-pass-2')
  await page
    .getByLabel(en.profile.repeatPassword, { exact: true })
    .fill('teacher-pass-2')
  await page.getByRole('button', { name: en.profile.changePassword }).click()
  await expect(page.getByText(en.profile.wrongPassword)).toBeVisible()

  // when the focused field is read, then both the border and the ring show
  await expectBorderAndRing(page, current)
})

async function expectHintLooksLikeError(
  page: Page,
  field: Locator,
  hint: string,
) {
  await expect(field).toHaveAttribute('aria-invalid', 'true')
  await expect(field).toHaveAccessibleDescription(hint)
  await expect(field).toHaveCSS('border-top-width', '2px')
  await expect(field).toHaveCSS(
    'border-top-color',
    await colourOf(page, '--color-danger-text'),
  )
  const message = page.getByText(hint, { exact: true })
  await expect(message.locator('svg')).toHaveCount(1)
  await expect(message).not.toHaveAttribute('role', /.+/)
}

test('[web-client/R30-S6] The set-password hints look like errors', async ({
  page,
}) => {
  // given the set-password page
  await page.goto('/set-password#token=abc')
  const next = page.getByLabel(en.setPassword.password, { exact: true })
  const repeat = page.getByLabel(en.setPassword.confirm, { exact: true })

  // when a 3-character password is typed, then a repeat that differs
  await next.fill('abc')
  // then the too-short hint is shown as an error
  await expectHintLooksLikeError(page, next, en.password.tooShort)
  await next.fill('long-enough-1')
  await repeat.fill('different-1')
  // and so is the mismatch hint
  await expectHintLooksLikeError(page, repeat, en.password.mismatch)
})

test('[web-client/R30-S6] The profile password form hints look like errors', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a teacher on /profile
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await page.getByRole('button', { name: en.header.profile }).click()
  const next = page.getByLabel(en.profile.newPassword, { exact: true })
  const repeat = page.getByLabel(en.profile.repeatPassword, { exact: true })

  // when a 3-character new password is typed, then a repeat that differs
  await next.fill('abc')
  await expectHintLooksLikeError(page, next, en.password.tooShort)
  await next.fill('long-enough-1')
  await repeat.fill('different-1')
  await expectHintLooksLikeError(page, repeat, en.password.mismatch)
})

/** The first text line's box of the message, and the icon's box. */
function messageGeometry(message: Locator) {
  return message.evaluate((paragraph) => {
    const icon = paragraph.querySelector('svg')!.getBoundingClientRect()
    const text = Array.from(paragraph.childNodes).find(
      (node) => node.nodeType === Node.TEXT_NODE,
    )!
    const range = document.createRange()
    range.selectNodeContents(text)
    const lines = Array.from(range.getClientRects())
    return {
      icon: {
        left: icon.left,
        right: icon.right,
        top: icon.top,
        bottom: icon.bottom,
      },
      lines: lines.map((line) => ({
        left: line.left,
        right: line.right,
        top: line.top,
        bottom: line.bottom,
      })),
      scrollWidth: paragraph.scrollWidth,
      clientWidth: paragraph.clientWidth,
    }
  })
}

test('[web-client/R30-S4] The error icon is centred on the first text line', async ({
  page,
}) => {
  // given the set-password page with a hint under the new password field
  await page.goto('/set-password#token=abc')
  await page.getByLabel(en.setPassword.password, { exact: true }).fill('abc')
  const message = page.getByText(en.password.tooShort)
  await expect(message).toBeVisible()

  // when the icon and the first line are measured
  const shape = await messageGeometry(message)
  const first = shape.lines[0]

  // then the icon's vertical centre is within the first line's box, and within a pixel of its centre
  const centre = (shape.icon.top + shape.icon.bottom) / 2
  expect(centre).toBeGreaterThanOrEqual(first.top)
  expect(centre).toBeLessThanOrEqual(first.bottom)
  expect(Math.abs(centre - (first.top + first.bottom) / 2)).toBeLessThanOrEqual(
    1,
  )

  // and the text starts to the right of the icon
  expect(first.left).toBeGreaterThan(shape.icon.right)
})

function controlShape(control: Locator) {
  return control.evaluate((element) => {
    const style = getComputedStyle(element)
    const box = element.getBoundingClientRect()
    const arrow = element.parentElement?.querySelector('[data-select-arrow]')
    const arrowBox = arrow?.getBoundingClientRect()
    return {
      height: box.height,
      textStart:
        box.left +
        parseFloat(style.borderLeftWidth) +
        parseFloat(style.paddingLeft),
      arrowLeft: arrowBox?.left ?? null,
    }
  })
}

function expectSame(
  before: Awaited<ReturnType<typeof controlShape>>,
  after: Awaited<ReturnType<typeof controlShape>>,
) {
  expect(Math.abs(after.height - before.height)).toBeLessThanOrEqual(1)
  expect(Math.abs(after.textStart - before.textStart)).toBeLessThanOrEqual(1)
  if (before.arrowLeft !== null && after.arrowLeft !== null) {
    expect(Math.abs(after.arrowLeft - before.arrowLeft)).toBeLessThanOrEqual(1)
  }
}

test('[web-client/R30-S5] A long Russian error wraps under its text in the dialog at 360 and moves nothing', async ({
  page,
}) => {
  // given an admin on a 360-pixel screen in Russian, and a teacher whose email is taken
  await page.setViewportSize({ width: 360, height: 800 })
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const email = `taken-${unique}@example.com`
  await loginAsAdminInUi(page)
  await useRussian(page)
  const dialog = page.getByRole('dialog')
  async function fill(name: string) {
    await page.getByRole('button', { name: ru.adminTeachers.add }).click()
    await expect(dialog).toBeVisible()
    await dialog.getByLabel(ru.adminTeachers.create.name).fill(name)
    await dialog.getByLabel(ru.adminTeachers.create.email).fill(email)
    await dialog
      .getByLabel(ru.adminTeachers.create.nativeLanguage)
      .selectOption('de')
  }
  await fill('Taken Owner')
  await dialog
    .getByRole('button', { name: ru.adminTeachers.create.submit })
    .click()
  await expect(dialog).toBeHidden()
  await fill('Анна')
  const emailField = dialog.getByLabel(ru.adminTeachers.create.email)
  const before = await controlShape(emailField)

  // when the email is refused
  await dialog
    .getByRole('button', { name: ru.adminTeachers.create.submit })
    .click()
  const message = dialog.getByText(ru.errors.EMAIL_TAKEN)
  await expect(message).toBeVisible()

  // then the message wraps onto more lines, all starting at one left edge right of the icon
  const shape = await messageGeometry(message)
  const tops = new Set(shape.lines.map((line) => Math.round(line.top)))
  expect(tops.size).toBeGreaterThan(1)
  const left = Math.min(...shape.lines.map((line) => line.left))
  expect(left).toBeGreaterThan(shape.icon.right)
  for (const line of shape.lines.filter((line) => line.right - line.left > 0)) {
    // a line that is not the last starts at the same edge; the last may be shorter but not left of it
    expect(line.left).toBeGreaterThanOrEqual(left - 1)
  }

  // and nothing is cut, and the page does not scroll sideways
  expect(shape.scrollWidth).toBeLessThanOrEqual(shape.clientWidth)
  const fits = await page.evaluate(
    () =>
      document.documentElement.scrollWidth <=
      document.documentElement.clientWidth,
  )
  expect(fits).toBe(true)

  // and the input's height and text start are the same as before the error
  expectSame(before, await controlShape(emailField))
})

test('[web-client/R30-S5] An error under the time zone select at 360 moves neither its text nor its arrow', async ({
  page,
  playwright,
  baseURL,
}) => {
  // given a teacher on /profile on a 360-pixel screen in Russian
  await page.setViewportSize({ width: 360, height: 800 })
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Anna')
  await useRussian(page)
  await page.getByRole('button', { name: ru.header.profile }).click()
  const select = page.getByLabel(ru.profile.timeZone, { exact: true })
  await expect(select).toBeVisible()
  const before = await controlShape(select)
  await page.route('**/api/me', async (route) => {
    if (route.request().method() === 'GET') {
      await route.continue()
      return
    }
    await route.fulfill({
      status: 400,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        status: 400,
        code: 'VALIDATION_FAILED',
        errors: [{ field: 'timeZone', code: 'INVALID_FORMAT' }],
      }),
    })
  })

  // when the save is refused with a time zone error
  await page.getByRole('button', { name: ru.profile.save }).click()
  const message = page.getByText(ru.fieldErrors.INVALID_FORMAT)
  await expect(message).toBeVisible()

  // then the message is whole and starts right of the icon
  const shape = await messageGeometry(message)
  expect(Math.min(...shape.lines.map((line) => line.left))).toBeGreaterThan(
    shape.icon.right,
  )
  expect(shape.scrollWidth).toBeLessThanOrEqual(shape.clientWidth)

  // and the select's height, text start and arrow are where they were
  expectSame(before, await controlShape(select))
  await page.unrouteAll({ behavior: 'ignoreErrors' })
})
