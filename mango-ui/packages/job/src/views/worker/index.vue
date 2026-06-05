<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>Worker</h2>
          <p>查看任务执行节点心跳、所属应用、引擎 Worker 标识和在线状态。</p>
        </div>
        <el-button v-auth="'job:worker:list'" :icon="Refresh" @click="loadRows">刷新</el-button>
      </div>

      <el-form :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="关键字" class="job-search-item job-search-item-wide">
          <el-input v-model="query.keyword" clearable placeholder="Worker 地址" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="应用" class="job-search-item">
          <el-input v-model="query.appCode" clearable placeholder="appCode" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="状态" class="job-search-item job-search-item-small">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option v-for="item in workerStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="引擎" class="job-search-item job-search-item-small">
          <el-select v-model="query.engineType" clearable placeholder="全部">
            <el-option v-for="item in engineTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
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

      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无 Worker 快照">
        <el-table-column prop="workerAddress" label="Worker 地址" min-width="240" fixed="left" show-overflow-tooltip />
        <el-table-column prop="appCode" label="应用" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="optionTagType(workerStatusOptions, row.status)" size="small">
              {{ optionLabel(workerStatusOptions, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="engineType" label="引擎" width="110" />
        <el-table-column prop="engineWorkerId" label="引擎 Worker" min-width="180" show-overflow-tooltip />
        <el-table-column prop="lastHeartbeatAt" label="最近心跳" width="170" show-overflow-tooltip />
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
  engineTypeOptions,
  jobApi,
  optionLabel,
  optionTagType,
  requestErrorMessage,
  workerStatusOptions,
  type JobWorkerQuery,
  type JobWorkerSnapshot,
} from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const errorMessage = ref('');
const rows = ref<JobWorkerSnapshot[]>([]);
const total = ref(0);
const query = reactive<JobWorkerQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  appCode: '',
  status: '',
  engineType: '',
});

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
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', appCode: '', status: '', engineType: '' });
  loadRows();
}
</script>
