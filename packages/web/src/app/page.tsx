import { Suspense } from 'react';
import Link from 'next/link';
import { LedgerTable } from './_components/LedgerTable';
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

      {/*
       * Suspense is required: useSearchParams() inside LedgerTable reads the
       * URL at render time, which means the component cannot be statically
       * pre-rendered. Wrapping it in Suspense tells Next to render a shell on
       * the server and hydrate the table on the client once the URL is known.
       */}
      <Suspense>
        <LedgerTable />
      </Suspense>
    </main>
  );
}
