'use client';

import Link from 'next/link';
import { Card, Table } from '@/design-system';
import { formatIsoDate, formatUsd } from '@/lib/format';
import { useTransactionsPage } from '@/lib/queries/transactions';
import styles from './Dashboard.module.scss';

export function RecentTransactions() {
  const { data, isPending, isError } = useTransactionsPage(0, 5);

  return (
    <Card
      title="Recent transactions"
      subtitle={
        <>
          Latest 5 across the ledger &mdash;{' '}
          <Link className={styles.cta} href="/ledger">
            View all →
          </Link>
        </>
      }
    >
      <RecentTransactionsBody data={data} isPending={isPending} isError={isError} />
    </Card>
  );
}

function RecentTransactionsBody({
  data,
  isPending,
  isError,
}: {
  data: ReturnType<typeof useTransactionsPage>['data'];
  isPending: boolean;
  isError: boolean;
}) {
  if (isPending) return <p className={styles.empty}>Loading…</p>;
  if (isError || !data) return <p className={styles.error}>Failed to load transactions.</p>;
  if (data.items.length === 0) return <p className={styles.empty}>No transactions recorded yet.</p>;

  return (
    <Table caption="Recent transactions">
      <thead>
        <tr>
          <th scope="col">Date</th>
          <th scope="col">Description</th>
          <th scope="col" className={styles.cellAmount}>
            USD
          </th>
        </tr>
      </thead>
      <tbody>
        {data.items.map((tx) => (
          <tr key={tx.id}>
            <td>{formatIsoDate(tx.transactionDate)}</td>
            <td>{tx.description}</td>
            <td className={styles.cellAmount}>{formatUsd(tx.purchaseAmountUsd)}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
