import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright runs against the Next.js dev server booted with MSW
 * enabled (`NEXT_PUBLIC_API_MOCKING=enabled`). The same component code
 * paths the unit tests exercise are driven here through a real browser,
 * with all `/api/*` requests intercepted by the service worker
 * registered by {@link MockingProvider}.
 *
 * The dev server is started by Playwright itself via the `webServer`
 * config so a fresh `pnpm test:e2e` requires no manual prep beyond
 * `pnpm install`. CI and the Dockerised `e2e` compose profile reuse
 * this same config — only the surrounding sandbox changes.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  ...(process.env.CI ? { workers: 1 } : {}),
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',

  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    stdout: 'ignore',
    stderr: 'pipe',
  },
});
