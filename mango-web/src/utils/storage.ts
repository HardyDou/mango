/**
 * Session Storage 封装
 */
export const Session = {
  get(key: string): any {
    const value = sessionStorage.getItem(key);
    if (value) {
      try {
        return JSON.parse(value);
      } catch {
        return value;
      }
    }
    return null;
  },

  set(key: string, value: any): void {
    if (typeof value === 'string') {
      sessionStorage.setItem(key, value);
    } else {
      sessionStorage.setItem(key, JSON.stringify(value));
    }
  },

  remove(key: string): void {
    sessionStorage.removeItem(key);
  },

  clear(): void {
    sessionStorage.clear();
  },

  getToken(): string | null {
    // Use sessionStorage for tokens - cleared when tab closes.
    // TODO (P1): For production, use httpOnly cookies (requires backend support).
    // XSS can still read sessionStorage, but data doesn't persist across sessions.
    return sessionStorage.getItem('MANGO_TOKEN');
  },

  setToken(token: string): void {
    // SECURITY: sessionStorage is still XSS-vulnerable but better than localStorage.
    // The real fix requires httpOnly cookies (backend change needed).
    sessionStorage.setItem('MANGO_TOKEN', token);
  },

  clearToken(): void {
    sessionStorage.removeItem('MANGO_TOKEN');
  },

  clearSession(): void {
    this.clearToken();
    this.clear();
  },
};

/**
 * Local Storage 封装
 */
export const Local = {
  get(key: string): any {
    const value = localStorage.getItem(key);
    if (value) {
      try {
        return JSON.parse(value);
      } catch {
        return value;
      }
    }
    return null;
  },

  set(key: string, value: any): void {
    if (typeof value === 'string') {
      localStorage.setItem(key, value);
    } else {
      localStorage.setItem(key, JSON.stringify(value));
    }
  },

  remove(key: string): void {
    localStorage.removeItem(key);
  },

  clear(): void {
    localStorage.clear();
  },
};
