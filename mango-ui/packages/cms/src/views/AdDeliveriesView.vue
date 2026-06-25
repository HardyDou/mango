<template>
  <CmsResourceView :config="adDeliveryConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { adDeliveryConfig } from './configs';
</script>
