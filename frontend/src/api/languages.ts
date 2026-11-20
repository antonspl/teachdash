import { apiRequest } from './client'

export async function listLanguages(signal?: AbortSignal): Promise<string[]> {
  const response = await apiRequest('languages', { signal })
  const body = (await response.json()) as unknown
  if (!Array.isArray(body)) {
    throw new Error('Response is not a list')
  }
  return body.map((item) => {
    const code = (item as { code?: unknown } | null)?.code
    if (typeof code !== 'string') {
      throw new Error('Language code is not a string')
    }
    return code
  })
}
