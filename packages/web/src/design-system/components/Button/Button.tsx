import clsx from 'clsx';
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { forwardRef } from 'react';
import styles from './Button.module.scss';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  iconLeft?: ReactNode;
  iconRight?: ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', size = 'md', className, iconLeft, iconRight, children, type, ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type ?? 'button'}
      className={clsx(styles.root, styles[variant], size !== 'md' && styles[size], className)}
      {...rest}
    >
      {iconLeft}
      {children}
      {iconRight}
    </button>
  );
});
