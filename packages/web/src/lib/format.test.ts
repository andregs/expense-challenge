import { describe, expect, it } from 'vitest';
import { formatIsoDate, formatUsd } from './format';

describe('formatUsd', () => {
  it('formats string decimals as USD currency to two fractional digits', () => {
    expect(formatUsd('124.5')).toBe('$124.50');
    expect(formatUsd('0')).toBe('$0.00');
  });

  it('accepts plain numbers as input', () => {
    expect(formatUsd(1234.56)).toBe('$1,234.56');
  });
});

describe('formatIsoDate', () => {
  it('renders an ISO date in long-month form', () => {
    expect(formatIsoDate('2026-04-12')).toBe('Apr 12, 2026');
  });

  it('returns the input verbatim when it cannot be parsed', () => {
    expect(formatIsoDate('not-a-date')).toBe('not-a-date');
  });
});
