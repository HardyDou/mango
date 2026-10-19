import type { HttpClient, HttpQuery, HttpQueryValue, HttpRequest } from '@mango/api-schema';

/** 公开托管页面允许访问的文件能力，全部以表单为边界。 */
const PUBLIC_FILE_PATHS = ['/file/files', '/file/files/detail', '/file/files/preview', '/file/files/download'];

/**
 * 托管表单是匿名页面，文件模块的上传与预览接口要求登录。
 * 这里把平台统一的上传/查询请求改写为当前表单的公开文件入口，
 * 上传控件与富文本因此都能继续使用平台既有实现，文件仍然保存到 mango-file。
 */
export function createInformationPublicFileClient(delegate: HttpClient, formCode: string | (() => string)): HttpClient {
  const resolvePrefix = () => {
    const code = typeof formCode === 'function' ? formCode() : formCode;
    return `/information/open/forms/${encodeURIComponent(code)}/files`;
  };
  return {
    request<TResponse = unknown, TBody = unknown>(request: HttpRequest<TBody>): Promise<TResponse> {
      const url = String(request.url || '');
      const match = PUBLIC_FILE_PATHS.find((path) => url === path || url.startsWith(`${path}?`));
      if (!match) {
        return delegate.request<TResponse, TBody>(request);
      }
      const suffix = match === '/file/files' ? '' : match.replace('/file/files', '');
      return delegate.request<TResponse, TBody>({
        ...request,
        url: `${resolvePrefix()}${suffix}`,
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
