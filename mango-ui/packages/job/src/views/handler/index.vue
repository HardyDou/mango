<template>
  <div class="job-page">
    <section class="job-toolbar">
      <div class="job-toolbar-head">
        <div>
          <h2>处理器</h2>
          <p>查看当前应用已注册的 Mango Job 处理器和默认执行策略。</p>
        </div>
        <el-button v-auth="'job:handler:list'" :icon="Refresh" @click="loadRows">刷新</el-button>
      </div>
    </section>

    <section class="job-panel">
      <el-alert v-if="errorMessage" class="job-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="loadRows">重试</el-button>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无处理器">
        <el-table-column label="处理器" min-width="220" fixed="left">
          <template #default="{ row }">
            <div class="job-name-cell">
              <strong>{{ row.handlerName }}</strong>
              <span>{{ row.appCode || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="任务类型" width="130">
          <template #default="{ row }">{{ optionLabel(jobTypeOptions, row.jobType) }}</template>
        </el-table-column>
        <el-table-column label="并发" width="90">
          <template #default="{ row }">
            <el-tag :type="row.concurrent ? 'success' : 'info'" size="small">{{ row.concurrent ? '允许' : '禁止' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认超时" width="110">
          <template #default="{ row }">{{ row.timeoutSeconds ?? '-' }} 秒</template>
        </el-table-column>
        <el-table-column prop="retryPolicy" label="重试策略" min-width="220" show-overflow-tooltip />
        <el-table-column label="参数 Schema" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.paramSchema" @click="openJson(row.paramSchema)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="jsonVisible" title="参数 Schema" width="680px" append-to-body>
      <pre class="job-json">{{ currentJson }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { onMounted, ref } from 'vue';
import { jobApi, jobTypeOptions, optionLabel, requestErrorMessage, type JobHandler } from '../../api/job';
import '../job-admin.css';

const loading = ref(false);
const errorMessage = ref('');
const rows = ref<JobHandler[]>([]);
const jsonVisible = ref(false);
const currentJson = ref('');

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    rows.value = await jobApi.listHandlers();
  } catch (error: unknown) {
    errorMessage.value = requestErrorMessage(error, '处理器加载失败');
  } finally {
    loading.value = false;
  }
}

function openJson(json?: string) {
  currentJson.value = formatJson(json);
  jsonVisible.value = true;
}

function formatJson(value?: string) {
  if (!value) {
    return '';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
</script>
