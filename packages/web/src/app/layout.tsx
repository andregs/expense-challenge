import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import './globals.css';

export const metadata: Metadata = {
  title: 'Expense Tracker',
  description:
    'Record USD purchase transactions and retrieve them converted into any currency reported by the US Treasury Reporting Rates of Exchange.',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
