<template>
  <div class="channel-field-template">
    <div
      v-for="(field, index) in fields"
      :key="index"
      class="channel-field-template__row"
    >
      <el-input v-model="field.name" placeholder="字段名，如 merchantNo" />
      <el-input v-model="field.label" placeholder="显示名，如 商户号" />
      <el-select v-model="field.component" placeholder="控件">
        <el-option
          v-for="item in CHANNEL_FIELD_COMPONENTS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-select v-model="field.dataType" placeholder="数据类型">
        <el-option label="字符串" value="string" />
        <el-option label="数字" value="number" />
        <el-option label="布尔" value="boolean" />
        <el-option label="URL" value="url" />
        <el-option label="日期时间" value="datetime" />
        <el-option label="JSON" value="json" />
        <el-option label="文件 ID" value="fileId" />
      </el-select>
      <el-checkbox v-model="field.required">必填</el-checkbox>
      <el-checkbox v-model="field.sensitive">敏感</el-checkbox>
      <el-checkbox v-model="field.encrypted">加密</el-checkbox>
      <el-checkbox v-model="field.masked">脱敏</el-checkbox>
      <el-button :icon="Delete" circle plain type="danger" @click="removeField(index)" />
      <el-input v-model="field.placeholder" placeholder="占位提示" />
      <el-input v-model="field.description" placeholder="字段说明" />
      <el-input v-model="field.group" placeholder="分组，如 基础资料 / 证书密钥" />
      <el-input-number v-model="field.sort" :min="0" :precision="0" controls-position="right" placeholder="排序" />
      <el-input v-model="field.validationRule" placeholder="校验规则，如 ^[A-Za-z0-9_\\-]+$" />
      <el-input
        :model-value="optionText(field)"
        type="textarea"
        :rows="2"
        placeholder="枚举选项，每行 label=value；非枚举字段可留空"
        @update:model-value="value => updateOptions(field, value)"
      />
      <el-input
        v-model="field.defaultValue"
        placeholder="默认值"
      />
    </div>
    <el-empty v-if="fields.length === 0" description="尚未配置签约字段" :image-size="72" />
    <el-button :icon="Plus" @click="addField">新增字段</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Delete, Plus } from '@element-plus/icons-vue';
import {
  CHANNEL_FIELD_COMPONENTS,
  parseChannelFieldTemplate,
  stringifyChannelFieldTemplate,
  type ChannelFieldOption,
  type ChannelFieldDefinition,
} from './channelFieldTemplate';

const props = defineProps<{
  modelValue?: string;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: string | undefined): void;
}>();

const fields = ref<ChannelFieldDefinition[]>([]);
let initialized = false;
let lastEmittedValue: string | undefined;

watch(
  () => props.modelValue,
  value => {
    if (initialized && value === lastEmittedValue) {
      return;
    }
    fields.value = parseChannelFieldTemplate(value);
    initialized = true;
  },
  { immediate: true },
);

watch(
  fields,
  value => {
    lastEmittedValue = stringifyChannelFieldTemplate(value);
    emit('update:modelValue', lastEmittedValue);
  },
  { deep: true },
);

function addField() {
  fields.value.push({
    name: '',
    label: '',
    component: 'input',
    dataType: 'string',
    required: false,
    sensitive: false,
    encrypted: false,
    masked: false,
    sort: fields.value.length + 1,
  });
}

function removeField(index: number) {
  fields.value = fields.value.filter((_, currentIndex) => currentIndex !== index);
}

function optionText(field: ChannelFieldDefinition) {
  return (field.options || [])
    .map(option => `${option.label}=${option.value}`)
    .join('\n');
}

function updateOptions(field: ChannelFieldDefinition, value: string) {
  const options = value
    .split('\n')
    .map(toOption)
    .filter((item): item is ChannelFieldOption => Boolean(item));
  field.options = options.length ? options : undefined;
}

function toOption(value: string): ChannelFieldOption | undefined {
  const text = value.trim();
  if (!text) return undefined;
  const separatorIndex = text.indexOf('=');
  if (separatorIndex < 0) {
    return { label: text, value: text };
  }
  const label = text.slice(0, separatorIndex).trim();
  const optionValue = text.slice(separatorIndex + 1).trim();
  return label && optionValue ? { label, value: optionValue } : undefined;
}
</script>

<style scoped>
.channel-field-template {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.channel-field-template__row {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(120px, 1fr) minmax(120px, 1fr) 132px 120px repeat(4, 64px) 36px;
  padding: 10px;
}

.channel-field-template__row :deep(.el-textarea),
.channel-field-template__row :deep(.el-input:nth-last-of-type(1)),
.channel-field-template__row :deep(.el-input:nth-last-of-type(2)),
.channel-field-template__row :deep(.el-input:nth-last-of-type(3)),
.channel-field-template__row :deep(.el-input:nth-last-of-type(4)) {
  grid-column: span 2;
}

.channel-field-template__row :deep(.el-checkbox) {
  margin-right: 0;
}

@media (max-width: 768px) {
  .channel-field-template__row {
    grid-template-columns: 1fr;
  }
}
</style>
