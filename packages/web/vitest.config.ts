import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, 'src'),
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: { modules: { classNameStrategy: 'non-scoped' } },
    include: ['src/**/*.test.{ts,tsx}'],
    exclude: ['node_modules', '.next', 'e2e'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        // Test scaffolding and the tests themselves
        'src/**/*.test.{ts,tsx}',
        'src/test/**',
        // Mock layer is testing infrastructure, not production code
        'src/mocks/**',
        // Type-only declarations
        'src/**/*.d.ts',
        // Next.js boilerplate that wires the app shell or route segments
        // to client components — no logic worth covering directly.
        'src/app/layout.tsx',
        'src/app/**/page.tsx',
        'src/lib/QueryProviders.tsx',
        // Design-system barrel re-export (no executable logic)
        'src/design-system/index.ts',
      ],
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 75,
        statements: 80,
      },
    },
  },
});
