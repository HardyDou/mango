<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <MangoListPage class="notice-announcement-user-page" data-page="notice.announcement-user">
    <template #search>
      <MangoSearchPanel :model="query" :columns="4" @search="search" @reset="resetSearch">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="搜索公告" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="阅读状态">
          <el-checkbox v-model="query.unreadOnly">仅未读</el-checkbox>
        </el-form-item>
        <el-form-item label="确认状态">
          <el-checkbox v-model="query.pendingConfirmOnly">仅待确认</el-checkbox>
        </el-form-item>
      </MangoSearchPanel>
    </template>
    <MangoListPanel>
      <template #view-actions>
        <el-button type="primary" plain :loading="loading" @click="loadData">刷新</el-button>
      </template>
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        border
        stripe
        data-surface="notice.announcement-user.table"
      >
        <el-table-column label="标题" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <el-badge is-dot :hidden="row.readStatus === 'READ'">
              <el-button link type="primary" @click="openDetail(row)">{{ row.title }}</el-button>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column label="确认" width="110">
          <template #default="{ row }">
            <el-tag :type="confirmTag(row.confirmStatus)">{{ confirmLabel(row.confirmStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.pinned" effect="plain">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #pagination>
        <Pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>

    <MangoDialog v-model="detailVisible" title="公告详情" width="760px" destroy-on-close>
      <template v-if="current">
        <h2 class="detail-title">{{ current.title }}</h2>
        <div class="detail-meta">
          <span>{{ current.publishTime || '-' }}</span>
          <el-tag v-if="current.confirmRequired" effect="plain" type="warning">{{
            confirmLabel(current.confirmStatus)
          }}</el-tag>
        </div>
        <div class="content-box">{{ current.content }}</div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="current?.confirmStatus === 'PENDING'"
          type="primary"
          :loading="confirming"
          @click="confirmCurrent"
        >
          确认已读
        </el-button>
      </template>
    </MangoDialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { MangoDialog, MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { confirmMyAnnouncement, getMyAnnouncement, getMyAnnouncements } from '../../api/notice';
import type { NoticeAnnouncement, NoticeAnnouncementConfirmStatus } from '../../types/notice';

const loading = ref(false);
const confirming = ref(false);
const detailVisible = ref(false);
const rows = ref<NoticeAnnouncement[]>([]);
const total = ref(0);
const current = ref<NoticeAnnouncement>();
const query = reactive<{
  pageNum: number;
  pageSize: number;
  keyword?: string;
  unreadOnly?: boolean;
  pendingConfirmOnly?: boolean;
}>({
  pageNum: 1,
  pageSize: 10,
});
const route = useRoute();

async function loadData() {
  loading.value = true;
  try {
    const result = await getMyAnnouncements(query);
    rows.value = result.list || [];
    total.value = Number(result.total || rows.value.length);
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  void loadData();
}

function resetSearch() {
  Object.assign(query, { pageNum: 1, keyword: undefined, unreadOnly: undefined, pendingConfirmOnly: undefined });
  void loadData();
}

async function openDetail(row: NoticeAnnouncement) {
  current.value = await getMyAnnouncement(row.id);
  detailVisible.value = true;
  loadData();
}

async function confirmCurrent() {
  if (!current.value) {
    return;
  }
  confirming.value = true;
  try {
    await confirmMyAnnouncement(current.value.id);
    ElMessage.success('已确认');
    current.value = await getMyAnnouncement(current.value.id);
    await loadData();
  } finally {
    confirming.value = false;
  }
}

function confirmLabel(status?: NoticeAnnouncementConfirmStatus) {
  return ({ NOT_REQUIRED: '无需确认', PENDING: '待确认', CONFIRMED: '已确认' } as Record<string, string>)[
    status || 'NOT_REQUIRED'
  ];
}

function confirmTag(status?: NoticeAnnouncementConfirmStatus) {
  return status === 'PENDING' ? 'warning' : status === 'CONFIRMED' ? 'success' : 'info';
}

onMounted(async () => {
  await loadData();
  const id = route.query.id;
  if (typeof id === 'string' && id) {
    await nextTick();
    await openDetail({ id } as NoticeAnnouncement);
  }
});
</script>

<style scoped>
.detail-title {
  margin: 0 0 8px;
  font-size: 20px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--el-text-color-secondary);
}

.content-box {
  margin-top: 16px;
  padding: 12px;
  min-height: 160px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  white-space: pre-wrap;
}
</style>
