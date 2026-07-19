<template>
  <CmsResourceView :config="navigationConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../composables/useCmsApi';
import { createCmsResourceConfigs } from './configs';

const { navigationConfig } = createCmsResourceConfigs(useCmsApi());
</script>
