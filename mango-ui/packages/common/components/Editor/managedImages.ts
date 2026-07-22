import type { FileId } from '../../api/upload';

const managedTokenPrefix = 'mango-file:';
const managedIdPattern = /^[1-9]\d*$/;

export interface ManagedHtmlResult {
  html: string;
  invalidImageCount: number;
}

export function managedImageToken(id: FileId | string) {
  return `${managedTokenPrefix}${String(id)}`;
}

export function normalizeManagedFileId(value?: string | null): FileId | undefined {
  const normalized = String(value || '').trim();
  return managedIdPattern.test(normalized) ? normalized : undefined;
}

export function managedFileIdFromImage(
  image: HTMLImageElement,
  previewIds: ReadonlyMap<string, FileId> = new Map(),
) {
  const dataId = normalizeManagedFileId(image.getAttribute('data-file-id'));
  const source = image.getAttribute('src') || '';
  const tokenId = source.startsWith(managedTokenPrefix)
    ? normalizeManagedFileId(source.slice(managedTokenPrefix.length))
    : undefined;
  if (dataId && tokenId && dataId !== tokenId) return undefined;
  return dataId || tokenId || previewIds.get(source);
}

export function collectManagedFileIds(source: string) {
  const root = parseHtml(source);
  const ids = new Set<FileId>();
  root.querySelectorAll('img').forEach((node) => {
    const id = managedFileIdFromImage(node as HTMLImageElement);
    if (id) ids.add(id);
  });
  return [...ids];
}

export function serializeManagedHtml(
  source: string,
  previewIds: ReadonlyMap<string, FileId> = new Map(),
): ManagedHtmlResult {
  const root = parseHtml(source);
  let invalidImageCount = 0;
  root.querySelectorAll('img').forEach((node) => {
    const image = node as HTMLImageElement;
    const id = managedFileIdFromImage(image, previewIds);
    if (!id) {
      invalidImageCount += 1;
      image.remove();
      return;
    }
    image.setAttribute('src', managedImageToken(id));
    image.setAttribute('data-file-id', id);
    image.removeAttribute('srcset');
    image.removeAttribute('data-managed-state');
    image.removeAttribute('data-managed-task');
  });
  return { html: root.innerHTML, invalidImageCount };
}

export function renderManagedHtml(
  source: string,
  previews: ReadonlyMap<FileId, string | undefined>,
  previewIds: Map<string, FileId>,
) {
  const root = parseHtml(source);
  root.querySelectorAll('img').forEach((node) => {
    const image = node as HTMLImageElement;
    const id = managedFileIdFromImage(image);
    if (!id) {
      image.remove();
      return;
    }
    const previewUrl = previews.get(id);
    image.setAttribute('data-file-id', id);
    image.removeAttribute('srcset');
    if (previewUrl) {
      image.setAttribute('src', previewUrl);
      image.setAttribute('data-managed-state', 'ready');
      previewIds.set(previewUrl, id);
      return;
    }
    image.setAttribute('src', managedImageToken(id));
    image.setAttribute('data-managed-state', 'failed');
    image.setAttribute('alt', image.getAttribute('alt') || '暂不可预览');
  });
  return root.innerHTML;
}

export function managedImageHtml(id: FileId, previewUrl: string | undefined, alt = '') {
  const source = previewUrl || managedImageToken(id);
  const state = previewUrl ? 'ready' : 'failed';
  return `<img src="${escapeHtmlAttribute(source)}" data-file-id="${escapeHtmlAttribute(id)}" data-managed-state="${state}" alt="${escapeHtmlAttribute(alt)}">`;
}

export function parseHtml(source: string) {
  const root = document.createElement('div');
  root.innerHTML = source || '';
  return root;
}

function escapeHtmlAttribute(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}
