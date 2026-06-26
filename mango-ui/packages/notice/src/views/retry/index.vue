<template>
  <div class="notice-retry-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-retry-page__header">
          <span>失败重试</span>
          <el-space>
            <el-button :disabled="selectedRecords.length === 0" :loading="actionLoading" @click="openBatchHandle('manualSuccess')">
              批量成功
            </el-button>
            <el-button :disabled="selectedRecords.length === 0" :loading="actionLoading" @click="openBatchHandle('ignore')">
              批量忽略
            </el-button>
            <el-button :disabled="selectedRecords.length === 0" :loading="actionLoading" @click="handleBatchRetry">
              批量重试
            </el-button>
          </el-space>
        </div>
      </template>

      <el-table :data="records" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column label="业务域" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ domainText(row.bizGroup) }}</template>
        </el-table-column>
        <el-table-column label="消息名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ recordMessageName(row) }}</template>
        </el-table-column>
        <el-table-column label="标题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.renderedTitle || '-' }}</template>
        </el-table-column>
        <el-table-column label="内容摘要" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ contentSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="接收人账号名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ recipientText(row) }}</template>
        </el-table-column>
        <el-table-column label="渠道" width="120" align="center">
          <template #default="{ row }">{{ channelLabel(row.channelType) }}</template>
        </el-table-column>
        <el-table-column label="失败状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="240" show-overflow-tooltip />
        <el-table-column prop="retryCount" label="失败次数" width="100" align="center" />
        <el-table-column prop="sentAt" label="最后失败时间" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" :loading="actionLoading" @click="handleRetry(row)">重试</el-button>
              <el-dropdown @command="command => handleMoreCommand(command, row)">
                <el-button link type="primary">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="manualSuccess">标记成功</el-dropdown-item>
                    <el-dropdown-item command="ignore">忽略失败</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="失败记录详情" width="860px">
      <div v-if="currentRecord" class="notice-retry-page__detail">
        <section>
          <h3>基础信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="业务域">{{ domainText(currentRecord.bizGroup) }}</el-descriptions-item>
            <el-descriptions-item label="消息名称">{{ recordMessageName(currentRecord) }}</el-descriptions-item>
            <el-descriptions-item label="标题" :span="2">{{ currentRecord.renderedTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="内容" :span="2">
              <pre class="notice-retry-page__pre">{{ currentRecord.renderedContent || '-' }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </section>
        <section>
          <h3>发送情况</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="渠道">{{ channelLabel(currentRecord.channelType) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusLabel(currentRecord.status) }}</el-descriptions-item>
            <el-descriptions-item label="接收人">{{ currentRecord.recipientName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="接收账号">{{ recipientAccountText(currentRecord) }}</el-descriptions-item>
            <el-descriptions-item label="失败编码">{{ currentRecord.failCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="失败原因">{{ currentRecord.failReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="失败次数">{{ currentRecord.retryCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="最后失败时间">{{ currentRecord.sentAt || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>
        <section>
          <h3>报文信息</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="请求明细">
              <pre class="notice-retry-page__code">{{ prettySnapshot(currentRecord.requestSnapshot) }}</pre>
            </el-descriptions-item>
            <el-descriptions-item label="响应明细">
              <pre class="notice-retry-page__code">{{ prettySnapshot(currentRecord.responseSnapshot) }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </section>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" :title="handleTitle" width="520px">
      <el-form label-width="84px">
        <el-form-item label="处理原因" required>
          <el-input
            v-model="handleReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入处理原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitHandle">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getSendRecords,
  ignoreSendRecord,
  ignoreSendRecords,
  markSendRecordManualSuccess,
  markSendRecordsManualSuccess,
  retrySendRecord,
  retrySendRecords,
} from '../../api/notice';
import type { NoticeChannelType, NoticeSendRecord, NoticeSendStatus } from '../../types/notice';
import { useNoticeDomains } from '../../components/useNoticeDomains';

const failedStatuses: NoticeSendStatus[] = ['FAILED', 'RETRY_WAITING', 'FINAL_FAILED'];
const loading = ref(false);
const actionLoading = ref(false);
const rawRecords = ref<NoticeSendRecord[]>([]);
const selectedRecords = ref<NoticeSendRecord[]>([]);
const detailVisible = ref(false);
const currentRecord = ref<NoticeSendRecord>();
const handleVisible = ref(false);
const handleReason = ref('');
const handleAction = ref<'manualSuccess' | 'ignore'>('manualSuccess');
const handleMode = ref<'single' | 'batch'>('single');
const handleRecord = ref<NoticeSendRecord>();
const { domainText, loadDomains } = useNoticeDomains();

const records = computed(() => rawRecords.value
  .filter(item => failedStatuses.includes(item.status))
  .sort((left, right) => stringValue(right.sentAt).localeCompare(stringValue(left.sentAt))));

async function load() {
  loading.value = true;
  try {
    const pages = await Promise.all(failedStatuses.map(status => getSendRecords({ status, pageSize: 50 })));
    rawRecords.value = pages.flatMap(page => page.list || []);
  } finally {
    loading.value = false;
  }
}

const handleTitle = computed(() => {
  if (handleMode.value === 'batch') {
    return handleAction.value === 'manualSuccess' ? '批量成功' : '批量忽略';
  }
  return handleAction.value === 'manualSuccess' ? '标记成功' : '忽略失败';
});

function handleSelectionChange(selection: NoticeSendRecord[]) {
  selectedRecords.value = selection;
}

function openDetail(row: NoticeSendRecord) {
  currentRecord.value = row;
  detailVisible.value = true;
}

async function handleRetry(row: NoticeSendRecord) {
  await ElMessageBox.confirm('确认立即重试该失败记录？', '重试确认', { type: 'warning' });
  actionLoading.value = true;
  try {
    await retrySendRecord(row.id);
    ElMessage.success('已提交重试');
    await load();
  } finally {
    actionLoading.value = false;
  }
}

async function handleBatchRetry() {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请选择失败记录');
    return;
  }
  await ElMessageBox.confirm(`确认立即重试选中的 ${selectedRecords.value.length} 条失败记录？`, '批量重试确认', {
    type: 'warning',
  });
  actionLoading.value = true;
  try {
    await retrySendRecords(selectedRecords.value.map(item => item.id));
    ElMessage.success('已提交批量重试');
    await load();
  } finally {
    actionLoading.value = false;
  }
}

function openBatchHandle(action: 'manualSuccess' | 'ignore') {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请选择失败记录');
    return;
  }
  handleAction.value = action;
  handleMode.value = 'batch';
  handleRecord.value = undefined;
  handleReason.value = '';
  handleVisible.value = true;
}

function handleMoreCommand(command: string | number | object, row: NoticeSendRecord) {
  if (command !== 'manualSuccess' && command !== 'ignore') {
    return;
  }
  handleAction.value = command;
  handleMode.value = 'single';
  handleRecord.value = row;
  handleReason.value = '';
  handleVisible.value = true;
}

async function submitHandle() {
  const reason = handleReason.value.trim();
  if (!reason) {
    ElMessage.warning('请输入处理原因');
    return;
  }
  if (handleMode.value === 'single' && !handleRecord.value) {
    ElMessage.warning('请选择失败记录');
    return;
  }
  if (handleMode.value === 'batch' && selectedRecords.value.length === 0) {
    ElMessage.warning('请选择失败记录');
    return;
  }
  actionLoading.value = true;
  try {
    if (handleAction.value === 'manualSuccess') {
      if (handleMode.value === 'batch') {
        await markSendRecordsManualSuccess(selectedRecords.value.map(item => item.id), reason);
      } else if (handleRecord.value) {
        await markSendRecordManualSuccess(handleRecord.value.id, reason);
      }
    } else {
      if (handleMode.value === 'batch') {
        await ignoreSendRecords(selectedRecords.value.map(item => item.id), reason);
      } else if (handleRecord.value) {
        await ignoreSendRecord(handleRecord.value.id, reason);
      }
    }
    ElMessage.success('处理成功');
    handleVisible.value = false;
    await load();
  } finally {
    actionLoading.value = false;
  }
}

function channelLabel(channel: NoticeChannelType) {
  const labels: Record<NoticeChannelType, string> = {
    SITE: '系统消息',
    SMS: '短信',
    EMAIL: '邮件',
    WECHAT_OFFICIAL: '微信公众号',
    WECOM: '企业微信',
    DINGTALK: '钉钉',
  };
  return labels[channel] || channel;
}

function recordMessageName(row: NoticeSendRecord) {
  return row.messageName || row.bizName || row.renderedTitle || '-';
}

function recipientText(row: NoticeSendRecord) {
  const recipientName = stringValue(row.recipientName);
  const recipientAccount = recipientAccountText(row);
  if (recipientName && recipientAccount !== '-') {
    return `${recipientName} / ${recipientAccount}`;
  }
  if (recipientName) {
    return recipientName;
  }
  if (recipientAccount !== '-') {
    return recipientAccount;
  }
  const request = parseSnapshot(row.requestSnapshot);
  const values = [
    stringValue(request.recipientName),
    stringValue(request.mobile),
    stringValue(request.email),
  ].filter(Boolean);
  return values[0] || '-';
}

function recipientAccountText(row: NoticeSendRecord) {
  return stringValue(row.recipientAccount) || '-';
}

function contentSummary(row: NoticeSendRecord) {
  return row.renderedContent || row.renderedTitle || '-';
}

function statusLabel(status: NoticeSendStatus) {
  const labels: Record<NoticeSendStatus, string> = {
    PENDING: '待发送',
    SENDING: '发送中',
    SUCCESS: '成功',
    FAILED: '失败',
    RETRY_WAITING: '等待重试',
    FINAL_FAILED: '最终失败',
    MANUAL_SUCCESS: '人工成功',
    IGNORED: '已忽略',
    CANCELED: '已取消',
  };
  return labels[status] || status;
}

function statusTag(status: NoticeSendStatus) {
  if (status === 'FINAL_FAILED') return 'danger';
  if (status === 'RETRY_WAITING') return 'warning';
  if (status === 'MANUAL_SUCCESS') return 'success';
  return 'info';
}

function prettySnapshot(snapshot?: string) {
  if (!snapshot) return '-';
  try {
    return JSON.stringify(JSON.parse(snapshot), null, 2);
  } catch {
    return snapshot;
  }
}

function parseSnapshot(snapshot?: string) {
  if (!snapshot) return {};
  try {
    const parsed = JSON.parse(snapshot);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {};
  } catch {
    return {};
  }
}

function stringValue(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : '';
}

onMounted(() => {
  loadDomains();
  load();
});
</script>

<style scoped>
.notice-retry-page {
  padding: 0;
}

.notice-retry-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
