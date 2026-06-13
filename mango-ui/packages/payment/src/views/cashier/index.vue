<template>
  <PaymentCashier
    :cashier-config-id="cashierConfigId"
    :business-order-id="businessOrderId"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import PaymentCashier from '../../components/PaymentCashier.vue';

const route = useRoute();

const cashierConfigId = computed(() => routeParamValue(route.params.cashierId) || routeParamValue(route.query.cashierConfigId));
const businessOrderId = computed(() => routeParamValue(route.query.businessOrderId));

function routeParamValue(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] || '');
  }
  return String(value || '');
}
</script>
