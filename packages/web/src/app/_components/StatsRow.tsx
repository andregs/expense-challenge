'use client';

import { Card } from '@/design-system';
import { formatUsd } from '@/lib/format';
import { useTransactionsPage } from '@/lib/queries/transactions';
import { CacheIndicator } from './CacheIndicator';
import styles from './Dashboard.module.scss';

export function StatsRow() {
  const { data, isPending, isError } = useTransactionsPage(0, 5);

  let totalLabel: string;
  let countLabel: string | number;
  if (isPending) {
    totalLabel = '—';
    countLabel = '—';
  } else if (isError) {
    totalLabel = '!';
    countLabel = '!';
  } else {
    const total = data.items.reduce((acc, tx) => acc + Number(tx.purchaseAmountUsd), 0);
    totalLabel = formatUsd(total);
    countLabel = data.totalElements;
  }

  return (
    <section className={styles.statsRow}>
      <Card title="Total USD spend" subtitle="Across loaded transactions">
        <p className={styles.statValue}>{totalLabel}</p>
      </Card>

      <Card title="Transactions" subtitle="Total recorded">
        <p className={styles.statValue}>{countLabel}</p>
      </Card>

      <Card title="Infra signal" subtitle="Cache health" className={styles.cacheCard}>
        <CacheIndicator />
      </Card>
    </section>
  );
}
