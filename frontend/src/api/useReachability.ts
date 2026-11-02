import { useEffect, useState } from 'react'
import { ApiError, apiRequest } from './client'

const REQUEST_TIMEOUT_MS = 10_000
const RETRY_DELAY_MS = 5_000
const POLL_DELAY_MS = 30_000

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
    const response = await apiRequest('me', { signal })
    return isJson(response)
  } catch (error) {
    return error instanceof ApiError && error.isProblem
  }
}

export function useReachability(): Reachability {
  const [status, setStatus] = useState<Reachability>('checking')

  useEffect(() => {
    let stopped = false
    let current: AbortController | undefined
    let timer: ReturnType<typeof setTimeout> | undefined

    // AbortSignal.any needs Safari 17.4 and the target is iOS 16.4, so one controller
    // per request serves both the cancel on unmount and the timeout.
    async function check() {
      const request = new AbortController()
      current = request
      const timeout = setTimeout(
        () => request.abort(new DOMException('', 'TimeoutError')),
        REQUEST_TIMEOUT_MS,
      )
      const reachable = await isReachable(request.signal)
      clearTimeout(timeout)
      if (stopped) {
        return
      }
      setStatus(reachable ? 'reachable' : 'unreachable')
      timer = setTimeout(check, reachable ? POLL_DELAY_MS : RETRY_DELAY_MS)
    }

    void check()
    return () => {
      stopped = true
      current?.abort(new Error('unmounted'))
      clearTimeout(timer)
    }
  }, [])

  return status
}
