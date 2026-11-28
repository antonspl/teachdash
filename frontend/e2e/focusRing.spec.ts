import { expect, test, type Locator } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }

// Playwright runs this test in Node. This is the only Node API it needs.
declare const process: { platform: string }

test('[web-client/R30-S1] Each login control and the not-found link has a 2px solid keyboard focus ring at a 2px offset', async ({
  page,
  isMobile,
  browserName,
}) => {
  // given the login page at the project's width
  if (isMobile) {
    await page.setViewportSize({ width: 360, height: 800 })
  }
  await page.goto('/login')
  await expect(
    page.getByRole('heading', { name: en.login.title }),
  ).toBeVisible()
  const loginControls = [
    page.getByRole('combobox', { name: en.uiLanguage.label }),
    page.getByLabel(en.login.email, { exact: true }),
    page.getByLabel(en.login.password, { exact: true }),
    page.getByRole('button', {
      name: en.ui.showPassword.replace('{{label}}', en.login.password),
    }),
    page.getByRole('button', { name: en.login.submit }),
  ]

  // macOS WebKit needs Option+Tab to visit all controls and links.
  const isMacWebKit = browserName === 'webkit' && process.platform === 'darwin'
  const tab = isMacWebKit ? 'Alt+Tab' : 'Tab'
  async function expectRingAfterTab(control: Locator) {
    await page.keyboard.press(tab)
    await expect(control).toBeFocused()
    await expect(control).toHaveCSS('outline-width', '2px')
    await expect(control).toHaveCSS('outline-style', 'solid')
    await expect(control).toHaveCSS('outline-offset', '2px')
  }

  // when the keyboard reaches each control, then its computed focus ring is visible
  for (const control of loginControls) {
    await expectRingAfterTab(control)
  }

  // then the existing link on the not-found page has the same keyboard focus ring
  await page.goto('/no-such-page')
  const homeLink = page.getByRole('link', { name: en.notFound.home })
  await expect(homeLink).toBeVisible()
  await expectRingAfterTab(homeLink)
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
