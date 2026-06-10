<template>
  <div class="payment-orders">
    <section class="payment-orders__header">
      <div>
        <h3>支付订单</h3>
        <p>查询每次支付尝试、实际通道请求、通道交易号和支付状态流转。</p>
      </div>
    </section>

    <section class="payment-orders__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="支付单号 / 业务单号 / 通道单号 / 方式 / 通道 / 商户号"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="query.statusCode" clearable placeholder="全部状态" @change="applyFilters">
            <el-option
              v-for="status in statusOptions"
              :key="status.statusCode"
              :label="status.statusName"
              :value="status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="payment-orders__filter-actions">
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
      class="payment-orders__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column label="订单信息" min-width="280">
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
      <el-table-column label="支付通道" min-width="280">
        <template #default="{ row }">
          <div class="payment-table-stack">
            <div class="payment-table-stack__primary">{{ methodText(row) }}</div>
            <div class="payment-table-stack__line">
              <span>实际通道</span>
              <strong>{{ channelText(row) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>商户号</span>
              <strong>{{ valueText(row.channelMerchantNo) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>通道单号</span>
              <strong>{{ valueText(row.channelTradeNo) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="金额/状态" width="160">
        <template #default="{ row }">
          <div class="payment-money-status">
            <strong>{{ formatMoney(row.amount) }}</strong>
            <span>元</span>
            <el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusName || row.status || '-' }}</el-tag>
            <el-tag v-if="showBusinessEntryResult(row)" :type="businessEntryTagType(row)" effect="plain">
              业务入账：{{ businessEntryText(row) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="时间信息" min-width="220">
        <template #default="{ row }">
          <div class="payment-table-stack payment-table-stack--compact">
            <div class="payment-table-stack__line">
              <span>支付成功</span>
              <strong>{{ valueText(row.payTime) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>过期</span>
              <strong>{{ valueText(row.expireTime) }}</strong>
            </div>
            <div class="payment-table-stack__line">
              <span>更新</span>
              <strong>{{ valueText(row.updateTime) }}</strong>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="156" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button link type="primary" :icon="RefreshLeft" :disabled="!canRefund(row)" @click="openRefund(row)">退款</el-button>
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-orders__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="支付订单详情"
      size="780px"
      destroy-on-close
      append-to-body
      class="payment-orders__drawer"
    >
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="订单信息" :column="2" border>
          <el-descriptions-item label="支付订单号">{{ valueText(detail.payOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="statusTagType(detail.status)" effect="light">{{ detail.statusName || detail.status || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="业务订单号">{{ valueText(detail.bizOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="支付标题">{{ valueText(detail.title) }}</el-descriptions-item>
          <el-descriptions-item label="AppId">{{ valueText(detail.appId) }}</el-descriptions-item>
          <el-descriptions-item label="企业主体">{{ valueText(detail.subjectName) }}</el-descriptions-item>
          <el-descriptions-item label="收银台">{{ valueText(detail.cashierName) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ valueText(detail.currency) }}</el-descriptions-item>
          <el-descriptions-item label="支付金额（元）">{{ formatMoney(detail.amount) }}</el-descriptions-item>
          <el-descriptions-item label="业务入账结果">
            <el-tag :type="businessEntryTagType(detail)" effect="light">
              {{ businessEntryText(detail) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="支付方式与通道请求" :column="2" border class="payment-orders__detail-block">
          <el-descriptions-item label="支付方式">{{ methodText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="实际通道">{{ channelText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="通道商户号">{{ valueText(detail.channelMerchantNo) }}</el-descriptions-item>
          <el-descriptions-item label="通道交易号">{{ valueText(detail.channelTradeNo) }}</el-descriptions-item>
          <el-descriptions-item label="签约配置">{{ valueText(detail.contractName) }}</el-descriptions-item>
          <el-descriptions-item label="签约能力 ID">{{ valueText(detail.contractCapabilityId) }}</el-descriptions-item>
          <el-descriptions-item label="路由规则 ID">{{ valueText(detail.routeRuleId) }}</el-descriptions-item>
          <el-descriptions-item label="交易流水号">{{ valueText(detail.flowNo) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-orders__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付成功时间">{{ valueText(detail.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ valueText(detail.expireTime) }}</el-descriptions-item>
        </el-descriptions>

        <section class="payment-orders__flow payment-orders__detail-block">
          <h4>状态流转</h4>
          <el-timeline v-if="detail.statusFlows?.length">
            <el-timeline-item
              v-for="flow in detail.statusFlows"
              :key="`${flow.statusCode}-${flow.happenTime || flow.source}`"
              :timestamp="valueText(flow.happenTime)"
              :type="timelineType(flow.statusCode)"
            >
              <div class="payment-orders__flow-title">{{ flow.statusName || flow.statusCode || '-' }}</div>
              <div class="payment-orders__flow-meta">{{ valueText(flow.source) }}</div>
              <div class="payment-orders__flow-remark">{{ valueText(flow.remark) }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态流转记录" />
        </section>
      </template>
      <el-empty v-else description="未查询到支付订单详情" />
    </el-drawer>

    <el-dialog
      v-model="refundVisible"
      title="发起退款"
      width="560px"
      append-to-body
      destroy-on-close
      @closed="resetRefundForm"
    >
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="108px" class="payment-dialog-form">
        <section class="payment-form-section">
          <h4 class="payment-form-section__title">原支付信息</h4>
          <div class="payment-form-grid">
            <el-form-item label="支付订单号">
              <span class="payment-form-readonly">{{ refundOrder?.payOrderNo || '-' }}</span>
            </el-form-item>
            <el-form-item label="可退金额（元）">
              <span class="payment-form-readonly payment-form-readonly--strong">{{ formatMoney(refundAvailableAmount) }}</span>
            </el-form-item>
          </div>
        </section>
        <section class="payment-form-section">
          <h4 class="payment-form-section__title">退款申请</h4>
          <div class="payment-form-grid">
            <el-form-item label="业务退款单号" class="payment-form-item--wide">
              <el-input v-model="refundForm.bizRefundNo" maxlength="64" placeholder="不填由服务端生成" />
            </el-form-item>
            <el-form-item label="退款金额（元）" prop="refundAmountYuan" class="payment-form-item--wide">
              <el-input-number
                v-model="refundForm.refundAmountYuan"
                :min="0.01"
                :max="refundAvailableAmount / 100"
                :precision="2"
                :step="1"
                controls-position="right"
                class="payment-orders__money-input"
              />
            </el-form-item>
            <el-form-item label="退款原因" prop="reason" class="payment-form-item--wide">
              <el-input v-model="refundForm.reason" type="textarea" :rows="3" maxlength="512" show-word-limit />
            </el-form-item>
            <el-form-item label="备注" class="payment-form-item--wide">
              <el-input v-model="refundForm.remark" type="textarea" :rows="2" maxlength="512" show-word-limit />
            </el-form-item>
          </div>
        </section>
        <el-alert
          title="提交后会创建退款审批单，审批通过后才进入通道退款流程。"
          type="info"
          show-icon
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Refresh, RefreshLeft, Search, Tickets } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import Pagination from '@mango/common/components/Pagination/index.vue';
import {
  paymentOrderApi,
  paymentRefundApprovalApi,
  type PaymentOrder,
  type PaymentOrderStatus,
  type PaymentPageQuery,
} from '../../api/payment';

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentOrder[]>([]);
const statusOptions = ref<PaymentOrderStatus[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentOrder>();
const refundVisible = ref(false);
const refundSubmitting = ref(false);
const refundOrder = ref<PaymentOrder>();
const refundFormRef = ref<FormInstance>();
const errorMessage = ref('');
const refundForm = reactive({
  bizRefundNo: '',
  refundAmountYuan: 0.01,
  reason: '',
  remark: '',
});
const refundRules: FormRules = {
  refundAmountYuan: [{ required: true, message: '请输入退款金额', trigger: 'change' }],
  reason: [{ required: true, message: '请输入退款原因', trigger: 'blur' }],
};

const emptyDescription = computed(() => {
  if (errorMessage.value) return '支付订单加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的支付订单' : '暂无支付订单';
});
const refundAvailableAmount = computed(() => {
  const amount = Number(refundOrder.value?.amount || 0);
  const refundedAmount = Number(refundOrder.value?.refundedAmount || 0);
  return Math.max(amount - refundedAmount, 0);
});

onMounted(async () => {
  await Promise.all([loadStatuses(), loadRows()]);
});

async function loadStatuses() {
  statusOptions.value = await paymentOrderApi.statuses();
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentOrderApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    rows.value = [];
    total.value = 0;
    errorMessage.value = error instanceof Error ? error.message : '支付订单加载失败';
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

async function openDetail(row: PaymentOrder) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentOrderApi.detail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

function canRefund(row: PaymentOrder) {
  return row.status === 'SUCCESS' && row.successFlag === 1 && Number(row.amount || 0) > 0;
}

function openRefund(row: PaymentOrder) {
  if (!canRefund(row)) return;
  refundOrder.value = row;
  refundForm.bizRefundNo = '';
  refundForm.refundAmountYuan = refundAvailableAmount.value / 100;
  refundForm.reason = '';
  refundForm.remark = '';
  refundVisible.value = true;
}

function resetRefundForm() {
  refundOrder.value = undefined;
  refundForm.bizRefundNo = '';
  refundForm.refundAmountYuan = 0.01;
  refundForm.reason = '';
  refundForm.remark = '';
  refundFormRef.value?.clearValidate();
}

async function submitRefund() {
  const order = refundOrder.value;
  if (!order?.id) return;
  const valid = await refundFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  const refundAmount = Math.round(Number(refundForm.refundAmountYuan || 0) * 100);
  if (refundAmount <= 0 || refundAmount > refundAvailableAmount.value) {
    ElMessage.warning('退款金额必须大于 0 且不能超过可退金额');
    return;
  }
  refundSubmitting.value = true;
  try {
    await paymentRefundApprovalApi.create({
      paymentOrderId: order.id,
      bizRefundNo: refundForm.bizRefundNo.trim() || undefined,
      refundAmount,
      reason: refundForm.reason.trim(),
      remark: refundForm.remark.trim() || undefined,
    });
    ElMessage.success('退款审批已创建');
    refundVisible.value = false;
    await loadRows();
  } finally {
    refundSubmitting.value = false;
  }
}

type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

function statusTagType(status?: string): TagType {
  if (status === 'CREATED') return 'primary';
  if (status === 'PAYING' || status === 'DUPLICATE_REFUNDING') return 'warning';
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'CLOSED' || status === 'DUPLICATE_REFUNDED') return 'info';
  return '';
}

function timelineType(status?: string): TagType {
  if (status === 'CREATED') return 'primary';
  if (status === 'PAYING' || status === 'DUPLICATE_REFUNDING') return 'warning';
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'CLOSED' || status === 'DUPLICATE_REFUNDED') return 'info';
  return 'primary';
}

function showBusinessEntryResult(row: PaymentOrder) {
  return row.status === 'SUCCESS' || row.status === 'DUPLICATE_REFUNDING' || row.status === 'DUPLICATE_REFUNDED';
}

function businessEntryTagType(row: PaymentOrder): TagType {
  if (row.successFlag === 1) return 'success';
  if (showBusinessEntryResult(row)) return 'warning';
  return 'info';
}

function businessEntryText(row: PaymentOrder) {
  if (row.successFlag === 1) return '已入账';
  if (showBusinessEntryResult(row)) return '重复成功未入账';
  return '待判定';
}

function methodText(row: PaymentOrder) {
  return row.methodName || '-';
}

function channelText(row: PaymentOrder) {
  return row.channelName || '-';
}

function formatMoney(value?: number) {
  const amount = Number(value || 0);
  return amount ? `￥${(amount / 100).toFixed(2)}` : '-';
}

function valueText(value?: string | number) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}
</script>

<style scoped>
.payment-orders__detail-block {
  margin-top: 18px;
}

.payment-orders__flow {
  padding: 0 2px;
}

.payment-orders__flow h4 {
  margin: 0 0 14px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0;
}

.payment-orders__flow-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.payment-orders__flow-meta,
.payment-orders__flow-remark {
  margin-top: 4px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.5;
}

.payment-orders__flow-meta {
  color: var(--el-text-color-secondary);
}

.payment-orders__money-input {
  width: 100%;
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
