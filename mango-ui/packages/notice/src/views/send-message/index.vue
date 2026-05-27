<template>
  <div class="notice-send-message-page">
    <div class="page-header">
      <h1>发送消息</h1>
      <div class="page-actions">
        <el-button :loading="recordLoading" @click="loadRecords">刷新</el-button>
        <el-button type="primary" @click="openSendDialog">发送消息</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="records" border stripe v-loading="recordLoading">
        <el-table-column prop="taskId" label="任务ID" width="120" show-overflow-tooltip />
        <el-table-column prop="bizType" label="消息类型" min-width="180" show-overflow-tooltip />
        <el-table-column label="渠道" width="120">
          <template #default="{ row }">{{ channelLabel(row.channelType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="renderedTitle" label="发送标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="renderedContent" label="发送内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="recipientId" label="接收人" width="120" show-overflow-tooltip />
        <el-table-column prop="requestId" label="请求流水号" width="180" show-overflow-tooltip />
        <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sentAt" label="发送时间" width="170" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-dialog
      v-model="sendDialogVisible"
      title="发送消息"
      width="720px"
      destroy-on-close
      class="send-message-dialog"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px" class="send-form">
        <section class="form-section">
          <div class="section-title">发送给</div>
          <el-form-item label="接收用户" prop="userIds">
            <el-select
              v-model="form.userIds"
              multiple
              filterable
              remote
              reserve-keyword
              collapse-tags
              collapse-tags-tooltip
              class="full-width"
              placeholder="请输入用户名、姓名、手机号或邮箱搜索"
              :remote-method="searchUsers"
              :loading="userLoading"
            >
              <el-option
                v-for="user in userOptions"
                :key="user.userId"
                :label="userLabel(user)"
                :value="String(user.userId)"
              >
                <div class="user-option">
                  <span>{{ user.nickname || user.username }}</span>
                  <span>{{ user.username }}</span>
                  <span>{{ user.phone || '-' }}</span>
                  <span>{{ user.email || '-' }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="section-title">消息模板</div>
          <el-form-item label="消息模板" prop="businessTypeId">
            <el-select
              v-model="form.businessTypeId"
              filterable
              clearable
              placeholder="请选择消息模板"
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
          <el-form-item v-if="selectedBusiness" label="模板配置">
            <div class="business-summary">
              <el-tag effect="plain">{{ selectedBusiness.bizGroup || '未分类' }}</el-tag>
              <span>{{ selectedBusiness.bizName }}</span>
              <span>生效版本：{{ selectedBusiness.activeVersion || '-' }}</span>
              <span>启用渠道：{{ enabledChannelText(selectedBusiness.enabledChannels) }}</span>
            </div>
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="section-title">自定义字段</div>
          <el-empty v-if="paramFields.length === 0" description="选择消息模板后显示需要填写的字段" />
          <el-row v-else :gutter="16">
            <el-col v-for="field in paramFields" :key="field.name" :xs="24" :sm="12">
              <el-form-item :label="field.label" :prop="`params.${field.name}`" :required="field.required">
                <el-input-number
                  v-if="field.type === 'number'"
                  v-model="form.params[field.name]"
                  class="full-width"
                  :placeholder="field.placeholder"
                  controls-position="right"
                />
                <el-switch v-else-if="field.type === 'boolean'" v-model="form.params[field.name]" />
                <el-date-picker
                  v-else-if="field.type === 'datetime'"
                  v-model="form.params[field.name]"
                  class="full-width"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择时间"
                />
                <el-input v-else v-model="form.params[field.name]" :placeholder="field.placeholder" clearable />
              </el-form-item>
            </el-col>
          </el-row>
        </section>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="saveDraft">暂存</el-button>
          <el-button type="primary" :loading="sending" @click="submit">发送</el-button>
          <el-button @click="sendDialogVisible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { getBusinessTypes, getIdentityUsers, getSendRecords, sendNotice, type NoticeIdentityUser } from '../../api/notice';
import type { NoticeBusinessType, NoticeChannelType, NoticeSendRecord, NoticeSendStatus } from '../../types/notice';

type ParamFieldType = 'string' | 'number' | 'boolean' | 'datetime';
type ParamValue = string | number | boolean | undefined;

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

const DRAFT_KEY = 'mango.notice.send-message.draft';

const formRef = ref<FormInstance>();
const recordLoading = ref(false);
const businessLoading = ref(false);
const sending = ref(false);
const userLoading = ref(false);
const sendDialogVisible = ref(false);
const records = ref<NoticeSendRecord[]>([]);
const businessTypes = ref<NoticeBusinessType[]>([]);
const userOptions = ref<NoticeIdentityUser[]>([]);
const form = reactive<{
  businessTypeId: string;
  params: Record<string, ParamValue>;
  userIds: string[];
}>({
  businessTypeId: '',
  params: {},
  userIds: [],
});

const rules = computed<FormRules>(() => ({
  userIds: [{ required: true, type: 'array', min: 1, message: '请选择接收用户', trigger: 'change' }],
  businessTypeId: [{ required: true, message: '请选择消息模板', trigger: 'change' }],
  ...paramFields.value.reduce<FormRules>((result, field) => {
    if (field.required) {
      result[`params.${field.name}`] = [{ required: true, message: `请填写${field.label}`, trigger: 'blur' }];
    }
    return result;
  }, {}),
}));

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

async function loadRecords() {
  recordLoading.value = true;
  try {
    const result = await getSendRecords({ pageNum: 1, pageSize: 50 });
    records.value = result.list || [];
  } finally {
    recordLoading.value = false;
  }
}

async function loadBusinessTypes() {
  businessLoading.value = true;
  try {
    const result = await getBusinessTypes({ enabled: true, pageNum: 1, pageSize: 200 });
    businessTypes.value = result.list || [];
  } finally {
    businessLoading.value = false;
  }
}

async function openSendDialog() {
  sendDialogVisible.value = true;
  if (businessTypes.value.length === 0) {
    await loadBusinessTypes();
  }
  searchUsers('');
  restoreDraft();
}

function handleBusinessChange() {
  Object.keys(form.params).forEach(key => delete form.params[key]);
  paramFields.value.forEach((field) => {
    form.params[field.name] = field.type === 'boolean' ? false : undefined;
  });
  formRef.value?.clearValidate();
}

async function searchUsers(keyword: string) {
  userLoading.value = true;
  try {
    const query = keyword.trim();
    const result = await getIdentityUsers(query, { pageNum: 1, pageSize: 20, status: 1 });
    userOptions.value = result.list || [];
  } finally {
    userLoading.value = false;
  }
}

function saveDraft() {
  localStorage.setItem(DRAFT_KEY, JSON.stringify({
    businessTypeId: form.businessTypeId,
    params: form.params,
    userIds: form.userIds,
  }));
  ElMessage.success('已暂存');
}

function restoreDraft() {
  const raw = localStorage.getItem(DRAFT_KEY);
  if (!raw) {
    return;
  }
  try {
    const draft = JSON.parse(raw) as { businessTypeId?: string; params?: Record<string, ParamValue>; userIds?: string[] };
    form.businessTypeId = draft.businessTypeId || '';
    form.userIds = Array.isArray(draft.userIds) ? draft.userIds : [];
    Object.keys(form.params).forEach(key => delete form.params[key]);
    Object.assign(form.params, isRecord(draft.params) ? draft.params : {});
  } catch {
    localStorage.removeItem(DRAFT_KEY);
  }
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
    const value = form.params[field.name];
    if (value !== undefined && value !== '') {
      result[field.name] = value;
    }
    return result;
  }, {});
}

function validateParams() {
  const missing = paramFields.value.find((field) => {
    const value = form.params[field.name];
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
    ElMessage.error('请选择消息模板');
    return;
  }
  if (!validateParams()) {
    return;
  }
  if (form.userIds.length === 0) {
    ElMessage.error('请选择接收用户');
    return;
  }
  sending.value = true;
  try {
    const result = await sendNotice({
      bizType: selectedBusiness.value.bizType,
      params: buildParams(),
      userIds: form.userIds,
    });
    const hasImmediateResult = result.successCount > 0 || result.failCount > 0;
    ElMessage.success(hasImmediateResult ? `发送已提交，成功 ${result.successCount} 条，失败 ${result.failCount} 条` : '发送任务已提交，可在发送列表查看结果');
    localStorage.removeItem(DRAFT_KEY);
    sendDialogVisible.value = false;
    await loadRecords();
  } finally {
    sending.value = false;
  }
}

function resetForm() {
  form.businessTypeId = '';
  form.userIds = [];
  Object.keys(form.params).forEach(key => delete form.params[key]);
  formRef.value?.clearValidate();
}

function userLabel(user: NoticeIdentityUser) {
  return [user.nickname || user.username, user.username, user.phone, user.email].filter(Boolean).join(' / ');
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

function statusLabel(status: NoticeSendStatus) {
  const labels: Record<NoticeSendStatus, string> = {
    PENDING: '待发送',
    SENDING: '发送中',
    SUCCESS: '成功',
    FAILED: '失败',
    RETRY_WAITING: '等待重试',
    FINAL_FAILED: '最终失败',
    CANCELED: '已取消',
  };
  return labels[status] || status;
}

function statusTag(status: NoticeSendStatus) {
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED' || status === 'FINAL_FAILED') return 'danger';
  if (status === 'RETRY_WAITING') return 'warning';
  return 'info';
}

onMounted(() => {
  loadRecords();
  loadBusinessTypes();
});
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

.page-actions {
  display: flex;
  gap: 12px;
}

.send-form {
  max-height: 62vh;
  padding-right: 6px;
  overflow-y: auto;
}

.form-section {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.form-section:last-of-type {
  margin-bottom: 0;
  border-bottom: 0;
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

.user-option {
  display: grid;
  grid-template-columns: minmax(90px, 1fr) minmax(90px, 1fr) minmax(110px, 1fr) minmax(150px, 1.4fr);
  gap: 12px;
  align-items: center;
}

.user-option span:not(:first-child) {
  color: var(--el-text-color-secondary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
