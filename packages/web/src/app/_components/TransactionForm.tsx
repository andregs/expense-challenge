'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import type { SyntheticEvent } from 'react';
import { Button, FormField, Input } from '@/design-system';
import {
  TransactionApiError,
  useCreateTransaction,
  type CreateTransactionRequest,
} from '@/lib/queries/transactions';
import styles from './TransactionForm.module.scss';

const DESCRIPTION_MAX = 50;

type FieldName = keyof CreateTransactionRequest;

interface FormState {
  description: string;
  transactionDate: string;
  purchaseAmountUsd: string;
}

const initial: FormState = {
  description: '',
  transactionDate: '',
  purchaseAmountUsd: '',
};

interface TransactionFormProps {
  onSuccess?: (id: string) => void;
  onCancel?: () => void;
}

/**
 * Maps the server's RFC 7807 `errors[]` array onto field-keyed messages so
 * each {@link FormField} can show its own inline error. Falls back to the
 * problem `title` for non-field errors (rendered above the form).
 */
function partitionProblemErrors(error: TransactionApiError): {
  fieldErrors: Partial<Record<FieldName, string>>;
  formError: string | null;
} {
  const fieldErrors: Partial<Record<FieldName, string>> = {};
  for (const entry of error.problem.errors ?? []) {
    if (entry.field in initial) {
      fieldErrors[entry.field as FieldName] = entry.message;
    }
  }
  const hasField = Object.keys(fieldErrors).length > 0;
  return { fieldErrors, formError: hasField ? null : error.problem.title };
}

export function TransactionForm({ onSuccess, onCancel }: TransactionFormProps) {
  const router = useRouter();
  const mutation = useCreateTransaction();
  const [values, setValues] = useState<FormState>(initial);

  const { fieldErrors, formError } =
    mutation.error instanceof TransactionApiError
      ? partitionProblemErrors(mutation.error)
      : { fieldErrors: {}, formError: mutation.error ? 'Unexpected error. Try again.' : null };

  const descLen = values.description.length;
  const descOver = descLen > DESCRIPTION_MAX;

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setValues((prev) => ({ ...prev, [key]: value }));
  }

  function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(
      {
        description: values.description.trim(),
        transactionDate: values.transactionDate,
        purchaseAmountUsd: values.purchaseAmountUsd,
      },
      {
        onSuccess: (tx) => {
          if (onSuccess) {
            onSuccess(tx.id);
          } else {
            router.push(`/transactions/${tx.id}`);
          }
        },
      },
    );
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      {formError ? (
        <p className={styles.summary} role="alert">
          {formError}
        </p>
      ) : null}

      <FormField
        label="Description"
        hint={
          <span className={styles.counter} data-over={descOver}>
            {descLen}/{DESCRIPTION_MAX}
          </span>
        }
        error={fieldErrors.description}
      >
        {(field) => (
          <Input
            {...field}
            name="description"
            value={values.description}
            onChange={(e) => {
              update('description', e.target.value);
            }}
            maxLength={DESCRIPTION_MAX}
            invalid={Boolean(fieldErrors.description) || descOver}
            required
          />
        )}
      </FormField>

      <FormField label="Transaction date" error={fieldErrors.transactionDate}>
        {(field) => (
          <Input
            {...field}
            name="transactionDate"
            type="date"
            value={values.transactionDate}
            onChange={(e) => {
              update('transactionDate', e.target.value);
            }}
            invalid={Boolean(fieldErrors.transactionDate)}
            required
          />
        )}
      </FormField>

      <FormField
        label="Amount (USD)"
        hint="Up to 2 decimal places"
        error={fieldErrors.purchaseAmountUsd}
      >
        {(field) => (
          <Input
            {...field}
            name="purchaseAmountUsd"
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0.01"
            value={values.purchaseAmountUsd}
            onChange={(e) => {
              update('purchaseAmountUsd', e.target.value);
            }}
            invalid={Boolean(fieldErrors.purchaseAmountUsd)}
            required
          />
        )}
      </FormField>

      <div className={styles.actions}>
        <Button
          type="button"
          variant="ghost"
          onClick={() => {
            if (onCancel) {
              onCancel();
            } else {
              router.back();
            }
          }}
        >
          Cancel
        </Button>
        <Button type="submit" disabled={mutation.isPending || descOver}>
          {mutation.isPending ? 'Saving…' : 'Create transaction'}
        </Button>
      </div>
    </form>
  );
}
