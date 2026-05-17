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
    // First try sessionStorage (set by frontend during login)
    const token = sessionStorage.getItem('MANGO_TOKEN');
    if (token) {
      return token;
    }
    const cookieToken = readCookie('MANGO_TOKEN');
    if (cookieToken) {
      return cookieToken;
    }
    // Fallback: try reading from httpOnly Cookie (set by backend)
    // Note: httpOnly Cookies can't be read by JS, so this fallback won't work for them
    // But it works for non-httpOnly cookies if any exist
    return null;
  },

  setToken(token: string): void {
    // Also store in sessionStorage for frontend auth checks (httpOnly Cookie can't be read by JS)
    // The httpOnly Cookie is for backend authentication
    sessionStorage.setItem('MANGO_TOKEN', token);
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(token)}; path=/; SameSite=Lax`;
  },

  clearToken(): void {
    sessionStorage.removeItem('MANGO_TOKEN');
    // Clear httpOnly Cookie by setting expired
    document.cookie = 'MANGO_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  },

  clearSession(): void {
    this.clearToken();
    this.clear();
  },
};

function readCookie(name: string): string | null {
  const cookies = document.cookie ? document.cookie.split(';') : [];
  for (const cookie of cookies) {
    const trimmed = cookie.trim();
    if (!trimmed.startsWith(`${name}=`)) {
      continue;
    }
    return decodeURIComponent(trimmed.slice(name.length + 1));
  }
  return null;
}

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
