'use client';

import { useQuery } from '@tanstack/react-query';

export type BackendHealth = 'healthy' | 'degraded' | 'down' | 'loading';

interface HealthResponse {
  status: string;
}

async function fetchHealth(path: string): Promise<HealthResponse> {
  const res = await fetch(path, { signal: AbortSignal.timeout(3000) });
  if (!res.ok) throw new Error(`Health check failed: ${res.status.toString()}`);
  return res.json() as Promise<HealthResponse>;
}

/**
 * Derives overall backend health from the two Actuator probe endpoints:
 *
 *   - /actuator/health/liveness  — db only; DOWN means the app is broken.
 *   - /actuator/health/readiness — db + redis + kafka; DOWN means degraded
 *     but still partially functional (FX cache misses, outbox stalls).
 *
 * Both endpoints are polled every 30 s. Network errors are treated as DOWN.
 */
export function useBackendHealth(): BackendHealth {
  const liveness = useQuery<HealthResponse>({
    queryKey: ['health', 'liveness'],
    queryFn: () => fetchHealth('/actuator/health/liveness'),
    refetchInterval: 30_000,
    staleTime: 30_000,
    retry: false,
    enabled: typeof window !== 'undefined',
  });

  const readiness = useQuery<HealthResponse>({
    queryKey: ['health', 'readiness'],
    queryFn: () => fetchHealth('/actuator/health/readiness'),
    refetchInterval: 30_000,
    staleTime: 30_000,
    retry: false,
    enabled: typeof window !== 'undefined',
  });

  if (liveness.isPending || readiness.isPending) return 'loading';
  if (liveness.isError || liveness.data.status !== 'UP') return 'down';
  if (readiness.isError || readiness.data.status !== 'UP') return 'degraded';
  return 'healthy';
}
