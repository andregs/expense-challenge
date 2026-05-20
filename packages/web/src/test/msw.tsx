import type { ReactNode } from 'react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { server } from '@/mocks/server';

export function setupMswServer(onUnhandledRequest: 'error' | 'warn' | 'bypass' = 'error') {
  beforeAll(() => {
    server.listen({ onUnhandledRequest });
  });
  afterEach(() => {
    server.resetHandlers();
  });
  afterAll(() => {
    server.close();
  });
}

export function renderWithProviders(ui: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}
