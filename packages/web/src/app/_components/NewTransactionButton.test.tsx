import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { setupMswServer, renderWithProviders } from '@/test/msw';
import { NewTransactionButton } from './NewTransactionButton';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
}));

setupMswServer('warn');

describe('NewTransactionButton', () => {
  it('renders a button and no modal initially', () => {
    renderWithProviders(<NewTransactionButton />);
    expect(screen.getByRole('button', { name: /new transaction/i })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('opens the modal with the form when the button is clicked', async () => {
    renderWithProviders(<NewTransactionButton />);
    await userEvent.click(screen.getByRole('button', { name: /new transaction/i }));
    expect(screen.getByRole('dialog', { name: /new transaction/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create transaction/i })).toBeVisible();
  });

  it('closes the modal when Cancel is clicked', async () => {
    renderWithProviders(<NewTransactionButton />);
    await userEvent.click(screen.getByRole('button', { name: /new transaction/i }));
    await userEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
