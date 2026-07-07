export function isAccessDeniedError(error: unknown): boolean {
  const record = isRecord(error) ? error : {};
  const response = isRecord(record.response) ? record.response : {};
  const data = isRecord(response.data) ? response.data : {};
  const status = toStatus(response.status) ?? toStatus(record.status);
  const code = String(record.code ?? data.code ?? data.errorCode ?? '').toUpperCase();

  return status === 401
    || status === 403
    || code === '401'
    || code === '403'
    || code === 'FORBIDDEN'
    || code === 'UNAUTHORIZED'
    || code.includes('NO_PERMISSION');
}

function toStatus(value: unknown): number | undefined {
  const status = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(status) ? status : undefined;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
