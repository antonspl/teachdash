import type { Page, Route } from '@playwright/test'

/**
 * Holds every request with the given method and URL pattern until `release()`, then
 * answers it with a 500 problem, so the page ends up in its failed state. `stop()` removes
 * the hold, and later requests go to the backend again.
 */
export async function holdRequest(page: Page, pattern: string, method: string) {
  let release: () => void = () => undefined
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  const answered: Promise<void>[] = []
  const handler = async (route: Route) => {
    if (route.request().method() !== method) {
      await route.fallback()
      return
    }
    const answer = gate.then(() =>
      route.fulfill({
        status: 500,
        contentType: 'application/problem+json',
        body: JSON.stringify({ status: 500, code: 'INTERNAL_ERROR' }),
      }),
    )
    answered.push(answer)
    await answer
  }
  await page.route(pattern, handler)
  return {
    release,
    // After `release()`: waits until the held requests are answered, then removes the hold.
    stop: async () => {
      await Promise.all(answered)
      await page.unroute(pattern, handler)
    },
  }
}
