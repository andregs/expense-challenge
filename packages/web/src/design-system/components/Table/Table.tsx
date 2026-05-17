import clsx from 'clsx';
import type { TableHTMLAttributes } from 'react';
import styles from './Table.module.scss';

export interface TableProps extends TableHTMLAttributes<HTMLTableElement> {
  caption?: string;
}

export function Table({ caption, className, children, ...rest }: TableProps) {
  return (
    <div className={styles.wrapper}>
      <table className={clsx(styles.table, className)} {...rest}>
        {caption ? <caption className={styles.caption}>{caption}</caption> : null}
        {children}
      </table>
    </div>
  );
}
