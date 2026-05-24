<template>
  <div class="payment-cashier">
    <aside v-loading="cashierLoading" class="tenant-rail">
      <div class="rail-head">
        <h2>租户收银台</h2>
        <el-tag type="success" effect="plain">沙箱</el-tag>
      </div>

      <button
        v-for="cashier in tenantCashiers"
        :key="cashier.cashierCode"
        class="tenant-card"
        :class="{ active: selectedCashier?.cashierCode === cashier.cashierCode }"
        type="button"
        @click="selectCashier(cashier)"
      >
        <strong>{{ cashier.tenantName }}</strong>
        <span>{{ cashier.cashierName }} / {{ cashier.appCode }}</span>
        <small>{{ cashier.enabledMethods.length }} 种方式，{{ cashier.expireMinutes }} 分钟过期</small>
      </button>
    </aside>

    <main class="cashier-surface">
      <section class="checkout-panel">
        <div class="checkout-head">
          <div>
            <h3>{{ selectedCashier?.cashierName || '租户收银台' }}</h3>
            <p>{{ selectedCashier ? `${selectedCashier.tenantName} · ${selectedCashier.appCode}` : '加载收银台配置' }}</p>
          </div>
          <el-button :icon="Refresh" @click="resetOrder">换一单</el-button>
        </div>

        <el-form ref="cashierFormRef" :model="cashierForm" :rules="cashierRules" label-width="108px" class="checkout-form">
          <el-form-item label="商户单号" prop="merchantOrderNo">
            <el-input v-model="cashierForm.merchantOrderNo" />
          </el-form-item>
          <el-form-item label="订单标题" prop="subject">
            <el-input v-model="cashierForm.subject" />
          </el-form-item>
          <el-form-item label="金额(分)" prop="amount">
            <el-input-number v-model="cashierForm.amount" :min="1" :step="100" />
          </el-form-item>
          <el-form-item label="支付方式" prop="payMethod">
            <el-radio-group v-model="cashierForm.payMethod">
              <el-radio-button v-for="item in availableMethodOptions" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <div class="order-total">
          <span>应付金额</span>
          <strong>{{ formatAmount(cashierForm.amount) }}</strong>
        </div>

        <div class="checkout-actions">
          <el-button type="primary" size="large" :disabled="!selectedCashier" :loading="creating" @click="createBizOrder">创建订单</el-button>
          <el-button size="large" :disabled="!bizOrderId" :loading="paying" @click="startPayment">发起支付</el-button>
          <el-button type="success" size="large" :disabled="!paymentOrder" :loading="sandboxCompleting" @click="completeSandboxPayment">
            沙箱付款成功
          </el-button>
        </div>
      </section>

      <section class="status-panel">
        <div class="status-head">
          <h3>支付流程</h3>
          <el-tag :type="flowStatusType">{{ flowStatusText }}</el-tag>
        </div>

        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step title="创建订单" />
          <el-step title="发起支付" />
          <el-step title="沙箱成功" />
          <el-step title="结果确认" />
        </el-steps>

        <el-descriptions :column="1" border class="flow-detail">
          <el-descriptions-item label="租户">{{ selectedCashier?.tenantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="应用编码">{{ selectedCashier?.appCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务单 ID">{{ bizOrderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付单 ID">{{ paymentOrder?.paymentOrderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付通道">{{ paymentOrder?.channelCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ cashierForm.payMethod }}</el-descriptions-item>
          <el-descriptions-item label="支付材料">{{ paymentOrder?.materialContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="沙箱事件">{{ sandboxNotify?.notifyEventId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务单状态">{{ bizOrderResult?.status || '-' }}</el-descriptions-item>
        </el-descriptions>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  paymentApi,
  type PayBizOrderVO,
  type PaymentMethodOption,
  type PaymentOrderVO,
  type PaymentTenantCashier,
  type SandboxPaymentNotifyVO,
} from '../../api/payment';

const tenantCashiers = ref<PaymentTenantCashier[]>([]);
const sandboxPayMethods = ref<PaymentMethodOption[]>([]);
const selectedCashier = ref<PaymentTenantCashier>();
const cashierFormRef = ref<FormInstance>();
const cashierForm = reactive({
  merchantOrderNo: nextNo('SO'),
  subject: '租户收银台沙箱订单',
  amount: 100,
  currency: 'CNY',
  payMethod: '',
});
const cashierRules: FormRules = {
  merchantOrderNo: [{ required: true, message: '请输入商户单号', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入订单标题', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'change' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
};
const bizOrderId = ref('');
const paymentOrder = ref<PaymentOrderVO>();
const sandboxNotify = ref<SandboxPaymentNotifyVO>();
const bizOrderResult = ref<PayBizOrderVO>();
const creating = ref(false);
const paying = ref(false);
const sandboxCompleting = ref(false);
const cashierLoading = ref(false);

const availableMethodOptions = computed(() => (selectedCashier.value?.enabledMethods || []).map(method => ({
  label: sandboxPayMethods.value.find(item => item.value === method)?.label || method,
  value: method,
})));
const activeStep = computed(() => {
  if (bizOrderResult.value?.status === 'PAID') return 4;
  if (paymentOrder.value?.status === 'SUCCESS') return 3;
  if (paymentOrder.value) return 2;
  if (bizOrderId.value) return 1;
  return 0;
});
const flowStatusText = computed(() => {
  if (bizOrderResult.value?.status === 'PAID') return '支付完成';
  if (paymentOrder.value?.status === 'SUCCESS') return '通道成功';
  if (paymentOrder.value) return '支付处理中';
  if (bizOrderId.value) return '待发起支付';
  return '待创建';
});
const flowStatusType = computed(() => bizOrderResult.value?.status === 'PAID' ? 'success' : paymentOrder.value ? 'warning' : 'info');

onMounted(() => {
  loadCashierData();
});

async function loadCashierData() {
  cashierLoading.value = true;
  try {
    const [cashiers, methods] = await Promise.all([
      paymentApi.listTenantCashiers(),
      paymentApi.listSandboxMethods(),
    ]);
    tenantCashiers.value = cashiers;
    sandboxPayMethods.value = methods;
    if (cashiers.length) {
      selectCashier(cashiers[0]);
    }
  } finally {
    cashierLoading.value = false;
  }
}

function selectCashier(cashier: PaymentTenantCashier) {
  selectedCashier.value = cashier;
  cashierForm.payMethod = cashier.defaultMethod;
  resetOrder();
}

function resetOrder() {
  cashierForm.merchantOrderNo = nextNo('SO');
  cashierForm.subject = `${selectedCashier.value?.tenantName || '租户'}沙箱支付订单`;
  bizOrderId.value = '';
  paymentOrder.value = undefined;
  sandboxNotify.value = undefined;
  bizOrderResult.value = undefined;
}

async function createBizOrder() {
  if (!selectedCashier.value) {
    return;
  }
  await cashierFormRef.value?.validate();
  creating.value = true;
  try {
    bizOrderId.value = await paymentApi.createBizOrder({
      appCode: selectedCashier.value.appCode,
      merchantOrderNo: cashierForm.merchantOrderNo,
      subject: cashierForm.subject,
      amount: cashierForm.amount,
      currency: cashierForm.currency,
    });
    paymentOrder.value = undefined;
    sandboxNotify.value = undefined;
    bizOrderResult.value = undefined;
    ElMessage.success('收银台订单已创建');
  } finally {
    creating.value = false;
  }
}

async function startPayment() {
  if (!bizOrderId.value) {
    return;
  }
  paying.value = true;
  try {
    paymentOrder.value = await paymentApi.pay({
      bizOrderId: bizOrderId.value,
      payMethod: cashierForm.payMethod,
      idempotencyKey: `PAY-${bizOrderId.value}-${Date.now()}`,
    });
    sandboxNotify.value = await paymentApi.createSandboxPaymentNotify({
      paymentOrderId: paymentOrder.value.paymentOrderId,
      sandboxEventId: `SANDBOX-EVT-${Date.now()}`,
    });
    ElMessage.success('沙箱支付已发起');
  } finally {
    paying.value = false;
  }
}

async function completeSandboxPayment() {
  if (!paymentOrder.value) {
    return;
  }
  sandboxCompleting.value = true;
  try {
    paymentOrder.value = await paymentApi.completeSandboxPayment({
      paymentOrderId: paymentOrder.value.paymentOrderId,
      sandboxEventId: sandboxNotify.value?.notifyEventId || `SANDBOX-EVT-${Date.now()}`,
    });
    if (bizOrderId.value) {
      bizOrderResult.value = await paymentApi.queryBizOrder({ bizOrderId: bizOrderId.value });
    }
    ElMessage.success('沙箱支付流程已完成');
  } finally {
    sandboxCompleting.value = false;
  }
}

function formatAmount(value?: number) {
  return `¥${(Number(value || 0) / 100).toFixed(2)}`;
}

function nextNo(prefix: string) {
  return `${prefix}${Date.now()}${Math.floor(Math.random() * 1000)}`;
}
</script>

<style scoped lang="scss">
.payment-cashier {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  min-height: 100%;
  padding: 16px;
  background: var(--el-bg-color-page);
}

.tenant-rail,
.checkout-panel,
.status-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.rail-head,
.checkout-head,
.status-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.rail-head h2,
.checkout-head h3,
.status-head h3 {
  margin: 0;
  color: var(--el-text-color-primary);
}

.checkout-head p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
}

.tenant-card {
  display: block;
  width: calc(100% - 20px);
  margin: 10px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color-page);
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.tenant-card.active,
.tenant-card:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.tenant-card strong,
.tenant-card span,
.tenant-card small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-card span {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
}

.tenant-card small {
  margin-top: 10px;
  color: var(--el-color-primary);
}

.cashier-surface {
  display: grid;
  grid-template-columns: minmax(360px, 0.8fr) minmax(0, 1.2fr);
  gap: 16px;
  min-width: 0;
}

.checkout-form {
  padding: 16px 16px 0;
}

.order-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 4px 16px 16px;
  padding: 16px;
  border-radius: 8px;
  background: var(--el-bg-color-page);
}

.order-total span {
  color: var(--el-text-color-secondary);
}

.order-total strong {
  color: var(--el-text-color-primary);
  font-size: 30px;
}

.checkout-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  padding: 0 16px 16px;
}

.flow-detail {
  margin: 24px 16px 16px;
}

@media (max-width: 1180px) {
  .payment-cashier,
  .cashier-surface {
    grid-template-columns: 1fr;
  }
}
</style>
