import createClient from 'openapi-fetch';
import type { paths } from '@expense-challenge/api-contract';
import { TEST_SCENARIO_HEADER, TEST_SCENARIO_QUERY_PARAM } from '@/mocks/scenarios';

// Browser `fetch` resolves relative URLs against `window.location`, so an
// empty base works in production once the Next.js rewrite is wired up.
// Node `fetch` (used by jsdom under Vitest, and by Next during SSR) rejects
// relative URLs, so we anchor on `window.location.origin` whenever it is
// available. The explicit env-var override still wins for deployments that
// point the UI at a remote API.
const baseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  (typeof window === 'undefined' ? '' : window.location.origin);

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
 *
 * The `fetch` thunk re-reads `globalThis.fetch` per request: openapi-fetch
 * captures the global at construction time, but MSW only patches it later
 * inside `server.listen()`. Without the thunk, tests would bypass MSW and
 * hit the real network. See mswjs/msw#2180.
 */
export const apiClient = createClient<paths>({
  baseUrl,
  fetch: (input: Request) => globalThis.fetch(input),
});

apiClient.use({
  onRequest({ request }) {
    if (typeof window === 'undefined') return request;
    const scenarios = new URLSearchParams(window.location.search).get(TEST_SCENARIO_QUERY_PARAM);
    if (scenarios) request.headers.set(TEST_SCENARIO_HEADER, scenarios);
    return request;
  },
});
