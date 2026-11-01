import { useEffect, useState } from 'react'
import { ApiError, apiRequest } from './client'

const REQUEST_TIMEOUT_MS = 10_000

export type Reachability = 'checking' | 'reachable' | 'unreachable'

export function useReachability(): Reachability {
  const [status, setStatus] = useState<Reachability>('checking')

  useEffect(() => {
    let active = true
    apiRequest('me', { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) })
      .then(
        (): Reachability => 'reachable',
        (error: unknown): Reachability =>
          error instanceof ApiError && error.isProblem
            ? 'reachable'
            : 'unreachable',
      )
      .then((result) => {
        if (active) {
          setStatus(result)
        }
      })
    return () => {
      active = false
    }
  }, [])

  return status
}
