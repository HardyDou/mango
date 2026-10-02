<template>
  <MangoListPage class="link-page" data-page="link-favorites">
    <template #search>
      <MangoSearchPanel :model="query" :columns="3" @search="loadRows" @reset="resetQuery">
        <el-form-item label="关键字" class="link-search-item">
          <el-input v-model="query.keyword" clearable placeholder="名称/地址/标签" @keyup.enter="loadRows" />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无收藏网址">
        <el-table-column label="网址" min-width="260">
          <template #default="{ row }">
            <div class="link-name-cell">
              <strong>{{ row.name || '-' }}</strong>
              <span>{{ row.url || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="150" />
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <div class="link-tags">
              <el-tag v-for="tag in row.tags || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="favoriteTime" label="收藏时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="link-actions">
              <el-button link type="primary" :icon="Position" @click="openLinkWithRedirect(row, 'FAVORITE')"
                >打开</el-button
              >
              <el-button link type="danger" :icon="Delete" @click="removeFavorite(row)">取消</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </MangoListPanel>
  </MangoListPage>
</template>

<script setup lang="ts">
import { Delete, Position } from '@element-plus/icons-vue';
import { MangoListPage, MangoListPanel, MangoSearchPanel } from '@mango/common';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { linkApi, normalizeApiId, openLinkWithRedirect, requestErrorMessage, type LinkFavorite } from '../api/link';

const rows = ref<LinkFavorite[]>([]);
const loading = ref(false);
const query = reactive({ keyword: '' });

async function loadRows() {
  loading.value = true;
  try {
    rows.value = await linkApi.listFavorites(query);
  } catch (error) {
    ElMessage.error(requestErrorMessage(error, '收藏网址加载失败'));
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = '';
  void loadRows();
}

async function removeFavorite(row: LinkFavorite) {
  const id = normalizeApiId(row.id);
  if (!id) {
    return;
  }
  await linkApi.deleteFavorite(id);
  ElMessage.success('已取消收藏');
  await loadRows();
}

onMounted(loadRows);
</script>
