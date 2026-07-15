import { describe, expect, it, vi } from 'vitest';
import { completeLogout } from '../runtime/logoutFlow';

describe('completeLogout', () => {
  it('revokes the server session before clearing local state and redirecting', async () => {
    const calls: string[] = [];

    await completeLogout({
      revokeSession: vi.fn(async () => {
        calls.push('revoke');
      }),
      clearSession: vi.fn(() => {
        calls.push('clear-session');
      }),
      clearNavigation: vi.fn(() => {
        calls.push('clear-navigation');
      }),
      clearUserInfo: vi.fn(() => {
        calls.push('clear-user');
      }),
      redirectToLogin: vi.fn(async () => {
        calls.push('redirect');
      }),
    });

    expect(calls).toEqual(['revoke', 'clear-session', 'redirect', 'clear-navigation', 'clear-user']);
  });

  it('keeps the authenticated local state when server revocation fails', async () => {
    const error = new Error('logout unavailable');
    const clearSession = vi.fn();
    const clearNavigation = vi.fn();
    const clearUserInfo = vi.fn();
    const redirectToLogin = vi.fn(async () => undefined);

    await expect(completeLogout({
      revokeSession: vi.fn(async () => {
        throw error;
      }),
      clearSession,
      clearNavigation,
      clearUserInfo,
      redirectToLogin,
    })).rejects.toBe(error);

    expect(clearSession).not.toHaveBeenCalled();
    expect(clearNavigation).not.toHaveBeenCalled();
    expect(clearUserInfo).not.toHaveBeenCalled();
    expect(redirectToLogin).not.toHaveBeenCalled();
  });
});
