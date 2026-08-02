<template>
  <div class="provider-callback-page">
    <el-card class="callback-panel" shadow="never">
      <div v-if="loading" class="callback-state" data-surface="provider-callback-loading">
        <el-icon class="is-loading" :size="30"><Loading /></el-icon>
        <h2>正在完成第三方授权</h2>
        <p>请稍候，不要重复提交。</p>
      </div>

      <div v-else-if="bindingRequired" class="bind-existing" data-surface="provider-bind-existing">
        <h2>绑定已有 Mango 账号</h2>
        <p>{{ providerDisplayName || '第三方账号' }}尚未绑定，请输入现有账号和密码完成绑定。</p>
        <el-form
          ref="bindFormRef"
          :model="bindForm"
          :rules="bindRules"
          label-position="top"
          @keyup.enter="submitBinding"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="bindForm.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="bindForm.password" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <el-button type="primary" :loading="binding" @click="submitBinding"> 确认绑定并登录 </el-button>
          <el-button :disabled="binding" @click="returnToLogin"> 返回登录 </el-button>
        </el-form>
      </div>

      <el-result
        v-else
        icon="error"
        title="授权未完成"
        :sub-title="errorMessage"
        data-surface="provider-callback-error"
      >
        <template #extra>
          <el-button type="primary" @click="returnToOrigin"> 返回 </el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="MangoProviderCallback">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Loading } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { bindExistingProviderAccount, completeProviderAuthorization } from '../api/provider';
import { useMangoLoginFlow } from '../composables/useMangoLoginFlow';

const PROVIDER_RETURN_KEY = 'mango-auth:provider-return';

const route = useRoute();
const router = useRouter();
const loginFlow = useMangoLoginFlow({ autoRedirect: false });
const bindFormRef = ref<FormInstance>();
const loading = ref(true);
const binding = ref(false);
const bindingRequired = ref(false);
const bindingTicket = ref('');
const providerDisplayName = ref('');
const errorMessage = ref('授权参数无效或已过期，请重新发起授权。');
const bindForm = reactive({ username: '', password: '' });
const bindRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

onMounted(() => {
  void completeAuthorization();
});

async function completeAuthorization() {
  const locationQuery = typeof window === 'undefined' ? undefined : new URLSearchParams(window.location.search);
  const state = firstQueryValue(route.query.state) || locationQuery?.get('state')?.trim() || '';
  const code =
    firstQueryValue(route.query.code) ||
    firstQueryValue(route.query.authCode) ||
    locationQuery?.get('code')?.trim() ||
    locationQuery?.get('authCode')?.trim() ||
    '';
  if (!state || !code) {
    loading.value = false;
    return;
  }

  clearCallbackQuery();

  try {
    const result = await completeProviderAuthorization({ state, code });
    providerDisplayName.value = result.providerDisplayName || '';
    if (result.status === 'LOGIN_SUCCESS' && result.login) {
      loginFlow.persistLoginResult(result.login);
      ElMessage.success('登录成功');
      await router.replace(readReturnPath('/home'));
      return;
    }
    if (result.status === 'BIND_SUCCESS') {
      ElMessage.success('第三方账号绑定成功');
      await router.replace(readReturnPath('/profile'));
      return;
    }
    if (result.status === 'BIND_REQUIRED' && result.bindingTicket) {
      bindingTicket.value = result.bindingTicket;
      bindingRequired.value = true;
      return;
    }
    errorMessage.value = '第三方授权返回了无法识别的状态，请重新发起授权。';
  } catch (error) {
    console.error('完成第三方授权失败:', error);
    errorMessage.value = '授权已失效、被重复使用或第三方服务暂时不可用，请重新授权。';
  } finally {
    loading.value = false;
  }
}

async function submitBinding() {
  if (!bindFormRef.value || binding.value) {
    return;
  }
  await bindFormRef.value.validate();
  binding.value = true;
  try {
    const login = await bindExistingProviderAccount({
      bindingTicket: bindingTicket.value,
      username: bindForm.username,
      password: bindForm.password,
    });
    loginFlow.persistLoginResult(login);
    ElMessage.success('绑定并登录成功');
    await router.replace(readReturnPath('/home'));
  } catch (error) {
    console.error('绑定已有账号失败:', error);
  } finally {
    binding.value = false;
  }
}

function returnToLogin() {
  void router.replace('/login');
}

function returnToOrigin() {
  void router.replace(readReturnPath('/login'));
}

function readReturnPath(fallback: string) {
  if (typeof window === 'undefined') {
    return fallback;
  }
  const stored = window.sessionStorage.getItem(PROVIDER_RETURN_KEY) || '';
  window.sessionStorage.removeItem(PROVIDER_RETURN_KEY);
  return stored.startsWith('/') && !stored.startsWith('//') ? stored : fallback;
}

function firstQueryValue(value: unknown) {
  const normalized = Array.isArray(value) ? value[0] : value;
  return typeof normalized === 'string' ? normalized.trim() : '';
}

function clearCallbackQuery() {
  if (typeof window === 'undefined' || !window.location.search) {
    return;
  }
  window.history.replaceState(
    window.history.state,
    document.title,
    `${window.location.pathname}${window.location.hash}`,
  );
}
</script>

<style scoped lang="scss">
.provider-callback-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--el-bg-color-page);
}

.callback-panel {
  width: min(460px, 100%);
}

.callback-state {
  padding: 36px 12px;
  text-align: center;

  h2 {
    margin: 18px 0 8px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
  }
}

.bind-existing {
  padding: 12px;

  h2 {
    margin: 0 0 8px;
    font-size: 20px;
  }

  > p {
    margin: 0 0 24px;
    color: var(--el-text-color-secondary);
    line-height: 1.6;
  }
}
</style>
