import { resolve } from 'node:path';
import { resolveE2EApiBaseURL } from '../../../../playwright.workspace';

const apiBaseURL = resolveE2EApiBaseURL({
  uiRoot: resolve(__dirname, '../../../..'),
  defaultURL: 'http://127.0.0.1:5555',
});

export function api(path: string): string {
  if (/^https?:\/\//.test(path)) {
    return path;
  }
  return `${apiBaseURL}${path.startsWith('/') ? path : `/${path}`}`;
}
