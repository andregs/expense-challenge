import type { Metadata } from 'next';
import { TransactionDetail } from './_components/TransactionDetail';
import styles from './_components/TransactionDetail.module.scss';

export const metadata: Metadata = {
  title: 'Transaction | Expense Tracker',
};

export default async function TransactionDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return (
    <main className={styles.shell}>
      <TransactionDetail id={id} />
    </main>
  );
}
