import type { ReactNode } from 'react';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { mockHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { TransactionForm } from './TransactionForm';

const push = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => {
    return { push, back: vi.fn() };
  },
}));

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});
afterEach(() => {
  server.resetHandlers();
  push.mockReset();
});
afterAll(() => {
  server.close();
});

function wrap(ui: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{ui}</QueryClientProvider>;
}

async function fillForm() {
  await userEvent.type(screen.getByLabelText(/description/i), 'Office supplies');
  await userEvent.type(screen.getByLabelText(/transaction date/i), '2026-05-10');
  await userEvent.type(screen.getByLabelText(/amount/i), '49.99');
}

describe('TransactionForm', () => {
  it('updates the live character counter as the user types the description', async () => {
    render(wrap(<TransactionForm />));
    await userEvent.type(screen.getByLabelText(/description/i), 'Office supplies');
    expect(screen.getByText('15/50')).toBeInTheDocument();
  });

  it('redirects to the new transaction detail on a successful create', async () => {
    render(wrap(<TransactionForm />));
    await fillForm();
    await userEvent.click(screen.getByRole('button', { name: /create transaction/i }));
    await waitFor(() => {
      expect(push).toHaveBeenCalledWith('/transactions/11111111-1111-4111-8111-111111111111');
    });
  });

  it('maps server validation errors onto the matching fields', async () => {
    server.use(mockHandlers.createTransaction.validationError);

    render(wrap(<TransactionForm />));
    await fillForm();
    await userEvent.click(screen.getByRole('button', { name: /create transaction/i }));

    expect(await screen.findByText('must not be blank')).toBeInTheDocument();
    expect(screen.getByText('must be greater than 0')).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });
});
