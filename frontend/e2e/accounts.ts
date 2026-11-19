import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import {
  expect,
  type APIRequestContext,
  type Page,
  type PlaywrightWorkerArgs,
} from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }
import { ACCEPT } from '../src/api/headers'

// @types/node is not installed; this is all the helper needs from it.
declare const process: {
  env: Record<string, string | undefined>
  cwd(): string
}

const REQUIRED = ['ADMIN_EMAIL', 'ADMIN_PASSWORD', 'ADMIN_NAME'] as const

function parseEnvFile(path: string): Record<string, string> {
  const values: Record<string, string> = {}
  if (!existsSync(path)) {
    return values
  }
  for (const line of readFileSync(path, 'utf8').split(/\r?\n/)) {
    const match = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/.exec(line)
    if (!match || line.trimStart().startsWith('#')) {
      continue
    }
    values[match[1]] = match[2].replace(/^(["'])(.*)\1$/, '$2')
  }
  return values
}

/**
 * Admin credentials the way compose resolves them: the environment first, then a root
 * `.env`, then `infra/dev.env`. Playwright runs from `frontend/`.
 */
export function adminCredentials() {
  const root = resolve(process.cwd(), '..')
  const sources = [
    process.env,
    parseEnvFile(resolve(root, '.env')),
    parseEnvFile(resolve(root, 'infra/dev.env')),
  ]
  const value = (name: string) =>
    sources.map((source) => source[name]).find((v) => v) ?? ''
  const missing = REQUIRED.filter((name) => !value(name))
  if (missing.length > 0) {
    throw new Error(
      `E2E needs the admin account the stack was started with, but ${missing.join(', ')} ` +
        'is not set. Set it in the environment, in a root .env or in infra/dev.env.',
    )
  }
  return {
    email: value('ADMIN_EMAIL'),
    password: value('ADMIN_PASSWORD'),
    name: value('ADMIN_NAME'),
  }
}

async function csrfHeaders(request: APIRequestContext) {
  await request.get('/api/me')
  const token = (await request.storageState()).cookies.find(
    (cookie) => cookie.name === 'XSRF-TOKEN',
  )?.value
  expect(token, 'the backend sets the XSRF-TOKEN cookie').toBeTruthy()
  return { Accept: ACCEPT, 'X-XSRF-TOKEN': token! }
}

let counter = 0

/** Logs the request context in as the admin; a page's `request` shares its cookies. */
export async function loginAsAdmin(request: APIRequestContext) {
  const admin = adminCredentials()
  const headers = await csrfHeaders(request)
  const login = await request.post('/api/auth/login', {
    headers,
    data: { email: admin.email, password: admin.password },
  })
  expect(
    login.status(),
    'the admin login (check ADMIN_EMAIL/ADMIN_PASSWORD)',
  ).toBe(200)
}

/** Logs in as the admin and creates a teacher with an email no earlier run used. */
export async function createTeacher(
  request: APIRequestContext,
  name = 'E2E Teacher',
  timeZone = 'UTC',
) {
  await loginAsAdmin(request)
  // The session id changes at login, so the CSRF token may too.
  const afterLogin = await csrfHeaders(request)
  const email = `e2e-${Date.now()}-${counter++}-${Math.random().toString(36).slice(2, 8)}@example.com`
  const created = await request.post('/api/admin/teachers', {
    headers: afterLogin,
    data: { name, email, timeZone },
  })
  expect(created.status(), 'creating a teacher').toBe(201)
  const { setPasswordLink } = (await created.json()) as {
    setPasswordLink: string
  }
  expect(
    setPasswordLink.includes('token='),
    'the set-password link has a token',
  ).toBe(true)
  return { name, email, setPasswordLink }
}

/** The link has the application's base URL; tests open it on the stack under test. */
export function linkPath(setPasswordLink: string) {
  const url = new URL(setPasswordLink)
  return url.pathname + url.search + url.hash
}

/** Logs in as the admin through the login page and waits for the admin cabinet. */
export async function loginAsAdminInUi(page: Page) {
  const admin = adminCredentials()
  await page.goto('/login')
  await page.getByLabel(en.login.email).fill(admin.email)
  await page.getByLabel(en.login.password, { exact: true }).fill(admin.password)
  await page.getByRole('button', { name: en.login.submit }).click()
  await expect(page).toHaveURL(/\/admin$/)
}

/**
 * Creates a teacher with the given name, gives them a password through the API (the
 * token of the set-password link) and logs in through the login page, ending on the
 * teacher cabinet. Returns the teacher's email and the password for later steps.
 */
export async function createTeacherAndLoginInUi(
  page: Page,
  playwright: PlaywrightWorkerArgs['playwright'],
  baseURL: string | undefined,
  name: string,
  password = 'teacher-pass-1',
  timeZone = 'UTC',
) {
  const admin = await playwright.request.newContext({ baseURL })
  const teacher = await createTeacher(admin, name, timeZone)
  await admin.dispose()
  const token = new URLSearchParams(
    new URL(teacher.setPasswordLink).hash.slice(1),
  ).get('token')
  expect(token, 'the set-password link has a token').toBeTruthy()
  // A separate anonymous context, so the page's own cookies stay clean.
  const anonymous = await playwright.request.newContext({ baseURL })
  const set = await anonymous.post('/api/password-links', {
    headers: await csrfHeaders(anonymous),
    data: { token, password },
  })
  expect(set.status(), 'setting the password with the link').toBe(200)
  await anonymous.dispose()

  await page.goto('/login')
  await page.getByLabel(en.login.email).fill(teacher.email)
  await page.getByLabel(en.login.password, { exact: true }).fill(password)
  await page.getByRole('button', { name: en.login.submit }).click()
  await expect(page).toHaveURL(/\/teacher$/)
  return { email: teacher.email, password }
}
