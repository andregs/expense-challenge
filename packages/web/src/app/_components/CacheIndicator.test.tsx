import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { CacheIndicator } from './CacheIndicator';

describe('CacheIndicator', () => {
  it('renders the warm FX cache badge', () => {
    render(<CacheIndicator />);
    expect(screen.getByText(/fx cache: warm/i)).toBeInTheDocument();
  });
});
