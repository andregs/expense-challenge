'use client';

import { useEffect, useRef, useState, type ReactNode } from 'react';

const isEnabled = process.env.NEXT_PUBLIC_API_MOCKING === 'enabled';

/**
 * Mounts MSW's service worker on the client when
 * `NEXT_PUBLIC_API_MOCKING=enabled`. Children render once the worker is
 * ready so initial requests are intercepted; when mocking is off the
 * provider is a pass-through.
 *
 * Only `/api/*` requests without a matching handler emit a console
 * warning. Everything else (Next.js HMR WebSockets, RSC fetches,
 * static assets, page navigations) is silently bypassed so the dev
 * console stays clean while API drift still surfaces immediately.
 */
export function MockingProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(!isEnabled);
  const cancelledRef = useRef(false);

  useEffect(() => {
    if (!isEnabled) return;
    cancelledRef.current = false;
    void (async () => {
      const { worker } = await import('./browser');
      await worker.start({
        onUnhandledRequest(request, print) {
          if (new URL(request.url).pathname.startsWith('/api/')) print.warning();
        },
      });
      if (!cancelledRef.current) setReady(true);
    })();
    return () => {
      cancelledRef.current = true;
    };
  }, []);

  if (!ready) return null;
  return <>{children}</>;
}
