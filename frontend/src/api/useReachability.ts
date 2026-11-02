import { useEffect, useState } from 'react'
import { ApiError, apiRequest, discardBody } from './client'

const REQUEST_TIMEOUT_MS = 10_000

export type Reachability = 'checking' | 'reachable' | 'unreachable'

function isJson(response: Response): boolean {
  const mediaType = (response.headers.get('Content-Type') ?? '')
    .split(';')[0]
    .trim()
    .toLowerCase()
  return mediaType === 'application/json' || mediaType.endsWith('+json')
}

async function isReachable(signal: AbortSignal): Promise<boolean> {
  try {
    const response = await apiRequest('me', { signal, credentials: 'omit' })
    if (!isJson(response)) {
      discardBody(response)
      return false
    }
    // Read inside the timeout: a body that drops or hangs means unreachable.
    await response.text()
    return true
  } catch (error) {
    return error instanceof ApiError && error.isProblem
  }
}

export function useReachability(): Reachability {
  const [status, setStatus] = useState<Reachability>('checking')

  useEffect(() => {
    let stopped = false
    let current: AbortController | undefined

    // AbortSignal.any needs Safari 17.4 and the target is iOS 16.4, so one controller
    // per request serves both the cancel on unmount and the timeout.
    async function check() {
      if (current) {
        return
      }
      const request = new AbortController()
      current = request
      const timeout = setTimeout(
        () => request.abort(new DOMException('', 'TimeoutError')),
        REQUEST_TIMEOUT_MS,
      )
      const reachable = await isReachable(request.signal)
      clearTimeout(timeout)
      current = undefined
      if (!stopped) {
        setStatus(reachable ? 'reachable' : 'unreachable')
      }
    }

    function onVisibilityChange() {
      if (!document.hidden) {
        void check()
      }
    }

    // A tab that opens hidden is checked once anyway: a single request, no timer.
    void check()
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('online', check)
    return () => {
      stopped = true
      document.removeEventListener('visibilitychange', onVisibilityChange)
      window.removeEventListener('online', check)
      current?.abort(new Error('unmounted'))
    }
  }, [])

  return status
}
