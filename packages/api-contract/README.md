# @expense-challenge/api-contract

OpenAPI 3.1 contract for the Expense Tracker API and the TypeScript types generated from it.

The spec is the single source of truth: the Spring Boot service implements it, the Next.js frontend imports the generated types, and MSW mock handlers shape their responses against it.

## Generate types

```bash
pnpm --filter @expense-challenge/api-contract generate
```

This runs [`openapi-typescript`](https://openapi-ts.dev/) against `openapi.yaml` and writes `generated/types.ts`. The `generated/` folder is gitignored.
