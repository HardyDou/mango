<template>
  <section class="home-list-page" data-page="home.list" :data-state="pageState">
    <section class="job-toolbar" data-surface="home.list.search">
      <div class="job-toolbar-head">
        <div>
          <h2>首页列表</h2>
          <p>查看所有用户自定义首页，默认展示全部用户数据。</p>
        </div>
      </div>

      <el-form ref="queryFormRef" :model="query" class="job-search" inline @submit.prevent>
        <el-form-item label="关键词" prop="keyword" class="job-search-item job-search-item-wide">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="首页名称 / 路由标识"
            data-field="home.list.keyword"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户 ID" prop="userId" class="job-search-item">
          <el-input
            v-model="query.userId"
            clearable
            placeholder="全部用户"
            data-field="home.list.user-id"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态" prop="enabled" class="job-search-item job-search-item-small">
          <el-select v-model="query.enabled" clearable placeholder="全部" data-field="home.list.enabled">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item class="job-search-actions home-job-search-actions">
          <el-button
            v-auth="'home:list:view'"
            type="primary"
            :icon="Search"
            :loading="loading"
            data-action="home.list.search"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button :icon="RefreshLeft" data-action="home.list.reset" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="job-panel" data-surface="home.list.table-panel">
      <el-alert v-if="errorMessage" class="home-list-error" type="error" :closable="false" show-icon>
        <template #title>
          {{ errorMessage }}
          <el-button link type="primary" data-action="home.list.retry" @click="loadPages">重试</el-button>
        </template>
      </el-alert>

      <el-table
        v-loading="loading"
        :data="pages"
        row-key="id"
        stripe
        data-surface="home.list.table"
      >
        <template #empty>
          <el-empty :description="emptyDescription" />
        </template>
        <el-table-column label="首页名称" min-width="240" fixed="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="home-list-name" :data-record-key="`home-page:${row.id || row.routeKey}`">
              <strong>{{ row.name }}</strong>
              <span>{{ row.id || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="routeKey" label="路由标识" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.routeKey || '-' }}</template>
        </el-table-column>
        <el-table-column label="用户 ID" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.userId || '-' }}</template>
        </el-table-column>
        <el-table-column label="来源" width="108">
          <template #default>
            <el-tag type="info" effect="light">用户定义</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="86">
          <template #default="{ row }">
            <el-tag v-if="row.defaultPage" type="success" effect="light">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="job-pagination">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="pager.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          data-surface="home.list.pagination"
          @current-change="loadPages"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>
  </section>
</template>

<script setup lang="ts" name="HomeListPage">
import { computed, onMounted, reactive, ref } from 'vue';
import { RefreshLeft, Search } from '@element-plus/icons-vue';
import type { FormInstance } from 'element-plus';
import { homePageApi, type HomePageVO, type UserHomePageResult } from '@mango/home';
import '../../../../../job/src/views/job-admin.css';

type UserHomePageResponse = UserHomePageResult | { data?: UserHomePageResult | { data?: UserHomePageResult } };

const queryFormRef = ref<FormInstance>();
const loading = ref(false);
const loaded = ref(false);
const errorMessage = ref('');
const pages = ref<HomePageVO[]>([]);
const query = reactive<{
  keyword: string;
  userId: string;
  enabled?: boolean;
}>({
  keyword: '',
  userId: '',
  enabled: undefined,
});
const pager = reactive({
  page: 1,
  size: 20,
  total: 0,
});

const pageState = computed(() => {
  if (errorMessage.value) return 'error';
  if (loading.value) return 'loading';
  if (!loaded.value) return 'idle';
  return pages.value.length > 0 ? 'ready' : 'empty';
});
const hasQuery = computed(() => Boolean(query.keyword.trim() || query.userId.trim() || query.enabled !== undefined));
const emptyDescription = computed(() => hasQuery.value ? '暂无符合条件的用户定义首页' : '暂无用户定义首页');

onMounted(loadPages);

async function loadPages(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await homePageApi.pageUserPages({
      page: pager.page,
      size: pager.size,
      keyword: normalizeText(query.keyword),
      userId: normalizeText(query.userId),
      enabled: query.enabled,
    }) as unknown as UserHomePageResponse;
    const result = normalizePageResult(response);
    pages.value = (result.list || []).filter(page => !page.builtIn && !page.readOnly && !page.templateId);
    pager.total = Number(result.total || pages.value.length || 0);
    loaded.value = true;
  } catch (error) {
    pages.value = [];
    pager.total = 0;
    errorMessage.value = '首页列表加载失败，请稍后重试。';
  } finally {
    loading.value = false;
  }
}

function handleSearch(): void {
  pager.page = 1;
  void loadPages();
}

function handleReset(): void {
  query.keyword = '';
  query.userId = '';
  query.enabled = undefined;
  pager.page = 1;
  queryFormRef.value?.clearValidate();
  void loadPages();
}

function handlePageSizeChange(): void {
  pager.page = 1;
  void loadPages();
}

function normalizeText(value: string): string | undefined {
  const text = value.trim();
  return text || undefined;
}

function normalizePageResult(response: UserHomePageResponse | undefined | null): UserHomePageResult {
  if (!response || typeof response !== 'object') {
    return emptyPageResult();
  }
  if ('list' in response && Array.isArray(response.list)) {
    return response;
  }
  const firstData = response.data;
  if (firstData && typeof firstData === 'object' && 'list' in firstData && Array.isArray(firstData.list)) {
    return firstData;
  }
  const nestedData = firstData && typeof firstData === 'object' && 'data' in firstData ? firstData.data : undefined;
  if (nestedData && typeof nestedData === 'object' && Array.isArray(nestedData.list)) {
    return nestedData;
  }
  return emptyPageResult();
}

function emptyPageResult(): UserHomePageResult {
  return { list: [], total: 0, page: pager.page, size: pager.size };
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}
</script>

<style scoped>
.home-list-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 100%;
}

.home-list-error {
  margin-bottom: 12px;
}

.home-job-search-actions {
  margin-left: auto;
}

.home-list-name {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.home-list-name strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-list-name span {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

</style>
