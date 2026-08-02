<template>
  <MangoListPage class="notice-site-message-page" data-page="notice.site-message">
    <template #search>
      <MangoSearchPanel :model="query" collapsible :collapsed-count="4" @search="search" @reset="resetSearch">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="标题/内容" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="业务域">
          <el-select
            v-model="query.bizGroup"
            clearable
            filterable
            placeholder="请选择业务域"
            class="notice-filter-form__select"
            :loading="domainLoading"
          >
            <el-option v-for="item in domainOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息分类">
          <el-select v-model="query.category" clearable placeholder="全部" class="notice-filter-form__select">
            <el-option label="审批类消息" value="APPROVAL" />
            <el-option label="系统通知" value="SYSTEM" />
            <el-option label="业务通知" value="BUSINESS" />
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
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button plain :disabled="selectedIds.length === 0" @click="markSelectedRead">批量已读</el-button>
        <el-button plain @click="markAllRead">全部已读</el-button>
      </template>
      <template #view-actions>
        <el-button type="primary" plain @click="loadMessages">刷新</el-button>
      </template>
      <el-table
        v-loading="loading"
        :data="messages"
        row-key="id"
        border
        stripe
        data-surface="notice.site-message.table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="业务域" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ domainText(row.bizGroup) }}</template>
        </el-table-column>
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
        <el-table-column label="消息动作" min-width="220">
          <template #default="{ row }">
            <div v-if="visibleActions(row).length > 0" class="notice-message-actions">
              <el-button
                v-for="action in visibleActions(row)"
                :key="action.actionCode"
                size="small"
                plain
                :type="action.interactionType === 'EVENT' ? 'primary' : 'success'"
                :disabled="isActionDisabled(action)"
                @click="handleMessageAction(row, action)"
              >
                {{ action.actionLabel }}
              </el-button>
            </div>
            <span v-else class="notice-message-actions__empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.readStatus !== 'READ'" link type="primary" @click="markRead(row.id)">已读</el-button>
            <el-button link type="danger" @click="removeMessage(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :total="total"
          @pagination="loadMessages"
        />
      </template>
    </MangoListPanel>

    <NoticeDetailDialog v-model="detailVisible" :message="currentMessage" @action="handleDetailAction" />

    <el-dialog v-model="flowDialogVisible" :title="flowDialogTitle" width="560px" class="notice-flow-dialog">
      <div v-if="flowContext" class="notice-flow">
        <div class="notice-flow__summary">
          <div class="notice-flow__title">{{ flowContext.message.title }}</div>
          <div class="notice-flow__desc">{{ flowContext.message.content }}</div>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="业务对象">
            {{ flowContext.message.subject?.subjectName || flowContext.message.bizName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="业务域">
            {{ domainText(flowContext.message.bizGroup) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理动作">
            {{ flowContext.action?.actionLabel || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="flowDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFlowAction">提交处理</el-button>
      </template>
    </el-dialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import NoticeDetailDialog from '../components/NoticeDetailDialog.vue';
import {
  deleteMySiteMessage,
  executeMySiteMessageAction,
  getMySiteMessageDetail,
  getMySiteMessages,
  markAllMySiteMessagesRead,
  markMySiteMessageRead,
  markMySiteMessagesRead,
} from '../api/notice';
import type {
  NoticePriority,
  NoticeSiteMessage,
  NoticeSiteMessageAction,
  NoticeSiteMessageCategory,
} from '../types/notice';
import { useNoticeDomains } from '../components/useNoticeDomains';
import { buildNoticeActionInput } from './interaction';
import type { NoticeInteractionPayload } from './interaction';
import { isNoticeActionDisabled, visibleNoticeActions } from './messagePresentation';

const loading = ref(false);
const props = defineProps<{
  category?: NoticeSiteMessageCategory;
  unreadOnly?: boolean;
}>();
const messages = ref<NoticeSiteMessage[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const currentMessage = ref<NoticeSiteMessage>();
const flowDialogVisible = ref(false);
const flowContext = ref<{
  message: NoticeSiteMessage;
  action?: NoticeSiteMessageAction;
  targetKey?: string;
  input?: Record<string, unknown>;
}>();
const selectedIds = ref<string[]>([]);
const readFilter = ref<'ALL' | 'UNREAD' | 'READ'>(props.unreadOnly ? 'UNREAD' : 'ALL');
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  unreadOnly: undefined as boolean | undefined,
  category: props.category,
  keyword: '',
  bizGroup: '',
  priority: undefined as NoticePriority | undefined,
});
const { domainLoading, domainOptions, domainText, loadDomains } = useNoticeDomains();

async function loadMessages() {
  loading.value = true;
  try {
    const result = await getMySiteMessages({
      ...query,
      unreadOnly: readFilter.value === 'UNREAD' ? true : undefined,
    });
    const list = result.list || [];
    messages.value = readFilter.value === 'READ' ? list.filter((item) => item.readStatus === 'READ') : list;
    const totalValue = Number(result.total ?? messages.value.length);
    total.value = Number.isFinite(totalValue) ? totalValue : messages.value.length;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  void loadMessages();
}

function resetSearch() {
  query.pageNum = 1;
  query.keyword = '';
  query.bizGroup = '';
  query.category = undefined;
  query.priority = undefined;
  readFilter.value = 'ALL';
  void loadMessages();
}

async function openDetail(row: NoticeSiteMessage) {
  if (row.bizType === 'notice.announcement.published' && row.bizId) {
    emit('announcement', row.bizId);
    await markMySiteMessageRead(row.id);
    await loadMessages();
    return;
  }
  currentMessage.value = await getMySiteMessageDetail(row.id);
  detailVisible.value = true;
}

async function markRead(id: string) {
  await markMySiteMessageRead(id);
  ElMessage.success('已标记为已读');
  await loadMessages();
}

function handleSelectionChange(rows: NoticeSiteMessage[]) {
  selectedIds.value = rows.map((row) => row.id);
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
  (event: 'announcement', id: string): void;
  (event: 'interaction', payload: NoticeInteractionPayload): void;
}>();

function priorityText(priority: NoticePriority) {
  return (
    ({ LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' } as Record<NoticePriority, string>)[priority] || priority
  );
}

function priorityTag(priority: NoticePriority) {
  return (
    (
      { LOW: 'info', NORMAL: 'info', HIGH: 'warning', URGENT: 'danger' } as Record<
        NoticePriority,
        'info' | 'warning' | 'danger'
      >
    )[priority] || 'info'
  );
}

function visibleActions(row: NoticeSiteMessage) {
  return visibleNoticeActions(row).slice(0, 2);
}

function isActionDisabled(action: NoticeSiteMessageAction) {
  return isNoticeActionDisabled(action);
}

function buildActionInput(row: NoticeSiteMessage, action: NoticeSiteMessageAction): Record<string, unknown> {
  return buildNoticeActionInput(row, action);
}

async function handleMessageAction(row: NoticeSiteMessage, action: NoticeSiteMessageAction) {
  const input = buildActionInput(row, action);
  if (action.interactionType === 'ROUTE') {
    const targetType = action.target?.targetType || row.target?.targetType;
    const targetKey = action.target?.targetKey || row.target?.targetKey;
    if (targetType === 'FLOW') {
      openFlowDialog(row, action, targetKey, input);
      return;
    }
    emit('interaction', {
      message: row,
      action,
      targetKey,
      targetType: 'ROUTE',
      params: input,
      onComplete: (success) => {
        if (success && currentMessage.value?.id === row.id) {
          detailVisible.value = false;
        }
      },
    });
    return;
  }
  await executeEventAction(row, action, true, input);
}

async function executeEventAction(
  row: NoticeSiteMessage,
  action: NoticeSiteMessageAction,
  requireConfirm: boolean,
  input = buildActionInput(row, action),
) {
  if (action.confirmRequired && requireConfirm) {
    await ElMessageBox.confirm(`确认执行“${action.actionLabel}”吗？`, '操作确认', { type: 'warning' });
  }
  await executeMySiteMessageAction(row.id, action.actionCode, input);
  ElMessage.success('操作已提交');
  if (currentMessage.value?.id === row.id) {
    detailVisible.value = false;
  }
  await loadMessages();
}

function handleDetailAction(action: NoticeSiteMessageAction) {
  if (!currentMessage.value) return;
  void handleMessageAction(currentMessage.value, action);
}

function openFlowDialog(
  row: NoticeSiteMessage,
  action: NoticeSiteMessageAction,
  targetKey?: string,
  input?: Record<string, unknown>,
) {
  flowContext.value = {
    message: row,
    action,
    targetKey,
    input,
  };
  flowDialogVisible.value = true;
}

const flowDialogTitle = '业务流程处理';

async function submitFlowAction() {
  if (!flowContext.value) return;
  const eventAction = (flowContext.value.message.actions || []).find(
    (action) => action.interactionType === 'EVENT' && !isActionDisabled(action),
  );
  if (eventAction) {
    await executeEventAction(flowContext.value.message, eventAction, false, {
      ...buildActionInput(flowContext.value.message, eventAction),
      ...(flowContext.value.input || {}),
    });
  } else {
    ElMessage.success('处理已提交');
  }
  flowDialogVisible.value = false;
  detailVisible.value = false;
}

onMounted(() => {
  void loadDomains();
  void loadMessages();
});

watch(
  () => [props.category, props.unreadOnly] as const,
  ([category, unreadOnly]) => {
    query.category = category;
    readFilter.value = unreadOnly ? 'UNREAD' : 'ALL';
    search();
  },
);
</script>

<style scoped>
.notice-filter-form__select {
  width: 100%;
}

.notice-message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.notice-message-actions :deep(.el-button) {
  margin-left: 0;
}

.notice-message-actions__empty {
  color: var(--el-text-color-placeholder);
}

.notice-flow {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.notice-flow__summary {
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.notice-flow__title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-flow__desc {
  margin-top: 6px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>
