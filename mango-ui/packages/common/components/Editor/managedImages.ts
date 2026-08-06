import type { FileId, UploadResult } from '../../api/upload';

const managedTokenPrefix = 'mango-file:';
const managedIdPattern = /^[1-9]\d*$/;

export type ManagedAssetKind = 'image' | 'attachment';
export type ManagedAssetPreview = string | undefined | Pick<UploadResult, 'url' | 'previewUrl' | 'downloadUrl'>;

export interface ManagedHtmlResult {
  html: string;
  invalidAssetCount: number;
  /** @deprecated Use invalidAssetCount. */
  invalidImageCount: number;
}

export function managedImageToken(id: FileId | string) {
  return `${managedTokenPrefix}${String(id)}`;
}

export function normalizeManagedFileId(value?: string | null): FileId | undefined {
  const normalized = String(value || '').trim();
  return managedIdPattern.test(normalized) ? normalized : undefined;
}

export function managedFileIdFromImage(image: HTMLImageElement, previewIds: ReadonlyMap<string, FileId> = new Map()) {
  return managedFileIdFromElement(image, 'src', previewIds);
}

export function managedFileIdFromAttachment(
  attachment: HTMLAnchorElement,
  previewIds: ReadonlyMap<string, FileId> = new Map(),
) {
  return managedFileIdFromElement(attachment, 'href', previewIds);
}

export function collectManagedFileIds(source: string) {
  const root = parseHtml(source);
  const ids = new Set<FileId>();
  root.querySelectorAll('img, a').forEach((node) => {
    const id = managedFileIdFromNode(node);
    if (id) ids.add(id);
  });
  return [...ids];
}

export function serializeManagedHtml(
  source: string,
  previewIds: ReadonlyMap<string, FileId> = new Map(),
): ManagedHtmlResult {
  const root = parseHtml(source);
  let invalidAssetCount = 0;

  root.querySelectorAll('img, a').forEach((node) => {
    const kind: ManagedAssetKind = node.tagName === 'IMG' ? 'image' : 'attachment';
    const urlAttribute = kind === 'image' ? 'src' : 'href';
    if (!hasManagedMarker(node, urlAttribute, previewIds)) return;

    const id = managedFileIdFromElement(node, urlAttribute, previewIds);
    if (!id) {
      invalidAssetCount += 1;
      node.remove();
      return;
    }

    node.setAttribute(urlAttribute, managedImageToken(id));
    node.setAttribute('data-file-id', id);
    node.setAttribute('data-file-kind', kind);
    node.removeAttribute('data-managed-state');
    node.removeAttribute('data-managed-task');
    if (kind === 'image') node.removeAttribute('srcset');
    else setSafeAttachmentTarget(node as HTMLAnchorElement);
  });

  return {
    html: root.innerHTML,
    invalidAssetCount,
    invalidImageCount: invalidAssetCount,
  };
}

export function renderManagedHtml(
  source: string,
  previews: ReadonlyMap<FileId, ManagedAssetPreview>,
  previewIds: Map<string, FileId> = new Map(),
) {
  const root = parseHtml(source);
  root.querySelectorAll('img, a').forEach((node) => {
    const kind: ManagedAssetKind = node.tagName === 'IMG' ? 'image' : 'attachment';
    const urlAttribute = kind === 'image' ? 'src' : 'href';
    if (!hasManagedMarker(node, urlAttribute)) return;

    const id = managedFileIdFromElement(node, urlAttribute);
    if (!id) {
      node.remove();
      return;
    }

    const previewUrl = resolveManagedAssetUrl(previews.get(id), kind);
    node.setAttribute('data-file-id', id);
    node.setAttribute('data-file-kind', kind);
    node.setAttribute(urlAttribute, previewUrl || managedImageToken(id));
    node.setAttribute('data-managed-state', previewUrl ? 'ready' : 'failed');
    if (previewUrl) previewIds.set(previewUrl, id);

    if (kind === 'image') {
      node.removeAttribute('srcset');
      if (!previewUrl) node.setAttribute('alt', node.getAttribute('alt') || '图片暂不可预览');
    } else {
      setSafeAttachmentTarget(node as HTMLAnchorElement);
      if (!node.textContent?.trim()) node.textContent = '附件';
    }
  });
  return root.innerHTML;
}

export function managedImageHtml(id: FileId, previewUrl: string | undefined, alt = '') {
  const source = previewUrl || managedImageToken(id);
  const state = previewUrl ? 'ready' : 'failed';
  return `<img src="${escapeHtmlAttribute(source)}" data-file-id="${escapeHtmlAttribute(id)}" data-file-kind="image" data-managed-state="${state}" alt="${escapeHtmlAttribute(alt)}">`;
}

export function managedAttachmentHtml(id: FileId, previewUrl: string | undefined, fileName: string) {
  const source = previewUrl || managedImageToken(id);
  const state = previewUrl ? 'ready' : 'failed';
  const label = fileName || '附件';
  return `<a href="${escapeHtmlAttribute(source)}" data-file-id="${escapeHtmlAttribute(id)}" data-file-kind="attachment" data-managed-state="${state}" target="_blank" rel="noopener noreferrer">${escapeHtml(label)}</a>`;
}

export function parseHtml(source: string) {
  const root = document.createElement('div');
  root.innerHTML = source || '';
  return root;
}

function managedFileIdFromNode(node: Element) {
  if (node.tagName === 'IMG') return managedFileIdFromElement(node, 'src');
  if (node.tagName === 'A') return managedFileIdFromElement(node, 'href');
  return undefined;
}

function managedFileIdFromElement(
  element: Element,
  urlAttribute: 'src' | 'href',
  previewIds: ReadonlyMap<string, FileId> = new Map(),
) {
  const dataId = normalizeManagedFileId(element.getAttribute('data-file-id'));
  const source = element.getAttribute(urlAttribute) || '';
  const tokenId = source.startsWith(managedTokenPrefix)
    ? normalizeManagedFileId(source.slice(managedTokenPrefix.length))
    : undefined;
  if (dataId && tokenId && dataId !== tokenId) return undefined;
  return dataId || tokenId || previewIds.get(source);
}

function hasManagedMarker(
  element: Element,
  urlAttribute: 'src' | 'href',
  previewIds: ReadonlyMap<string, FileId> = new Map(),
) {
  const source = element.getAttribute(urlAttribute) || '';
  return element.hasAttribute('data-file-id') || source.startsWith(managedTokenPrefix) || previewIds.has(source);
}

function resolveManagedAssetUrl(preview: ManagedAssetPreview, kind: ManagedAssetKind) {
  if (!preview) return undefined;
  if (typeof preview === 'string') return preview;
  const url =
    kind === 'image' ? preview.previewUrl || preview.url : preview.previewUrl || preview.downloadUrl || preview.url;
  return url && !url.startsWith(managedTokenPrefix) ? url : undefined;
}

function setSafeAttachmentTarget(attachment: HTMLAnchorElement) {
  attachment.setAttribute('target', '_blank');
  attachment.setAttribute('rel', 'noopener noreferrer');
}

function escapeHtmlAttribute(value: string) {
  return escapeHtml(value).replaceAll('"', '&quot;');
}

function escapeHtml(value: string) {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
}
