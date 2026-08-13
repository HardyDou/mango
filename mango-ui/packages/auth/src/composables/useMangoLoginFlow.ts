import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { defaultPasswordPolicy, getPasswordPolicyMessage, isPasswordPolicyPassed } from '@mango/common';
import { Session } from '@mango/common/utils/storage';
import {
  changeRequiredPassword,
  getAccountLoginTenantOptions,
  getLoginTenantOptions,
  login,
  type LoginResult,
  type LoginTenantOption,
} from '../api/sys';
import { useUserInfo, type UserInfosState } from '../store/userInfo';
import { providerCallbackUri, startProviderAuthorization } from '../api/provider';
import type { MangoAuthConfig } from '../config';
import { useAuthConfig } from './useAuthConfig';

const MANGO_AUTH_LOGIN_SUCCESS_EVENT = 'mango-auth:login-success';
const PROVIDER_RETURN_KEY = 'mango-auth:provider-return';

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

export type MangoLoginActionStatus = 'success' | 'password-reset-required' | 'failed' | 'ignored';

export interface MangoLoginActionResult {
  status: MangoLoginActionStatus;
  result?: LoginResult;
  error?: unknown;
}

export interface MangoWecomActionResult extends MangoLoginActionResult {
  shouldOpenWecomDialog: boolean;
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
  const loginRedirectTarget = computed(() =>
    normalizeLoginRedirect(route.query[loginDefaults.value.redirectQueryKey || 'redirect']),
  );

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
  const wecomAuthorizationUrl = ref('');
  const passwordResetRequired = ref(false);
  const passwordResetTicket = ref('');
  const passwordResetFallback = ref<MangoLoginContextFallback>({});
  const lastError = ref<unknown>();

  const selectedTenant = computed(() => tenantOptions.value.find((tenant) => tenant.tenantId === form.tenantId));
  const wecomQrUrl = computed(() => wecomAuthorizationUrl.value);
  const passwordPolicyMessage = computed(() => getPasswordPolicyMessage(defaultPasswordPolicy));
  const canSubmitPasswordReset = computed(
    () =>
      isPasswordPolicyPassed(passwordResetForm.newPassword, defaultPasswordPolicy) &&
      passwordResetForm.confirmPassword === passwordResetForm.newPassword,
  );
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
      form.tenantId =
        tenantOptions.value.find((tenant) => tenant.tenantCode === (loginDefaults.value.tenantCode || 'default'))
          ?.tenantId || tenantOptions.value[0].tenantId;
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
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent(MANGO_AUTH_LOGIN_SUCCESS_EVENT));
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
      const authorization = await startProviderAuthorization({
        tenantId: form.tenantId,
        appCode: loginDefaults.value.appCode || 'internal-admin',
        provider: 'WECOM',
        intent: 'LOGIN',
        redirectUri: providerCallbackUri(),
      });
      wecomAuthorizationUrl.value = authorization.authorizationUrl;
      if (typeof window !== 'undefined') {
        window.sessionStorage.setItem(PROVIDER_RETURN_KEY, resolveLoginRedirectPath());
      }
      return { status: 'success', shouldOpenWecomDialog: true };
    } catch (error) {
      lastError.value = error;
      wecomAuthorizationUrl.value = '';
      notify('warning', '未读取到企业微信登录配置');
      return { status: 'failed', error, shouldOpenWecomDialog: false };
    } finally {
      wecomLoading.value = false;
    }
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

async function waitForMinLoading(startedAt: number, minMs: number) {
  const remaining = minMs - (Date.now() - startedAt);
  if (remaining > 0) {
    await new Promise((resolve) => setTimeout(resolve, remaining));
  }
}

function stringifyOptional(value: unknown) {
  return value == null ? undefined : String(value);
}
