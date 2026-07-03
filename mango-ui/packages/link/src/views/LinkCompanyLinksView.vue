<template>
  <div class="link-page" data-page="link-company">
    <section class="link-toolbar">
      <div class="link-toolbar-head">
        <h2>我的分类</h2>
      </div>
      <el-form :model="query" class="link-search" inline @submit.prevent>
        <el-form-item label="关键字" class="link-search-item">
          <el-input v-model="query.keyword" clearable placeholder="分类名称/说明" @keyup.enter="loadRows" />
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
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无可见分类">
        <el-table-column prop="name" label="分类名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sourceLabel" label="范围" width="130">
          <template #default="{ row }">
            <el-tag :type="row.scope === 'PERSONAL' ? 'warning' : ''">{{ row.sourceLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="网址数量" width="100" prop="linkCount" />
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue';
import { onMounted, reactive, ref } from 'vue';
import { linkApi, requestErrorMessage, type LinkCategory, type LinkItem } from '../api/link';

interface VisibleCategoryRow extends LinkCategory {
  scope: 'COMPANY' | 'PERSONAL';
  linkCount: number;
  sourceLabel: string;
}

const rows = ref<VisibleCategoryRow[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const query = reactive({ keyword: '' });

function normalizeKeyword() {
  return (query.keyword || '').trim().toLowerCase();
}

function buildCompanyCategories(items: LinkItem[]) {
  const map = new Map<string, VisibleCategoryRow>();
  items.forEach(item => {
    const categoryId = item.categoryId;
    if (!categoryId) {
      return;
    }
    const id = String(categoryId);
    const existing = map.get(id);
    if (existing) {
      existing.linkCount += 1;
      return;
    }
    map.set(id, {
      id,
      name: item.categoryName || '-',
      scope: 'COMPANY',
      sourceLabel: '企业',
      remark: '-',
      linkCount: 1,
    });
  });
  return [...map.values()];
}

function buildPersonalCategories(personalItems: LinkItem[]) {
  const countMap = new Map<string, number>();
  personalItems.forEach(item => {
    if (!item.categoryId) {
      return;
    }
    const categoryId = String(item.categoryId);
    countMap.set(categoryId, (countMap.get(categoryId) || 0) + 1);
  });
  return linkApi.listPersonalCategories().then(items =>
    items.map(item => ({
      ...item,
      scope: 'PERSONAL',
      sourceLabel: '个人',
      linkCount: item.id ? countMap.get(String(item.id)) || 0 : 0,
      remark: item.remark || '-',
      name: item.name || '-',
    } as VisibleCategoryRow))
  );
}

function filterByKeyword(rows: VisibleCategoryRow[]) {
  const keyword = normalizeKeyword();
  if (!keyword) {
    return rows;
  }
  return rows.filter((row) =>
    (row.name || '').toLowerCase().includes(keyword) ||
    (row.remark || '').toLowerCase().includes(keyword)
  );
}

function sortRows(rows: VisibleCategoryRow[]) {
  return rows.sort((a, b) => {
    const scopeCompare = a.scope.localeCompare(b.scope);
    if (scopeCompare !== 0) {
      return scopeCompare;
    }
    return (a.name || '').localeCompare((b.name || ''));
  });
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [links, personalPage] = await Promise.all([
      linkApi.listCompanyLinks(query),
      linkApi.pagePersonalItems({ pageNum: 1, pageSize: 500, keyword: query.keyword }),
    ]);
    const personalCategories = await buildPersonalCategories(personalPage.list as LinkItem[]);
    rows.value = filterByKeyword(sortRows([
      ...buildCompanyCategories(links),
      ...personalCategories,
    ]));
  } catch (error) {
    errorMessage.value = requestErrorMessage(error, '分类列表加载失败');
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = '';
  void loadRows();
}

onMounted(loadRows);
</script>
