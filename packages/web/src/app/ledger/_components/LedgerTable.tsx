'use client';

import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Card, Table } from '@/design-system';
import { formatIsoDate, formatUsd } from '@/lib/format';
import { useTransactionsPage } from '@/lib/queries/transactions';
import { Pagination } from './Pagination';
import styles from './Ledger.module.scss';

const PAGE_SIZE = 10;

export function LedgerTable() {
  const searchParams = useSearchParams();
  const rawPage = Number(searchParams.get('page') ?? '0');
  const page = Number.isNaN(rawPage) ? 0 : Math.max(0, rawPage);
  const { data, isPending, isError } = useTransactionsPage(page, PAGE_SIZE);

  const subtitle = data ? `${data.totalElements.toString()} total` : undefined;

  return (
    <Card title="Transactions" subtitle={subtitle}>
      <LedgerBody data={data} isPending={isPending} isError={isError} />
      {data && data.totalPages > 1 ? (
        <div className={styles.paginationWrapper}>
          <Pagination page={page} totalPages={data.totalPages} />
        </div>
      ) : null}
    </Card>
  );
}

function LedgerBody({
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
    <Table caption="All transactions">
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
          <tr key={tx.id} className={styles.row}>
            <td>{formatIsoDate(tx.transactionDate)}</td>
            <td>
              <Link href={`/transactions/${tx.id}`} className={styles.rowLink}>
                {tx.description}
              </Link>
            </td>
            <td className={styles.cellAmount}>{formatUsd(tx.purchaseAmountUsd)}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
