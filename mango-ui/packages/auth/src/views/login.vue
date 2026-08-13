<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <component :is="loginSlots.brand" v-if="loginSlots.brand" :brand="loginBrand" />
        <div v-else class="login-title">
          <img v-if="loginBrand.logoUrl" class="login-logo" :src="loginBrand.logoUrl" alt="logo" />
          <h1>{{ loginBrand.title }}</h1>
          <p>{{ loginBrand.subtitle }}</p>
          <img v-if="loginBrand.imageUrl" class="login-brand-image" :src="loginBrand.imageUrl" alt="login brand" />
        </div>
      </div>
      <div class="login-form">
        <component :is="loginSlots.formHeader" v-if="loginSlots.formHeader" :form="form" />
        <h2 class="form-title">
          {{ loginBrand.panelTitle || '登录' }}
        </h2>
        <component
          :is="loginSlots.formBefore"
          v-if="loginSlots.formBefore"
          :form="form"
          :tenant-options="tenantOptions"
        />
        <el-form ref="loginFormRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
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
                <component :is="loginSlots.tenantOption" v-if="loginSlots.tenantOption" :tenant="tenant" />
                <template v-else>
                  <span>{{ tenant.tenantName }}</span>
                  <span class="tenant-code">{{ tenant.tenantCode }}</span>
                </template>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              type="password"
              size="large"
              prefix-icon="Lock"
              show-password
              clearable
              @blur="loginFlow.loadAccountLoginTenants()"
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
              登录
            </el-button>
          </el-form-item>
        </el-form>
        <div v-if="providerLoading || availableProviders.length > 0" class="provider-login">
          <el-divider>其他登录方式</el-divider>
          <div class="provider-actions">
            <el-button
              v-for="provider in availableProviders"
              :key="provider.provider"
              size="large"
              :data-provider="provider.provider"
              :loading="authorizingProvider === provider.provider"
              :disabled="loading || Boolean(authorizingProvider)"
              @click="startExternalLogin(provider.provider)"
            >
              使用{{ provider.displayName }}登录
            </el-button>
          </div>
        </div>
        <component
          :is="loginSlots.formAfter"
          v-if="loginSlots.formAfter"
          :form="form"
          :tenant-options="tenantOptions"
        />
        <component :is="loginSlots.footer" v-if="loginSlots.footer" />
      </div>
    </div>
    <MangoDialog
      v-model="passwordResetDialogVisible"
      title="修改登录密码"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-form
        ref="passwordResetFormRef"
        :model="passwordResetForm"
        :rules="passwordResetRules"
        label-width="96px"
        @keyup.enter="handleChangeRequiredPassword"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordResetForm.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="至少8位，包含字母和数字"
          />
          <PasswordPolicyHint :password="passwordResetForm.newPassword" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordResetForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          type="primary"
          :loading="passwordResetLoading"
          :disabled="!canSubmitPasswordReset"
          @click="handleChangeRequiredPassword"
        >
          确定
        </el-button>
      </template>
    </MangoDialog>
  </div>
</template>

<script setup lang="ts" name="Login">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { type FormInstance, type FormRules } from 'element-plus';
import { defaultPasswordPolicy, isPasswordPolicyPassed, MangoDialog, PasswordPolicyHint } from '@mango/common';
import { useAuthConfig } from '../composables/useAuthConfig';
import { useMangoLoginFlow } from '../composables/useMangoLoginFlow';
import {
  listAvailableProviders,
  providerCallbackUri,
  startProviderAuthorization,
  type AvailableProvider,
} from '../api/provider';
import type { ExternalAuthProvider } from '../api/identity';

const loginFormRef = ref();
const passwordResetFormRef = ref<FormInstance>();
const authConfig = useAuthConfig();
const loginSlots = computed(() => authConfig.value.login?.slots || {});
const loginBrand = computed(() => ({
  title: authConfig.value.login?.brand?.title || 'Mango Admin',
  subtitle: authConfig.value.login?.brand?.subtitle || '企业级管理平台',
  panelTitle: authConfig.value.login?.brand?.panelTitle,
  logoUrl: authConfig.value.login?.brand?.logoUrl || '',
  imageUrl: authConfig.value.login?.brand?.imageUrl || '',
}));
const loginFlow = useMangoLoginFlow();
const {
  form,
  passwordResetForm,
  tenantOptions,
  tenantLoading,
  loading,
  passwordResetLoading,
  canSubmitPasswordReset,
  passwordPolicyMessage,
} = loginFlow;

// 校验规则
const rules = {
  tenantId: [{ required: true, message: '请选择机构', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const passwordResetDialogVisible = ref(false);
const providerLoading = ref(false);
const availableProviders = ref<AvailableProvider[]>([]);
const authorizingProvider = ref<ExternalAuthProvider>();

const passwordResetRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!isPasswordPolicyPassed(String(value || ''), defaultPasswordPolicy)) {
          callback(new Error(passwordPolicyMessage.value));
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordResetForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
};

onMounted(() => {
  void (async () => {
    await loginFlow.loadLoginTenants();
    await loadAvailableProviders();
  })();
});

watch(
  () => form.tenantId,
  () => {
    void loadAvailableProviders();
  },
);

async function handleChangeRequiredPassword() {
  if (!passwordResetFormRef.value || passwordResetLoading.value) {
    return;
  }
  await passwordResetFormRef.value.validate();
  const result = await loginFlow.submitRequiredPasswordChange();
  if (result.status === 'success') {
    passwordResetDialogVisible.value = false;
  }
}

async function loadAvailableProviders() {
  if (!form.tenantId) {
    availableProviders.value = [];
    return;
  }
  providerLoading.value = true;
  try {
    availableProviders.value = await listAvailableProviders(
      form.tenantId,
      loginFlow.loginDefaults.value.appCode || 'internal-admin',
    );
  } catch (error) {
    console.error('加载第三方登录方式失败:', error);
    availableProviders.value = [];
  } finally {
    providerLoading.value = false;
  }
}

async function startExternalLogin(provider: ExternalAuthProvider) {
  if (!form.tenantId || authorizingProvider.value) return;
  authorizingProvider.value = provider;
  try {
    const authorization = await startProviderAuthorization({
      tenantId: form.tenantId,
      appCode: loginFlow.loginDefaults.value.appCode || 'internal-admin',
      provider,
      intent: 'LOGIN',
      redirectUri: providerCallbackUri(),
    });
    window.sessionStorage.setItem('mango-auth:provider-return', loginFlow.resolveLoginRedirectPath());
    window.location.assign(authorization.authorizationUrl);
  } catch (error) {
    console.error('发起第三方登录失败:', error);
    authorizingProvider.value = undefined;
  }
}

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value || loading.value) return;

  await loginFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    const result = await loginFlow.submitPasswordLogin();
    if (result.status === 'password-reset-required') {
      passwordResetDialogVisible.value = true;
      await nextTick();
      passwordResetFormRef.value?.clearValidate();
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
  min-height: 500px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgb(0 0 0 / 30%);
  overflow: hidden;
}

.login-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 50%;
  padding: 40px;
  background: linear-gradient(135deg, #2e5cf6 0%, #764ba2 100%);

  .login-title {
    color: #fff;

    .login-logo {
      width: 56px;
      height: 56px;
      margin-bottom: 18px;
      object-fit: contain;
    }

    h1 {
      font-size: 36px;
      font-weight: 700;
      margin-bottom: 12px;
    }

    p {
      font-size: 18px;
      opacity: 0.9;
    }

    .login-brand-image {
      width: 100%;
      max-height: 180px;
      margin-top: 28px;
      object-fit: contain;
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
}

.provider-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;

  .el-button {
    width: 100%;
    margin: 0;
  }
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

@media (width <= 768px) {
  .login-container {
    align-items: flex-start;
    padding: 20px;
    overflow-y: auto;
  }

  .login-box {
    width: 100%;
    height: auto;
    min-height: 0;
  }

  .login-left {
    display: none;
  }

  .login-form {
    width: 100%;
    padding: 28px 20px;
  }
}
</style>
