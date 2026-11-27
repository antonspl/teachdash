import { expect, test } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }

test('[web-client/R30-S1] Every focused element of the login page has the 2px solid ring at a 2px offset', async ({
  page,
  isMobile,
}) => {
  // given the login page, with the keyboard moved by Tab
  if (isMobile) {
    await page.setViewportSize({ width: 360, height: 800 })
  }
  await page.goto('/login')
  await expect(
    page.getByRole('heading', { name: en.login.title }),
  ).toBeVisible()
  const email = page.getByLabel(en.login.email)
  const password = page.getByLabel(en.login.password, { exact: true })
  const submit = page.getByRole('button', { name: en.login.submit })
  const toggle = page.getByRole('button', { name: /Show/ })

  // when each of the focusable elements is focused by the keyboard
  const rings = []
  await page.keyboard.press('Tab')
  for (let step = 0; step < 12; step++) {
    const focused = page.locator(':focus')
    if (!(await focused.count())) break
    const known =
      (await email.evaluate((node) => node === document.activeElement)) ||
      (await password.evaluate((node) => node === document.activeElement)) ||
      (await toggle.evaluate((node) => node === document.activeElement)) ||
      (await submit.evaluate((node) => node === document.activeElement))
    if (known) {
      rings.push(
        await focused.evaluate((node) => {
          const style = getComputedStyle(node)
          return [
            style.outlineWidth,
            style.outlineStyle,
            style.outlineOffset,
          ].join(' ')
        }),
      )
    }
    await page.keyboard.press('Tab')
  }

  // then each has a 2 pixel solid outline at an offset of 2 pixels
  expect(rings.length).toBeGreaterThanOrEqual(4)
  for (const ring of rings) {
    expect(ring).toBe('2px solid 2px')
  }
})

test('[web-client/R27-S9] The login submit fills the form width on a phone and keeps its natural width on desktop', async ({
  page,
  isMobile,
}) => {
  // given the login page at the project's width
  if (isMobile) {
    await page.setViewportSize({ width: 360, height: 800 })
  }
  await page.goto('/login')
  const submit = page.getByRole('button', { name: en.login.submit })
  const email = page.getByLabel(en.login.email)

  // when the submit and the email input are measured
  const button = (await submit.boundingBox())!
  const input = (await email.boundingBox())!

  // then on a phone the submit is as wide as the input, and on desktop it is narrower
  if (isMobile) {
    expect(Math.abs(button.width - input.width)).toBeLessThanOrEqual(1)
  } else {
    expect(button.width).toBeLessThan(input.width / 2)
  }
})
