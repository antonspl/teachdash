import { createRoot } from 'react-dom/client'
import '../../src/styles/base.css'
import '../../src/styles/tokens.css'
import { Select } from '../../src/ui/Select'

const props = {
  icon: 'globe' as const,
  value: 'en',
  options: ['en'],
  labels: { en: 'English' },
  onChange: () => {},
}

createRoot(document.body).render(
  <>
    <Select {...props} label="Enabled language" />
    <Select {...props} label="Disabled language" disabled />
    <Select {...props} label="Invalid language" error="Choose a language." />
  </>,
)
