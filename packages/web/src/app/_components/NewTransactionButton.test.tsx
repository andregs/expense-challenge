import type { ReactNode } from 'react';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { server } from '@/mocks/server';
import { NewTransactionButton } from './NewTransactionButton';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
}));

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'warn' });
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

describe('NewTransactionButton', () => {
  it('renders a button and no modal initially', () => {
    render(wrap(<NewTransactionButton />));
    expect(screen.getByRole('button', { name: /new transaction/i })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('opens the modal with the form when the button is clicked', async () => {
    render(wrap(<NewTransactionButton />));
    await userEvent.click(screen.getByRole('button', { name: /new transaction/i }));
    expect(screen.getByRole('dialog', { name: /new transaction/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create transaction/i })).toBeVisible();
  });

  it('closes the modal when Cancel is clicked', async () => {
    render(wrap(<NewTransactionButton />));
    await userEvent.click(screen.getByRole('button', { name: /new transaction/i }));
    await userEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
