'use client';

import { useQuery } from '@tanstack/react-query';
import type { components } from '@expense-challenge/api-contract';
import { apiClient } from '@/lib/api-client';

export type TransactionPage = components['schemas']['TransactionPage'];

export const transactionsQueryKey = (page: number, size: number) =>
  ['transactions', { page, size }] as const;

/**
 * Fetches a single page of transactions. The hook is shared by the
 * dashboard stats and the recent-transactions table so they hit the
 * react-query cache instead of duplicating the network call.
 */
export function useTransactionsPage(page = 0, size = 20) {
  return useQuery({
    queryKey: transactionsQueryKey(page, size),
    queryFn: async (): Promise<TransactionPage> => {
      const { data, error } = await apiClient.GET('/api/v1/transactions', {
        params: { query: { page, size } },
      });
      if (error) throw new Error('Failed to load transactions');
      return data;
    },
    // Skip the fetch during server-side rendering. The page ships its
    // initial HTML in the `isPending` state and the real request fires
    // after hydration, where MSW (a browser-only library) can intercept.
    // TODO: replace with HydrationBoundary + prefetchQuery on the server
    //       component once MSW supports a Node.js service-worker equivalent,
    //       eliminating the pending-state flash in production.
    enabled: typeof window !== 'undefined',
  });
}
