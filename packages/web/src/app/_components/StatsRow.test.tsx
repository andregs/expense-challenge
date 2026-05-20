import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { mockHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { setupMswServer, renderWithProviders } from '@/test/msw';
import { StatsRow } from './StatsRow';

setupMswServer();

describe('StatsRow', () => {
  it('shows summed USD spend and transaction count from the fixture', async () => {
    renderWithProviders(<StatsRow />);

    // samplePage: 124.99 + 899.00 + 212.50 = 1236.49, totalElements = 3
    expect(await screen.findByText('$1,236.49')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('shows an error sentinel when the list endpoint fails', async () => {
    server.use(mockHandlers.listTransactions.serverError);
    renderWithProviders(<StatsRow />);

    // Both stat cards render "!" when the query errors out
    const errorMarks = await screen.findAllByText('!');
    expect(errorMarks.length).toBeGreaterThanOrEqual(2);
  });
});
