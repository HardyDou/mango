/**
 * FilePreviewPanel 只能把真正适合展示的地址交给 img、iframe、video、audio。
 * 下载接口通常会携带 Content-Disposition: attachment，误用作 src 会触发浏览器下载。
 */
export function isFileDownloadEndpointUrl(value?: string): boolean {
  if (!value) return false;
  const normalizedPath = urlPathname(value).replace(/\/+$/, '');
  return normalizedPath === '/api/file/files/download'
    || normalizedPath === '/file/files/download';
}

export function isPreviewDisplayUrl(value?: string): boolean {
  if (!value) return false;
  return !isFileDownloadEndpointUrl(value);
}

function urlPathname(value: string): string {
  try {
    return new URL(value, window.location.origin).pathname;
  } catch {
    const [path] = value.split(/[?#]/);
    return path || '';
  }
}
