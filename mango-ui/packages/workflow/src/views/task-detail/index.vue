<template>
  <div class="workflow-task-detail-page">
    <el-card v-loading="loading" class="task-detail-shell">
      <template #header>
        <div class="card-header">
          <div>
            <div class="page-title">{{ readonlyMode ? '流程详情' : '处理任务' }}</div>
            <div class="page-subtitle">{{ detail?.process.processName || '-' }}</div>
          </div>
          <div class="header-actions">
            <el-tag v-if="detail?.process.status" effect="plain">{{ detail.process.status }}</el-tag>
            <el-button @click="backToList">返回</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!detail" description="暂无流程详情" />

      <div v-else class="task-detail-layout">
        <main class="task-main">
          <section class="task-section process-summary">
            <div class="section-header">
              <div>
                <h3>流程基础信息</h3>
                <p>{{ detail.process.processKey }}</p>
              </div>
              <el-tag v-if="detail.task?.taskName" type="warning" effect="plain">{{ detail.task.taskName }}</el-tag>
            </div>
            <div class="summary-grid">
              <div class="summary-item important">
                <span>流程名称</span>
                <strong>{{ detail.process.processName }}</strong>
              </div>
              <div class="summary-item important">
                <span>发起人</span>
                <strong>{{ detail.process.initiatorName || '-' }}</strong>
              </div>
              <div class="summary-item important">
                <span>办理人</span>
                <strong>{{ detail.task?.assigneeName || '-' }}</strong>
              </div>
              <div class="summary-item muted">
                <span>开始时间</span>
                <strong>{{ detail.process.startTime || '-' }}</strong>
              </div>
              <div class="summary-item muted">
                <span>结束时间</span>
                <strong>{{ detail.process.endTime || '-' }}</strong>
              </div>
              <div class="summary-item muted">
                <span>业务关联</span>
                <strong>{{ detail.process.businessKey || detail.task?.businessKey || '-' }}</strong>
              </div>
            </div>
          </section>

          <section class="task-section">
            <div class="section-header compact">
              <h3>{{ businessComponent ? '业务审批信息' : '业务表单信息' }}</h3>
            </div>
            <component
              :is="businessComponent"
              v-if="businessComponent && businessContext"
              :context="businessContext"
            />
            <RuntimeFormRenderer
              v-else-if="shouldRenderDynamicForm && runtimeFields.length"
              :fields="runtimeFields"
              :model="detail.variables"
              :readonly="readonlyMode"
              :permissions="effectiveFormPermissions"
            />
            <el-alert
              v-else-if="isCustomRenderMode"
              title="当前流程配置为自定义业务表单，但未找到匹配的审批组件。"
              type="warning"
              :closable="false"
              show-icon
            />
            <el-descriptions v-else :column="1" border>
              <el-descriptions-item label="流程变量">
                <pre class="json-preview">{{ formatJson(detail.variables) }}</pre>
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              v-if="unsupportedFields.length"
              class="detail-alert"
              :title="`有 ${unsupportedFields.length} 个复杂表单组件暂以变量 JSON 展示。`"
              type="warning"
              :closable="false"
              show-icon
            />
          </section>

          <section v-if="!readonlyMode && showActionCommentInput" class="task-section approval-section">
            <div class="section-header compact">
              <h3>审批动作</h3>
            </div>
            <el-form label-width="84px">
              <el-form-item label="审批意见">
                <el-input v-model="actionForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
              </el-form-item>
            </el-form>
          </section>

          <div v-if="!readonlyMode" class="approval-action-bar">
            <el-tooltip
              v-for="action in visibleNodeActions"
              :key="action.key"
              :content="action.tooltip || ''"
              :disabled="!action.tooltip"
            >
              <el-button
                :type="action.buttonType"
                :plain="action.key !== 'complete'"
                :disabled="action.disabled"
                :loading="submittingAction === action.key"
                @click="submitAction(action.key)"
              >
                {{ action.label }}
              </el-button>
            </el-tooltip>
          </div>
        </main>

        <aside v-if="showRecordPanel" class="record-panel">
          <div class="record-header">
            <h3>{{ customRecordPanelComponent ? '审批信息' : '审批记录' }}</h3>
            <span>{{ detail.records.length }} 条</span>
          </div>
          <component
            :is="customRecordPanelComponent"
            v-if="customRecordPanelComponent && businessContext"
            :context="businessContext"
          />
          <el-timeline v-else-if="detail.records.length" class="record-timeline">
            <el-timeline-item
              v-for="record in detail.records"
              :key="record.id || `${record.action}-${record.createdTime}`"
              :timestamp="record.createdTime"
              placement="top"
            >
              <div class="record-title">{{ record.actionName }} · {{ record.operatorName || '-' }}</div>
              <div v-if="record.taskName" class="record-meta">{{ record.taskName }}</div>
              <div v-if="record.comment" class="record-comment">{{ record.comment }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审批记录" />
        </aside>
      </div>
    </el-card>

    <el-dialog
      v-model="selectorDialog.visible"
      :title="selectorDialog.action === 'transfer' ? '选择转办人员' : '选择加签人员'"
      width="520px"
      append-to-body
      destroy-on-close
      @closed="cancelUserSelection"
    >
      <UserSelector
        v-if="selectorDialog.action === 'transfer'"
        v-model="selectorDialog.targetUserId"
        mode="dialog"
        placeholder="请选择目标办理人"
        title="选择转办人员"
      />
      <UserSelector
        v-else
        v-model="selectorDialog.targetUserIds"
        mode="dialog"
        multiple
        placeholder="请选择加签人"
        title="选择加签人员"
      />
      <template #footer>
        <el-button @click="cancelUserSelection">取消</el-button>
        <el-button type="primary" @click="confirmUserSelection">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { UserSelector } from '@mango/common';
import { workflowApi, type WorkflowBusinessApply, type WorkflowProcessDetail, type WorkflowTaskActionKey, type WorkflowTaskDetail } from '../../api/workflow';
import {
  applyIdOf,
  businessPermissionsOf,
  businessTypeOf,
  collectBusinessApprovalComment,
  collectBusinessApprovalVariables,
  resolveBusinessApprovalRegistration,
  type BusinessApprovalContext,
} from '../../components/businessApproval';
import RuntimeFormRenderer from '../../components/RuntimeFormRenderer.vue';
import { parseRuntimeForm, type RuntimeFormField } from '../../components/runtimeForm';
import {
  defaultWorkflowActionLabel,
  isWorkflowCommentRequired,
  normalizeWorkflowNodeActions,
  resolveVisibleWorkflowActions,
} from '../../components/taskActions';
import { parseWorkflowFormConfig } from '../../workflowFormConfig';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const submittingAction = ref<WorkflowTaskActionKey | ''>('');
const taskDetail = ref<WorkflowTaskDetail | null>(null);
const processDetail = ref<WorkflowProcessDetail | null>(null);
const businessApply = ref<WorkflowBusinessApply | null>(null);
const runtimeFields = ref<RuntimeFormField[]>([]);
const unsupportedFields = ref<Array<{ label: string; type: string }>>([]);
const actionForm = ref({
  comment: '',
});
const selectorDialog = ref({
  visible: false,
  action: '' as 'transfer' | 'addSign' | '',
  targetUserId: '',
  targetUserIds: [] as string[],
});

const readonlyMode = computed(() => route.query.mode === 'view' || !route.query.taskId);
const detail = computed(() => {
  if (taskDetail.value) {
    return taskDetail.value;
  }
  if (!processDetail.value) {
    return null;
  }
  return {
    task: null,
    process: processDetail.value.process,
    formCode: processDetail.value.formCode,
    formJson: processDetail.value.formJson,
    variables: processDetail.value.variables,
    records: processDetail.value.records,
    formPermissions: processDetail.value.renderConfig?.formPermissions || {},
    renderConfig: processDetail.value.renderConfig,
  } as unknown as WorkflowTaskDetail;
});
const currentTaskDefinitionKey = computed(() =>
  detail.value?.task?.taskDefinitionKey
  || (detail.value?.task?.id ? firstCurrentTaskDefinitionKey() : latestTaskDefinitionKey()),
);
const renderConfig = computed(() => detail.value?.renderConfig);
const businessType = computed(() => renderConfig.value?.businessType || businessApply.value?.businessType || businessTypeOf(detail.value?.variables));
const formConfig = computed(() => parseWorkflowFormConfig(detail.value?.formJson));
const renderMode = computed(() => {
  if (formConfig.value.mode === 'CUSTOM_PAGE') {
    return 'CUSTOM_PAGE';
  }
  return renderConfig.value?.renderMode || businessApply.value?.renderMode || 'DYNAMIC_FORM';
});
const approvePageKey = computed(() =>
  String(renderConfig.value?.nodeExtension?.approvePageKey || renderConfig.value?.approvePageKey || formConfig.value.customConfig.approvePageKey || '').trim(),
);
const businessRegistration = computed(() => renderMode.value === 'CUSTOM_PAGE'
  ? resolveBusinessApprovalRegistration(approvePageKey.value)
  : null);
const businessComponent = computed(() => businessRegistration.value?.component || null);
const recordPanelMode = computed(() => businessRegistration.value?.recordPanelMode || 'DEFAULT');
const customRecordPanelComponent = computed(() => recordPanelMode.value === 'CUSTOM'
  ? businessRegistration.value?.recordPanelComponent || null
  : null);
const showRecordPanel = computed(() => recordPanelMode.value !== 'HIDDEN');
const isCustomRenderMode = computed(() => renderMode.value === 'CUSTOM_PAGE');
const shouldRenderDynamicForm = computed(() => !isCustomRenderMode.value);
const effectiveFormPermissions = computed(() => {
  const permissions = { ...(detail.value?.formPermissions || {}) };
  if (readonlyMode.value) {
    return permissions;
  }
  fillDefaultApprovalPermissions(runtimeFields.value, permissions);
  return permissions;
});
const commentMode = computed(() => businessRegistration.value?.commentMode || 'ACTION_BAR');
const showActionCommentInput = computed(() => commentMode.value === 'ACTION_BAR');
const visibleNodeActions = computed(() => {
  const overrides = businessContext.value && businessRegistration.value?.getActionOverrides
    ? businessRegistration.value.getActionOverrides(businessContext.value)
    : {};
  const task = detail.value?.task;
  const candidateOverrides = {
    ...overrides,
    claim: {
      ...(overrides.claim || {}),
      visible: !readonlyMode.value && Boolean(task?.id) && Boolean(task?.claimable),
      label: overrides.claim?.label || '认领',
    },
    unclaim: {
      ...(overrides.unclaim || {}),
      visible: !readonlyMode.value && Boolean(task?.id) && Boolean(task?.unclaimable),
      label: overrides.unclaim?.label || '释放',
    },
  };
  return resolveVisibleWorkflowActions(normalizedNodeActions.value, candidateOverrides);
});
const normalizedNodeActions = computed(() => ({
  ...normalizeWorkflowNodeActions(renderConfig.value?.nodeActions),
  claim: { enabled: true, label: '认领', order: 5 },
  unclaim: { enabled: true, label: '释放', order: 6 },
}));
const businessContext = computed<BusinessApprovalContext | null>(() => {
  if (!detail.value || !businessType.value) {
    return null;
  }
  const variables = detail.value.variables || {};
  return {
    businessType: businessType.value,
    businessKey: String(renderConfig.value?.businessKey || businessApply.value?.businessKey || variables.businessKey || detail.value.process.businessKey || detail.value.task?.businessKey || ''),
    applyId: String(renderConfig.value?.applyId || businessApply.value?.id || applyIdOf(variables)),
    processInstanceId: detail.value.process.processInstanceId,
    taskId: detail.value.task?.id,
    taskDefinitionKey: currentTaskDefinitionKey.value,
    nodeName: detail.value.task?.taskName || latestTaskName(),
    nodeExtension: renderConfig.value?.nodeExtension || {},
    readonly: readonlyMode.value,
    variables,
    permissions: renderConfig.value?.businessPermissions || businessPermissionsOf(variables, currentTaskDefinitionKey.value),
    records: detail.value.records || [],
  };
});

async function loadDetail() {
  loading.value = true;
  try {
    taskDetail.value = null;
    processDetail.value = null;
    businessApply.value = null;
    runtimeFields.value = [];
    unsupportedFields.value = [];
    actionForm.value.comment = '';
    const taskId = String(route.query.taskId || '');
    const processInstanceId = String(route.query.processInstanceId || '');
    if (taskId) {
      taskDetail.value = await workflowApi.taskDetail(taskId);
      processDetail.value = null;
    } else if (processInstanceId) {
      processDetail.value = await workflowApi.processDetail(processInstanceId);
      taskDetail.value = null;
    } else {
      ElMessage.warning('缺少任务ID或流程实例ID');
      return;
    }
    await loadBusinessApply();
    const parsed = parseRuntimeForm(detail.value?.formJson);
    runtimeFields.value = parsed.fields;
    unsupportedFields.value = parsed.unsupported;
  } finally {
    loading.value = false;
  }
}

async function loadBusinessApply() {
  const processInstanceId = detail.value?.process.processInstanceId;
  if (!processInstanceId || renderMode.value !== 'CUSTOM_PAGE') {
    businessApply.value = null;
    return;
  }
  try {
    businessApply.value = await workflowApi.businessApplyByProcessInstance(processInstanceId);
  } catch {
    businessApply.value = null;
  }
}

async function submitAction(action: WorkflowTaskActionKey) {
  const taskId = String(route.query.taskId || '');
  if (!taskId) {
    ElMessage.warning('缺少任务ID');
    return;
  }
  const actionConfig = visibleNodeActions.value.find(item => item.key === action);
  if (!actionConfig || actionConfig.disabled) {
    if (actionConfig?.tooltip) ElMessage.warning(actionConfig.tooltip);
    return;
  }
  const actionName = actionConfig.label || defaultWorkflowActionLabel(action);
  const comment = collectActionComment(action);
  if (isWorkflowCommentRequired(actionConfig, comment)) {
    ElMessage.warning('请填写审批意见');
    return;
  }
  try {
    if (businessRegistration.value?.validateBeforeAction && businessContext.value) {
      await businessRegistration.value.validateBeforeAction(businessContext.value, action);
    }
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '请检查审批信息');
    return;
  }
  const extraPayload = await collectExtraActionPayload(action);
  if (extraPayload === false) {
    return;
  }
  await ElMessageBox.confirm(actionConfig.confirmText || `确认${actionName}当前任务？`, `审批${actionName}`, { type: action === 'reject' ? 'error' : 'warning' });
  submitting.value = true;
  submittingAction.value = action;
  try {
    if (businessRegistration.value?.beforeAction && businessContext.value) {
      await businessRegistration.value.beforeAction(businessContext.value, action);
    }
    const variables = editableFormVariables(action);
    let result: unknown;
    result = await executeTaskAction(action, taskId, comment, variables, extraPayload);
    if (businessRegistration.value?.afterAction && businessContext.value) {
      await businessRegistration.value.afterAction(businessContext.value, action, result);
    }
    ElMessage.success(action === 'save' ? '暂存成功' : `审批已${actionName}`);
    if (action === 'save') {
      await loadDetail();
    } else {
      await router.push(action === 'claim' || action === 'unclaim' ? '/workflow/task/todo' : '/workflow/task/done');
    }
  } catch (error) {
    if (!isKnownRequestError(error)) {
      ElMessage.error(error instanceof Error ? error.message : `${actionName}失败`);
    }
  } finally {
    submitting.value = false;
    submittingAction.value = '';
  }
}

async function collectExtraActionPayload(action: WorkflowTaskActionKey) {
  if (action === 'transfer') {
    const targetUserId = await pickTransferUser();
    return targetUserId ? { targetUserId } : false;
  }
  if (action === 'addSign') {
    const targetUserIds = await pickAddSignUsers();
    return targetUserIds.length ? { targetUserIds } : false;
  }
  return {};
}

function pickTransferUser(): Promise<string | false> {
  return pickUsers('transfer');
}

async function pickAddSignUsers(): Promise<string[]> {
  const result = await pickUsers('addSign');
  return Array.isArray(result) ? result : [];
}

function pickUsers(action: 'transfer'): Promise<string | false>;
function pickUsers(action: 'addSign'): Promise<string[] | false>;
function pickUsers(action: 'transfer' | 'addSign') {
  selectorDialog.value = {
    visible: true,
    action,
    targetUserId: '',
    targetUserIds: [],
  };
  return new Promise<string | string[] | false>((resolve) => {
    pendingUserResolve = resolve;
  });
}

let pendingUserResolve: ((value: string | string[] | false) => void) | null = null;

function cancelUserSelection() {
  if (!pendingUserResolve) {
    return;
  }
  selectorDialog.value.visible = false;
  pendingUserResolve?.(false);
  pendingUserResolve = null;
}

function confirmUserSelection() {
  if (selectorDialog.value.action === 'transfer') {
    const target = selectorDialog.value.targetUserId.trim();
    if (!target) {
      ElMessage.warning('请选择目标办理人');
      return;
    }
    selectorDialog.value.visible = false;
    pendingUserResolve?.(target);
    pendingUserResolve = null;
    return;
  }
  const targets = selectorDialog.value.targetUserIds.map(item => item.trim()).filter(Boolean);
  if (!targets.length) {
    ElMessage.warning('请选择加签人');
    return;
  }
  selectorDialog.value.visible = false;
  pendingUserResolve?.(targets);
  pendingUserResolve = null;
}

async function executeTaskAction(
  action: WorkflowTaskActionKey,
  taskId: string,
  comment: string,
  variables: Record<string, any>,
  extraPayload: any,
) {
  if (action === 'complete') {
    return workflowApi.completeTask({ taskId, comment, variables });
  }
  if (action === 'reject') {
    return workflowApi.rejectTask({ taskId, comment, variables });
  }
  if (action === 'save') {
    return workflowApi.saveTask({ taskId, comment, variables });
  }
  if (action === 'transfer') {
    return workflowApi.transferTask({ taskId, comment, targetUserId: extraPayload.targetUserId });
  }
  if (action === 'addSign') {
    return workflowApi.addSignTask({ taskId, comment, targetUserIds: extraPayload.targetUserIds });
  }
  if (action === 'claim') {
    return workflowApi.claimTask(taskId);
  }
  if (action === 'unclaim') {
    return workflowApi.unclaimTask(taskId);
  }
  throw new Error(`Unsupported workflow action: ${action}`);
}

function editableFormVariables(action: WorkflowTaskActionKey) {
  const current = detail.value?.variables || {};
  const permissions = effectiveFormPermissions.value;
  const values = runtimeFields.value.reduce<Record<string, any>>((result, field) => {
    if (permissions[field.key] === 'EDITABLE') {
      result[field.key] = current[field.key];
    }
    return result;
  }, {});
  return {
    ...values,
    ...collectBusinessApprovalVariables(businessRegistration.value, businessContext.value, action),
  };
}

function fillDefaultApprovalPermissions(fields: RuntimeFormField[], permissions: Record<string, string>) {
  fields.forEach((field) => {
    if (field.children?.length) {
      fillDefaultApprovalPermissions(field.children, permissions);
    }
    if (!field.key || field.key.startsWith('__runtime_')) {
      return;
    }
    if (!permissions[field.key]) {
      permissions[field.key] = 'READONLY';
    }
  });
}

function collectActionComment(action: WorkflowTaskActionKey) {
  if (commentMode.value === 'NONE') {
    return '';
  }
  return collectBusinessApprovalComment(businessRegistration.value, businessContext.value, action, actionForm.value.comment);
}

function isKnownRequestError(error: unknown) {
  return Boolean(error && typeof error === 'object' && 'response' in error);
}

function backToList() {
  const mode = String(route.query.from || '');
  if (mode === 'initiated') {
    router.push('/workflow/task/initiated');
  } else if (mode === 'done') {
    router.push('/workflow/task/done');
  } else {
    router.push('/workflow/task/todo');
  }
}

function formatJson(value: any) {
  return JSON.stringify(value || {}, null, 2);
}

function firstCurrentTaskDefinitionKey() {
  return String(detail.value?.records?.find(record => record.taskDefinitionKey)?.taskDefinitionKey || '');
}

function latestTaskDefinitionKey() {
  const records = detail.value?.records || [];
  return String([...records].reverse().find(record => record.taskDefinitionKey)?.taskDefinitionKey || '');
}

function latestTaskName() {
  const records = detail.value?.records || [];
  return String([...records].reverse().find(record => record.taskName)?.taskName || '');
}

watch(
  () => [route.query.taskId, route.query.processInstanceId, route.query.mode],
  loadDetail,
  { immediate: true },
);
</script>

<style scoped>
.workflow-task-detail-page {
  padding: 0;
}

.task-detail-shell :deep(.el-card__body) {
  background: var(--el-fill-color-extra-light);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
}

.page-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.task-main {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.task-section,
.record-panel {
  padding: 18px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.section-header,
.record-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-header.compact {
  margin-bottom: 12px;
}

.section-header h3,
.record-header h3 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.section-header p,
.record-header span {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-width: 0;
}

.summary-item span,
.summary-item strong {
  display: block;
}

.summary-item span {
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.summary-item strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-item.muted strong {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 400;
}

.process-summary {
  border-color: transparent;
}

.detail-alert {
  margin-top: 12px;
}

.json-preview {
  max-height: 240px;
  padding: 12px;
  overflow: auto;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #475569;
  white-space: pre-wrap;
}

.approval-section {
  padding-bottom: 78px;
}

.approval-action-bar {
  position: sticky;
  right: 0;
  bottom: 0;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: -78px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: color-mix(in srgb, var(--el-bg-color) 92%, transparent);
  box-shadow: 0 -8px 24px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(8px);
}

.record-panel {
  position: sticky;
  top: 12px;
  max-height: calc(100vh - 150px);
  overflow: auto;
}

.record-timeline {
  padding-left: 4px;
}

.record-timeline :deep(.el-timeline-item__tail) {
  border-left-color: var(--el-color-primary-light-7);
}

.record-timeline :deep(.el-timeline-item__node) {
  background: var(--el-color-primary);
}

.record-title {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.record-meta {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.record-comment {
  margin-top: 8px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

@media (max-width: 1180px) {
  .task-detail-layout {
    grid-template-columns: 1fr;
  }

  .record-panel {
    position: static;
    max-height: none;
  }
}

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
