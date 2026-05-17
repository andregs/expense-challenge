/**
 * Pre-built Intl formatters reused across the UI. Strings come from the
 * API as decimal text (BigDecimal-safe), so callers parse them through
 * `Number` before handing the value to a formatter. Formatters are
 * cached at module load — constructing an `Intl.NumberFormat` is the
 * expensive part of the call site.
 */

export const usdFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export const isoDateFormatter = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: '2-digit',
  timeZone: 'UTC',
});

export function formatUsd(value: string | number): string {
  return usdFormatter.format(typeof value === 'string' ? Number(value) : value);
}

export function formatIsoDate(iso: string): string {
  const [year, month, day] = iso.split('-').map(Number);
  if (!year || !month || !day) return iso;
  return isoDateFormatter.format(new Date(Date.UTC(year, month - 1, day)));
}
