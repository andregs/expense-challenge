'use client';

import { Badge, type BadgeTone } from '@/design-system';
import { useBackendHealth, type BackendHealth } from '@/lib/queries/health';

const config: Record<BackendHealth, { tone: BadgeTone; label: string }> = {
  healthy: { tone: 'success', label: 'Backend healthy' },
  degraded: { tone: 'warning', label: 'Backend degraded' },
  down: { tone: 'danger', label: 'Backend down' },
  loading: { tone: 'neutral', label: 'Checking…' },
};

export function BackendHealthBadge() {
  const health = useBackendHealth();
  const { tone, label } = config[health];
  return <Badge tone={tone}>{label}</Badge>;
}
