<template>
  <div class="channel-config-values">
    <template v-if="fieldGroups.length > 0">
      <section
        v-for="group in fieldGroups"
        :key="group.name"
        class="channel-config-values__group"
      >
        <h4 v-if="group.name !== DEFAULT_GROUP_NAME">{{ group.name }}</h4>
        <el-form-item
          v-for="field in group.fields"
          :key="field.name"
          :label="field.label"
          :required="field.required"
        >
          <el-switch
            v-if="field.component === 'switch'"
            :model-value="Boolean(values[field.name])"
            @update:model-value="value => updateValue(field.name, value)"
          />
          <el-input
            v-else-if="field.component === 'textarea'"
            :model-value="String(values[field.name] || '')"
            type="textarea"
            :rows="3"
            :placeholder="field.placeholder || field.description"
            @update:model-value="value => updateValue(field.name, value)"
          />
          <el-input
            v-else-if="field.component === 'password'"
            :model-value="String(values[field.name] || '')"
            show-password
            :placeholder="field.placeholder || field.description"
            @update:model-value="value => updateValue(field.name, value)"
          />
          <MUpload
            v-else-if="field.component === 'fileId'"
            :model-value="fileValue(field.name)"
            value-type="id"
            display="list"
            purpose="payment-channel-contract"
            access-level="PRIVATE"
            biz-type="payment-channel-contract"
            button-text="上传文件"
            @update:model-value="value => updateFileValue(field.name, value)"
          />
          <el-select
            v-else-if="field.component === 'select'"
            :model-value="String(values[field.name] || '')"
            clearable
            :placeholder="field.placeholder || field.description"
            @update:model-value="value => updateValue(field.name, value)"
          >
            <el-option
              v-for="option in field.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input-number
            v-else-if="field.component === 'number'"
            :model-value="numberValue(field.name)"
            :placeholder="field.placeholder || field.description"
            :controls-position="'right'"
            @update:model-value="value => updateValue(field.name, value ?? undefined)"
          />
          <el-input
            v-else-if="field.component === 'url'"
            :model-value="String(values[field.name] || '')"
            :placeholder="field.placeholder || 'https://'"
            @update:model-value="value => updateValue(field.name, value)"
          />
          <el-date-picker
            v-else-if="field.component === 'datetime'"
            :model-value="dateTimeValue(field.name)"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            :placeholder="field.placeholder || field.description"
            @update:model-value="value => updateValue(field.name, value || undefined)"
          />
          <el-input
            v-else-if="field.component === 'json'"
            :model-value="String(values[field.name] || '')"
            type="textarea"
            :rows="5"
            :placeholder="field.placeholder || '{ }'"
            @update:model-value="value => updateValue(field.name, value)"
          />
          <el-input
            v-else
            :model-value="String(values[field.name] || '')"
            :placeholder="field.placeholder || field.description"
            @update:model-value="value => updateValue(field.name, value)"
          />
          <div v-if="field.description || field.sensitive || field.encrypted" class="channel-config-values__tip">
            <span v-if="field.description">{{ field.description }}</span>
            <span v-if="field.sensitive || field.encrypted">敏感字段保存后脱敏展示</span>
          </div>
        </el-form-item>
      </section>
    </template>
    <el-alert
      v-else
      type="info"
      :closable="false"
      title="当前支付通道未配置签约字段模板"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { ApiId } from '@mango/api-schema';
import { MUpload } from '@mango/file';
import {
  parseChannelConfigValues,
  parseChannelFieldTemplate,
  stringifyChannelConfigValues,
  type ChannelFieldDefinition,
  type ChannelConfigValues,
} from './channelFieldTemplate';
import type { PaymentChannel, PaymentResourceApi } from '../api/payment';

const props = defineProps<{
  modelValue?: string;
  fieldTemplateJson?: string;
  channelId?: ApiId | string;
  channelApi?: PaymentResourceApi<PaymentChannel>;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: string | undefined): void;
}>();

const DEFAULT_GROUP_NAME = '基础配置';
const loadedTemplate = ref<string>();
const fields = computed(() => parseChannelFieldTemplate(props.fieldTemplateJson || loadedTemplate.value)
  .slice()
  .sort((left, right) => (left.sort ?? 1000) - (right.sort ?? 1000)));
const fieldGroups = computed(() => {
  const groups: Array<{ name: string; fields: ChannelFieldDefinition[] }> = [];
  fields.value.forEach((field) => {
    const groupName = field.group || DEFAULT_GROUP_NAME;
    let group = groups.find(item => item.name === groupName);
    if (!group) {
      group = { name: groupName, fields: [] };
      groups.push(group);
    }
    group.fields.push(field);
  });
  return groups;
});
const values = computed(() => parseChannelConfigValues(props.modelValue));

watch(
  () => props.channelId,
  async (channelId) => {
    loadedTemplate.value = undefined;
    if (!channelId || !props.channelApi) {
      return;
    }
    loadedTemplate.value = (await props.channelApi.detail(channelId)).fieldTemplateJson;
  },
  { immediate: true },
);

function updateValue(name: string, value: string | number | boolean | undefined) {
  const next: ChannelConfigValues = {
    ...values.value,
    [name]: value,
  };
  emit('update:modelValue', stringifyChannelConfigValues(next));
}

function fileValue(name: string) {
  const value = values.value[name];
  return value ? String(value) : undefined;
}

function updateFileValue(name: string, value: string | string[] | Record<string, unknown> | Record<string, unknown>[] | null | undefined) {
  if (Array.isArray(value)) {
    updateValue(name, normalizeFileValue(value[0]));
    return;
  }
  updateValue(name, normalizeFileValue(value));
}

function normalizeFileValue(value: string | Record<string, unknown> | null | undefined) {
  if (!value) return undefined;
  if (typeof value === 'string') return value;
  const id = value.id;
  return id ? String(id) : undefined;
}

function numberValue(name: string) {
  const value = values.value[name];
  if (value === undefined || value === '') return undefined;
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
}

function dateTimeValue(name: string) {
  const value = values.value[name];
  return value ? String(value) : undefined;
}
</script>

<style scoped>
.channel-config-values {
  width: 100%;
}

.channel-config-values__group + .channel-config-values__group {
  margin-top: 16px;
}

.channel-config-values__group h4 {
  margin: 0 0 12px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.channel-config-values__tip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  margin-top: 4px;
}
</style>
