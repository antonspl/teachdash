import type { FullConfig } from '@playwright/test'

export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0].use.baseURL
  if (!baseURL) {
    throw new Error('baseURL is not set in playwright.config.ts')
  }

  const hint =
    'Start the stack with `docker compose up --build --wait` from the repo root, then run the tests again.'

  const page = await fetch(baseURL, {
    signal: AbortSignal.timeout(5000),
  }).catch(() => null)
  if (!page?.ok) {
    throw new Error(`The web container does not answer at ${baseURL}. ${hint}`)
  }
}
