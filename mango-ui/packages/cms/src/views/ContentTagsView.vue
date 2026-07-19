<template>
  <CmsResourceView :config="contentTagConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../composables/useCmsApi';
import { createCmsResourceConfigs } from './configs';

const { contentTagConfig } = createCmsResourceConfigs(useCmsApi());
</script>
