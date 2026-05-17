import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Card } from './Card';

describe('Card', () => {
  it('renders title and subtitle only when provided', () => {
    const { rerender } = render(<Card>content</Card>);
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();

    rerender(
      <Card title="Total" subtitle="USD">
        content
      </Card>,
    );
    expect(screen.getByRole('heading', { name: 'Total' })).toBeInTheDocument();
    expect(screen.getByText('USD')).toBeInTheDocument();
  });

  it('renders as the requested polymorphic tag', () => {
    const { container } = render(<Card as="article">x</Card>);
    expect(container.querySelector('article')).not.toBeNull();
  });
});
