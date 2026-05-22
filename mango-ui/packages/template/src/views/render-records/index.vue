<template>
  <div class="template-container">
    <el-card class="template-main">
      <el-form :inline="true" class="search-form">
        <el-form-item label="模板编码">
          <el-input
            v-model="query.templateCode"
            placeholder="输入模板编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="等待中" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" class="data-table" stripe>
        <template #empty>
          <el-empty description="暂无渲染记录" />
        </template>
        <el-table-column prop="templateCode" label="模板编码" min-width="180" show-overflow-tooltip />
        <el-table-column prop="versionNo" label="版本" width="90">
          <template #default="{ row }">V{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column prop="outputFormat" label="输出格式" width="110" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="recordStatusType(row.status)">{{ recordStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="outputFileId" label="文件ID" width="110">
          <template #default="{ row }">{{ row.outputFileId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="失败摘要" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadData"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="渲染详情" size="680px">
      <el-descriptions v-if="currentRecord" :column="2" border>
        <el-descriptions-item label="记录ID">{{ currentRecord.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ recordStatusText(currentRecord.status) }}</el-descriptions-item>
        <el-descriptions-item label="模板编码">{{ currentRecord.templateCode }}</el-descriptions-item>
        <el-descriptions-item label="版本">V{{ currentRecord.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="输出格式">{{ currentRecord.outputFormat }}</el-descriptions-item>
        <el-descriptions-item label="文件ID">{{ currentRecord.outputFileId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRecord.createdTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentRecord.updatedTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <section v-if="currentRecord?.outputContent" class="detail-block">
        <h2>输出内容</h2>
        <pre>{{ currentRecord.outputContent }}</pre>
      </section>
      <section v-if="currentRecord?.errorMessage" class="detail-block">
        <h2>失败原因</h2>
        <pre class="error-text">{{ currentRecord.errorMessage }}</pre>
      </section>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Pagination } from '@mango/common';
import {
  templateApi,
  type TemplateRenderRecord,
  type TemplateRenderRecordQuery,
  type TemplateRenderStatus,
} from '../../api/template';

const loading = ref(false);
const tableData = ref<TemplateRenderRecord[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const currentRecord = ref<TemplateRenderRecord | null>(null);

const query = reactive<TemplateRenderRecordQuery>({
  pageNum: 1,
  pageSize: 10,
  templateCode: '',
  status: '',
});

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const result = await templateApi.renderRecordPage(query);
    tableData.value = result.list;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    templateCode: '',
    status: '',
  });
  loadData();
}

async function handleDetail(row: TemplateRenderRecord) {
  currentRecord.value = await templateApi.renderRecord(row.id);
  detailVisible.value = true;
}

function recordStatusType(value: TemplateRenderStatus) {
  if (value === 'SUCCESS') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'RUNNING') return 'warning';
  return 'info';
}

function recordStatusText(value: TemplateRenderStatus) {
  const map: Record<TemplateRenderStatus, string> = {
    PENDING: '等待中',
    RUNNING: '执行中',
    SUCCESS: '成功',
    FAILED: '失败',
  };
  return map[value] || value;
}
</script>

<style scoped>
.template-container {
  display: flex;
  min-height: calc(100vh - var(--mango-header-height) - var(--mango-tags-view-height) - 32px);
  padding: 0;
}

.template-main {
  display: flex;
  flex: 1;
  min-width: 0;
}

.template-main :deep(.el-card__body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 16px;
}

.search-form {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 8px;
}

.action-toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: 12px;
}

.toolbar-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.data-table {
  flex: 1;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 420px;
}

.template-main :deep(.pagination-container) {
  flex-shrink: 0;
}

.detail-block {
  margin-top: 18px;
}

.detail-block h2 {
  margin: 0 0 8px;
  font-size: 15px;
}

.detail-block pre {
  min-height: 96px;
  padding: 12px;
  overflow: auto;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}

.error-text {
  color: var(--el-color-danger);
}

@media (max-width: 760px) {
  .page-head {
    flex-direction: column;
  }

  .query-keyword,
  .query-select {
    width: 100%;
  }
}
</style>
