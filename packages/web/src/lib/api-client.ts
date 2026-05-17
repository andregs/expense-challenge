import createClient from 'openapi-fetch';
import type { paths } from '@expense-challenge/api-contract';
import { TEST_SCENARIO_HEADER, TEST_SCENARIO_QUERY_PARAM } from '@/mocks/scenarios';

/**
 * Typed REST client for the Expense Tracker API. Endpoints, request bodies
 * and response shapes are inferred from the OpenAPI spec via {@link paths}
 * so any contract drift surfaces at compile time.
 *
 * In the browser the client forwards the page's `?test_scenario=` query
 * param as the {@link TEST_SCENARIO_HEADER} request header so MSW handlers
 * can pick the requested variant.
 *
 * All calls go through Next's `/api/*` rewrite (see `next.config.ts`), so
 * the browser sees them as same-origin and CORS never enters the picture.
 * `NEXT_PUBLIC_API_BASE_URL` exists as an escape hatch for environments
 * that need to bypass the proxy (e.g. point a deployed UI at a remote API).
 */
export const apiClient = createClient<paths>({
  baseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? '',
});

apiClient.use({
  onRequest({ request }) {
    if (typeof window === 'undefined') return request;
    const scenarios = new URLSearchParams(window.location.search).get(TEST_SCENARIO_QUERY_PARAM);
    if (scenarios) request.headers.set(TEST_SCENARIO_HEADER, scenarios);
    return request;
  },
});
