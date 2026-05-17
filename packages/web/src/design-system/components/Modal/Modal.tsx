'use client';

import clsx from 'clsx';
import type { DialogHTMLAttributes, MouseEvent, ReactNode } from 'react';
import { useEffect, useRef } from 'react';
import styles from './Modal.module.scss';

export interface ModalProps extends Omit<DialogHTMLAttributes<HTMLDialogElement>, 'title' | 'open'> {
  open: boolean;
  title?: ReactNode;
  onClose?: () => void;
}

/**
 * Wraps the native HTML `<dialog>` element. Opening calls `showModal()` so
 * the browser handles focus trap, scroll locking, the `::backdrop` pseudo
 * element and Escape-to-close for us. The native `close` event covers all
 * three close paths (Escape, the close() API and form `method="dialog"`
 * submissions); we forward it via `onClose`.
 */
export function Modal({ open, title, onClose, className, children, ...rest }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog || !onClose) return;
    const handle = () => {
      onClose();
    };
    dialog.addEventListener('close', handle);
    return () => {
      dialog.removeEventListener('close', handle);
    };
  }, [onClose]);

  function onBackdropClick(event: MouseEvent<HTMLDialogElement>) {
    if (event.target === ref.current) ref.current.close();
  }

  return (
    <dialog
      ref={ref}
      aria-label={typeof title === 'string' ? title : undefined}
      className={clsx(styles.dialog, className)}
      onClick={onBackdropClick}
      {...rest}
    >
      {title ? <h2 className={styles.title}>{title}</h2> : null}
      {children}
    </dialog>
  );
}
