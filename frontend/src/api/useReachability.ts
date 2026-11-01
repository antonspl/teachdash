import { useEffect, useState } from 'react'
import { ApiError, apiRequest } from './client'

const REQUEST_TIMEOUT_MS = 10_000
const RETRY_DELAY_MS = 5_000

export type Reachability = 'checking' | 'reachable' | 'unreachable'

async function isReachable(): Promise<boolean> {
  try {
    await apiRequest('me', { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) })
    return true
  } catch (error) {
    return error instanceof ApiError && error.isProblem
  }
}

export function useReachability(): Reachability {
  const [status, setStatus] = useState<Reachability>('checking')

  useEffect(() => {
    let active = true
    let timer: ReturnType<typeof setTimeout> | undefined

    async function check() {
      const reachable = await isReachable()
      if (!active) {
        return
      }
      if (reachable) {
        setStatus('reachable')
      } else {
        setStatus('unreachable')
        timer = setTimeout(check, RETRY_DELAY_MS)
      }
    }

    void check()
    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [])

  return status
}
