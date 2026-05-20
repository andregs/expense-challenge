import { http, HttpResponse, type HttpResponseResolver } from 'msw';
import { fixtures } from './data';
import { selectVariant } from './scenarios';

const problemHeaders = { 'Content-Type': 'application/problem+json' };

const { transactions: tx, problems } = fixtures;

type HttpMethod = 'get' | 'post' | 'put' | 'patch' | 'delete';
type Resolver = HttpResponseResolver;

interface RouteSpec<V extends Record<string, Resolver>> {
  method: HttpMethod;
  path: string;
  variants: V;
  default: keyof V & string;
}

/**
 * Each route is declared once with its full variant table and a default
 * pick. From a single declaration we derive two surfaces:
 *
 *   1. `handlers` — one MSW handler per route that defers variant selection
 *      to {@link selectVariant} on the `X-Test-Scenario` request header.
 *      Used by dev (browser worker) and by tests that want the default
 *      behaviour.
 *
 *   2. `mockHandlers` — a route → variant → MSW handler map. Tests reach
 *      for `server.use(mockHandlers.createTransaction.validationError)` to
 *      pin a specific response without re-declaring path/method/body.
 */
const routes = {
  listTransactions: {
    method: 'get',
    path: '*/api/v1/transactions',
    variants: {
      happy: () => HttpResponse.json(tx.samplePage),
      empty: () => HttpResponse.json(tx.emptyPage),
      serverError: () =>
        HttpResponse.json(problems.serverError, { status: 500, headers: problemHeaders }),
    },
    default: 'happy',
  },
  createTransaction: {
    method: 'post',
    path: '*/api/v1/transactions',
    variants: {
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
    default: 'happy',
  },
  getTransaction: {
    method: 'get',
    path: '*/api/v1/transactions/:id',
    variants: {
      happy: ({ request }) => {
        const currency = new URL(request.url).searchParams.get('currency');
        return HttpResponse.json(currency ? tx.sampleConverted : tx.singleTransaction);
      },
      notFound: () =>
        HttpResponse.json(problems.notFound, { status: 404, headers: problemHeaders }),
      unconvertible: ({ request }) => {
        const currency = new URL(request.url).searchParams.get('currency');
        if (!currency) return HttpResponse.json(tx.singleTransaction);
        return HttpResponse.json(problems.unprocessable, { status: 422, headers: problemHeaders });
      },
      serverError: () =>
        HttpResponse.json(problems.serverError, { status: 500, headers: problemHeaders }),
    },
    default: 'happy',
  },
  evictTransactionCache: {
    method: 'delete',
    path: '*/api/v1/transactions/:id/cache',
    variants: {
      happy: () => new HttpResponse(null, { status: 204 }),
      notFound: () =>
        HttpResponse.json(problems.notFound, { status: 404, headers: problemHeaders }),
    },
    default: 'happy',
  },
  healthLiveness: {
    method: 'get',
    path: '*/actuator/health/liveness',
    variants: {
      happy: () => HttpResponse.json({ status: 'UP' }),
      serverError: () => HttpResponse.json({ status: 'DOWN' }, { status: 503 }),
    },
    default: 'happy',
  },
  healthReadiness: {
    method: 'get',
    path: '*/actuator/health/readiness',
    variants: {
      happy: () => HttpResponse.json({ status: 'UP' }),
      serverError: () => HttpResponse.json({ status: 'DOWN' }, { status: 503 }),
    },
    default: 'happy',
  },
} as const satisfies Record<string, RouteSpec<Record<string, Resolver>>>;

type Routes = typeof routes;

export const handlers = Object.entries(routes).map(([name, route]) =>
  http[route.method](route.path, (info) => {
    const variant = selectVariant(info.request, name, route.variants, route.default);
    return variant(info);
  }),
);

/**
 * Per-route, per-variant standalone handlers. Each one matches the same
 * path/method as its scenario-aware sibling but unconditionally returns the
 * named variant — handy for `server.use(...)` in tests.
 */
export const mockHandlers = Object.fromEntries(
  Object.entries(routes).map(([name, route]) => [
    name,
    Object.fromEntries(
      Object.entries(route.variants).map(([variant, resolver]) => [
        variant,
        http[route.method](route.path, resolver),
      ]),
    ),
  ]),
) as {
  [K in keyof Routes]: {
    [V in keyof Routes[K]['variants']]: ReturnType<(typeof http)[Routes[K]['method']]>;
  };
};
