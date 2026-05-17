import { http, HttpResponse } from 'msw';
import { fixtures } from './data';
import { selectVariant } from './scenarios';

const problemHeaders = { 'Content-Type': 'application/problem+json' };

const { transactions: tx, problems } = fixtures;

/**
 * Each handler maps {@link selectVariant} over a static variant table. To
 * change which variant is returned, pass `?test_scenario=<handler>:<variant>`
 * on the page URL. The API client forwards the query as the
 * `X-Test-Scenario` request header.
 *
 * The `*` prefix in each path matcher lets handlers fire regardless of
 * origin so the same setup works whether the API client is hitting Next's
 * `/api/*` rewrite, an absolute URL via `NEXT_PUBLIC_API_BASE_URL`, or the
 * Vitest jsdom default origin.
 */
export const handlers = [
  http.get('*/api/v1/transactions', ({ request }) =>
    selectVariant(
      request,
      'listTransactions',
      {
        happy: () => HttpResponse.json(tx.samplePage),
        empty: () => HttpResponse.json(tx.emptyPage),
        serverError: () =>
          HttpResponse.json(problems.serverError, { status: 500, headers: problemHeaders }),
      },
      'happy',
    ),
  ),

  http.post('*/api/v1/transactions', ({ request }) =>
    selectVariant(
      request,
      'createTransaction',
      {
        happy: () =>
          HttpResponse.json(tx.singleTransaction, {
            status: 201,
            headers: { Location: `/api/v1/transactions/${tx.singleTransaction.id}` },
          }),
        validationError: () =>
          HttpResponse.json(problems.validation, { status: 400, headers: problemHeaders }),
        serverError: () =>
          HttpResponse.json(problems.serverError, { status: 500, headers: problemHeaders }),
      },
      'happy',
    ),
  ),

  http.get('*/api/v1/transactions/:id', ({ request }) => {
    const currency = new URL(request.url).searchParams.get('currency');
    return selectVariant(
      request,
      'getTransaction',
      {
        happy: () => HttpResponse.json(currency ? tx.sampleConverted : tx.singleTransaction),
        notFound: () =>
          HttpResponse.json(problems.notFound, { status: 404, headers: problemHeaders }),
        unconvertible: () =>
          HttpResponse.json(problems.unprocessable, { status: 422, headers: problemHeaders }),
        serverError: () =>
          HttpResponse.json(problems.serverError, { status: 500, headers: problemHeaders }),
      },
      'happy',
    );
  }),
];
