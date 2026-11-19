import { expect, test } from 'vitest'
import { passwordHints, passwordProblem } from './passwordRules'

test('a pair that differs is a mismatch', () => {
  expect(passwordProblem('longenough', 'longenough!')).toBe('password.mismatch')
})

test('a 7-character password is too short', () => {
  expect(passwordProblem('1234567', '1234567')).toBe('password.tooShort')
})

test('40 Cyrillic letters are 80 bytes and too long', () => {
  const password = 'ж'.repeat(40)
  expect(passwordProblem(password, password)).toBe('password.tooLong')
})

test('a valid pair has no problem', () => {
  expect(passwordProblem('longenough', 'longenough')).toBeUndefined()
})

test.each([
  ['', '', undefined, undefined],
  ['abc', '', 'password.tooShort', undefined],
  ['1234567', '1234567', 'password.tooShort', undefined],
  ['12345678', '', undefined, undefined],
  ['я'.repeat(36), '', undefined, undefined],
  ['я'.repeat(40), '', 'password.tooLong', undefined],
  ['teacher-pass-2', 'teacher', undefined, 'password.mismatch'],
  ['', 'x', undefined, 'password.mismatch'],
  ['teacher-pass-2', 'teacher-pass-2', undefined, undefined],
  ['abc', 'abd', 'password.tooShort', 'password.mismatch'],
])('hints for %j and %j', (password, repeat, onPassword, onRepeat) => {
  expect(passwordHints(password, repeat)).toEqual({
    password: onPassword,
    repeat: onRepeat,
  })
})
