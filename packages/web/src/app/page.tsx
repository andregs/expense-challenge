import Link from 'next/link';
import { RecentTransactions } from './_components/RecentTransactions';
import { StatsRow } from './_components/StatsRow';
import styles from './_components/Dashboard.module.scss';

export default function DashboardPage() {
  return (
    <main className={styles.shell}>
      <header className={styles.header}>
        <div>
          <h1>Expense Tracker</h1>
          <p>Record USD purchases. Retrieve them in any Treasury-supported currency.</p>
        </div>
        <Link className={styles.cta} href="/transactions/new">
          New transaction →
        </Link>
      </header>

      <StatsRow />
      <RecentTransactions />
    </main>
  );
}
