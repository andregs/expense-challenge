import clsx from 'clsx';
import type { HTMLAttributes } from 'react';
import styles from './Badge.module.scss';

export type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone;
}

export function Badge({ tone = 'neutral', className, children, ...rest }: BadgeProps) {
  return (
    <span className={clsx(styles.root, tone !== 'neutral' && styles[tone], className)} {...rest}>
      {children}
    </span>
  );
}
