import { expect, test, type Page, type Request } from '@playwright/test'
import { ACCEPT } from '../src/api/headers'
import en from '../src/i18n/en.json' with { type: 'json' }
import { loginAsAdmin } from './accounts'

const PHONE_WIDTH = 360

function expectSecurityHeaders(headers: Record<string, string>) {
  expect(headers['content-security-policy']).toContain("default-src 'self'")
  expect(headers['content-security-policy']).toContain("frame-ancestors 'none'")
  expect(headers['x-content-type-options']).toBe('nosniff')
  expect(headers['referrer-policy']).toBeTruthy()
}

async function expectIconLoads(page: Page) {
  const href = await page.locator('link[rel="icon"]').getAttribute('href')
  expect(href).toBeTruthy()
  // The browser may not fetch the tab icon in every mode, so load the same file as an
  // image: it must pass the page's own CSP.
  const loaded = await page.evaluate(
    (url) =>
      new Promise<boolean>((resolve) => {
        const image = new Image()
        image.onload = () => resolve(true)
        image.onerror = () => resolve(false)
        image.src = url
      }),
    href!,
  )
  expect(loaded).toBe(true)
}

async function expectFitsPhoneScreen(
  page: Page,
  path: string,
  expectedWidth: number,
) {
  await page.goto(path)
  await expect(page.getByRole('heading')).toBeVisible()

  const layout = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
    innerWidth: window.innerWidth,
  }))

  expect(layout.scrollWidth).toBeLessThanOrEqual(layout.clientWidth)
  expect(layout.innerWidth).toBe(expectedWidth)
}

// The set-password page needs only a token in the fragment to show its form; the cabinet
// needs a login, so the admin account the stack was started with is used for it.
async function expectAllPagesFit(page: Page, expectedWidth: number) {
  await expectFitsPhoneScreen(page, '/login', expectedWidth)
  await expectFitsPhoneScreen(page, '/set-password#token=abc', expectedWidth)
  await loginAsAdmin(page.request)
  await expectFitsPhoneScreen(page, '/admin', expectedWidth)
  await expectFitsPhoneScreen(page, '/no-such-page', expectedWidth)
}

test('[web-client/R11-S1] Root path opens the login page for an anonymous user', async ({
  page,
}) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/login$/)
  await expect(
    page.getByRole('heading', { name: en.login.title }),
  ).toBeVisible()
  await expect(page).toHaveTitle(new RegExp(en.app.name))
})

test('[web-client/R11-S2] Unknown path opened directly shows the not-found page', async ({
  page,
}) => {
  const response = await page.goto('/no-such-page')

  expect(response?.status()).toBe(200)
  expect(response?.headers()['content-type']).toContain('text/html')
  await expect(page.locator('#root')).toBeAttached()
  await expect(
    page.getByRole('heading', { name: en.notFound.title }),
  ).toBeVisible()
  await expect(page.getByText(en.notFound.message)).toBeVisible()
})

test('[web-client/R11-S3] Missing asset is a real 404', async ({ request }) => {
  for (const path of [
    '/assets/no-such-file.js',
    '/robots.txt',
    '/favicon.ico',
  ]) {
    const response = await request.get(path)
    const body = await response.text()

    expect(response.status(), path).toBe(404)
    expect(response.headers()['content-type'] ?? '', path).not.toContain(
      'text/html',
    )
    expect(body, path).not.toContain('id="root"')
    expect(response.headers()['cache-control'] ?? '', path).not.toContain(
      'immutable',
    )
    expectSecurityHeaders(response.headers())
  }
})

test('The icon is served as an image under the security headers', async ({
  request,
}) => {
  const response = await request.get('/favicon.svg')

  expect(response.status()).toBe(200)
  expect(response.headers()['content-type']).toContain('image/svg+xml')
  expectSecurityHeaders(response.headers())
})

test('[web-client/R12-S1] Reachable backend shows no warning', async ({
  page,
}) => {
  const meResponse = page.waitForResponse(
    (response) => new URL(response.url()).pathname === '/api/me',
  )
  await page.goto('/login')
  await meResponse
  await expect(
    page.getByRole('heading', { name: en.login.title }),
  ).toBeVisible()

  await expect(page.getByText(en.login.unreachable)).toHaveCount(0)
})

test('[web-client/R12-S3] Status check does not send the session cookie', async ({
  page,
  context,
  baseURL,
}) => {
  const session = 'session-cookie-probe'
  await context.addCookies([{ name: 'SESSION', value: session, url: baseURL! }])
  const meRequests: Request[] = []
  page.on('request', (request) => {
    if (new URL(request.url()).pathname === '/api/me') {
      meRequests.push(request)
    }
  })

  await page.goto('/login')
  await expect(
    page.getByRole('heading', { name: en.login.title }),
  ).toBeVisible()
  // The page sends two requests to /api/me: loading the user, which carries the session,
  // and the status check, which must not.
  await expect.poll(() => meRequests.length).toBe(2)
  const cookies = await Promise.all(
    meRequests.map(
      async (request) => (await request.allHeaders())['cookie'] ?? '',
    ),
  )

  expect(cookies.filter((cookie) => cookie.includes(session))).toHaveLength(1)
  expect(cookies.filter((cookie) => !cookie.includes(session))).toHaveLength(1)
})

test('[web-client/R6-S1] API request reaches the backend on the same origin', async ({
  request,
}) => {
  const response = await request.get('/api/me', {
    headers: { Origin: 'https://other.example' },
  })

  expect(response.status()).toBe(401)
  expect(response.headers()['content-type']).toContain(
    'application/problem+json',
  )
  expect((await response.json()).code).toBe('UNAUTHENTICATED')
  expect(response.headers()['access-control-allow-origin']).toBeUndefined()
})

test('[web-client/R6-S2] Actuator is not exposed through the web container', async ({
  request,
}) => {
  const response = await request.get('/actuator/health')
  const body = await response.text()

  expect(body).not.toContain('"status"')
  expect(body).not.toContain('"UP"')
})

test('[web-client/R5-S3] Real backend error is recognised as a problem response', async ({
  request,
}) => {
  await request.get('/api/me')
  const token = (await request.storageState()).cookies.find(
    (cookie) => cookie.name === 'XSRF-TOKEN',
  )?.value
  expect(token).toBeTruthy()

  const response = await request.post('/api/auth/login', {
    headers: {
      Accept: ACCEPT,
      'X-XSRF-TOKEN': token!,
    },
    data: {},
  })

  expect(response.headers()['content-type']).toContain(
    'application/problem+json',
  )
  expect((await response.json()).code).toBe('VALIDATION_FAILED')
})

test('[web-client/R8-S1] Page is served with security headers and works under them', async ({
  page,
}) => {
  const problems: string[] = []
  await page.addInitScript(() => {
    document.addEventListener('securitypolicyviolation', (event) => {
      console.error(
        `CSP violation: ${event.violatedDirective} ${event.blockedURI}`,
      )
    })
  })
  // The anonymous status check gets a 401 on /api/me; any other failed load is a problem.
  const isAnonymousMeCheck = (url: string, status?: number) =>
    new URL(url).pathname === '/api/me' &&
    (status === undefined || status === 401)
  page.on('console', (message) => {
    if (message.type() !== 'error') {
      return
    }
    const loadedFrom = message.location().url
    if (loadedFrom && isAnonymousMeCheck(loadedFrom)) {
      return
    }
    problems.push(message.text())
  })
  page.on('response', (response) => {
    if (
      response.status() >= 400 &&
      !isAnonymousMeCheck(response.url(), response.status())
    ) {
      problems.push(`${response.status()} ${response.url()}`)
    }
  })
  page.on('requestfailed', (request) => {
    problems.push(`request failed: ${request.url()}`)
  })
  page.on('pageerror', (error) => problems.push(error.message))

  const response = await page.goto('/')
  await expect(
    page.getByRole('heading', { name: en.login.title }),
  ).toBeVisible()
  await expect(page.getByLabel(en.login.email)).toBeVisible()

  const headers = response!.headers()
  expectSecurityHeaders(headers)
  await expectIconLoads(page)

  await page.goto('/no-such-page')
  await expect(page.getByText(en.notFound.message)).toBeVisible()
  await expectIconLoads(page)
  expect(problems).toEqual([])
})

test('[web-client/R9-S1] Hashed asset is long-lived and compressed', async ({
  request,
}) => {
  const html = await (await request.get('/')).text()
  const script = html.match(/src="(\/assets\/[^"]+\.js)"/)?.[1]
  expect(script).toBeTruthy()

  const response = await request.get(script!, {
    headers: { 'Accept-Encoding': 'gzip' },
  })

  expect(response.status()).toBe(200)
  expect(response.headers()['cache-control']).toContain('immutable')
  expect(response.headers()['cache-control']).toContain('max-age=31536000')
  expect(response.headers()['content-encoding']).toBe('gzip')
  expectSecurityHeaders(response.headers())
})

test('[web-client/R9-S2] HTML page is revalidated', async ({ request }) => {
  const response = await request.get('/')

  expect(response.headers()['cache-control']).toBe('no-cache')
})

test('[web-client/R10-S1] Health endpoint answers without the application', async ({
  request,
}) => {
  const response = await request.get('/healthz')
  const body = await response.text()

  expect(response.status()).toBe(200)
  expect(response.headers()['content-type'] ?? '').not.toContain('text/html')
  expect(body).not.toContain('id="root"')
})

test.describe('phone screens', () => {
  test.skip(
    ({ isMobile }) => !isMobile,
    'R7-S1 runs only on the phone profiles',
  )

  test('[web-client/R7-S1] Pages fit a phone screen', async ({
    page,
    viewport,
  }) => {
    // At the profile's own width, then at the narrowest supported width.
    const profileWidth = viewport!.width
    await expectAllPagesFit(page, profileWidth)

    await page.setViewportSize({ width: PHONE_WIDTH, height: 640 })
    await expectAllPagesFit(page, PHONE_WIDTH)
  })
})
