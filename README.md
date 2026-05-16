# Expense Tracker

Full-stack reference application for recording USD purchase transactions and retrieving them converted into any currency reported by the [US Treasury Reporting Rates of Exchange](https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange) API.

## Stack

- **Backend** — Spring Boot 4 (Spring Framework 7) on Java 25, Spring Data JDBC, Flyway, Spring Kafka, Spring Data Redis.
- **Frontend** — Next.js 16 (App Router) with React 19 and TypeScript 6.
- **Storage** — PostgreSQL with `NUMERIC(19,4)` precision and a transactional outbox.
- **Messaging** — Apache Kafka topic `purchase.transactions.created` driven by an outbox relay.
- **Cache** — Redis (cache-aside) for FX rates with long TTL since historical rates are immutable.
- **Tests** — JUnit 5 + Testcontainers (Postgres, Kafka, Redis), WireMock for Treasury, Vitest + React Testing Library, Playwright.
- **Tooling** — pnpm workspaces, Turborepo, Gradle 9 (Kotlin DSL), ESLint 9 flat config, Prettier, asdf via `.tool-versions`.

## Repository layout

```
expense-challenge/
├── backend/                  # Spring Boot service (Gradle)
├── packages/
│   ├── api-contract/         # OpenAPI 3.1 spec + generated TS types
│   ├── design-system/        # Shared React components and SCSS tokens
│   └── web/                  # Next.js App Router frontend
├── docker-compose.yml        # postgres + kafka + redis + backend + web
├── turbo.json
└── pnpm-workspace.yaml
```

## Prerequisites

- [`asdf`](https://asdf-vm.com/) — versions are pinned in [`.tool-versions`](.tool-versions).
- Docker (for Testcontainers and `docker compose up`).

Install runtimes via asdf:

```bash
asdf plugin add nodejs && asdf plugin add pnpm && asdf plugin add java
asdf install
```

## Quickstart

### Full stack via Docker Compose

```bash
docker compose up --build
# Backend on http://localhost:8080
# Frontend on http://localhost:3000
```

### Backend only

```bash
cd backend
./gradlew bootRun
```

### Frontend only (against MSW mocks — no backend needed)

```bash
pnpm install
pnpm --filter @expense-challenge/web dev
```

## API

| Method | Path                              | Purpose                                   |
| ------ | --------------------------------- | ----------------------------------------- |
| POST   | `/api/v1/transactions`            | Store a purchase transaction              |
| GET    | `/api/v1/transactions`            | Paginated ledger                          |
| GET    | `/api/v1/transactions/{id}`       | Retrieve transaction (`?currency=` to convert) |

Conversion rule: the rate used is the most recent Treasury record dated on or before the purchase date, within the prior 6 months. If no such rate exists, the response is `422 Unprocessable Entity`.

All monetary values are handled as `BigDecimal` end-to-end and stored as `NUMERIC(19,4)`. The converted amount is rounded `HALF_UP` to two decimal places at the response boundary.

## Testing

```bash
# Backend
cd backend && ./gradlew test

# Frontend unit + component
pnpm test

# Playwright end-to-end
pnpm test:e2e
```

## License

MIT — see [LICENSE](LICENSE).
