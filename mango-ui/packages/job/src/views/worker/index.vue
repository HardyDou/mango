<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>Worker 节点</h2>
          <p>查看任务执行节点地址、所属应用、通信方式和在线状态。</p>
        </div>
        <div class="job-toolbar-actions">
          <el-button v-auth="'job:worker:add'" type="primary" :icon="Plus" @click="openCreateDialog">登记 Worker</el-button>
          <el-button v-auth="'job:worker:list'" :icon="Refresh" @click="loadRows">刷新</el-button>
        </div>
      </div>

      <el-form :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="关键字" class="job-search-item job-search-item-wide">
          <el-input v-model="query.keyword" clearable placeholder="Worker 地址/实例标识" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="应用" class="job-search-item">
          <el-input v-model="query.appCode" clearable placeholder="appCode" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="服务" class="job-search-item">
          <el-input v-model="query.serviceCode" clearable placeholder="serviceCode" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="Worker组" class="job-search-item">
          <el-input v-model="query.workerGroup" clearable placeholder="workerGroup" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="状态" class="job-search-item job-search-item-small">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option v-for="item in workerStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="运行时" class="job-search-item job-search-item-small">
          <el-select v-model="query.engineType" clearable placeholder="全部">
            <el-option v-for="item in engineTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="通信" class="job-search-item job-search-item-small">
          <el-select v-model="query.transportType" clearable placeholder="全部">
            <el-option v-for="item in transportTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="job-search-actions">
          <el-button v-auth="'job:worker:list'" type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="job-panel">
      <el-alert v-if="errorMessage" class="job-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="loadRows">重试</el-button>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无 Worker 节点">
        <el-table-column prop="workerAddress" label="Worker 地址" min-width="240" fixed="left" show-overflow-tooltip />
        <el-table-column prop="appCode" label="应用" min-width="140" show-overflow-tooltip />
        <el-table-column label="服务/组" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.serviceCode || '-' }} / {{ row.workerGroup || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="optionTagType(workerStatusOptions, row.status)" size="small">
              {{ optionLabel(workerStatusOptions, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="engineType" label="运行时" width="120" />
        <el-table-column label="通信" width="92">
          <template #default="{ row }">
            <el-tag :type="optionTagType(transportTypeOptions, row.transportType)" size="small">
              {{ optionLabel(transportTypeOptions, row.transportType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册来源" width="112">
          <template #default="{ row }">
            <el-tag :type="optionTagType(workerRegisterSourceOptions, row.registerSource)" size="small">
              {{ optionLabel(workerRegisterSourceOptions, row.registerSource) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="engineWorkerId" label="实例标识" min-width="180" show-overflow-tooltip />
        <el-table-column prop="runtimeAddress" label="运行地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="lastHeartbeatAt" label="最近心跳" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="'job:worker:status'" link type="primary" :disabled="row.status === 'DRAINING'" @click="updateStatus(row, 'DRAINING')">
              排空
            </el-button>
            <el-button v-auth="'job:worker:status'" link type="warning" :disabled="row.status === 'OFFLINE'" @click="updateStatus(row, 'OFFLINE')">
              下线
            </el-button>
            <el-button v-if="row.status !== 'DISABLED'" v-auth="'job:worker:status'" link type="danger" @click="updateStatus(row, 'DISABLED')">
              禁用
            </el-button>
            <el-button v-else v-auth="'job:worker:status'" link type="success" @click="updateStatus(row, 'ONLINE')">
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="job-pagination">
        <Pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadRows" />
      </div>
    </section>

    <el-dialog v-model="createDialogVisible" title="登记 Worker" width="640px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="所属应用" prop="appCode">
          <el-input v-model="createForm.appCode" placeholder="mango-job" />
        </el-form-item>
        <el-form-item label="执行服务">
          <el-input v-model="createForm.serviceCode" clearable placeholder="默认跟随所属应用" />
        </el-form-item>
        <el-form-item label="Worker组">
          <el-input v-model="createForm.workerGroup" clearable placeholder="默认跟随执行服务" />
        </el-form-item>
        <el-form-item label="Worker 地址" prop="workerAddress">
          <el-input v-model="createForm.workerAddress" placeholder="http://127.0.0.1:18658" />
        </el-form-item>
        <el-form-item label="实例标识">
          <el-input v-model="createForm.workerInstanceId" clearable placeholder="默认使用 Worker 地址" />
        </el-form-item>
        <el-form-item label="处理器" prop="handlerName">
          <el-input v-model="createForm.handlerName" placeholder="mangoJobRuntimeProbeHandler" />
        </el-form-item>
        <el-form-item label="参数 Schema">
          <el-input v-model="createForm.paramSchema" type="textarea" :rows="4" placeholder="{ &quot;type&quot;: &quot;object&quot; }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  engineTypeOptions,
  jobApi,
  optionLabel,
  optionTagType,
  requestErrorMessage,
  transportTypeOptions,
  workerRegisterSourceOptions,
  workerStatusOptions,
  type CreateJobWorkerPayload,
  type JobWorkerQuery,
  type JobWorkerSnapshot,
  type JobWorkerStatus,
} from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const rows = ref<JobWorkerSnapshot[]>([]);
const total = ref(0);
const createDialogVisible = ref(false);
const createFormRef = ref<FormInstance>();
const query = reactive<JobWorkerQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  appCode: '',
  serviceCode: '',
  workerGroup: '',
  status: '',
  engineType: '',
  transportType: '',
  registerSource: '',
});
const createForm = reactive({
  appCode: 'mango-job',
  serviceCode: '',
  workerGroup: '',
  workerAddress: '',
  workerInstanceId: '',
  handlerName: '',
  paramSchema: '',
});
const createRules: FormRules = {
  appCode: [{ required: true, message: '请输入所属应用', trigger: 'blur' }],
  workerAddress: [
    { required: true, message: '请输入 Worker 地址', trigger: 'blur' },
    { pattern: /^https?:\/\/.+/i, message: 'Worker 地址必须是 http(s)://', trigger: 'blur' },
  ],
  handlerName: [{ required: true, message: '请输入处理器名称', trigger: 'blur' }],
};

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const page = await jobApi.pageWorkers(query);
    rows.value = page.list;
    total.value = page.total;
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, 'Worker 加载失败');
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
    serviceCode: '',
    workerGroup: '',
    status: '',
    engineType: '',
    transportType: '',
    registerSource: '',
  });
  loadRows();
}

function openCreateDialog() {
  Object.assign(createForm, {
    appCode: 'mango-job',
    serviceCode: '',
    workerGroup: '',
    workerAddress: '',
    workerInstanceId: '',
    handlerName: '',
    paramSchema: '',
  });
  createDialogVisible.value = true;
}

async function submitCreate() {
  await createFormRef.value?.validate();
  saving.value = true;
  try {
    const payload: CreateJobWorkerPayload = {
      appCode: createForm.appCode.trim(),
      serviceCode: createForm.serviceCode.trim() || undefined,
      workerGroup: createForm.workerGroup.trim() || undefined,
      workerAddress: createForm.workerAddress.trim(),
      transportType: 'HTTP_INTERNAL',
      workerInstanceId: createForm.workerInstanceId.trim() || undefined,
      handlers: [{
        appCode: createForm.appCode.trim(),
        serviceCode: createForm.serviceCode.trim() || undefined,
        workerGroup: createForm.workerGroup.trim() || undefined,
        handlerName: createForm.handlerName.trim(),
        jobType: 'BUILTIN',
        paramSchema: createForm.paramSchema.trim() || undefined,
      }],
    };
    await jobApi.createWorker(payload);
    ElMessage.success('Worker 已登记');
    createDialogVisible.value = false;
    loadRows();
  } catch (error: unknown) {
    ElMessage.error(requestErrorMessage(error, 'Worker 登记失败'));
  } finally {
    saving.value = false;
  }
}

async function updateStatus(row: JobWorkerSnapshot, status: JobWorkerStatus) {
  if (!row.id) {
    return;
  }
  const label = optionLabel(workerStatusOptions, status);
  await ElMessageBox.confirm(`确认将 Worker 调整为“${label}”？`, '调整 Worker 状态', {
    type: status === 'DISABLED' || status === 'OFFLINE' ? 'warning' : 'info',
  });
  try {
    await jobApi.updateWorkerStatus({ id: row.id, status });
    ElMessage.success('Worker 状态已更新');
    loadRows();
  } catch (error: unknown) {
    ElMessage.error(requestErrorMessage(error, 'Worker 状态更新失败'));
  }
}
</script>
