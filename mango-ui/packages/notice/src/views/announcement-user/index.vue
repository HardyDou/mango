<template>
  <div class="notice-announcement-user-page">
    <div class="page-header">
      <h1>公告</h1>
      <div class="page-actions">
        <el-checkbox v-model="query.unreadOnly" @change="loadData">未读</el-checkbox>
        <el-checkbox v-model="query.pendingConfirmOnly" @change="loadData">待确认</el-checkbox>
        <el-input v-model="query.keyword" clearable placeholder="搜索公告" class="filter-control" @keyup.enter="loadData" />
        <el-button :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="rows" border stripe v-loading="loading">
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
    </el-card>

    <el-dialog v-model="detailVisible" title="公告详情" width="760px" destroy-on-close>
      <template v-if="current">
        <h2 class="detail-title">{{ current.title }}</h2>
        <div class="detail-meta">
          <span>{{ current.publishTime || '-' }}</span>
          <el-tag v-if="current.confirmRequired" effect="plain" type="warning">{{ confirmLabel(current.confirmStatus) }}</el-tag>
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
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { confirmMyAnnouncement, getMyAnnouncement, getMyAnnouncements } from '../../api/notice';
import type { NoticeAnnouncement, NoticeAnnouncementConfirmStatus } from '../../types/notice';

const loading = ref(false);
const confirming = ref(false);
const detailVisible = ref(false);
const rows = ref<NoticeAnnouncement[]>([]);
const current = ref<NoticeAnnouncement>();
const query = reactive<{ keyword?: string; unreadOnly?: boolean; pendingConfirmOnly?: boolean }>({});
const route = useRoute();

async function loadData() {
  loading.value = true;
  try {
    const result = await getMyAnnouncements({ pageNum: 1, pageSize: 50, ...query });
    rows.value = result.list || [];
  } finally {
    loading.value = false;
  }
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
  return ({ NOT_REQUIRED: '无需确认', PENDING: '待确认', CONFIRMED: '已确认' } as Record<string, string>)[status || 'NOT_REQUIRED'];
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
.notice-announcement-user-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header {
  justify-content: space-between;
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.filter-control {
  width: 220px;
}

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
