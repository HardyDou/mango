<template>
  <div class="workflow-task-detail-page">
    <el-card v-loading="loading" class="task-detail-shell">
      <el-empty v-if="!detail" description="暂无流程详情" />

      <WorkflowLayout
        v-else
        :title="detail.process.processName || '流程详情'"
        @back="backToList"
      >
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

        <template #sidebar>
          <WorkflowSidebar
            :summary="workflowSummary"
            :node="workflowDefinitionNode"
            :current-node-key="workflowCurrentNodeKey"
            :visited-node-keys="workflowVisitedNodeKeys"
            :status="workflowStatus"
            :records="detail.records"
            :business-type="businessType"
            :business-key="workflowBusinessKey"
            :mode="sidebarMode"
          >
            <template #default>
              <div v-if="customRecordPanelComponent && showRecordPanel" class="aside-records">
                <div class="record-header">
                  <h3>审批信息</h3>
                  <span>{{ detail.records.length }} 条</span>
                </div>
                <component
                  :is="customRecordPanelComponent"
                  v-if="businessContext"
                  :context="businessContext"
                />
              </div>
            </template>
          </WorkflowSidebar>
        </template>
      </WorkflowLayout>
    </el-card>

    <div v-if="!readonlyMode && detail" class="approval-action-bar">
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
import { parseDesignerJson, workflowApi, type WorkflowBusinessApply, type WorkflowDesignerNode, type WorkflowDefinitionVersion, type WorkflowProcessDetail, type WorkflowTaskActionKey, type WorkflowTaskDetail } from '../../api/workflow';
import {
  applyIdOf,
  businessPermissionsOf,
  businessTypeOf,
  collectBusinessApprovalComment,
  collectBusinessApprovalVariables,
  resolveBusinessApprovalRegistration,
  type BusinessApprovalContext,
} from '../../components/businessApproval';
import WorkflowLayout from '../../components/business-ui/WorkflowLayout.vue';
import WorkflowSidebar from '../../components/business-ui/WorkflowSidebar.vue';
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
const workflowDefinitionNode = ref<WorkflowDesignerNode | null>(null);
const runtimeFields = ref<RuntimeFormField[]>([]);
const unsupportedFields = ref<Array<{ label: string; type: string }>>([]);
const actionForm = ref({ comment: '' });
let detailLoadSeq = 0;
const selectorDialog = ref({
  visible: false,
  action: '' as 'transfer' | 'addSign' | '',
  targetUserId: '',
  targetUserIds: [] as string[],
});

const readonlyMode = computed(() => route.query.mode === 'view' || !route.query.taskId);
const detail = computed(() => taskDetail.value || (processDetail.value ? {
  task: null,
  process: processDetail.value.process,
  formCode: processDetail.value.formCode,
  formJson: processDetail.value.formJson,
  variables: processDetail.value.variables,
  records: processDetail.value.records,
  formPermissions: processDetail.value.renderConfig?.formPermissions || {},
  renderConfig: processDetail.value.renderConfig,
} as unknown as WorkflowTaskDetail : null));
const currentTaskDefinitionKey = computed(() =>
  detail.value?.task?.taskDefinitionKey || (detail.value?.task?.id ? firstCurrentTaskDefinitionKey() : latestTaskDefinitionKey()),
);
const renderConfig = computed(() => detail.value?.renderConfig);
const businessType = computed(() => renderConfig.value?.businessType || businessApply.value?.businessType || businessTypeOf(detail.value?.variables));
const formConfig = computed(() => parseWorkflowFormConfig(detail.value?.formJson));
const renderMode = computed(() => formConfig.value.mode === 'CUSTOM_PAGE'
  ? 'CUSTOM_PAGE'
  : renderConfig.value?.renderMode || businessApply.value?.renderMode || 'DYNAMIC_FORM');
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
const workflowSummary = computed(() => ({
  currentNodeName: detail.value?.task?.taskName || latestTaskName(),
  status: detail.value?.process.status || '',
  initiatorName: detail.value?.process.initiatorName || '-',
  assigneeName: detail.value?.task?.assigneeName || '-',
  startTime: detail.value?.process.startTime || '-',
}));
const workflowDefinitionNodeComputed = computed(() => workflowDefinitionNode.value);
const workflowCurrentNodeKey = computed(() => currentTaskDefinitionKey.value);
const workflowVisitedNodeKeys = computed(() => {
  const keys = detail.value?.records?.map(record => record.taskDefinitionKey).filter(Boolean) as string[] | undefined;
  return Array.from(new Set(keys || []));
});
const workflowStatus = computed(() => detail.value?.process.status || '');
const workflowBusinessKey = computed(() => String(renderConfig.value?.businessKey || businessApply.value?.businessKey || detail.value?.process.businessKey || detail.value?.task?.businessKey || ''));
const sidebarMode = computed(() => {
  if (!showRecordPanel.value) return 'HIDDEN';
  if (customRecordPanelComponent.value) return 'CUSTOM';
  if (workflowDefinitionNodeComputed.value) return 'PROGRESS';
  return 'APPROVAL_RECORDS';
});

async function loadDetail() {
  const seq = ++detailLoadSeq;
  loading.value = true;
  try {
    taskDetail.value = null;
    processDetail.value = null;
    businessApply.value = null;
    workflowDefinitionNode.value = null;
    runtimeFields.value = [];
    unsupportedFields.value = [];
    actionForm.value.comment = '';
    const taskId = String(route.query.taskId || '');
    const processInstanceId = String(route.query.processInstanceId || '');
    if (taskId) {
      const nextTaskDetail = await workflowApi.taskDetail(taskId);
      if (seq !== detailLoadSeq) return;
      taskDetail.value = nextTaskDetail;
      processDetail.value = null;
    } else if (processInstanceId) {
      const nextProcessDetail = await workflowApi.processDetail(processInstanceId);
      if (seq !== detailLoadSeq) return;
      processDetail.value = nextProcessDetail;
      taskDetail.value = null;
    } else {
      ElMessage.warning('缺少任务ID或流程实例ID');
      return;
    }
    await loadBusinessApply();
    if (seq !== detailLoadSeq) return;
    const parsed = parseRuntimeForm(detail.value?.formJson);
    runtimeFields.value = parsed.fields;
    unsupportedFields.value = parsed.unsupported;
    void loadWorkflowDefinitionTree(seq);
  } finally {
    if (seq === detailLoadSeq) {
      loading.value = false;
    }
  }
}

async function loadBusinessApply() {
  const processInstanceId = detail.value?.process.processInstanceId;
  if (!processInstanceId) {
    businessApply.value = null;
    return;
  }
  try {
    businessApply.value = await workflowApi.businessApplyByProcessInstance(processInstanceId);
  } catch {
    businessApply.value = null;
  }
}

async function loadWorkflowDefinitionTree(seq = detailLoadSeq) {
  const definition = await resolveWorkflowDefinitionForGraph();
  if (seq !== detailLoadSeq) {
    return;
  }
  if (!definition?.designerJson) {
    workflowDefinitionNode.value = null;
    return;
  }
  workflowDefinitionNode.value = parseDesignerJson(definition.designerJson);
}

async function resolveWorkflowDefinitionForGraph() {
  const definitionId = resolveWorkflowDefinitionId();
  try {
    if (definitionId) {
      const versions = await workflowApi.definitionVersions(definitionId);
      const matchedVersion = resolveWorkflowDefinitionVersion(versions, detail.value?.process.processDefinitionId, detail.value?.process.processInstanceId);
      if (matchedVersion?.designerJson) {
        return matchedVersion;
      }
      return await workflowApi.definitionDetail(definitionId);
    }
    return await resolveWorkflowDefinitionByProcessKey();
  } catch {
    return await resolveWorkflowDefinitionByProcessKey();
  }
}

function resolveWorkflowDefinitionId() {
  const candidates = [
    detail.value?.process.definitionId,
    businessApply.value?.processDefinitionId,
  ];
  return candidates.map(item => String(item || '').trim()).find(isBackendDefinitionId) || '';
}

function isBackendDefinitionId(value: string) {
  return /^\d+$/.test(value);
}

async function resolveWorkflowDefinitionByProcessKey() {
  const processKey = String(detail.value?.process.processKey || businessApply.value?.processDefinitionKey || '').trim();
  if (!processKey) {
    return null;
  }
  const page = await workflowApi.definitionsPage({ keyword: processKey, publishedOnly: true, pageSize: 20 });
  return page.list.find(item => item.definitionKey === processKey) || page.list[0] || null;
}

function resolveWorkflowDefinitionVersion(
  versions: WorkflowDefinitionVersion[],
  processDefinitionId?: string,
  processInstanceId?: string,
) {
  if (!Array.isArray(versions) || !versions.length) {
    return null;
  }
  if (processDefinitionId) {
    const byProcessDefinition = versions.find(version => version.processDefinitionId === processDefinitionId);
    if (byProcessDefinition) {
      return byProcessDefinition;
    }
  }
  if (processInstanceId) {
    const byProcessInstance = versions.find(version => version.id === processInstanceId);
    if (byProcessInstance) {
      return byProcessInstance;
    }
  }
  return versions[0] || null;
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
    const result = await executeTaskAction(action, taskId, comment, variables, extraPayload);
    if (businessRegistration.value?.afterAction && businessContext.value) {
      await businessRegistration.value.afterAction(businessContext.value, action, result);
    }
    ElMessage.success(action === 'save' ? '暂存成功' : `审批已${actionName}`);
    if (action === 'save') {
      await loadDetail();
    } else {
      await router.push(resolveBusinessReturnLocation() || defaultActionReturnLocation(action));
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
  selectorDialog.value = { visible: true, action, targetUserId: '', targetUserIds: [] };
  return new Promise<string | string[] | false>((resolve) => {
    pendingUserResolve = resolve;
  });
}

let pendingUserResolve: ((value: string | string[] | false) => void) | null = null;

function cancelUserSelection() {
  if (!pendingUserResolve) return;
  selectorDialog.value.visible = false;
  pendingUserResolve(false);
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
  if (action === 'complete') return workflowApi.completeTask({ taskId, comment, variables });
  if (action === 'reject') return workflowApi.rejectTask({ taskId, comment, variables });
  if (action === 'save') return workflowApi.saveTask({ taskId, comment, variables });
  if (action === 'transfer') return workflowApi.transferTask({ taskId, comment, targetUserId: extraPayload.targetUserId });
  if (action === 'addSign') return workflowApi.addSignTask({ taskId, comment, targetUserIds: extraPayload.targetUserIds });
  if (action === 'claim') return workflowApi.claimTask(taskId);
  if (action === 'unclaim') return workflowApi.unclaimTask(taskId);
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
    if (!field.key || field.key.startsWith('__runtime_')) return;
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
  const businessReturnLocation = resolveBusinessReturnLocation();
  if (businessReturnLocation) {
    router.push(businessReturnLocation);
    return;
  }
  router.push(defaultListReturnLocation());
}

function resolveBusinessReturnLocation() {
  const path = normalizeReturnPath(route.query.returnPath);
  if (!path) return null;
  const query = parseReturnQuery(route.query.returnQuery);
  return Object.keys(query).length ? { path, query } : path;
}

function normalizeReturnPath(value: unknown) {
  const path = firstQueryValue(value).trim();
  if (!path || !path.startsWith('/') || path.startsWith('//')) return '';
  if (path.includes('\\') || path.includes('?') || path.includes('#') || /[\u0000-\u001F\u007F]/.test(path)) return '';
  if (/^[a-z][a-z0-9+.-]*:/i.test(path)) return '';
  return path;
}

function parseReturnQuery(value: unknown) {
  const raw = firstQueryValue(value).trim();
  const query: Record<string, string | string[]> = {};
  if (!raw) return query;
  new URLSearchParams(raw.startsWith('?') ? raw.slice(1) : raw).forEach((itemValue, key) => {
    if (!key) return;
    const existing = query[key];
    if (Array.isArray(existing)) {
      existing.push(itemValue);
    } else if (typeof existing === 'string') {
      query[key] = [existing, itemValue];
    } else {
      query[key] = itemValue;
    }
  });
  return query;
}

function firstQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] || '');
  }
  return String(value || '');
}

function defaultActionReturnLocation(action: WorkflowTaskActionKey) {
  return action === 'claim' || action === 'unclaim' ? '/workflow/task/todo' : '/workflow/task/done';
}

function defaultListReturnLocation() {
  const mode = String(route.query.from || '');
  if (mode === 'initiated') return '/workflow/task/initiated';
  if (mode === 'done') return '/workflow/task/done';
  return '/workflow/task/todo';
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

.task-section {
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

.approval-action-bar {
  position: sticky;
  bottom: 0;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: color-mix(in srgb, var(--el-bg-color) 92%, transparent);
  box-shadow: 0 -8px 24px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(8px);
}

.aside-records {
  padding: 18px;
  border-top: 1px solid var(--el-border-color-lighter);
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

@media (max-width: 760px) {
  .section-header {
    flex-direction: column;
  }
}
</style>
