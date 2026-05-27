<template>
  <div class="notice-site-message-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-site-message-page__header">
          <span>系统消息</span>
          <div class="notice-site-message-page__actions">
            <el-checkbox v-model="query.unreadOnly" @change="loadMessages">只看未读</el-checkbox>
            <el-button :disabled="selectedIds.length === 0" @click="markSelectedRead">批量已读</el-button>
            <el-button @click="markAllRead">全部已读</el-button>
            <el-button type="primary" plain @click="loadMessages">刷新</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的消息" name="mine">
          <el-table :data="messages" border stripe v-loading="loading" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column prop="bizType" label="业务类型" width="160" />
            <el-table-column prop="bizId" label="业务对象" width="160" />
            <el-table-column prop="priority" label="优先级" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.readStatus === 'READ' ? 'info' : 'warning'">
                  {{ row.readStatus === 'READ' ? '已读' : '未读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="220" fixed="right">
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
        </el-tab-pane>

        <el-tab-pane label="发送系统消息" name="send">
          <el-form :model="sendForm" label-width="120px" class="notice-site-message-page__send-form">
            <el-form-item label="接收用户ID"><el-input v-model="sendForm.userId" placeholder="请输入用户ID" /></el-form-item>
            <el-form-item label="业务类型"><el-input v-model="sendForm.bizType" placeholder="例如 SYSTEM_NOTICE" /></el-form-item>
            <el-form-item label="业务ID"><el-input v-model="sendForm.bizId" /></el-form-item>
            <el-form-item label="标题"><el-input v-model="sendForm.title" /></el-form-item>
            <el-form-item label="内容"><el-input v-model="sendForm.content" type="textarea" :rows="4" /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <NoticeDetailDialog v-model="detailVisible" :message="currentMessage" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import NoticeDetailDialog from '../../components/NoticeDetailDialog.vue';
import {
  deleteMySiteMessage,
  getMySiteMessageDetail,
  getMySiteMessages,
  markAllMySiteMessagesRead,
  markMySiteMessageRead,
  markMySiteMessagesRead,
  sendSiteNotice,
} from '../../api/notice';
import type { NoticeSiteMessage } from '../../types/notice';

const activeTab = ref('mine');
const loading = ref(false);
const sending = ref(false);
const messages = ref<NoticeSiteMessage[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const currentMessage = ref<NoticeSiteMessage>();
const selectedIds = ref<string[]>([]);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  unreadOnly: false,
});
const sendForm = reactive({
  userId: '',
  bizType: 'SYSTEM_NOTICE',
  bizId: '',
  title: '',
  content: '',
});

async function loadMessages() {
  loading.value = true;
  try {
    const result = await getMySiteMessages(query);
    messages.value = result.list || [];
    total.value = result.total || 0;
  } finally {
    loading.value = false;
  }
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
  if (selectedIds.value.length === 0) {
    return;
  }
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

async function sendMessage() {
  sending.value = true;
  try {
    await sendSiteNotice({
      bizType: sendForm.bizType,
      bizId: sendForm.bizId,
      userId: sendForm.userId,
      title: sendForm.title,
      content: sendForm.content,
      priority: 'NORMAL',
    });
    ElMessage.success('系统消息已发送');
    activeTab.value = 'mine';
    await loadMessages();
  } finally {
    sending.value = false;
  }
}

onMounted(loadMessages);
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

.notice-site-message-page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.notice-site-message-page__send-form {
  max-width: 720px;
}
</style>
