import type { ReactNode } from 'react';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { mockHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { RecentTransactions } from './RecentTransactions';

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});
afterEach(() => {
  server.resetHandlers();
});
afterAll(() => {
  server.close();
});

function wrap(ui: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{ui}</QueryClientProvider>;
}

describe('RecentTransactions', () => {
  it('renders rows for each fixture transaction with a View-all link', async () => {
    render(wrap(<RecentTransactions />));

    expect(await screen.findByText('Office supplies')).toBeInTheDocument();
    expect(screen.getByText('Conference ticket')).toBeInTheDocument();
    expect(screen.getByText('Team lunch')).toBeInTheDocument();

    expect(screen.getByRole('link', { name: /view all/i })).toHaveAttribute('href', '/ledger');
  });

  it('shows empty state when there are no transactions', async () => {
    server.use(mockHandlers.listTransactions.empty);
    render(wrap(<RecentTransactions />));
    expect(await screen.findByText(/no transactions recorded yet/i)).toBeInTheDocument();
  });

  it('shows error state on a server failure', async () => {
    server.use(mockHandlers.listTransactions.serverError);
    render(wrap(<RecentTransactions />));
    expect(await screen.findByText(/failed to load transactions/i)).toBeInTheDocument();
  });
});
