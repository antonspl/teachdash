import { defineConfig, devices } from '@playwright/test'

// @types/node is not installed; this is all the config needs from it.
declare const process: { env: Record<string, string | undefined> }

export default defineConfig({
  testDir: 'e2e',
  forbidOnly: !!process.env.CI,
  globalSetup: './e2e/global-setup.ts',
  outputDir: 'test-results',
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],
  use: {
    baseURL: 'http://localhost:8081',
  },
  projects: [
    {
      name: 'desktop-chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'phone-chrome',
      use: { ...devices['Pixel 7'] },
    },
    {
      name: 'phone-safari',
      use: { ...devices['iPhone 14'] },
    },
  ],
})
