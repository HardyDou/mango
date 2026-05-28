<template>
  <div class="notice-site-message-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-site-message-page__header">
          <span>我的消息</span>
          <div class="notice-site-message-page__actions">
            <el-button :disabled="selectedIds.length === 0" @click="markSelectedRead">批量已读</el-button>
            <el-button @click="markAllRead">全部已读</el-button>
            <el-button type="primary" plain @click="loadMessages">刷新</el-button>
            <el-button type="primary" @click="openReceiveSetting">设置</el-button>
          </div>
        </div>
      </template>

      <el-form :model="query" inline class="notice-filter-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="标题/内容" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="业务域">
          <el-select v-model="query.bizGroup" clearable filterable placeholder="请选择业务域" class="notice-filter-form__select">
            <el-option v-for="item in bizGroupOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" clearable placeholder="全部" class="notice-filter-form__select">
            <el-option label="低" value="LOW" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="readFilter" class="notice-filter-form__select" @change="search">
            <el-option label="全部" value="ALL" />
            <el-option label="未读" value="UNREAD" />
            <el-option label="已读" value="READ" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="messages" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="bizGroup" label="业务域" width="120" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="bizName" label="消息名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityTag(row.priority)">{{ priorityText(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.readStatus === 'READ' ? 'info' : 'warning'">
              {{ row.readStatus === 'READ' ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.readStatus !== 'READ'" link type="primary" @click="markRead(row.id)">已读</el-button>
            <el-button link type="danger" @click="removeMessage(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="notice-site-message-page__pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadMessages"
          @current-change="loadMessages"
        />
      </div>
    </el-card>

    <NoticeDetailDialog v-model="detailVisible" :message="currentMessage" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import NoticeDetailDialog from '../components/NoticeDetailDialog.vue';
import {
  deleteMySiteMessage,
  getBusinessTypes,
  getMySiteMessageDetail,
  getMySiteMessages,
  markAllMySiteMessagesRead,
  markMySiteMessageRead,
  markMySiteMessagesRead,
} from '../api/notice';
import type { NoticePriority, NoticeSiteMessage } from '../types/notice';

const loading = ref(false);
const messages = ref<NoticeSiteMessage[]>([]);
const businessTypes = ref<Array<{ bizGroup?: string }>>([]);
const total = ref(0);
const detailVisible = ref(false);
const currentMessage = ref<NoticeSiteMessage>();
const selectedIds = ref<string[]>([]);
const readFilter = ref<'ALL' | 'UNREAD' | 'READ'>('ALL');
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  unreadOnly: undefined as boolean | undefined,
  keyword: '',
  bizGroup: '',
  priority: undefined as NoticePriority | undefined,
});

const bizGroupOptions = computed(() => Array.from(new Set(
  businessTypes.value.map(item => item.bizGroup).filter((item): item is string => Boolean(item)),
)));

async function loadMessages() {
  loading.value = true;
  try {
    const result = await getMySiteMessages({
      ...query,
      unreadOnly: readFilter.value === 'UNREAD' ? true : undefined,
    });
    const list = result.list || [];
    messages.value = readFilter.value === 'READ' ? list.filter(item => item.readStatus === 'READ') : list;
    total.value = result.total || messages.value.length;
  } finally {
    loading.value = false;
  }
}

async function loadBusinessTypes() {
  const result = await getBusinessTypes({ pageNum: 1, pageSize: 200 });
  businessTypes.value = result.list || [];
}

function search() {
  query.pageNum = 1;
  void loadMessages();
}

function resetSearch() {
  query.pageNum = 1;
  query.keyword = '';
  query.bizGroup = '';
  query.priority = undefined;
  readFilter.value = 'ALL';
  void loadMessages();
}

async function openDetail(row: NoticeSiteMessage) {
  currentMessage.value = await getMySiteMessageDetail(row.id);
  detailVisible.value = true;
}

async function markRead(id: string) {
  await markMySiteMessageRead(id);
  ElMessage.success('已标记为已读');
  await loadMessages();
}

function handleSelectionChange(rows: NoticeSiteMessage[]) {
  selectedIds.value = rows.map(row => row.id);
}

async function markSelectedRead() {
  if (selectedIds.value.length === 0) return;
  await markMySiteMessagesRead(selectedIds.value);
  ElMessage.success('已批量标记为已读');
  selectedIds.value = [];
  await loadMessages();
}

async function markAllRead() {
  await markAllMySiteMessagesRead();
  ElMessage.success('已全部标记为已读');
  await loadMessages();
}

async function removeMessage(id: string) {
  await ElMessageBox.confirm('确认删除这条系统消息吗？', '删除确认', { type: 'warning' });
  await deleteMySiteMessage(id);
  ElMessage.success('已删除');
  await loadMessages();
}

const emit = defineEmits<{
  (event: 'settings'): void;
}>();

function openReceiveSetting() {
  emit('settings');
}

function priorityText(priority: NoticePriority) {
  return ({ LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' } as Record<NoticePriority, string>)[priority] || priority;
}

function priorityTag(priority: NoticePriority) {
  return ({ LOW: 'info', NORMAL: '', HIGH: 'warning', URGENT: 'danger' } as Record<NoticePriority, '' | 'info' | 'warning' | 'danger'>)[priority] || '';
}

onMounted(() => {
  void loadBusinessTypes();
  void loadMessages();
});
</script>

<style scoped>
.notice-site-message-page {
  padding: 0;
}

.notice-site-message-page__header,
.notice-site-message-page__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notice-filter-form {
  margin-bottom: 12px;
}

.notice-filter-form__select {
  width: 140px;
}

.notice-site-message-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
