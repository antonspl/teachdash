import { act, cleanup, render, screen } from '@testing-library/react'
import { StrictMode, useEffect, useRef, useState } from 'react'
import { afterEach, expect, test } from 'vitest'
import '../i18n'
import { ApiError } from '../api/client'
import { useFieldErrors } from './useFieldErrors'

afterEach(cleanup)

type Api = ReturnType<typeof useFieldErrors<'name' | 'email'>>

function problem(...entries: [string, string][]) {
  return new ApiError({
    message: 'x',
    status: 400,
    fieldErrors: entries.map(([field, code]) => ({ field, code })),
  })
}

let api: Api
let setDisabled: (value: boolean) => void

function Form({ messages }: { messages?: Partial<Record<string, string>> }) {
  const nameRef = useRef<HTMLInputElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  const [disabled, setDisabledState] = useState(false)
  const hook = useFieldErrors(
    [
      { key: 'name', backend: 'name', ref: nameRef },
      { key: 'email', backend: 'email', ref: emailRef },
    ],
    messages,
  )
  useEffect(() => {
    api = hook
    setDisabled = setDisabledState
  }, [hook, setDisabledState])
  return (
    <form>
      <input aria-label="name" ref={nameRef} disabled={disabled} />
      <input aria-label="email" ref={emailRef} disabled={disabled} />
      <p data-testid="errors">{JSON.stringify(hook.errors)}</p>
    </form>
  )
}

function shown() {
  return JSON.parse(screen.getByTestId('errors').textContent ?? '{}')
}

function placeIn(error: ApiError) {
  let result = false
  act(() => {
    result = api.place(error)
  })
  return result
}

test('unknown backend field returns false and stores nothing', () => {
  render(<Form />)
  expect(placeIn(problem(['name', 'REQUIRED'], ['nope', 'REQUIRED']))).toBe(
    false,
  )
  expect(shown()).toEqual({})
  expect(document.activeElement).toBe(document.body)
})

test('unknown code returns false and stores nothing', () => {
  render(<Form />)
  expect(placeIn(problem(['name', 'REQUIRED'], ['email', 'WEIRD']))).toBe(false)
  expect(shown()).toEqual({})
})

test('empty list returns false', () => {
  render(<Form />)
  expect(placeIn(problem())).toBe(false)
  expect(shown()).toEqual({})
})

test('first entry per field wins', () => {
  render(<Form />)
  expect(placeIn(problem(['name', 'TOO_SHORT'], ['name', 'REQUIRED']))).toBe(
    true,
  )
  expect(shown()).toEqual({ name: "Hmm, that's too short." })
})

test('focus goes to the topmost field in field order', () => {
  render(<Form />)
  placeIn(problem(['email', 'REQUIRED'], ['name', 'REQUIRED']))
  expect(document.activeElement).toBe(screen.getByLabelText('name'))
})

test('focus waits until the element is enabled, once, under StrictMode', () => {
  render(
    <StrictMode>
      <Form />
    </StrictMode>,
  )
  act(() => setDisabled(true))
  placeIn(problem(['email', 'REQUIRED']))
  expect(document.activeElement).toBe(document.body)
  act(() => setDisabled(false))
  expect(document.activeElement).toBe(screen.getByLabelText('email'))
  screen.getByLabelText('name').focus()
  act(() => setDisabled(true))
  act(() => setDisabled(false))
  expect(document.activeElement).toBe(screen.getByLabelText('name'))
})

test('clear removes one field error', () => {
  render(<Form />)
  placeIn(problem(['name', 'REQUIRED'], ['email', 'EMAIL_TAKEN']))
  act(() => api.clear('name'))
  expect(shown()).toEqual({
    email: 'An account with this email already exists.',
  })
})

test('clearAll removes all errors and a pending focus', () => {
  render(<Form />)
  act(() => setDisabled(true))
  placeIn(problem(['name', 'REQUIRED']))
  act(() => api.clearAll())
  act(() => setDisabled(false))
  expect(shown()).toEqual({})
  expect(document.activeElement).toBe(document.body)
})

test('messages override the default text and add codes', () => {
  render(<Form messages={{ REQUIRED: 'Custom', WRONG_PASSWORD: 'Wrong' }} />)
  placeIn(problem(['name', 'REQUIRED'], ['email', 'WRONG_PASSWORD']))
  expect(shown()).toEqual({ name: 'Custom', email: 'Wrong' })
})

test('WRONG_PASSWORD without an override is unknown', () => {
  render(<Form />)
  expect(placeIn(problem(['name', 'WRONG_PASSWORD']))).toBe(false)
})
