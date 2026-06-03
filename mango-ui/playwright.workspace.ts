import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

type WorkspaceEnv = Record<string, string>;

type ResolveOptions = {
  uiRoot: string;
  defaultURL: string;
};

function cleanValue(value: string | undefined): string {
  if (!value) {
    return '';
  }
  return value.trim().replace(/^['"]|['"]$/g, '');
}

function normalizeUrl(url: string): string {
  return cleanValue(url).replace(/\/+$/g, '');
}

function readWorkspaceEnv(uiRoot: string): WorkspaceEnv {
  const envPath = resolve(uiRoot, '..', '.mango', 'dev-workspace.env');
  if (!existsSync(envPath)) {
    return {};
  }

  return readFileSync(envPath, 'utf8')
    .split(/\r?\n/)
    .reduce<WorkspaceEnv>((env, line) => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) {
        return env;
      }
      const separator = trimmed.indexOf('=');
      if (separator === -1) {
        return env;
      }
      const key = trimmed.slice(0, separator).trim();
      const value = cleanValue(trimmed.slice(separator + 1));
      env[key] = value;
      return env;
    }, {});
}

function workspaceFrontendUrl(uiRoot: string): string {
  const env = readWorkspaceEnv(uiRoot);
  if (!env.MANGO_FRONTEND_PORT) {
    return '';
  }
  return `http://${env.MANGO_FRONTEND_HOST || '127.0.0.1'}:${env.MANGO_FRONTEND_PORT}`;
}

function workspaceApiBaseUrl(uiRoot: string): string {
  const env = readWorkspaceEnv(uiRoot);
  if (!env.MANGO_BACKEND_PORT) {
    return '';
  }
  return `http://127.0.0.1:${env.MANGO_BACKEND_PORT}`;
}

export function resolveE2EBaseURL({ uiRoot, defaultURL }: ResolveOptions): string {
  return normalizeUrl(process.env.PLAYWRIGHT_BASE_URL || workspaceFrontendUrl(uiRoot) || defaultURL);
}

export function resolveE2EApiBaseURL({ uiRoot, defaultURL }: ResolveOptions): string {
  return normalizeUrl(
    process.env.PLAYWRIGHT_API_BASE_URL
      || process.env.VITE_ADMIN_PROXY_PATH
      || workspaceApiBaseUrl(uiRoot)
      || defaultURL,
  );
}
