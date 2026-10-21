export function health(requestId: string) {
  return {
    requestId,
    data: {
      status: 'ok' as const,
      service: 'geotreasure-api' as const,
      version: '0.1.0',
      timestamp: new Date().toISOString(),
    },
  };
}
