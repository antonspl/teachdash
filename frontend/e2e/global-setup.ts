const BASE_URL = 'http://localhost:8081'
const BACKEND_WAIT_MS = 30_000

async function fetchOrNull(path: string) {
  try {
    return await fetch(`${BASE_URL}${path}`, {
      signal: AbortSignal.timeout(5000),
    })
  } catch {
    return null
  }
}

async function backendAnswersProblem() {
  const response = await fetchOrNull('/api/me')
  const contentType = response?.headers.get('content-type') ?? ''
  return contentType.includes('application/problem+json')
}

async function waitForBackend() {
  const deadline = Date.now() + BACKEND_WAIT_MS
  while (Date.now() < deadline) {
    if (await backendAnswersProblem()) {
      return true
    }
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }
  return false
}

export default async function globalSetup() {
  const hint =
    'Start the stack with `docker compose up --build` from the repo root, then run the tests again.'

  const page = await fetchOrNull('/')
  if (!page?.ok) {
    throw new Error(`The web container does not answer at ${BASE_URL}. ${hint}`)
  }

  // The backend takes a few seconds to start after the web container is up.
  if (!(await waitForBackend())) {
    throw new Error(
      `The web container is up but the backend does not answer through it. ${hint}`,
    )
  }
}
