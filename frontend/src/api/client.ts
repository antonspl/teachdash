import i18n from '../i18n'

const API_PREFIX = '/api'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

export class ApiError extends Error {
  readonly status: number | undefined
  readonly code: string | undefined
  readonly isProblem: boolean

  constructor(options: {
    message: string
    status?: number
    code?: string
    isProblem?: boolean
    cause?: unknown
  }) {
    super(options.message, { cause: options.cause })
    this.name = 'ApiError'
    this.status = options.status
    this.code = options.code
    this.isProblem = options.isProblem ?? false
  }
}

export function messageForCode(code: string | undefined): string {
  if (code && i18n.exists(`errors.${code}`)) {
    return i18n.t(`errors.${code}`)
  }
  return i18n.t('errors.generic')
}

function isAbortError(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'name' in error &&
    error.name === 'AbortError'
  )
}

function readCookie(name: string): string | undefined {
  for (const part of document.cookie.split(';')) {
    const separator = part.indexOf('=')
    if (separator < 0) {
      continue
    }
    if (part.slice(0, separator).trim() === name) {
      return part.slice(separator + 1).trim()
    }
  }
  return undefined
}

function isProblemResponse(response: Response): boolean {
  const contentType = response.headers.get('Content-Type') ?? ''
  const mediaType = contentType.split(';')[0].trim().toLowerCase()
  return mediaType === 'application/problem+json'
}

function parseProblemCode(text: string): string | undefined {
  try {
    const body: unknown = JSON.parse(text)
    if (typeof body === 'object' && body !== null && 'code' in body) {
      return typeof body.code === 'string' ? body.code : undefined
    }
  } catch {
    // A complete body that is not valid JSON is a problem without a code.
  }
  return undefined
}

async function toApiError(response: Response): Promise<ApiError> {
  if (!isProblemResponse(response)) {
    return new ApiError({
      message: messageForCode(undefined),
      status: response.status,
      isProblem: false,
    })
  }
  let text: string
  try {
    text = await response.text()
  } catch (error) {
    if (isAbortError(error)) {
      throw error
    }
    // The body never arrived completely, so this is not a usable problem response.
    return new ApiError({
      message: messageForCode(undefined),
      status: response.status,
      isProblem: false,
      cause: error,
    })
  }
  const code = parseProblemCode(text)
  return new ApiError({
    message: messageForCode(code),
    status: response.status,
    code,
    isProblem: true,
  })
}

export async function apiRequest(
  path: string,
  options: { method?: string; body?: unknown; signal?: AbortSignal } = {},
): Promise<Response> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = {
    Accept: 'application/problem+json, application/json',
  }

  if (!SAFE_METHODS.has(method)) {
    const token = readCookie('XSRF-TOKEN')
    if (token) {
      headers['X-XSRF-TOKEN'] = token
    }
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(`${API_PREFIX}/${path}`, {
      method,
      headers,
      body:
        options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    })
  } catch (error) {
    if (isAbortError(error)) {
      throw error
    }
    throw new ApiError({ message: i18n.t('errors.generic'), cause: error })
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  return response
}
