<template>
  <div class="payment-refund-orders">
    <section class="payment-refund-orders__header">
      <div>
        <h3>退款订单</h3>
        <p>查询退款申请、原支付订单、退款状态和通道退款结果。</p>
      </div>
    </section>

    <section class="payment-refund-orders__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="退款单号 / 业务退款号 / 支付单号 / 通道单号"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="退款状态">
          <el-select v-model="query.statusCode" clearable placeholder="全部状态" @change="applyFilters">
            <el-option
              v-for="status in statusOptions"
              :key="status.statusCode"
              :label="status.statusName"
              :value="status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button link type="primary" :icon="Refresh" @click="loadRows">重新加载</el-button>
      </template>
    </el-alert>

    <el-table
      :data="rows"
      v-loading="loading"
      row-key="id"
      stripe
      highlight-current-row
      class="payment-refund-orders__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column label="退款信息" min-width="280">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__primary">{{ valueText(row.refundOrderNo) }}</div>
            <div class="payment-table-stack__line">
              <span>业务退款</span>
              <strong>{{ valueText(row.bizRefundNo) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="原支付订单" min-width="280">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__primary">{{ valueText(row.payOrderNo) }}</div>
            <div class="payment-table-stack__line">
              <span>业务订单</span>
              <strong>{{ valueText(row.bizOrderNo) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="支付通道" min-width="260">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__primary">{{ methodText(row) }}</div>
            <div class="payment-table-stack__line">
              <span>实际通道</span>
              <strong>{{ channelText(row) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>退款单号</span>
              <strong>{{ valueText(row.channelRefundNo) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="金额/状态" width="150">
        <template #default="{ row }">
          <div class="payment-money-status">
            <strong>{{ formatMoney(row.refundAmount) }}</strong>
            <span>元</span>
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusName || normalizedStatus(row.status) || '-' }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="时间信息" min-width="190">
        <template #default="{ row }">
          <div class="payment-table-stack payment-table-stack--compact">
            <div class="payment-table-stack__line">
              <span>退款成功</span>
              <strong>{{ valueText(row.refundTime) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>更新</span>
              <strong>{{ valueText(row.updateTime) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="192" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button
              v-if="canQueryRefund(row)"
              link
              type="primary"
              :icon="Refresh"
              :loading="queryingId === row.id"
              @click="queryRefund(row)"
            >
              主动查退款
            </el-button>
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-refund-orders__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="退款订单详情"
      size="780px"
      destroy-on-close
      append-to-body
      class="payment-refund-orders__drawer"
    >
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="退款申请" :column="2" border>
          <el-descriptions-item label="退款订单号">{{ valueText(detail.refundOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="退款状态">
            <el-tag :type="statusTagType(detail.status)" effect="light">{{ detail.statusName || normalizedStatus(detail.status) || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="业务退款号">{{ valueText(detail.bizRefundNo) }}</el-descriptions-item>
          <el-descriptions-item label="退款金额（元）">{{ formatMoney(detail.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ valueText(detail.reason) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ valueText(detail.currency) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="原支付订单" :column="2" border class="payment-refund-orders__detail-block">
          <el-descriptions-item label="支付订单号">{{ valueText(detail.payOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="业务订单号">{{ valueText(detail.bizOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="支付标题">{{ valueText(detail.title) }}</el-descriptions-item>
          <el-descriptions-item label="AppId">{{ valueText(detail.appId) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ methodText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="实际通道">{{ channelText(detail) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="通道结果" :column="2" border class="payment-refund-orders__detail-block">
          <el-descriptions-item label="通道商户号">{{ valueText(detail.channelMerchantNo) }}</el-descriptions-item>
          <el-descriptions-item label="通道交易号">{{ valueText(detail.channelTradeNo) }}</el-descriptions-item>
          <el-descriptions-item label="通道退款单号">{{ valueText(detail.channelRefundNo) }}</el-descriptions-item>
          <el-descriptions-item label="交易流水号">{{ valueText(detail.flowNo) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-refund-orders__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="退款成功时间">{{ valueText(detail.refundTime) }}</el-descriptions-item>
        </el-descriptions>

        <section class="payment-refund-orders__flow payment-refund-orders__detail-block">
          <h4>状态流转</h4>
          <el-timeline v-if="detail.statusFlows?.length">
            <el-timeline-item
              v-for="flow in detail.statusFlows"
              :key="`${flow.statusCode}-${flow.happenTime || flow.source}`"
              :timestamp="valueText(flow.happenTime)"
              :type="timelineType(flow.statusCode)"
            >
              <div class="payment-refund-orders__flow-title">{{ flow.statusName || flow.statusCode || '-' }}</div>
              <div class="payment-refund-orders__flow-meta">{{ valueText(flow.source) }}</div>
              <div class="payment-refund-orders__flow-remark">{{ valueText(flow.remark) }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态流转记录" />
        </section>
      </template>
      <el-empty v-else description="未查询到退款订单详情" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { Refresh, Search, Tickets } from '@element-plus/icons-vue';
import {
  paymentRefundOrderApi,
  type PaymentPageQuery,
  type PaymentRefundOrder,
  type PaymentRefundOrderStatus,
} from '../../api/payment';

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentRefundOrder[]>([]);
const statusOptions = ref<PaymentRefundOrderStatus[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentRefundOrder>();
const errorMessage = ref('');
const queryingId = ref('');

const emptyDescription = computed(() => {
  if (errorMessage.value) return '退款订单加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的退款订单' : '暂无退款订单';
});

onMounted(async () => {
  await Promise.all([loadStatuses(), loadRows()]);
});

async function loadStatuses() {
  statusOptions.value = await paymentRefundOrderApi.statuses();
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentRefundOrderApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    rows.value = [];
    total.value = 0;
    errorMessage.value = error instanceof Error ? error.message : '退款订单加载失败';
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  query.statusCode = '';
  void loadRows();
}

function applyFilters() {
  query.pageNum = 1;
  void loadRows();
}

async function openDetail(row: PaymentRefundOrder) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentRefundOrderApi.detail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

function canQueryRefund(row: PaymentRefundOrder) {
  return normalizedStatus(row.status) === 'REFUNDING';
}

async function queryRefund(row: PaymentRefundOrder) {
  if (!row.id || queryingId.value) return;
  queryingId.value = String(row.id);
  try {
    const result = await paymentRefundOrderApi.queryChannel({ id: row.id });
    const index = rows.value.findIndex(item => item.id === row.id);
    if (index >= 0) {
      rows.value[index] = result;
    }
    if (detail.value?.id === row.id) {
      detail.value = result;
    }
    await loadRows();
  } finally {
    queryingId.value = '';
  }
}

function normalizedStatus(status?: string) {
  return status === 'PROCESSING' ? 'REFUNDING' : status;
}

function statusTagType(status?: string) {
  const normalized = normalizedStatus(status);
  if (normalized === 'SUCCESS') return 'success';
  if (normalized === 'REFUNDING') return 'warning';
  if (normalized === 'CREATED' || normalized === 'CLOSED') return 'info';
  if (normalized === 'FAILED') return 'danger';
  return '';
}

function timelineType(status?: string) {
  const normalized = normalizedStatus(status);
  if (normalized === 'SUCCESS') return 'success';
  if (normalized === 'REFUNDING') return 'warning';
  if (normalized === 'FAILED') return 'danger';
  return 'info';
}

function methodText(row: PaymentRefundOrder) {
  return row.methodName || row.methodCode || '-';
}

function channelText(row: PaymentRefundOrder) {
  return row.channelName || row.channelCode || '-';
}

function valueText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function formatMoney(value?: number | string | null) {
  if (value === undefined || value === null || value === '') return '-';
  return `￥${(Number(value) / 100).toFixed(2)}`;
}
</script>

<style scoped>
.payment-refund-orders__detail-block {
  margin-top: 18px;
}

.payment-refund-orders__flow h4 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.payment-refund-orders__flow-title {
  font-weight: 600;
}

.payment-refund-orders__flow-meta,
.payment-refund-orders__flow-remark {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.payment-table-stack {
  display: grid;
  gap: 5px;
  min-width: 0;
  line-height: 1.45;
}

.payment-table-stack--compact {
  gap: 3px;
}

.payment-table-stack__primary {
  color: var(--el-text-color-primary);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-table-stack__line {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 8px;
  align-items: baseline;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.payment-table-stack__line strong {
  color: var(--el-text-color-regular);
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-money-status {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.payment-money-status strong {
  color: var(--el-text-color-primary);
  font-weight: 650;
}

.payment-money-status span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
