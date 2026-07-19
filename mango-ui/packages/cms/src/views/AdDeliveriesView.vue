<template>
  <CmsResourceView :config="adDeliveryConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../composables/useCmsApi';
import { createCmsResourceConfigs } from './configs';

const { adDeliveryConfig } = createCmsResourceConfigs(useCmsApi());
</script>
