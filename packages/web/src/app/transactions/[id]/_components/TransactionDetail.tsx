'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useQueryClient } from '@tanstack/react-query';
import { Card } from '@/design-system';
import { formatIsoDate, formatUsd } from '@/lib/format';
import {
  TransactionApiError,
  useGetTransaction,
  type ConvertedTransaction,
  type Transaction,
} from '@/lib/queries/transactions';
import { ConversionResult } from './ConversionResult';
import { CurrencySelect } from './CurrencySelect';
import styles from './TransactionDetail.module.scss';

export function TransactionDetail({ id }: { id: string }) {
  const [currency, setCurrency] = useState('');
  const queryClient = useQueryClient();
  const { data, isPending, isFetching, error } = useGetTransaction(id, currency || undefined);

  // When a conversion error clears `data`, fall back to the base transaction
  // already cached under the no-currency key so the header stays visible.
  const cachedBase = queryClient.getQueryData<Transaction | ConvertedTransaction>([
    'transaction',
    id,
    undefined,
  ]);
  const tx = data ?? cachedBase;

  if (isPending) return <p className={styles.empty}>Loading…</p>;

  if (!tx) {
    const is404 = error instanceof TransactionApiError && error.httpStatus === 404;
    return (
      <p className={styles.error}>
        {is404 ? 'Transaction not found.' : 'Failed to load transaction.'}
      </p>
    );
  }

  return (
    <>
      <Link href="/ledger" className={styles.backLink}>
        ← Back to ledger
      </Link>

      <Card title={tx.description}>
        <dl className={styles.txMeta}>
          <div className={styles.metaRow}>
            <dt>Date</dt>
            <dd>{formatIsoDate(tx.transactionDate)}</dd>
          </div>
          <div className={styles.metaRow}>
            <dt>Amount (USD)</dt>
            <dd className={styles.amount}>{formatUsd(tx.purchaseAmountUsd)}</dd>
          </div>
        </dl>
      </Card>

      <Card title="Currency conversion">
        <CurrencySelect value={currency} onChange={setCurrency} disabled={isFetching} />
        <ConversionSection currency={currency} isFetching={isFetching} data={data} error={error} />
      </Card>
    </>
  );
}

function ConversionSection({
  currency,
  isFetching,
  data,
  error,
}: {
  currency: string;
  isFetching: boolean;
  data: ReturnType<typeof useGetTransaction>['data'];
  error: ReturnType<typeof useGetTransaction>['error'];
}) {
  if (!currency) return null;
  if (isFetching) return <p className={styles.empty}>Converting…</p>;
  if (error) {
    const is422 = error instanceof TransactionApiError && error.httpStatus === 422;
    return (
      <p className={styles.error} role="alert">
        {is422
          ? 'No exchange rate available within 6 months for this currency.'
          : 'Failed to retrieve conversion.'}
      </p>
    );
  }
  if (data && isConverted(data)) return <ConversionResult data={data} />;
  return null;
}

function isConverted(
  tx: NonNullable<ReturnType<typeof useGetTransaction>['data']>,
): tx is ConvertedTransaction {
  return 'currency' in tx;
}
