<template>
  <CmsResourceView :config="bannerConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../composables/useCmsApi';
import { createCmsResourceConfigs } from './configs';

const { bannerConfig } = createCmsResourceConfigs(useCmsApi());
</script>
