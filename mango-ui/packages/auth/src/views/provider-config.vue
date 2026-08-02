<template>
  <div class="provider-config-page" data-surface="provider-config">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <h2>第三方登录配置</h2>
          <p>配置当前租户、指定应用可使用的企业微信和钉钉登录。</p>
        </div>
        <div class="app-filter">
          <el-input v-model="appCode" placeholder="应用编码，如 internal-admin" clearable @keyup.enter="loadConfigs" />
          <el-button v-auth="'auth:provider-config:view'" type="primary" :loading="loading" @click="loadConfigs">
            查询
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="loadFailed"
        type="error"
        title="配置加载失败，请检查权限或稍后重试。"
        :closable="false"
        show-icon
      />

      <el-table v-loading="loading" :data="providerRows" empty-text="暂无配置">
        <el-table-column label="登录方式" min-width="140">
          <template #default="{ row }">
            <span :data-provider="row.provider">{{ providerName(row.provider) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="appCode" label="应用编码" min-width="160" />
        <el-table-column label="配置状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.complete ? 'success' : 'warning'" size="small">
              {{ row.complete ? '完整' : '待完善' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密钥" width="110">
          <template #default="{ row }">
            {{ row.secretConfigured ? '已配置' : '未配置' }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ row.updatedAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="'auth:provider-config:edit'" link type="primary" @click="openEditor(row.provider, row)">
              配置
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="`${providerName(form.provider)}配置`" width="620px">
      <el-form ref="formRef" :model="form" label-width="116px">
        <el-form-item label="应用编码" required>
          <el-input v-model="form.appCode" disabled />
        </el-form-item>

        <template v-if="form.provider === 'WECOM'">
          <el-form-item label="企业 ID" required>
            <el-input v-model="form.providerTenantId" placeholder="企业微信 CorpId" />
          </el-form-item>
          <el-form-item label="AgentId" required>
            <el-input v-model="form.agentId" placeholder="自建应用 AgentId" />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="ClientId" required>
            <el-input v-model="form.clientId" placeholder="钉钉应用 ClientId" />
          </el-form-item>
          <el-form-item label="组织标识">
            <el-input v-model="form.providerTenantId" placeholder="可选，用于区分钉钉组织" />
          </el-form-item>
        </template>

        <el-form-item label="Secret" :required="!form.secretConfigured">
          <el-input
            v-model="form.secret"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="form.secretConfigured ? '已配置，留空表示不修改' : '请输入应用 Secret'"
          />
        </el-form-item>
        <el-form-item label="回调地址" required>
          <el-input v-model="form.redirectUrisText" type="textarea" :rows="4" placeholder="每行一个完整回调地址" />
          <div class="field-hint">登录和个人中心绑定发起授权时使用的地址必须在此列表中。</div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MangoProviderConfig">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Session } from '@mango/common/utils/storage';
import { listProviderConfigs, saveProviderConfig, type ProviderConfig } from '../api/provider';
import type { ExternalAuthProvider } from '../api/identity';

interface ProviderConfigForm {
  id?: string | number;
  appCode: string;
  provider: ExternalAuthProvider;
  clientId: string;
  providerTenantId: string;
  agentId: string;
  secret: string;
  secretConfigured: boolean;
  redirectUrisText: string;
  enabled: boolean;
}

const sessionUser = Session.get('userInfo') || {};
const appCode = ref(String(sessionUser.appCode || 'internal-admin'));
const loading = ref(false);
const saving = ref(false);
const loadFailed = ref(false);
const dialogVisible = ref(false);
const configs = ref<ProviderConfig[]>([]);
const form = reactive<ProviderConfigForm>(emptyForm('WECOM'));
const providerRows = computed(() =>
  (['WECOM', 'DINGTALK'] as ExternalAuthProvider[]).map((provider) => {
    const saved = configs.value.find((item) => item.provider === provider);
    return (
      saved || {
        appCode: appCode.value.trim(),
        provider,
        redirectUris: [],
        enabled: false,
        secretConfigured: false,
        complete: false,
      }
    );
  }),
);

onMounted(() => {
  void loadConfigs();
});

async function loadConfigs() {
  const normalizedAppCode = appCode.value.trim();
  if (!normalizedAppCode) {
    ElMessage.warning('请输入应用编码');
    return;
  }
  loading.value = true;
  loadFailed.value = false;
  try {
    configs.value = await listProviderConfigs(normalizedAppCode);
  } catch (error) {
    console.error('加载第三方登录配置失败:', error);
    configs.value = [];
    loadFailed.value = true;
  } finally {
    loading.value = false;
  }
}

function openEditor(provider: ExternalAuthProvider, config?: ProviderConfig) {
  Object.assign(form, emptyForm(provider), config || {}, {
    appCode: appCode.value.trim(),
    secret: '',
    secretConfigured: Boolean(config?.secretConfigured),
    redirectUrisText: (config?.redirectUris || []).join('\n'),
  });
  dialogVisible.value = true;
}

async function saveConfig() {
  const redirectUris = form.redirectUrisText
    .split('\n')
    .map((value) => value.trim())
    .filter(Boolean);
  const missingProviderFields =
    form.provider === 'WECOM' ? !form.providerTenantId.trim() || !form.agentId.trim() : !form.clientId.trim();
  if (missingProviderFields || redirectUris.length === 0 || (!form.secretConfigured && !form.secret.trim())) {
    ElMessage.warning('请填写完整的登录配置');
    return;
  }
  if (redirectUris.some((uri) => !/^https?:\/\//.test(uri))) {
    ElMessage.warning('回调地址必须是完整的 http 或 https 地址');
    return;
  }

  saving.value = true;
  try {
    await saveProviderConfig({
      id: form.id,
      appCode: form.appCode,
      provider: form.provider,
      clientId: form.clientId.trim() || undefined,
      providerTenantId: form.providerTenantId.trim() || undefined,
      agentId: form.agentId.trim() || undefined,
      secret: form.secret.trim() || undefined,
      redirectUris,
      enabled: form.enabled,
    });
    ElMessage.success('配置已保存');
    dialogVisible.value = false;
    await loadConfigs();
  } catch (error) {
    console.error('保存第三方登录配置失败:', error);
  } finally {
    saving.value = false;
  }
}

function emptyForm(provider: ExternalAuthProvider): ProviderConfigForm {
  return {
    appCode: appCode?.value?.trim?.() || 'internal-admin',
    provider,
    clientId: '',
    providerTenantId: '',
    agentId: '',
    secret: '',
    secretConfigured: false,
    redirectUrisText: '',
    enabled: false,
  };
}

function providerName(provider: ExternalAuthProvider) {
  return provider === 'WECOM' ? '企业微信' : '钉钉';
}
</script>

<style scoped lang="scss">
.provider-config-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;

  h2 {
    margin: 0 0 6px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
  }
}

.app-filter {
  display: flex;
  gap: 8px;
  width: min(440px, 100%);
}

.field-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (width <= 768px) {
  .page-header {
    flex-direction: column;
  }

  .app-filter {
    width: 100%;
  }
}
</style>
