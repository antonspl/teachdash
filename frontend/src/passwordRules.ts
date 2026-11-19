const MIN_LENGTH = 8
const MAX_BYTES = 72

export type PasswordProblem =
  'password.mismatch' | 'password.tooShort' | 'password.tooLong'

// Returns the translation key of the first broken rule, or undefined when the pair is valid.
export function passwordProblem(
  password: string,
  repeat: string,
): PasswordProblem | undefined {
  if (password !== repeat) {
    return 'password.mismatch'
  }
  if (password.length < MIN_LENGTH) {
    return 'password.tooShort'
  }
  if (new TextEncoder().encode(password).length > MAX_BYTES) {
    return 'password.tooLong'
  }
  return undefined
}
