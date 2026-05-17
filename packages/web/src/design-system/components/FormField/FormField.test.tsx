import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FormField } from './FormField';
import { Input } from '../Input/Input';

describe('FormField', () => {
  it('wires the label id to the rendered child input', () => {
    render(
      <FormField label="Description">
        {(props) => <Input {...props} aria-label="desc" />}
      </FormField>,
    );
    const label = screen.getByText('Description');
    const input = screen.getByLabelText('desc');
    expect(label).toHaveAttribute('for', input.id);
  });

  it('composes aria-describedby from hint and error ids when both are present', () => {
    render(
      <FormField label="Amount" hint="Enter cents" error="too low">
        {(props) => <Input {...props} aria-label="amount" />}
      </FormField>,
    );
    const input = screen.getByLabelText('amount');
    const describedBy = input.getAttribute('aria-describedby') ?? '';
    expect(describedBy).toMatch(/-hint/);
    expect(describedBy).toMatch(/-error/);
    expect(screen.getByRole('alert')).toHaveTextContent('too low');
  });

  it('leaves aria-describedby unset when there is no hint or error', () => {
    render(
      <FormField label="Plain">
        {(props) => <Input {...props} aria-label="plain" />}
      </FormField>,
    );
    expect(screen.getByLabelText('plain')).not.toHaveAttribute('aria-describedby');
  });
});
