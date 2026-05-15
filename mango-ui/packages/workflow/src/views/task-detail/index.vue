<template>
  <div class="workflow-task-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <div>
            <div class="page-title">{{ readonlyMode ? '流程详情' : '处理任务' }}</div>
            <div class="page-subtitle">{{ detail?.process.processName || '-' }}</div>
          </div>
          <div class="header-actions">
            <el-tag v-if="detail?.process.status">{{ detail.process.status }}</el-tag>
            <el-button @click="backToList">返回</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!detail" description="暂无流程详情" />

      <template v-else>
        <el-descriptions :column="2" border class="detail-section">
          <el-descriptions-item label="流程名称">{{ detail.process.processName }}</el-descriptions-item>
          <el-descriptions-item label="流程编码">{{ detail.process.processKey }}</el-descriptions-item>
          <el-descriptions-item label="业务主键">{{ detail.process.businessKey || detail.task?.businessKey || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ detail.process.initiatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前任务">{{ detail.task?.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="办理人">{{ detail.task?.assigneeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.process.startTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.process.endTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" class="detail-section">
          <template #header>
            <span>表单数据</span>
          </template>
          <RuntimeFormRenderer
            v-if="runtimeFields.length"
            :fields="runtimeFields"
            :model="detail.variables"
            :readonly="readonlyMode"
            :permissions="detail.formPermissions"
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
        </el-card>

        <el-card shadow="never" class="detail-section">
          <template #header>
            <span>审批记录</span>
          </template>
          <el-timeline v-if="detail.records.length">
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
        </el-card>

        <el-card v-if="!readonlyMode" shadow="never" class="detail-section">
          <template #header>
            <span>审批处理</span>
          </template>
          <el-form label-width="96px">
            <el-form-item label="审批意见">
              <el-input v-model="actionForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
            </el-form-item>
            <el-form-item label="审批变量">
              <el-input v-model="actionForm.variablesJson" type="textarea" :rows="5" placeholder='例如：{"approved":true}' />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submitAction('complete')">通过</el-button>
              <el-button type="danger" :loading="submitting" @click="submitAction('reject')">拒绝</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { workflowApi, type WorkflowProcessDetail, type WorkflowTaskDetail } from '../../api/workflow';
import RuntimeFormRenderer from '../../components/RuntimeFormRenderer.vue';
import { parseRuntimeForm, type RuntimeFormField } from '../../components/runtimeForm';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const taskDetail = ref<WorkflowTaskDetail | null>(null);
const processDetail = ref<WorkflowProcessDetail | null>(null);
const runtimeFields = ref<RuntimeFormField[]>([]);
const unsupportedFields = ref<Array<{ label: string; type: string }>>([]);
const actionForm = ref({
  comment: '',
  variablesJson: '{}',
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
  } as unknown as WorkflowTaskDetail;
});

async function loadDetail() {
  loading.value = true;
  try {
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
    const parsed = parseRuntimeForm(detail.value?.formJson);
    runtimeFields.value = parsed.fields;
    unsupportedFields.value = parsed.unsupported;
  } finally {
    loading.value = false;
  }
}

async function submitAction(action: 'complete' | 'reject') {
  const taskId = String(route.query.taskId || '');
  if (!taskId) {
    ElMessage.warning('缺少任务ID');
    return;
  }
  let variables: Record<string, any> = {};
  try {
    variables = actionForm.value.variablesJson.trim() ? JSON.parse(actionForm.value.variablesJson) : {};
  } catch {
    ElMessage.error('审批变量必须是合法 JSON');
    return;
  }
  if (!variables || Array.isArray(variables) || typeof variables !== 'object') {
    ElMessage.error('审批变量 JSON 必须是对象');
    return;
  }
  const actionName = action === 'complete' ? '通过' : '拒绝';
  await ElMessageBox.confirm(`确认${actionName}当前任务？`, `审批${actionName}`, { type: action === 'complete' ? 'warning' : 'error' });
  submitting.value = true;
  try {
    variables = {
      ...editableFormVariables(),
      ...variables,
    };
    if (action === 'complete') {
      await workflowApi.completeTask({ taskId, comment: actionForm.value.comment, variables });
    } else {
      await workflowApi.rejectTask({ taskId, comment: actionForm.value.comment, variables });
    }
    ElMessage.success(`审批已${actionName}`);
    await router.push('/workflow/task/done');
  } finally {
    submitting.value = false;
  }
}

function editableFormVariables() {
  const current = detail.value?.variables || {};
  const permissions = detail.value?.formPermissions || {};
  return runtimeFields.value.reduce<Record<string, any>>((values, field) => {
    if (permissions[field.key] === 'EDITABLE') {
      values[field.key] = current[field.key];
    }
    return values;
  }, {});
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

onMounted(loadDetail);
</script>

<style scoped>
.workflow-task-detail-page {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  color: #111827;
  font-size: 18px;
  font-weight: 600;
}

.page-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-section {
  margin-bottom: 16px;
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

.record-title {
  color: #1f2937;
  font-weight: 600;
}

.record-meta {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.record-comment {
  margin-top: 8px;
  color: #374151;
}
</style>
