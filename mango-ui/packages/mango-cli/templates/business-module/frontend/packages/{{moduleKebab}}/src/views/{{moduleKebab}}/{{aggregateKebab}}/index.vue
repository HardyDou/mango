<template>
  <MangoListPage data-page="{{moduleKebab}}.{{aggregateKebab}}">
    <template #search>
      <MangoSearchPanel :model="query" collapsible :collapsed-count="3" @search="handleSearch" @reset="handleReset">
        <el-form-item label="{{aggregateName}}名称">
          <el-input
            v-model="query.name"
            clearable
            placeholder="请输入{{aggregateName}}名称"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button type="primary" plain @click="openCreateDialog">新增</el-button>
      </template>

      <el-table v-loading="loading" :data="records" row-key="id" stripe highlight-current-row>
        <el-table-column prop="name" label="{{aggregateName}}名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="id" label="业务标识" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.page" v-model:limit="query.size" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>

    <el-dialog
      v-model="formDialogVisible"
      :title="formMode === 'create' ? '新增{{aggregateName}}' : '编辑{{aggregateName}}'"
      width="520px"
      :close-on-click-modal="!submitting"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="120px">
        <el-form-item label="{{aggregateName}}名称" prop="name">
          <el-input
            v-model="formModel.name"
            maxlength="128"
            show-word-limit
            placeholder="请输入{{aggregateName}}名称"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="submitting" @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="{{aggregateName}}详情" size="420px">
      <el-descriptions v-if="detailRecord" :column="1" border>
        <el-descriptions-item label="业务标识">
          <span v-text="detailRecord.id" />
        </el-descriptions-item>
        <el-descriptions-item label="{{aggregateName}}名称">
          <span v-text="detailRecord.name" />
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无详情数据" />
    </el-drawer>
  </MangoListPage>
</template>

<script setup lang="ts">
import { MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { type {{aggregatePascal}}VO } from '@{{projectKebab}}/{{moduleKebab}}-api';
import { get{{aggregatePascal}}Api } from '../../../api-context';

defineOptions({ name: '{{aggregatePascal}}ListPage' });

type FormMode = 'create' | 'edit';

interface {{aggregatePascal}}FormModel {
  id?: {{aggregatePascal}}VO['id'];
  name: string;
}

const loading = ref(false);
const submitting = ref(false);
const records = ref<{{aggregatePascal}}VO[]>([]);
const total = ref(0);
const formDialogVisible = ref(false);
const detailVisible = ref(false);
const formMode = ref<FormMode>('create');
const detailRecord = ref<{{aggregatePascal}}VO | null>(null);
const formRef = ref<FormInstance>();
const pageAbortController = new AbortController();
let listRequestController: AbortController | undefined;
const {{aggregateCamel}}Api = get{{aggregatePascal}}Api();

const query = reactive({
  page: 1,
  size: 20,
  name: '',
});

const formModel = reactive<{{aggregatePascal}}FormModel>({
  name: '',
});

const formRules: FormRules<{{aggregatePascal}}FormModel> = {
  name: [
    { required: true, message: '请输入{{aggregateName}}名称', trigger: 'blur' },
    { max: 128, message: '{{aggregateName}}名称不能超过 128 个字符', trigger: 'blur' },
  ],
};

async function loadData() {
  listRequestController?.abort('Superseded by a newer list request');
  const requestController = new AbortController();
  listRequestController = requestController;
  const abortForPageUnmount = () => requestController.abort('{{moduleKebab}} page unmounted');
  pageAbortController.signal.addEventListener('abort', abortForPageUnmount, { once: true });
  loading.value = true;
  try {
    const result = await {{aggregateCamel}}Api.page({ ...query }, requestController.signal);
    if (listRequestController !== requestController) return;
    records.value = result.records;
    total.value = Number(result.total || 0);
  } catch (error) {
    if (!isAbortedFailure(error)) ElMessage.error(errorMessage(error, '加载{{aggregateName}}列表失败'));
  } finally {
    pageAbortController.signal.removeEventListener('abort', abortForPageUnmount);
    if (listRequestController === requestController) {
      listRequestController = undefined;
      loading.value = false;
    }
  }
}

function handleSearch() {
  query.page = 1;
  void loadData();
}

function handleReset() {
  query.name = '';
  query.page = 1;
  void loadData();
}

function resetFormModel() {
  formModel.id = undefined;
  formModel.name = '';
  formRef.value?.clearValidate();
}

function openCreateDialog() {
  formMode.value = 'create';
  resetFormModel();
  formDialogVisible.value = true;
}

function openEditDialog(record: {{aggregatePascal}}VO) {
  formMode.value = 'edit';
  resetFormModel();
  formModel.id = record.id;
  formModel.name = record.name;
  formDialogVisible.value = true;
}

async function submitForm() {
  const valid = await formRef.value?.validate();
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    if (formMode.value === 'create') {
      await {{aggregateCamel}}Api.create({ name: formModel.name }, pageAbortController.signal);
      ElMessage.success('新增成功');
    } else if (formModel.id) {
      await {{aggregateCamel}}Api.update(
        { id: formModel.id, name: formModel.name },
        pageAbortController.signal,
      );
      ElMessage.success('保存成功');
    }
    formDialogVisible.value = false;
    await loadData();
  } catch (error) {
    if (!isAbortedFailure(error)) ElMessage.error(errorMessage(error, '保存{{aggregateName}}失败'));
  } finally {
    submitting.value = false;
  }
}

async function openDetail(record: {{aggregatePascal}}VO) {
  try {
    detailRecord.value = await {{aggregateCamel}}Api.detail(String(record.id), pageAbortController.signal);
    detailVisible.value = true;
  } catch (error) {
    if (!isAbortedFailure(error)) ElMessage.error(errorMessage(error, '加载{{aggregateName}}详情失败'));
  }
}

async function handleDelete(record: {{aggregatePascal}}VO) {
  try {
    await ElMessageBox.confirm(`确认删除“${record.name}”？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  try {
    await {{aggregateCamel}}Api.delete({ id: record.id }, pageAbortController.signal);
    ElMessage.success('删除成功');
    if (records.value.length === 1 && query.page > 1) query.page -= 1;
    await loadData();
  } catch (error) {
    if (!isAbortedFailure(error)) ElMessage.error(errorMessage(error, '删除{{aggregateName}}失败'));
  }
}

function isAbortedFailure(error: unknown): boolean {
  return (
    error instanceof Error &&
    (error.name === 'AbortError' || (error as Error & { kind?: string }).kind === 'aborted')
  );
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

function handleDialogClose() {
  resetFormModel();
}

onMounted(() => {
  void loadData();
});

onBeforeUnmount(() => {
  listRequestController?.abort('{{moduleKebab}} page unmounted');
  pageAbortController.abort('{{moduleKebab}} page unmounted');
});
</script>
