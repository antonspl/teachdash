import type { FullConfig } from '@playwright/test'

const hint =
  'Start the stack with `docker compose up --build --wait` from the repo root, then run the tests again.'

async function probe(url: string) {
  return fetch(url, { signal: AbortSignal.timeout(5000) }).catch(() => null)
}

export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0].use.baseURL
  if (!baseURL) {
    throw new Error('baseURL is not set in playwright.config.ts')
  }

  const page = await probe(baseURL)
  if (!page?.ok) {
    throw new Error(`The web container does not answer at ${baseURL}. ${hint}`)
  }

  const me = await probe(new URL('/api/me', baseURL).href)
  if (!me?.headers.get('content-type')?.includes('application/problem+json')) {
    throw new Error(
      `The web container is up, but the backend does not answer at ${baseURL}/api/me. ` +
        'Start it with `docker compose up -d --wait` from the repo root, then run the tests again.',
    )
  }
}
