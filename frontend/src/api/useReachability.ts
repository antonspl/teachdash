import { useEffect, useState } from 'react'
import { ApiError, apiRequest } from './client'

export type Reachability = 'checking' | 'reachable' | 'unreachable'

export function useReachability(): Reachability {
  const [status, setStatus] = useState<Reachability>('checking')

  useEffect(() => {
    let active = true
    apiRequest('me')
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
