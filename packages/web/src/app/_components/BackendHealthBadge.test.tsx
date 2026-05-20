import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BackendHealthBadge } from './BackendHealthBadge';

vi.mock('@/lib/queries/health', () => ({
  useBackendHealth: vi.fn(),
}));

import { useBackendHealth } from '@/lib/queries/health';
const mockHealth = vi.mocked(useBackendHealth);

describe('BackendHealthBadge', () => {
  it('shows success badge when healthy', () => {
    mockHealth.mockReturnValue('healthy');
    render(<BackendHealthBadge />);
    expect(screen.getByText('Backend healthy')).toBeInTheDocument();
  });

  it('shows warning badge when degraded', () => {
    mockHealth.mockReturnValue('degraded');
    render(<BackendHealthBadge />);
    expect(screen.getByText('Backend degraded')).toBeInTheDocument();
  });

  it('shows danger badge when down', () => {
    mockHealth.mockReturnValue('down');
    render(<BackendHealthBadge />);
    expect(screen.getByText('Backend down')).toBeInTheDocument();
  });

  it('shows neutral badge while loading', () => {
    mockHealth.mockReturnValue('loading');
    render(<BackendHealthBadge />);
    expect(screen.getByText('Checking…')).toBeInTheDocument();
  });
});
