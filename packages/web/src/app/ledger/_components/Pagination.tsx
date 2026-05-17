import Link from 'next/link';
import styles from './Ledger.module.scss';

interface PaginationProps {
  page: number;
  totalPages: number;
}

export function Pagination({ page, totalPages }: PaginationProps) {
  const hasPrev = page > 0;
  const hasNext = page < totalPages - 1;

  return (
    <nav className={styles.pagination} aria-label="Pagination">
      {hasPrev ? (
        <Link href={`?page=${(page - 1).toString()}`} className={styles.pageBtn}>
          ← Previous
        </Link>
      ) : (
        <span className={styles.pageBtnDisabled} aria-disabled="true">
          ← Previous
        </span>
      )}

      <span className={styles.pageInfo}>
        Page {page + 1} of {totalPages}
      </span>

      {hasNext ? (
        <Link href={`?page=${(page + 1).toString()}`} className={styles.pageBtn}>
          Next →
        </Link>
      ) : (
        <span className={styles.pageBtnDisabled} aria-disabled="true">
          Next →
        </span>
      )}
    </nav>
  );
}
