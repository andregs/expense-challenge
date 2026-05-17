import type { Metadata } from 'next';
import { TransactionForm } from './_components/TransactionForm';

export const metadata: Metadata = {
  title: 'New transaction · Expense Tracker',
};

export default function NewTransactionPage() {
  return (
    <main>
      <h1>New transaction</h1>
      <p>Record a USD purchase. Convert it to another currency from the detail page.</p>
      <TransactionForm />
    </main>
  );
}
