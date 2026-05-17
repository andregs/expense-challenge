/**
 * Scenario plumbing for MSW handler selection.
 *
 * Front-end developers steer mock responses without touching code by passing
 * `?test_scenario=<handlerName>:<variant>,<handlerName>:<variant>` on the
 * page URL. The API client lifts this query param onto every outgoing
 * request as the `X-Test-Scenario` header; handlers read the header and
 * pick a matching pre-canned response from their variant table. When a
 * handler is not mentioned in the header it falls back to its declared
 * default variant.
 *
 * Handlers never compute on input — every variant is a hardcoded fixture.
 * Business logic lives on the backend.
 */

export const TEST_SCENARIO_HEADER = 'x-test-scenario';
export const TEST_SCENARIO_QUERY_PARAM = 'test_scenario';

/**
 * Parses a scenario header value into a `Map<handlerName, variant>`. Tolerant
 * to extra whitespace, blank pairs and malformed entries (silently skipped).
 */
export function parseScenarioHeader(value: string | null | undefined): Map<string, string> {
  const map = new Map<string, string>();
  if (!value) return map;
  for (const raw of value.split(',')) {
    const trimmed = raw.trim();
    if (!trimmed) continue;
    const [name, variant] = trimmed.split(':').map((s) => s.trim());
    if (name && variant) map.set(name, variant);
  }
  return map;
}

/**
 * Picks a variant for `handlerName` based on the request's
 * `X-Test-Scenario` header, falling back to `defaultVariant` when the
 * header is missing the entry or names a variant outside `variants`.
 */
export function selectVariant<TVariants extends Record<string, () => Response>>(
  request: Request,
  handlerName: string,
  variants: TVariants,
  defaultVariant: keyof TVariants & string,
): Response {
  const header = request.headers.get(TEST_SCENARIO_HEADER);
  const map = parseScenarioHeader(header);
  const requested = map.get(handlerName);
  const variant: keyof TVariants & string =
    requested && requested in variants ? requested : defaultVariant;
  const handler = variants[variant];
  if (!handler) {
    throw new Error(`Missing scenario "${variant}" for mock handler "${handlerName}"`);
  }
  return handler();
}
