<template>
  <div class="payment-gateway-result">
    <section class="payment-gateway-result__panel">
      <el-result
        icon="info"
        title="已返回收银台"
        :sub-title="description"
      >
        <template #extra>
          <div class="payment-gateway-result__actions">
            <el-button type="primary" @click="notifyAndClose">返回收银台</el-button>
            <el-button @click="closeCurrentWindow">关闭页面</el-button>
          </div>
        </template>
      </el-result>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

interface GatewayResultMessage {
  type: 'MANGO_PAYMENT_GATEWAY_RESULT';
  payOrderNo: string;
  source: 'gateway-page';
}

const payOrderNo = ref('');

const description = computed(() => {
  if (!payOrderNo.value) {
    return '支付机构已完成页面跳转，请回到原收银台等待系统确认支付结果。';
  }
  return `支付订单 ${payOrderNo.value} 已完成页面跳转，请回到原收银台等待系统确认支付结果。`;
});

onMounted(() => {
  payOrderNo.value = resolvePayOrderNo();
  notifyOpener();
  window.setTimeout(() => {
    closeCurrentWindow();
  }, 1200);
});

function notifyAndClose() {
  notifyOpener();
  closeCurrentWindow();
}

function notifyOpener() {
  if (!window.opener || !payOrderNo.value) {
    return;
  }
  const message: GatewayResultMessage = {
    type: 'MANGO_PAYMENT_GATEWAY_RESULT',
    payOrderNo: payOrderNo.value,
    source: 'gateway-page',
  };
  window.opener.postMessage(message, window.location.origin);
}

function closeCurrentWindow() {
  window.close();
}

function resolvePayOrderNo() {
  const params = new URLSearchParams();
  collectParams(params, window.location.search);
  const hashQueryIndex = window.location.hash.indexOf('?');
  if (hashQueryIndex >= 0) {
    collectParams(params, window.location.hash.slice(hashQueryIndex));
  }
  return params.get('order_id')
    || params.get('payOrderNo')
    || params.get('mchnt_order_no')
    || '';
}

function collectParams(target: URLSearchParams, query: string) {
  if (!query) {
    return;
  }
  const source = new URLSearchParams(query.startsWith('?') ? query.slice(1) : query);
  source.forEach((value, key) => {
    if (!target.has(key)) {
      target.set(key, value);
    }
  });
}
</script>

<style scoped>
.payment-gateway-result {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: var(--el-bg-color-page);
}

.payment-gateway-result__panel {
  width: min(520px, 100%);
  padding: 24px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.payment-gateway-result__actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
