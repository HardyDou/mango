<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>执行日志</h2>
          <p>查询 Mango 记录的日志索引、引擎实例和拉取游标。</p>
        </div>
        <el-button v-auth="'job:log:list'" :icon="Refresh" @click="loadRows">刷新</el-button>
      </div>

      <el-form :model="query" class="job-search" @submit.prevent>
        <el-form-item label="任务ID">
          <el-input v-model="query.jobId" clearable placeholder="jobId" style="width: 140px" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="实例ID">
          <el-input v-model="query.instanceId" clearable placeholder="instanceId" style="width: 140px" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="引擎">
          <el-select v-model="query.engineType" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="item in engineTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-auth="'job:log:list'" type="primary" :icon="Search" @click="loadRows">查询</el-button>
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

      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无日志索引">
        <el-table-column prop="id" label="日志ID" width="120" fixed="left" />
        <el-table-column prop="jobId" label="任务ID" width="120" />
        <el-table-column prop="instanceId" label="实例ID" width="120" />
        <el-table-column prop="engineType" label="引擎" width="110" />
        <el-table-column prop="engineInstanceId" label="引擎实例" min-width="170" show-overflow-tooltip />
        <el-table-column prop="logLocation" label="日志位置" min-width="260" show-overflow-tooltip />
        <el-table-column prop="readOffset" label="偏移量" width="110" />
        <el-table-column prop="lastFetchedAt" label="最近拉取" width="170" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" show-overflow-tooltip />
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
import { engineTypeOptions, jobApi, requestErrorMessage, type JobLogIndex, type JobLogQuery } from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const errorMessage = ref('');
const rows = ref<JobLogIndex[]>([]);
const total = ref(0);
const query = reactive<JobLogQuery>({
  pageNum: 1,
  pageSize: 10,
  jobId: '',
  instanceId: '',
  engineType: '',
});

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const page = await jobApi.pageLogs(query);
    rows.value = page.list;
    total.value = page.total;
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, '执行日志加载失败');
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, jobId: '', instanceId: '', engineType: '' });
  loadRows();
}
</script>
