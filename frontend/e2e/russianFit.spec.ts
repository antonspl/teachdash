import { expect, test, type Page } from '@playwright/test'
import ru from '../src/i18n/ru.json' with { type: 'json' }
import { createTeacherAndLoginInUi, loginAsAdminInUi } from './accounts'
import { holdRequest } from './hold'

const STORAGE_KEY = 'teachdash.lang'

async function useRussian(page: Page) {
  await page.evaluate(([key]) => localStorage.setItem(key, 'ru'), [STORAGE_KEY])
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('lang', 'ru')
}

// Nothing scrolls sideways and no element is wider than the viewport.
async function expectFits(page: Page, where: string) {
  const result = await page.evaluate(() => {
    const root = document.documentElement
    const wide = Array.from(document.body.querySelectorAll('*'))
      .filter((el) => el.tagName !== 'OPTION')
      .filter((el) => el.getBoundingClientRect().width > root.clientWidth)
      .map((el) => `${el.tagName.toLowerCase()}.${el.className}`)
    return { scroll: root.scrollWidth, client: root.clientWidth, wide }
  })
  expect(result.scroll, `${where}: scrollWidth`).toBeLessThanOrEqual(
    result.client,
  )
  expect(result.wide, `${where}: elements wider than the viewport`).toEqual([])
}

function escapeRegExp(text: string) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function problem(status: number, code: string) {
  return {
    status,
    contentType: 'application/problem+json',
    body: JSON.stringify({ status, code }),
  }
}

test('[web-client/R15-S6] Russian pages fit a phone screen: login, set-password, not found and the login banners', async ({
  page,
}) => {
  await page.setViewportSize({ width: 360, height: 800 })
  await page.goto('/login')
  await useRussian(page)
  await expect(
    page.getByRole('heading', { name: ru.login.title }),
  ).toBeVisible()
  await expectFits(page, 'login')

  await page.goto('/set-password#token=abc')
  await expect(
    page.getByRole('heading', { name: ru.setPassword.title }),
  ).toBeVisible()
  await page.getByLabel(ru.setPassword.password, { exact: true }).fill('short')
  await expect(page.getByText(ru.password.tooShort)).toBeVisible()
  await expectFits(page, 'set-password with a hint')

  await page.goto('/set-password')
  await expect(page.getByText(ru.setPassword.incompleteLink)).toBeVisible()
  await expectFits(page, 'set-password, incomplete link')

  await page.goto('/nowhere')
  await expect(
    page.getByRole('heading', { name: ru.notFound.title }),
  ).toBeVisible()
  await expectFits(page, 'not found')

  // the server cannot be reached: the session check answers, the reachability check fails
  let calls = 0
  await page.route('**/api/me', (route) =>
    ++calls === 1
      ? route.fulfill(problem(401, 'UNAUTHENTICATED'))
      : route.abort(),
  )
  await page.goto('/login')
  await expect(page.getByText(ru.login.unreachable)).toBeVisible()
  await expectFits(page, 'login, unreachable banner')
  await page.unroute('**/api/me')
})

test('[web-client/R15-S6] Russian pages fit a phone screen: the teacher cabinet and Profile', async ({
  page,
  playwright,
  baseURL,
}) => {
  await page.setViewportSize({ width: 360, height: 800 })
  await createTeacherAndLoginInUi(
    page,
    playwright,
    baseURL,
    'Анна Владимировна Петрова-Сидорова',
  )
  await useRussian(page)
  await expect(
    page.getByRole('heading', { name: ru.cabinet.teacher }),
  ).toBeVisible()
  await expectFits(page, 'teacher cabinet')

  await page.goto('/profile')
  await expect(
    page.getByRole('heading', { name: ru.profile.title }),
  ).toBeVisible()
  await expectFits(page, 'profile')

  await page.route('**/api/me', (route) =>
    route.request().method() === 'PATCH'
      ? route.fulfill(problem(400, 'VALIDATION_FAILED'))
      : route.continue(),
  )
  await page.getByRole('button', { name: ru.profile.save }).click()
  await expect(page.getByText(ru.profile.validationFailedTeacher)).toBeVisible()
  await expectFits(page, 'profile validation banner')

  await page.route('**/api/me/password', (route) =>
    route.fulfill(problem(403, 'WRONG_PASSWORD')),
  )
  await page
    .getByLabel(ru.profile.currentPassword, { exact: true })
    .fill('wrong-password')
  await page
    .getByLabel(ru.profile.newPassword, { exact: true })
    .fill('new-password-1')
  await page
    .getByLabel(ru.profile.repeatPassword, { exact: true })
    .fill('new-password-1')
  await page.getByRole('button', { name: ru.profile.changePassword }).click()
  await expect(page.getByText(ru.profile.wrongPassword)).toBeVisible()
  await expectFits(page, 'profile wrong password banner')
  await page.unroute('**/api/me/password')
  await page.route('**/api/me/password', (route) =>
    route.fulfill(problem(400, 'VALIDATION_FAILED')),
  )
  await page.getByLabel(ru.profile.currentPassword, { exact: true }).fill('x')
  await page
    .getByLabel(ru.profile.newPassword, { exact: true })
    .fill('new-password-1')
  await page
    .getByLabel(ru.profile.repeatPassword, { exact: true })
    .fill('new-password-1')
  await page.getByRole('button', { name: ru.profile.changePassword }).click()
  await expect(
    page.getByText(ru.profile.passwordValidationFailed),
  ).toBeVisible()
  await expectFits(page, 'profile password validation banner')
})

test('[web-client/R15-S6] Russian pages fit a phone screen: the admin page, its dialogs and banners, and 768 px', async ({
  page,
  playwright,
  baseURL,
}) => {
  await page.setViewportSize({ width: 360, height: 800 })
  // a teacher with a password: the row has «Ссылка для сброса пароля»
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const withPassword = `Дарья Сергеевна Ковалёва-Смирнова ${unique}`
  await createTeacherAndLoginInUi(page, playwright, baseURL, withPassword)
  await page.context().clearCookies()
  await loginAsAdminInUi(page)
  await useRussian(page)
  await expect(
    page.getByRole('heading', { name: ru.cabinet.admin }),
  ).toBeVisible()
  const passwordRow = page
    .getByRole('listitem')
    .filter({ hasText: withPassword })
  await expect(
    passwordRow.getByRole('button', {
      name: ru.adminTeachers.actions.resetLink,
    }),
  ).toBeVisible()
  await expectFits(page, 'admin list')

  // the create dialog, with its form banner
  await page.getByRole('button', { name: ru.adminTeachers.add }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await expectFits(page, 'create dialog')
  await dialog
    .getByRole('button', { name: ru.adminTeachers.create.submit })
    .click()
  await expect(dialog.getByText(ru.fieldErrors.REQUIRED).first()).toBeVisible()
  await expectFits(page, 'create dialog with errors')
  const invited = `Борис Александрович Иванов-Петров ${unique}`
  await dialog.getByLabel(ru.adminTeachers.create.name).fill(invited)
  await dialog
    .getByLabel(ru.adminTeachers.create.email)
    .fill(`ru-fit-${unique}@example.com`)
  await dialog
    .getByLabel(ru.adminTeachers.create.nativeLanguage)
    .selectOption('de')
  await page.route('**/api/admin/teachers', (route) =>
    route.request().method() === 'POST'
      ? route.fulfill(problem(400, 'VALIDATION_FAILED'))
      : route.continue(),
  )
  await dialog
    .getByRole('button', { name: ru.adminTeachers.create.submit })
    .click()
  await expect(
    dialog.getByText(ru.adminTeachers.create.validationFailed),
  ).toBeVisible()
  await expectFits(page, 'create dialog with the form banner')
  await page.unroute('**/api/admin/teachers')

  // created: the issued-link box and the longest row
  await dialog
    .getByRole('button', { name: ru.adminTeachers.create.submit })
    .click()
  const newRow = page.getByRole('listitem').filter({ hasText: invited })
  await expect(newRow.getByText(ru.adminTeachers.justAdded)).toBeVisible()
  await expect(
    newRow.getByRole('button', { name: ru.adminTeachers.actions.inviteLink }),
  ).toBeVisible()
  await expectFits(page, 'admin list with the issued link')
  await newRow.getByRole('button', { name: ru.issuedLink.copy }).click()
  await expect(
    newRow.getByText(
      new RegExp(
        `${escapeRegExp(ru.issuedLink.copied)}|Не удалось скопировать`,
      ),
    ),
  ).toBeVisible()
  await expectFits(page, 'issued link banner')

  // the reset link of the teacher with a password
  await passwordRow
    .getByRole('button', { name: ru.adminTeachers.actions.resetLink })
    .click()
  await expect(
    passwordRow.getByRole('button', { name: ru.issuedLink.copy }),
  ).toBeVisible()
  await expectFits(page, 'admin list with the reset link')

  // the deactivate dialog
  await passwordRow
    .getByRole('button', { name: ru.adminTeachers.actions.deactivate })
    .click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await expectFits(page, 'deactivate dialog')
  await page
    .getByRole('button', { name: ru.adminTeachers.deactivate.cancel })
    .click()

  // 768 px: the list with its two longest rows
  await page.setViewportSize({ width: 768, height: 900 })
  await expect(newRow.getByText(ru.adminTeachers.justAdded)).toBeVisible()
  await expectFits(page, 'admin list at 768 px')
  await page.setViewportSize({ width: 360, height: 800 })

  // the admin's own Profile
  await page.goto('/profile')
  await expect(
    page.getByRole('heading', { name: ru.profile.title }),
  ).toBeVisible()
  await expectFits(page, 'admin profile')
  await page.goto('/admin')
  await expect(
    page.getByRole('heading', { name: ru.cabinet.admin }),
  ).toBeVisible()

  // the load error banner
  await page.route('**/api/admin/teachers', (route) =>
    route.fulfill(problem(500, 'INTERNAL_ERROR')),
  )
  await page.reload()
  await expect(page.getByText(ru.errors.INTERNAL_ERROR)).toBeVisible()
  await expect(page.getByRole('button', { name: ru.ui.tryAgain })).toBeVisible()
  await expectFits(page, 'admin load error')

  // the session ended: the login page with its banner
  await page.unroute('**/api/admin/teachers')
  await page.route('**/api/admin/teachers', (route) =>
    route.fulfill(problem(401, 'UNAUTHENTICATED')),
  )
  await page.reload()
  await expect(page.getByText(ru.login.sessionEnded)).toBeVisible()
  await expectFits(page, 'login, session ended banner')
})

test('[web-client/R29-S6] Russian busy labels fit a phone screen: login, set-password and the profile forms', async ({
  page,
  playwright,
  baseURL,
}) => {
  await page.setViewportSize({ width: 360, height: 800 })

  await page.goto('/login')
  await useRussian(page)
  await page.getByLabel(ru.login.email).fill('nobody@example.com')
  await page.getByLabel(ru.login.password, { exact: true }).fill('wrong-pass-1')
  const login = await holdRequest(page, '**/api/auth/login', 'POST')
  await page.getByRole('button', { name: ru.login.submit }).click()
  await expect(
    page.getByRole('button', { name: ru.login.submitting }),
  ).toBeDisabled()
  await expectFits(page, 'login, busy')
  login.release()
  await login.stop()

  await page.goto('/set-password#token=abc')
  await page
    .getByLabel(ru.setPassword.password, { exact: true })
    .fill('long-enough-1')
  await page
    .getByLabel(ru.setPassword.confirm, { exact: true })
    .fill('long-enough-1')
  const setPassword = await holdRequest(page, '**/api/password-links', 'POST')
  await page.getByRole('button', { name: ru.setPassword.submit }).click()
  await expect(
    page.getByRole('button', { name: ru.setPassword.submitting }),
  ).toBeDisabled()
  await expectFits(page, 'set-password, busy')
  setPassword.release()
  await setPassword.stop()

  // the helper logs in with the English labels
  await page.evaluate(([key]) => localStorage.removeItem(key), [STORAGE_KEY])
  await createTeacherAndLoginInUi(
    page,
    playwright,
    baseURL,
    'Анна Владимировна Петрова-Сидорова',
  )
  await page.goto('/profile')
  await useRussian(page)
  const save = await holdRequest(page, '**/api/me', 'PATCH')
  await page.getByRole('button', { name: ru.profile.save }).click()
  await expect(
    page.getByRole('button', { name: ru.profile.saving }),
  ).toBeDisabled()
  await expectFits(page, 'profile Save, busy')
  save.release()
  await save.stop()
  await expect(
    page.getByRole('button', { name: ru.profile.save }),
  ).toBeEnabled()

  await page
    .getByLabel(ru.profile.currentPassword, { exact: true })
    .fill('teacher-pass-1')
  await page
    .getByLabel(ru.profile.newPassword, { exact: true })
    .fill('new-password-1')
  await page
    .getByLabel(ru.profile.repeatPassword, { exact: true })
    .fill('new-password-1')
  const change = await holdRequest(page, '**/api/me/password', 'POST')
  await page.getByRole('button', { name: ru.profile.changePassword }).click()
  await expect(
    page.getByRole('button', { name: ru.profile.changingPassword }),
  ).toBeDisabled()
  await expectFits(page, 'profile Change password, busy')
  change.release()
  await change.stop()
})

test('[web-client/R29-S6] Russian busy labels fit a phone screen: the admin dialogs and every row button', async ({
  page,
  playwright,
  baseURL,
}) => {
  await page.setViewportSize({ width: 360, height: 800 })
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const withPassword = `Дарья Сергеевна Ковалёва-Смирнова ${unique}`
  await createTeacherAndLoginInUi(page, playwright, baseURL, withPassword)
  await page.context().clearCookies()
  await loginAsAdminInUi(page)
  await useRussian(page)
  const passwordRow = page
    .getByRole('listitem')
    .filter({ hasText: withPassword })
  await expect(
    passwordRow.getByRole('button', {
      name: ru.adminTeachers.actions.resetLink,
    }),
  ).toBeVisible()

  // the reset link: the longest Russian busy label
  const reset = await holdRequest(
    page,
    '**/api/admin/users/*/reset-link',
    'POST',
  )
  await passwordRow
    .getByRole('button', { name: ru.adminTeachers.actions.resetLink })
    .click()
  await expect(
    passwordRow.getByRole('button', { name: ru.adminTeachers.busy.resetLink }),
  ).toBeDisabled()
  await expectFits(page, 'row, reset link busy')
  reset.release()
  await reset.stop()
  await expect(
    passwordRow.getByRole('button', {
      name: ru.adminTeachers.actions.resetLink,
    }),
  ).toBeEnabled()

  // the create dialog
  await page.getByRole('button', { name: ru.adminTeachers.add }).click()
  const dialog = page.getByRole('dialog')
  const invited = `Борис Александрович Иванов-Петров ${unique}`
  await dialog.getByLabel(ru.adminTeachers.create.name).fill(invited)
  await dialog
    .getByLabel(ru.adminTeachers.create.email)
    .fill(`ru-busy-${unique}@example.com`)
  await dialog
    .getByLabel(ru.adminTeachers.create.nativeLanguage)
    .selectOption('de')
  const create = await holdRequest(page, '**/api/admin/teachers', 'POST')
  await dialog
    .getByRole('button', { name: ru.adminTeachers.create.submit })
    .click()
  await expect(
    dialog.getByRole('button', { name: ru.adminTeachers.create.submitting }),
  ).toBeDisabled()
  await expectFits(page, 'create dialog, busy')
  create.release()
  await create.stop()
  await expect(
    dialog.getByRole('button', { name: ru.adminTeachers.create.submit }),
  ).toBeEnabled()

  // created for real: the new teacher has no password, so the row has the invite link
  await dialog
    .getByRole('button', { name: ru.adminTeachers.create.submit })
    .click()
  const newRow = page.getByRole('listitem').filter({ hasText: invited })
  await expect(
    newRow.getByRole('button', { name: ru.adminTeachers.actions.inviteLink }),
  ).toBeVisible()
  const invite = await holdRequest(
    page,
    '**/api/admin/users/*/reset-link',
    'POST',
  )
  await newRow
    .getByRole('button', { name: ru.adminTeachers.actions.inviteLink })
    .click()
  await expect(
    newRow.getByRole('button', { name: ru.adminTeachers.busy.inviteLink }),
  ).toBeDisabled()
  await expectFits(page, 'row, invite link busy')
  invite.release()
  await invite.stop()

  // the deactivate dialog, then the reactivate button of the deactivated row
  const deactivate = await holdRequest(
    page,
    '**/api/admin/teachers/*/deactivate',
    'POST',
  )
  await passwordRow
    .getByRole('button', { name: ru.adminTeachers.actions.deactivate })
    .click()
  await page
    .getByRole('dialog')
    .getByRole('button', { name: ru.adminTeachers.deactivate.confirm })
    .click()
  await expect(
    page.getByRole('button', { name: ru.adminTeachers.deactivate.confirming }),
  ).toBeDisabled()
  await expectFits(page, 'deactivate dialog, busy')
  deactivate.release()
  await deactivate.stop()
  await expect(page.getByRole('dialog')).toBeHidden()

  await passwordRow
    .getByRole('button', { name: ru.adminTeachers.actions.deactivate })
    .click()
  await page
    .getByRole('dialog')
    .getByRole('button', { name: ru.adminTeachers.deactivate.confirm })
    .click()
  await expect(
    passwordRow.getByText(ru.adminTeachers.status.DEACTIVATED),
  ).toBeVisible()
  const reactivate = await holdRequest(
    page,
    '**/api/admin/teachers/*/reactivate',
    'POST',
  )
  await passwordRow
    .getByRole('button', { name: ru.adminTeachers.actions.reactivate })
    .click()
  await expect(
    passwordRow.getByRole('button', { name: ru.adminTeachers.busy.reactivate }),
  ).toBeDisabled()
  await expectFits(page, 'row, reactivate busy')
  reactivate.release()
  await reactivate.stop()
})
