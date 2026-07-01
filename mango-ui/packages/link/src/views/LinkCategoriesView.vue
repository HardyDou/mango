<template>
  <div class="link-page" data-page="link-categories">
    <section class="link-toolbar">
      <div class="link-toolbar-head">
        <h2>网址分类</h2>
        <el-button type="primary" :icon="Plus" data-action="create-category" @click="openEditor()">新增</el-button>
      </div>
      <el-form :model="query" class="link-search" inline @submit.prevent>
        <el-form-item label="关键字" class="link-search-item">
          <el-input v-model="query.keyword" clearable placeholder="分类名称/编码" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="状态" class="link-search-item">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item class="link-search-actions">
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="link-panel">
      <el-alert v-if="errorMessage" class="link-error" type="error" :closable="false" show-icon>
        <template #title>{{ errorMessage }}</template>
      </el-alert>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe empty-text="暂无网址分类">
        <el-table-column label="分类名称" min-width="180">
          <template #default="{ row }">
            <div class="link-name-cell">
              <strong>{{ row.name || '-' }}</strong>
              <span>{{ row.code || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortNo" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ row.updateTime || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="link-actions">
              <el-button link type="primary" :icon="Edit" @click="openEditor(row)">编辑</el-button>
              <el-button v-if="row.status === 'ENABLED'" link type="warning" @click="changeStatus(row, 'DISABLED')">停用</el-button>
              <el-button v-else link type="success" @click="changeStatus(row, 'ENABLED')">启用</el-button>
              <el-button link type="danger" :icon="Delete" @click="deleteRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="link-pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @size-change="loadRows"
          @current-change="loadRows"
        />
      </div>
    </section>

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑网址分类' : '新增网址分类'" width="620px" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="例如：办公系统" />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input v-model="form.code" maxlength="64" show-word-limit placeholder="例如：office" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.summary" type="textarea" maxlength="200" show-word-limit :rows="3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" :max="999999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { linkApi, requestErrorMessage, type LinkCategory, type LinkPageQuery, type LinkStatus } from '../api/link';

const rows = ref<LinkCategory[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const editorVisible = ref(false);
const errorMessage = ref('');
const formRef = ref<FormInstance>();
const query = reactive<LinkPageQuery>({ pageNum: 1, pageSize: 10, keyword: '', status: '' });
const form = reactive<LinkCategory>({ name: '', code: '', summary: '', sortNo: 0 });
const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
};

function statusText(status?: LinkStatus) {
  return status === 'DISABLED' ? '停用' : '启用';
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const page = await linkApi.pageCategories(query);
    rows.value = page.list;
    total.value = page.total;
  } catch (error) {
    errorMessage.value = requestErrorMessage(error, '网址分类加载失败');
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

function openEditor(row?: LinkCategory) {
  Object.assign(form, {
    id: row?.id,
    name: row?.name || '',
    code: row?.code || '',
    summary: row?.summary || '',
    sortNo: row?.sortNo || 0,
  });
  editorVisible.value = true;
}

async function saveRow() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.id) {
      await linkApi.updateCategory(form);
    } else {
      await linkApi.createCategory(form);
    }
    ElMessage.success('保存成功');
    editorVisible.value = false;
    await loadRows();
  } finally {
    saving.value = false;
  }
}

async function changeStatus(row: LinkCategory, status: LinkStatus) {
  if (!row.id) {
    return;
  }
  if (status === 'ENABLED') {
    await linkApi.enableCategory(row.id);
  } else {
    await linkApi.disableCategory(row.id);
  }
  ElMessage.success('状态已更新');
  await loadRows();
}

async function deleteRow(row: LinkCategory) {
  if (!row.id) {
    return;
  }
  await ElMessageBox.confirm(`确定删除分类「${row.name || row.id}」？`, '删除确认', { type: 'warning' });
  await linkApi.deleteCategory(row.id);
  ElMessage.success('删除成功');
  await loadRows();
}

onMounted(loadRows);
</script>
