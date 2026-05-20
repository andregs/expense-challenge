'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button, Modal } from '@/design-system';
import { TransactionForm } from './TransactionForm';

export function NewTransactionButton() {
  const [open, setOpen] = useState(false);
  const router = useRouter();

  function handleSuccess(id: string) {
    setOpen(false);
    router.push(`/transactions/${id}`);
  }

  return (
    <>
      <Button
        onClick={() => {
          setOpen(true);
        }}
      >
        New transaction
      </Button>
      <Modal
        open={open}
        onClose={() => {
          setOpen(false);
        }}
        title="New transaction"
      >
        <TransactionForm
          onSuccess={handleSuccess}
          onCancel={() => {
            setOpen(false);
          }}
        />
      </Modal>
    </>
  );
}
