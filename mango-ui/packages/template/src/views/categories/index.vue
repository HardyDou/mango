<template>
  <div class="template-container">
    <el-card class="template-main">
      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索分类名称或编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="handleCreate">新增分类</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" class="data-table" stripe>
        <template #empty>
          <el-empty description="暂无模板分类">
            <el-button type="primary" @click="handleCreate">新增分类</el-button>
          </el-empty>
        </template>
        <el-table-column prop="categoryCode" label="分类编码" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column prop="updatedTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="editVisible" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="560px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="96px">
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="categoryForm.categoryCode" :disabled="!!categoryForm.id" placeholder="如 CONTRACT" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" placeholder="如 合同文书" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="categoryForm.remark" type="textarea" :rows="3" placeholder="记录分类边界或维护说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Pagination } from '@mango/common';
import {
  templateCategoryApi,
  type SaveTemplateCategoryPayload,
  type TemplateCategory,
  type TemplateCategoryQuery,
} from '../../api/template';

const loading = ref(false);
const tableData = ref<TemplateCategory[]>([]);
const total = ref(0);
const editVisible = ref(false);
const categoryFormRef = ref<FormInstance>();

const query = reactive<TemplateCategoryQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
});

const categoryForm = reactive<SaveTemplateCategoryPayload>({
  categoryCode: '',
  categoryName: '',
  sort: 0,
  remark: '',
});

const categoryRules: FormRules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
};

onMounted(loadData);

async function loadData() {
  loading.value = true;
  try {
    const result = await templateCategoryApi.page(query);
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
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = '';
  loadData();
}

function handleCreate() {
  Object.assign(categoryForm, {
    id: undefined,
    categoryCode: '',
    categoryName: '',
    sort: 0,
    remark: '',
  });
  editVisible.value = true;
}

function handleEdit(row: TemplateCategory) {
  Object.assign(categoryForm, { ...row });
  editVisible.value = true;
}

async function submitCategory() {
  await categoryFormRef.value?.validate();
  if (categoryForm.id) {
    await templateCategoryApi.update(categoryForm);
    ElMessage.success('分类已保存');
  } else {
    await templateCategoryApi.create(categoryForm);
    ElMessage.success('分类已创建');
  }
  editVisible.value = false;
  loadData();
}

async function handleDelete(row: TemplateCategory) {
  await ElMessageBox.confirm(`确认删除分类“${row.categoryName}”？已关联模板不会自动迁移。`, '删除分类', { type: 'warning' });
  await templateCategoryApi.delete(row.id);
  ElMessage.success('分类已删除');
  loadData();
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 12px;
}

.form-select {
  width: 100%;
}

@media (max-width: 760px) {
  .page-head {
    flex-direction: column;
  }

  .query-keyword,
  .query-select {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
