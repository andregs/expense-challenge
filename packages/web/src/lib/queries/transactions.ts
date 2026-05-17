'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { components } from '@expense-challenge/api-contract';
import { apiClient } from '@/lib/api-client';

export type TransactionPage = components['schemas']['TransactionPage'];
export type Transaction = components['schemas']['Transaction'];
export type ConvertedTransaction = components['schemas']['ConvertedTransaction'];
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
 * Fetches a single transaction by id. When `currency` is supplied the API
 * returns a {@link ConvertedTransaction} enriched with `exchangeRate`,
 * `convertedAmount` and `rateDate`. On 4xx/5xx throws a
 * {@link TransactionApiError} so callers can distinguish 404 (not found)
 * from 422 (no qualifying rate within the prior 6 months).
 *
 * Pass `enabled = false` to keep the hook mounted but dormant (e.g. the
 * conversion query before the user has picked a currency).
 */
export function useGetTransaction(id: string, currency?: string) {
  return useQuery({
    queryKey: ['transaction', id, currency],
    queryFn: async (): Promise<Transaction | ConvertedTransaction> => {
      const { data, error, response } = await apiClient.GET('/api/v1/transactions/{id}', {
        params: {
          path: { id },
          query: currency ? { currency } : {},
        },
      });
      if (error) throw new TransactionApiError(error, response.status);
      return data;
    },
    // Keep previous key's data visible while re-fetching (e.g. currency change)
    // so the transaction header stays populated during the conversion request.
    placeholderData: (prev) => prev,
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
