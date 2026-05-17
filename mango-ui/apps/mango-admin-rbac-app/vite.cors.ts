export function createAllowedOrigins(allowedOriginsValue?: string) {
  return (allowedOriginsValue || 'http://localhost:5176,http://127.0.0.1:5176,http://a.mango.io:5176')
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);
}
