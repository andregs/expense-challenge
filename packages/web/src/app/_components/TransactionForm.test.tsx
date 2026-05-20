import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mockHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { setupMswServer, renderWithProviders } from '@/test/msw';
import { TransactionForm } from './TransactionForm';

const push = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn() }),
}));

setupMswServer();
afterEach(() => {
  push.mockReset();
});

async function fillForm() {
  await userEvent.type(screen.getByLabelText(/description/i), 'Office supplies');
  await userEvent.type(screen.getByLabelText(/transaction date/i), '2026-05-10');
  await userEvent.type(screen.getByLabelText(/amount/i), '49.99');
}

describe('TransactionForm', () => {
  it('updates the live character counter as the user types the description', async () => {
    renderWithProviders(<TransactionForm />);
    await userEvent.type(screen.getByLabelText(/description/i), 'Office supplies');
    expect(screen.getByText('15/50')).toBeInTheDocument();
  });

  it('redirects to the new transaction detail on a successful create (default path)', async () => {
    renderWithProviders(<TransactionForm />);
    await fillForm();
    await userEvent.click(screen.getByRole('button', { name: /create transaction/i }));
    await waitFor(() => {
      expect(push).toHaveBeenCalledWith('/transactions/11111111-1111-4111-8111-111111111111');
    });
  });

  it('calls onSuccess with the transaction id instead of redirecting when provided', async () => {
    const onSuccess = vi.fn();
    renderWithProviders(<TransactionForm onSuccess={onSuccess} />);
    await fillForm();
    await userEvent.click(screen.getByRole('button', { name: /create transaction/i }));
    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalledWith('11111111-1111-4111-8111-111111111111');
    });
    expect(push).not.toHaveBeenCalled();
  });

  it('calls onCancel when Cancel is clicked and onCancel is provided', async () => {
    const onCancel = vi.fn();
    renderWithProviders(<TransactionForm onCancel={onCancel} />);
    await userEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('maps server validation errors onto the matching fields', async () => {
    server.use(mockHandlers.createTransaction.validationError);

    renderWithProviders(<TransactionForm />);
    await fillForm();
    await userEvent.click(screen.getByRole('button', { name: /create transaction/i }));

    expect(await screen.findByText('must not be blank')).toBeInTheDocument();
    expect(screen.getByText('must be greater than 0')).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });
});
