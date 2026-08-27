export interface DateRangeParams {
  startTime?: string;
  endTime?: string;
}

export interface BackendDateRangeOptions {
  startKey?: string;
  endKey?: string;
}

/**
 * Expands date-only range bounds while preserving complete date-time values.
 */
export function toBackendDateRangeParams<T extends object>(
  params?: T,
  options: BackendDateRangeOptions = {},
): T | undefined {
  if (!params) return params;
  const startKey = options.startKey || 'startTime';
  const endKey = options.endKey || 'endTime';
  const values = params as Record<string, unknown>;
  const normalized = { ...values };
  if (startKey in values) {
    normalized[startKey] = toBackendDateTime(values[startKey], 'start');
  }
  if (endKey in values) {
    normalized[endKey] = toBackendDateTime(values[endKey], 'end');
  }
  return normalized as T;
}

function toBackendDateTime(value: unknown, boundary: 'start' | 'end'): unknown {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  return `${value} ${boundary === 'start' ? '00:00:00' : '23:59:59'}`;
}
