import { Suspense } from 'react';
import Link from 'next/link';
import type { Metadata } from 'next';
import { LedgerTable } from './_components/LedgerTable';
import styles from './_components/Ledger.module.scss';

export const metadata: Metadata = {
  title: 'Ledger | Expense Tracker',
};

export default function LedgerPage() {
  return (
    <main className={styles.shell}>
      <header className={styles.header}>
        <div>
          <h1>Ledger</h1>
          <p>All recorded transactions, newest first.</p>
        </div>
        <Link className={styles.cta} href="/transactions/new">
          New transaction →
        </Link>
      </header>

      {/*
       * Suspense is required here: useSearchParams() inside LedgerTable reads
       * the URL at render time, which means the component cannot be statically
       * pre-rendered. Wrapping it in Suspense tells Next to render the
       * fallback shell on the server and hydrate the table on the client once
       * the URL is available.
       */}
      <Suspense>
        <LedgerTable />
      </Suspense>
    </main>
  );
}
