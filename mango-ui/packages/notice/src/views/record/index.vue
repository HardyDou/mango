<template>
  <div class="notice-record-page">
    <el-card shadow="never" class="notice-record-page__search">
      <el-form :model="searchForm" label-width="96px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="业务域">
              <el-select v-model="searchForm.bizGroup" clearable filterable placeholder="请选择业务域" :loading="domainLoading">
                <el-option v-for="item in domainOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="消息名称">
              <el-input v-model="searchForm.messageName" clearable placeholder="请输入消息名称" @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="渠道">
              <el-select v-model="searchForm.channelType" clearable placeholder="请选择渠道">
                <el-option v-for="item in channelOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="接收人">
              <el-select
                v-model="searchForm.recipientKeyword"
                :loading="recipientLoading"
                clearable
                filterable
                remote
                :remote-method="searchRecipients"
                placeholder="请选择接收人"
                @visible-change="handleRecipientVisible"
              >
                <el-option
                  v-for="item in recipientOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.keyword"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" clearable placeholder="请选择状态">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="10" :lg="8">
            <el-form-item label="发送时间">
              <el-config-provider :locale="zhCn">
                <el-date-picker
                  v-model="searchForm.sentTimeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                />
              </el-config-provider>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6" :lg="4">
            <el-form-item label-width="0">
              <el-space>
                <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
                <el-button @click="resetSearch">重置</el-button>
              </el-space>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="notice-record-page__header">
          <span>发送记录</span>
          <el-button type="primary" plain :loading="loading" @click="loadRecords">刷新</el-button>
        </div>
      </template>

      <el-table :data="records" border stripe v-loading="loading">
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
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="sendStatusTag(row.status)">{{ sendStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发送日期" width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sentAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.failReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="发送记录详情" width="860px">
      <div v-if="currentRecord" class="notice-record-page__detail">
        <section>
          <h3>基础信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="业务域">{{ domainText(currentRecord.bizGroup) }}</el-descriptions-item>
            <el-descriptions-item label="消息名称">{{ recordMessageName(currentRecord) }}</el-descriptions-item>
            <el-descriptions-item label="消息Key">{{ currentRecord.bizType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="业务对象">{{ currentRecord.bizId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="标题" :span="2">{{ currentRecord.renderedTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="内容" :span="2">
              <pre class="notice-record-page__pre">{{ currentRecord.renderedContent || '-' }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </section>
        <section>
          <h3>发送情况</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="渠道">{{ channelTypeText(currentRecord.channelType) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ sendStatusText(currentRecord.status) }}</el-descriptions-item>
            <el-descriptions-item label="接收人">{{ currentRecord.recipientName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="接收账号">{{ recipientAccountText(currentRecord) }}</el-descriptions-item>
            <el-descriptions-item label="发送时间">{{ currentRecord.sentAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="重试次数">{{ currentRecord.retryCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="失败编码">{{ currentRecord.failCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="失败原因">{{ currentRecord.failReason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>
        <section>
          <h3>通道与模板</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="通道名称">{{ currentRecord.channelConfigName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="通道配置ID">{{ currentRecord.channelConfigId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模板名称">{{ currentRecord.businessChannelTemplateName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模板ID">{{ currentRecord.businessChannelTemplateId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模板版本">{{ currentRecord.templateVersion ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="请求流水号">{{ currentRecord.requestId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="三方消息ID">{{ currentRecord.providerMessageId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="任务ID">{{ currentRecord.taskId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发送记录ID">{{ currentRecord.id || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>
        <section>
          <h3>报文信息</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="请求明细">
              <pre class="notice-record-page__code">{{ prettySnapshot(currentRecord.requestSnapshot) }}</pre>
            </el-descriptions-item>
            <el-descriptions-item label="响应明细">
              <pre class="notice-record-page__code">{{ prettySnapshot(currentRecord.responseSnapshot) }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </section>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import { getIdentityUsers, getSendRecords, type NoticeIdentityUser } from '../../api/notice';
import type { NoticeChannelType, NoticeSendRecord, NoticeSendStatus } from '../../types/notice';
import { useNoticeDomains } from '../../components/useNoticeDomains';

interface SearchForm {
  bizGroup: string;
  messageName: string;
  channelType?: NoticeChannelType;
  recipientKeyword: string;
  status?: NoticeSendStatus;
  sentTimeRange: string[];
}

const loading = ref(false);
const recipientLoading = ref(false);
const records = ref<NoticeSendRecord[]>([]);
const recipientOptions = ref<Array<{ value: string; label: string; keyword: string }>>([]);
const detailVisible = ref(false);
const currentRecord = ref<NoticeSendRecord>();
const searchForm = reactive<SearchForm>({
  bizGroup: '',
  messageName: '',
  channelType: undefined,
  recipientKeyword: '',
  status: undefined,
  sentTimeRange: [],
});

const channelOptions: Array<{ label: string; value: NoticeChannelType }> = [
  { label: '系统消息', value: 'SITE' },
  { label: '短信', value: 'SMS' },
  { label: '邮件', value: 'EMAIL' },
  { label: '公众号', value: 'WECHAT_OFFICIAL' },
  { label: '企业微信', value: 'WECOM' },
  { label: '钉钉', value: 'DINGTALK' },
];

const statusOptions: Array<{ label: string; value: NoticeSendStatus }> = [
  { label: '待发送', value: 'PENDING' },
  { label: '发送中', value: 'SENDING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '等待重试', value: 'RETRY_WAITING' },
  { label: '最终失败', value: 'FINAL_FAILED' },
  { label: '人工成功', value: 'MANUAL_SUCCESS' },
  { label: '已忽略', value: 'IGNORED' },
  { label: '已取消', value: 'CANCELED' },
];

const { domainLoading, domainOptions, domainText, loadDomains } = useNoticeDomains();

async function loadRecords() {
  loading.value = true;
  try {
    const result = await getSendRecords(searchParams());
    records.value = result.list || [];
  } finally {
    loading.value = false;
  }
}

async function searchRecipients(keyword: string) {
  recipientLoading.value = true;
  try {
    const result = await getIdentityUsers(keyword.trim(), { pageNum: 1, pageSize: 20, status: 1 });
    recipientOptions.value = (result.list || []).map(toRecipientOption).filter((item): item is {
      value: string;
      label: string;
      keyword: string;
    } => Boolean(item));
  } finally {
    recipientLoading.value = false;
  }
}

function handleRecipientVisible(visible: boolean) {
  if (visible && recipientOptions.value.length === 0) {
    searchRecipients('');
  }
}

function handleSearch() {
  loadRecords();
}

function resetSearch() {
  searchForm.bizGroup = '';
  searchForm.messageName = '';
  searchForm.channelType = undefined;
  searchForm.recipientKeyword = '';
  searchForm.status = undefined;
  searchForm.sentTimeRange = [];
  loadRecords();
}

function openDetail(row: NoticeSendRecord) {
  currentRecord.value = row;
  detailVisible.value = true;
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

function searchParams() {
  return {
    bizGroup: searchForm.bizGroup || undefined,
    messageName: searchForm.messageName || undefined,
    channelType: searchForm.channelType || undefined,
    recipientKeyword: searchForm.recipientKeyword || undefined,
    status: searchForm.status || undefined,
    startTime: searchForm.sentTimeRange?.[0],
    endTime: searchForm.sentTimeRange?.[1],
  };
}

function channelTypeText(type: NoticeChannelType) {
  return ({
    SITE: '系统消息',
    SMS: '短信',
    EMAIL: '邮件',
    WECHAT_OFFICIAL: '公众号',
    WECOM: '企业微信',
    DINGTALK: '钉钉',
  } as Record<NoticeChannelType, string>)[type] || type;
}

function sendStatusText(status: NoticeSendStatus) {
  return ({
    PENDING: '待发送',
    SENDING: '发送中',
    SUCCESS: '成功',
    FAILED: '失败',
    RETRY_WAITING: '等待重试',
    FINAL_FAILED: '最终失败',
    MANUAL_SUCCESS: '人工成功',
    IGNORED: '已忽略',
    CANCELED: '已取消',
  } as Record<NoticeSendStatus, string>)[status] || status;
}

function sendStatusTag(status: NoticeSendStatus) {
  return ({
    PENDING: 'info',
    SENDING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
    RETRY_WAITING: 'warning',
    FINAL_FAILED: 'danger',
    MANUAL_SUCCESS: 'success',
    IGNORED: 'info',
    CANCELED: 'info',
  } as Record<NoticeSendStatus, 'success' | 'warning' | 'danger' | 'info'>)[status] || 'info';
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

function prettySnapshot(snapshot?: string) {
  if (!snapshot) return '-';
  try {
    return JSON.stringify(JSON.parse(snapshot), null, 2);
  } catch {
    return snapshot;
  }
}

function stringValue(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : '';
}

function toRecipientOption(item: NoticeIdentityUser) {
  if (item.userId === undefined) {
    return undefined;
  }
  const name = item.nickname || item.username || String(item.userId);
  const account = [item.phone, item.email].map(stringValue).filter(Boolean).join(' / ');
  return {
    value: String(item.userId),
    label: account ? `${name}（${account}）` : name,
    keyword: name,
  };
}

onMounted(() => {
  loadDomains();
  loadRecords();
});
</script>

<style scoped>
.notice-record-page {
  padding: 0;
}

.notice-record-page__search {
  margin-bottom: 12px;
}

.notice-record-page__search :deep(.el-form-item) {
  margin-bottom: 16px;
}

.notice-record-page__search :deep(.el-select),
.notice-record-page__search :deep(.el-date-editor),
.notice-record-page__search :deep(.el-config-provider) {
  width: 100%;
}

.notice-record-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notice-record-page__detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-record-page__detail h3 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-record-page__pre {
  max-height: 260px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--el-font-family);
}

.notice-record-page__code {
  max-height: 320px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  white-space: pre;
  word-break: normal;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}
</style>
