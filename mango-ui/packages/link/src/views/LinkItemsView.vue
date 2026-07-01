<template>
  <div class="link-page" data-page="link-items">
    <section class="link-toolbar">
      <div class="link-toolbar-head">
        <h2>网址列表</h2>
        <el-button type="primary" :icon="Plus" data-action="create-link-item" @click="openEditor()">新增</el-button>
      </div>
      <el-form :model="query" class="link-search" inline @submit.prevent>
        <el-form-item label="关键字" class="link-search-item">
          <el-input v-model="query.keyword" clearable placeholder="名称/地址/标签" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item label="分类" class="link-search-item">
          <el-select v-model="query.categoryId" clearable filterable placeholder="全部分类">
            <el-option v-for="item in categoryOptions" :key="String(item.id)" :label="item.name || item.code" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见范围" class="link-search-item">
          <el-select v-model="query.visibilityScope" clearable placeholder="全部">
            <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无网址">
        <el-table-column label="网址" min-width="280">
          <template #default="{ row }">
            <div class="link-name-cell">
              <strong>{{ row.name || '-' }}</strong>
              <span>{{ row.url || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="150" />
        <el-table-column label="可见范围" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ visibilityText(row.visibilityScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <div class="link-tags">
              <el-tag v-if="row.recommended" type="success" size="small">推荐</el-tag>
              <el-tag v-for="tag in row.tags || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">{{ row.status === 'DISABLED' ? '停用' : '启用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ row.updateTime || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="link-actions">
              <el-button link type="primary" :icon="Position" @click="openLinkWithRedirect(row, navigationSourceOf(row.visibilityScope))">打开</el-button>
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

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑网址' : '新增网址'" width="820px" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" maxlength="128" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" filterable placeholder="请选择分类">
                <el-option v-for="item in categoryOptions" :key="String(item.id)" :label="item.name || item.code" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="网址" prop="url">
              <el-input v-model="form.url" placeholder="https://example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介">
              <el-input v-model="form.summary" type="textarea" maxlength="256" show-word-limit :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标地址">
              <el-input v-model="form.iconUrl" placeholder="https://example.com/favicon.ico" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortNo" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签">
              <el-select v-model="form.tags" multiple filterable allow-create default-first-option collapse-tags collapse-tags-tooltip placeholder="输入后回车">
                <el-option v-for="tag in form.tags || []" :key="tag" :label="tag" :value="tag" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见范围" prop="visibilityScope">
              <el-select v-model="form.visibilityScope" @change="syncTargetType">
                <el-option v-for="item in visibilityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推荐">
              <el-switch v-model="form.recommended" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.visibilityScope === 'DEPARTMENT' || form.visibilityScope === 'USER'" :span="24">
            <el-form-item :label="form.visibilityScope === 'DEPARTMENT' ? '部门 ID' : '用户 ID'" prop="targetIdsText">
              <el-input v-model="form.targetIdsText" placeholder="多个 ID 用英文逗号分隔，例如：1001,1002" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" maxlength="256" show-word-limit :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, Edit, Plus, Position, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import {
  linkApi,
  navigationSourceOf,
  normalizeApiId,
  openLinkWithRedirect,
  type ApiId,
  type LinkCategory,
  type LinkItem,
  type LinkPageQuery,
  type LinkStatus,
  type LinkVisibilityScope,
  type LinkVisibilityTarget,
  type LinkVisibilityTargetType,
} from '../api/link';

interface LinkItemForm extends LinkItem {
  targetIdsText?: string;
}

const visibilityOptions: Array<{ label: string; value: LinkVisibilityScope }> = [
  { label: '公开', value: 'PUBLIC' },
  { label: '公司内', value: 'COMPANY' },
  { label: '指定部门', value: 'DEPARTMENT' },
  { label: '指定用户', value: 'USER' },
];

const rows = ref<LinkItem[]>([]);
const categoryOptions = ref<LinkCategory[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const editorVisible = ref(false);
const formRef = ref<FormInstance>();
const query = reactive<LinkPageQuery>({ pageNum: 1, pageSize: 10, keyword: '', categoryId: '', visibilityScope: '', status: '' });
const form = reactive<LinkItemForm>({
  name: '',
  url: '',
  categoryId: '',
  summary: '',
  iconUrl: '',
  tags: [],
  visibilityScope: 'COMPANY',
  recommended: false,
  sortNo: 0,
  remark: '',
  targetIdsText: '',
});
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入网址', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  visibilityScope: [{ required: true, message: '请选择可见范围', trigger: 'change' }],
};

function visibilityText(scope?: LinkVisibilityScope) {
  return visibilityOptions.find(item => item.value === scope)?.label || '-';
}

async function loadCategories() {
  categoryOptions.value = await linkApi.listCategories({ includeDisabled: false });
}

async function loadRows() {
  loading.value = true;
  try {
    const page = await linkApi.pageItems(query);
    rows.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  query.categoryId = '';
  query.visibilityScope = '';
  query.status = '';
  void loadRows();
}

function targetIdsText(targets?: LinkVisibilityTarget[]) {
  return (targets || []).map(item => item.targetId).filter(Boolean).join(',');
}

function syncTargetType() {
  if (form.visibilityScope !== 'DEPARTMENT' && form.visibilityScope !== 'USER') {
    form.targetIdsText = '';
  }
}

function openEditor(row?: LinkItem) {
  Object.assign(form, {
    id: row?.id,
    name: row?.name || '',
    url: row?.url || '',
    categoryId: row?.categoryId || '',
    summary: row?.summary || '',
    iconUrl: row?.iconUrl || '',
    tags: [...(row?.tags || [])],
    visibilityScope: row?.visibilityScope || 'COMPANY',
    recommended: Boolean(row?.recommended),
    sortNo: row?.sortNo || 0,
    remark: row?.remark || '',
    targetIdsText: targetIdsText(row?.visibilityTargets),
  });
  editorVisible.value = true;
}

function parseTargets(scope?: LinkVisibilityScope, idsText?: string): LinkVisibilityTarget[] {
  if (scope !== 'DEPARTMENT' && scope !== 'USER') {
    return [];
  }
  const targetType: LinkVisibilityTargetType = scope === 'DEPARTMENT' ? 'DEPARTMENT' : 'USER';
  return (idsText || '')
    .split(',')
    .map(value => value.trim())
    .filter(Boolean)
    .map(targetId => ({ targetType, targetId: targetId as ApiId }));
}

function toPayload(): LinkItem {
  return {
    ...form,
    visibilityTargets: parseTargets(form.visibilityScope, form.targetIdsText),
  };
}

async function saveRow() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = toPayload();
    if (payload.id) {
      await linkApi.updateItem(payload);
    } else {
      await linkApi.createItem(payload);
    }
    ElMessage.success('保存成功');
    editorVisible.value = false;
    await loadRows();
  } finally {
    saving.value = false;
  }
}

async function changeStatus(row: LinkItem, status: LinkStatus) {
  const id = normalizeApiId(row.id);
  if (!id) {
    return;
  }
  if (status === 'ENABLED') {
    await linkApi.enableItem(id);
  } else {
    await linkApi.disableItem(id);
  }
  ElMessage.success('状态已更新');
  await loadRows();
}

async function deleteRow(row: LinkItem) {
  const id = normalizeApiId(row.id);
  if (!id) {
    return;
  }
  await ElMessageBox.confirm(`确定删除「${row.name || id}」？`, '删除确认', { type: 'warning' });
  await linkApi.deleteItem(id);
  ElMessage.success('删除成功');
  await loadRows();
}

onMounted(async () => {
  await loadCategories();
  await loadRows();
});
</script>
