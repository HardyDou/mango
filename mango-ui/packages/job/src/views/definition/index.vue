<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>任务定义</h2>
          <p>维护 Mango 原生任务契约，底层调度由已配置引擎执行。</p>
        </div>
        <el-button v-auth="'job:definition:add'" type="primary" :icon="Plus" @click="openEditor()">新增任务</el-button>
      </div>

      <el-form :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="关键字" class="job-search-item job-search-item-wide">
          <el-input v-model="query.keyword" clearable placeholder="编码/名称/处理器" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="应用" class="job-search-item">
          <el-input v-model="query.appCode" clearable placeholder="appCode" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="状态" class="job-search-item job-search-item-small">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option v-for="item in jobDefinitionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="job-search-actions">
          <el-button v-auth="'job:definition:list'" type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button text type="primary" @click="advancedVisible = !advancedVisible">
            {{ advancedVisible ? '收起' : '更多筛选' }}
            <el-icon class="job-more-icon" :class="{ 'is-open': advancedVisible }"><ArrowDown /></el-icon>
          </el-button>
        </el-form-item>
        <div v-show="advancedVisible" class="job-search-more">
          <el-form-item label="任务类型" class="job-search-item">
            <el-select v-model="query.jobType" clearable placeholder="全部">
              <el-option v-for="item in jobTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="调度类型" class="job-search-item">
            <el-select v-model="query.scheduleType" clearable placeholder="全部">
              <el-option v-for="item in scheduleTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="引擎" class="job-search-item job-search-item-small">
            <el-select v-model="query.engineType" clearable placeholder="全部">
              <el-option v-for="item in engineTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
    </section>

    <section class="job-panel">
      <el-alert v-if="errorMessage" class="job-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="loadRows">重试</el-button>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无任务定义">
        <el-table-column label="任务" min-width="280" fixed="left">
          <template #default="{ row }">
            <div class="job-name-cell">
              <strong>{{ row.jobName }}</strong>
              <span>{{ row.jobCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="appCode" label="应用" min-width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="optionTagType(jobDefinitionStatusOptions, row.status)" size="small">
              {{ optionLabel(jobDefinitionStatusOptions, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调度" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ optionLabel(scheduleTypeOptions, row.scheduleType) }}
            <span v-if="row.scheduleExpression" class="job-muted"> / {{ row.scheduleExpression }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理器" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handlerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="引擎同步" width="112">
          <template #default="{ row }">
            <el-tooltip v-if="row.syncError" :content="row.syncError" placement="top">
              <el-tag :type="optionTagType(syncStatusOptions, row.syncStatus)" size="small">
                {{ optionLabel(syncStatusOptions, row.syncStatus) }}
              </el-tag>
            </el-tooltip>
            <el-tag v-else :type="optionTagType(syncStatusOptions, row.syncStatus)" size="small">
              {{ optionLabel(syncStatusOptions, row.syncStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <div class="job-actions">
              <el-button v-auth="'job:definition:edit'" link type="primary" :icon="Edit" @click="openEditor(row)">编辑</el-button>
              <el-button
                v-for="action in statusActions(row.status)"
                :key="action.status"
                v-auth="'job:definition:status'"
                link
                type="primary"
                @click="changeStatus(row, action.status)"
              >
                {{ action.label }}
              </el-button>
              <el-button v-auth="'job:definition:trigger'" link type="success" :icon="VideoPlay" :disabled="row.status === 'DRAFT' || row.status === 'DISABLED'" @click="openTrigger(row)">触发</el-button>
              <el-button v-auth="'job:definition:delete'" link type="danger" :icon="Delete" :disabled="row.status !== 'DRAFT'" @click="deleteRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="job-pagination">
        <Pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadRows" />
      </div>
    </section>

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑任务' : '新增任务'" width="780px" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="112px">
        <div class="job-form-section-title">基本信息</div>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="所属应用" prop="appCode">
              <el-input v-model="form.appCode" placeholder="例如 mango-admin" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务编码" prop="jobCode">
              <el-input v-model="form.jobCode" :disabled="Boolean(form.id)" placeholder="例如 dailyReport" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="任务展示名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="底层引擎" prop="engineType">
              <el-select v-model="form.engineType" style="width: 100%">
                <el-option v-for="item in engineTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="job-form-section-title">执行配置</div>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="任务类型" prop="jobType">
              <el-select v-model="form.jobType" style="width: 100%">
                <el-option v-for="item in jobTypeOptions.filter(item => item.value !== 'SCRIPT')" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处理器" prop="handlerName">
              <el-input v-model="form.handlerName" placeholder="内置/远程任务必填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调度类型" prop="scheduleType">
              <el-select v-model="form.scheduleType" style="width: 100%">
                <el-option v-for="item in scheduleTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调度表达式" prop="scheduleExpression">
              <el-input v-model="form.scheduleExpression" :placeholder="form.scheduleType === 'MANUAL' ? '手动任务可为空' : 'Cron/秒/时间表达式'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="超时秒数">
              <el-input-number v-model="form.timeoutSeconds" :min="1" :max="86400" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="并发策略">
              <el-input v-model="form.concurrencyPolicy" placeholder="例如 SERIAL/DISCARD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="错过策略">
              <el-input v-model="form.misfireStrategy" placeholder="例如 IGNORE/FIRE_ONCE" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="job-form-section-title">参数配置</div>
        <el-form-item label="参数 Schema">
          <el-input v-model="form.paramSchema" type="textarea" :rows="4" placeholder="JSON Schema，可为空" />
        </el-form-item>
        <el-form-item label="默认参数">
          <el-input v-model="form.paramValue" type="textarea" :rows="4" placeholder="JSON，可为空" />
        </el-form-item>
        <el-form-item label="重试策略">
          <el-input v-model="form.retryPolicy" type="textarea" :rows="3" placeholder="JSON，可为空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="triggerVisible" title="手动触发" width="560px" destroy-on-close append-to-body>
      <el-form label-width="96px">
        <el-form-item label="任务">
          <span>{{ triggerTarget?.jobName }} / {{ triggerTarget?.jobCode }}</span>
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="triggerForm.triggerBatchNo" clearable placeholder="为空时由服务端生成" />
        </el-form-item>
        <el-form-item label="触发参数">
          <el-input v-model="triggerForm.paramValue" type="textarea" :rows="5" placeholder="JSON，可为空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerVisible = false">取消</el-button>
        <el-button type="primary" :loading="triggering" @click="triggerRow">触发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, Delete, Edit, Plus, Refresh, Search, VideoPlay } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  engineTypeOptions,
  jobApi,
  jobDefinitionStatusOptions,
  jobTypeOptions,
  optionLabel,
  optionTagType,
  requestErrorMessage,
  scheduleTypeOptions,
  syncStatusOptions,
  type JobDefinition,
  type JobDefinitionQuery,
  type JobDefinitionStatus,
  type SaveJobDefinitionPayload,
} from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const saving = ref(false);
const triggering = ref(false);
const errorMessage = ref('');
const advancedVisible = ref(false);
const rows = ref<JobDefinition[]>([]);
const total = ref(0);
const editorVisible = ref(false);
const triggerVisible = ref(false);
const triggerTarget = ref<JobDefinition>();
const formRef = ref<FormInstance>();

const query = reactive<JobDefinitionQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  appCode: '',
  status: '',
  jobType: '',
  scheduleType: '',
  engineType: '',
});

const form = reactive<SaveJobDefinitionPayload>({
  appCode: '',
  jobCode: '',
  jobName: '',
  jobType: 'BUILTIN',
  scheduleType: 'CRON',
  scheduleExpression: '',
  handlerName: '',
  paramSchema: '',
  paramValue: '',
  misfireStrategy: '',
  concurrencyPolicy: '',
  timeoutSeconds: 300,
  retryPolicy: '',
  engineType: 'POWERJOB',
});

const triggerForm = reactive({
  triggerBatchNo: '',
  paramValue: '',
});

const rules: FormRules = {
  appCode: [{ required: true, message: '请输入所属应用', trigger: 'blur' }],
  jobCode: [{ required: true, message: '请输入任务编码', trigger: 'blur' }],
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  jobType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  scheduleType: [{ required: true, message: '请选择调度类型', trigger: 'change' }],
  engineType: [{ required: true, message: '请选择底层引擎', trigger: 'change' }],
};

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const page = await jobApi.pageDefinitions(query);
    rows.value = page.list;
    total.value = page.total;
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, '任务定义加载失败');
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    appCode: '',
    status: '',
    jobType: '',
    scheduleType: '',
    engineType: '',
  });
  loadRows();
}

function openEditor(row?: JobDefinition) {
  Object.assign(form, {
    id: row?.id,
    appCode: row?.appCode || '',
    jobCode: row?.jobCode || '',
    jobName: row?.jobName || '',
    jobType: row?.jobType || 'BUILTIN',
    scheduleType: row?.scheduleType || 'CRON',
    scheduleExpression: row?.scheduleExpression || '',
    handlerName: row?.handlerName || '',
    paramSchema: row?.paramSchema || '',
    paramValue: row?.paramValue || '',
    misfireStrategy: row?.misfireStrategy || '',
    concurrencyPolicy: row?.concurrencyPolicy || '',
    timeoutSeconds: row?.timeoutSeconds || 300,
    retryPolicy: row?.retryPolicy || '',
    engineType: row?.engineType || 'POWERJOB',
  });
  editorVisible.value = true;
}

async function saveRow() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.id) {
      await jobApi.updateDefinition(form);
      ElMessage.success('任务已更新');
    } else {
      await jobApi.createDefinition(form);
      ElMessage.success('任务已创建');
    }
    editorVisible.value = false;
    await loadRows();
  } finally {
    saving.value = false;
  }
}

function statusActions(status?: JobDefinitionStatus) {
  if (status === 'DRAFT') {
    return [{ label: '启用', status: 'ENABLED' as JobDefinitionStatus }, { label: '禁用', status: 'DISABLED' as JobDefinitionStatus }];
  }
  if (status === 'ENABLED') {
    return [{ label: '暂停', status: 'PAUSED' as JobDefinitionStatus }, { label: '禁用', status: 'DISABLED' as JobDefinitionStatus }];
  }
  if (status === 'PAUSED') {
    return [{ label: '启用', status: 'ENABLED' as JobDefinitionStatus }, { label: '禁用', status: 'DISABLED' as JobDefinitionStatus }];
  }
  if (status === 'DISABLED') {
    return [{ label: '启用', status: 'ENABLED' as JobDefinitionStatus }, { label: '退回草稿', status: 'DRAFT' as JobDefinitionStatus }];
  }
  return [];
}

async function changeStatus(row: JobDefinition, status: JobDefinitionStatus) {
  await ElMessageBox.confirm(`确认将任务「${row.jobName}」调整为「${optionLabel(jobDefinitionStatusOptions, status)}」？`, '调整状态', {
    type: 'warning',
  });
  await jobApi.updateDefinitionStatus(row.id!, status);
  ElMessage.success('状态已更新');
  await loadRows();
}

async function deleteRow(row: JobDefinition) {
  await ElMessageBox.confirm(`确认删除任务「${row.jobName}」？`, '删除任务', { type: 'warning' });
  await jobApi.deleteDefinition(row.id!);
  ElMessage.success('任务已删除');
  await loadRows();
}

function openTrigger(row: JobDefinition) {
  triggerTarget.value = row;
  triggerForm.triggerBatchNo = '';
  triggerForm.paramValue = row.paramValue || '';
  triggerVisible.value = true;
}

async function triggerRow() {
  if (!triggerTarget.value?.id) {
    return;
  }
  triggering.value = true;
  try {
    const instanceId = await jobApi.triggerDefinition({
      jobId: triggerTarget.value.id,
      triggerBatchNo: triggerForm.triggerBatchNo,
      paramValue: triggerForm.paramValue,
    });
    ElMessage.success(`已触发，实例ID：${instanceId}`);
    triggerVisible.value = false;
  } finally {
    triggering.value = false;
  }
}
</script>
