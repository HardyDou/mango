<template>
  <div class="payment-page">
    <section class="payment-page__header">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="payment-page__actions">
        <slot name="extra-actions" />
        <el-button v-if="editable" type="primary" :icon="Plus" @click="openEditor()">新增</el-button>
      </div>
    </section>

    <section class="payment-page__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            :placeholder="keywordPlaceholder"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item v-if="withStatus" label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-table :data="rows" v-loading="loading" row-key="id" stripe highlight-current-row class="payment-table">
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <el-tag v-if="column.variant === 'tag'" effect="light" round>{{ formatCell(row, column) }}</el-tag>
          <div v-else-if="column.variant === 'tags'" class="payment-table__tags">
            <el-tag
              v-for="item in formatTags(formatCell(row, column))"
              :key="item"
              effect="light"
              round
            >
              {{ item }}
            </el-tag>
            <span v-if="!formatTags(formatCell(row, column)).length">-</span>
          </div>
          <span v-else-if="column.money">{{ formatMoney(row[column.prop]) }}</span>
          <span v-else>{{ formatCell(row, column) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="withStatus" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" :width="operationWidth" fixed="right" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <PaymentRowActions :actions="rowActions(row)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-page__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-dialog
      v-if="editable"
      v-model="editorVisible"
      :title="editorTitle"
      :width="editorWidth"
      destroy-on-close
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="126px" class="payment-editor payment-dialog-form">
        <slot name="form" :form="form" />
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import Pagination from '@mango/common/components/Pagination/index.vue';
import type { ApiId } from '@mango/api-schema';
import type { PageResult, PaymentPageQuery, PaymentResourceApi, PaymentTableColumn } from '../api/payment';
import PaymentRowActions from './PaymentRowActions.vue';
import type { PaymentRowAction } from './PaymentRowActions';

type PaymentRecord = Record<string, unknown>;

const props = withDefaults(defineProps<{
  title: string;
  description: string;
  keywordPlaceholder?: string;
  columns: PaymentTableColumn[];
  api: PaymentResourceApi<PaymentRecord>;
  defaults?: PaymentRecord;
  rules?: FormRules;
  editable?: boolean;
  withStatus?: boolean;
  deleteConfirmMessage?: (row: PaymentRecord) => string;
  editorWidth?: string;
  operationWidth?: number;
  rowActions?: (row: PaymentRecord) => PaymentRowAction[];
  toSavePayload?: (form: PaymentRecord, editing: boolean) => PaymentRecord;
  onSaved?: (result: unknown, form: PaymentRecord, editing: boolean) => void | Promise<void>;
  onEditorOpened?: (form: PaymentRecord, row?: PaymentRecord) => void | Promise<void>;
}>(), {
  keywordPlaceholder: '名称 / 编码',
  defaults: () => ({}),
  rules: () => ({}),
  editable: true,
  withStatus: true,
  editorWidth: '760px',
  operationWidth: 220,
});

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
});
const rows = ref<PaymentRecord[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const editorVisible = ref(false);
const editingId = ref<ApiId | undefined>();
const formRef = ref<FormInstance>();
const form = reactive<PaymentRecord>({});

const editorTitle = computed(() => `${editingId.value ? '编辑' : '新增'}${props.title}`);

onMounted(loadRows);

async function loadRows() {
  loading.value = true;
  try {
    const result: PageResult<PaymentRecord> = await props.api.page(query);
    rows.value = result.list;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  query.status = '';
  void loadRows();
}

async function openEditor(row?: PaymentRecord) {
  editingId.value = row?.id as ApiId | undefined;
  Object.keys(form).forEach(key => delete form[key]);
  Object.assign(form, props.defaults, row || {});
  editorVisible.value = true;
  await props.onEditorOpened?.(form, row);
}

async function saveRow() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = props.toSavePayload ? props.toSavePayload(form, Boolean(editingId.value)) : form;
    if (editingId.value) {
      const result = await props.api.update(payload);
      await props.onSaved?.(result, form, true);
      ElMessage.success('已保存');
    } else {
      const result = await props.api.create(payload);
      await props.onSaved?.(result, form, false);
      ElMessage.success('已新增');
    }
    editorVisible.value = false;
    await loadRows();
  } finally {
    saving.value = false;
  }
}

async function removeRow(row: PaymentRecord) {
  const message = props.deleteConfirmMessage?.(row) || `确认删除 ${row.name || row.appName || row.channelName || row.methodName || row.cashierName || row.id}？`;
  await ElMessageBox.confirm(message, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  });
  await props.api.remove(row.id as ApiId);
  ElMessage.success('已删除');
  await loadRows();
}

function rowActions(row: PaymentRecord): PaymentRowAction[] {
  const actions: PaymentRowAction[] = [...(props.rowActions?.(row) || [])];
  if (props.editable) {
    actions.push({
      key: 'edit',
      label: '编辑',
      type: 'primary',
      icon: Edit,
      onClick: () => openEditor(row),
    });
    actions.push({
      key: 'delete',
      label: '删除',
      type: 'danger',
      icon: Delete,
      onClick: () => removeRow(row),
    });
  }
  return actions;
}

function formatCell(row: PaymentRecord, column: PaymentTableColumn) {
  const value = row[column.prop];
  if (column.formatter) {
    return column.formatter(row, value);
  }
  return value === undefined || value === null || value === '' ? '-' : value;
}

function formatTags(value: unknown) {
  if (value === '-') {
    return [];
  }
  return String(value)
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
}

function formatMoney(value: unknown) {
  const amount = Number(value || 0) / 100;
  return amount ? `￥${amount.toFixed(2)}` : '-';
}
</script>

<style scoped>
.payment-table__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.payment-editor :deep(.el-input),
.payment-editor :deep(.el-select),
.payment-editor :deep(.el-input-number) {
  width: 100%;
}
</style>
