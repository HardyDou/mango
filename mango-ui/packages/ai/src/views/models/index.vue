<template>
  <MangoListPage class="mango-ai-model-management" data-page="ai.models">
    <template #search>
      <MangoSearchPanel v-if="activeTab === 'models'" :model="query" @search="handleSearch" @reset="handleReset">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="模型名称、标识或平台别名"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" clearable placeholder="全部状态">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <el-row :gutter="16" class="mango-ai-model-management__workspace">
      <el-col :xs="24" :md="8" :lg="7">
        <el-card shadow="never" data-surface="ai.models.providers">
          <template #header
            ><div class="mango-ai-model-management__section-title">
              <span>供应商接入</span
              ><el-button
                v-auth="'ai:model:provider:add'"
                type="primary"
                link
                data-action="ai.models.provider.add"
                @click="openProvider()"
                >新增</el-button
              ><el-button
                v-if="providers.length < providerTypes.length"
                v-auth="'ai:model:provider:add'"
                link
                :loading="saving"
                data-action="ai.models.provider.bootstrap"
                @click="addBuiltinProviders"
                >初始化内置供应商</el-button
              >
            </div></template
          >
          <el-alert v-if="providerError" :title="providerError" type="error" :closable="false" show-icon>
            <template #default>
              <el-button link type="primary" @click="load">重试</el-button>
            </template>
          </el-alert>
          <el-empty v-else-if="!providers.length" description="暂无供应商接入" />
          <el-menu v-else :default-active="selectedProviderId" @select="selectProvider">
            <el-menu-item
              v-for="provider in providers"
              :key="provider.id"
              :index="provider.id"
              data-record-key="ai.models.provider"
            >
              <div class="mango-ai-model-management__provider-item">
                <div>
                  <strong>{{ provider.displayName }}</strong
                  ><small>{{ provider.code }}</small>
                </div>
                <el-tag size="small" :type="provider.enabled ? 'success' : 'info'">{{
                  provider.enabled ? '启用' : '停用'
                }}</el-tag>
              </div>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="16" :lg="17">
        <el-tabs v-model="activeTab" class="mango-ai-model-management__catalog-tabs">
          <el-tab-pane label="模型目录" name="models">
            <MangoListPanel data-surface="ai.models.catalog">
              <template #actions>
                <div class="mango-ai-model-management__provider-summary">
                  <strong>{{ currentProvider?.displayName || '请选择供应商' }}</strong>
                  <small v-if="currentProvider">{{ currentProvider.baseUrl }}</small>
                </div>
                <el-button
                  v-auth="'ai:model:add'"
                  type="primary"
                  plain
                  :disabled="!currentProvider"
                  @click="openModel()"
                >
                  新增模型
                </el-button>
              </template>
              <template #view-actions>
                <el-button
                  v-if="currentProvider"
                  v-auth="'ai:model:provider:edit'"
                  plain
                  @click="openProvider(currentProvider)"
                >
                  接入配置
                </el-button>
                <el-button
                  v-if="currentProvider"
                  v-auth="'ai:model:provider:delete'"
                  plain
                  type="danger"
                  @click="removeProvider(currentProvider)"
                >
                  删除供应商
                </el-button>
              </template>
              <el-alert v-if="modelError" :title="modelError" type="error" :closable="false" show-icon>
                <template #default>
                  <el-button link type="primary" @click="loadModels">重试</el-button>
                </template>
              </el-alert>
              <el-table v-else v-loading="modelsLoading" :data="pageModels" row-key="id" data-surface="ai.models.table"
                ><el-table-column prop="displayName" label="模型名称" min-width="150" /><el-table-column
                  prop="modelName"
                  label="模型标识"
                  min-width="160" /><el-table-column label="能力" min-width="180"
                  ><template #default="{ row }"
                    ><el-tag v-for="item in row.capabilities" :key="item" size="small" effect="plain">{{
                      capabilityName(item)
                    }}</el-tag></template
                  ></el-table-column
                ><el-table-column label="输入 / 输出模态" min-width="180"
                  ><template #default="{ row }"
                    >{{ row.inputModalities.join('、') }} / {{ row.outputModalities.join('、') }}</template
                  ></el-table-column
                ><el-table-column label="调用协议" min-width="150"
                  ><template #default="{ row }">{{ apiProtocolName(row.apiProtocol) }}</template></el-table-column
                ><el-table-column label="状态" width="90"
                  ><template #default="{ row }"
                    ><el-tag :type="row.enabled ? 'success' : 'info'">{{
                      row.enabled ? '启用' : '停用'
                    }}</el-tag></template
                  ></el-table-column
                ><el-table-column label="操作" width="150" fixed="right"
                  ><template #default="{ row }"
                    ><el-button v-auth="'ai:model:edit'" link type="primary" @click="openModel(row)">编辑</el-button
                    ><el-button v-auth="'ai:model:delete'" link type="danger" @click="removeModel(row)"
                      >删除</el-button
                    ></template
                  ></el-table-column
                ><template #empty><el-empty description="暂无模型" /></template
              ></el-table>
              <template #pagination>
                <Pagination
                  v-model:page="query.page"
                  v-model:limit="query.size"
                  :total="filteredModels.length"
                  @pagination="normalizePage"
                />
              </template>
            </MangoListPanel>
          </el-tab-pane>
          <el-tab-pane label="能力路由" name="routes">
            <MangoListPanel data-surface="ai.models.routes">
              <el-alert
                title="路由决定业务能力实际使用的默认模型；只有启用且声明对应能力的模型可被设置。"
                type="info"
                :closable="false"
                show-icon
              />
              <el-table :data="routeRows" class="mango-ai-model-management__routes"
                ><el-table-column prop="capability" label="能力" width="180"
                  ><template #default="{ row }">{{ capabilityName(row.capability) }}</template></el-table-column
                ><el-table-column prop="modelDisplayName" label="当前模型" min-width="180"
                  ><template #default="{ row }"
                    >{{ row.modelDisplayName || '未配置'
                    }}<small v-if="row.providerDisplayName"> · {{ row.providerDisplayName }}</small></template
                  ></el-table-column
                ><el-table-column label="操作" width="150"
                  ><template #default="{ row }"
                    ><el-select
                      v-model="routeSelection[row.capability]"
                      placeholder="选择模型"
                      @change="setRoute(row.capability)"
                      ><el-option
                        v-for="model in routableModels(row.capability)"
                        :key="model.id"
                        :label="model.displayName"
                        :value="model.id" /></el-select></template></el-table-column
              ></el-table>
            </MangoListPanel>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>

    <MangoDialog
      v-model="providerDialog"
      :title="providerForm.id ? '编辑供应商接入' : '新增供应商接入'"
      width="620px"
      :close-on-click-modal="false"
      ><el-form :model="providerForm" label-width="110px"
        ><el-form-item label="接入编码" required
          ><el-input v-model="providerForm.code" maxlength="64" :disabled="Boolean(providerForm.id)" /></el-form-item
        ><el-form-item label="显示名称" required
          ><el-input v-model="providerForm.displayName" maxlength="100" /></el-form-item
        ><el-form-item label="供应商类型" required
          ><el-select
            v-model="providerForm.providerType"
            :disabled="Boolean(providerForm.id)"
            @change="providerTypeChanged"
            ><el-option
              v-for="item in providerTypes"
              :key="item.code"
              :label="item.name"
              :value="item.code" /></el-select></el-form-item
        ><el-form-item label="Base URL" required
          ><el-input v-model="providerForm.baseUrl" maxlength="255" /></el-form-item
        ><el-form-item label="API Key" :required="providerApiKeyRequired"
          ><el-input
            v-model="providerForm.apiKey"
            type="password"
            show-password
            autocomplete="new-password"
            :placeholder="providerForm.id ? '留空保留原密钥' : '请输入密钥'" /></el-form-item
        ><el-form-item label="启用"><el-switch v-model="providerForm.enabled" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="providerDialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveProvider">保存</el-button></template
      ></MangoDialog
    >
    <MangoDialog
      v-model="modelDialog"
      :title="modelForm.id ? '编辑模型' : '新增模型'"
      width="760px"
      :close-on-click-modal="false"
      ><el-form :model="modelForm" label-width="110px"
        ><el-form-item label="模型名称" required
          ><el-input v-model="modelForm.displayName" maxlength="100" /></el-form-item
        ><el-form-item label="模型标识" required
          ><el-input v-model="modelForm.modelName" maxlength="128" /></el-form-item
        ><el-form-item label="平台别名"><el-input v-model="modelForm.platformAlias" maxlength="128" /></el-form-item
        ><el-form-item label="调用协议" required
          ><el-select v-model="modelForm.apiProtocol"
            ><el-option v-for="item in apiProtocolOptions" :key="item.value" :label="item.label" :value="item.value"
          /></el-select>
          <div class="mango-ai-model-management__help">
            必须与供应商模型实际支持的端点一致，运行时不会自动切换协议。
          </div></el-form-item
        ><el-form-item label="能力" required
          ><el-checkbox-group v-model="modelForm.capabilities"
            ><el-checkbox v-for="item in capabilities" :key="item" :value="item">{{
              capabilityName(item)
            }}</el-checkbox></el-checkbox-group
          ></el-form-item
        ><el-row :gutter="16"
          ><el-col :span="12"
            ><el-form-item label="输入模态" required
              ><el-checkbox-group v-model="modelForm.inputModalities"
                ><el-checkbox v-for="item in modalities" :key="item" :value="item">{{
                  item
                }}</el-checkbox></el-checkbox-group
              ></el-form-item
            ></el-col
          ><el-col :span="12"
            ><el-form-item label="输出模态" required
              ><el-checkbox-group v-model="modelForm.outputModalities"
                ><el-checkbox v-for="item in modalities" :key="item" :value="item">{{
                  item
                }}</el-checkbox></el-checkbox-group
              ></el-form-item
            ></el-col
          ></el-row
        ><el-form-item label="参数 JSON"
          ><el-input
            v-model="modelForm.parameterJson"
            type="textarea"
            :rows="3"
            placeholder="供应商特有参数，可留空" /></el-form-item
        ><el-form-item label="启用"><el-switch v-model="modelForm.enabled" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="modelDialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="saveModel">保存</el-button></template
      ></MangoDialog
    >
  </MangoListPage>
</template>

<script setup lang="ts">
import type {
  AiApiProtocol,
  AiCapability,
  AiCapabilityRoute,
  AiModel,
  AiModelManagementApi,
  AiModality,
  AiProviderConnection,
  AiProviderType,
  AiProviderTypeOption,
} from '@mango/ai-api';
import { MangoDialog, MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useAiModelManagementApi } from '../../composables/useAiModelManagementApi';
import { isDialogCancellation, isRequestAborted, requestErrorMessage } from '../../utils/requestError';
defineOptions({ name: 'AiModelsView' });
const api: AiModelManagementApi = useAiModelManagementApi();
const providers = ref<AiProviderConnection[]>([]);
const providerTypes = ref<AiProviderTypeOption[]>([]);
const models = ref<AiModel[]>([]);
const routes = ref<AiCapabilityRoute[]>([]);
const selectedProviderId = ref('');
const providerError = ref('');
const modelError = ref('');
const modelsLoading = ref(false);
const activeTab = ref('models');
const saving = ref(false);
const providerDialog = ref(false);
const modelDialog = ref(false);
let controller: AbortController | undefined;
const query = reactive<{ keyword: string; enabled: boolean | undefined; page: number; size: number }>({
  keyword: '',
  enabled: undefined,
  page: 1,
  size: 20,
});
const criteria = reactive<{ keyword: string; enabled: boolean | undefined }>({ keyword: '', enabled: undefined });
const capabilities: AiCapability[] = [
  'CHAT',
  'EMBEDDING',
  'RERANK',
  'IMAGE_GENERATION',
  'SPEECH_TO_TEXT',
  'TEXT_TO_SPEECH',
  'VIDEO_GENERATION',
];
const modalities: AiModality[] = ['TEXT', 'IMAGE', 'AUDIO', 'VIDEO', 'FILE', 'VECTOR'];
const providerForm = reactive<{
  id?: string;
  code: string;
  displayName: string;
  providerType: AiProviderType;
  baseUrl: string;
  apiKey: string;
  enabled: boolean;
}>({ code: '', displayName: '', providerType: 'DEEPSEEK', baseUrl: '', apiKey: '', enabled: true });
const modelForm = reactive<{
  id?: string;
  providerConnectionId: string;
  modelName: string;
  displayName: string;
  platformAlias: string;
  apiProtocol: AiApiProtocol;
  capabilities: AiCapability[];
  inputModalities: AiModality[];
  outputModalities: AiModality[];
  parameterJson: string;
  enabled: boolean;
}>({
  providerConnectionId: '',
  modelName: '',
  displayName: '',
  platformAlias: '',
  apiProtocol: 'CHAT_COMPLETIONS',
  capabilities: ['CHAT'],
  inputModalities: ['TEXT'],
  outputModalities: ['TEXT'],
  parameterJson: '',
  enabled: true,
});
const currentProvider = computed(() => providers.value.find((item) => item.id === selectedProviderId.value));
const apiProtocolOptions = computed(() => {
  const options: Array<{ label: string; value: AiApiProtocol }> = [
    { label: 'Chat Completions', value: 'CHAT_COMPLETIONS' },
  ];
  if (currentProvider.value?.providerType === 'OPENAI_COMPATIBLE') {
    options.push({ label: 'Responses', value: 'RESPONSES' });
  }
  return options;
});
const providerTypeRequiresKey = computed(() => {
  const option = providerTypes.value.find((item) => item.code === providerForm.providerType);
  return Boolean(option?.apiKeyRequired);
});
const providerApiKeyRequired = computed(() => providerTypeRequiresKey.value && !providerForm.id);
const routeRows = computed(() =>
  capabilities.map(
    (capability) => routes.value.find((item) => item.capability === capability) ?? { capability, modelId: '' },
  ),
);
const filteredModels = computed(() =>
  models.value.filter((item) => {
    const keyword = criteria.keyword;
    const matchesKeyword =
      !keyword ||
      item.displayName.toLowerCase().includes(keyword) ||
      item.modelName.toLowerCase().includes(keyword) ||
      item.platformAlias?.toLowerCase().includes(keyword);
    return matchesKeyword && (criteria.enabled === undefined || item.enabled === criteria.enabled);
  }),
);
const pageModels = computed(() => {
  const start = (query.page - 1) * query.size;
  return filteredModels.value.slice(start, start + query.size);
});
const routeSelection = reactive<Record<string, string>>({});
function normalizePage() {
  query.page = Math.min(query.page, Math.max(1, Math.ceil(filteredModels.value.length / query.size)));
}
function handleSearch() {
  criteria.keyword = query.keyword.trim().toLowerCase();
  criteria.enabled = query.enabled;
  query.page = 1;
}
function handleReset() {
  query.keyword = '';
  query.enabled = undefined;
  criteria.keyword = '';
  criteria.enabled = undefined;
  query.page = 1;
}
function capabilityName(value: AiCapability) {
  return (
    {
      CHAT: '聊天',
      EMBEDDING: '向量',
      RERANK: '排序',
      IMAGE_GENERATION: '图片生成',
      SPEECH_TO_TEXT: '语音识别',
      TEXT_TO_SPEECH: '语音合成',
      VIDEO_GENERATION: '视频生成',
    } as Record<AiCapability, string>
  )[value];
}
function apiProtocolName(value: AiApiProtocol) {
  return value === 'RESPONSES' ? 'Responses' : 'Chat Completions';
}
async function load() {
  controller?.abort();
  controller = new AbortController();
  providerError.value = '';
  try {
    [providers.value, providerTypes.value, routes.value] = await Promise.all([
      api.providers(controller.signal),
      api.providerTypes(controller.signal),
      api.routes(controller.signal),
    ]);
    routes.value.forEach((route) => {
      routeSelection[route.capability] = route.modelId;
    });
    if (!selectedProviderId.value && providers.value[0]) selectedProviderId.value = providers.value[0].id;
    await loadModels();
  } catch (error) {
    if (!isRequestAborted(error)) providerError.value = requestErrorMessage(error, '加载模型管理数据失败');
  }
}
async function loadModels() {
  if (!selectedProviderId.value) {
    models.value = [];
    return;
  }
  modelsLoading.value = true;
  modelError.value = '';
  try {
    models.value = await api.models(selectedProviderId.value, {}, controller?.signal);
    normalizePage();
  } catch (error) {
    if (!isRequestAborted(error)) modelError.value = requestErrorMessage(error, '加载模型目录失败');
  } finally {
    modelsLoading.value = false;
  }
}
function selectProvider(id: string) {
  selectedProviderId.value = id;
  handleReset();
  void loadModels();
}
function openProvider(item?: AiProviderConnection) {
  Object.assign(
    providerForm,
    item
      ? { ...item, apiKey: '' }
      : {
          id: undefined,
          code: '',
          displayName: '',
          providerType: providerTypes.value[0]?.code ?? 'DEEPSEEK',
          baseUrl: providerTypes.value[0]?.defaultBaseUrl ?? '',
          apiKey: '',
          enabled: true,
        },
  );
  if (!item) providerTypeChanged(providerForm.providerType);
  providerDialog.value = true;
}
function providerTypeChanged(type: AiProviderType) {
  const option = providerTypes.value.find((item) => item.code === type);
  if (option && !providerForm.id) {
    providerForm.code = option.defaultCode;
    providerForm.displayName = option.name;
    providerForm.baseUrl = option.defaultBaseUrl;
  }
}
async function addBuiltinProviders() {
  saving.value = true;
  try {
    const existingTypes = new Set(providers.value.map((provider) => provider.providerType));
    for (const option of providerTypes.value.filter((item) => !existingTypes.has(item.code))) {
      await api.createProvider({
        code: option.defaultCode,
        displayName: option.name,
        providerType: option.code,
        baseUrl: option.defaultBaseUrl,
        apiKey: '',
        enabled: false,
      });
    }
    ElMessage.success('已初始化 8 个供应商，请配置密钥后启用');
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '初始化供应商失败'));
    await load();
  } finally {
    saving.value = false;
  }
}
async function saveProvider() {
  if (
    !providerForm.code ||
    !providerForm.displayName ||
    !providerForm.baseUrl ||
    (providerApiKeyRequired.value && !providerForm.apiKey) ||
    (providerForm.enabled &&
      providerTypeRequiresKey.value &&
      providerForm.id &&
      !currentProvider.value?.apiKeyConfigured &&
      !providerForm.apiKey)
  )
    return ElMessage.error('请完整填写供应商接入信息');
  saving.value = true;
  try {
    if (providerForm.id)
      await api.updateProvider({
        id: providerForm.id,
        code: providerForm.code,
        displayName: providerForm.displayName,
        providerType: providerForm.providerType,
        baseUrl: providerForm.baseUrl,
        enabled: providerForm.enabled,
        ...(providerForm.apiKey ? { apiKey: providerForm.apiKey } : {}),
      });
    else await api.createProvider(providerForm);
    ElMessage.success('供应商接入已保存');
    providerDialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '保存供应商接入失败'));
  } finally {
    saving.value = false;
  }
}
async function removeProvider(item: AiProviderConnection) {
  try {
    await ElMessageBox.confirm(`确认删除供应商“${item.displayName}”？`, '删除供应商', { type: 'warning' });
    await api.deleteProvider(item.id);
    selectedProviderId.value = '';
    ElMessage.success('供应商已删除');
    await load();
  } catch (error) {
    if (!isDialogCancellation(error)) ElMessage.error(requestErrorMessage(error, '删除供应商失败'));
  }
}
function openModel(item?: AiModel) {
  Object.assign(
    modelForm,
    item
      ? {
          ...item,
          capabilities: [...item.capabilities],
          inputModalities: [...item.inputModalities],
          outputModalities: [...item.outputModalities],
          parameterJson: item.parameterJson ?? '',
        }
      : {
          id: undefined,
          providerConnectionId: selectedProviderId.value,
          modelName: '',
          displayName: '',
          platformAlias: '',
          apiProtocol: 'CHAT_COMPLETIONS',
          capabilities: ['CHAT'],
          inputModalities: ['TEXT'],
          outputModalities: ['TEXT'],
          parameterJson: '',
          enabled: true,
        },
  );
  modelDialog.value = true;
}
async function saveModel() {
  if (
    !modelForm.displayName ||
    !modelForm.modelName ||
    !modelForm.capabilities.length ||
    !modelForm.inputModalities.length ||
    !modelForm.outputModalities.length
  )
    return ElMessage.error('请完整填写模型目录信息');
  saving.value = true;
  try {
    if (modelForm.id) {
      await api.updateModel({
        id: modelForm.id,
        modelName: modelForm.modelName,
        displayName: modelForm.displayName,
        platformAlias: modelForm.platformAlias,
        apiProtocol: modelForm.apiProtocol,
        capabilities: modelForm.capabilities,
        inputModalities: modelForm.inputModalities,
        outputModalities: modelForm.outputModalities,
        parameterJson: modelForm.parameterJson,
        enabled: modelForm.enabled,
      });
    } else {
      await api.createModel(modelForm);
    }
    ElMessage.success('模型已保存');
    modelDialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '保存模型失败'));
  } finally {
    saving.value = false;
  }
}
async function removeModel(item: AiModel) {
  try {
    await ElMessageBox.confirm(`确认删除模型“${item.displayName}”？`, '删除模型', { type: 'warning' });
    await api.deleteModel(item.id);
    ElMessage.success('模型已删除');
    await load();
  } catch (error) {
    if (!isDialogCancellation(error)) ElMessage.error(requestErrorMessage(error, '删除模型失败'));
  }
}
function routableModels(capability: AiCapability) {
  return models.value.filter((item) => item.enabled && item.capabilities.includes(capability));
}
async function setRoute(capability: AiCapability) {
  const modelId = routeSelection[capability];
  if (!modelId) return;
  try {
    await api.setRoute({ capability, modelId });
    ElMessage.success('能力路由已更新');
    routes.value = await api.routes(controller?.signal);
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '更新能力路由失败'));
  }
}
onMounted(() => {
  void load();
});
onBeforeUnmount(() => controller?.abort());
</script>
