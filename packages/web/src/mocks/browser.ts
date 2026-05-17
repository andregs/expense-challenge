import { setupWorker } from 'msw/browser';
import { ws } from 'msw';
import { handlers } from './handlers';

// Passthrough all WebSocket connections to the real server so MSW does not
// emit "unhandled connection" warnings for Next.js HMR sockets and other
// framework-internal WebSockets that are none of the mock layer's business.
const passthroughWs = ws.link('ws://*');
const wsPassthrough = passthroughWs.addEventListener('connection', ({ server }) => {
  server.connect();
});

export const worker = setupWorker(...handlers, wsPassthrough);
