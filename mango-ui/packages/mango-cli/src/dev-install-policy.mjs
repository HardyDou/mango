const FALSE_VALUES = new Set(['0', 'false', 'no', 'off']);

/**
 * Decides whether a development app should execute its configured install command.
 * Backend installs remain enabled unless the workspace explicitly opts out.
 */
export function shouldRunDevInstall(app, env = {}) {
  if (!app?.install) {
    return false;
  }
  if (app.type !== 'spring-boot-maven') {
    return true;
  }
  const configured = String(env.MANGO_BACKEND_AUTO_INSTALL ?? 'true').trim().toLowerCase();
  return !FALSE_VALUES.has(configured);
}
