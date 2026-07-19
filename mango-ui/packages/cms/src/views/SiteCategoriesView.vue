<template>
  <CmsResourceView :config="siteCategoryConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../api/context';
import { createCmsResourceConfigs } from './configs';

const { siteCategoryConfig } = createCmsResourceConfigs(useCmsApi());
</script>
