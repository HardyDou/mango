import type { HttpClient, HttpQuery, HttpQueryValue, HttpRequest } from '@mango/api-schema';

/** 公开托管页面允许访问的文件能力，全部以表单为边界。 */
const PUBLIC_FILE_PATHS = ['/file/files', '/file/files/detail', '/file/files/preview', '/file/files/download'];

/**
 * 托管表单是匿名页面，文件模块的上传与预览接口要求登录。
 * 这里把平台统一的上传/查询请求改写为当前表单的公开文件入口，
 * 上传控件与富文本因此都能继续使用平台既有实现，文件仍然保存到 mango-file。
 */
export function createInformationPublicFileClient(delegate: HttpClient, formCode: string | (() => string)): HttpClient {
  const resolveCode = () => (typeof formCode === 'function' ? formCode() : formCode);
  const resolvePrefix = () => `/information/open/forms/${encodeURIComponent(resolveCode())}/files`;

  /** 票据按表单缓存，剩余不足 30 秒时重新领取。 */
  let cached: { code: string; ticket: string; expiresAt: number } | undefined;
  async function ensureTicket(): Promise<string> {
    const code = resolveCode();
    const now = Date.now();
    if (cached && cached.code === code && cached.expiresAt - now > 30_000) {
      return cached.ticket;
    }
    const issued = await delegate.request<{ ticket: string; expiresIn: number }, undefined>({
      method: 'POST',
      url: `/information/open/forms/${encodeURIComponent(code)}/upload-ticket`,
    });
    cached = { code, ticket: issued.ticket, expiresAt: now + issued.expiresIn * 1000 };
    return issued.ticket;
  }

  return {
    async request<TResponse = unknown, TBody = unknown>(request: HttpRequest<TBody>): Promise<TResponse> {
      const url = String(request.url || '');
      const match = PUBLIC_FILE_PATHS.find((path) => url === path || url.startsWith(`${path}?`));
      if (!match) {
        return delegate.request<TResponse, TBody>(request);
      }
      const isUpload = match === '/file/files';
      const suffix = isUpload ? '' : match.replace('/file/files', '');
      const headers = isUpload
        ? { ...(request.headers || {}), 'X-Mango-Upload-Ticket': await ensureTicket() }
        : request.headers;
      return delegate.request<TResponse, TBody>({
        ...request,
        url: `${resolvePrefix()}${suffix}`,
        headers,
        query: normalizeQuery(request.query),
      });
    },
  };
}

/** 文件接口的 query 形如 { id }；统一收窄为纯量，保证类型契约不变。 */
function normalizeQuery(query: HttpQuery | undefined): Record<string, HttpQueryValue> | undefined {
  if (!query) {
    return undefined;
  }
  const result: Record<string, HttpQueryValue> = {};
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return;
    }
    result[key] = Array.isArray(value) ? value.map(String) : typeof value === 'object' ? JSON.stringify(value) : value;
  });
  return Object.keys(result).length ? result : undefined;
}
