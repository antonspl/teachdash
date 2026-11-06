import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { ApiError, apiRequest, setUnauthorizedHandler } from '../api/client'

export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'

export type User = {
  id: string
  name: string
  email: string
  role: Role
  timeZone: string
}

export type SessionEnded = { path: string }

export type AuthState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'anonymous'; sessionEnded?: SessionEnded }
  | { status: 'user'; user: User }

export type AuthContextValue = {
  state: AuthState
  login: (email: string, password: string) => Promise<User>
  logout: () => Promise<void>
  setPasswordWithLink: (token: string, password: string) => Promise<User>
  retry: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

async function readUser(response: Response): Promise<User> {
  return (await response.json()) as User
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const userActive = useRef(false)

  useEffect(() => {
    userActive.current = state.status === 'user'
  }, [state])

  useEffect(() => {
    const controller = new AbortController()
    apiRequest('me', { signal: controller.signal })
      .then(readUser)
      .then((user) => setState({ status: 'user', user }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        if (error instanceof ApiError && error.status === 401) {
          setState({ status: 'anonymous' })
        } else {
          setState({ status: 'error' })
        }
      })
    return () => controller.abort()
  }, [attempt])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      if (!userActive.current) {
        return
      }
      userActive.current = false
      setState({
        status: 'anonymous',
        sessionEnded: {
          path: window.location.pathname + window.location.search,
        },
      })
    })
    return () => setUnauthorizedHandler(undefined)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiRequest('auth/login', {
      method: 'POST',
      body: { email, password },
    })
    const user = await readUser(response)
    setState({ status: 'user', user })
    return user
  }, [])

  const setPasswordWithLink = useCallback(
    async (token: string, password: string) => {
      const response = await apiRequest('password-links', {
        method: 'POST',
        body: { token, password },
      })
      const user = await readUser(response)
      setState({ status: 'user', user })
      return user
    },
    [],
  )

  const logout = useCallback(async () => {
    try {
      await apiRequest('auth/logout', { method: 'POST' })
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 401)) {
        throw error
      }
    }
    setState({ status: 'anonymous' })
  }, [])

  const retry = useCallback(() => {
    setState({ status: 'loading' })
    setAttempt((current) => current + 1)
  }, [])

  const value = useMemo(
    () => ({ state, login, logout, setPasswordWithLink, retry }),
    [state, login, logout, setPasswordWithLink, retry],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return value
}
