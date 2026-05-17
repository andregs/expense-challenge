import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Badge } from './Badge';

describe('Badge', () => {
  it('applies the tone class when a non-neutral tone is supplied', () => {
    render(<Badge tone="success">Paid</Badge>);
    expect(screen.getByText('Paid')).toHaveClass('root', 'success');
  });

  it('only applies the root class for the neutral tone', () => {
    render(<Badge>Draft</Badge>);
    const badge = screen.getByText('Draft');
    expect(badge).toHaveClass('root');
    expect(badge).not.toHaveClass('success', 'warning', 'danger', 'info');
  });
});
