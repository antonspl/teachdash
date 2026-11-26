export const FALLBACK_LANGUAGE = 'en'

export function detectLanguage(
  supported: readonly string[],
  stored: string | null | undefined,
  languages: readonly string[] | null | undefined,
  language: string | null | undefined,
): string {
  const storedLower = stored?.toLowerCase()
  if (storedLower && supported.includes(storedLower)) {
    return storedLower
  }
  const candidates = languages?.length ? languages : language ? [language] : []
  for (const candidate of candidates) {
    const primary = candidate.split('-')[0].toLowerCase()
    if (supported.includes(primary)) {
      return primary
    }
  }
  return FALLBACK_LANGUAGE
}
