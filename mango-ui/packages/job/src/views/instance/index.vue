<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <MangoListPage class="job-page" data-page="job.instance">
    <template #search>
      <MangoSearchPanel :model="query" :columns="4" @search="loadRows" @reset="resetQuery">
        <el-form-item label="任务">
          <el-select
            v-model="query.jobId"
            clearable
            filterable
            remote
            remote-show-suffix
            reserve-keyword
            :loading="definitionLoading"
            :remote-method="searchDefinitions"
            placeholder="任务名称/编码"
          >
            <el-option
              v-for="item in definitionOptions"
              :key="String(item.id)"
              :label="definitionOptionLabel(item)"
              :value="item.id || ''"
            >
              <div class="job-option-main">{{ item.jobName || item.jobCode }}</div>
              <div class="job-option-sub">{{ item.jobCode }}</div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option
              v-for="item in instanceStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="触发类型">
          <el-select v-model="query.triggerType" clearable placeholder="全部">
            <el-option v-for="item in triggerTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="query.triggerBatchNo" clearable placeholder="triggerBatchNo" @keyup.enter="loadRows" />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button v-auth="'job:instance:sync'" plain :loading="syncing" @click="syncRows">同步实例</el-button>
        <el-button v-auth="'job:instance:list'" plain :icon="Refresh" @click="loadRows">刷新</el-button>
      </template>
      <el-alert v-if="errorMessage" class="job-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="loadRows">重试</el-button>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无执行实例">
        <el-table-column label="任务" min-width="240" fixed="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="job-name-cell">
              <strong>{{ row.jobName || row.jobCode || `任务 ${row.jobId || '-'}` }}</strong>
              <span>{{ row.jobCode || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="optionTagType(instanceStatusOptions, row.status)" size="small">
              {{ optionLabel(instanceStatusOptions, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ optionLabel(triggerTypeOptions, row.triggerType) }}
            <span v-if="row.triggerBatchNo" class="job-muted"> / {{ row.triggerBatchNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="scheduledFireTime" label="计划触发" width="170" show-overflow-tooltip />
        <el-table-column prop="actualFireTime" label="实际触发" width="170" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="170" show-overflow-tooltip />
        <el-table-column prop="endTime" label="结束时间" width="170" show-overflow-tooltip />
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">{{ formatDuration(row.durationMillis) }}</template>
        </el-table-column>
        <el-table-column prop="workerAddress" label="Worker" min-width="210" show-overflow-tooltip />
        <el-table-column prop="attemptCount" label="尝试" width="76" />
        <el-table-column prop="resultSummary" label="结果摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="traceId" label="Trace ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="errorSummary" label="错误摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="'job:log:list'" link type="primary" :icon="Document" @click="openInstanceLogs(row)">
              日志
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadRows" />
      </template>
    </MangoListPanel>

    <el-drawer v-model="logVisible" title="执行日志详情" size="760px" destroy-on-close>
      <el-alert v-if="logError" class="job-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ logError }}
          <el-button v-if="selectedInstance" link type="primary" @click="openInstanceLogs(selectedInstance)"
            >重试</el-button
          >
        </template>
      </el-alert>

      <div v-loading="logLoading" class="job-log-detail">
        <el-descriptions v-if="selectedInstance" :column="2" border>
          <el-descriptions-item label="任务名称">{{ selectedInstance.jobName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务编码">{{ selectedInstance.jobCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实例状态">
            <el-tag :type="optionTagType(instanceStatusOptions, selectedInstance.status)" effect="plain">
              {{ optionLabel(instanceStatusOptions, selectedInstance.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="批次号">{{ selectedInstance.triggerBatchNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Worker">{{ selectedInstance.workerAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="尝试次数">{{ selectedInstance.attemptCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划触发">{{ selectedInstance.scheduledFireTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际触发">{{ selectedInstance.actualFireTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-empty v-if="!logLoading && selectedInstance && !logDetail" description="当前执行实例暂无日志" />

        <el-alert
          v-if="logPolling && logDetail && !nativeLogContent(logDetail)"
          class="job-log-polling"
          type="info"
          :closable="false"
          show-icon
          title="执行日志归档中，页面会自动刷新。"
        />

        <el-descriptions v-if="logDetail" class="job-log-meta" :column="2" border>
          <el-descriptions-item label="日志状态">
            <el-tag :type="executionLogContent(logDetail) ? 'success' : 'warning'" effect="plain">
              {{ executionLogContent(logDetail) ? '可查看' : formatLogFetchStatus(logDetail.logFetchStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="日志位置" :span="2">{{ logDetail.logLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近拉取">{{ logDetail.lastFetchedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ logDetail.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="logDetail.engineResult" label="处理器返回值" :span="2">
            <pre class="job-inline-result">{{ logDetail.engineResult }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="logDetail.errorSummary" label="错误摘要" :span="2">
            <span class="job-log-error">{{ logDetail.errorSummary }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <section v-if="logDetail" class="job-log-output">
          <div class="job-panel-head">
            <div>
              <h3>执行日志</h3>
            </div>
          </div>
          <el-empty v-if="!executionLogContent(logDetail)" description="暂无执行日志" />
          <pre v-else class="job-json job-log-content">{{ executionLogContent(logDetail) }}</pre>
        </section>
      </div>
    </el-drawer>
  </MangoListPage>
</template>

<script setup lang="ts">
import { Document, Refresh } from '@element-plus/icons-vue';
import { MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { onMounted, reactive, ref } from 'vue';
import {
  instanceStatusOptions,
  jobApi,
  optionLabel,
  optionTagType,
  requestErrorMessage,
  triggerTypeOptions,
  type ApiId,
  type JobDefinition,
  type JobInstance,
  type JobInstanceQuery,
  type JobLogDetail,
} from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const syncing = ref(false);
const definitionLoading = ref(false);
const logLoading = ref(false);
const errorMessage = ref('');
const logError = ref('');
const rows = ref<JobInstance[]>([]);
const total = ref(0);
const definitionOptions = ref<JobDefinition[]>([]);
const logVisible = ref(false);
const selectedInstance = ref<JobInstance | null>(null);
const logDetail = ref<JobLogDetail | null>(null);
const logPolling = ref(false);
let logLoadToken = 0;
const query = reactive<JobInstanceQuery>({
  pageNum: 1,
  pageSize: 10,
  jobId: '',
  status: '',
  triggerType: '',
  triggerBatchNo: '',
});

onMounted(() => {
  searchDefinitions('');
  loadRows();
});

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const page = await jobApi.pageInstances(query);
    rows.value = page.list;
    total.value = page.total;
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, '执行实例加载失败');
  } finally {
    loading.value = false;
  }
}

async function syncRows() {
  syncing.value = true;
  errorMessage.value = '';
  try {
    await jobApi.syncInstances({
      jobId: query.jobId,
      triggerTimeStart: query.triggerTimeStart,
      triggerTimeEnd: query.triggerTimeEnd,
      size: query.pageSize,
    });
    await loadRows();
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, '执行实例同步失败');
  } finally {
    syncing.value = false;
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, jobId: '', status: '', triggerType: '', triggerBatchNo: '' });
  loadRows();
}

async function searchDefinitions(keyword: string) {
  definitionLoading.value = true;
  try {
    const page = await jobApi.pageDefinitions({ keyword, pageNum: 1, pageSize: 20 });
    definitionOptions.value = page.list;
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, '任务选项加载失败');
  } finally {
    definitionLoading.value = false;
  }
}

async function openInstanceLogs(row: JobInstance) {
  if (!row.id || !row.jobId) {
    return;
  }
  const currentToken = ++logLoadToken;
  logVisible.value = true;
  selectedInstance.value = row;
  logLoading.value = true;
  logError.value = '';
  logDetail.value = null;
  logPolling.value = false;
  try {
    logDetail.value = await loadInstanceLogUntilReadable(row.id, currentToken);
  } catch (error: unknown) {
    logError.value = requestErrorMessage(error, '执行日志加载失败');
  } finally {
    logLoading.value = false;
  }
}

async function loadInstanceLogUntilReadable(instanceId: ApiId, token: number) {
  const startedAt = Date.now();
  let latest = await jobApi.detailInstanceLog(instanceId);
  logDetail.value = latest;
  logLoading.value = false;
  while (
    token === logLoadToken &&
    logVisible.value &&
    !nativeLogContent(latest) &&
    latest.logFetchStatus === 'UNAVAILABLE' &&
    Date.now() - startedAt < 180_000
  ) {
    logPolling.value = true;
    await wait(3000);
    latest = await jobApi.detailInstanceLog(instanceId);
    logDetail.value = latest;
  }
  if (token === logLoadToken) {
    logPolling.value = false;
  }
  return latest;
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function definitionOptionLabel(item: JobDefinition) {
  if (item.jobName && item.jobCode) {
    return `${item.jobName} / ${item.jobCode}`;
  }
  return item.jobName || item.jobCode || String(item.id || '-');
}

function formatDuration(value?: number) {
  if (value === undefined || value === null) {
    return '-';
  }
  return `${value} ms`;
}

function formatLogFetchStatus(value?: string) {
  if (value === 'AVAILABLE') {
    return '可查看';
  }
  if (value === 'UNAVAILABLE') {
    return '暂无日志';
  }
  return value || '未知';
}

function executionLogContent(detail: JobLogDetail) {
  return detail.nativeLogContent || detail.content || '';
}

function nativeLogContent(detail: JobLogDetail) {
  return detail.nativeLogContent || detail.content || '';
}
</script>

<style scoped>
.job-option-main {
  line-height: 20px;
}

.job-option-sub {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.job-log-detail {
  min-height: 320px;
}

.job-log-meta,
.job-log-output {
  margin-top: 16px;
}

.job-log-polling {
  margin-top: 16px;
}

.job-log-content {
  max-height: 420px;
}

.job-toolbar-actions {
  display: flex;
  gap: 8px;
}

.job-inline-result {
  max-height: 120px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
}

.job-log-error {
  color: var(--el-color-danger);
}
</style>
