const LOOPBACK_HOSTS = new Set(['127.0.0.1', 'localhost', '::1', '[::1]']);
const WILDCARD_HOSTS = new Set(['0.0.0.0', '::', '[::]']);
const DEFAULT_DEV_WORKSPACE_ENV_FILE = '.mango/dev-workspace.env';

/**
 * True when the listening host only accepts connections originated on this machine.
 */
export function isLoopbackHost(host) {
  return LOOPBACK_HOSTS.has(
    String(host ?? '')
      .trim()
      .toLowerCase(),
  );
}

/**
 * True when the listening host accepts connections from any network interface.
 */
export function isWildcardHost(host) {
  return WILDCARD_HOSTS.has(
    String(host ?? '')
      .trim()
      .toLowerCase(),
  );
}

/**
 * Builds the one-line listening notice printed right after a development app starts.
 *
 * <p>The notice exists because the generated workspace env pins the frontend to loopback while
 * Spring Boot binds every interface by default: without it, a workspace that is reachable from a
 * container or another machine looks identical to one that is not, and the only way to find out is
 * to read {@code src/index.mjs}.
 */
export function describeDevListenAddress(app, options = {}) {
  const port = Number(app?.port);
  if (!Number.isFinite(port) || port <= 0) {
    return '';
  }
  const host = String(app.host ?? '').trim() || '127.0.0.1';
  const url = app.url || `http://${host}:${port}`;
  const envFile = options.envFile || DEFAULT_DEV_WORKSPACE_ENV_FILE;
  const hostEnv = app.hostEnv || '';
  if (!hostEnv) {
    return url;
  }
  if (isWildcardHost(host)) {
    return `${url} (all interfaces)`;
  }
  if (isLoopbackHost(host)) {
    return (
      `${url} (loopback only; set ${hostEnv}=0.0.0.0 in ${envFile} ` +
      'and restart this app to expose it to other machines)'
    );
  }
  return `${url} (host ${host}; reachable through the address other machines can resolve)`;
}
