import { expect, test } from 'vitest'
import { passwordProblem } from './passwordRules'

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
