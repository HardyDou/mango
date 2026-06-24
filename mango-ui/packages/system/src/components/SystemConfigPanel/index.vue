<template>
  <div class="mango-system-config-panel">
    <div class="mango-system-config-panel__bar">
      <el-tabs
        v-model="activeDomain"
        class="mango-system-config-panel__tabs"
        @tab-change="handleDomainChange"
      >
        <el-tab-pane
          v-for="domain in normalizedDomains"
          :key="domain"
          :label="domainLabel(domain)"
          :name="domain"
        />
      </el-tabs>
      <el-button
        v-if="showRefresh"
        class="mango-system-config-panel__refresh"
        :icon="Refresh"
        :loading="currentState.loading"
        @click="loadDomain(activeDomain, true)"
      >
        刷新
      </el-button>
    </div>

    <el-alert
      v-if="!normalizedDomains.length"
      title="请传入需要管理的业务域"
      type="warning"
      show-icon
      :closable="false"
    />

    <el-skeleton
      v-else-if="currentState.loading"
      :rows="5"
      animated
    />

    <el-result
      v-else-if="currentState.error"
      icon="error"
      title="系统配置加载失败，请重试"
      :sub-title="currentState.error"
    >
      <template #extra>
        <el-button
          type="primary"
          @click="loadDomain(activeDomain, true)"
        >
          重试
        </el-button>
      </template>
    </el-result>

    <el-empty
      v-else-if="!visibleConfigs.length"
      description="当前业务域暂无系统配置"
    >
      <el-button @click="loadDomain(activeDomain, true)">
        刷新
      </el-button>
    </el-empty>

    <div
      v-else
      class="mango-system-config-panel__grid"
    >
      <el-card
        v-for="config in visibleConfigs"
        :key="config.id || config.configKey"
        class="mango-system-config-panel__card"
        shadow="never"
      >
        <template #header>
          <div class="mango-system-config-panel__card-head">
            <div class="mango-system-config-panel__heading">
              <div class="mango-system-config-panel__title">
                <span>{{ config.configName || config.configKey }}</span>
                <el-tag
                  v-if="!isEditable(config)"
                  :type="statusTagType(config)"
                  effect="plain"
                  size="small"
                >
                  {{ statusText(config) }}
                </el-tag>
                <el-tag
                  v-else-if="savingKeys[configKey(config)]"
                  type="primary"
                  effect="plain"
                  size="small"
                >
                  保存中
                </el-tag>
              </div>
              <div class="mango-system-config-panel__subline">
                <span>{{ config.groupName || config.groupCode || '默认分组' }}</span>
                <span>{{ valueTypeLabel(config.valueType) }}</span>
              </div>
            </div>
            <el-button
              :icon="View"
              link
              type="primary"
              @click="openDetail(config)"
            >
              详情
            </el-button>
          </div>
        </template>

        <div class="mango-system-config-panel__intro">
          {{ config.description || config.remark || '未填写配置介绍' }}
        </div>

        <div class="mango-system-config-panel__control">
          <el-radio-group
            v-if="resolveValueType(config) === 'RADIO'"
            v-model="draftValues[configKey(config)]"
            class="mango-system-config-panel__radio"
            :disabled="!isEditable(config) || savingKeys[configKey(config)]"
            @change="handleInlineChange(config)"
          >
            <el-radio-button
              v-for="option in optionList(config)"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
          <component
            :is="controlComponent(config)"
            v-else
            v-model="draftValues[configKey(config)]"
            v-bind="controlProps(config)"
            :disabled="!isEditable(config) || savingKeys[configKey(config)]"
            @change="handleInlineChange(config)"
          >
            <el-option
              v-for="option in optionList(config)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </component>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="detailVisible"
      :title="detailDialogTitle"
      width="760px"
      class="mango-system-config-panel__dialog"
      :close-on-click-modal="false"
    >
      <template v-if="detailConfig">
        <div class="mango-system-config-panel__dialog-hero">
          <div>
            <div class="mango-system-config-panel__dialog-title">
              {{ detailConfig.configName || detailConfig.configKey }}
            </div>
            <div class="mango-system-config-panel__dialog-desc">
              {{ detailConfig.description || detailConfig.remark || '未填写配置介绍' }}
            </div>
          </div>
          <el-tag
            :type="statusTagType(detailConfig)"
            effect="plain"
          >
            {{ statusText(detailConfig) }}
          </el-tag>
        </div>

        <div class="mango-system-config-panel__dialog-grid">
          <div class="mango-system-config-panel__fact">
            <span>业务域</span>
            <strong>{{ domainLabel(detailConfig.domainCode || activeDomain) }}</strong>
          </div>
          <div class="mango-system-config-panel__fact">
            <span>配置类型</span>
            <strong>{{ valueTypeLabel(detailConfig.valueType) }}</strong>
          </div>
          <div class="mango-system-config-panel__fact">
            <span>选项来源</span>
            <strong>{{ optionSourceText(detailConfig) }}</strong>
          </div>
          <div class="mango-system-config-panel__fact">
            <span>默认值</span>
            <strong>{{ displayDefaultValue(detailConfig) }}</strong>
          </div>
        </div>

        <el-form
          class="mango-system-config-panel__detail-form"
          label-width="88px"
        >
          <el-form-item label="当前值">
            <el-radio-group
              v-if="resolveValueType(detailConfig) === 'RADIO'"
              v-model="detailValue"
              class="mango-system-config-panel__radio"
              :disabled="!isEditable(detailConfig) || detailSaving"
            >
              <el-radio-button
                v-for="option in optionList(detailConfig)"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </el-radio-button>
            </el-radio-group>
            <component
              :is="controlComponent(detailConfig)"
              v-else
              v-model="detailValue"
              v-bind="controlProps(detailConfig)"
              :disabled="!isEditable(detailConfig) || detailSaving"
            >
              <el-option
                v-for="option in optionList(detailConfig)"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </component>
          </el-form-item>

          <el-form-item
            v-if="optionList(detailConfig).length > 0"
            label="可选值"
          >
            <div class="mango-system-config-panel__options">
              <el-tag
                v-for="option in optionList(detailConfig)"
                :key="option.value"
                effect="plain"
                size="small"
              >
                {{ option.label }}
              </el-tag>
            </div>
          </el-form-item>

          <el-form-item label="配置键">
            <el-text
              class="mango-system-config-panel__code"
              truncated
            >
              {{ detailConfig.configKey }}
            </el-text>
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
        <el-button
          type="primary"
          :disabled="!detailConfig || !isEditable(detailConfig)"
          :loading="detailSaving"
          @click="saveDetail"
        >
          保存当前值
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue';
import { Refresh, View } from '@element-plus/icons-vue';
import { ElInput, ElInputNumber, ElDatePicker, ElSelect, ElSwitch, ElMessage } from 'element-plus';
import { configApi, type ConfigValueType, type SysConfig } from '../../api/config';
import { dictDataApi } from '../../api/dict';

interface ConfigOption {
  label: string;
  value: string;
}

interface DomainState {
  loading: boolean;
  loaded: boolean;
  error: string;
  configs: SysConfig[];
}

const props = withDefaults(defineProps<{
  domainCodes: string[];
  domainLabels?: Record<string, string>;
  keyword?: string;
  readonly?: boolean;
  showRefresh?: boolean;
  typeFilter?: ConfigValueType[];
}>(), {
  domainLabels: () => ({}),
  keyword: '',
  readonly: true,
  showRefresh: true,
  typeFilter: () => [],
});

const emit = defineEmits<{
  loaded: [domainCode: string, configs: SysConfig[]];
  update: [config: SysConfig];
}>();

const activeDomain = ref('');
const domainStates = reactive<Record<string, DomainState>>({});
const draftValues = reactive<Record<string, string | number | boolean | string[] | null>>({});
const savingKeys = reactive<Record<string, boolean>>({});
const dictOptions = reactive<Record<string, ConfigOption[]>>({});
const dictLoading = reactive<Record<string, boolean>>({});
const detailVisible = ref(false);
const detailConfig = ref<SysConfig>();
const detailValue = ref<string | number | boolean | string[] | null>(null);
const detailSaving = ref(false);

const normalizedDomains = computed(() => {
  return Array.from(new Set(props.domainCodes.map((item) => item.trim()).filter(Boolean)));
});

const currentState = computed(() => ensureState(activeDomain.value));

const visibleConfigs = computed(() => {
  const text = props.keyword.trim().toLowerCase();
  return currentState.value.configs.filter((config) => {
    const typeMatched = !props.typeFilter.length || props.typeFilter.includes(resolveValueType(config));
    const keywordMatched = !text
      || [
        config.configKey,
        config.configName,
        config.description,
        config.remark,
      ].some((value) => String(value || '').toLowerCase().includes(text));
    return typeMatched && keywordMatched;
  });
});

const detailDialogTitle = computed(() => {
  return detailConfig.value ? `配置操作：${detailConfig.value.configName || detailConfig.value.configKey}` : '配置操作';
});

watch(
  normalizedDomains,
  (domains) => {
    activeDomain.value = domains[0] || '';
    if (activeDomain.value) {
      loadDomain(activeDomain.value);
    }
  },
  { immediate: true },
);

function ensureState(domainCode: string): DomainState {
  if (!domainStates[domainCode]) {
    domainStates[domainCode] = {
      loading: false,
      loaded: false,
      error: '',
      configs: [],
    };
  }
  return domainStates[domainCode];
}

async function handleDomainChange(name: string | number) {
  const domainCode = String(name);
  const state = ensureState(domainCode);
  if (!state.loaded) {
    await loadDomain(domainCode);
  }
}

async function loadDomain(domainCode: string, force = false) {
  if (!domainCode) {
    return;
  }
  const state = ensureState(domainCode);
  if (state.loading || (state.loaded && !force)) {
    return;
  }
  state.loading = true;
  state.error = '';
  try {
    const result = await configApi.list({ domainCode });
    state.configs = result.list || [];
    state.loaded = true;
    state.configs.forEach((config) => {
      draftValues[configKey(config)] = toModelValue(config);
    });
    await preloadDictOptions(state.configs);
    emit('loaded', domainCode, state.configs);
  } catch (error) {
    state.error = error instanceof Error ? error.message : '加载失败';
  } finally {
    state.loading = false;
  }
}

function domainLabel(domainCode: string) {
  return props.domainLabels[domainCode] || domainCode;
}

function configKey(config: SysConfig) {
  return String(config.id || config.configKey);
}

function resolveValueType(config: SysConfig): ConfigValueType {
  return config.valueType || 'STRING';
}

function isEditable(config: SysConfig) {
  return !props.readonly && config.status !== 0 && config.editable !== false;
}

function controlComponent(config: SysConfig) {
  const type = resolveValueType(config);
  if (type === 'BOOLEAN') {
    return ElSwitch;
  }
  if (type === 'NUMBER') {
    return ElInputNumber;
  }
  if (type === 'SELECT') {
    return ElSelect;
  }
  if (type === 'MULTI_SELECT') {
    return ElSelect;
  }
  if (type === 'DATE' || type === 'DATE_RANGE') {
    return ElDatePicker;
  }
  return ElInput;
}

function controlProps(config: SysConfig) {
  const type = resolveValueType(config);
  if (type === 'BOOLEAN') {
    return {
      activeValue: true,
      inactiveValue: false,
      activeText: '开启',
      inactiveText: '关闭',
      inlinePrompt: true,
      class: 'mango-system-config-panel__switch',
    };
  }
  if (type === 'NUMBER') {
    return {
      controlsPosition: 'right',
      min: 0,
      class: 'mango-system-config-panel__input',
    };
  }
  if (type === 'SELECT' || type === 'MULTI_SELECT') {
    return {
      placeholder: optionList(config).length ? '请选择' : '缺少可选项',
      multiple: type === 'MULTI_SELECT',
      collapseTags: type === 'MULTI_SELECT',
      collapseTagsTooltip: type === 'MULTI_SELECT',
      filterable: true,
      class: 'mango-system-config-panel__input',
    };
  }
  if (type === 'DATE') {
    return {
      type: 'date',
      valueFormat: 'YYYY-MM-DD',
      placeholder: '请选择日期',
      class: 'mango-system-config-panel__input',
    };
  }
  if (type === 'DATE_RANGE') {
    return {
      type: 'daterange',
      valueFormat: 'YYYY-MM-DD',
      startPlaceholder: '开始日期',
      endPlaceholder: '结束日期',
      class: 'mango-system-config-panel__input',
    };
  }
  return {
    type: 'textarea',
    autosize: { minRows: 2, maxRows: 4 },
    placeholder: '请输入配置值',
    class: 'mango-system-config-panel__textarea',
  };
}

function optionList(config: SysConfig): ConfigOption[] {
  if (config.optionSource === 'DICT' && config.dictType) {
    return dictOptions[config.dictType] || [];
  }
  if (!config.options) {
    return [];
  }
  try {
    const parsed = JSON.parse(config.options) as Array<Partial<ConfigOption>>;
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
      .filter((item) => item.value !== undefined)
      .map((item) => ({
        label: item.label || String(item.value),
        value: String(item.value),
      }));
  } catch {
    return [];
  }
}

async function preloadDictOptions(configs: SysConfig[]) {
  const dictTypes = Array.from(new Set(configs
    .filter((config) => config.optionSource === 'DICT' && config.dictType)
    .map((config) => String(config.dictType))));
  await Promise.all(dictTypes.map(loadDictOptions));
}

async function loadDictOptions(dictType: string) {
  if (!dictType || dictOptions[dictType] || dictLoading[dictType]) {
    return;
  }
  dictLoading[dictType] = true;
  try {
    const list = await dictDataApi.options(dictType);
    dictOptions[dictType] = list
      .filter((item) => item.status !== 0)
      .map((item) => ({
        label: item.label,
        value: item.value,
      }));
  } finally {
    dictLoading[dictType] = false;
  }
}

function toModelValue(config: SysConfig): string | number | boolean | string[] | null {
  const value = config.configValue ?? config.defaultValue ?? '';
  if (resolveValueType(config) === 'BOOLEAN') {
    return value === true || value === 'true' || value === '1';
  }
  if (resolveValueType(config) === 'NUMBER') {
    const numericValue = Number(value);
    return Number.isNaN(numericValue) ? 0 : numericValue;
  }
  if (resolveValueType(config) === 'DATE_RANGE' || resolveValueType(config) === 'MULTI_SELECT') {
    try {
      const parsed = JSON.parse(String(value));
      return Array.isArray(parsed) ? parsed.map(String) : [];
    } catch {
      return [];
    }
  }
  return value;
}

function toStorageValue(config: SysConfig, value: string | number | boolean | string[] | null): string {
  if (resolveValueType(config) === 'DATE_RANGE' || resolveValueType(config) === 'MULTI_SELECT') {
    return JSON.stringify(Array.isArray(value) ? value : []);
  }
  if (resolveValueType(config) === 'BOOLEAN') {
    return value ? 'true' : 'false';
  }
  return value === null || value === undefined ? '' : String(value);
}

function statusText(config: SysConfig) {
  if (config.status === 0) {
    return '禁用';
  }
  if (!isEditable(config)) {
    return props.readonly ? '只读' : (config.editableReason || '不可编辑');
  }
  return '可改';
}

function statusTagType(config: SysConfig) {
  if (config.status === 0) {
    return 'info';
  }
  return isEditable(config) ? 'success' : 'warning';
}

function optionSourceText(config: SysConfig) {
  if (resolveValueType(config) === 'BOOLEAN') {
    return '开关值';
  }
  if (config.optionSource === 'DICT') {
    return config.dictType ? `字典 ${config.dictType}` : '字典未绑定';
  }
  return optionList(config).length > 0 ? '自定义选项' : '无固定选项';
}

function displayDefaultValue(config: SysConfig) {
  return displayConfigValue(config, config.defaultValue);
}

function displayConfigValue(config: SysConfig, rawValue?: string) {
  if (rawValue === undefined || rawValue === null || rawValue === '') {
    return '-';
  }
  const type = resolveValueType(config);
  if (type === 'BOOLEAN') {
    return rawValue === 'true' || rawValue === '1' ? '开启' : '关闭';
  }
  if (type === 'DATE_RANGE' || type === 'MULTI_SELECT') {
    try {
      const parsed = JSON.parse(String(rawValue));
      if (!Array.isArray(parsed) || parsed.length === 0) {
        return '-';
      }
      if (type === 'DATE_RANGE') {
        return parsed.map(String).join(' 至 ');
      }
      const options = optionList(config);
      return parsed
        .map((value) => labelByValue(options, String(value)))
        .join('、');
    } catch {
      return String(rawValue);
    }
  }
  if (type === 'RADIO' || type === 'SELECT') {
    return labelByValue(optionList(config), String(rawValue));
  }
  return String(rawValue);
}

function labelByValue(options: ConfigOption[], value: string) {
  return options.find((option) => option.value === value)?.label || value;
}

async function handleInlineChange(config: SysConfig) {
  await nextTick();
  await saveConfigValue(config, draftValues[configKey(config)]);
}

async function saveConfigValue(config: SysConfig, value: string | number | boolean | string[] | null) {
  if (!config.id) {
    ElMessage.error('配置ID不能为空');
    return false;
  }
  if (!isEditable(config)) {
    ElMessage.warning(config.editableReason || '此配置不可编辑');
    draftValues[configKey(config)] = toModelValue(config);
    return false;
  }
  const key = configKey(config);
  const oldValue = config.configValue;
  savingKeys[key] = true;
  try {
    const storageValue = toStorageValue(config, value);
    await configApi.updateValue(config.id, storageValue);
    config.configValue = storageValue;
    draftValues[key] = toModelValue(config);
    emit('update', config);
    ElMessage.success('保存成功');
    return true;
  } catch {
    config.configValue = oldValue;
    draftValues[key] = toModelValue(config);
    ElMessage.error('保存失败');
    return false;
  } finally {
    savingKeys[key] = false;
  }
}

function openDetail(config: SysConfig) {
  detailConfig.value = config;
  detailValue.value = toModelValue(config);
  detailVisible.value = true;
}

async function saveDetail() {
  if (!detailConfig.value) {
    return;
  }
  detailSaving.value = true;
  try {
    const saved = await saveConfigValue(detailConfig.value, detailValue.value);
    if (saved) {
      detailVisible.value = false;
    }
  } finally {
    detailSaving.value = false;
  }
}

function valueTypeLabel(type?: ConfigValueType) {
  const labels: Record<ConfigValueType, string> = {
    BOOLEAN: '开关',
    STRING: '文本',
    NUMBER: '数字',
    RADIO: '单选',
    SELECT: '下拉',
    MULTI_SELECT: '多选',
    DATE: '日期',
    DATE_RANGE: '日期区间',
  };
  return labels[type || 'STRING'];
}
</script>

<style scoped lang="scss">
.mango-system-config-panel {
  width: 100%;
}

.mango-system-config-panel__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.mango-system-config-panel__refresh {
  flex: none;
}

.mango-system-config-panel__tabs {
  min-width: 0;
  flex: 1;
}

.mango-system-config-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.mango-system-config-panel__card {
  border-radius: 8px;

  :deep(.el-card__header) {
    padding: 14px 16px 10px;
  }

  :deep(.el-card__body) {
    display: flex;
    min-height: 156px;
    flex-direction: column;
    padding: 12px 16px 14px;
  }
}

.mango-system-config-panel__card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.mango-system-config-panel__heading {
  min-width: 0;
}

.mango-system-config-panel__title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  font-weight: 600;

  span {
    min-width: 0;
    overflow: hidden;
    flex: 0 1 auto;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.el-tag) {
    flex: none;
  }
}

.mango-system-config-panel__subline {
  display: flex;
  gap: 8px;
  margin-top: 5px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  line-height: 18px;

  span + span::before {
    margin-right: 8px;
    color: var(--el-border-color);
    content: '/';
  }
}

.mango-system-config-panel__intro {
  display: -webkit-box;
  min-height: 40px;
  margin-bottom: 12px;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  line-height: 22px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.mango-system-config-panel__control {
  display: flex;
  align-items: center;
  min-height: 44px;
  margin-top: 0;
  padding-top: 2px;
}

.mango-system-config-panel__input {
  width: 100%;
}

.mango-system-config-panel__textarea {
  width: 100%;

  :deep(.el-textarea__inner) {
    min-height: 52px !important;
  }
}

.mango-system-config-panel__switch {
  min-width: 96px;
}

.mango-system-config-panel__radio {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;

  :deep(.el-radio-button__inner) {
    border-left: var(--el-border);
    border-radius: 6px;
    min-width: 54px;
  }
}

.mango-system-config-panel__detail-form {
  margin-top: 20px;

  .mango-system-config-panel__input,
  .mango-system-config-panel__textarea {
    max-width: 520px;
  }
}

.mango-system-config-panel__dialog-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.mango-system-config-panel__dialog-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 24px;
}

.mango-system-config-panel__dialog-desc {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  line-height: 22px;
}

.mango-system-config-panel__dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.mango-system-config-panel__fact {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;

  span {
    display: block;
    margin-bottom: 6px;
    color: var(--el-text-color-placeholder);
    font-size: 12px;
  }

  strong {
    display: block;
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.mango-system-config-panel__options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mango-system-config-panel__code {
  max-width: 520px;
}

@media (max-width: 768px) {
  .mango-system-config-panel__bar {
    align-items: stretch;
    flex-direction: column;
  }

  .mango-system-config-panel__grid,
  .mango-system-config-panel__dialog-grid {
    grid-template-columns: 1fr;
  }

  .mango-system-config-panel__dialog-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
