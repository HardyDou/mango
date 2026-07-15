export interface LogoutFlowDependencies {
  revokeSession: () => Promise<unknown>;
  clearSession: () => void;
  clearNavigation: () => void;
  clearUserInfo: () => void;
  redirectToLogin: () => Promise<unknown>;
}

/**
 * Revoke the server session before clearing the local shell state.
 *
 * Keeping this order prevents an HttpOnly authentication cookie from remaining
 * valid after the UI has already presented the user as logged out.
 */
export async function completeLogout(dependencies: LogoutFlowDependencies): Promise<void> {
  await dependencies.revokeSession();
  dependencies.clearSession();
  await dependencies.redirectToLogin();
  dependencies.clearNavigation();
  dependencies.clearUserInfo();
}
