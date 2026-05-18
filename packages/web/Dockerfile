FROM node:25-alpine AS build
RUN npm install -g pnpm@11.1.2 --quiet
WORKDIR /app
# Install deps in an isolated layer so source edits don't bust the cache.
COPY pnpm-workspace.yaml pnpm-lock.yaml package.json ./
COPY packages/api-contract/package.json ./packages/api-contract/
COPY packages/web/package.json ./packages/web/
RUN pnpm install --frozen-lockfile
# Source + generate types, then build.
COPY packages/ ./packages/
# BACKEND_URL is consumed by next.config.ts rewrites() which is evaluated at
# build time in production. The arg defaults to the compose service hostname
# so the image works out-of-the-box with `docker compose up`.
ARG BACKEND_URL=http://backend:8080
ENV NEXT_PUBLIC_API_MOCKING=disabled \
    BACKEND_URL=$BACKEND_URL
RUN pnpm --filter @expense-challenge/api-contract generate && \
    pnpm --filter @expense-challenge/web build

FROM node:25-alpine AS runtime
ENV NODE_ENV=production \
    NEXT_PUBLIC_API_MOCKING=disabled
WORKDIR /app
# Copy workspace root manifests and the pnpm virtual store so symlinks resolve.
COPY --from=build /app/pnpm-workspace.yaml /app/package.json /app/pnpm-lock.yaml ./
COPY --from=build /app/node_modules ./node_modules
# Copy the web package: built output, static assets, and its own node_modules
# (which are symlinks into the root node_modules/.pnpm virtual store above).
COPY --from=build /app/packages/web/package.json ./packages/web/
COPY --from=build /app/packages/web/.next ./packages/web/.next
COPY --from=build /app/packages/web/public ./packages/web/public
COPY --from=build /app/packages/web/node_modules ./packages/web/node_modules
EXPOSE 3000
WORKDIR /app/packages/web
# Invoke next directly to bypass pnpm's workspace dependency resolution
# (api-contract is a build-time dep, not needed at runtime).
CMD ["node_modules/.bin/next", "start"]
