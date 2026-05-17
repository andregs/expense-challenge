import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from './Button';

describe('Button', () => {
  it('renders as primary at medium size by default', () => {
    render(<Button>Save</Button>);
    const button = screen.getByRole('button', { name: 'Save' });
    expect(button).toHaveClass('root', 'primary');
    expect(button).not.toHaveClass('sm', 'lg');
  });

  it('applies the requested variant and size classes', () => {
    render(
      <Button variant="danger" size="lg">
        Delete
      </Button>,
    );
    expect(screen.getByRole('button')).toHaveClass('danger', 'lg');
  });

  it('defaults the html type attribute to "button" (not "submit")', () => {
    render(<Button>X</Button>);
    expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
  });
});
