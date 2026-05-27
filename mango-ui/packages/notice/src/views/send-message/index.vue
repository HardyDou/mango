<template>
  <div class="notice-send-message-page">
    <div class="page-header">
      <div>
        <h1>发送消息</h1>
        <p>人工触发业务消息发送</p>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px" class="send-form">
        <section class="form-section">
          <div class="section-title">基础信息</div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="业务消息" prop="businessTypeId">
                <el-select
                  v-model="form.businessTypeId"
                  filterable
                  clearable
                  placeholder="请选择业务消息"
                  class="full-width"
                  @change="handleBusinessChange"
                >
                  <el-option
                    v-for="item in businessOptions"
                    :key="item.id"
                    :label="`${item.bizName}（${item.bizType}）`"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="业务单号" prop="bizId">
                <el-input v-model="form.bizId" placeholder="请输入业务单号" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="form.priority" class="full-width">
                  <el-option label="低" value="LOW" />
                  <el-option label="普通" value="NORMAL" />
                  <el-option label="高" value="HIGH" />
                  <el-option label="紧急" value="URGENT" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="幂等键">
                <el-input v-model="form.idempotentKey" placeholder="可选" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="24" v-if="selectedBusiness">
              <el-form-item label="当前配置">
                <div class="business-summary">
                  <el-tag effect="plain">{{ selectedBusiness.bizGroup || '未分类' }}</el-tag>
                  <span>{{ selectedBusiness.bizType }}</span>
                  <span>生效版本：{{ selectedBusiness.activeVersion || '-' }}</span>
                  <span>启用渠道：{{ enabledChannelText(selectedBusiness.enabledChannels) }}</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-title">接收对象</div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="用户ID">
                <el-input v-model="recipient.userId" placeholder="系统消息接收用户" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="接收人">
                <el-input v-model="recipient.recipientName" placeholder="接收人名称" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="手机号">
                <el-input v-model="recipient.mobile" placeholder="短信接收手机号" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="邮箱">
                <el-input v-model="recipient.email" placeholder="邮件接收邮箱" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="微信OpenId">
                <el-input v-model="recipient.wechatOpenid" placeholder="公众号接收人" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="企微用户ID">
                <el-input v-model="recipient.wecomUserId" placeholder="企业微信接收人" clearable />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="钉钉用户ID">
                <el-input v-model="recipient.dingtalkUserId" placeholder="钉钉接收人" clearable />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-title">发送渠道</div>
          <el-form-item label="指定渠道">
            <el-checkbox-group v-model="form.channelTypes">
              <el-checkbox v-for="item in channelOptions" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="section-title">业务参数</div>
          <el-empty v-if="paramFields.length === 0" description="当前业务消息未配置参数" />
          <el-row v-else :gutter="16">
            <el-col v-for="field in paramFields" :key="field.name" :xs="24" :md="12">
              <el-form-item :label="field.label" :required="field.required">
                <el-input-number
                  v-if="field.type === 'number'"
                  v-model="paramValues[field.name]"
                  class="full-width"
                  :placeholder="field.placeholder"
                  controls-position="right"
                />
                <el-switch v-else-if="field.type === 'boolean'" v-model="paramValues[field.name]" />
                <el-date-picker
                  v-else-if="field.type === 'datetime'"
                  v-model="paramValues[field.name]"
                  class="full-width"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择时间"
                />
                <el-input v-else v-model="paramValues[field.name]" :placeholder="field.placeholder" clearable />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <div class="form-actions">
          <el-button @click="resetForm">重置</el-button>
          <el-button type="primary" :loading="sending" @click="submit">发送</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { getBusinessTypes, sendNotice } from '../../api/notice';
import type { NoticeBusinessType, NoticeChannelType, NoticePriority, NoticeRecipientCommand } from '../../types/notice';

type ParamFieldType = 'string' | 'number' | 'boolean' | 'datetime';

interface JsonSchemaProperty {
  type?: string;
  title?: string;
  description?: string;
  format?: string;
  default?: unknown;
  example?: unknown;
}

interface JsonSchema {
  type?: string;
  properties?: Record<string, JsonSchemaProperty>;
  required?: string[];
}

interface ParamField {
  name: string;
  label: string;
  type: ParamFieldType;
  required: boolean;
  placeholder: string;
}

type ParamValue = string | number | boolean | undefined;

const formRef = ref<FormInstance>();
const loading = ref(false);
const sending = ref(false);
const businessTypes = ref<NoticeBusinessType[]>([]);
const paramValues = reactive<Record<string, ParamValue>>({});
const recipient = reactive<NoticeRecipientCommand>({
  userId: '',
  recipientName: '',
  mobile: '',
  email: '',
  wechatOpenid: '',
  wecomUserId: '',
  dingtalkUserId: '',
});
const form = reactive<{
  businessTypeId: string;
  bizId: string;
  priority: NoticePriority;
  idempotentKey: string;
  channelTypes: NoticeChannelType[];
}>({
  businessTypeId: '',
  bizId: '',
  priority: 'NORMAL',
  idempotentKey: '',
  channelTypes: [],
});

const rules: FormRules = {
  businessTypeId: [{ required: true, message: '请选择业务消息', trigger: 'change' }],
};

const channelOptions: Array<{ label: string; value: NoticeChannelType }> = [
  { label: '系统消息', value: 'SITE' },
  { label: '短信', value: 'SMS' },
  { label: '邮件', value: 'EMAIL' },
  { label: '微信公众号', value: 'WECHAT_OFFICIAL' },
  { label: '企业微信', value: 'WECOM' },
  { label: '钉钉', value: 'DINGTALK' },
];

const businessOptions = computed(() => businessTypes.value.filter(item => item.enabled));
const selectedBusiness = computed(() => businessOptions.value.find(item => item.id === form.businessTypeId));
const paramFields = computed(() => parseParamFields(selectedBusiness.value?.paramsSchema));

async function loadBusinessTypes() {
  loading.value = true;
  try {
    const result = await getBusinessTypes({ enabled: true, pageNum: 1, pageSize: 200 });
    businessTypes.value = result.list || [];
  } finally {
    loading.value = false;
  }
}

function handleBusinessChange() {
  Object.keys(paramValues).forEach(key => delete paramValues[key]);
  paramFields.value.forEach((field) => {
    paramValues[field.name] = field.type === 'boolean' ? false : undefined;
  });
  form.channelTypes = [];
}

function parseParamFields(schemaText?: string): ParamField[] {
  const schema = parseJsonSchema(schemaText);
  if (!schema?.properties) {
    return [];
  }
  const required = new Set(schema.required || []);
  return Object.entries(schema.properties).map(([name, property]) => ({
    name,
    label: property.title || property.description || name,
    type: paramFieldType(property),
    required: required.has(name),
    placeholder: property.example === undefined ? `请输入${property.title || name}` : String(property.example),
  }));
}

function parseJsonSchema(schemaText?: string): JsonSchema | undefined {
  if (!schemaText?.trim()) {
    return undefined;
  }
  try {
    const parsed = JSON.parse(schemaText) as unknown;
    if (!isRecord(parsed)) {
      return undefined;
    }
    const properties = isRecord(parsed.properties) ? Object.entries(parsed.properties).reduce<Record<string, JsonSchemaProperty>>((result, [key, value]) => {
      if (isRecord(value)) {
        result[key] = {
          type: typeof value.type === 'string' ? value.type : undefined,
          title: typeof value.title === 'string' ? value.title : undefined,
          description: typeof value.description === 'string' ? value.description : undefined,
          format: typeof value.format === 'string' ? value.format : undefined,
          default: value.default,
          example: value.example,
        };
      }
      return result;
    }, {}) : {};
    return {
      type: typeof parsed.type === 'string' ? parsed.type : undefined,
      properties,
      required: Array.isArray(parsed.required) ? parsed.required.filter(item => typeof item === 'string') : [],
    };
  } catch {
    return undefined;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function paramFieldType(property: JsonSchemaProperty): ParamFieldType {
  if (property.type === 'number' || property.type === 'integer') {
    return 'number';
  }
  if (property.type === 'boolean') {
    return 'boolean';
  }
  if (property.type === 'string' && property.format === 'date-time') {
    return 'datetime';
  }
  return 'string';
}

function buildParams() {
  return paramFields.value.reduce<Record<string, unknown>>((result, field) => {
    const value = paramValues[field.name];
    if (value !== undefined && value !== '') {
      result[field.name] = value;
    }
    return result;
  }, {});
}

function normalizeRecipient(): NoticeRecipientCommand {
  return Object.entries(recipient).reduce<NoticeRecipientCommand>((result, [key, value]) => {
    if (value !== undefined && String(value).trim()) {
      result[key as keyof NoticeRecipientCommand] = String(value).trim();
    }
    return result;
  }, {});
}

function hasRecipient(value: NoticeRecipientCommand) {
  return Boolean(value.userId || value.mobile || value.email || value.wechatOpenid || value.wecomUserId || value.dingtalkUserId || value.externalId);
}

function validateParams() {
  const missing = paramFields.value.find((field) => {
    const value = paramValues[field.name];
    return field.required && (value === undefined || value === '');
  });
  if (missing) {
    ElMessage.error(`请填写${missing.label}`);
    return false;
  }
  return true;
}

async function submit() {
  await formRef.value?.validate();
  if (!selectedBusiness.value) {
    ElMessage.error('请选择业务消息');
    return;
  }
  if (!validateParams()) {
    return;
  }
  const normalizedRecipient = normalizeRecipient();
  if (!hasRecipient(normalizedRecipient)) {
    ElMessage.error('请至少填写一个接收对象');
    return;
  }
  sending.value = true;
  try {
    const result = await sendNotice({
      bizType: selectedBusiness.value.bizType,
      bizId: form.bizId || undefined,
      params: buildParams(),
      recipients: [normalizedRecipient],
      channelTypes: form.channelTypes.length > 0 ? form.channelTypes : undefined,
      priority: form.priority,
      idempotentKey: form.idempotentKey || undefined,
    });
    const hasImmediateResult = result.successCount > 0 || result.failCount > 0;
    ElMessage.success(hasImmediateResult ? `发送已提交，成功 ${result.successCount} 条，失败 ${result.failCount} 条` : '发送任务已提交，可在发送记录查看结果');
  } finally {
    sending.value = false;
  }
}

function resetForm() {
  form.businessTypeId = '';
  form.bizId = '';
  form.priority = 'NORMAL';
  form.idempotentKey = '';
  form.channelTypes = [];
  Object.keys(recipient).forEach((key) => {
    recipient[key as keyof NoticeRecipientCommand] = '';
  });
  Object.keys(paramValues).forEach(key => delete paramValues[key]);
  formRef.value?.clearValidate();
}

function enabledChannelText(value?: string) {
  if (!value?.trim()) {
    return '-';
  }
  return value.split(',').map(item => channelLabel(item.trim())).join(' / ');
}

function channelLabel(value: string) {
  return channelOptions.find(item => item.value === value)?.label || value;
}

onMounted(loadBusinessTypes);
</script>

<style scoped>
.notice-send-message-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.page-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.send-form {
  max-width: 1120px;
}

.form-section {
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.form-section:last-of-type {
  margin-bottom: 0;
}

.section-title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.full-width {
  width: 100%;
}

.business-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: var(--el-text-color-regular);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 18px;
}

@media (max-width: 768px) {
  .send-form {
    max-width: none;
  }
}
</style>
