import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Route, Routes } from 'react-router'
import { NotFoundPage } from './pages/NotFoundPage'
import { PlaceholderPage } from './pages/PlaceholderPage'

export function App() {
  const { t } = useTranslation()

  useEffect(() => {
    document.title = t('app.name')
  }, [t])

  return (
    <Routes>
      <Route path="/" element={<PlaceholderPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
