import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Pagination } from './Pagination';

describe('Pagination', () => {
  it('disables Previous on the first page', () => {
    render(<Pagination page={0} totalPages={5} />);
    expect(screen.queryByRole('link', { name: /previous/i })).toBeNull();
    expect(screen.getByText(/← Previous/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /next/i })).toHaveAttribute('href', '?page=1');
  });

  it('enables both controls on an interior page', () => {
    render(<Pagination page={2} totalPages={5} />);
    expect(screen.getByRole('link', { name: /← Previous/i })).toHaveAttribute('href', '?page=1');
    expect(screen.getByRole('link', { name: /Next →/i })).toHaveAttribute('href', '?page=3');
  });

  it('disables Next on the last page', () => {
    render(<Pagination page={4} totalPages={5} />);
    expect(screen.getByRole('link', { name: /previous/i })).toHaveAttribute('href', '?page=3');
    expect(screen.queryByRole('link', { name: /next/i })).toBeNull();
    expect(screen.getByText(/Next →/)).toBeInTheDocument();
  });

  it('renders the correct page label', () => {
    render(<Pagination page={1} totalPages={7} />);
    expect(screen.getByText('Page 2 of 7')).toBeInTheDocument();
  });
});
