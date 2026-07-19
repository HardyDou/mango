<template>
  <CmsResourceView :config="contentCategoryConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../api/context';
import { createCmsResourceConfigs } from './configs';

const { contentCategoryConfig } = createCmsResourceConfigs(useCmsApi());
</script>
