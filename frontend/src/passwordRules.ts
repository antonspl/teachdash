const MIN_LENGTH = 8
const MAX_BYTES = 72

export type PasswordProblem =
  'password.mismatch' | 'password.tooShort' | 'password.tooLong'

// The length rule alone, for a password that is not empty.
function lengthProblem(password: string): PasswordProblem | undefined {
  if (password.length < MIN_LENGTH) {
    return 'password.tooShort'
  }
  if (new TextEncoder().encode(password).length > MAX_BYTES) {
    return 'password.tooLong'
  }
  return undefined
}

// Returns the translation key of the first broken rule, or undefined when the pair is valid.
// This is the gate on submit.
export function passwordProblem(
  password: string,
  repeat: string,
): PasswordProblem | undefined {
  if (password !== repeat) {
    return 'password.mismatch'
  }
  return lengthProblem(password)
}

// The hint to show under each field while the user types. An empty field has no hint.
export function passwordHints(
  password: string,
  repeat: string,
): { password?: PasswordProblem; repeat?: PasswordProblem } {
  return {
    password: password === '' ? undefined : lengthProblem(password),
    repeat:
      repeat !== '' && repeat !== password ? 'password.mismatch' : undefined,
  }
}
