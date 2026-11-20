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
  // The focus requested by the last successful `place`. `active` is the element that
  // had the focus at that moment (usually the submit button). A ref, because only the
  // effect reads it; `place` also sets `errors`, which renders the form.
  const pendingFocus = useRef<{ key: K; active: Element | null } | null>(null)
  // The element that had the focus when the submit started, recorded by `clearAll`
  // (every form calls it first, before awaiting the request). `undefined` when
  // `clearAll` has not been called since the last `place`: then `place` falls back to
  // the element focused at that moment.
  const submitActive = useRef<Element | null | undefined>(undefined)

  // Runs after every render while a focus is pending. Inputs are disabled while a
  // request runs and `focus()` does nothing on them, so wait for an enabled one. Drop
  // the request when there is nothing to focus (no element), and do not take the focus
  // from an element the user moved it to meanwhile: only move it when it is on the page
  // body or still on the element that had it when the submit started.
  useEffect(() => {
    const pending = pendingFocus.current
    if (pending === null) {
      return
    }
    const element = fields.find((field) => field.key === pending.key)?.ref
      .current
    if (!element) {
      pendingFocus.current = null
      return
    }
    if (element.matches(':disabled')) {
      return
    }
    pendingFocus.current = null
    const active = document.activeElement
    if (
      active === null ||
      active === document.body ||
      active === pending.active
    ) {
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

  // A call that returns false keeps the errors already shown (clear them with
  // `clearAll` first) but cancels any pending focus.
  function place(error: ApiError): boolean {
    pendingFocus.current = null
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
    const first = fields.find((field) => placed[field.key] !== undefined)
    const active =
      submitActive.current === undefined
        ? document.activeElement
        : submitActive.current
    submitActive.current = undefined
    pendingFocus.current = first ? { key: first.key, active } : null
    setErrors(placed)
    return true
  }

  // Shows errors found by the form itself, all at once, and focuses the first field.
  function show(shown: Partial<Record<K, string>>) {
    const first = fields.find((field) => shown[field.key] !== undefined)
    const active =
      submitActive.current === undefined
        ? document.activeElement
        : submitActive.current
    submitActive.current = undefined
    pendingFocus.current = first ? { key: first.key, active } : null
    setErrors(shown)
  }

  function clear(key: K) {
    if (pendingFocus.current?.key === key) {
      pendingFocus.current = null
    }
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
    submitActive.current = document.activeElement
    setErrors((current) => (Object.keys(current).length === 0 ? current : {}))
  }

  return { errors, place, show, clear, clearAll }
}
