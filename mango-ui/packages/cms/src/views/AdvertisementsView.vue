<template>
  <CmsResourceView :config="advertisementConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../composables/useCmsApi';
import { createCmsResourceConfigs } from './configs';

const { advertisementConfig } = createCmsResourceConfigs(useCmsApi());
</script>
