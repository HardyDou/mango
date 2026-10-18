<template>
  <div class="information-page" data-page="information.form.submissions">
    <MangoSearchPanel :model="query" :columns="4" @search="search" @reset="reset">
      <el-form-item label="表单">
        <el-select v-model="query.formId" data-filter="form" filterable placeholder="请选择表单" @change="onFormChange">
          <el-option
            v-for="item in formOptions"
            :key="item.id"
            :label="`${item.formName}（${item.formCode}）`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="姓名/手机号/内容" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable>
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
    </MangoSearchPanel>

    <MangoListPanel>
      <template #view-actions>
        <MangoStatusText v-if="pendingCount" tone="warning">{{ pendingCount }} 条待处理</MangoStatusText>
        <el-button data-action="information.submission.export" :disabled="!rows.length" @click="exportRows">
          导出记录
        </el-button>
      </template>

      <el-alert v-if="errorMessage" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" @click="load">重试</el-button>
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无提交记录">
        <el-table-column label="提交编号" width="215" fixed="left">
          <template #default="{ row }"
            ><span class="information-code information-nowrap">{{ row.submissionNo }}</span></template
          >
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <MangoStatusText :tone="submissionStatusTone(row.status)">{{
              submissionStatusLabel(row.status)
            }}</MangoStatusText>
          </template>
        </el-table-column>
        <el-table-column label="来源页面" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sourcePage || '-' }}</template>
        </el-table-column>
        <el-table-column
          v-for="field in columns"
          :key="field.fieldCode"
          :label="field.fieldName"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ cellValue(row, field) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDrawer(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @pagination="load"
        />
      </template>
    </MangoListPanel>

    <el-drawer
      v-model="drawerVisible"
      title="提交详情"
      size="min(460px, 100vw)"
      :destroy-on-close="false"
      class="information-drawer"
    >
      <div v-if="current" class="information-drawer-body">
        <div class="information-record">
          <div>
            <h3 class="information-record-title">{{ currentTitle }}</h3>
            <div class="information-record-meta">
              {{ formatDateTime(current.submittedAt) }} · {{ current.sourcePage || '直接提交' }} · 匿名提交
            </div>
          </div>
          <MangoStatusText :tone="submissionStatusTone(current.status)">
            {{ submissionStatusLabel(current.status) }}
          </MangoStatusText>
        </div>

        <div class="information-record-fields">
          <div v-for="field in detailFields" :key="field.fieldCode" class="information-record-field">
            <label>{{ field.fieldName }}</label>
            <span>{{ cellValue(current, field) }}</span>
          </div>
        </div>

        <div class="information-note">个人信息仅对拥有提交记录查看权限的角色展示。当前列表已默认脱敏。</div>

        <div class="information-drawer-remark">
          <label>处理备注</label>
          <el-input v-model="processRemark" type="textarea" :rows="3" placeholder="记录跟进结果" />
        </div>
        <div class="information-drawer-remark">
          <label>处理状态</label>
          <el-select v-model="processStatus" style="width: 100%" :disabled="current.status === 'CLOSED'">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button
          type="primary"
          data-action="information.submission.process"
          :loading="processing"
          :disabled="current?.status === 'CLOSED'"
          @click="saveProcess"
        >
          保存并更新
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { MangoListPanel, MangoSearchPanel, MangoStatusText, Pagination } from '@mango/common';
import { useInformationApi } from '../composables/useInformationApi';
import type {
  InformationForm,
  InformationFormDetail,
  InformationSubmission,
  InformationSubmissionStatus,
} from '../api/information';
import { formatDateTime, submissionStatusLabel, submissionStatusTone } from '../domain/status';
import { displayFieldValue, parseRules, rulesToFields, type InformationFieldDefinition } from '../domain/rules';

const api = useInformationApi();
const route = useRoute();

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', formId: '' as string });
const formOptions = ref<InformationForm[]>([]);
const rows = ref<InformationSubmission[]>([]);
const total = ref(0);
const pendingCount = ref(0);
const loading = ref(false);
const errorMessage = ref('');
const form = ref<InformationFormDetail>();

const drawerVisible = ref(false);
const current = ref<InformationSubmission>();
const processRemark = ref('');
const processStatus = ref<InformationSubmissionStatus>('PENDING');
const processing = ref(false);

const formId = computed(() => query.formId || String(route.params.id || ''));
const columns = computed<InformationFieldDefinition[]>(() =>
  rulesToFields(parseRules(form.value?.definitionJson)).filter(
    (field) => field.fieldType !== 'static' && field.fieldType !== 'divider',
  ),
);
const detailFields = computed(() => columns.value);
const currentTitle = computed(() => {
  if (!current.value) {
    return '提交详情';
  }
  const values = parseValues(current.value.valuesJson);
  const first = columns.value[0];
  const value = first ? values[first.fieldCode] : undefined;
  const display = first ? displayFieldValue(first, value) : '';
  return display || current.value.submissionNo || '提交详情';
});

function parseValues(valuesJson?: string): Record<string, unknown> {
  try {
    return JSON.parse(valuesJson || '{}') as Record<string, unknown>;
  } catch {
    return {};
  }
}

function cellValue(row: InformationSubmission, field: InformationFieldDefinition): string {
  const value = parseValues(row.valuesJson)[field.fieldCode];
  const display = displayFieldValue(field, value);
  return display === '' ? '-' : display;
}

/** 独立菜单入口：先加载可选表单，再按当前选中表单查询记录。 */
async function loadForms() {
  try {
    const routeFormId = String(route.params.id || '');
    if (routeFormId) {
      query.formId = routeFormId;
    }
    const result = await api.pageForms({ pageNum: 1, pageSize: 100 });
    formOptions.value = result.list;
    if (!query.formId && result.list.length) {
      query.formId = result.list[0].id || '';
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表单列表加载失败';
  }
}

function onFormChange() {
  query.pageNum = 1;
  return load();
}

async function load() {
  if (!formId.value) {
    rows.value = [];
    total.value = 0;
    pendingCount.value = 0;
    form.value = undefined;
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    const [detail, page, pending] = await Promise.all([
      api.detailForm(formId.value),
      api.pageSubmissions({ ...query, formId: formId.value }),
      api.pageSubmissions({ pageNum: 1, pageSize: 1, status: 'PENDING', formId: formId.value }),
    ]);
    form.value = detail;
    rows.value = page.list;
    total.value = page.total;
    pendingCount.value = pending.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '提交记录加载失败';
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  return load();
}

function reset() {
  query.keyword = '';
  query.status = '';
  return search();
}

function openDrawer(row: InformationSubmission) {
  current.value = row;
  processRemark.value = row.processRemark || '';
  processStatus.value = row.status || 'PENDING';
  drawerVisible.value = true;
}

async function saveProcess() {
  if (!current.value) {
    return;
  }
  processing.value = true;
  try {
    await api.processSubmission({
      id: current.value.id!,
      status: processStatus.value,
      processRemark: processRemark.value,
    });
    ElMessage.success('处理结果已保存');
    drawerVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    processing.value = false;
  }
}

async function exportRows() {
  try {
    const csv = await api.exportSubmissions({ ...query, formId: formId.value });
    const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${form.value?.formCode || 'information'}-submissions-${Date.now()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败');
  }
}

onMounted(async () => {
  await loadForms();
  await load();
});
</script>
