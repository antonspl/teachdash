import { useCallback, useEffect, useState } from 'react'
import { listLanguages } from './languages'

export type Languages = {
  codes: string[]
  failed: boolean
  retry: () => void
}

// Loads the list when the form mounts; no cache beyond the component.
export function useLanguages(): Languages {
  const [codes, setCodes] = useState<string[]>([])
  const [failed, setFailed] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    listLanguages(controller.signal).then(
      (list) => {
        if (!controller.signal.aborted) {
          setCodes(list)
          setFailed(false)
        }
      },
      () => {
        if (!controller.signal.aborted) {
          setFailed(true)
        }
      },
    )
    return () => controller.abort()
  }, [attempt])

  const retry = useCallback(() => {
    setFailed(false)
    setAttempt((value) => value + 1)
  }, [])

  return { codes, failed, retry }
}
