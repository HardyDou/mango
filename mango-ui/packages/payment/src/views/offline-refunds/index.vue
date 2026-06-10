<template>
  <div class="payment-offline-refunds">
    <section class="payment-offline-refunds__header">
      <div>
        <h3>线下退款订单</h3>
        <p>查询线下收款通道独立退款订单、退款账户、退款凭证和退款状态。</p>
      </div>
    </section>

    <section class="payment-offline-refunds__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="退款单号 / 收款单号 / 支付单号 / 业务单号 / 退款账户"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="退款状态">
          <el-select v-model="query.statusCode" clearable placeholder="全部状态" @change="applyFilters">
            <el-option
              v-for="status in statusOptions"
              :key="statusCode(status)"
              :label="statusName(status)"
              :value="statusCode(status)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false">
      <template #default>
        <el-button link type="primary" :icon="Refresh" @click="loadRows">重新加载</el-button>
      </template>
    </el-alert>

    <el-table :data="rows" v-loading="loading" row-key="id" stripe highlight-current-row class="payment-offline-refunds__table">
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="offlineRefundNo" label="线下退款单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="refundOrderNo" label="统一退款单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="offlineCollectionNo" label="线下收款单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="payOrderNo" label="支付订单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="bizOrderNo" label="业务订单号" min-width="170" show-overflow-tooltip />
      <el-table-column label="退款金额（元）" width="120" align="right">
        <template #default="{ row }">
          <span>{{ formatMoney(row.refundAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="refundAccountName" label="退款户名" min-width="150" show-overflow-tooltip />
      <el-table-column prop="refundBankName" label="退款开户行" min-width="150" show-overflow-tooltip />
      <el-table-column label="退款状态" width="120">
        <template #default="{ row }">
          <el-tag type="success" effect="light">{{ row.refundStatusName || row.refundStatus || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="凭证" width="88" align="right">
        <template #default="{ row }">
          <span>{{ row.refundVoucherCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="refundedTime" label="退款时间" width="170" show-overflow-tooltip />
      <el-table-column prop="operatorName" label="操作人" width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="104" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-offline-refunds__pagination">
      <Pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadRows" />
    </div>

    <el-drawer v-model="detailVisible" title="线下退款详情" size="760px" destroy-on-close append-to-body>
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="退款信息" :column="2" border>
          <el-descriptions-item label="线下退款单号">{{ valueText(detail.offlineRefundNo) }}</el-descriptions-item>
          <el-descriptions-item label="退款状态">
            <el-tag type="success" effect="light">{{ detail.refundStatusName || detail.refundStatus || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="统一退款单号">{{ valueText(detail.refundOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="退款金额（元）">{{ formatMoney(detail.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ valueText(detail.currency) }}</el-descriptions-item>
          <el-descriptions-item label="退款户名">{{ valueText(detail.refundAccountName) }}</el-descriptions-item>
          <el-descriptions-item label="退款账号">{{ valueText(detail.refundAccountNoMask) }}</el-descriptions-item>
          <el-descriptions-item label="退款开户行">{{ valueText(detail.refundBankName) }}</el-descriptions-item>
          <el-descriptions-item label="退款凭证">{{ valueText(detail.refundVoucherFileIds) }}</el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ valueText(detail.reason) }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ valueText(detail.remark) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="关联订单" :column="2" border class="payment-offline-refunds__detail-block">
          <el-descriptions-item label="线下收款单号">{{ valueText(detail.offlineCollectionNo) }}</el-descriptions-item>
          <el-descriptions-item label="支付订单号">{{ valueText(detail.payOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="业务订单号">{{ valueText(detail.bizOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="支付标题">{{ valueText(detail.title) }}</el-descriptions-item>
          <el-descriptions-item label="AppId">{{ valueText(detail.appId) }}</el-descriptions-item>
          <el-descriptions-item label="支付通道">{{ valueText(detail.channelName || detail.channelCode) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="操作信息" :column="2" border class="payment-offline-refunds__detail-block">
          <el-descriptions-item label="退款时间">{{ valueText(detail.refundedTime) }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ valueText(detail.operatorName || detail.operatorId) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到线下退款详情" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Refresh, Search, Tickets } from '@element-plus/icons-vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import {
  paymentOfflineRefundApi,
  type PaymentOfflineRefund,
  type PaymentOfflineRefundStatus,
  type PaymentPageQuery,
} from '../../api/payment';

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentOfflineRefund[]>([]);
const statusOptions = ref<PaymentOfflineRefundStatus[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentOfflineRefund>();
const errorMessage = ref('');

const emptyDescription = computed(() => {
  if (errorMessage.value) return '线下退款加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的线下退款' : '暂无线下退款记录';
});

onMounted(async () => {
  await Promise.all([loadStatuses(), loadRows()]);
});

async function loadStatuses() {
  statusOptions.value = await paymentOfflineRefundApi.statuses();
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentOfflineRefundApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    rows.value = [];
    total.value = 0;
    errorMessage.value = error instanceof Error ? error.message : '线下退款加载失败';
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

async function openDetail(row: PaymentOfflineRefund) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentOfflineRefundApi.detail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

function statusCode(status: PaymentOfflineRefundStatus) {
  return status.statusCode || status.code || '';
}

function statusName(status: PaymentOfflineRefundStatus) {
  return status.statusName || status.label || statusCode(status);
}

function formatMoney(value?: number | string) {
  if (value === undefined || value === null || value === '') return '-';
  const cents = Number(value);
  if (!Number.isFinite(cents)) return '-';
  return `¥${(cents / 100).toFixed(2)}`;
}

function valueText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}
</script>

<style scoped>
.payment-offline-refunds__detail-block {
  margin-top: 16px;
}
</style>
