import type { ReactNode } from 'react';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { mockHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { LedgerTable } from './LedgerTable';

vi.mock('next/navigation', () => ({
  useSearchParams: () => new URLSearchParams(),
}));

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

describe('LedgerTable', () => {
  it('renders a row per fixture transaction with a link to its detail page', async () => {
    render(wrap(<LedgerTable />));

    // samplePage fixture has 3 transactions; each description is a link
    expect(await screen.findByText('Office supplies')).toBeInTheDocument();
    expect(screen.getByText('Conference ticket')).toBeInTheDocument();
    expect(screen.getByText('Team lunch')).toBeInTheDocument();

    const officeLink = screen.getByRole('link', { name: 'Office supplies' });
    expect(officeLink).toHaveAttribute(
      'href',
      '/transactions/11111111-1111-4111-8111-111111111111',
    );
  });

  it('shows empty state when the page returns no items', async () => {
    server.use(mockHandlers.listTransactions.empty);
    render(wrap(<LedgerTable />));
    expect(await screen.findByText(/no transactions recorded yet/i)).toBeInTheDocument();
  });

  it('shows error state on a server failure', async () => {
    server.use(mockHandlers.listTransactions.serverError);
    render(wrap(<LedgerTable />));
    expect(await screen.findByText(/failed to load/i)).toBeInTheDocument();
  });
});
