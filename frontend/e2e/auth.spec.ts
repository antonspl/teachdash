import { expect, test, type Page } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { createTeacher, linkPath } from './accounts'

const TEACHER_PASSWORD = 'teacher-pass-1'

async function setPasswordOnLinkPage(page: Page, link: string) {
  await page.goto(linkPath(link))
  await page
    .getByLabel(en.setPassword.password, { exact: true })
    .fill(TEACHER_PASSWORD)
  await page
    .getByLabel(en.setPassword.confirm, { exact: true })
    .fill(TEACHER_PASSWORD)
  await page.getByRole('button', { name: en.setPassword.submit }).click()
}

async function logIn(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.getByLabel(en.login.email).fill(email)
  await page.getByLabel(en.login.password, { exact: true }).fill(password)
  await page.getByRole('button', { name: en.login.submit }).click()
}

test.describe('with a teacher created by the admin', () => {
  test('[web-auth/R5-S1] Valid link sets the password and opens the cabinet', async ({
    page,
    playwright,
    baseURL,
  }) => {
    const admin = await playwright.request.newContext({ baseURL })
    const teacher = await createTeacher(admin)
    await admin.dispose()

    await setPasswordOnLinkPage(page, teacher.setPasswordLink)

    await expect(page).toHaveURL(/\/teacher$/)
    await expect(
      page.getByRole('heading', { name: en.cabinet.teacher }),
    ).toBeVisible()

    // The teacher can later log in with the new password.
    await page.context().clearCookies()
    await logIn(page, teacher.email, TEACHER_PASSWORD)
    await expect(page).toHaveURL(/\/teacher$/)
    await expect(
      page.getByRole('heading', { name: en.cabinet.teacher }),
    ).toBeVisible()
  })

  test('[web-auth/R1-S1] Valid credentials open the cabinet', async ({
    page,
    playwright,
    baseURL,
  }) => {
    const admin = await playwright.request.newContext({ baseURL })
    const teacher = await createTeacher(admin, 'Login Teacher')
    // Set the password through the API, so this test does not depend on the page above.
    const link = new URL(teacher.setPasswordLink)
    const token = new URLSearchParams(link.hash.slice(1)).get('token')
    await admin.dispose()
    const anonymous = await playwright.request.newContext({ baseURL })
    await anonymous.get('/api/me')
    const xsrf = (await anonymous.storageState()).cookies.find(
      (cookie) => cookie.name === 'XSRF-TOKEN',
    )?.value
    const set = await anonymous.post('/api/password-links', {
      headers: { 'X-XSRF-TOKEN': xsrf! },
      data: { token, password: TEACHER_PASSWORD },
    })
    expect(set.status()).toBe(200)
    await anonymous.dispose()

    await logIn(page, teacher.email, TEACHER_PASSWORD)

    await expect(page).toHaveURL(/\/teacher$/)
    await expect(
      page.getByRole('heading', { name: en.cabinet.teacher }),
    ).toBeVisible()
  })

  test('[web-auth/R4-S1] Logout ends the session', async ({
    page,
    playwright,
    baseURL,
  }) => {
    const admin = await playwright.request.newContext({ baseURL })
    const teacher = await createTeacher(admin, 'Logout Teacher')
    await admin.dispose()
    await setPasswordOnLinkPage(page, teacher.setPasswordLink)
    await expect(page).toHaveURL(/\/teacher$/)

    await page.getByRole('button', { name: en.header.logout }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(
      page.getByRole('heading', { name: en.login.title }),
    ).toBeVisible()
    await page.goto('/teacher')
    await expect(page).toHaveURL(/\/login$/)
    await expect(
      page.getByRole('heading', { name: en.login.title }),
    ).toBeVisible()
  })

  test('[web-auth/R5-S10] A logged-in admin uses a teacher link', async ({
    page,
  }) => {
    // page.request shares the page's cookies, so this logs the admin in the browser.
    const teacher = await createTeacher(page.request, 'Linked Teacher')
    await page.goto('/admin')
    await expect(
      page.getByRole('heading', { name: en.cabinet.admin }),
    ).toBeVisible()

    await setPasswordOnLinkPage(page, teacher.setPasswordLink)

    await expect(page).toHaveURL(/\/teacher$/)
    await expect(
      page.getByRole('heading', { name: en.cabinet.teacher }),
    ).toBeVisible()
    await expect(page.getByText(teacher.name)).toBeVisible()
    // The admin's session has ended: the admin cabinet is not available.
    await page.goto('/admin')
    await expect(page).toHaveURL(/\/teacher$/)
  })
})
