<template>
  <section class="{{moduleKebab}}-{{aggregateKebab}}-page">
    <el-form :model="query" class="query-form" inline @submit.prevent>
      <el-form-item label="{{aggregateName}}名称">
        <el-input
          v-model="query.name"
          clearable
          placeholder="请输入{{aggregateName}}名称"
          class="query-input"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-toolbar">
      <el-button type="primary" @click="openCreateDialog">新增</el-button>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="records" row-key="id" border>
      <el-table-column prop="name" label="{{aggregateName}}名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="id" label="业务标识" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="loadData"
      />
    </div>

    <el-dialog
      v-model="formDialogVisible"
      :title="formMode === 'create' ? '新增{{aggregateName}}' : '编辑{{aggregateName}}'"
      width="520px"
      :close-on-click-modal="!submitting"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="120px">
        <el-form-item label="{{aggregateName}}名称" prop="name">
          <el-input v-model="formModel.name" maxlength="128" show-word-limit placeholder="请输入{{aggregateName}}名称" />
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
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  create{{aggregatePascal}},
  delete{{aggregatePascal}},
  get{{aggregatePascal}}Detail,
  page{{aggregatePascal}},
  update{{aggregatePascal}},
  type {{aggregatePascal}}VO,
} from '@{{projectKebab}}/{{moduleKebab}}-api';

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
  loading.value = true;
  try {
    const result = await page{{aggregatePascal}}(query);
    records.value = result.records;
    total.value = Number(result.total || 0);
  } finally {
    loading.value = false;
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

function handlePageSizeChange() {
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
      await create{{aggregatePascal}}({ name: formModel.name });
      ElMessage.success('新增成功');
    } else if (formModel.id) {
      await update{{aggregatePascal}}({ id: formModel.id, name: formModel.name });
      ElMessage.success('保存成功');
    }
    formDialogVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

async function openDetail(record: {{aggregatePascal}}VO) {
  detailRecord.value = await get{{aggregatePascal}}Detail(String(record.id));
  detailVisible.value = true;
}

async function handleDelete(record: {{aggregatePascal}}VO) {
  try {
    await ElMessageBox.confirm(`确认删除“${record.name}”？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await delete{{aggregatePascal}}({ id: record.id });
  ElMessage.success('删除成功');
  if (records.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await loadData();
}

function handleDialogClose() {
  resetFormModel();
}

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.{{moduleKebab}}-{{aggregateKebab}}-page {
  padding: 16px;
}

.query-form {
  margin-bottom: 12px;
}

.query-input {
  width: 240px;
}

.table-toolbar {
  display: flex;
  gap: 8px;
  justify-content: space-between;
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
