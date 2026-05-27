<template>
  <div class="notice-send-message-page">
    <div class="page-header">
      <h1>发送任务</h1>
      <div class="page-actions">
        <el-button :loading="recordLoading" @click="loadRecords">刷新</el-button>
        <el-button type="primary" @click="openSendDialog">新增任务</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="tasks" border stripe v-loading="recordLoading">
        <el-table-column prop="taskCode" label="登记编号" width="190" show-overflow-tooltip />
        <el-table-column label="业务域" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.bizGroup || '-' }}</template>
        </el-table-column>
        <el-table-column label="消息模板名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.bizName || row.bizType }}</template>
        </el-table-column>
        <el-table-column label="计划渠道" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ enabledChannelText(row.channelTypes) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="taskStatusTag(row.status)">{{ taskStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="明细数" width="90" align="right" />
        <el-table-column prop="successCount" label="成功" width="80" align="right" />
        <el-table-column prop="failCount" label="失败" width="80" align="right" />
        <el-table-column prop="createdAt" label="提交时间" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTaskDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="sendDialogVisible"
      title="新增任务"
      width="720px"
      destroy-on-close
      class="send-message-dialog"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px" class="send-form">
        <div class="send-form-body">
          <el-form-item label="发送给" prop="recipientTargets">
            <ParticipantSelector
              v-model="recipientValue"
              :user-options="participantUserOptions"
              :role-options="participantRoleOptions"
              :post-options="participantPostOptions"
              :org-tree-options="orgTreeOptions"
              :target-loading="participantLoading"
              @ensure-users="ensureUsersLoaded"
              @ensure-orgs="loadOrgTree"
              @ensure-posts="loadPosts"
              @ensure-roles="loadRoles"
            />
          </el-form-item>

          <el-form-item label="消息类型" prop="businessTypeId">
            <el-select
              v-model="form.businessTypeId"
              filterable
              clearable
              placeholder="请选择消息类型"
              class="full-width"
              :loading="businessLoading"
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
          <el-form-item v-if="selectedBusiness" label="配置概览">
            <div class="business-summary">
              <el-tag effect="plain">{{ selectedBusiness.bizGroup || '未分类' }}</el-tag>
              <span>{{ selectedBusiness.bizName }}</span>
              <span>生效版本：{{ selectedBusiness.activeVersion || '-' }}</span>
              <span>启用渠道：{{ enabledChannelText(selectedBusiness.enabledChannels) }}</span>
            </div>
          </el-form-item>

          <div class="param-panel">
            <el-empty v-if="paramFields.length === 0" description="选择消息类型后显示需要填写的字段" />
            <el-row v-else :gutter="16">
              <el-col v-for="field in paramFields" :key="field.name" :xs="24" :sm="12">
                <el-form-item :label="field.label" :prop="`params.${field.name}`" :required="field.required">
                  <el-select
                    v-if="field.type === 'select'"
                    v-model="form.params[field.name]"
                    class="full-width"
                    clearable
                    :placeholder="field.placeholder"
                  >
                    <el-option
                      v-for="option in field.options"
                      :key="String(option.value)"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-input-number
                    v-else-if="field.type === 'number' || field.type === 'integer'"
                    v-model="form.params[field.name]"
                    class="full-width"
                    :placeholder="field.placeholder"
                    controls-position="right"
                    :precision="field.type === 'integer' ? 0 : undefined"
                  />
                  <el-switch v-else-if="field.type === 'boolean'" v-model="form.params[field.name]" />
                  <el-date-picker
                    v-else-if="field.type === 'date'"
                    v-model="form.params[field.name]"
                    class="full-width"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择日期"
                  />
                  <el-time-picker
                    v-else-if="field.type === 'time'"
                    v-model="form.params[field.name]"
                    class="full-width"
                    value-format="HH:mm:ss"
                    placeholder="请选择时间"
                  />
                  <el-date-picker
                    v-else-if="field.type === 'datetime'"
                    v-model="form.params[field.name]"
                    class="full-width"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请选择时间"
                  />
                  <el-input
                    v-else-if="field.type === 'textarea' || field.type === 'json'"
                    v-model="form.params[field.name]"
                    :placeholder="field.placeholder"
                    type="textarea"
                    :rows="field.type === 'json' ? 4 : 3"
                  />
                  <el-input v-else v-model="form.params[field.name]" :placeholder="field.placeholder" clearable />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="saveDraft">暂存</el-button>
          <el-button type="primary" :loading="sending" @click="submit">发送</el-button>
          <el-button @click="sendDialogVisible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="taskDetailVisible" title="发送详情" width="760px" destroy-on-close>
      <el-descriptions v-if="selectedTask" :column="2" border>
        <el-descriptions-item label="登记编号">{{ selectedTask.taskCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="taskStatusTag(selectedTask.status)">{{ taskStatusLabel(selectedTask.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="业务域">{{ selectedTask.bizGroup || '-' }}</el-descriptions-item>
        <el-descriptions-item label="消息模板名称">{{ selectedTask.bizName || selectedTask.bizType }}</el-descriptions-item>
        <el-descriptions-item label="模板 Key">{{ selectedTask.bizType }}</el-descriptions-item>
        <el-descriptions-item label="计划渠道">{{ enabledChannelText(selectedTask.channelTypes) }}</el-descriptions-item>
        <el-descriptions-item label="明细数">{{ selectedTask.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ selectedTask.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ selectedTask.successCount }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ selectedTask.failCount }}</el-descriptions-item>
      </el-descriptions>
      <el-tabs v-if="selectedTask" class="detail-tabs">
        <el-tab-pane label="接收对象 JSON">
          <pre class="json-viewer">{{ snapshotJson(selectedTask.recipientTargetsSnapshot, '[]') }}</pre>
        </el-tab-pane>
        <el-tab-pane label="参数 JSON">
          <pre class="json-viewer">{{ snapshotJson(selectedTask.paramsSnapshot, '{}') }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import ParticipantSelector from '../../../../system/src/components/ParticipantSelector/index.vue';
import type {
  ParticipantOrgTreeOption,
  ParticipantSelectorValue,
  ParticipantTargetOption,
} from '../../../../system/src/components/ParticipantSelector/types';
import {
  getBusinessTypes,
  getIdentityUsers,
  getNoticeOrgTree,
  getNoticePosts,
  getNoticeRoles,
  getNoticeTasks,
  sendNotice,
  type NoticeIdentityUser,
  type NoticeOrgNode,
  type NoticePost,
  type NoticeRole,
} from '../../api/notice';
import type {
  NoticeBusinessType,
  NoticeChannelType,
  NoticeRecipientTargetCommand,
  NoticeRecipientTargetType,
  NoticeTask,
  NoticeTaskStatus,
} from '../../types/notice';

type ParamFieldType = 'string' | 'textarea' | 'number' | 'integer' | 'boolean' | 'date' | 'time' | 'datetime' | 'select' | 'json';
type ParamValue = string | number | boolean | undefined;

interface ParamOption {
  label: string;
  value: string | number | boolean;
}

interface JsonSchemaProperty {
  type?: string;
  title?: string;
  description?: string;
  format?: string;
  default?: unknown;
  example?: unknown;
  enum?: Array<string | number | boolean>;
  enumNames?: string[];
  options?: ParamOption[];
  oneOf?: Array<{ title?: string; const?: unknown; value?: unknown }>;
}

interface JsonSchema {
  type?: string;
  properties?: Record<string, JsonSchemaProperty>;
  required?: string[];
}

interface LegacyParamSchema {
  params?: Array<{
    name?: unknown;
    label?: unknown;
    description?: unknown;
    type?: unknown;
    required?: unknown;
    example?: unknown;
  }>;
}

interface ParamField {
  name: string;
  label: string;
  type: ParamFieldType;
  required: boolean;
  placeholder: string;
  options: ParamOption[];
}

const DRAFT_KEY = 'mango.notice.send-message.draft';

const formRef = ref<FormInstance>();
const recordLoading = ref(false);
const businessLoading = ref(false);
const sending = ref(false);
const userLoading = ref(false);
const orgLoading = ref(false);
const postLoading = ref(false);
const roleLoading = ref(false);
const sendDialogVisible = ref(false);
const taskDetailVisible = ref(false);
const tasks = ref<NoticeTask[]>([]);
const selectedTask = ref<NoticeTask>();
const businessTypes = ref<NoticeBusinessType[]>([]);
const userOptions = ref<NoticeIdentityUser[]>([]);
const orgTreeOptions = ref<ParticipantOrgTreeOption[]>([]);
const postOptions = ref<NoticePost[]>([]);
const roleOptions = ref<NoticeRole[]>([]);
const form = reactive<{
  businessTypeId: string;
  params: Record<string, ParamValue>;
  recipientTargets: NoticeRecipientTargetCommand[];
}>({
  businessTypeId: '',
  params: {},
  recipientTargets: [],
});

const rules = computed<FormRules>(() => ({
  recipientTargets: [{ required: true, type: 'array', min: 1, message: '请选择接收对象', trigger: 'change' }],
  businessTypeId: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  ...paramFields.value.reduce<FormRules>((result, field) => {
    if (field.required) {
      result[`params.${field.name}`] = [{ required: true, message: `请填写${field.label}`, trigger: 'blur' }];
    }
    return result;
  }, {}),
}));

const recipientValue = computed<ParticipantSelectorValue>({
  get: () => ({
    userIds: targetIds('USER'),
    orgIds: targetIds('ORG'),
    roleIds: targetIds('ROLE'),
    postIds: targetIds('POST'),
  }),
  set: value => {
    form.recipientTargets = [
      ...recipientTargetsOf('USER', value.userIds || [], participantUserOptions.value),
      ...recipientTargetsOf('ORG', value.orgIds || [], flattenOrgOptions(orgTreeOptions.value)),
      ...recipientTargetsOf('POST', value.postIds || [], participantPostOptions.value),
      ...recipientTargetsOf('ROLE', value.roleIds || [], participantRoleOptions.value),
    ];
    formRef.value?.clearValidate('recipientTargets');
  },
});

const participantLoading = computed(() => ({
  users: userLoading.value,
  orgs: orgLoading.value,
  posts: postLoading.value,
  roles: roleLoading.value,
}));

const participantUserOptions = computed<ParticipantTargetOption[]>(() => userOptions.value
  .filter(item => item.userId !== undefined)
  .map(item => ({
    value: String(item.userId),
    label: item.nickname || item.username || String(item.userId),
  })));

const participantPostOptions = computed<ParticipantTargetOption[]>(() => postOptions.value
  .filter(item => item.id !== undefined)
  .map(item => ({
    value: String(item.id),
    label: postLabel(item),
  })));

const participantRoleOptions = computed<ParticipantTargetOption[]>(() => roleOptions.value
  .filter(item => item.roleId !== undefined)
  .map(item => ({
    value: String(item.roleId),
    label: roleLabel(item),
  })));

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
    const result = await getNoticeTasks({ pageNum: 1, pageSize: 50 });
    tasks.value = result.list || [];
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
  ensureUsersLoaded();
  restoreDraft();
}

function openTaskDetail(task: NoticeTask) {
  selectedTask.value = task;
  taskDetailVisible.value = true;
}

function handleBusinessChange() {
  Object.keys(form.params).forEach(key => delete form.params[key]);
  paramFields.value.forEach((field) => {
    form.params[field.name] = defaultParamValue(field);
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

async function ensureUsersLoaded() {
  if (userOptions.value.length > 0) {
    return;
  }
  await searchUsers('');
}

async function loadOrgTree() {
  if (orgTreeOptions.value.length > 0) {
    return;
  }
  orgLoading.value = true;
  try {
    const result = await getNoticeOrgTree({ parentId: '0', includeDisabled: false });
    orgTreeOptions.value = toOrgTreeOptions(result || []);
  } finally {
    orgLoading.value = false;
  }
}

async function loadPosts() {
  if (postOptions.value.length > 0) {
    return;
  }
  postLoading.value = true;
  try {
    const result = await getNoticePosts({ pageNum: 1, pageSize: 200, postStatus: '1' });
    postOptions.value = (result.list || []).filter(item => item.id !== undefined);
  } finally {
    postLoading.value = false;
  }
}

async function loadRoles() {
  if (roleOptions.value.length > 0) {
    return;
  }
  roleLoading.value = true;
  try {
    const result = await getNoticeRoles();
    roleOptions.value = (result || []).filter(item => item.roleId !== undefined && item.status !== 0);
  } finally {
    roleLoading.value = false;
  }
}

function saveDraft() {
  localStorage.setItem(DRAFT_KEY, JSON.stringify({
    businessTypeId: form.businessTypeId,
    params: form.params,
    recipientTargets: form.recipientTargets,
  }));
  ElMessage.success('已暂存');
}

function restoreDraft() {
  const raw = localStorage.getItem(DRAFT_KEY);
  if (!raw) {
    return;
  }
  try {
    const draft = JSON.parse(raw) as {
      businessTypeId?: string;
      params?: Record<string, ParamValue>;
      recipientTargets?: NoticeRecipientTargetCommand[];
      userIds?: string[];
    };
    form.businessTypeId = draft.businessTypeId || '';
    form.recipientTargets = Array.isArray(draft.recipientTargets)
      ? draft.recipientTargets.filter(isRecipientTarget).map(target => ({ ...target, targetId: String(target.targetId) }))
      : legacyUserTargets(draft.userIds);
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
    options: paramFieldOptions(property),
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
    const legacySchema = normalizeLegacyParamSchema(parsed as LegacyParamSchema);
    if (legacySchema) {
      return legacySchema;
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
          enum: Array.isArray(value.enum) ? value.enum.filter(isOptionValue) : undefined,
          enumNames: Array.isArray(value.enumNames) ? value.enumNames.filter(item => typeof item === 'string') : undefined,
          options: Array.isArray(value.options) ? value.options.filter(isParamOption) : undefined,
          oneOf: Array.isArray(value.oneOf) ? value.oneOf.filter(isRecord).map(item => ({
            title: typeof item.title === 'string' ? item.title : undefined,
            const: item.const,
            value: item.value,
          })) : undefined,
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

function normalizeLegacyParamSchema(schema: LegacyParamSchema): JsonSchema | undefined {
  if (!Array.isArray(schema.params)) {
    return undefined;
  }
  const properties: Record<string, JsonSchemaProperty> = {};
  const required: string[] = [];
  schema.params.forEach((item) => {
    if (!isRecord(item) || typeof item.name !== 'string' || !item.name.trim()) {
      return;
    }
    const name = item.name.trim();
    properties[name] = {
      type: typeof item.type === 'string' ? item.type : 'string',
      title: typeof item.label === 'string' ? item.label : undefined,
      description: typeof item.description === 'string' ? item.description : undefined,
      example: item.example,
    };
    if (item.required === true) {
      required.push(name);
    }
  });
  return { type: 'object', properties, required };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function isOptionValue(value: unknown): value is string | number | boolean {
  return ['string', 'number', 'boolean'].includes(typeof value);
}

function isParamOption(value: unknown): value is ParamOption {
  return isRecord(value) && typeof value.label === 'string' && isOptionValue(value.value);
}

function paramFieldType(property: JsonSchemaProperty): ParamFieldType {
  const type = String(property.type || '').toLowerCase();
  const format = String(property.format || '').toLowerCase();
  if (paramFieldOptions(property).length > 0) {
    return 'select';
  }
  if (type === 'number') {
    return 'number';
  }
  if (type === 'integer' || type === 'int' || type === 'long') {
    return 'integer';
  }
  if (type === 'boolean' || type === 'bool') {
    return 'boolean';
  }
  if (type === 'datetime' || type === 'date-time' || format === 'date-time' || format === 'datetime') {
    return 'datetime';
  }
  if (type === 'date' || format === 'date') {
    return 'date';
  }
  if (type === 'time' || format === 'time') {
    return 'time';
  }
  if (type === 'textarea' || format === 'textarea') {
    return 'textarea';
  }
  if (type === 'object' || type === 'array' || type === 'json' || format === 'json') {
    return 'json';
  }
  return 'string';
}

function paramFieldOptions(property: JsonSchemaProperty): ParamOption[] {
  if (property.options?.length) {
    return property.options;
  }
  if (property.enum?.length) {
    return property.enum.map((value, index) => ({
      value,
      label: property.enumNames?.[index] || String(value),
    }));
  }
  if (property.oneOf?.length) {
    return property.oneOf
      .map((item) => {
        const value = isOptionValue(item.const) ? item.const : isOptionValue(item.value) ? item.value : undefined;
        return value === undefined ? undefined : { value, label: item.title || String(value) };
      })
      .filter((item): item is ParamOption => Boolean(item));
  }
  return [];
}

function defaultParamValue(field: ParamField): ParamValue {
  if (field.type === 'boolean') {
    return false;
  }
  return undefined;
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
    ElMessage.error('请选择消息类型');
    return;
  }
  if (!validateParams()) {
    return;
  }
  if (form.recipientTargets.length === 0) {
    ElMessage.error('请选择接收对象');
    return;
  }
  sending.value = true;
  try {
    const result = await sendNotice({
      bizType: selectedBusiness.value.bizType,
      params: buildParams(),
      recipientTargets: form.recipientTargets,
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
  form.recipientTargets = [];
  Object.keys(form.params).forEach(key => delete form.params[key]);
  formRef.value?.clearValidate();
}

function targetIds(type: NoticeRecipientTargetType) {
  return form.recipientTargets
    .filter(item => item.targetType === type)
    .map(item => String(item.targetId));
}

function recipientTargetsOf(type: NoticeRecipientTargetType, ids: string[], options: ParticipantTargetOption[]): NoticeRecipientTargetCommand[] {
  return ids.map(id => ({
    targetType: type,
    targetId: id,
    targetName: options.find(item => item.value === id)?.label || id,
  }));
}

function isRecipientTarget(value: unknown): value is NoticeRecipientTargetCommand {
  if (!isRecord(value)) {
    return false;
  }
  return ['USER', 'ORG', 'POST', 'ROLE'].includes(String(value.targetType))
    && (typeof value.targetId === 'string' || typeof value.targetId === 'number');
}

function legacyUserTargets(userIds?: string[]) {
  return Array.isArray(userIds)
    ? userIds.map(id => ({ targetType: 'USER' as const, targetId: id, targetName: id }))
    : [];
}

function postLabel(post: NoticePost) {
  return [post.postName, post.postCode].filter(Boolean).join(' / ');
}

function roleLabel(role: NoticeRole) {
  return [role.roleName, role.roleCode].filter(Boolean).join(' / ');
}

function toOrgTreeOptions(nodes: NoticeOrgNode[]): ParticipantOrgTreeOption[] {
  return nodes.map(node => ({
    value: String(node.id),
    label: node.orgName,
    children: node.children?.length ? toOrgTreeOptions(node.children) : undefined,
  }));
}

function flattenOrgOptions(items: ParticipantOrgTreeOption[]): ParticipantTargetOption[] {
  const result: ParticipantTargetOption[] = [];
  const visit = (nodes: ParticipantOrgTreeOption[]) => {
    for (const node of nodes || []) {
      result.push({ value: node.value, label: node.label });
      if (node.children?.length) {
        visit(node.children);
      }
    }
  };
  visit(items);
  return result;
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

function taskStatusLabel(status: NoticeTaskStatus) {
  const labels: Record<NoticeTaskStatus, string> = {
    WAITING: '待发送',
    SENDING: '发送中',
    PARTIAL_SUCCESS: '部分成功',
    SUCCESS: '成功',
    FAILED: '失败',
    CANCELED: '已取消',
  };
  return labels[status] || status;
}

function taskStatusTag(status: NoticeTaskStatus) {
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'PARTIAL_SUCCESS') return 'warning';
  return 'info';
}

function snapshotJson(snapshot?: string, emptyValue = '-') {
  if (!snapshot?.trim()) {
    return emptyValue;
  }
  try {
    return JSON.stringify(JSON.parse(snapshot));
  } catch {
    return snapshot;
  }
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
  max-height: 580px;
  padding: 0 8px 0 0;
  overflow-y: auto;
}

.send-form-body {
  display: grid;
  gap: 4px;
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

.param-panel {
  padding-top: 2px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.detail-tabs {
  margin-top: 16px;
}

.json-viewer {
  min-height: 160px;
  max-height: 280px;
  padding: 12px;
  margin: 0;
  overflow: auto;
  font-family: var(--el-font-family);
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}
</style>
