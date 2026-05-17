import clsx from 'clsx';
import type { HTMLAttributes, ReactNode } from 'react';
import { useId } from 'react';
import styles from './FormField.module.scss';

export interface FormFieldRenderProps {
  id: string;
  'aria-describedby': string | undefined;
}

export interface FormFieldProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  label: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  htmlFor?: string;
  children: (props: FormFieldRenderProps) => ReactNode;
}

export function FormField({
  label,
  hint,
  error,
  htmlFor,
  className,
  children,
  ...rest
}: FormFieldProps) {
  const generatedId = useId();
  const id = htmlFor ?? generatedId;
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy =
    [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined;

  return (
    <div className={clsx(styles.root, className)} {...rest}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      {children({ id, 'aria-describedby': describedBy })}
      {hint ? (
        <span id={hintId} className={styles.hint}>
          {hint}
        </span>
      ) : null}
      {error ? (
        <span id={errorId} className={styles.error} role="alert">
          {error}
        </span>
      ) : null}
    </div>
  );
}
