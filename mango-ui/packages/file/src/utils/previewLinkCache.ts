import type { FileApi, FilePreviewLink } from '../api/file';

/**
 * 后端预览链接默认缓存时长。未返回 expireSeconds 时按该值兜底。
 */
const DEFAULT_TTL_MS = 60 * 1000;
/**
 * 提前失效余量，避免使用即将过期的令牌。
 */
const EXPIRY_MARGIN_MS = 30 * 1000;

interface PreviewLinkCacheEntry {
  expiresAt: number;
  value?: FilePreviewLink;
  pending?: Promise<FilePreviewLink>;
}

const cache = new Map<string, PreviewLinkCacheEntry>();

/**
 * 按 fileId 复用 preview-link 结果：同一 fileId 重复打开预览只请求一次后端，
 * 并发调用共享同一个在途请求。预览令牌是短期的，缓存按 expireSeconds 提前失效。
 */
export function cachedPreviewLink(api: FileApi, fileId: string): Promise<FilePreviewLink> {
  const key = String(fileId);
  const cached = cache.get(key);
  if (cached?.pending) return cached.pending;
  if (cached?.value && cached.expiresAt > Date.now()) return Promise.resolve(cached.value);
  const pending = api
    .previewLink(key)
    .then((link) => {
      cache.set(key, { expiresAt: Date.now() + ttlMs(link), value: link });
      return link;
    })
    .catch((error: unknown) => {
      cache.delete(key);
      throw error;
    });
  cache.set(key, { expiresAt: 0, pending });
  return pending;
}

/** 清空缓存，供测试与预览链接失效后重新获取使用。 */
export function clearPreviewLinkCache(): void {
  cache.clear();
}

function ttlMs(link: FilePreviewLink): number {
  const seconds = Number(link.expireSeconds ?? 0);
  if (!Number.isFinite(seconds) || seconds <= 0) return DEFAULT_TTL_MS;
  return Math.max(seconds * 1000 - EXPIRY_MARGIN_MS, 1000);
}
