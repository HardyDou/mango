<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="login-title">
          <h1>Mango Admin</h1>
          <p>企业级管理平台</p>
        </div>
      </div>
      <div class="login-form">
        <h2 class="form-title">
          {{ $t('login.title') }}
        </h2>
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
                <span>{{ tenant.tenantName }}</span>
                <span class="tenant-code">{{ tenant.tenantCode }}</span>
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
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="Login">
import { computed, onMounted, ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Session } from '@mango/common';
import {
  getAccountLoginTenantOptions,
  getLoginTenantOptions,
  login,
  type LoginTenantOption,
} from '../api/sys';
import { useUserInfo } from '../store/userInfo';

const router = useRouter();
const loginFormRef = ref();
const userInfoStore = useUserInfo();

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
const tenantLoading = ref(false);
const tenantOptions = ref<LoginTenantOption[]>([]);
const accountTenantResolvedKey = ref('');
const accountTenantPendingKey = ref('');
let accountTenantPendingPromise: Promise<boolean> | undefined;

const selectedTenant = computed(() => {
  return tenantOptions.value.find((tenant) => tenant.tenantId === form.tenantId);
});

const loadLoginTenants = async () => {
  tenantLoading.value = true;
  try {
    const options = await getLoginTenantOptions();
    tenantOptions.value = Array.isArray(options) ? options : [];
    if (!form.tenantId && tenantOptions.value.length > 0) {
      form.tenantId = tenantOptions.value.find((tenant) => tenant.tenantCode === 'default')?.tenantId
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
    form.tenantId = tenantOptions.value.find((tenant) => tenant.tenantCode === 'default')?.tenantId
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
      realm: 'INTERNAL',
      appCode: 'internal-admin',
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
  void loadLoginTenants();
});

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
        realm: 'INTERNAL',
        actorType: 'INTERNAL_USER',
        partyType: 'INTERNAL_ORG',
        appCode: 'internal-admin',
      };

      // 调用真实登录接口
      const res = await login(loginData);

      // 校验响应数据 - 兼容多种响应格式
      const token = res?.accessToken || res?.token;
      if (!res || !token) {
        throw new Error('登录响应无效');
      }

      // 保存 Token 和用户信息
      const userInfo = res.userInfo || res;
      Session.setToken(token);
      userInfoStore.setUserInfos(userInfo);
      if (res.tenantId) {
        Session.set('tenantId', res.tenantId);
      }

      ElMessage.success('登录成功');
      await router.push('/home');
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
