const ALLOWED_TAGS = new Set(['A', 'B', 'BR', 'CODE', 'EM', 'I', 'LI', 'OL', 'P', 'S', 'SPAN', 'STRONG', 'U', 'UL']);

const DROP_WITH_CONTENT = new Set(['IFRAME', 'MATH', 'OBJECT', 'SCRIPT', 'STYLE', 'SVG', 'TEMPLATE']);

const SAFE_LINK_PROTOCOLS = new Set(['http:', 'https:', 'mailto:', 'tel:']);

export function sanitizeNoticeHtml(value: unknown): string {
  const source = value == null || value === '' ? '-' : String(value);
  if (typeof DOMParser === 'undefined') {
    return escapeNoticeHtml(source);
  }

  const document = new DOMParser().parseFromString(source, 'text/html');
  const elements = Array.from(document.body.querySelectorAll('*')).reverse();
  elements.forEach((element) => {
    if (DROP_WITH_CONTENT.has(element.tagName)) {
      element.remove();
      return;
    }
    if (!ALLOWED_TAGS.has(element.tagName)) {
      element.replaceWith(...Array.from(element.childNodes));
      return;
    }
    sanitizeAttributes(element);
  });
  return document.body.innerHTML || '-';
}

export function noticePlainText(value: unknown): string {
  const sanitized = sanitizeNoticeHtml(value);
  if (typeof DOMParser === 'undefined') {
    return String(value == null || value === '' ? '-' : value)
      .replace(/<[^>]*>/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }
  const document = new DOMParser().parseFromString(sanitized, 'text/html');
  return (document.body.textContent || '-').replace(/\s+/g, ' ').trim() || '-';
}

function sanitizeAttributes(element: Element) {
  const href = element.tagName === 'A' ? safeHref(element.getAttribute('href')) : undefined;
  const title = element.tagName === 'A' ? element.getAttribute('title') : undefined;
  const openInNewWindow = element.tagName === 'A' && element.getAttribute('target') === '_blank';
  Array.from(element.attributes).forEach((attribute) => element.removeAttribute(attribute.name));
  if (href) {
    element.setAttribute('href', href);
    if (title) element.setAttribute('title', title);
    if (openInNewWindow) {
      element.setAttribute('target', '_blank');
      element.setAttribute('rel', 'noopener noreferrer');
    }
  }
}

function safeHref(value: string | null): string | undefined {
  const href = value?.trim();
  if (!href) return undefined;
  if ((href.startsWith('/') && !href.startsWith('//')) || href.startsWith('#')) return href;
  try {
    const url = new URL(href);
    return SAFE_LINK_PROTOCOLS.has(url.protocol) ? href : undefined;
  } catch {
    return undefined;
  }
}

function escapeNoticeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
