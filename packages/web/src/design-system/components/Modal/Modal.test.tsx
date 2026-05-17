import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Modal } from './Modal';

describe('Modal', () => {
  it('does not expose a dialog role when closed', () => {
    render(
      <Modal open={false} title="Confirm">
        body
      </Modal>,
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('opens the dialog with the title as its accessible name', () => {
    render(
      <Modal open title="Confirm">
        body
      </Modal>,
    );
    expect(screen.getByRole('dialog', { name: 'Confirm' })).toBeInTheDocument();
  });

  it('opens and closes the underlying dialog as the open prop toggles', () => {
    const { rerender } = render(
      <Modal open={false} title="Confirm">
        body
      </Modal>,
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    rerender(
      <Modal open title="Confirm">
        body
      </Modal>,
    );
    expect(screen.getByRole('dialog')).toBeVisible();

    rerender(
      <Modal open={false} title="Confirm">
        body
      </Modal>,
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('forwards the native close event to onClose', () => {
    const onClose = vi.fn();
    render(
      <Modal open title="Confirm" onClose={onClose}>
        body
      </Modal>,
    );
    const dialog = screen.getByRole('dialog');
    (dialog as HTMLDialogElement).close();
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
