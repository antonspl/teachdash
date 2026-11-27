import {
  expect,
  test,
  type Locator,
  type Page,
  type TestInfo,
} from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import ru from '../src/i18n/ru.json' with { type: 'json' }
import { createTeacherAndLoginInUi, loginAsAdminInUi } from './accounts'
import { holdRequest } from './hold'

const STORAGE_KEY = 'teachdash.lang'
const LANGUAGES = [
  ['en', en],
  ['ru', ru],
] as const

async function switchLanguage(page: Page, code: 'en' | 'ru') {
  await page.evaluate(
    ([key, value]) => localStorage.setItem(key, value),
    [STORAGE_KEY, code],
  )
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('lang', code)
}

// The 360-pixel phone project is the narrow run; the desktop project keeps its window.
async function narrowOnPhone(page: Page, testInfo: TestInfo) {
  if (testInfo.project.name.startsWith('phone')) {
    await page.setViewportSize({ width: 360, height: 800 })
  }
}

async function sizeOf(button: Locator) {
  const box = await button.boundingBox()
  expect(box, 'the button is on screen').not.toBeNull()
  return { width: box!.width, height: box!.height }
}

// Presses `idle`, waits for the busy label on the same button, and compares the boxes.
async function expectSameBox(
  page: Page,
  idleName: string,
  busyName: string,
  where: string,
  within: Page | Locator = page,
) {
  const idle = within.getByRole('button', { name: idleName, exact: true })
  const before = await sizeOf(idle)
  await idle.click()
  const busy = within.getByRole('button', { name: busyName, exact: true })
  await expect(busy).toBeDisabled()
  await expect(busy).toHaveAttribute('aria-busy', 'true')
  const during = await sizeOf(busy)
  expect(during.width, `${where}: width`).toBeCloseTo(before.width, 1)
  expect(during.height, `${where}: height`).toBeCloseTo(before.height, 1)
  expect(
    before.width,
    `${where}: the button is not zero width`,
  ).toBeGreaterThan(0)
}

test('[web-client/R29-S2] The login button keeps its width and height while the request runs (en and ru, desktop only)', async ({
  page,
}, testInfo) => {
  test.skip(
    testInfo.project.name !== 'desktop-chromium',
    'on a phone the login button is full width, a different box',
  )
  for (const [code, t] of LANGUAGES) {
    await page.goto('/login')
    await switchLanguage(page, code)
    await page.getByLabel(t.login.email).fill('nobody@example.com')
    await page
      .getByLabel(t.login.password, { exact: true })
      .fill('wrong-pass-1')
    const hold = await holdRequest(page, '**/api/auth/login', 'POST')
    await expectSameBox(
      page,
      t.login.submit,
      t.login.submitting,
      `login ${code}`,
    )
    hold.release()
    await expect(
      page.getByRole('button', { name: t.login.submit, exact: true }),
    ).toBeEnabled()
    await hold.stop()
  }
})

test('[web-client/R29-S2] The profile Save and Change password buttons keep their box while the request runs (en and ru; 360 on the phone project)', async ({
  page,
  playwright,
  baseURL,
}, testInfo) => {
  await narrowOnPhone(page, testInfo)
  await createTeacherAndLoginInUi(page, playwright, baseURL, 'Busy Profile')
  await page.goto('/profile')
  for (const [code, t] of LANGUAGES) {
    await switchLanguage(page, code)
    await expect(
      page.getByRole('heading', { name: t.profile.title }),
    ).toBeVisible()
    const save = await holdRequest(page, '**/api/me', 'PATCH')
    await expectSameBox(
      page,
      t.profile.save,
      t.profile.saving,
      `profile Save ${code}`,
    )
    save.release()
    await expect(
      page.getByRole('button', { name: t.profile.save, exact: true }),
    ).toBeEnabled()
    await save.stop()

    await page
      .getByLabel(t.profile.currentPassword, { exact: true })
      .fill('teacher-pass-1')
    await page
      .getByLabel(t.profile.newPassword, { exact: true })
      .fill('teacher-pass-2')
    await page
      .getByLabel(t.profile.repeatPassword, { exact: true })
      .fill('teacher-pass-2')
    const change = await holdRequest(page, '**/api/me/password', 'POST')
    await expectSameBox(
      page,
      t.profile.changePassword,
      t.profile.changingPassword,
      `profile Change password ${code}`,
    )
    change.release()
    await expect(
      page.getByRole('button', {
        name: t.profile.changePassword,
        exact: true,
      }),
    ).toBeEnabled()
    await change.stop()
  }
})

test('[web-client/R29-S2] Create teacher and a row button keep their box while the request runs (en and ru; 360 on the phone project)', async ({
  page,
  playwright,
  baseURL,
}, testInfo) => {
  await narrowOnPhone(page, testInfo)
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const name = `Busy Row ${unique}`
  await createTeacherAndLoginInUi(page, playwright, baseURL, name)
  await page.context().clearCookies()
  await loginAsAdminInUi(page)

  for (const [code, t] of LANGUAGES) {
    await switchLanguage(page, code)
    const row = page.getByRole('listitem').filter({ hasText: name })
    await expect(
      row.getByRole('button', { name: t.adminTeachers.actions.resetLink }),
    ).toBeVisible()

    // the row button: the longest Russian label is the busy one
    const link = await holdRequest(
      page,
      '**/api/admin/users/*/reset-link',
      'POST',
    )
    await expectSameBox(
      page,
      t.adminTeachers.actions.resetLink,
      t.adminTeachers.busy.resetLink,
      `row button ${code}`,
      row,
    )
    link.release()
    await expect(
      row.getByRole('button', {
        name: t.adminTeachers.actions.resetLink,
        exact: true,
      }),
    ).toBeEnabled()
    await link.stop()

    // the dialog's submit
    await page.getByRole('button', { name: t.adminTeachers.add }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel(t.adminTeachers.create.name).fill(`Held ${unique}`)
    await dialog
      .getByLabel(t.adminTeachers.create.email)
      .fill(`held-${code}-${unique}@example.com`)
    await dialog
      .getByLabel(t.adminTeachers.create.nativeLanguage)
      .selectOption('de')
    const create = await holdRequest(page, '**/api/admin/teachers', 'POST')
    await expectSameBox(
      page,
      t.adminTeachers.create.submit,
      t.adminTeachers.create.submitting,
      `Create teacher ${code}`,
      dialog,
    )
    create.release()
    await expect(
      dialog.getByRole('button', {
        name: t.adminTeachers.create.submit,
        exact: true,
      }),
    ).toBeEnabled()
    await create.stop()
    await dialog
      .getByRole('button', { name: t.adminTeachers.create.cancel })
      .click()
  }
})
