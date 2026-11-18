import { useEffect, useRef, useState, type RefObject } from 'react'
import { useTranslation } from 'react-i18next'
import type { ApiError } from '../api/client'

export type FieldSpec<K extends string> = {
  key: K
  // The field name the backend uses in `fieldErrors`.
  backend: string
  ref: RefObject<HTMLElement | null>
}

// Codes with a default message; any code can be overridden through `messages`.
const DEFAULT_MESSAGE_KEYS: Record<string, string> = {
  REQUIRED: 'fieldErrors.REQUIRED',
  TOO_SHORT: 'fieldErrors.TOO_SHORT',
  TOO_LONG: 'fieldErrors.TOO_LONG',
  INVALID_FORMAT: 'fieldErrors.INVALID_FORMAT',
  INVALID_TYPE: 'fieldErrors.INVALID_TYPE',
  EMAIL_TAKEN: 'errors.EMAIL_TAKEN',
}

// `fields` is in the form's top-to-bottom order.
export function useFieldErrors<K extends string>(
  fields: FieldSpec<K>[],
  messages?: Partial<Record<string, string>>,
) {
  const { t } = useTranslation()
  const [errors, setErrors] = useState<Partial<Record<K, string>>>({})
  // The field to focus once it is enabled. A ref, because only the effect reads it;
  // `place` also sets `errors`, which renders the form.
  const pendingFocus = useRef<K | null>(null)

  // Runs after every render while a focus is pending: inputs are disabled while a
  // request runs and `focus()` does nothing on them, so wait for an enabled one.
  useEffect(() => {
    const key = pendingFocus.current
    if (key === null) {
      return
    }
    const element = fields.find((field) => field.key === key)?.ref.current
    if (element && !element.matches(':disabled')) {
      pendingFocus.current = null
      element.focus()
    }
  })

  function messageFor(code: string): string | undefined {
    const override = messages?.[code]
    if (override) {
      return override
    }
    const key = DEFAULT_MESSAGE_KEYS[code]
    return key ? t(key) : undefined
  }

  function place(error: ApiError): boolean {
    if (error.fieldErrors.length === 0) {
      return false
    }
    const placed: Partial<Record<K, string>> = {}
    for (const entry of error.fieldErrors) {
      const field = fields.find(
        (candidate) => candidate.backend === entry.field,
      )
      const message = messageFor(entry.code)
      if (!field || !message) {
        return false
      }
      if (placed[field.key] === undefined) {
        placed[field.key] = message
      }
    }
    pendingFocus.current =
      fields.find((field) => placed[field.key] !== undefined)?.key ?? null
    setErrors(placed)
    return true
  }

  function clear(key: K) {
    setErrors((current) => {
      if (current[key] === undefined) {
        return current
      }
      const next = { ...current }
      delete next[key]
      return next
    })
  }

  function clearAll() {
    pendingFocus.current = null
    setErrors((current) => (Object.keys(current).length === 0 ? current : {}))
  }

  return { errors, place, clear, clearAll }
}
