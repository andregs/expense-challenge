'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { components } from '@expense-challenge/api-contract';
import { apiClient } from '@/lib/api-client';

export type TransactionPage = components['schemas']['TransactionPage'];
export type Transaction = components['schemas']['Transaction'];
export type CreateTransactionRequest = components['schemas']['CreateTransactionRequest'];
export type Problem = components['schemas']['Problem'];

/**
 * Error thrown by {@link useCreateTransaction} when the API rejects the
 * payload. Carries the RFC 7807 problem document verbatim so the form can
 * surface field-level messages from `problem.errors`.
 */
export class TransactionApiError extends Error {
  constructor(
    public readonly problem: Problem,
    public readonly httpStatus: number,
  ) {
    super(problem.title);
    this.name = 'TransactionApiError';
  }
}

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

/**
 * Posts a new transaction. On success invalidates the listing cache so the
 * dashboard and ledger refetch on next render. On 4xx/5xx throws a
 * {@link TransactionApiError} carrying the parsed problem document.
 */
export function useCreateTransaction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateTransactionRequest): Promise<Transaction> => {
      const result = await apiClient.POST('/api/v1/transactions', { body });
      if (result.error) {
        throw new TransactionApiError(result.error, result.response.status);
      }
      return result.data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['transactions'] }),
  });
}
