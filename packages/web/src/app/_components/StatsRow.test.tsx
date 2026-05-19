import type { ReactNode } from 'react';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { mockHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { StatsRow } from './StatsRow';

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

describe('StatsRow', () => {
  it('shows summed USD spend and transaction count from the fixture', async () => {
    render(wrap(<StatsRow />));

    // samplePage: 124.99 + 899.00 + 212.50 = 1236.49, totalElements = 3
    expect(await screen.findByText('$1,236.49')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('shows an error sentinel when the list endpoint fails', async () => {
    server.use(mockHandlers.listTransactions.serverError);
    render(wrap(<StatsRow />));

    // Both stat cards render "!" when the query errors out
    const errorMarks = await screen.findAllByText('!');
    expect(errorMarks.length).toBeGreaterThanOrEqual(2);
  });
});
