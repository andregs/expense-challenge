import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { QueryProviders } from '@/lib/QueryProviders';
import { MockingProvider } from '@/mocks/MockingProvider';
import '@/design-system/styles/index.scss';

export const metadata: Metadata = {
  title: 'Expense Tracker',
  description:
    'Record USD purchase transactions and retrieve them converted into any currency reported by the US Treasury Reporting Rates of Exchange.',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <MockingProvider>
          <QueryProviders>{children}</QueryProviders>
        </MockingProvider>
      </body>
    </html>
  );
}
