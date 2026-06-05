<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>引擎状态</h2>
          <p>汇总 Mango 任务与底层调度引擎的同步结果。</p>
        </div>
        <el-button v-auth="'job:engine:list'" :icon="Refresh" @click="loadRows">刷新</el-button>
      </div>
    </section>

    <section class="job-panel">
      <el-alert v-if="errorMessage" class="job-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="loadRows">重试</el-button>
        </template>
      </el-alert>

      <div class="job-kv">
        <div class="job-kv-item">
          <span>待同步</span>
          <strong>{{ totalPending }}</strong>
        </div>
        <div class="job-kv-item">
          <span>已同步</span>
          <strong>{{ totalSynced }}</strong>
        </div>
        <div class="job-kv-item">
          <span>同步失败</span>
          <strong>{{ totalFailed }}</strong>
        </div>
        <div class="job-kv-item">
          <span>引擎数量</span>
          <strong>{{ rows.length }}</strong>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无引擎同步状态">
        <el-table-column prop="engineType" label="引擎" min-width="140" fixed="left" />
        <el-table-column prop="pendingCount" label="待同步" width="120" />
        <el-table-column prop="syncedCount" label="已同步" width="120" />
        <el-table-column prop="failedCount" label="同步失败" width="120" />
        <el-table-column prop="lastUpdatedAt" label="最近更新" width="180" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';
import { jobApi, requestErrorMessage, type JobEngineStatus } from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const errorMessage = ref('');
const rows = ref<JobEngineStatus[]>([]);

const totalPending = computed(() => rows.value.reduce((sum, item) => sum + Number(item.pendingCount || 0), 0));
const totalSynced = computed(() => rows.value.reduce((sum, item) => sum + Number(item.syncedCount || 0), 0));
const totalFailed = computed(() => rows.value.reduce((sum, item) => sum + Number(item.failedCount || 0), 0));

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    rows.value = await jobApi.listEngineStatus();
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, '引擎状态加载失败');
  } finally {
    loading.value = false;
  }
}
</script>
