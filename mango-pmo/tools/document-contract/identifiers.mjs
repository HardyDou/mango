function isUpper(char) {
  return char >= 'A' && char <= 'Z';
}

function isDigit(char) {
  return char >= '0' && char <= '9';
}

function isIdentifierBoundary(char) {
  return !char || (!isUpper(char) && !isDigit(char) && char !== '-' && char !== '_');
}

export function scanIdentifiers(value) {
  const text = String(value ?? '');
  const tokens = [];
  for (let index = 0; index < text.length; index += 1) {
    if (!isUpper(text[index]) || !isIdentifierBoundary(text[index - 1])) continue;
    const start = index;
    while (isUpper(text[index])) index += 1;
    const prefix = text.slice(start, index);
    if (prefix.length < 2 || prefix.length > 8 || text[index] !== '-') {
      index = start;
      continue;
    }
    index += 1;
    const digitStart = index;
    while (isDigit(text[index])) index += 1;
    const digits = text.slice(digitStart, index);
    if (digits.length !== 3 || !isIdentifierBoundary(text[index])) {
      index = start;
      continue;
    }
    tokens.push({ id: `${prefix}-${digits}`, prefix, index: start });
    index -= 1;
  }
  return tokens;
}

export function isExactIdentifier(value, prefix) {
  const text = String(value ?? '').trim();
  const tokens = scanIdentifiers(text);
  return tokens.length === 1 && tokens[0].id === text && tokens[0].prefix === prefix;
}

export function containsPlaceholder(value) {
  const text = String(value ?? '');
  return text.includes('{{') || text.includes('}}');
}

export function isSha256(value) {
  const text = String(value ?? '');
  if (text.length !== 64) return false;
  for (const char of text) {
    const lower = char.toLowerCase();
    if (!isDigit(char) && !(lower >= 'a' && lower <= 'f')) return false;
  }
  return true;
}

export function isSemver(value) {
  const parts = String(value ?? '').split('.');
  if (parts.length !== 3) return false;
  return parts.every((part) => part.length > 0 && [...part].every(isDigit));
}

export function isDocumentId(value, prefix) {
  const text = String(value ?? '');
  if (!text.startsWith(`${prefix}-`)) return false;
  const segments = text.split('-');
  if (segments.length < 2 || segments.some((segment) => !segment)) return false;
  return segments.every((segment) => [...segment].every((char) => isUpper(char) || isDigit(char)));
}
