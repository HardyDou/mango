import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  defaultPasswordPolicy,
  getPasswordPolicyMessage,
  isPasswordPolicyPassed,
} from '@mango/common';
import { Session } from '@mango/common/utils/storage';
import {
  changeRequiredPassword,
  getAccountLoginTenantOptions,
  getLoginTenantOptions,
  getWecomLoginConfig,
  login,
  wecomLogin,
  type LoginResult,
  type LoginTenantOption,
  type WecomLoginConfig,
} from '../api/sys';
import { useUserInfo, type UserInfosState } from '../store/userInfo';
import type { MangoAuthConfig } from '../config';
import { useAuthConfig } from './useAuthConfig';

export interface MangoLoginForm {
  tenantId: string;
  username: string;
  password: string;
}

export interface MangoPasswordResetForm {
  newPassword: string;
  confirmPassword: string;
}

export type MangoLoginDefaults = NonNullable<MangoAuthConfig['login']>['defaults'];

export interface MangoLoginFlowOptions {
  defaults?: MangoLoginDefaults;
  minLoadingMs?: number;
  showMessage?: boolean;
  autoRedirect?: boolean;
}

export interface MangoWecomCallbackState {
  tenantId?: string;
  channelConfigId?: string;
}

export type MangoLoginActionStatus = 'success' | 'password-reset-required' | 'failed' | 'ignored';

export interface MangoLoginActionResult {
  status: MangoLoginActionStatus;
  result?: LoginResult;
  error?: unknown;
}

export interface MangoWecomActionResult extends MangoLoginActionResult {
  shouldOpenWecomDialog: boolean;
}

export interface MangoLoginInitializeResult {
  hasWecomCallback: boolean;
  shouldOpenWecomDialog: boolean;
  result?: MangoWecomActionResult;
}

interface MangoLoginContextFallback {
  tenantId?: string | number;
  tenantCode?: string;
  tenantName?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: string | number;
  appCode?: string;
}

type MangoStoredUserInfo = Partial<UserInfosState['userInfos']> & Record<string, unknown>;

const DEFAULT_LOGIN_MIN_LOADING_MS = 500;

export function useMangoLoginFlow(options: MangoLoginFlowOptions = {}) {
  const router = useRouter();
  const route = useRoute();
  const userInfoStore = useUserInfo();
  const authConfig = useAuthConfig();

  const loginDefaults = computed(() => ({
    ...(authConfig.value.login?.defaults || {}),
    ...(options.defaults || {}),
  }));
  const showMessage = computed(() => options.showMessage !== false);
  const shouldAutoRedirect = computed(() => options.autoRedirect !== false);
  const minLoadingMs = computed(() => options.minLoadingMs ?? DEFAULT_LOGIN_MIN_LOADING_MS);
  const loginRedirectTarget = computed(() => normalizeLoginRedirect(
    route.query[loginDefaults.value.redirectQueryKey || 'redirect']
  ));

  const form = reactive<MangoLoginForm>({
    tenantId: '',
    username: '',
    password: '',
  });
  const passwordResetForm = reactive<MangoPasswordResetForm>({
    newPassword: '',
    confirmPassword: '',
  });

  const loading = ref(false);
  const tenantLoading = ref(false);
  const wecomLoading = ref(false);
  const passwordResetLoading = ref(false);
  const tenantOptions = ref<LoginTenantOption[]>([]);
  const wecomCode = ref('');
  const wecomLoginConfig = ref<WecomLoginConfig>();
  const passwordResetRequired = ref(false);
  const passwordResetTicket = ref('');
  const passwordResetFallback = ref<MangoLoginContextFallback>({});
  const lastError = ref<unknown>();

  const selectedTenant = computed(() => tenantOptions.value.find((tenant) => tenant.tenantId === form.tenantId));
  const passwordPolicyMessage = computed(() => getPasswordPolicyMessage(defaultPasswordPolicy));
  const canSubmitPasswordReset = computed(() =>
    isPasswordPolicyPassed(passwordResetForm.newPassword, defaultPasswordPolicy)
    && passwordResetForm.confirmPassword === passwordResetForm.newPassword
  );
  const wecomQrUrl = computed(() => {
    const config = wecomLoginConfig.value;
    if (!config?.corpId || !config.agentId || !config.redirectUri) {
      return '';
    }
    const params = new URLSearchParams({
      appid: config.corpId,
      agentid: String(config.agentId),
      redirect_uri: config.redirectUri,
      state: buildWecomState(config),
    });
    return `https://open.work.weixin.qq.com/wwopen/sso/qrConnect?${params.toString()}`;
  });

  function notify(type: 'success' | 'warning' | 'error', message: string) {
    if (!showMessage.value) {
      return;
    }
    ElMessage[type](message);
  }

  function setTenantId(tenantId: string | number | undefined) {
    form.tenantId = tenantId == null ? '' : String(tenantId);
  }

  function setCredentials(credentials: Partial<Pick<MangoLoginForm, 'username' | 'password'>>) {
    if (credentials.username != null) {
      form.username = credentials.username;
    }
    if (credentials.password != null) {
      form.password = credentials.password;
    }
  }

  function applyTenantOptions(optionsList: LoginTenantOption[], keepExistingTenant = true) {
    tenantOptions.value = Array.isArray(optionsList) ? optionsList : [];
    if (tenantOptions.value.length === 0) {
      form.tenantId = '';
      return;
    }
    const selectedExists = tenantOptions.value.some((tenant) => tenant.tenantId === form.tenantId);
    if (keepExistingTenant && form.tenantId && selectedExists) {
      return;
    }
    if (!form.tenantId || !selectedExists) {
      form.tenantId = tenantOptions.value.find((tenant) =>
        tenant.tenantCode === (loginDefaults.value.tenantCode || 'default'))?.tenantId
        || tenantOptions.value[0].tenantId;
    }
  }

  async function loadLoginTenants() {
    tenantLoading.value = true;
    lastError.value = undefined;
    try {
      const optionsList = await getLoginTenantOptions();
      applyTenantOptions(optionsList, true);
      return tenantOptions.value;
    } catch (error) {
      lastError.value = error;
      console.error('获取登录机构失败:', error);
      notify('error', '获取登录机构失败');
      return [];
    } finally {
      tenantLoading.value = false;
    }
  }

  async function loadAccountLoginTenants(username = form.username) {
    const account = username.trim();
    if (!account) {
      return loadLoginTenants();
    }
    tenantLoading.value = true;
    lastError.value = undefined;
    try {
      const optionsList = await getAccountLoginTenantOptions({
        username: account,
        realm: loginDefaults.value.realm || 'INTERNAL',
        appCode: loginDefaults.value.appCode || 'internal-admin',
      });
      applyTenantOptions(optionsList, false);
      return tenantOptions.value;
    } catch (error) {
      lastError.value = error;
      console.error('获取账号可登录机构失败:', error);
      notify('error', '获取账号可登录机构失败');
      return [];
    } finally {
      tenantLoading.value = false;
    }
  }

  function resolveLoginRedirectPath(): string {
    return loginRedirectTarget.value || loginDefaults.value.redirectPath || '/home';
  }

  async function redirectAfterLogin() {
    if (!shouldAutoRedirect.value) {
      return;
    }
    await router.push(resolveLoginRedirectPath());
  }

  function persistLoginResult(res: LoginResult, fallback: MangoLoginContextFallback = {}) {
    const token = res?.accessToken || res?.token;
    if (!res || !token) {
      throw new Error('登录响应无效');
    }
    const userInfo = (res.userInfo || res) as MangoStoredUserInfo;
    const normalizedUserInfo: MangoStoredUserInfo = {
      ...userInfo,
      tenantId: stringifyOptional(userInfo.tenantId ?? res.tenantId ?? fallback.tenantId),
      tenantCode: userInfo.tenantCode ?? res.tenantCode ?? fallback.tenantCode,
      tenantName: userInfo.tenantName ?? res.tenantName ?? fallback.tenantName,
      realm: userInfo.realm ?? res.realm ?? fallback.realm,
      actorType: userInfo.actorType ?? res.actorType ?? fallback.actorType,
      partyType: userInfo.partyType ?? res.partyType ?? fallback.partyType,
      partyId: userInfo.partyId ?? res.partyId ?? fallback.partyId,
      appCode: userInfo.appCode ?? res.appCode ?? fallback.appCode,
    };
    Session.setToken(token, {
      refreshToken: res.refreshToken,
      expiresIn: Number(res.expiresIn) || undefined,
    });
    userInfoStore.setUserInfos(normalizedUserInfo as Partial<UserInfosState['userInfos']>);
    if (normalizedUserInfo.tenantId) {
      Session.set('tenantId', String(normalizedUserInfo.tenantId));
    }
  }

  function openPasswordReset(res: LoginResult, fallback: MangoLoginContextFallback) {
    if (!res.passwordResetTicket) {
      throw new Error('强制改密票据缺失');
    }
    passwordResetTicket.value = res.passwordResetTicket;
    passwordResetFallback.value = fallback;
    passwordResetForm.newPassword = '';
    passwordResetForm.confirmPassword = '';
    passwordResetRequired.value = true;
    notify('warning', '当前账号需要修改密码后才能继续登录');
  }

  function clearPasswordReset() {
    passwordResetRequired.value = false;
    passwordResetTicket.value = '';
    passwordResetFallback.value = {};
    passwordResetForm.newPassword = '';
    passwordResetForm.confirmPassword = '';
  }

  async function submitPasswordLogin(): Promise<MangoLoginActionResult> {
    if (loading.value) {
      return { status: 'ignored' };
    }

    loading.value = true;
    lastError.value = undefined;
    const loadingStartedAt = Date.now();
    try {
      const loginData = {
        username: form.username,
        password: form.password,
        tenantId: form.tenantId,
        tenantCode: selectedTenant.value?.tenantCode,
        realm: loginDefaults.value.realm || 'INTERNAL',
        actorType: loginDefaults.value.actorType || 'INTERNAL_USER',
        partyType: loginDefaults.value.partyType || 'INTERNAL_ORG',
        appCode: loginDefaults.value.appCode || 'internal-admin',
      };
      const res = await login(loginData);
      const fallback: MangoLoginContextFallback = {
        tenantId: form.tenantId,
        tenantCode: selectedTenant.value?.tenantCode ?? loginData.tenantCode,
        tenantName: selectedTenant.value?.tenantName,
        realm: loginData.realm,
        actorType: loginData.actorType,
        partyType: loginData.partyType,
        appCode: loginData.appCode,
      };

      if (res.passwordResetRequired || res.loginAction === 'CHANGE_PASSWORD') {
        openPasswordReset(res, fallback);
        return { status: 'password-reset-required', result: res };
      }

      persistLoginResult(res, fallback);
      notify('success', '登录成功');
      await redirectAfterLogin();
      return { status: 'success', result: res };
    } catch (error) {
      lastError.value = error;
      return { status: 'failed', error };
    } finally {
      await waitForMinLoading(loadingStartedAt, minLoadingMs.value);
      loading.value = false;
    }
  }

  async function submitRequiredPasswordChange(): Promise<MangoLoginActionResult> {
    if (passwordResetLoading.value) {
      return { status: 'ignored' };
    }
    if (!passwordResetTicket.value) {
      notify('error', '强制改密票据缺失');
      return { status: 'failed' };
    }
    passwordResetLoading.value = true;
    lastError.value = undefined;
    try {
      const res = await changeRequiredPassword({
        passwordResetTicket: passwordResetTicket.value,
        newPassword: passwordResetForm.newPassword,
        confirmPassword: passwordResetForm.confirmPassword,
      });
      persistLoginResult(res, passwordResetFallback.value);
      clearPasswordReset();
      notify('success', '密码已修改');
      await redirectAfterLogin();
      return { status: 'success', result: res };
    } catch (error) {
      lastError.value = error;
      console.error('强制改密失败:', error);
      notify('error', '密码修改失败，请确认密码符合复杂度要求');
      return { status: 'failed', error };
    } finally {
      passwordResetLoading.value = false;
    }
  }

  async function prepareWecomLogin(): Promise<MangoWecomActionResult> {
    if (!form.tenantId) {
      notify('warning', '请先选择机构');
      return { status: 'failed', shouldOpenWecomDialog: false };
    }
    wecomLoading.value = true;
    lastError.value = undefined;
    try {
      wecomLoginConfig.value = await getWecomLoginConfig(form.tenantId);
    } catch (error) {
      lastError.value = error;
      wecomLoginConfig.value = undefined;
      notify('warning', '未读取到企业微信扫码登录配置');
    } finally {
      wecomLoading.value = false;
    }
    return { status: 'success', shouldOpenWecomDialog: true };
  }

  async function submitWecomLogin(code = wecomCode.value): Promise<MangoWecomActionResult> {
    const authorizationCode = code.trim();
    if (!authorizationCode) {
      notify('warning', '请输入企业微信授权 code');
      return { status: 'failed', shouldOpenWecomDialog: true };
    }
    if (!form.tenantId) {
      notify('warning', '企业微信回调缺少机构信息，请重新扫码');
      return { status: 'failed', shouldOpenWecomDialog: true };
    }
    wecomCode.value = authorizationCode;
    wecomLoading.value = true;
    lastError.value = undefined;
    try {
      const loginData = {
        code: authorizationCode,
        channelConfigId: wecomLoginConfig.value?.channelConfigId,
        tenantId: form.tenantId,
        tenantCode: selectedTenant.value?.tenantCode,
        appCode: loginDefaults.value.appCode || 'internal-admin',
      };
      const res = await wecomLogin(loginData);
      persistLoginResult(res, {
        tenantId: form.tenantId,
        tenantCode: selectedTenant.value?.tenantCode,
        tenantName: selectedTenant.value?.tenantName,
        appCode: loginData.appCode,
      });
      notify('success', '登录成功');
      await redirectAfterLogin();
      return { status: 'success', result: res, shouldOpenWecomDialog: true };
    } catch (error) {
      lastError.value = error;
      console.error('企业微信登录失败:', error);
      notify('error', '企业微信登录失败，请确认账号已绑定');
      return { status: 'failed', error, shouldOpenWecomDialog: true };
    } finally {
      wecomLoading.value = false;
    }
  }

  async function handleWecomCallback(
    code: string,
    state: MangoWecomCallbackState
  ): Promise<MangoWecomActionResult> {
    wecomCode.value = code;
    if (!state.tenantId) {
      notify('warning', '企业微信回调缺少机构信息，请重新扫码');
      return { status: 'failed', shouldOpenWecomDialog: true };
    }
    setTenantId(state.tenantId);
    if (!form.tenantId) {
      notify('warning', '企业微信回调缺少机构信息，请重新选择机构后登录');
      return { status: 'failed', shouldOpenWecomDialog: true };
    }
    if (tenantOptions.value.length > 0 && !tenantOptions.value.some((tenant) => tenant.tenantId === form.tenantId)) {
      notify('warning', '企业微信回调机构不可用，请重新选择机构后登录');
      return { status: 'failed', shouldOpenWecomDialog: true };
    }

    wecomLoginConfig.value = state.channelConfigId
      ? { channelConfigId: state.channelConfigId }
      : undefined;
    if (!wecomLoginConfig.value?.channelConfigId) {
      try {
        wecomLoginConfig.value = await getWecomLoginConfig(form.tenantId);
      } catch (error) {
        console.warn('恢复企业微信扫码登录配置失败:', error);
      }
    }
    return submitWecomLogin(code);
  }

  async function initializeLoginFlow(): Promise<MangoLoginInitializeResult> {
    const callback = readWecomCallback();
    if (callback.state.tenantId) {
      setTenantId(callback.state.tenantId);
    }
    if (callback.hasCallbackParams) {
      clearWecomCallbackUrl();
    }

    await loadLoginTenants();
    if (!callback.code) {
      return {
        hasWecomCallback: callback.hasCallbackParams,
        shouldOpenWecomDialog: false,
      };
    }

    const result = await handleWecomCallback(callback.code, callback.state);
    return {
      hasWecomCallback: true,
      shouldOpenWecomDialog: result.shouldOpenWecomDialog,
      result,
    };
  }

  function buildWecomState(config: WecomLoginConfig) {
    const state = {
      t: String(form.tenantId || ''),
      c: config.channelConfigId == null ? '' : String(config.channelConfigId),
    };
    return `mwc.${base64UrlEncode(JSON.stringify(state))}`;
  }

  return {
    form,
    passwordResetForm,
    loading,
    tenantLoading,
    wecomLoading,
    passwordResetLoading,
    tenantOptions,
    selectedTenant,
    wecomCode,
    wecomLoginConfig,
    wecomQrUrl,
    passwordResetRequired,
    passwordResetTicket,
    passwordPolicyMessage,
    canSubmitPasswordReset,
    lastError,
    loginDefaults,
    setTenantId,
    setCredentials,
    applyTenantOptions,
    loadLoginTenants,
    loadAccountLoginTenants,
    resolveLoginRedirectPath,
    persistLoginResult,
    clearPasswordReset,
    submitPasswordLogin,
    submitRequiredPasswordChange,
    prepareWecomLogin,
    submitWecomLogin,
    handleWecomCallback,
    initializeLoginFlow,
    buildWecomState,
  };
}

export function normalizeLoginRedirect(value: unknown): string {
  const rawValue = Array.isArray(value) ? value[0] : value;
  if (typeof rawValue !== 'string') {
    return '';
  }
  const target = rawValue.trim();
  if (!target || !target.startsWith('/') || target.startsWith('//')) {
    return '';
  }
  const targetPath = target.split('?')[0].split('#')[0];
  if (targetPath === '/login') {
    return '';
  }
  return target;
}

export function parseWecomState(rawState: string | null): MangoWecomCallbackState {
  if (!rawState) {
    return {};
  }
  if (rawState.startsWith('tenant:')) {
    return { tenantId: rawState.slice('tenant:'.length) || undefined };
  }
  const statePrefix = rawState.startsWith('mango-wecom.')
    ? 'mango-wecom.'
    : 'mwc.';
  if (!rawState.startsWith(statePrefix)) {
    return {};
  }
  try {
    const decoded = JSON.parse(base64UrlDecode(rawState.slice(statePrefix.length))) as Record<string, unknown>;
    return {
      tenantId: decoded.t ? String(decoded.t) : undefined,
      channelConfigId: decoded.c ? String(decoded.c) : undefined,
    };
  } catch (error) {
    console.warn('解析企业微信登录 state 失败:', error);
    return {};
  }
}

export function readWecomCallback() {
  if (typeof window === 'undefined') {
    return {
      code: '',
      state: {},
      hasCallbackParams: false,
    };
  }
  const params = new URLSearchParams(window.location.search);
  const hashQueryStart = window.location.hash.indexOf('?');
  if (hashQueryStart >= 0) {
    const hashParams = new URLSearchParams(window.location.hash.slice(hashQueryStart + 1));
    hashParams.forEach((value, key) => {
      if (!params.has(key)) {
        params.set(key, value);
      }
    });
  }
  const code = params.get('code')?.trim() || '';
  const state = params.get('state');
  return {
    code,
    state: parseWecomState(state),
    hasCallbackParams: Boolean(code || state),
  };
}

export function clearWecomCallbackUrl() {
  if (typeof window === 'undefined') {
    return;
  }
  const url = new URL(window.location.href);
  url.search = removeParams(url.search, ['code', 'state']);
  const hashQueryStart = url.hash.indexOf('?');
  if (hashQueryStart >= 0) {
    const hashPath = url.hash.slice(0, hashQueryStart);
    const hashSearch = removeParams(url.hash.slice(hashQueryStart), ['code', 'state']);
    url.hash = `${hashPath}${hashSearch}`;
  }
  window.history.replaceState(window.history.state, document.title, `${url.pathname}${url.search}${url.hash}`);
}

function removeParams(search: string, names: string[]) {
  const params = new URLSearchParams(search);
  names.forEach((name) => params.delete(name));
  const next = params.toString();
  return next ? `?${next}` : '';
}

function base64UrlEncode(value: string) {
  const bytes = new TextEncoder().encode(value);
  let binary = '';
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return window.btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '');
}

function base64UrlDecode(value: string) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=');
  const binary = window.atob(padded);
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

async function waitForMinLoading(startedAt: number, minMs: number) {
  const remaining = minMs - (Date.now() - startedAt);
  if (remaining > 0) {
    await new Promise((resolve) => setTimeout(resolve, remaining));
  }
}

function stringifyOptional(value: unknown) {
  return value == null ? undefined : String(value);
}
