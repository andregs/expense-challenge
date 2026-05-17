/**
 * Static fixtures used by MSW handler variants. Each constant is a frozen
 * canonical response shape — handlers never mutate or compute on these.
 * Business behaviour belongs to the backend; mocks return canned data.
 */

import type { components } from '@expense-challenge/api-contract';

type Transaction = components['schemas']['Transaction'];
type ConvertedTransaction = components['schemas']['ConvertedTransaction'];
type TransactionPage = components['schemas']['TransactionPage'];
type Problem = components['schemas']['Problem'];

const transactions = [
  {
    id: '11111111-1111-4111-8111-111111111111',
    description: 'Office supplies',
    transactionDate: '2026-04-12',
    purchaseAmountUsd: '124.99',
  },
  {
    id: '22222222-2222-4222-8222-222222222222',
    description: 'Conference ticket',
    transactionDate: '2026-03-04',
    purchaseAmountUsd: '899.00',
  },
  {
    id: '33333333-3333-4333-8333-333333333333',
    description: 'Team lunch',
    transactionDate: '2026-05-08',
    purchaseAmountUsd: '212.50',
  },
] as const satisfies readonly Transaction[];

const samplePage: TransactionPage = {
  items: [...transactions],
  page: 0,
  size: 20,
  totalElements: transactions.length,
  totalPages: 1,
};

const emptyPage: TransactionPage = {
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

const singleTransaction: Transaction = transactions[0];

const sampleConverted: ConvertedTransaction = {
  ...singleTransaction,
  currency: 'BRL',
  exchangeRate: '5.123400',
  convertedAmount: '640.40',
  rateDate: '2026-03-31',
};

const validationProblem: Problem = {
  type: 'about:blank',
  title: 'Validation failed',
  status: 400,
  errors: [
    { field: 'description', message: 'must not be blank' },
    { field: 'purchaseAmountUsd', message: 'must be greater than 0' },
  ],
};

const notFoundProblem: Problem = {
  type: 'about:blank',
  title: 'Transaction not found',
  status: 404,
};

const unprocessableProblem: Problem = {
  type: 'about:blank',
  title: 'No exchange rate available within 6 months for this currency',
  status: 422,
};

const serverErrorProblem: Problem = {
  type: 'about:blank',
  title: 'Internal server error',
  status: 500,
};

export const fixtures = {
  transactions: {
    samplePage,
    emptyPage,
    singleTransaction,
    sampleConverted,
  },
  problems: {
    validation: validationProblem,
    notFound: notFoundProblem,
    unprocessable: unprocessableProblem,
    serverError: serverErrorProblem,
  },
} as const;
