import { expect, test, type Page } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }

const PHONE_WIDTH = 360

function expectSecurityHeaders(headers: Record<string, string>) {
  expect(headers['content-security-policy']).toContain("default-src 'self'")
  expect(headers['content-security-policy']).toContain("frame-ancestors 'none'")
  expect(headers['x-content-type-options']).toBe('nosniff')
  expect(headers['referrer-policy']).toBeTruthy()
}

async function expectFitsPhoneScreen(
  page: Page,
  path: string,
  expectedWidth: number,
) {
  await page.goto(path)
  await expect(page.getByRole('heading')).toBeVisible()
  // The status text arrives after the request finishes; wait so the layout is final.
  if (path === '/') {
    await expect(page.getByText(en.status.reachable)).toBeVisible()
  }

  const layout = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
    innerWidth: window.innerWidth,
  }))

  expect(layout.scrollWidth).toBeLessThanOrEqual(layout.clientWidth)
  expect(layout.innerWidth).toBe(expectedWidth)
}

test('[web-client/R1-S1] Root path shows the placeholder page', async ({
  page,
}) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: en.app.name })).toBeVisible()
})

test('[web-client/R1-S2] Unknown path opened directly shows the not-found page', async ({
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

test('[web-client/R1-S3] Missing asset is a real 404', async ({ request }) => {
  const response = await request.get('/assets/no-such-file.js')
  const body = await response.text()

  expect(response.status()).toBe(404)
  expect(response.headers()['content-type'] ?? '').not.toContain('text/html')
  expect(body).not.toContain('id="root"')
  expect(response.headers()['cache-control'] ?? '').not.toContain('immutable')
  expectSecurityHeaders(response.headers())
})

test('[web-client/R3-S1] Anonymous user sees that the backend is reachable', async ({
  page,
}) => {
  await page.goto('/')

  await expect(page.getByText(en.status.reachable)).toBeVisible()
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
      Accept: 'application/problem+json, application/json',
      'X-XSRF-TOKEN': token!,
    },
    data: {
      email: 'admin@example.com',
      password: 'not-the-admin-password',
    },
  })

  expect(response.headers()['content-type']).toContain(
    'application/problem+json',
  )
  expect((await response.json()).code).toBe('WRONG_PASSWORD')
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
  page.on('console', (message) => {
    // The anonymous status check gets a 401, which the browser logs as a failed load.
    const expectedLoad = message.text().startsWith('Failed to load resource')
    if (message.type() === 'error' && !expectedLoad) {
      problems.push(message.text())
    }
  })
  page.on('pageerror', (error) => problems.push(error.message))

  const response = await page.goto('/')
  await expect(page.getByText(en.status.reachable)).toBeVisible()

  const headers = response!.headers()
  expectSecurityHeaders(headers)
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

  test('[web-client/R7-S1] Pages fit a phone screen at the profile width', async ({
    page,
    viewport,
  }) => {
    const profileWidth = viewport!.width

    await expectFitsPhoneScreen(page, '/', profileWidth)
    await expectFitsPhoneScreen(page, '/no-such-page', profileWidth)
  })

  test.describe(`at ${PHONE_WIDTH} CSS pixels`, () => {
    test.use({ viewport: { width: PHONE_WIDTH, height: 640 } })

    test('[web-client/R7-S1] Pages fit a phone screen at 360 CSS pixels', async ({
      page,
    }) => {
      await expectFitsPhoneScreen(page, '/', PHONE_WIDTH)
      await expectFitsPhoneScreen(page, '/no-such-page', PHONE_WIDTH)
    })
  })
})
