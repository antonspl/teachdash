import i18n from '../i18n'
import { ACCEPT } from './headers'

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

// Requests whose 401 is an answer to the request itself, not a sign that a session ended.
const UNAUTHORIZED_EXEMPT = new Set([
  'auth/login',
  'auth/logout',
  'password-links',
])

// A 401 on reading the current user is the startup answer; on other methods it ends a session.
const UNAUTHORIZED_EXEMPT_GET = new Set(['me'])

let unauthorizedHandler: (() => void) | undefined

// Set by AuthProvider; called on a 401 from any request that is not exempt.
export function setUnauthorizedHandler(handler: (() => void) | undefined) {
  unauthorizedHandler = handler
}

function isExemptFromUnauthorized(path: string, method: string): boolean {
  const clean = path.split(/[?#]/)[0].replace(/\/+$/, '')
  return (
    UNAUTHORIZED_EXEMPT.has(clean) ||
    (method === 'GET' && UNAUTHORIZED_EXEMPT_GET.has(clean))
  )
}

export function messageForCode(code: string | undefined): string {
  if (code && i18n.exists(`errors.${code}`)) {
    return i18n.t(`errors.${code}`)
  }
  return i18n.t('errors.generic')
}

// A cancel is deliberate when the caller's signal is aborted for any reason except a timeout.
function isDeliberateCancel(signal: AbortSignal | undefined): boolean {
  if (!signal?.aborted) {
    return false
  }
  const reason: unknown = signal.reason
  return !(
    typeof reason === 'object' &&
    reason !== null &&
    'name' in reason &&
    reason.name === 'TimeoutError'
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

// Not awaited: a slow body must not delay the error, and a cancel failure must not replace it.
export function discardBody(response: Response): void {
  try {
    response.body?.cancel().catch(() => undefined)
  } catch {
    // Nothing to release.
  }
}

async function toApiError(
  response: Response,
  signal: AbortSignal | undefined,
): Promise<ApiError> {
  if (!isProblemResponse(response)) {
    discardBody(response)
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
    if (isDeliberateCancel(signal)) {
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
  options: {
    method?: string
    body?: unknown
    signal?: AbortSignal
    credentials?: RequestCredentials
  } = {},
): Promise<Response> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = {
    Accept: ACCEPT,
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
      credentials: options.credentials,
    })
  } catch (error) {
    if (isDeliberateCancel(options.signal)) {
      throw error
    }
    throw new ApiError({ message: i18n.t('errors.generic'), cause: error })
  }

  if (!response.ok) {
    const error = await toApiError(response, options.signal)
    if (response.status === 401 && !isExemptFromUnauthorized(path, method)) {
      unauthorizedHandler?.()
    }
    throw error
  }
  return response
}
