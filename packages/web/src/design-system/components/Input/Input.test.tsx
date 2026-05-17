import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Input } from './Input';

describe('Input', () => {
  it('marks the field as invalid via aria-invalid when prop is true', () => {
    render(<Input invalid aria-label="amount" />);
    expect(screen.getByLabelText('amount')).toHaveAttribute('aria-invalid', 'true');
  });

  it('omits aria-invalid when the prop is not set', () => {
    render(<Input aria-label="amount" />);
    expect(screen.getByLabelText('amount')).not.toHaveAttribute('aria-invalid');
  });
});
