import { closeSync, openSync, readSync, statSync } from 'node:fs';

export const DEFAULT_LOG_READ_LIMIT_BYTES = 1024 * 1024;

export function getLogFileSize(path) {
  try {
    const stats = statSync(path);
    return stats.isFile() ? stats.size : 0;
  } catch (error) {
    if (error?.code === 'ENOENT') {
      return 0;
    }
    throw error;
  }
}

export function readLogSuffixSince(path, offset, maxBytes = DEFAULT_LOG_READ_LIMIT_BYTES) {
  const size = getLogFileSize(path);
  const normalizedOffset = Math.max(0, Number.isFinite(offset) ? Math.floor(offset) : 0);
  if (size <= normalizedOffset || maxBytes <= 0) {
    return '';
  }
  const start = Math.max(normalizedOffset, size - Math.floor(maxBytes));
  return readLogRange(path, start, size - start);
}

export function tailLogFile(path, lineCount, maxBytes = DEFAULT_LOG_READ_LIMIT_BYTES) {
  const content = readLogSuffixSince(path, 0, maxBytes);
  if (!content) {
    return '';
  }
  const lines = content.split(/\r?\n/u);
  return `${lines.slice(Math.max(0, lines.length - lineCount)).join('\n')}\n`;
}

function readLogRange(path, start, length) {
  const buffer = Buffer.alloc(length);
  const descriptor = openSync(path, 'r');
  let bytesRead = 0;
  try {
    while (bytesRead < length) {
      const current = readSync(descriptor, buffer, bytesRead, length - bytesRead, start + bytesRead);
      if (current === 0) {
        break;
      }
      bytesRead += current;
    }
  } finally {
    closeSync(descriptor);
  }
  return buffer.subarray(0, bytesRead).toString('utf8');
}
