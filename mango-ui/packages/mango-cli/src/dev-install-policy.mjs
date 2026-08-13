/**
 * Decides whether a development app should execute its configured install command.
 * Spring Boot apps always run from their Maven reactor and never install workspace artifacts.
 */
export function shouldRunDevInstall(app) {
  if (!app?.install) {
    return false;
  }
  if (app.type !== 'spring-boot-maven') {
    return true;
  }
  return false;
}
