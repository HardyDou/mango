declare module '@mango/common' {
  export interface RequestConfig {
    params?: object;
    headers?: Record<string, string>;
    [key: string]: unknown;
  }

  export function get<T = unknown>(url: string, config?: RequestConfig): Promise<T>;
  export function post<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>;
  export function put<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>;
  export function del<T = unknown>(url: string, config?: RequestConfig): Promise<T>;
}
