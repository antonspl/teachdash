import { act, cleanup, render, screen } from '@testing-library/react'
import { StrictMode, useEffect, useRef } from 'react'
import { afterEach, expect, test, vi } from 'vitest'
import '../i18n'
import en from '../i18n/en.json'
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

type Props = {
  onApi: (api: Api) => void
  disabled?: boolean
  showEmail?: boolean
  messages?: Partial<Record<string, string>>
}

function Form({ onApi, disabled, showEmail = true, messages }: Props) {
  const nameRef = useRef<HTMLInputElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  const hook = useFieldErrors(
    [
      { key: 'name', backend: 'name', ref: nameRef },
      { key: 'email', backend: 'email', ref: emailRef },
    ],
    messages,
  )
  useEffect(() => {
    onApi(hook)
  })
  return (
    <form>
      <input aria-label="name" ref={nameRef} disabled={disabled} />
      {showEmail ? (
        <input aria-label="email" ref={emailRef} disabled={disabled} />
      ) : null}
      <button type="button">other</button>
      <p data-testid="errors">{JSON.stringify(hook.errors)}</p>
    </form>
  )
}

// Each test gets its own harness, so an API cannot leak between tests.
function setup(initial: Omit<Props, 'onApi'> = {}, strict = false) {
  const holder: { api?: Api } = {}
  const onApi = (api: Api) => {
    holder.api = api
  }
  const wrap = (props: Omit<Props, 'onApi'>) => {
    const form = <Form onApi={onApi} {...props} />
    return strict ? <StrictMode>{form}</StrictMode> : form
  }
  const view = render(wrap(initial))
  return {
    api: () => holder.api as Api,
    update: (props: Omit<Props, 'onApi'>) => view.rerender(wrap(props)),
    shown: () => JSON.parse(screen.getByTestId('errors').textContent ?? '{}'),
    place(error: ApiError) {
      let result = false
      act(() => {
        result = holder.api!.place(error)
      })
      return result
    },
  }
}

test('unknown backend field returns false and stores nothing', () => {
  const h = setup()
  expect(h.place(problem(['name', 'REQUIRED'], ['nope', 'REQUIRED']))).toBe(
    false,
  )
  expect(h.shown()).toEqual({})
  expect(document.activeElement).toBe(document.body)
})

test('unknown code returns false and stores nothing', () => {
  const h = setup()
  expect(h.place(problem(['name', 'REQUIRED'], ['email', 'WEIRD']))).toBe(false)
  expect(h.shown()).toEqual({})
})

test('empty list returns false', () => {
  const h = setup()
  expect(h.place(problem())).toBe(false)
  expect(h.shown()).toEqual({})
})

test('first entry per field wins', () => {
  const h = setup()
  expect(h.place(problem(['name', 'TOO_SHORT'], ['name', 'REQUIRED']))).toBe(
    true,
  )
  expect(h.shown()).toEqual({ name: en.fieldErrors.TOO_SHORT })
})

test('focus goes to the topmost field in field order', () => {
  const h = setup()
  h.place(problem(['email', 'REQUIRED'], ['name', 'REQUIRED']))
  expect(document.activeElement).toBe(screen.getByLabelText('name'))
})

test('focus waits until the element is enabled and is applied once, under StrictMode', () => {
  const h = setup({ disabled: true }, true)
  const email = screen.getByLabelText('email')
  const focus = vi.spyOn(email, 'focus')
  h.place(problem(['email', 'REQUIRED']))
  expect(focus).not.toHaveBeenCalled()
  h.update({ disabled: false })
  expect(focus).toHaveBeenCalledTimes(1)
  expect(document.activeElement).toBe(email)
  h.update({ disabled: true })
  h.update({ disabled: false })
  expect(focus).toHaveBeenCalledTimes(1)
})

test('a pending focus whose element never renders is dropped', () => {
  const h = setup({ showEmail: false })
  h.place(problem(['email', 'REQUIRED']))
  h.update({ showEmail: true })
  expect(document.activeElement).toBe(document.body)
})

test('clear cancels the pending focus of that field', () => {
  const h = setup({ disabled: true })
  h.place(problem(['email', 'REQUIRED']))
  act(() => h.api().clear('email'))
  h.update({ disabled: false })
  expect(document.activeElement).toBe(document.body)
})

test('a rejected place keeps earlier errors but cancels the pending focus', () => {
  const h = setup({ disabled: true })
  expect(h.place(problem(['email', 'REQUIRED']))).toBe(true)
  expect(h.place(problem(['name', 'WEIRD']))).toBe(false)
  h.update({ disabled: false })
  // Forms call clearAll at the start of a submit, so stale errors do not linger.
  expect(h.shown()).toEqual({ email: 'Please fill this in.' })
  expect(document.activeElement).toBe(document.body)
})

test('a successful place replaces the pending focus', () => {
  const h = setup({ disabled: true })
  h.place(problem(['name', 'REQUIRED']))
  h.place(problem(['email', 'REQUIRED']))
  h.update({ disabled: false })
  expect(document.activeElement).toBe(screen.getByLabelText('email'))
})

test('focus is not taken from an element the user focused meanwhile', () => {
  const h = setup({ disabled: true })
  h.place(problem(['email', 'REQUIRED']))
  const other = screen.getByRole('button', { name: 'other' })
  other.focus()
  h.update({ disabled: false })
  expect(document.activeElement).toBe(other)
})

test('focus moves from the element that had it when the submit started (clearAll)', () => {
  const h = setup()
  const other = screen.getByRole('button', { name: 'other' })
  other.focus()
  act(() => h.api().clearAll())
  h.place(problem(['email', 'REQUIRED']))
  expect(document.activeElement).toBe(screen.getByLabelText('email'))
})

test('focus stays on an element focused after the submit started', () => {
  const h = setup()
  const other = screen.getByRole('button', { name: 'other' })
  screen.getByLabelText('name').focus()
  act(() => h.api().clearAll())
  other.focus()
  h.place(problem(['email', 'REQUIRED']))
  expect(document.activeElement).toBe(other)
})

test('clear removes one field error', () => {
  const h = setup()
  h.place(problem(['name', 'REQUIRED'], ['email', 'EMAIL_TAKEN']))
  act(() => h.api().clear('name'))
  expect(h.shown()).toEqual({
    email: 'An account with this email already exists.',
  })
})

test('clearAll removes all errors and a pending focus', () => {
  const h = setup({ disabled: true })
  h.place(problem(['name', 'REQUIRED']))
  act(() => h.api().clearAll())
  h.update({ disabled: false })
  expect(h.shown()).toEqual({})
  expect(document.activeElement).toBe(document.body)
})

test('messages override the default text and add codes', () => {
  const h = setup({ messages: { REQUIRED: 'Custom', WRONG_PASSWORD: 'Wrong' } })
  h.place(problem(['name', 'REQUIRED'], ['email', 'WRONG_PASSWORD']))
  expect(h.shown()).toEqual({ name: 'Custom', email: 'Wrong' })
})

test('WRONG_PASSWORD without an override is unknown', () => {
  const h = setup()
  expect(h.place(problem(['name', 'WRONG_PASSWORD']))).toBe(false)
})
