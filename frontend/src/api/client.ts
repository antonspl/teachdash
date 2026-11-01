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
  }) {
    super(options.message)
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

async function readProblemCode(
  response: Response,
): Promise<string | undefined> {
  try {
    const body: unknown = await response.json()
    if (typeof body === 'object' && body !== null && 'code' in body) {
      return typeof body.code === 'string' ? body.code : undefined
    }
  } catch {
    // An unreadable body is handled like a problem without a code.
  }
  return undefined
}

async function toApiError(response: Response): Promise<ApiError> {
  const isProblem = isProblemResponse(response)
  const code = isProblem ? await readProblemCode(response) : undefined
  return new ApiError({
    message: messageForCode(code),
    status: response.status,
    code,
    isProblem,
  })
}

export async function apiRequest(
  path: string,
  options: { method?: string; body?: unknown } = {},
): Promise<Response> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = { Accept: 'application/json' }

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
    })
  } catch {
    throw new ApiError({ message: i18n.t('errors.generic') })
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  return response
}
