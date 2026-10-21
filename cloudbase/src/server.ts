import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { health } from '../functions/api/health.ts';

const port = Number(process.env.PORT ?? 3000);
if (!Number.isInteger(port) || port < 0 || port > 65535) {
  throw new Error('PORT must be an integer between 0 and 65535');
}

const server = createServer((request, response) => {
  const requestId = randomUUID();
  response.setHeader('Content-Type', 'application/json; charset=utf-8');
  response.setHeader('X-Request-Id', requestId);
  response.setHeader('Cache-Control', 'no-store');
  const path = new URL(request.url ?? '/', 'http://localhost').pathname;

  if (path === '/health' && request.method === 'GET') {
    response.writeHead(200);
    response.end(JSON.stringify(health(requestId)));
    return;
  }

  response.writeHead(404);
  response.end(JSON.stringify({
    requestId,
    error: { code: 'NOT_FOUND', message: 'Route not found' },
  }));
});

server.listen(port, '127.0.0.1', () => {
  const address = server.address();
  if (address && typeof address !== 'string') {
    const baseUrl = `http://127.0.0.1:${address.port}`;
    console.log(`Local API: ${baseUrl}/health`);
    if (process.send) process.send({ type: 'ready', baseUrl });
  }
});

process.on('SIGTERM', () => server.close());
process.on('SIGINT', () => server.close());
