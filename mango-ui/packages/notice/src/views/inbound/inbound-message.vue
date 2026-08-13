<template>
  <MangoListPage class="notice-inbound-page" data-page="notice.inbound">
    <template #search>
      <MangoSearchPanel :model="query" :columns="4" @search="search" @reset="resetSearch">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="主题、发送方或消息ID" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="query.channelType" clearable placeholder="全部渠道">
            <el-option label="邮件" value="EMAIL" />
            <el-option label="企业微信" value="WECOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收时间">
          <el-date-picker
            v-model="query.receivedTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #view-actions>
        <el-button type="primary" plain :loading="loading" @click="loadData">刷新</el-button>
      </template>
      <el-table v-loading="loading" :data="rows" row-key="id" border stripe data-surface="notice.inbound.table">
        <el-table-column label="渠道" width="110">
          <template #default="{ row }">{{ channelLabel(row.channelType) }}</template>
        </el-table-column>
        <el-table-column label="主题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.subject || '(无主题)' }}</template>
        </el-table-column>
        <el-table-column prop="fromAddress" label="发送方" min-width="200" show-overflow-tooltip />
        <el-table-column label="提供方" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ providerLabel(row.channelType, row.providerCode) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receivedAt" label="接收时间" width="180" />
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #pagination>
        <Pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>

    <MangoDialog v-model="detailVisible" title="接收消息详情" width="900px" destroy-on-close>
      <div v-loading="detailLoading" class="notice-inbound-page__detail">
        <template v-if="current">
          <MangoPageSection title="消息内容">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="渠道">{{ channelLabel(current.channelType) }}</el-descriptions-item>
              <el-descriptions-item label="提供方">{{
                providerLabel(current.channelType, current.providerCode)
              }}</el-descriptions-item>
              <el-descriptions-item label="主题" :span="2">{{ current.subject || '(无主题)' }}</el-descriptions-item>
              <el-descriptions-item label="发送方">{{ current.fromAddress || '-' }}</el-descriptions-item>
              <el-descriptions-item label="接收方">{{ addressText(current.toAddressesJson) }}</el-descriptions-item>
              <el-descriptions-item label="正文" :span="2">
                <RichTextViewer :content="bodyContent(current)" class="notice-inbound-page__body" />
              </el-descriptions-item>
            </el-descriptions>
          </MangoPageSection>

          <MangoPageSection title="附件">
            <el-table :data="current.attachments || []" border empty-text="无附件">
              <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
              <el-table-column label="大小" width="120">
                <template #default="{ row }">{{ fileSizeText(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">{{ attachmentStatusLabel(row.status) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <el-button
                    circle
                    :icon="View"
                    :disabled="!row.fileId"
                    title="预览附件"
                    aria-label="预览附件"
                    @click="openAttachmentPreview(row)"
                  />
                  <el-button
                    circle
                    :icon="Download"
                    :disabled="!row.fileId"
                    title="下载附件"
                    aria-label="下载附件"
                    @click="downloadAttachment(row)"
                  />
                </template>
              </el-table-column>
            </el-table>
          </MangoPageSection>

          <MangoPageSection title="处理状态">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="状态">{{ statusLabel(current.status) }}</el-descriptions-item>
              <el-descriptions-item label="尝试次数">{{ current.attemptCount }}</el-descriptions-item>
              <el-descriptions-item label="接收时间">{{ current.receivedAt }}</el-descriptions-item>
              <el-descriptions-item label="处理时间">{{ current.processedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="广播事件ID" :span="2">{{ current.eventId }}</el-descriptions-item>
              <el-descriptions-item label="来源消息ID" :span="2">{{ current.messageId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="失败原因" :span="2">{{ current.failureReason || '-' }}</el-descriptions-item>
            </el-descriptions>
          </MangoPageSection>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </MangoDialog>

    <MangoDialog
      v-model="previewVisible"
      :title="previewAttachment?.fileName || '附件预览'"
      width="900px"
      destroy-on-close
    >
      <div v-if="previewAttachment?.fileId" class="notice-inbound-page__file-preview">
        <iframe
          :src="filePreviewUrl(previewAttachment.fileId)"
          :title="previewAttachment.fileName || '附件预览'"
          class="notice-inbound-page__file-preview-frame"
        />
      </div>
      <el-empty v-else description="暂无可预览文件" />
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </MangoDialog>
  </MangoListPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Download, View } from '@element-plus/icons-vue';
import {
  downloadUploadedFile,
  MangoDialog,
  MangoListPage,
  MangoListPanel,
  MangoPageSection,
  MangoSearchPanel,
  Pagination,
  RichTextViewer,
} from '@mango/common';
import { getInboundMessage, getInboundMessages } from '../../api/notice';
import type {
  NoticeChannelType,
  NoticeInboundAttachment,
  NoticeInboundAttachmentStatus,
  NoticeInboundMessage,
  NoticeInboundMessageStatus,
} from '../../types/notice';

interface InboundQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  channelType?: NoticeChannelType;
  status?: NoticeInboundMessageStatus;
  receivedTimeRange?: string[];
}

const loading = ref(false);
const detailLoading = ref(false);
const detailVisible = ref(false);
const previewVisible = ref(false);
const rows = ref<NoticeInboundMessage[]>([]);
const total = ref(0);
const current = ref<NoticeInboundMessage>();
const previewAttachment = ref<NoticeInboundAttachment>();
const query = reactive<InboundQuery>({ pageNum: 1, pageSize: 10 });

const statusOptions: Array<{ label: string; value: NoticeInboundMessageStatus }> = [
  { label: '已接收', value: 'RECEIVED' },
  { label: '附件处理中', value: 'ATTACHMENT_PROCESSING' },
  { label: '待广播', value: 'READY_TO_BROADCAST' },
  { label: '已广播', value: 'BROADCASTED' },
  { label: '可重试失败', value: 'RETRYABLE_FAILED' },
  { label: '死信', value: 'DEAD_LETTER' },
];

async function loadData() {
  loading.value = true;
  try {
    const result = await getInboundMessages(requestParams());
    rows.value = result.list || [];
    total.value = Number(result.total || 0);
  } finally {
    loading.value = false;
  }
}

function requestParams() {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    keyword: query.keyword || undefined,
    channelType: query.channelType,
    status: query.status,
    startTime: query.receivedTimeRange?.[0],
    endTime: query.receivedTimeRange?.[1],
  };
}

function search() {
  query.pageNum = 1;
  void loadData();
}

function resetSearch() {
  Object.assign(query, {
    pageNum: 1,
    keyword: undefined,
    channelType: undefined,
    status: undefined,
    receivedTimeRange: undefined,
  });
  void loadData();
}

async function openDetail(row: NoticeInboundMessage) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    current.value = await getInboundMessage(row.id);
  } finally {
    detailLoading.value = false;
  }
}

async function downloadAttachment(attachment: NoticeInboundAttachment) {
  if (!attachment.fileId) return;
  const response = await downloadUploadedFile(attachment.fileId);
  const blob = response.data instanceof Blob ? response.data : new Blob([response.data]);
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = attachment.fileName;
  link.click();
  URL.revokeObjectURL(objectUrl);
}

function openAttachmentPreview(attachment: NoticeInboundAttachment) {
  if (!attachment.fileId) return;
  previewAttachment.value = attachment;
  previewVisible.value = true;
}

function filePreviewUrl(fileId: string) {
  return `/api/file/files/preview-content?id=${encodeURIComponent(fileId)}`;
}

function bodyContent(message: NoticeInboundMessage) {
  if (message.bodyHtml) return message.bodyHtml;
  if (!message.bodyText) return '<p>-</p>';
  return escapeHtml(message.bodyText).replace(/\r?\n/g, '<br>');
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function channelLabel(channelType: NoticeChannelType) {
  return channelType === 'EMAIL' ? '邮件' : channelType === 'WECOM' ? '企业微信' : channelType;
}

function providerLabel(channelType: NoticeChannelType, providerCode?: string) {
  if (!providerCode) return '-';
  const labels: Record<string, string> = {
    INTERNAL: '系统内置',
    ALIYUN_SMS: '阿里云短信',
    TENCENT_SMS: '腾讯云短信',
    CUSTOM_SMTP: '自建 SMTP',
    SMTP: '自建 SMTP',
    ALIYUN_DM: '阿里云邮件推送',
    STANDARD_MAIL: '标准邮件',
    WECHAT_OFFICIAL: '微信公众号',
    WECOM: '企业微信',
    DINGTALK: '钉钉',
  };
  return labels[providerCode] || `${channelLabel(channelType)}（${providerCode}）`;
}

function statusLabel(status: NoticeInboundMessageStatus) {
  return statusOptions.find((item) => item.value === status)?.label || status;
}

function statusTag(status: NoticeInboundMessageStatus) {
  if (status === 'BROADCASTED') return 'success';
  if (status === 'DEAD_LETTER') return 'danger';
  if (status === 'RETRYABLE_FAILED') return 'warning';
  return 'info';
}

function attachmentStatusLabel(status: NoticeInboundAttachmentStatus) {
  return {
    PENDING: '待处理',
    PROCESSING: '处理中',
    SAVED: '已保存',
    RETRYABLE_FAILED: '可重试失败',
    DEAD_LETTER: '死信',
  }[status];
}

function addressText(value?: string) {
  if (!value) return '-';
  try {
    const addresses: unknown = JSON.parse(value);
    return Array.isArray(addresses) ? addresses.join(', ') : value;
  } catch {
    return value;
  }
}

function fileSizeText(size: number) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

onMounted(loadData);
</script>

<style scoped>
.notice-inbound-page__detail {
  min-height: 240px;
}

.notice-inbound-page__body {
  max-height: 320px;
  margin: 0;
  overflow: auto;
  line-height: 1.6;
}

.notice-inbound-page__body :deep(.rich-text-viewer__content) {
  overflow-wrap: anywhere;
}
</style>
