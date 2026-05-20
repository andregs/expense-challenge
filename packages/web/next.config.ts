import type { NextConfig } from 'next';

/**
 * Frontend and backend are deployed at the same origin in production (the
 * web container reverse-proxies `/api/*` to the Spring service). To keep
 * the same URL shape during local development, Next's built-in rewrite
 * rule forwards `/api/*` to whichever backend the developer set in
 * `BACKEND_URL`, defaulting to the value used by `docker compose`. The
 * client therefore always calls relative paths, sidestepping CORS.
 */
const nextConfig: NextConfig = {
  // eslint-disable-next-line @typescript-eslint/require-await -- Next's config contract requires an async function here
  async rewrites() {
    const backend = process.env.BACKEND_URL ?? 'http://localhost:8080';
    return [
      {
        source: '/api/:path*',
        destination: `${backend}/api/:path*`,
      },
      {
        source: '/actuator/:path*',
        destination: `${backend}/actuator/:path*`,
      },
    ];
  },
};

export default nextConfig;
