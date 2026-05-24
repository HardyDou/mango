<template>
  <el-card class="payment-sandbox" shadow="never">
    <template #header>
      <div class="card-header">
        <span>沙箱支付联调</span>
        <el-tag type="success" size="small">SANDBOX</el-tag>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
      <el-form-item label="应用编码" prop="appCode">
        <el-input v-model="form.appCode" />
      </el-form-item>
      <el-form-item label="商户单号" prop="merchantOrderNo">
        <el-input v-model="form.merchantOrderNo" />
      </el-form-item>
      <el-form-item label="订单标题" prop="subject">
        <el-input v-model="form.subject" />
      </el-form-item>
      <el-form-item label="金额(分)" prop="amount">
        <el-input-number v-model="form.amount" :min="1" />
      </el-form-item>
      <el-form-item label="支付方式" prop="payMethod">
        <el-select v-model="payMethod" placeholder="选择沙箱支付方式">
          <el-option v-for="item in sandboxPayMethods" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="creating" @click="createOrder">创建业务单</el-button>
        <el-button :disabled="!bizOrderId || !payMethod" :loading="paying" @click="payOrder">发起支付</el-button>
        <el-button :disabled="!paymentOrder" :loading="sandboxCompleting" type="success" @click="completeSandboxPay">沙箱付款成功</el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="bizOrderId || paymentOrder" :column="1" border>
      <el-descriptions-item label="业务支付单 ID">{{ bizOrderId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付单 ID">{{ paymentOrder?.paymentOrderId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付通道">{{ paymentOrder?.channelCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付状态">{{ paymentOrder?.status || '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付材料">{{ paymentOrder?.materialContent || '-' }}</el-descriptions-item>
      <el-descriptions-item label="沙箱回调">{{ sandboxNotify?.channelOrderNo || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="创建业务单后可继续发起支付" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import type { ApiId } from '@mango/api-schema';
import { paymentApi, type PaymentMethodOption, type PaymentOrderVO, type SandboxPaymentNotifyVO } from '../api/payment';

const emit = defineEmits<{
  (event: 'created', bizOrderId: ApiId): void;
  (event: 'paid', paymentOrder: PaymentOrderVO): void;
}>();

const formRef = ref<FormInstance>();
const form = reactive({
  appCode: 'mango-demo',
  merchantOrderNo: `SO${Date.now()}`,
  subject: '平台支付能力测试订单',
  amount: 100,
  currency: 'CNY',
});
const payMethod = ref('');
const sandboxPayMethods = ref<PaymentMethodOption[]>([]);
const rules: FormRules = {
  appCode: [{ required: true, message: '请输入应用编码', trigger: 'blur' }],
  merchantOrderNo: [{ required: true, message: '请输入商户单号', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入订单标题', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入支付金额', trigger: 'change' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
};
const bizOrderId = ref<ApiId>();
const paymentOrder = ref<PaymentOrderVO>();
const sandboxNotify = ref<SandboxPaymentNotifyVO>();
const creating = ref(false);
const paying = ref(false);
const sandboxCompleting = ref(false);

onMounted(() => {
  loadSandboxMethods();
});

async function loadSandboxMethods() {
  sandboxPayMethods.value = await paymentApi.listSandboxMethods();
  payMethod.value = sandboxPayMethods.value[0]?.value || '';
}

async function createOrder() {
  await formRef.value?.validate();
  creating.value = true;
  try {
    bizOrderId.value = await paymentApi.createBizOrder(form);
    paymentOrder.value = undefined;
    sandboxNotify.value = undefined;
    emit('created', bizOrderId.value);
  } finally {
    creating.value = false;
  }
}

async function payOrder() {
  if (!bizOrderId.value) {
    return;
  }
  paying.value = true;
  try {
    paymentOrder.value = await paymentApi.pay({
      bizOrderId: bizOrderId.value,
      payMethod: payMethod.value,
      idempotencyKey: `PAY-${bizOrderId.value}-${Date.now()}`,
    });
    sandboxNotify.value = await paymentApi.createSandboxPaymentNotify({
      paymentOrderId: paymentOrder.value.paymentOrderId,
      sandboxEventId: `SANDBOX-EVT-${Date.now()}`,
    });
    emit('paid', paymentOrder.value);
  } finally {
    paying.value = false;
  }
}

async function completeSandboxPay() {
  if (!paymentOrder.value) {
    return;
  }
  sandboxCompleting.value = true;
  try {
    paymentOrder.value = await paymentApi.completeSandboxPayment({
      paymentOrderId: paymentOrder.value.paymentOrderId,
      sandboxEventId: sandboxNotify.value?.notifyEventId || `SANDBOX-EVT-${Date.now()}`,
    });
    emit('paid', paymentOrder.value);
  } finally {
    sandboxCompleting.value = false;
  }
}
</script>

<style scoped lang="scss">
.payment-sandbox {
  max-width: 620px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
