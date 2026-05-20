import { describe, expect, it } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mockHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { setupMswServer, renderWithProviders } from '@/test/msw';
import { TransactionDetail } from './TransactionDetail';

// TransactionDetail uses Link and no router hooks, so no next/navigation mock needed.

const TX_ID = '11111111-1111-4111-8111-111111111111';

setupMswServer();

describe('TransactionDetail', () => {
  it('shows transaction description, date and USD amount', async () => {
    renderWithProviders(<TransactionDetail id={TX_ID} />);
    expect(await screen.findByText('Office supplies')).toBeInTheDocument();
    expect(screen.getByText('Apr 12, 2026')).toBeInTheDocument();
    expect(screen.getByText('$124.99')).toBeInTheDocument();
  });

  it('shows conversion result after selecting a currency', async () => {
    renderWithProviders(<TransactionDetail id={TX_ID} />);
    await screen.findByText('Office supplies');

    await userEvent.type(screen.getByLabelText('Convert to'), 'BRL');

    // sampleConverted fixture: convertedAmount=640.40, currency=BRL, exchangeRate=5.123400
    await waitFor(() => {
      expect(screen.getByText('640.40 BRL')).toBeInTheDocument();
    });
    expect(screen.getByText('5.123400 BRL/USD')).toBeInTheDocument();
    expect(screen.getByText('Mar 31, 2026')).toBeInTheDocument();
  });

  it('shows a 422 message when no rate is available for the selected currency', async () => {
    server.use(mockHandlers.getTransaction.unconvertible);
    renderWithProviders(<TransactionDetail id={TX_ID} />);
    await screen.findByText('Office supplies');

    await userEvent.type(screen.getByLabelText('Convert to'), 'BRL');

    expect(
      await screen.findByText(/no exchange rate available within 6 months/i),
    ).toBeInTheDocument();
  });

  it('shows a not-found message for an unknown transaction id', async () => {
    server.use(mockHandlers.getTransaction.notFound);
    renderWithProviders(<TransactionDetail id="00000000-0000-0000-0000-000000000000" />);
    expect(await screen.findByText(/transaction not found/i)).toBeInTheDocument();
  });

  it('shows an evict cached FX rates button', async () => {
    renderWithProviders(<TransactionDetail id={TX_ID} />);
    await screen.findByText('Office supplies');
    expect(screen.getByRole('button', { name: /evict cached fx rates/i })).toBeInTheDocument();
  });

  it('shows a transient confirmation notice after the evict button is clicked', async () => {
    renderWithProviders(<TransactionDetail id={TX_ID} />);
    await screen.findByText('Office supplies');

    // Toast span is always in the DOM; clicking evict applies the visible CSS class.
    await userEvent.click(screen.getByRole('button', { name: /evict cached fx rates/i }));

    const toast = await screen.findByText(/cache cleared/i);
    expect(toast).toBeInTheDocument();
    expect(toast.className).toMatch(/evictToastVisible/);
  });
});
