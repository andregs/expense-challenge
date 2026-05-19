import { defineConfig, devices } from '@playwright/test';

/**
 * Single spec suite that runs in two modes, selected by NEXT_PUBLIC_API_MOCKING:
 *
 * enabled  — MSW intercepts all /api/* calls inside the browser; no backend or
 *            Treasury connection required. Playwright starts the Next.js dev
 *            server automatically. Local: pnpm test:e2e:mocked
 *            In compose: docker compose --profile e2e run --rm \
 *                          -e NEXT_PUBLIC_API_MOCKING=enabled e2e
 *
 * disabled — Calls hit the real backend. BASE_URL and BACKEND_URL must point at
 * (default)  running services. The docker-compose e2e profile handles this:
 *              docker compose --profile e2e run --rm e2e
 */
const mocking = process.env.NEXT_PUBLIC_API_MOCKING === 'enabled';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  ...(process.env.CI ? { workers: 1 } : {}),
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',

  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  ...(mocking
    ? {
        webServer: {
          // `dev` script already exports NEXT_PUBLIC_API_MOCKING=enabled
          command: 'pnpm dev',
          url: 'http://localhost:3000',
          reuseExistingServer: !process.env.CI,
          timeout: 120_000,
          stdout: 'ignore',
          stderr: 'pipe',
        },
      }
    : {}),
});
