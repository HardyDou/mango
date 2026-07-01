<template>
  <div class="link-page" data-page="link-company">
    <section class="link-toolbar">
      <div class="link-toolbar-head">
        <h2>公司网址</h2>
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
      <el-alert v-if="errorMessage" class="link-error" type="error" :closable="false" show-icon>
        <template #title>{{ errorMessage }}</template>
      </el-alert>
      <el-empty v-if="!loading && rows.length === 0" description="暂无公司网址" />
      <div v-loading="loading" class="link-card-grid">
        <article v-for="row in rows" :key="String(row.id)" class="link-url-card">
          <div class="link-url-icon">
            <el-image v-if="row.iconUrl" :src="row.iconUrl" fit="cover">
              <template #error><el-icon><LinkIcon /></el-icon></template>
            </el-image>
            <el-icon v-else><LinkIcon /></el-icon>
          </div>
          <div class="link-url-body">
            <h3>{{ row.name || '-' }}</h3>
            <p>{{ row.summary || row.url || '-' }}</p>
            <div class="link-tags">
              <el-tag v-if="row.recommended" type="success" size="small">推荐</el-tag>
              <el-tag v-for="tag in row.tags || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
          </div>
          <div class="link-url-actions">
            <el-button type="primary" :icon="Position" @click="openLinkWithRedirect(row, 'COMPANY')">打开</el-button>
            <el-button :icon="Star" @click="favorite(row)">收藏</el-button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Link as LinkIcon, Position, Refresh, Search, Star } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { linkApi, normalizeApiId, openLinkWithRedirect, requestErrorMessage, type LinkItem } from '../api/link';

const rows = ref<LinkItem[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const query = reactive({ keyword: '' });

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    rows.value = await linkApi.listCompanyLinks(query);
  } catch (error) {
    errorMessage.value = requestErrorMessage(error, '公司网址加载失败');
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = '';
  void loadRows();
}

async function favorite(row: LinkItem) {
  const id = normalizeApiId(row.id);
  if (!id) {
    return;
  }
  await linkApi.createFavorite(id);
  ElMessage.success('已收藏');
}

onMounted(loadRows);
</script>
