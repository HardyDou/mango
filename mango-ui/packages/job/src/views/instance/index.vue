<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>执行实例</h2>
          <p>查询任务触发、运行结果、耗时、批次号和链路追踪信息。</p>
        </div>
        <el-button v-auth="'job:instance:list'" :icon="Refresh" @click="loadRows">刷新</el-button>
      </div>

      <el-form :model="query" class="job-search" @submit.prevent>
        <el-form-item label="任务ID">
          <el-input v-model="query.jobId" clearable placeholder="jobId" style="width: 140px" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="item in instanceStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发类型">
          <el-select v-model="query.triggerType" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="item in triggerTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="query.triggerBatchNo" clearable placeholder="triggerBatchNo" style="width: 190px" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item>
          <el-button v-auth="'job:instance:list'" type="primary" :icon="Search" @click="loadRows">查询</el-button>
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

      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无执行实例">
        <el-table-column prop="id" label="实例ID" width="120" fixed="left" />
        <el-table-column prop="jobId" label="任务ID" width="120" />
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
        <el-table-column prop="triggerTime" label="触发时间" width="170" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="170" show-overflow-tooltip />
        <el-table-column prop="endTime" label="结束时间" width="170" show-overflow-tooltip />
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">{{ row.durationMillis ?? '-' }} ms</template>
        </el-table-column>
        <el-table-column prop="engineType" label="引擎" width="110" />
        <el-table-column prop="engineInstanceId" label="引擎实例" min-width="170" show-overflow-tooltip />
        <el-table-column prop="traceId" label="Trace ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="errorSummary" label="错误摘要" min-width="220" show-overflow-tooltip />
      </el-table>

      <div class="job-pagination">
        <Pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadRows" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue';
import { onMounted, reactive, ref } from 'vue';
import {
  instanceStatusOptions,
  jobApi,
  optionLabel,
  optionTagType,
  requestErrorMessage,
  triggerTypeOptions,
  type JobInstance,
  type JobInstanceQuery,
} from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const errorMessage = ref('');
const rows = ref<JobInstance[]>([]);
const total = ref(0);
const query = reactive<JobInstanceQuery>({
  pageNum: 1,
  pageSize: 10,
  jobId: '',
  status: '',
  triggerType: '',
  triggerBatchNo: '',
});

onMounted(loadRows);

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

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, jobId: '', status: '', triggerType: '', triggerBatchNo: '' });
  loadRows();
}
</script>
