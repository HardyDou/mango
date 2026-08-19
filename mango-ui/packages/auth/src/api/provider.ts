import { get, post, put } from '@mango/common/utils/request';
import type { LoginResult } from './sys';
import type { ExternalAuthProvider } from './identity';

export type ProviderAuthorizationIntent = 'LOGIN' | 'BIND_CURRENT';
export type ProviderAuthorizationStatus = 'LOGIN_SUCCESS' | 'BIND_SUCCESS' | 'BIND_REQUIRED';

export interface AvailableProvider {
  provider: ExternalAuthProvider;
  displayName: string;
}

export interface ProviderAuthorization {
  authorizationUrl: string;
  expiresInSeconds: number;
}

export interface ProviderAuthorizationResult {
  status: ProviderAuthorizationStatus;
  login?: LoginResult;
  bindingTicket?: string;
  providerDisplayName?: string;
  expiresInSeconds?: number;
}

export interface ProviderConfig {
  id?: string | number;
  appCode: string;
  provider: ExternalAuthProvider;
  clientId?: string;
  providerTenantId?: string;
  agentId?: string;
  redirectUris: string[];
  enabled: boolean;
  secretConfigured?: boolean;
  complete?: boolean;
  updatedAt?: string;
}

export interface SaveProviderConfigPayload {
  id?: string | number;
  appCode: string;
  provider: ExternalAuthProvider;
  clientId?: string;
  providerTenantId?: string;
  agentId?: string;
  secret?: string;
  redirectUris: string[];
  enabled: boolean;
}

export function listAvailableProviders(tenantId: string | number, appCode: string) {
  return get<AvailableProvider[]>('/auth/providers', {
    params: { tenantId, appCode },
    ignoreToken: true,
  });
}

export function startProviderAuthorization(data: {
  tenantId: string | number;
  appCode: string;
  provider: ExternalAuthProvider;
  intent: ProviderAuthorizationIntent;
  redirectUri: string;
}) {
  return post<ProviderAuthorization>('/auth/providers/authorize', data, {
    ignoreToken: data.intent === 'LOGIN',
  });
}

export function completeProviderAuthorization(data: { state: string; code: string }) {
  return post<ProviderAuthorizationResult>('/auth/providers/complete', data, { ignoreToken: true });
}

export function bindExistingProviderAccount(data: { bindingTicket: string; username: string; password: string }) {
  return post<LoginResult>('/auth/providers/bind-existing', data, { ignoreToken: true });
}

export function refreshCurrentWecomProfile() {
  return post<boolean>('/auth/providers/wecom/profile/refresh');
}

export function listProviderConfigs(appCode: string) {
  return get<ProviderConfig[]>('/auth/provider-configs', { params: { appCode } });
}

export function saveProviderConfig(data: SaveProviderConfigPayload) {
  return data.id
    ? put<ProviderConfig>('/auth/provider-configs', data)
    : post<ProviderConfig>('/auth/provider-configs', data);
}

export function providerCallbackUri(path = '/provider-callback') {
  if (typeof window === 'undefined') {
    return path;
  }
  return `${window.location.origin}${window.location.pathname}`;
}

export function captureProviderCallbackBeforeHashRouter() {
  if (typeof window === 'undefined') {
    return;
  }
  const params = new URLSearchParams(window.location.search);
  const hasCallback = Boolean(params.get('state') && (params.get('code') || params.get('authCode')));
  if (!hasCallback || window.location.hash.startsWith('#/provider-callback')) {
    return;
  }
  window.history.replaceState(
    window.history.state,
    document.title,
    `${window.location.pathname}${window.location.search}#/provider-callback`,
  );
}
