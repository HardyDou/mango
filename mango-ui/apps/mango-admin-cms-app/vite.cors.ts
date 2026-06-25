export function createAllowedOrigins(allowedOriginsValue?: string) {
  return (allowedOriginsValue || 'http://localhost:5176,http://127.0.0.1:5176,http://a.mango.io:5176,http://a.mango.io:4176')
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);
}

export function createPreviewCorsHeaders(allowedOrigins: string[]) {
  return {
    'Access-Control-Allow-Origin': allowedOrigins[0] || 'http://a.mango.io:4176',
    'Access-Control-Allow-Credentials': 'true',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With',
    Vary: 'Origin',
  };
}
