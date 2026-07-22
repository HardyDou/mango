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
          {{ loginBrand.panelTitle || $t('login.title') }}
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
            <el-input
              v-model="form.username"
              :placeholder="$t('login.username.placeholder')"
              size="large"
              prefix-icon="User"
              clearable
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
        <component :is="loginSlots.footer" v-if="loginSlots.footer" />
      </div>
    </div>
    <el-dialog v-model="wecomDialogVisible" title="企业微信扫码登录" width="420px">
      <div class="wecom-login-panel">
        <iframe v-if="wecomQrUrl" :src="wecomQrUrl" class="wecom-qr-frame" />
        <div v-else class="wecom-login-placeholder">
          请在通知管理的企业微信渠道配置中启用扫码登录，并补充 AgentId 和扫码回调地址；本地联调可输入授权 code。
        </div>
        <el-input v-model="wecomCode" placeholder="企业微信回调 code" clearable />
      </div>
      <template #footer>
        <el-button @click="wecomDialogVisible = false"> 取消 </el-button>
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
    <el-dialog
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
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Login">
import { computed, nextTick, onMounted, ref } from 'vue';
import { type FormInstance, type FormRules } from 'element-plus';
import { defaultPasswordPolicy, isPasswordPolicyPassed, PasswordPolicyHint } from '@mango/common';
import { useAuthConfig } from '../composables/useAuthConfig';
import { useMangoLoginFlow } from '../composables/useMangoLoginFlow';

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
  wecomLoading,
  passwordResetLoading,
  wecomCode,
  wecomQrUrl,
  canSubmitPasswordReset,
  passwordPolicyMessage,
} = loginFlow;

// 校验规则
const rules = {
  tenantId: [{ required: true, message: '请选择机构', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const wecomDialogVisible = ref(false);
const passwordResetDialogVisible = ref(false);

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
    const result = await loginFlow.initializeLoginFlow();
    if (result.shouldOpenWecomDialog) {
      wecomDialogVisible.value = true;
    }
  })();
});

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

async function openWecomLogin() {
  const result = await loginFlow.prepareWecomLogin();
  wecomDialogVisible.value = result.shouldOpenWecomDialog;
}

async function handleWecomLogin() {
  await loginFlow.submitWecomLogin();
}

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value || loading.value) return;

  await loginFormRef.value.validate(async (valid) => {
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
