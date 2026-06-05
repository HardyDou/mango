<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <component
          :is="loginSlots.brand"
          v-if="loginSlots.brand"
          :brand="loginBrand"
        />
        <div
          v-else
          class="login-title"
        >
          <h1>{{ loginBrand.title }}</h1>
          <p>{{ loginBrand.subtitle }}</p>
        </div>
      </div>
      <div class="login-form">
        <component
          :is="loginSlots.formHeader"
          v-if="loginSlots.formHeader"
          :form="form"
        />
        <h2 class="form-title">
          {{ loginBrand.panelTitle || $t('login.title') }}
        </h2>
        <component
          :is="loginSlots.formBefore"
          v-if="loginSlots.formBefore"
          :form="form"
          :tenant-options="tenantOptions"
        />
        <el-form
          ref="loginFormRef"
          :model="form"
          :rules="rules"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="tenantId">
            <el-select
              v-model="form.tenantId"
              placeholder="请选择机构"
              size="large"
              class="tenant-select"
              :loading="tenantLoading"
              filterable
            >
              <el-option
                v-for="tenant in tenantOptions"
                :key="tenant.tenantId"
              :label="tenant.tenantName"
              :value="tenant.tenantId"
            >
                <component
                  :is="loginSlots.tenantOption"
                  v-if="loginSlots.tenantOption"
                  :tenant="tenant"
                />
                <template v-else>
                  <span>{{ tenant.tenantName }}</span>
                  <span class="tenant-code">{{ tenant.tenantCode }}</span>
                </template>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              :placeholder="$t('login.username.placeholder')"
              size="large"
              prefix-icon="User"
              clearable
              @blur="refreshAccountTenants()"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              :placeholder="$t('login.password.placeholder')"
              type="password"
              size="large"
              prefix-icon="Lock"
              show-password
              clearable
              @blur="refreshAccountTenants()"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              :disabled="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ $t('login.btn') }}
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button
              size="large"
              class="wecom-login-btn"
              :loading="wecomLoading"
              :disabled="loading || wecomLoading || !form.tenantId"
              @click="openWecomLogin"
            >
              企业微信扫码登录
            </el-button>
          </el-form-item>
        </el-form>
        <component
          :is="loginSlots.formAfter"
          v-if="loginSlots.formAfter"
          :form="form"
          :tenant-options="tenantOptions"
        />
        <component
          :is="loginSlots.footer"
          v-if="loginSlots.footer"
        />
      </div>
    </div>
    <el-dialog
      v-model="wecomDialogVisible"
      title="企业微信扫码登录"
      width="420px"
    >
      <div class="wecom-login-panel">
        <iframe
          v-if="wecomQrUrl"
          :src="wecomQrUrl"
          class="wecom-qr-frame"
        />
        <div
          v-else
          class="wecom-login-placeholder"
        >
          请在通知中心的企业微信渠道配置中启用扫码登录，并补充 AgentId 和扫码回调地址；本地联调可输入授权 code。
        </div>
        <el-input
          v-model="wecomCode"
          placeholder="企业微信回调 code"
          clearable
        />
      </div>
      <template #footer>
        <el-button @click="wecomDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="wecomLoading"
          :disabled="!wecomCode.trim() || !form.tenantId"
          @click="handleWecomLogin()"
        >
          登录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Login">
import { computed, onMounted, ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Session } from '@mango/common/utils/storage';
import {
  getAccountLoginTenantOptions,
  getWecomLoginConfig,
  getLoginTenantOptions,
  login,
  wecomLogin,
  type WecomLoginConfig,
  type LoginTenantOption,
} from '../api/sys';
import { useUserInfo } from '../store/userInfo';
import { useAuthConfig } from '../composables/useAuthConfig';

const router = useRouter();
const loginFormRef = ref();
const userInfoStore = useUserInfo();
const authConfig = useAuthConfig();
const loginDefaults = computed(() => authConfig.value.login?.defaults || {});
const loginSlots = computed(() => authConfig.value.login?.slots || {});
const loginBrand = computed(() => ({
  title: authConfig.value.login?.brand?.title || 'Mango Admin',
  subtitle: authConfig.value.login?.brand?.subtitle || '企业级管理平台',
  panelTitle: authConfig.value.login?.brand?.panelTitle,
}));

// 表单数据
const form = reactive({
  tenantId: '',
  username: '',
  password: '',
});

// 校验规则
const rules = {
  tenantId: [{ required: true, message: '请选择机构', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

// 状态
const loading = ref(false);
const wecomLoading = ref(false);
const wecomDialogVisible = ref(false);
const wecomCode = ref('');
const wecomLoginConfig = ref<WecomLoginConfig>();
const tenantLoading = ref(false);
const tenantOptions = ref<LoginTenantOption[]>([]);
const accountTenantResolvedKey = ref('');
const accountTenantPendingKey = ref('');
let accountTenantPendingPromise: Promise<boolean> | undefined;

interface WecomCallbackState {
  tenantId?: string;
  channelConfigId?: string;
}

const selectedTenant = computed(() => {
  return tenantOptions.value.find((tenant) => tenant.tenantId === form.tenantId);
});
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

function buildWecomState(config: WecomLoginConfig) {
  const state = {
    t: String(form.tenantId || ''),
    c: config.channelConfigId == null ? '' : String(config.channelConfigId),
  };
  return `mwc.${base64UrlEncode(JSON.stringify(state))}`;
}

function parseWecomState(rawState: string | null): WecomCallbackState {
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
    const decoded = JSON.parse(base64UrlDecode(rawState.slice(statePrefix.length)));
    return {
      tenantId: decoded?.t ? String(decoded.t) : undefined,
      channelConfigId: decoded?.c ? String(decoded.c) : undefined,
    };
  } catch (error) {
    console.warn('解析企业微信登录 state 失败:', error);
    return {};
  }
}

function readWecomCallback() {
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

function removeParams(search: string, names: string[]) {
  const params = new URLSearchParams(search);
  names.forEach((name) => params.delete(name));
  const next = params.toString();
  return next ? `?${next}` : '';
}

function clearWecomCallbackUrl() {
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

const loadLoginTenants = async () => {
  tenantLoading.value = true;
  try {
    const options = await getLoginTenantOptions();
    tenantOptions.value = Array.isArray(options) ? options : [];
    if (!form.tenantId && tenantOptions.value.length > 0) {
      form.tenantId = tenantOptions.value.find((tenant) => tenant.tenantCode === (loginDefaults.value.tenantCode || 'default'))?.tenantId
        || tenantOptions.value[0].tenantId;
    }
  } catch (error) {
    console.error('获取登录机构失败:', error);
    ElMessage.error('获取登录机构失败');
  } finally {
    tenantLoading.value = false;
  }
};

const accountTenantKey = () => `${form.username.trim()}:${form.password}:${form.tenantId}`;

const applyTenantOptions = (options: LoginTenantOption[]) => {
  tenantOptions.value = Array.isArray(options) ? options : [];
  if (tenantOptions.value.length === 0) {
    form.tenantId = '';
    return;
  }

  const selectedExists = tenantOptions.value.some((tenant) => tenant.tenantId === form.tenantId);
  if (!form.tenantId || !selectedExists) {
    form.tenantId = tenantOptions.value.find((tenant) => tenant.tenantCode === (loginDefaults.value.tenantCode || 'default'))?.tenantId
      || tenantOptions.value[0].tenantId;
  }
};

const refreshAccountTenants = async (strict = false) => {
  if (!form.username.trim() || !form.password) {
    return !strict;
  }

  const key = accountTenantKey();
  if (accountTenantResolvedKey.value === key) {
    return true;
  }
  if (accountTenantPendingPromise && accountTenantPendingKey.value === key) {
    return accountTenantPendingPromise;
  }

  const selectedTenantId = form.tenantId;
  tenantLoading.value = true;
  accountTenantPendingKey.value = key;
  accountTenantPendingPromise = (async () => {
    const options = await getAccountLoginTenantOptions({
      username: form.username.trim(),
      password: form.password,
      realm: loginDefaults.value.realm || 'INTERNAL',
      appCode: loginDefaults.value.appCode || 'internal-admin',
    });
    accountTenantResolvedKey.value = accountTenantKey();
    const canKeepSelected = !selectedTenantId
      || options.some((tenant) => tenant.tenantId === selectedTenantId);
    applyTenantOptions(options);
    if (strict && !canKeepSelected) {
      ElMessage.warning('当前账号不可进入所选机构，请重新选择机构');
      return false;
    }
    return true;
  })();

  try {
    return await accountTenantPendingPromise;
  } catch (error) {
    console.error('查询账号可登录机构失败:', error);
    return false;
  } finally {
    tenantLoading.value = false;
    accountTenantPendingKey.value = '';
    accountTenantPendingPromise = undefined;
  }
};

onMounted(() => {
  void (async () => {
    const callback = readWecomCallback();
    if (callback.state.tenantId) {
      form.tenantId = callback.state.tenantId;
    }
    if (callback.hasCallbackParams) {
      clearWecomCallbackUrl();
    }

    await loadLoginTenants();
    if (callback.code) {
      await handleWecomCallback(callback.code, callback.state);
    }
  })();
});

function persistLoginResult(res: any, fallback: Record<string, any>) {
  const token = res?.accessToken || res?.token;
  if (!res || !token) {
    throw new Error('登录响应无效');
  }
  const userInfo = res.userInfo || res;
  const normalizedUserInfo = {
    ...userInfo,
    tenantId: userInfo.tenantId ?? res.tenantId ?? fallback.tenantId,
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
  userInfoStore.setUserInfos(normalizedUserInfo);
  if (normalizedUserInfo.tenantId) {
    Session.set('tenantId', normalizedUserInfo.tenantId);
  }
}

async function openWecomLogin() {
  if (!form.tenantId) {
    ElMessage.warning('请先选择机构');
    return;
  }
  wecomLoading.value = true;
  try {
    wecomLoginConfig.value = await getWecomLoginConfig(form.tenantId);
  } catch {
    wecomLoginConfig.value = undefined;
    ElMessage.warning('未读取到企业微信扫码登录配置');
  } finally {
    wecomLoading.value = false;
  }
  wecomDialogVisible.value = true;
}

async function handleWecomLogin() {
  if (!wecomCode.value.trim()) {
    ElMessage.warning('请输入企业微信授权 code');
    return;
  }
  if (!form.tenantId) {
    ElMessage.warning('企业微信回调缺少机构信息，请重新扫码');
    return;
  }
  wecomLoading.value = true;
  try {
    const loginData = {
      code: wecomCode.value.trim(),
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
    ElMessage.success('登录成功');
    await router.push(loginDefaults.value.redirectPath || '/home');
  } catch (error) {
    console.error('企业微信登录失败:', error);
    ElMessage.error('企业微信登录失败，请确认账号已绑定');
  } finally {
    wecomLoading.value = false;
  }
}

async function handleWecomCallback(code: string, state: WecomCallbackState) {
  wecomCode.value = code;
  if (!state.tenantId) {
    wecomDialogVisible.value = true;
    ElMessage.warning('企业微信回调缺少机构信息，请重新扫码');
    return;
  }
  if (state.tenantId) {
    form.tenantId = state.tenantId;
  }
  if (!form.tenantId) {
    wecomDialogVisible.value = true;
    ElMessage.warning('企业微信回调缺少机构信息，请重新选择机构后登录');
    return;
  }
  if (tenantOptions.value.length > 0 && !tenantOptions.value.some((tenant) => tenant.tenantId === form.tenantId)) {
    wecomDialogVisible.value = true;
    ElMessage.warning('企业微信回调机构不可用，请重新选择机构后登录');
    return;
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
  wecomDialogVisible.value = true;
  await handleWecomLogin();
}

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value || loading.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return;
    if (loading.value) return;

    loading.value = true;
    const loadingStartedAt = Date.now();
    try {
      const accountTenantsReady = accountTenantResolvedKey.value === accountTenantKey()
        || await refreshAccountTenants(true);
      if (!accountTenantsReady) {
        throw new Error('账号机构校验失败');
      }

      // 构造登录数据
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

      // 调用真实登录接口
      const res = await login(loginData);

      persistLoginResult(res, {
        tenantId: form.tenantId,
        tenantCode: selectedTenant.value?.tenantCode ?? loginData.tenantCode,
        tenantName: selectedTenant.value?.tenantName,
        realm: loginData.realm,
        actorType: loginData.actorType,
        partyType: loginData.partyType,
        appCode: loginData.appCode,
      });

      ElMessage.success('登录成功');
      await router.push(loginDefaults.value.redirectPath || '/home');
    } catch (error) {
      console.error('登录失败:', error);
      ElMessage.error('登录失败，请检查用户名和密码');
    } finally {
      const remaining = 500 - (Date.now() - loadingStartedAt);
      if (remaining > 0) {
        await new Promise((resolve) => setTimeout(resolve, remaining));
      }
      loading.value = false;
    }
  });
};
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  display: flex;
  width: 900px;
  height: 500px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.login-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 50%;
  padding: 40px;
  background: linear-gradient(135deg, #2E5CF6 0%, #764ba2 100%);

  .login-title {
    color: #fff;

    h1 {
      font-size: 36px;
      font-weight: 700;
      margin-bottom: 12px;
    }

    p {
      font-size: 18px;
      opacity: 0.9;
    }
  }
}

.login-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 50%;
  padding: 40px;

  .form-title {
    margin-bottom: 30px;
    font-size: 24px;
    font-weight: 600;
    color: #333;
    text-align: center;
  }

  .login-btn {
    width: 100%;
  }

  .wecom-login-btn {
    width: 100%;
  }
}

.wecom-login-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wecom-qr-frame {
  width: 100%;
  height: 260px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}

.wecom-login-placeholder {
  padding: 24px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}

:deep(.el-input__wrapper) {
  padding: 4px 12px;
}

.tenant-select {
  width: 100%;
}

.tenant-code {
  float: right;
  color: #909399;
  font-size: 12px;
}
</style>
