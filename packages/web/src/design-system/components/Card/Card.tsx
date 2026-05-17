import clsx from 'clsx';
import type { HTMLAttributes, ReactNode } from 'react';
import styles from './Card.module.scss';

export interface CardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  title?: ReactNode;
  subtitle?: ReactNode;
  as?: 'section' | 'article' | 'div';
}

export function Card({ title, subtitle, as = 'section', className, children, ...rest }: CardProps) {
  const Tag = as;
  return (
    <Tag className={clsx(styles.root, className)} {...rest}>
      {title ? <h2 className={styles.title}>{title}</h2> : null}
      {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
      {children}
    </Tag>
  );
}
