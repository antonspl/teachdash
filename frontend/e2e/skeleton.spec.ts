import { expect, test, type Page } from '@playwright/test'
import en from '../src/i18n/en.json' with { type: 'json' }

const PHONE_WIDTH = 360

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
