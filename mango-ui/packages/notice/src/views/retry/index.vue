<template>
  <div class="notice-retry-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-retry-page__header">
          <span>失败重试</span>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>

      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column prop="bizType" label="消息编码" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bizId" label="业务单号" width="160" show-overflow-tooltip />
        <el-table-column label="渠道" width="120">
          <template #default="{ row }">{{ channelLabel(row.channelType) }}</template>
        </el-table-column>
        <el-table-column prop="recipientId" label="接收人" width="140" />
        <el-table-column label="失败状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failCode" label="失败码" width="150" show-overflow-tooltip />
        <el-table-column prop="failReason" label="失败原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="retryCount" label="失败次数" width="100" />
        <el-table-column prop="sentAt" label="最后失败时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getSendRecords } from '../../api/notice';
import type { NoticeChannelType, NoticeSendRecord, NoticeSendStatus } from '../../types/notice';

const failedStatuses: NoticeSendStatus[] = ['FAILED', 'RETRY_WAITING', 'FINAL_FAILED'];
const loading = ref(false);
const rawRecords = ref<NoticeSendRecord[]>([]);

const records = computed(() => rawRecords.value.filter(item => failedStatuses.includes(item.status)));

async function load() {
  loading.value = true;
  try {
    const pages = await Promise.all(failedStatuses.map(status => getSendRecords({ status, pageSize: 50 })));
    rawRecords.value = pages.flatMap(page => page.list || []);
  } finally {
    loading.value = false;
  }
}

function channelLabel(channel: NoticeChannelType) {
  const labels: Record<NoticeChannelType, string> = {
    SITE: '站内信',
    SMS: '短信',
    EMAIL: '邮件',
    WECHAT_OFFICIAL: '微信公众号',
    WECOM: '企业微信',
    DINGTALK: '钉钉',
  };
  return labels[channel] || channel;
}

function statusLabel(status: NoticeSendStatus) {
  const labels: Record<NoticeSendStatus, string> = {
    PENDING: '待发送',
    SENDING: '发送中',
    SUCCESS: '成功',
    FAILED: '失败',
    RETRY_WAITING: '等待重试',
    FINAL_FAILED: '最终失败',
    CANCELED: '已取消',
  };
  return labels[status] || status;
}

function statusTag(status: NoticeSendStatus) {
  if (status === 'FINAL_FAILED') return 'danger';
  if (status === 'RETRY_WAITING') return 'warning';
  return 'info';
}

onMounted(load);
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
