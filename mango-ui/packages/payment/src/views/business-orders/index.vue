<template>
  <div class="payment-business-orders">
    <section class="payment-business-orders__header">
      <div>
        <h3>业务订单</h3>
        <p>查询业务系统提交到支付平台的支付意图、金额、通知地址和支付状态。</p>
      </div>
      <el-button type="primary" plain :icon="Plus" @click="openCreateDialog">新增</el-button>
    </section>

    <section class="payment-business-orders__toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="业务单号 / 应用 / 标题 / 企业主体"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="接入应用">
          <PaymentEntitySelect
            v-model="query.applicationId"
            :api="paymentApplicationApi"
            label-field="appName"
            description-field="appId"
            placeholder="全部应用"
            clearable
            class="payment-business-orders__entity-select"
            @update:model-value="applyFilters"
          />
        </el-form-item>
        <el-form-item label="企业主体">
          <PaymentEntitySelect
            v-model="query.enterpriseSubjectId"
            :api="paymentEnterpriseSubjectApi"
            label-field="subjectName"
            description-field="creditCodeMask"
            placeholder="全部主体"
            clearable
            class="payment-business-orders__entity-select"
            @update:model-value="applyFilters"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select
            v-model="query.statusCode"
            clearable
            fit-input-width
            placeholder="全部状态"
            class="payment-business-orders__status-select"
            @change="applyFilters"
          >
            <el-option
              v-for="status in statusOptions"
              :key="status.statusCode"
              :label="status.statusName"
              :value="status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="payment-business-orders__filter-actions">
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
      class="payment-business-orders__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="bizOrderNo" label="业务订单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="appName" label="接入应用" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ valueText(row.appName) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="支付标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="subjectName" label="企业主体" min-width="190" show-overflow-tooltip />
      <el-table-column label="应付金额（元）" width="120" align="right">
        <template #default="{ row }">
          <span>{{ formatMoney(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已支付（元）" width="120" align="right">
        <template #default="{ row }">
          <span>{{ formatMoney(row.paidAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已退款（元）" width="120" align="right">
        <template #default="{ row }">
          <span>{{ formatMoney(row.refundedAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusName || row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expireTime" label="过期时间" width="170" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="156" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <PaymentRowActions :actions="businessOrderRowActions(row)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="payment-business-orders__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="业务订单详情"
      size="720px"
      destroy-on-close
      append-to-body
      class="payment-business-orders__drawer"
    >
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detail">
        <el-descriptions title="订单信息" :column="2" border>
          <el-descriptions-item label="业务订单号">{{ valueText(detail.bizOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusTagType(detail.status)" effect="light">{{ detail.statusName || detail.status || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接入应用">{{ valueText(detail.appName) }}</el-descriptions-item>
          <el-descriptions-item label="支付标题">{{ valueText(detail.title) }}</el-descriptions-item>
          <el-descriptions-item label="企业主体">{{ valueText(detail.subjectName) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ valueText(detail.currency) }}</el-descriptions-item>
          <el-descriptions-item label="应付金额（元）">{{ formatMoney(detail.amount) }}</el-descriptions-item>
          <el-descriptions-item label="已支付（元）">{{ formatMoney(detail.paidAmount) }}</el-descriptions-item>
          <el-descriptions-item label="已退款（元）">{{ formatMoney(detail.refundedAmount) }}</el-descriptions-item>
          <el-descriptions-item label="支付订单数">{{ detail.paymentOrderCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="退款订单数">{{ detail.refundOrderCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ valueText(detail.expireTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="通知与扩展" :column="1" border class="payment-business-orders__detail-block">
          <el-descriptions-item label="通知地址">{{ valueText(detail.notifyUrl) }}</el-descriptions-item>
          <el-descriptions-item label="返回地址">{{ valueText(detail.returnUrl) }}</el-descriptions-item>
          <el-descriptions-item label="扩展信息">
            <pre class="payment-business-orders__extend">{{ formatExtendInfo(detail.extendInfo) }}</pre>
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-business-orders__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>

        <section class="payment-business-orders__flow payment-business-orders__detail-block">
          <h4>状态流转</h4>
          <el-timeline v-if="detail.statusFlows?.length">
            <el-timeline-item
              v-for="flow in detail.statusFlows"
              :key="`${flow.statusCode}-${flow.happenTime || flow.source}`"
              :timestamp="valueText(flow.happenTime)"
              :type="timelineType(flow.statusCode)"
            >
              <div class="payment-business-orders__flow-title">{{ flow.statusName || flow.statusCode || '-' }}</div>
              <div class="payment-business-orders__flow-meta">
                {{ valueText(flow.source) }}
                <span v-if="flow.triggerNo"> · {{ flow.triggerNo }}</span>
              </div>
              <div class="payment-business-orders__flow-remark">{{ valueText(flow.remark) }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态流转记录" />
        </section>
      </template>
      <el-empty v-else description="未查询到业务订单详情" />
    </el-drawer>

    <el-dialog
      v-model="cashierVisible"
      :title="cashierDialogTitle"
      width="680px"
      append-to-body
      destroy-on-close
      class="payment-cashier-dialog"
    >
      <PaymentCashier
        v-if="cashierOrder?.cashierConfigId"
        :cashier-config-id="cashierOrder.cashierConfigId"
        :business-order-id="cashierOrder.id"
        embedded
        @success="handleCashierSuccess"
        @close="closeCashierDialog"
      />
    </el-dialog>

    <el-dialog
      v-model="createVisible"
      title="新增业务订单"
      width="520px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      :before-close="beforeCloseCreateDialog"
      class="payment-business-orders__create-dialog"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="108px"
        class="payment-business-orders__create-form"
      >
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="收银台" prop="cashierConfigId">
              <PaymentEntitySelect
                v-model="createForm.cashierConfigId"
                :api="paymentCashierConfigApi"
                label-field="cashierName"
                description-field="enterpriseSubjectNames"
                placeholder="请选择收银台"
                class="payment-business-orders__create-select"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品名称" prop="title">
              <el-input v-model="createForm.title" maxlength="128" show-word-limit placeholder="请输入商品或业务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="应付金额" prop="amountYuan">
              <div class="payment-business-orders__amount-input">
                <el-input-number
                  v-model="createForm.amountYuan"
                  :min="0.01"
                  :precision="2"
                  :step="0.01"
                  controls-position="right"
                />
                <span>元</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button :disabled="creating" @click="requestCloseCreateDialog">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreateOrder">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CreditCard, Plus, Refresh, Search, Tickets } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import Pagination from '@mango/common/components/Pagination/index.vue';
import PaymentCashier from '../../components/PaymentCashier.vue';
import PaymentEntitySelect from '../../components/PaymentEntitySelect.vue';
import PaymentRowActions from '../../components/PaymentRowActions.vue';
import type { PaymentRowAction } from '../../components/PaymentRowActions';
import {
  paymentApplicationApi,
  paymentBusinessOrderApi,
  paymentCashierConfigApi,
  paymentEnterpriseSubjectApi,
  type PaymentBusinessOrder,
  type PaymentBusinessOrderStatus,
  type PaymentCashierConfig,
  type PaymentCashierPayResult,
  type CreatePaymentBusinessOrderCommand,
  type PaymentPageQuery,
} from '../../api/payment';

interface CreateBusinessOrderForm {
  cashierConfigId: string;
  title: string;
  amountYuan: number;
}

interface ResolvedCreateDefaults {
  cashier: PaymentCashierConfig;
  appId: string;
  subjectId: string;
}

const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
  applicationId: '',
  enterpriseSubjectId: '',
});
const rows = ref<PaymentBusinessOrder[]>([]);
const statusOptions = ref<PaymentBusinessOrderStatus[]>([]);
const total = ref(0);
const loading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentBusinessOrder>();
const cashierVisible = ref(false);
const cashierOrder = ref<PaymentBusinessOrder>();
const errorMessage = ref('');
const createVisible = ref(false);
const creating = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive<CreateBusinessOrderForm>(defaultCreateForm());

const emptyDescription = computed(() => {
  if (errorMessage.value) return '业务订单加载失败';
  return query.keyword || query.statusCode || query.applicationId || query.enterpriseSubjectId ? '未查询到匹配的业务订单' : '暂无业务订单';
});
const cashierDialogTitle = computed(() => cashierOrder.value?.bizOrderNo ? `收银台 - ${cashierOrder.value.bizOrderNo}` : '收银台');
const createDirty = computed(() => JSON.stringify(createForm) !== JSON.stringify(defaultCreateForm()));

const createRules: FormRules<CreateBusinessOrderForm> = {
  title: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 128, message: '商品名称长度不能超过 128 个字符', trigger: 'blur' },
  ],
  cashierConfigId: [
    { required: true, message: '请选择收银台', trigger: 'change' },
  ],
  amountYuan: [
    { required: true, type: 'number', min: 0.01, message: '请输入大于 0 的应付金额', trigger: 'blur' },
  ],
};

onMounted(async () => {
  await Promise.all([loadStatuses(), loadRows()]);
});

async function loadStatuses() {
  statusOptions.value = await paymentBusinessOrderApi.statuses();
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentBusinessOrderApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    rows.value = [];
    total.value = 0;
    errorMessage.value = error instanceof Error ? error.message : '业务订单加载失败';
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  query.statusCode = '';
  query.applicationId = '';
  query.enterpriseSubjectId = '';
  void loadRows();
}

function applyFilters() {
  query.pageNum = 1;
  void loadRows();
}

async function openDetail(row: PaymentBusinessOrder) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentBusinessOrderApi.detail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

function openCashier(row: PaymentBusinessOrder) {
  if (!isPayable(row)) {
    ElMessage.warning(payDisabledReason(row));
    return;
  }
  if (!row.cashierConfigId) {
    ElMessage.warning('当前业务订单未匹配收银台配置');
    return;
  }
  cashierOrder.value = row;
  cashierVisible.value = true;
}

function closeCashierDialog() {
  cashierVisible.value = false;
  cashierOrder.value = undefined;
}

async function handleCashierSuccess(_result: PaymentCashierPayResult) {
  closeCashierDialog();
  await loadRows();
}

function openCreateDialog() {
  resetCreateForm();
  createVisible.value = true;
}

async function beforeCloseCreateDialog(done?: () => void) {
  if (!creating.value && createDirty.value) {
    try {
      await ElMessageBox.confirm('当前新增业务订单尚未保存，确认关闭？', '关闭确认', {
        confirmButtonText: '确认关闭',
        cancelButtonText: '继续编辑',
        type: 'warning',
      });
    } catch {
      return;
    }
  }
  createVisible.value = false;
  done?.();
}

function requestCloseCreateDialog() {
  void beforeCloseCreateDialog();
}

async function submitCreateOrder() {
  await createFormRef.value?.validate();
  creating.value = true;
  try {
    const created = await paymentBusinessOrderApi.create(await toCreatePayload());
    ElMessage.success(`已新增业务订单 ${created.bizOrderNo || ''}`.trim());
    createVisible.value = false;
    query.pageNum = 1;
    query.keyword = created.bizOrderNo || '';
    await loadRows();
  } finally {
    creating.value = false;
  }
}

function businessOrderRowActions(row: PaymentBusinessOrder): PaymentRowAction[] {
  return [
    {
      key: 'pay',
      label: '支付',
      type: 'primary',
      icon: CreditCard,
      disabled: !isPayable(row),
      tooltip: payDisabledReason(row),
      onClick: () => openCashier(row),
    },
    {
      key: 'detail',
      label: '详情',
      type: 'primary',
      icon: Tickets,
      onClick: () => openDetail(row),
    },
  ];
}

type TagType = '' | 'success' | 'warning' | 'info' | 'primary' | 'danger';

function statusTagType(status?: string): TagType {
  if (status === 'TO_PAY') return 'primary';
  if (status === 'PAYING' || status === 'REFUNDING') return 'warning';
  if (status === 'PAID' || status === 'SUCCESS') return 'success';
  if (status === 'PARTIAL_REFUNDED') return 'primary';
  if (status === 'REFUNDED' || status === 'CLOSED') return 'info';
  if (status === 'FAILED') return 'danger';
  return '';
}

function timelineType(status?: string): TagType {
  if (status === 'TO_PAY') return 'primary';
  if (status === 'PAYING' || status === 'REFUNDING') return 'warning';
  if (status === 'PAID' || status === 'SUCCESS') return 'success';
  if (status === 'PARTIAL_REFUNDED') return 'primary';
  if (status === 'REFUNDED' || status === 'CLOSED') return 'info';
  if (status === 'FAILED') return 'danger';
  return 'primary';
}

function isPayable(row: PaymentBusinessOrder) {
  return row.payable === true;
}

function payDisabledReason(row: PaymentBusinessOrder) {
  return row.payDisabledReason || '当前业务订单不可发起支付';
}

function formatMoney(value?: number) {
  const amount = Number(value || 0);
  return amount ? `￥${(amount / 100).toFixed(2)}` : '-';
}

function yuanToCents(value: number) {
  return Math.round(Number(value || 0) * 100);
}

function valueText(value?: string | number) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function formatExtendInfo(value?: string) {
  if (!value) return '-';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function defaultCreateForm(): CreateBusinessOrderForm {
  return {
    cashierConfigId: '',
    title: '',
    amountYuan: 0.01,
  };
}

function resetCreateForm() {
  Object.assign(createForm, defaultCreateForm());
  createFormRef.value?.clearValidate();
}

async function toCreatePayload(): Promise<CreatePaymentBusinessOrderCommand> {
  const defaults = await resolveCreateDefaults();
  return {
    appId: defaults.appId,
    subjectId: defaults.subjectId,
    title: createForm.title.trim(),
    amount: yuanToCents(createForm.amountYuan),
    currency: 'CNY',
    returnUrl: defaults.cashier.resultReturnUrl || '/payment/cashier-result',
  };
}

async function resolveCreateDefaults(): Promise<ResolvedCreateDefaults> {
  const cashier = await paymentCashierConfigApi.detail(createForm.cashierConfigId);
  if (!cashier?.applicationId) {
    throw new Error('所选收银台未关联接入应用');
  }
  const subjectId = String(cashier.enterpriseSubjectIds || '')
    .split(',')
    .map(item => item.trim())
    .find(Boolean);
  if (!subjectId) {
    throw new Error('默认收银台未配置签约主体');
  }
  const application = await paymentApplicationApi.detail(cashier.applicationId);
  if (!application.appId) {
    throw new Error('默认收银台关联应用缺少 AppId');
  }
  return {
    cashier,
    appId: application.appId,
    subjectId,
  };
}
</script>

<style scoped>
.payment-business-orders__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.payment-business-orders__header .el-button {
  flex: 0 0 auto;
}

.payment-business-orders__entity-select,
.payment-business-orders__status-select {
  width: 180px;
}

.payment-business-orders__detail-block {
  margin-top: 18px;
}

.payment-business-orders__extend {
  margin: 0;
  color: var(--el-text-color-regular);
  font-family: var(--el-font-family);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.payment-business-orders__flow h4 {
  margin: 0 0 14px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 650;
}

.payment-business-orders__flow-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.payment-business-orders__flow-meta,
.payment-business-orders__flow-remark {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

:global(.payment-cashier-dialog) {
  max-height: 86vh;
  overflow: hidden;
}

.payment-business-orders__amount-input {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-regular);
}

.payment-business-orders__amount-input :deep(.el-input-number) {
  flex: 1;
}

.payment-business-orders__create-select {
  width: 100%;
}

</style>
