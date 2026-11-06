import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { AnonymousOnly, HomeRedirect, RequireRole } from './auth/guards'
import { CabinetPage } from './pages/CabinetPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { SetPasswordPage } from './pages/SetPasswordPage'

export function App() {
  const { t } = useTranslation()

  useEffect(() => {
    document.title = t('app.name')
  }, [t])

  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route
          path="/login"
          element={
            <AnonymousOnly>
              <LoginPage />
            </AnonymousOnly>
          }
        />
        <Route path="/set-password" element={<SetPasswordPage />} />
        <Route
          path="/admin"
          element={
            <RequireRole role="ADMIN">
              <CabinetPage role="ADMIN" />
            </RequireRole>
          }
        />
        <Route
          path="/teacher"
          element={
            <RequireRole role="TEACHER">
              <CabinetPage role="TEACHER" />
            </RequireRole>
          }
        />
        <Route
          path="/student"
          element={
            <RequireRole role="STUDENT">
              <CabinetPage role="STUDENT" />
            </RequireRole>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AuthProvider>
  )
}
