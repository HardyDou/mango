<template>
  <div class="link-page" data-page="link-my-links">
    <section class="link-toolbar">
      <div class="link-toolbar-head">
        <h2>我的网址</h2>
        <el-button type="primary" :icon="Plus" data-action="create-personal-link" @click="openEditor()">新增</el-button>
      </div>
      <el-form :model="query" class="link-search" inline @submit.prevent>
        <el-form-item label="关键字" class="link-search-item">
          <el-input v-model="query.keyword" clearable placeholder="名称/地址/标签" @keyup.enter="loadRows" />
        </el-form-item>
        <el-form-item class="link-search-actions">
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="link-panel">
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无我的网址">
        <el-table-column label="网址" min-width="280">
          <template #default="{ row }">
            <div class="link-name-cell">
              <strong>{{ row.name || '-' }}</strong>
              <span>{{ row.url || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <div class="link-tags">
              <el-tag v-for="tag in row.tags || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ row.updateTime || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="link-actions">
              <el-button link type="primary" :icon="Position" @click="openLinkWithRedirect(row, 'PERSONAL')">打开</el-button>
              <el-button link type="primary" :icon="Edit" @click="openEditor(row)">编辑</el-button>
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

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑我的网址' : '新增我的网址'" width="720px" destroy-on-close append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item label="网址" prop="url">
          <el-input v-model="form.url" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.summary" type="textarea" maxlength="200" show-word-limit :rows="3" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple filterable allow-create default-first-option collapse-tags collapse-tags-tooltip placeholder="输入后回车">
            <el-option v-for="tag in form.tags || []" :key="tag" :label="tag" :value="tag" />
          </el-select>
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
import { Delete, Edit, Plus, Position, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { linkApi, normalizeApiId, openLinkWithRedirect, type LinkPageQuery, type LinkPersonalItem } from '../api/link';

const rows = ref<LinkPersonalItem[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const editorVisible = ref(false);
const formRef = ref<FormInstance>();
const query = reactive<LinkPageQuery>({ pageNum: 1, pageSize: 10, keyword: '' });
const form = reactive<LinkPersonalItem>({ name: '', url: '', summary: '', tags: [] });
const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入网址', trigger: 'blur' }],
};

async function loadRows() {
  loading.value = true;
  try {
    const page = await linkApi.pagePersonalItems(query);
    rows.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  void loadRows();
}

function openEditor(row?: LinkPersonalItem) {
  Object.assign(form, {
    id: row?.id,
    name: row?.name || '',
    url: row?.url || '',
    summary: row?.summary || '',
    iconUrl: row?.iconUrl || '',
    tags: [...(row?.tags || [])],
  });
  editorVisible.value = true;
}

async function saveRow() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.id) {
      await linkApi.updatePersonalItem(form);
    } else {
      await linkApi.createPersonalItem(form);
    }
    ElMessage.success('保存成功');
    editorVisible.value = false;
    await loadRows();
  } finally {
    saving.value = false;
  }
}

async function deleteRow(row: LinkPersonalItem) {
  const id = normalizeApiId(row.id);
  if (!id) {
    return;
  }
  await ElMessageBox.confirm(`确定删除「${row.name || id}」？`, '删除确认', { type: 'warning' });
  await linkApi.deletePersonalItem(id);
  ElMessage.success('删除成功');
  await loadRows();
}

onMounted(loadRows);
</script>
