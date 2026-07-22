const DEFAULT_HEALTH_POLL_INTERVAL_MS = 500;
const MINIMUM_HEALTH_POLL_INTERVAL_MS = 100;

/**
 * Resolves the interval used while waiting for a development app health endpoint.
 */
export function resolveHealthPollIntervalMs(configuredValue) {
  const parsed = Number(configuredValue);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return DEFAULT_HEALTH_POLL_INTERVAL_MS;
  }
  return Math.max(MINIMUM_HEALTH_POLL_INTERVAL_MS, parsed);
}
