<template>
  <CmsResourceView :config="contentCategoryConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../composables/useCmsApi';
import { createCmsResourceConfigs } from './configs';

const { contentCategoryConfig } = createCmsResourceConfigs(useCmsApi());
</script>
