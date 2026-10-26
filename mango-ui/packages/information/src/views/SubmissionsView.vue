<template>
  <div class="information-page" data-page="information.form.submissions">
    <MangoSearchPanel :model="query" :columns="4" @search="search" @reset="reset">
      <el-form-item label="表单">
        <el-select
          v-model="query.formId"
          data-filter="form"
          data-select="submission-form"
          filterable
          :loading="formsLoading"
          :placeholder="formsLoading ? '表单加载中…' : '请选择表单'"
          @change="onFormChange"
        >
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
        <el-select v-model="query.status" data-select="submission-status" placeholder="全部" clearable>
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="timeRange"
          data-select="submission-time"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :shortcuts="timeShortcuts"
          @change="onTimeRangeChange"
        />
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

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        highlight-current-row
        :empty-text="emptyText"
        @row-click="openDrawer"
      >
        <el-table-column label="记录ID" width="206" fixed="left" show-overflow-tooltip>
          <template #default="{ row }"
            ><span class="information-code information-nowrap">{{ row.id }}</span></template
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
          <template #default="{ row }">{{ sourceLabel(row.sourcePage) }}</template>
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
            <el-button link type="primary" data-row-action="process" @click.stop="openDrawer(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @pagination="onPageChange"
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

        <div class="information-note">采集记录仅对拥有查看权限的角色开放，字段按原始值展示，便于直接回访。</div>

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
          <div class="information-quick-actions">
            <el-button
              v-for="item in quickActions"
              :key="item.status"
              link
              type="primary"
              :disabled="current.status === 'CLOSED'"
              :data-quick-action="item.status"
              @click="processStatus = item.status"
            >
              {{ item.label }}
            </el-button>
          </div>
        </div>
        <p v-if="current.status === 'CLOSED'" class="information-note">该记录已关闭，只能查看，不能再修改处理状态。</p>
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
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
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
const router = useRouter();

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  formId: '' as string,
  startTime: '' as string,
  endTime: '' as string,
});
/** 没选表单时提示去选，选了但没数据时说明该表单确实还没有提交。 */
const emptyText = computed(() => (query.formId ? '该表单暂无采集记录' : '请选择表单后查看采集记录'));

const timeRange = ref<[string, string] | null>(null);
/** 常用时间窗口，减少手工选择。 */
const timeShortcuts = [
  { text: '今天', value: () => [startOfToday(), new Date()] },
  { text: '近 7 天', value: () => [daysAgo(6), new Date()] },
  { text: '近 30 天', value: () => [daysAgo(29), new Date()] },
];

function startOfToday(): Date {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  return date;
}

function daysAgo(days: number): Date {
  const date = startOfToday();
  date.setDate(date.getDate() - days);
  return date;
}

function onTimeRangeChange(value: [string, string] | null) {
  query.startTime = value?.[0] || '';
  query.endTime = value?.[1] || '';
  query.pageNum = 1;
  return load();
}
const formOptions = ref<InformationForm[]>([]);
const formsLoading = ref(false);
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
/** 抽屉里的常用处理动作，等价于选择对应状态。 */
const quickActions: Array<{ status: InformationSubmissionStatus; label: string }> = [
  { status: 'PROCESSING', label: '标记处理中' },
  { status: 'COMPLETED', label: '标记已完成' },
  { status: 'CLOSED', label: '关闭记录' },
];

const formId = computed(() => query.formId || String(route.query.id || ''));
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
  return display || `#${current.value.id ?? ''}` || '提交详情';
});

function parseValues(valuesJson?: string): Record<string, unknown> {
  try {
    return JSON.parse(valuesJson || '{}') as Record<string, unknown>;
  } catch {
    return {};
  }
}

/** 来源页面只展示可读的域名与路径，完整地址放在悬浮提示里。 */
function sourceLabel(sourcePage?: string): string {
  if (!sourcePage) {
    return '-';
  }
  try {
    const url = new URL(sourcePage);
    const hashPath = url.hash.replace(/^#/, '');
    return `${url.host}${hashPath || url.pathname}`;
  } catch {
    return sourcePage;
  }
}

function cellValue(row: InformationSubmission, field: InformationFieldDefinition): string {
  const value = parseValues(row.valuesJson)[field.fieldCode];
  const display = displayFieldValue(field, value);
  return display === '' ? '-' : display;
}

/** 本页最近一次写回的地址参数，用于区分「自己的回声」和「外部导航」。 */
let lastWrittenQuery = '';

function currentQuerySignature(): string {
  return JSON.stringify({
    ...(query.formId ? { formId: query.formId } : {}),
    ...(query.keyword ? { keyword: query.keyword } : {}),
    ...(query.status ? { status: query.status } : {}),
    ...(query.startTime ? { startTime: query.startTime } : {}),
    ...(query.endTime ? { endTime: query.endTime } : {}),
    ...(query.pageNum > 1 ? { page: String(query.pageNum) } : {}),
  });
}

function routeQuerySignature(): string {
  const { formId: urlFormId, keyword, status, startTime, endTime, page } = route.query;
  return JSON.stringify({
    ...(typeof urlFormId === 'string' && urlFormId ? { formId: urlFormId } : {}),
    ...(typeof keyword === 'string' && keyword ? { keyword } : {}),
    ...(typeof status === 'string' && status ? { status } : {}),
    ...(typeof startTime === 'string' && startTime ? { startTime } : {}),
    ...(typeof endTime === 'string' && endTime ? { endTime } : {}),
    ...(typeof page === 'string' && Number(page) > 1 ? { page } : {}),
  });
}

/** 筛选状态写入地址栏，刷新或把链接发给同事时能保持同一视图。 */
function syncQueryToUrl() {
  lastWrittenQuery = currentQuerySignature();
  router.replace({ query: JSON.parse(lastWrittenQuery) as Record<string, string> });
}

function readQueryFromUrl() {
  const { formId: urlFormId, keyword, status, page, startTime, endTime } = route.query;
  query.formId = typeof urlFormId === 'string' && urlFormId ? urlFormId : String(route.query.id || '');
  query.keyword = typeof keyword === 'string' ? keyword : '';
  query.status = typeof status === 'string' ? status : '';
  query.startTime = typeof startTime === 'string' ? startTime : '';
  query.endTime = typeof endTime === 'string' ? endTime : '';
  timeRange.value = query.startTime && query.endTime ? [query.startTime, query.endTime] : null;
  query.pageNum = Number(page) > 0 ? Number(page) : 1;
}

/** 独立菜单入口：先加载可选表单，再按当前选中表单查询记录。 */
async function loadForms() {
  formsLoading.value = true;
  try {
    const result = await api.pageForms({ pageNum: 1, pageSize: 100 });
    formOptions.value = result.list;
    if (!query.formId && result.list.length) {
      query.formId = result.list[0].id || '';
    }
    syncQueryToUrl();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表单列表加载失败';
  } finally {
    formsLoading.value = false;
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
      api.pageSubmissions({
        pageNum: 1,
        pageSize: 1,
        status: 'PENDING',
        formId: formId.value,
        startTime: query.startTime,
        endTime: query.endTime,
      }),
    ]);
    form.value = detail;
    rows.value = page.list;
    total.value = page.total;
    pendingCount.value = pending.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '采集记录加载失败';
  } finally {
    loading.value = false;
  }
  syncQueryToUrl();
}

function search() {
  query.pageNum = 1;
  return load();
}

function reset() {
  query.keyword = '';
  query.status = '';
  query.startTime = '';
  query.endTime = '';
  timeRange.value = null;
  return search();
}

function onPageChange() {
  return load();
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
    ElMessage.success(`已更新为「${submissionStatusLabel(processStatus.value)}」`);
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
    ElMessage.success(`已导出 ${total.value} 条记录`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败');
  }
}

watch(
  () => route.query,
  async () => {
    // 只有外部导航（深链、前进后退）才需要重新读取；自己的写回直接跳过
    if (routeQuerySignature() === lastWrittenQuery) {
      return;
    }
    const before = currentQuerySignature();
    readQueryFromUrl();
    if (before !== currentQuerySignature()) {
      await load();
    }
  },
);

onMounted(async () => {
  readQueryFromUrl();
  await loadForms();
  await load();
});
</script>
