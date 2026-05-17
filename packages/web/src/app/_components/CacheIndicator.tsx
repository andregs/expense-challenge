'use client';

import { Badge } from '@/design-system';

/**
 * Placeholder indicator for the Redis FX cache layer. The backend will
 * eventually surface cache hit/miss via a response header on the
 * GET /transactions/{id} endpoint; until then the dashboard renders a
 * static "warm" badge so the slot is visible in the layout.
 */
export function CacheIndicator() {
  return <Badge tone="info">FX cache: warm</Badge>;
}
