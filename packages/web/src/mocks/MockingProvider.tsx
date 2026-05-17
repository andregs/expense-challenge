'use client';

import { useEffect, useRef, useState, type ReactNode } from 'react';

const isEnabled = process.env.NEXT_PUBLIC_API_MOCKING === 'enabled';

/**
 * Mounts MSW's service worker on the client when
 * `NEXT_PUBLIC_API_MOCKING=enabled`. Children render once the worker is
 * ready so initial requests are intercepted; when mocking is off the
 * provider is a pass-through.
 *
 * Unhandled requests use MSW's default `warn` behaviour — drift between
 * the OpenAPI spec and handler coverage surfaces in the browser console
 * rather than silently leaking out.
 */
export function MockingProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(!isEnabled);
  const cancelledRef = useRef(false);

  useEffect(() => {
    if (!isEnabled) return;
    cancelledRef.current = false;
    void (async () => {
      const { worker } = await import('./browser');
      await worker.start();
      if (!cancelledRef.current) setReady(true);
    })();
    return () => {
      cancelledRef.current = true;
    };
  }, []);

  if (!ready) return null;
  return <>{children}</>;
}
