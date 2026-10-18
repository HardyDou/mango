<template>
  <MangoListPage class="information-page" data-page="information.submissions">
    <template #search
      ><MangoSearchPanel :model="query" :columns="3" @search="load" @reset="reset"
        ><el-form-item label="关键字"
          ><el-input
            v-model="query.keyword"
            clearable
            placeholder="提交编号/来源页面"
            @keyup.enter="load" /></el-form-item
        ><el-form-item label="状态"
          ><el-select v-model="query.status" clearable placeholder="全部"
            ><el-option label="待处理" value="PENDING" /><el-option label="处理中" value="PROCESSING" /><el-option
              label="已完成"
              value="COMPLETED" /><el-option
              label="已关闭"
              value="CLOSED" /></el-select></el-form-item></MangoSearchPanel
    ></template>
    <MangoListPanel>
      <template #actions
        ><el-button
          data-action="information.submission.export"
          type="primary"
          plain
          :disabled="!rows.length"
          @click="exportRows"
          >导出当前筛选</el-button
        ></template
      >
      <el-alert v-if="error" type="error" :closable="false" show-icon>{{ error }}</el-alert>
      <el-table v-loading="loading" :data="rows" stripe row-key="id" empty-text="暂无提交记录">
        <el-table-column prop="submissionNo" label="提交编号" width="205" fixed="left" />
        <el-table-column prop="submittedAt" label="提交时间" width="180" />
        <el-table-column prop="status" label="状态" width="110"
          ><template #default="{ row }"
            ><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template
          ></el-table-column
        >
        <el-table-column
          v-for="column in dynamicColumns"
          :key="column.code"
          :label="column.name"
          min-width="150"
          show-overflow-tooltip
          ><template #default="{ row }">{{ valueOf(row, column.code) }}</template></el-table-column
        >
        <el-table-column prop="sourcePage" label="来源页面" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="showDetail(row)">详情</el-button
            ><el-button v-if="row.status !== 'CLOSED'" link type="success" @click="process(row, 'COMPLETED')"
              >完成</el-button
            ><el-button v-if="row.status !== 'CLOSED'" link type="danger" @click="process(row, 'CLOSED')"
              >关闭</el-button
            ></template
          ></el-table-column
        >
      </el-table>
      <template #pagination
        ><Pagination
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @pagination="load"
      /></template>
    </MangoListPanel>
    <el-dialog v-model="detailVisible" title="提交详情" width="720px"
      ><el-descriptions v-if="detail" :column="1" border
        ><el-descriptions-item label="提交编号">{{ detail.submissionNo }}</el-descriptions-item
        ><el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item
        ><el-descriptions-item label="来源页面">{{ detail.sourcePage || '-' }}</el-descriptions-item
        ><el-descriptions-item label="提交内容">
          <pre class="information-json">{{ pretty(detail.valuesJson) }}</pre></el-descriptions-item
        ><el-descriptions-item label="处理备注">{{
          detail.processRemark || '-'
        }}</el-descriptions-item></el-descriptions
      ></el-dialog
    >
  </MangoListPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationSubmission, SubmissionStatus } from '../api/information';

const api = useInformationApi();
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' });
const rows = ref<InformationSubmission[]>([]);
const total = ref(0);
const loading = ref(false);
const error = ref('');
const detail = ref<InformationSubmission>();
const detailVisible = ref(false);
const dynamicColumns = computed(() => {
  const columns = new Map<string, { code: string; name: string }>();
  rows.value.forEach((row) => {
    try {
      const definition = JSON.parse(row.definitionJson || '[]') as Array<{ fieldCode: string; fieldName: string }>;
      definition.forEach((field) => columns.set(field.fieldCode, { code: field.fieldCode, name: field.fieldName }));
    } catch {
      /* malformed data remains visible in detail */
    }
  });
  return [...columns.values()];
});
const statusLabel = (status?: string) =>
  ({ PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', CLOSED: '已关闭' })[status || ''] || status || '-';
const statusType = (status?: string) =>
  status === 'COMPLETED' ? 'success' : status === 'CLOSED' ? 'info' : status === 'PROCESSING' ? 'warning' : 'danger';
async function load() {
  loading.value = true;
  error.value = '';
  try {
    const result = await api.pageSubmissions(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败';
  } finally {
    loading.value = false;
  }
}
function reset() {
  query.keyword = '';
  query.status = '';
  query.pageNum = 1;
  load();
}
function valueOf(row: InformationSubmission, code: string) {
  try {
    const value = (JSON.parse(row.valuesJson || '{}') as Record<string, unknown>)[code];
    return value === undefined || value === null ? '-' : String(value);
  } catch {
    return '-';
  }
}
async function exportRows() {
  try {
    const csv = await api.exportSubmissions(query);
    const blob = new Blob([`\\ufeff${csv}`], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `information-submissions-${Date.now()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败');
  }
}
async function process(row: InformationSubmission, status: SubmissionStatus) {
  try {
    await ElMessageBox.confirm(
      status === 'CLOSED' ? '关闭后不可继续处理，确认关闭？' : '确认标记为已完成？',
      '处理确认',
      { type: 'warning' },
    );
    await api.processSubmission({ id: row.id!, status });
    ElMessage.success('处理成功');
    await load();
  } catch {
    /* 用户取消 */
  }
}
async function showDetail(row: InformationSubmission) {
  detail.value = await api.detailSubmission(row.id!);
  detailVisible.value = true;
}
function pretty(json?: string) {
  try {
    return JSON.stringify(JSON.parse(json || ''), null, 2);
  } catch {
    return json || '';
  }
}
onMounted(load);
</script>
