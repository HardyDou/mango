<template>
  <div class="payment-refund-approvals">
    <section class="payment-refund-approvals__header">
      <div>
        <h3>退款审批</h3>
        <p>审核后台发起的退款申请，审批通过后进入统一退款流程。</p>
      </div>
    </section>

    <section class="payment-refund-approvals__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="审批单号 / 支付单号 / 业务退款号 / 退款单号"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="审批状态">
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

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false">
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
      class="payment-refund-approvals__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column label="审批信息" min-width="280">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__primary">{{ valueText(row.approvalNo) }}</div>
            <div class="payment-table-stack__line">
              <span>业务退款</span>
              <strong>{{ valueText(row.bizRefundNo) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="关联订单" min-width="300">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__primary">{{ valueText(row.payOrderNo) }}</div>
            <div class="payment-table-stack__line">
              <span>业务订单</span>
              <strong>{{ valueText(row.bizOrderNo) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>退款订单</span>
              <strong>{{ valueText(row.refundOrderNo) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="金额/状态" width="150">
        <template #default="{ row }">
          <div class="payment-money-status">
            <strong>{{ formatMoney(row.refundAmount) }}</strong>
            <span>元</span>
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusName || row.status || '-' }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申请/审核" min-width="260">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__line">
              <span>申请人</span>
              <strong>{{ valueText(row.applicantName) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>申请时间</span>
              <strong>{{ valueText(row.applyTime) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>审核人</span>
              <strong>{{ valueText(row.reviewerName) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="审批进度" min-width="240">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__line">
              <span>工作流</span>
              <strong>{{ valueText(row.workflowApplyStatusName || row.workflowApplyStatus) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>当前节点</span>
              <strong>{{ valueText(row.workflowCurrentTaskNames) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>处理人</span>
              <strong>{{ valueText(row.workflowCurrentAssigneeNames) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="right" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions payment-table__actions--right">
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-refund-approvals__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-drawer v-model="detailVisible" title="退款审批详情" size="760px" destroy-on-close append-to-body>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detail">
        <el-descriptions title="审批申请" :column="2" border>
          <el-descriptions-item label="审批单号">{{ valueText(detail.approvalNo) }}</el-descriptions-item>
          <el-descriptions-item label="审批状态">
            <el-tag :type="statusTagType(detail.status)" effect="light">{{ detail.statusName || detail.status || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="业务退款号">{{ valueText(detail.bizRefundNo) }}</el-descriptions-item>
          <el-descriptions-item label="退款金额（元）">{{ formatMoney(detail.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ valueText(detail.reason) }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ valueText(detail.remark) }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="工作流进度" :column="2" border class="payment-refund-approvals__detail-block">
          <el-descriptions-item label="流程实例">{{ valueText(detail.workflowProcessInstanceId) }}</el-descriptions-item>
          <el-descriptions-item label="流程定义">{{ valueText(detail.workflowProcessDefinitionKey) }}</el-descriptions-item>
          <el-descriptions-item label="工作流状态">{{ valueText(detail.workflowApplyStatusName || detail.workflowApplyStatus) }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ valueText(detail.workflowCurrentTaskNames) }}</el-descriptions-item>
          <el-descriptions-item label="当前处理人">{{ valueText(detail.workflowCurrentAssigneeNames) }}</el-descriptions-item>
          <el-descriptions-item label="同步时间">{{ valueText(detail.workflowSyncedAt) }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="关联订单" :column="2" border class="payment-refund-approvals__detail-block">
          <el-descriptions-item label="支付订单号">{{ valueText(detail.payOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="业务订单号">{{ valueText(detail.bizOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="退款订单号">{{ valueText(detail.refundOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="AppId">{{ valueText(detail.appId) }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="操作记录" :column="2" border class="payment-refund-approvals__detail-block">
          <el-descriptions-item label="申请人">{{ valueText(detail.applicantName) }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ valueText(detail.applyTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ valueText(detail.reviewerName) }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ valueText(detail.reviewTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核说明">{{ valueText(detail.reviewReason) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到退款审批详情" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { Refresh, Search, Tickets } from '@element-plus/icons-vue';
import {
  paymentRefundApprovalApi,
  type PaymentPageQuery,
  type PaymentRefundApproval,
  type PaymentRefundApprovalStatus,
} from '../../api/payment';

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentRefundApproval[]>([]);
const statusOptions = ref<PaymentRefundApprovalStatus[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentRefundApproval>();
const errorMessage = ref('');
type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

const emptyDescription = computed(() => {
  if (errorMessage.value) return '退款审批加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的退款审批' : '暂无退款审批';
});

onMounted(async () => {
  await Promise.all([loadStatuses(), loadRows()]);
});

async function loadStatuses() {
  statusOptions.value = await paymentRefundApprovalApi.statuses();
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentRefundApprovalApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    rows.value = [];
    total.value = 0;
    errorMessage.value = error instanceof Error ? error.message : '退款审批加载失败';
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

async function openDetail(row: PaymentRefundApproval) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentRefundApprovalApi.detail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

function statusTagType(status?: string): TagType {
  if (status === 'APPROVED') return 'success';
  if (status === 'PENDING' || status === 'IN_APPROVAL') return 'warning';
  if (status === 'REJECTED') return 'danger';
  return 'info';
}

function valueText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function formatMoney(value?: number | string | null) {
  if (value === undefined || value === null || value === '') return '-';
  return (Number(value) / 100).toFixed(2);
}
</script>

<style scoped>
.payment-refund-approvals__detail-block {
  margin-top: 18px;
}

.payment-table-stack {
  display: grid;
  gap: 5px;
}

.payment-table-stack__primary {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.payment-table-stack__line {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.payment-table-stack__line strong {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-money-status {
  display: grid;
  gap: 4px;
  align-items: start;
}

.payment-money-status strong {
  color: var(--el-color-danger);
  font-size: 16px;
}

.payment-money-status span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.payment-table__actions {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
}

.payment-table__actions--right {
  justify-content: flex-end;
}
</style>
